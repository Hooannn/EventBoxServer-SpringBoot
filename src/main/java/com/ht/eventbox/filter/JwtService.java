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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${application.security.jwt.refresh-secret-key}")
    private String refreshSecretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.refresh.expiration}")
    private long jwtRefreshExpiration;
    @Value("${application.security.jwt.qrcode-secret-key}")
    private String qrcodeSecretKey;
    @Value("${application.security.jwt.qrcode.expiration}")
    private long qrcodeExpiration;

    private final PrivateKey atPrivateKey;

    private Claims extractAllClaims(String token, String secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims extractAllClaims(String token, Key signingKey) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, String secretKey, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, secretKey);
        return claimsResolver.apply(claims);
    }

    public <T> T extractClaim(String token, Key signingKey, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, signingKey);
        return claimsResolver.apply(claims);
    }

    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractSub(String jwt, String secretKey) {
        return extractClaim(jwt, secretKey, Claims::getSubject);
    }

    public String extractSub(String jwt, Key signingKey) {
        return extractClaim(jwt, signingKey, Claims::getSubject);
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

    public List<String> extractRoles(String jwt, Key signingKey) {
        return extractClaim(jwt, signingKey, claims -> {
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

    public List<String> extractPermissions(String jwt, Key signingKey) {
        return extractClaim(jwt, signingKey, claims -> {
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
        return buildToken(claims, user.getId().toString(), jwtExpiration, atPrivateKey, SignatureAlgorithm.RS256);
    }

    public String generateRefreshToken(User user) {
        HashMap<String, Object> claims = new HashMap<>();
        return buildToken(claims, user.getId().toString(), jwtRefreshExpiration, getSigningKey(refreshSecretKey), SignatureAlgorithm.HS256);
    }

    public String generateQrCode(long ticketItemId) {
        HashMap<String, Object> claims = new HashMap<>();
        return buildToken(claims, String.valueOf(ticketItemId), qrcodeExpiration, getSigningKey(qrcodeSecretKey), SignatureAlgorithm.HS256);
    }

    private Date extractExpiration(String token, String secretKey) {
        return extractClaim(token, secretKey, Claims::getExpiration);
    }

    private Date extractExpiration(String token, Key signingKey) {
        return extractClaim(token, signingKey, Claims::getExpiration);
    }

    public boolean isTokenValid(String token, String secretKey) {
        return !isTokenExpired(token, secretKey);
    }

    public boolean isTokenValid(String token, Key signingKey) {
        return !isTokenExpired(token, signingKey);
    }

    private boolean isTokenExpired(String token, String secretKey) {
        return extractExpiration(token, secretKey).before(new Date());
    }

    private boolean isTokenExpired(String token, Key signingKey) {
        return extractExpiration(token, signingKey).before(new Date());
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            long expiration,
            Key signingKey,
            SignatureAlgorithm signatureAlgorithm
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey, signatureAlgorithm)
                .compact();
    }
}
