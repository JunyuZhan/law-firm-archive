package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.auth.LoginRequest;
import com.archivesystem.dto.auth.LoginResponse;
import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.security.JwtUtils;
import com.archivesystem.security.LoginSecurityService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.security.TokenBlacklistService;
import com.archivesystem.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 认证控制器.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、登出、刷新令牌等")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final LoginSecurityService loginSecurityService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户登录.
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String clientIp = getClientIp(httpRequest);
        
        // 检查IP是否被锁定
        if (loginSecurityService.isIpLocked(clientIp)) {
            log.warn("IP被锁定，拒绝登录: ip={}", clientIp);
            return Result.error("1005", "当前IP因多次登录失败已被临时锁定，请稍后重试");
        }
        
        // 检查账号是否被锁定
        if (loginSecurityService.isAccountLocked(username)) {
            long remainingSeconds = loginSecurityService.getRemainingLockoutTime(username);
            long remainingMinutes = (remainingSeconds + 59) / 60;
            log.warn("账号被锁定，拒绝登录: username={}, remainingMinutes={}", username, remainingMinutes);
            return Result.error("1006", String.format("账号因多次登录失败已被锁定，请%d分钟后重试", remainingMinutes));
        }
        
        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            // 登录成功，清除失败记录
            loginSecurityService.clearFailedAttempts(username);

            // 生成Token
            String accessToken = jwtUtils.generateAccessToken(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUserType()
            );
            String refreshToken = jwtUtils.generateRefreshToken(userDetails.getId());

            // 更新最后登录时间和IP
            User user = userMapper.selectById(userDetails.getId());
            if (user != null) {
                user.setLastLoginAt(LocalDateTime.now());
                user.setLastLoginIp(clientIp);
                userMapper.updateById(user);
            } else {
                log.warn("登录成功但用户信息未找到: userId={}", userDetails.getId());
            }

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L) // 24小时
                    .userId(userDetails.getId())
                    .username(userDetails.getUsername())
                    .realName(userDetails.getRealName())
                    .userType(userDetails.getUserType())
                    .build();

            log.info("用户登录成功: username={}, ip={}", username, clientIp);
            return Result.success("登录成功", response);

        } catch (DisabledException e) {
            log.warn("用户已禁用: {}", username);
            return Result.error("1003", "用户已被禁用");
        } catch (BadCredentialsException e) {
            // 记录失败尝试
            int failedCount = loginSecurityService.recordFailedAttempt(username, clientIp);
            int remaining = loginSecurityService.getRemainingAttempts(username);
            
            if (remaining > 0) {
                return Result.error("1004", String.format("用户名或密码错误，还剩%d次尝试机会", remaining));
            } else {
                return Result.error("1006", "账号因多次登录失败已被锁定，请30分钟后重试");
            }
        }
    }
    
    /**
     * 获取客户端IP.
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int index = ip.indexOf(',');
                return index != -1 ? ip.substring(0, index).trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 刷新令牌.
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<LoginResponse> refresh(@RequestBody String refreshToken) {
        try {
            if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
                return Result.error("1001", "无效的刷新令牌");
            }

            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            User user = userMapper.selectById(userId);
            
            if (user == null || !User.STATUS_ACTIVE.equals(user.getStatus())) {
                return Result.error("1003", "用户不存在或已被禁用");
            }

            String newAccessToken = jwtUtils.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    user.getUserType()
            );

            LoginResponse response = LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // 刷新令牌不变
                    .expiresIn(86400L)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .userType(user.getUserType())
                    .build();

            return Result.success(response);

        } catch (Exception e) {
            log.error("刷新令牌失败", e);
            return Result.error("1002", "令牌已过期，请重新登录");
        }
    }

    /**
     * 获取当前用户信息.
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<LoginResponse> getCurrentUser() {
        UserDetailsImpl userDetails = SecurityUtils.getCurrentUser();
        if (userDetails == null) {
            return Result.error("401", "未登录");
        }

        LoginResponse response = LoginResponse.builder()
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .userType(userDetails.getUserType())
                .build();

        return Result.success(response);
    }

    /**
     * 用户登出.
     * 将当前Token加入黑名单
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.addToBlacklist(token);
            log.info("用户登出，Token已加入黑名单");
        }
        return Result.success("登出成功", null);
    }
}
