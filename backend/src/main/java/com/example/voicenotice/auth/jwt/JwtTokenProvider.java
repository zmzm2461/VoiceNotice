package com.example.voicenotice.auth.jwt;

import com.example.voicenotice.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String secret = "voice-notice-jwt-secret-key-voice-notice-jwt-secret-key";
    private final long accessTokenValidityMs = 1000L * 60 * 60 * 24 * 7;

    private final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("provider", user.getProvider())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }
}