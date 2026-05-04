package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.dto.auth.LoginRequest;
import com.archivesystem.dto.auth.LoginResponse;
import com.archivesystem.entity.User;
import com.archivesystem.security.JwtUtils;
import com.archivesystem.security.LoginSecurityService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.security.TokenBlacklistService;
import com.archivesystem.security.UserDetailsImpl;
import com.archivesystem.security.UserRoleUtils;
import com.archivesystem.service.UserService;
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

import io.jsonwebtoken.Claims;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 认证控制器.
 * @author junyuzhan
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录、登出、刷新令牌等")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final LoginSecurityService loginSecurityService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户登录.
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!userService.isSystemInitialized()) {
            return Result.error("1007", "系统尚未初始化，请先设置管理员密码");
        }

        String username = request.getUsername();
        String clientIp = ClientIpUtils.resolve(httpRequest);
        
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
            String effectiveUserType = UserRoleUtils.normalize(userDetails.getUserType());
            // 登录成功，清除失败记录
            loginSecurityService.clearFailedAttempts(username);

            // 生成Token（JWT 内 userType 与接口返回一致，均为归一化后的产品角色）
            String accessToken = jwtUtils.generateAccessToken(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    effectiveUserType
            );
            String refreshToken = jwtUtils.generateRefreshToken(userDetails.getId());

            userService.recordLoginSuccess(userDetails.getId(), clientIp);

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L) // 24小时
                    .userId(userDetails.getId())
                    .username(userDetails.getUsername())
                    .realName(userDetails.getRealName())
                    .userType(effectiveUserType)
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
            log.warn("登录失败: username={}, ip={}, 连续失败次数={}", username, clientIp, failedCount);
            
            if (remaining > 0) {
                return Result.error("1004", String.format("用户名或密码错误，还剩%d次尝试机会", remaining));
            } else {
                return Result.error("1006", "账号因多次登录失败已被锁定，请30分钟后重试");
            }
        }
    }
    
    /**
     * 刷新令牌.
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<LoginResponse> refresh(@RequestBody String refreshToken) {
        try {
            refreshToken = normalizeToken(refreshToken);
            if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
                return Result.error("1001", "无效的刷新令牌");
            }

            Claims claims = jwtUtils.parseToken(refreshToken);
            Long userId = claims.get("userId", Long.class);
            long issuedAt = claims.getIssuedAt().getTime();
            if (tokenBlacklistService.isBlacklisted(refreshToken)
                    || tokenBlacklistService.isUserBlacklisted(userId, issuedAt)) {
                return Result.error("1001", "无效的刷新令牌");
            }

            User user = userService.getActiveById(userId);
            
            if (user == null) {
                return Result.error("1003", "用户不存在或已被禁用");
            }

            String effectiveUserType = UserRoleUtils.normalize(user.getUserType());
            String newAccessToken = jwtUtils.generateAccessToken(
                    user.getId(),
                    user.getUsername(),
                    effectiveUserType
            );
            String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());
            tokenBlacklistService.addToBlacklist(refreshToken);

            LoginResponse response = LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(86400L)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .realName(user.getRealName())
                    .userType(effectiveUserType)
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

        String effectiveUserType = UserRoleUtils.normalize(userDetails.getUserType());
        LoginResponse response = LoginResponse.builder()
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .userType(effectiveUserType)
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
            try {
                Claims claims = jwtUtils.parseToken(token);
                Long userId = claims.get("userId", Long.class);
                tokenBlacklistService.blacklistUserTokens(
                        userId,
                        Math.max(1, Math.ceilDiv(jwtUtils.getRefreshExpirationMillis(), 1000))
                );
                log.info("用户登出，用户令牌已全部吊销: userId={}", userId);
            } catch (Exception e) {
                log.warn("用户登出时无法解析Token，仅吊销当前Token");
            }
        }
        return Result.success("登出成功", null);
    }

    @GetMapping("/bootstrap/status")
    @Operation(summary = "获取初始化状态")
    public Result<BootstrapStatusResponse> getBootstrapStatus() {
        boolean initialized = userService.isSystemInitialized();
        return Result.success(BootstrapStatusResponse.builder()
                .initialized(initialized)
                .build());
    }

    @PostMapping("/bootstrap/initialize")
    @Operation(summary = "首次初始化管理员密码")
    public Result<Void> initializeBootstrap(@Valid @RequestBody BootstrapInitializeRequest request,
                                            HttpServletRequest httpRequest) {
        if (!isBootstrapRequestAllowed(httpRequest)) {
            return Result.error("1008", "首次初始化仅允许从本机或内网访问");
        }
        userService.initializeSystemAdmin(request.getPassword());
        return Result.success("初始化成功，请使用 admin 登录", null);
    }

    private String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        token = token.trim();
        if (token.length() >= 2 && token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1).trim();
        }
        return token;
    }

    private boolean isBootstrapRequestAllowed(HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(clientIp.trim());
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()) {
                return true;
            }
            String hostAddress = address.getHostAddress().toLowerCase();
            return hostAddress.startsWith("fc") || hostAddress.startsWith("fd");
        } catch (UnknownHostException ex) {
            log.warn("首次初始化来源IP解析失败: ip={}", clientIp);
            return false;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BootstrapStatusResponse {
        private boolean initialized;
    }

    @lombok.Data
    public static class BootstrapInitializeRequest {
        @jakarta.validation.constraints.NotBlank(message = "密码不能为空")
        private String password;
    }
}
