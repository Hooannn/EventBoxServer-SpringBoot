package com.ht.elearning.jwt;

import com.ht.elearning.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.access-secret-key}")
    private String accessSecretKey;
    @Value("${application.security.jwt.refresh-secret-key}")
    private String refreshSecretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(accessSecretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims extractAllClaims(String token, boolean refresh) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(refreshSecretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, boolean refresh) {
        final Claims claims = extractAllClaims(token, refresh);
        return claimsResolver.apply(claims);
    }

    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractSub(String jwt) {
        return extractClaim(jwt, Claims::getSubject);
    }

    public String extractSub(String jwt, boolean refresh) {
        return extractClaim(jwt, Claims::getSubject, refresh);
    }

    public String extractRole(String jwt) {
        return extractClaim(jwt, claims -> claims.get("role")).toString();
    }

    public String generateAccessToken(User user) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().toString());
        return buildToken(claims, user, jwtExpiration, accessSecretKey);
    }

    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user, refreshExpiration, refreshSecretKey);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Date extractExpiration(String token, boolean refresh) {
        return extractClaim(token, Claims::getExpiration, refresh);
    }

    public boolean isTokenValid(String token) {
        return !isTokenExpired(token);
    }

    public boolean isTokenValid(String token, boolean refresh) {
        return !isTokenExpired(token, refresh);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private boolean isTokenExpired(String token, boolean refresh) {
        return extractExpiration(token, refresh).before(new Date());
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            User user,
            long expiration,
            String secret
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(user.getId())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }
}
