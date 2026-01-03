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

    private static final String TOKEN_CACHE_PREFIX = "token:";
    private static final long TOKEN_CACHE_HOURS = 24;

    /**
     * 用户登录
     */
    public LoginResult login(String username, String password, String ip, String userAgent) {
        try {
            // 1. 认证（Spring Security会调用UserDetailsService）
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

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
            log.warn("登录失败，用户名或密码错误: {}", username);
            // 记录登录失败日志
            loginLogService.recordLoginFailure(username, ip, userAgent, "用户名或密码错误");
            throw new BusinessException("用户名或密码错误");
        } catch (Exception e) {
            log.error("登录异常: {}", e.getMessage(), e);
            // 记录登录失败日志
            loginLogService.recordLoginFailure(username, ip, userAgent, "登录异常: " + e.getMessage());
            throw new BusinessException("登录失败: " + e.getMessage());
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

