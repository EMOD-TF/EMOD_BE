package com.emod.emod.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtProvider {

    private final Key key;
    private final long validitySeconds; // 초 단위

    public JwtProvider(JwtProperties props) {
        byte[] raw = normalizeSecret(props.getSecret());
        if (raw.length < 32) {
            // 응급 확장: SHA-256으로 32바이트 확보
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                raw = md.digest(raw);
            } catch (Exception e) {
                throw new IllegalStateException("JWT secret too short and SHA-256 unavailable", e);
            }
        }
        this.key = Keys.hmacShaKeyFor(raw);
        this.validitySeconds = props.getAccessTokenValiditySeconds();
    }

    private byte[] normalizeSecret(String secret) {
        if (secret == null) throw new IllegalArgumentException("jwt.secret is null");
        // 공백/개행 제거
        String s = secret.trim();
        // base64 시도
        try {
            return Decoders.BASE64.decode(s);
        } catch (IllegalArgumentException ignore) {
            // base64가 아니면 raw bytes 사용
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String generateToken(Long authId, String deviceCode) {
        Instant now = Instant.now();
        Instant exp = (validitySeconds >= 0) ? now.plusSeconds(validitySeconds) : null;

        JwtBuilder b = Jwts.builder()
                .setSubject(String.valueOf(authId))
                .addClaims(Map.of("authId", authId, "deviceCode", deviceCode))
                .setIssuedAt(Date.from(now))
                .signWith(key, SignatureAlgorithm.HS256);

        if (exp != null) b.setExpiration(Date.from(exp));
        return b.compact();
    }

    public Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
