package com.lawfirm.application.system.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.security.LoginUser;
import com.lawfirm.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LoginLogService loginLogService;
    private final SessionAppService sessionAppService;
    private final com.lawfirm.application.system.util.UserAgentParser userAgentParser;
    private final com.lawfirm.infrastructure.notification.AlertService alertService;

    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final long TOKEN_CACHE_HOURS = 24;

    /**
     * 用户登录
     */
    public LoginResult login(String username, String password, String ip, String userAgent) {
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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
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
                    .expiresIn(86400L)
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
            redisTemplate.opsForValue().set(ipLimitKeyForBadCreds, currentAttempts + 1, 15, TimeUnit.MINUTES);
            
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
     */
    public LoginResult refreshToken(String refreshToken) {
        // 1. 验证refreshToken
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效或已过期");
        }

        // 2. 获取用户信息
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 3. 查询用户（确保用户仍然有效）
        User user = userRepository.findById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 4. 生成新Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, username);

        // 5. 更新缓存
        String cacheKey = TOKEN_CACHE_PREFIX + userId;
        redisTemplate.opsForValue().set(cacheKey, newAccessToken, TOKEN_CACHE_HOURS, TimeUnit.HOURS);

        // 6. 查询角色权限
        var roles = new HashSet<>(userRepository.findRoleCodesByUserId(userId));
        var permissions = new HashSet<>(userRepository.findPermissionsByUserId(userId));

        return LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(86400L)
                .userId(userId)
                .username(username)
                .realName(user.getRealName())
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /**
     * 登出
     */
    public void logout(Long userId, String token) {
        // 1. 删除Redis缓存
        String cacheKey = TOKEN_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);

        // 2. 更新会话状态
        if (token != null) {
            sessionAppService.logoutSessionByToken(token);
        }

        log.info("用户登出: {}", userId);
    }

    /**
     * 更新登录信息
     */
    private void updateLoginInfo(Long userId, String ip) {
        User user = userRepository.findById(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            userRepository.updateById(user);
        }
    }

    /**
     * 登录结果
     */
    @lombok.Data
    @lombok.Builder
    public static class LoginResult {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
        private Long userId;
        private String username;
        private String realName;
        private java.util.Set<String> roles;
        private java.util.Set<String> permissions;
    }
}

