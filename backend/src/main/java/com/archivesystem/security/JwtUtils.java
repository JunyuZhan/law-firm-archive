package com.archivesystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类.
 * @author junyuzhan
 */
@Slf4j
@Component
public class JwtUtils {

    private static final int MIN_SECRET_BYTES = 32;
    private static final String EXAMPLE_SECRET = "your-256-bit-secret-key-for-archive-system-2026";

    private final RuntimeSecretProvider runtimeSecretProvider;
    private final long expiration;
    private final long refreshExpiration;

    @Autowired
    public JwtUtils(
            RuntimeSecretProvider runtimeSecretProvider,
            @org.springframework.beans.factory.annotation.Value("${jwt.expiration:86400000}") long expiration,
            @org.springframework.beans.factory.annotation.Value("${jwt.refresh-expiration:604800000}") long refreshExpiration) {
        this.runtimeSecretProvider = runtimeSecretProvider;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    JwtUtils(String secret, long expiration, long refreshExpiration) {
        this(new FixedRuntimeSecretProvider(validateSecret(secret)), expiration, refreshExpiration);
    }

    /**
     * 生成访问令牌.
     */
    public String generateAccessToken(Long userId, String username, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        claims.put("type", "access");
        return generateToken(claims, expiration);
    }

    /**
     * 生成刷新令牌.
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return generateToken(claims, refreshExpiration);
    }

    /**
     * 生成令牌.
     */
    private String generateToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析令牌.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
            throw e;
        } catch (MalformedJwtException | SignatureException e) {
            log.warn("Token格式错误或签名无效");
            throw e;
        }
    }

    /**
     * 验证令牌是否有效.
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从令牌中获取用户名.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从令牌中获取用户类型.
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 判断是否是刷新令牌.
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }

    public long getRefreshExpirationMillis() {
        return refreshExpiration;
    }

    /**
     * 获取签名密钥.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = runtimeSecretProvider.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static String validateSecret(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret 未配置，服务启动已拒绝");
        }
        String normalized = rawSecret.trim();
        if (EXAMPLE_SECRET.equals(normalized)) {
            throw new IllegalStateException("jwt.secret 不能使用示例默认值");
        }
        if (normalized.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret 长度不足，至少需要32字节");
        }
        return normalized;
    }

    private static final class FixedRuntimeSecretProvider extends RuntimeSecretProvider {

        private final String secret;

        private FixedRuntimeSecretProvider(String secret) {
            super(null);
            this.secret = secret;
        }

        @Override
        public String getJwtSecret() {
            return secret;
        }

        @Override
        public String getCryptoSecret() {
            return secret;
        }

        @Override
        public String getLegacyCryptoSecret() {
            return null;
        }
    }
}
