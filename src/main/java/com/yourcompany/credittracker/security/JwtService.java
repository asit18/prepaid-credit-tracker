package com.yourcompany.credittracker.security;

import com.yourcompany.credittracker.model.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecurityProperties securityProperties;

    public JwtService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String createAccessToken(AdminUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(securityProperties.jwt().issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(securityProperties.jwt().accessTokenTtlMinutes() * 60L)))
                .id(UUID.randomUUID().toString())
                .signWith(key())
                .compact();
    }

    public String createMfaTempToken(AdminUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(securityProperties.jwt().issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("type", "mfa_temp")
                .claim("mfa_pending", true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(securityProperties.jwt().mfaTempTokenTtlMinutes() * 60L)))
                .id(UUID.randomUUID().toString())
                .signWith(key())
                .compact();
    }

    public Claims validateAccessToken(String token) {
        Claims claims = parse(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Invalid token type");
        }
        return claims;
    }

    public Long validateMfaTempTokenAndGetUserId(String token) {
        Claims claims = parse(token);
        if (!"mfa_temp".equals(claims.get("type", String.class))
                || !Boolean.TRUE.equals(claims.get("mfa_pending", Boolean.class))) {
            throw new BadCredentialsException("Invalid MFA token");
        }
        return Long.valueOf(claims.getSubject());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(securityProperties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey key() {
        String secret = securityProperties.jwt().secret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
