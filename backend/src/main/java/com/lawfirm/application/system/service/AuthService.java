package com.lawfirm.application.system.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.security.LoginUser;
import com.lawfirm.infrastructure.security.UserDetailsServiceImpl;
import com.lawfirm.infrastructure.security.jwt.JwtTokenProvider;
import com.lawfirm.infrastructure.security.jwt.TokenBlacklistService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/** 认证服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  /** 认证管理器. */
  private final AuthenticationManager authenticationManager;

  /** JWT令牌提供者. */
  private final JwtTokenProvider jwtTokenProvider;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** Redis模板. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** 登录日志服务. */
  private final LoginLogService loginLogService;

  /** 会话应用服务. */
  private final SessionAppService sessionAppService;

  /** 用户代理解析器. */
  private final com.lawfirm.application.system.util.UserAgentParser userAgentParser;

  /** 告警服务. */
  private final com.lawfirm.infrastructure.notification.AlertService alertService;

  /** 用户详细信息服务. */
  private final UserDetailsServiceImpl userDetailsService;

  /** 令牌黑名单服务. */
  private final TokenBlacklistService tokenBlacklistService;

  /** 令牌缓存前缀. */
  private static final String TOKEN_CACHE_PREFIX = "token:";

  /** Token过期时间（秒）：24小时 */
  private static final long TOKEN_EXPIRE_SECONDS = 86400L;

  /** IP登录失败锁定时间（分钟） */
  private static final int IP_LOCK_DURATION_MINUTES = 15;

  /** Token刷新时额外缓冲时间（秒） */
  private static final int TOKEN_REFRESH_BUFFER_SECONDS = 60;

  /** 令牌缓存小时数. */
  private static final long TOKEN_CACHE_HOURS = 24;

  /**
   * 用户登录
   *
   * @param username 用户名
   * @param password 密码
   * @param ip IP地址
   * @param userAgent User-Agent
   * @return 登录结果
   */
  public LoginResult login(
      final String username, final String password, final String ip, final String userAgent) {
    // ⚠️ 安全加固：登录前检查账户锁定状态和失败次数
    // 1. 检查账户是否被锁定
    User user = userRepository.findByUsername(username).orElse(null);
    if (user != null && "LOCKED".equals(user.getStatus())) {
      log.warn("尝试登录已锁定账户: username={}, ip={}", username, ip);
      loginLogService.recordLoginFailure(username, ip, userAgent, "账户已被锁定");
      throw new BusinessException("账户已被锁定，请联系管理员");
    }

    // 2. 检查登录失败次数（防止撞库攻击）
    if (loginLogService.shouldLockAccount(username)) {
      // 自动锁定账户
      if (user != null) {
        user.setStatus("LOCKED");
        userRepository.updateById(user);
        log.warn("账户因连续登录失败被自动锁定: username={}, ip={}", username, ip);
        // 发送账户锁定告警
        try {
          alertService.sendAccountLockedAlert(username, ip, "连续登录失败次数过多");
        } catch (Exception e) {
          log.warn("发送账户锁定告警失败", e);
        }
      }
      loginLogService.recordLoginFailure(username, ip, userAgent, "连续登录失败次数过多，账户已锁定");
      throw new BusinessException("连续登录失败次数过多，账户已被锁定，请30分钟后重试或联系管理员");
    }

    // 3. IP级别的速率限制（防止同一IP频繁尝试）
    String ipLimitKey = "login:ip:" + ip;
    Integer ipAttempts = (Integer) redisTemplate.opsForValue().get(ipLimitKey);
    if (ipAttempts != null && ipAttempts >= 10) {
      log.warn("IP登录尝试次数过多: ip={}, attempts={}", ip, ipAttempts);
      loginLogService.recordLoginFailure(username, ip, userAgent, "IP登录尝试次数过多");
      // 发送异常IP告警
      try {
        alertService.sendSuspiciousIpAlert(ip, ipAttempts, "短时间内登录尝试次数过多，可能存在暴力破解行为");
      } catch (Exception e) {
        log.warn("发送异常IP告警失败", e);
      }
      throw new BusinessException("登录尝试次数过多，请15分钟后重试");
    }

    try {
      // 4. 认证（Spring Security会调用UserDetailsService）
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, password));

      // 登录成功，清除IP限制计数
      redisTemplate.delete(ipLimitKey);

      // 2. 获取认证后的用户信息
      LoginUser loginUser = (LoginUser) authentication.getPrincipal();

      // 3. 生成Token
      String accessToken = jwtTokenProvider.generateAccessToken(loginUser.getUserId(), username);
      String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser.getUserId(), username);

      // 4. 缓存Token（用于单点登录控制）
      String cacheKey = TOKEN_CACHE_PREFIX + loginUser.getUserId();
      redisTemplate.opsForValue().set(cacheKey, accessToken, TOKEN_CACHE_HOURS, TimeUnit.HOURS);

      // 5. 更新最后登录信息
      updateLoginInfo(loginUser.getUserId(), ip);

      // 6. 记录登录成功日志
      loginLogService.recordLoginSuccess(loginUser.getUserId(), username, ip, userAgent);

      // 7. 创建会话
      String deviceType = userAgentParser.parseDeviceType(userAgent);
      String browser = userAgentParser.parseBrowser(userAgent);
      String os = userAgentParser.parseOS(userAgent);
      sessionAppService.createSession(
          loginUser.getUserId(),
          username,
          accessToken,
          refreshToken,
          ip,
          userAgent,
          deviceType,
          browser,
          os,
          null // location可以通过IP地址解析，这里先留空
          );

      log.info("用户登录成功: {}", username);

      // 8. 返回结果
      return LoginResult.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .expiresIn(TOKEN_EXPIRE_SECONDS)
          .userId(loginUser.getUserId())
          .username(loginUser.getUsername())
          .realName(loginUser.getRealName())
          .roles(loginUser.getRoles())
          .permissions(loginUser.getPermissions())
          .build();

    } catch (BadCredentialsException e) {
      log.warn("登录失败，用户名或密码错误: username={}, ip={}", username, ip);

      // 增加IP尝试次数
      String ipLimitKeyForBadCreds = "login:ip:" + ip;
      Integer currentAttempts = (Integer) redisTemplate.opsForValue().get(ipLimitKeyForBadCreds);
      if (currentAttempts == null) {
        currentAttempts = 0;
      }
      redisTemplate
          .opsForValue()
          .set(
              ipLimitKeyForBadCreds,
              currentAttempts + 1,
              IP_LOCK_DURATION_MINUTES,
              TimeUnit.MINUTES);

      // 记录登录失败日志
      loginLogService.recordLoginFailure(username, ip, userAgent, "用户名或密码错误");

      // 检查是否需要锁定账户
      if (loginLogService.shouldLockAccount(username)) {
        User lockedUser = userRepository.findByUsername(username).orElse(null);
        if (lockedUser != null) {
          lockedUser.setStatus("LOCKED");
          userRepository.updateById(lockedUser);
          log.warn("账户因连续登录失败被自动锁定: username={}", username);
          // 发送账户锁定告警
          try {
            alertService.sendAccountLockedAlert(username, ip, "连续登录失败次数过多");
          } catch (Exception alertEx) {
            log.warn("发送账户锁定告警失败", alertEx);
          }
        }
        throw new BusinessException("连续登录失败次数过多，账户已被锁定，请30分钟后重试或联系管理员");
      }

      // 登录失败次数较多时发送告警（5次以上）
      int failCount = loginLogService.getRecentFailureCount(username);
      if (failCount >= 3) {
        try {
          alertService.sendLoginFailureAlert(username, ip, failCount);
        } catch (Exception alertEx) {
          log.warn("发送登录失败告警失败", alertEx);
        }
      }

      throw new BusinessException("用户名或密码错误");
    } catch (BusinessException e) {
      // 业务异常（如账户锁定）直接抛出
      throw e;
    } catch (Exception e) {
      log.error("登录异常: username={}, ip={}", username, ip, e);

      // ⚠️ 安全修复：系统异常不增加IP尝试次数，避免DoS攻击
      // 攻击者可能故意发送畸形请求导致异常，从而快速锁定正常IP
      // 只记录日志，不增加计数器

      // 记录登录失败日志（不泄露详细错误信息）
      loginLogService.recordLoginFailure(username, ip, userAgent, "系统异常");
      throw new BusinessException("登录失败，请稍后重试");
    }
  }

  /**
   * 刷新Token
   *
   * <p>安全增强： 1. 检查 refresh token 是否已被使用（防止重放攻击） 2. 使用后将旧 refresh token 标记为已使用 3. 实现 Token 轮换机制
   */
  /**
   * 刷新Token
   *
   * @param refreshToken 刷新令牌
   * @return 登录结果
   */
  public LoginResult refreshToken(final String refreshToken) {
    // 1. 验证refreshToken
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new BusinessException("刷新令牌无效或已过期");
    }

    // 2. 检查 refresh token 是否已被使用（防止重放攻击）
    if (tokenBlacklistService.isRefreshTokenUsed(refreshToken)) {
      log.warn("检测到 Refresh Token 重放攻击！");
      // 可选：失效该用户的所有 token
      Long userIdFromToken = jwtTokenProvider.getUserIdFromToken(refreshToken);
      tokenBlacklistService.invalidateUserTokens(userIdFromToken);
      throw new BusinessException("安全风险：刷新令牌已被使用，请重新登录");
    }

    // 3. 获取用户信息
    Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

    // 4. 查询用户（确保用户仍然有效）
    User user = userRepository.findById(userId);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException("用户不存在或已被禁用");
    }

    // 5. 标记旧 refresh token 为已使用（在生成新 token 之前）
    long remainingSeconds = jwtTokenProvider.getRemainingExpirationSeconds(refreshToken);
    if (!tokenBlacklistService.markRefreshTokenUsed(
        refreshToken, remainingSeconds + TOKEN_REFRESH_BUFFER_SECONDS)) {
      // 并发情况下可能有其他请求已经使用了这个 token
      log.warn("Refresh Token 标记失败，可能存在并发使用");
      throw new BusinessException("刷新令牌已被使用，请重新登录");
    }

    // 6. 生成新Token
    String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username);

    // 7. 更新缓存
    String cacheKey = TOKEN_CACHE_PREFIX + userId;
    redisTemplate.opsForValue().set(cacheKey, newAccessToken, TOKEN_CACHE_HOURS, TimeUnit.HOURS);

    // 8. 查询角色权限
    var roles = new HashSet<>(userRepository.findRoleCodesByUserId(userId));
    var permissions = new HashSet<>(userRepository.findPermissionsByUserId(userId));

    log.info("Token 刷新成功: userId={}", userId);

    return LoginResult.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .expiresIn(TOKEN_EXPIRE_SECONDS)
        .userId(userId)
        .username(username)
        .realName(user.getRealName())
        .roles(roles)
        .permissions(permissions)
        .build();
  }

  /**
   * 登出
   *
   * <p>安全增强： 1. 将 access token 加入黑名单 2. 防止已登出的 token 被继续使用
   *
   * @param userId 用户ID
   * @param token 访问令牌
   */
  public void logout(final Long userId, final String token) {
    // 1. 删除Redis缓存
    String cacheKey = TOKEN_CACHE_PREFIX + userId;
    redisTemplate.delete(cacheKey);

    // 2. 将 token 加入黑名单
    if (token != null && !token.isEmpty()) {
      try {
        long remainingSeconds = jwtTokenProvider.getRemainingExpirationSeconds(token);
        if (remainingSeconds > 0) {
          tokenBlacklistService.addToBlacklist(token, remainingSeconds);
          log.debug("Token 已加入黑名单，剩余有效期: {}秒", remainingSeconds);
        }
      } catch (Exception e) {
        // Token 可能已过期或无效，忽略
        log.debug("无法获取 token 剩余有效期: {}", e.getMessage());
      }
    }

    // 3. 更新会话状态
    if (token != null) {
      sessionAppService.logoutSessionByToken(token);
    }

    // 4. 清除用户认证缓存
    userDetailsService.clearUserAuthCacheByUserId(userId);

    log.info("用户登出: {}", userId);
  }

  /**
   * 强制登出用户（使该用户的所有 token 失效） 用于密码修改、账户锁定等场景
   *
   * @param userId 用户ID
   */
  public void forceLogoutUser(final Long userId) {
    // 1. 失效该用户的所有 token
    tokenBlacklistService.invalidateUserTokens(userId);

    // 2. 删除Redis缓存
    String cacheKey = TOKEN_CACHE_PREFIX + userId;
    redisTemplate.delete(cacheKey);

    // 3. 清除用户认证缓存
    userDetailsService.clearUserAuthCacheByUserId(userId);

    log.info("用户强制登出，所有 Token 已失效: userId={}", userId);
  }

  /**
   * 更新登录信息
   *
   * @param userId 用户ID
   * @param ip IP地址
   */
  private void updateLoginInfo(final Long userId, final String ip) {
    User user = userRepository.findById(userId);
    if (user != null) {
      user.setLastLoginAt(LocalDateTime.now());
      user.setLastLoginIp(ip);
      userRepository.updateById(user);
    }
  }

  /** 登录结果. */
  @lombok.Data
  @lombok.Builder
  public static class LoginResult {
    /** 访问令牌. */
    private String accessToken;

    /** 刷新令牌. */
    private String refreshToken;

    /** 过期时间（秒）. */
    private Long expiresIn;

    /** 用户ID. */
    private Long userId;

    /** 用户名. */
    private String username;

    /** 真实姓名. */
    private String realName;

    /** 角色集合. */
    private java.util.Set<String> roles;

    /** 权限集合. */
    private java.util.Set<String> permissions;
  }
}
