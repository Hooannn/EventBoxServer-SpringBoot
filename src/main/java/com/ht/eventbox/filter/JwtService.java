package com.ht.eventbox.filter;

import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${application.security.jwt.access-secret-key}")
    private String accessSecretKey;
    @Value("${application.security.jwt.refresh-secret-key}")
    private String refreshSecretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh.expiration}")
    private long jwtRefreshExpiration;

    private Claims extractAllClaims(String token, String secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, String secretKey, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, secretKey);
        return claimsResolver.apply(claims);
    }
    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractSub(String jwt, String secretKey) {
        return extractClaim(jwt, secretKey, Claims::getSubject);
    }

    public List<String> extractRoles(String jwt, String secretKey) {
        return extractClaim(jwt, secretKey, claims -> {
            Object roles = claims.get("roles");
            if (roles instanceof List<?>) {
                return ((List<?>) roles).stream()
                        .filter(role -> role instanceof String)
                        .map(Object::toString)
                        .toList();
            }
            return List.of();
        });
    }

    public List<String> extractPermissions(String jwt, String secretKey) {
        return extractClaim(jwt, secretKey, claims -> {
            Object roles = claims.get("permissions");
            if (roles instanceof List<?>) {
                return ((List<?>) roles).stream()
                        .filter(role -> role instanceof String)
                        .map(Object::toString)
                        .toList();
            }
            return List.of();
        });
    }

    public String generateAccessToken(User user) {
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream().map(Role::getName).toList());
        var permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .distinct()
                .toList();
        claims.put("permissions",
                permissions.stream()
                        .map(Permission::getName)
                        .toList()
                );
        return buildToken(claims, user.getId().toString(), jwtExpiration, accessSecretKey);
    }

    public String generateRefreshToken(User user) {
        HashMap<String, Object> claims = new HashMap<>();
        return buildToken(claims, user.getId().toString(), jwtRefreshExpiration, refreshSecretKey);
    }

    private Date extractExpiration(String token, String secretKey) {
        return extractClaim(token, secretKey, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, String secretKey) {
        return !isTokenExpired(token, secretKey);
    }

    private boolean isTokenExpired(String token, String secretKey) {
        return extractExpiration(token, secretKey).before(new Date());
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration,
            String secret
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }
}
