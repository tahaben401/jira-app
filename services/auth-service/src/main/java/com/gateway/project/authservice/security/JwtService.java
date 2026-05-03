package com.gateway.project.authservice.security;

import com.gateway.project.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;  // ms

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role",   user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey())
                .compact();
    }

    // ── Validate ──────────────────────────────────────────────────────────────

    public boolean isValid(String token) {
        try {
            return !extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Extract claims ────────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, c -> c.get("userId", Long.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
