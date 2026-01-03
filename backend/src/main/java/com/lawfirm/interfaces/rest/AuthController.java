package com.lawfirm.interfaces.rest;

import com.lawfirm.application.system.service.AuthService;
import com.lawfirm.application.system.service.CaptchaService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 认证 Controller
 */
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaService.CaptchaResult> getCaptcha() {
        CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
        return Result.success(result);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    @OperationLog(module = "认证", action = "用户登录", saveResult = false)
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                        HttpServletRequest httpRequest) {
        // 验证验证码
        if (request.getCaptchaId() != null && request.getCaptchaCode() != null) {
            boolean verified = captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!verified) {
                return Result.error("验证码错误或已过期");
            }
        }

        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthService.LoginResult result = authService.login(
                request.getUsername(),
                request.getPassword(),
                ip,
                userAgent
        );

        LoginResponse response = new LoginResponse();
        response.setAccessToken(result.getAccessToken());
        response.setRefreshToken(result.getRefreshToken());
        response.setExpiresIn(result.getExpiresIn());
        response.setUserId(result.getUserId());
        response.setUsername(result.getUsername());
        response.setRealName(result.getRealName());
        response.setRoles(result.getRoles());
        response.setPermissions(result.getPermissions());

        return Result.success(response);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<LoginResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        AuthService.LoginResult result = authService.refreshToken(request.getRefreshToken());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(result.getAccessToken());
        response.setRefreshToken(result.getRefreshToken());
        response.setExpiresIn(result.getExpiresIn());
        response.setUserId(result.getUserId());
        response.setUsername(result.getUsername());
        response.setRealName(result.getRealName());
        response.setRoles(result.getRoles());
        response.setPermissions(result.getPermissions());

        return Result.success(response);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @OperationLog(module = "认证", action = "用户登出")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.getUserId();
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(userId, token);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserInfoResponse> getCurrentUser() {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(SecurityUtils.getUserId());
        response.setUsername(SecurityUtils.getUsername());
        response.setRealName(SecurityUtils.getRealName());
        response.setRoles(SecurityUtils.getRoles());
        response.setPermissions(SecurityUtils.getPermissions());
        response.setDepartmentId(SecurityUtils.getDepartmentId());
        response.setCompensationType(SecurityUtils.getCompensationType());
        return Result.success(response);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ========== Request/Response DTOs ==========

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String captchaId; // 验证码ID
        private String captchaCode; // 验证码
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;
    }

    @Data
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
        private Long userId;
        private String username;
        private String realName;
        private Set<String> roles;
        private Set<String> permissions;
    }

    @Data
    public static class UserInfoResponse {
        private Long userId;
        private String username;
        private String realName;
        private Long departmentId;
        private String compensationType;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
