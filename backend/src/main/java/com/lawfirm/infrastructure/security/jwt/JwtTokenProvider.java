package com.lawfirm.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * JWT Token 提供者.
 *
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
public class JwtTokenProvider {

  /** JWT密钥. */
  @Value("${jwt.secret}")
  private String secret;

  /** Spring环境对象. */
  private final Environment environment;

  /** 最小密钥长度（字节）. */
  private static final int MIN_SECRET_LENGTH = 32;

  /** JWT过期时间. */
  @Value("${jwt.expiration}")
  private long expiration;

  /** JWT刷新过期时间. */
  @Value("${jwt.refresh-expiration}")
  private long refreshExpiration;

  /**
   * 构造函数
   *
   * @param environment Spring环境对象
   */
  public JwtTokenProvider(final Environment environment) {
    this.environment = environment;
  }

  /** 启动时验证JWT密钥安全性. */
  @PostConstruct
  public void validateSecretKey() {
    // 默认弱密钥
    String defaultWeakSecret = "your-256-bit-secret-key-here-change-in-production";

    // 检查是否为生产环境
    boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

    // 如果使用默认密钥且是生产环境，记录严重警告
    if (defaultWeakSecret.equals(secret) && isProd) {
      log.error("================================================");
      log.error("⚠️  严重安全警告：生产环境使用了默认JWT密钥！");
      log.error("⚠️  请立即通过环境变量 JWT_SECRET 设置强随机密钥");
      log.error("⚠️  生成密钥命令: openssl rand -base64 32");
      log.error("================================================");
    } else if (defaultWeakSecret.equals(secret)) {
      log.warn("开发环境使用默认JWT密钥，生产环境请务必修改");
    }

    // 验证密钥长度（至少32字节，256位）
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
      log.warn("JWT密钥长度不足32字节，建议使用更长的密钥以提高安全性");
    }
  }

  /**
   * 获取签名密钥.
   *
   * @return 签名密钥
   */
  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 生成访问令牌
   *
   * @param userId 用户ID
   * @param username 用户名
   * @return 访问令牌
   */
  public String generateAccessToken(final Long userId, final String username) {
    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * 生成刷新令牌
   *
   * @param userId 用户ID
   * @param username 用户名
   * @return 刷新令牌
   */
  public String generateRefreshToken(final Long userId, final String username) {
    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("type", "refresh")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * 从令牌获取用户名
   *
   * @param token JWT令牌
   * @return 用户名
   */
  public String getUsernameFromToken(final String token) {
    return parseClaims(token).getSubject();
  }

  /**
   * 从令牌获取用户ID
   *
   * @param token JWT令牌
   * @return 用户ID
   */
  public Long getUserIdFromToken(final String token) {
    return parseClaims(token).get("userId", Long.class);
  }

  /**
   * 从令牌获取签发时间
   *
   * @param token JWT令牌
   * @return 签发时间（毫秒时间戳）
   */
  public long getIssuedAtFromToken(final String token) {
    return parseClaims(token).getIssuedAt().getTime();
  }

  /**
   * 从令牌获取过期时间
   *
   * @param token JWT令牌
   * @return 过期时间（毫秒时间戳）
   */
  public long getExpirationFromToken(final String token) {
    return parseClaims(token).getExpiration().getTime();
  }

  /**
   * 获取令牌剩余有效期（秒）
   *
   * @param token JWT令牌
   * @return 剩余有效期（秒）
   */
  public long getRemainingExpirationSeconds(final String token) {
    long expirationTime = getExpirationFromToken(token);
    long currentTime = System.currentTimeMillis();
    return Math.max(0, (expirationTime - currentTime) / 1000);
  }

  /**
   * 验证令牌
   *
   * @param token JWT令牌
   * @return 是否有效
   */
  public boolean validateToken(final String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.warn("JWT令牌验证失败: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 解析令牌获取Claims
   *
   * @param token JWT令牌
   * @return Claims对象
   */
  private Claims parseClaims(final String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
