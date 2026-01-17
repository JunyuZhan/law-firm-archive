package com.lawfirm.interfaces.rest;

import com.lawfirm.application.system.service.AuthService;
import com.lawfirm.application.system.service.CaptchaService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RateLimiter;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.common.util.IpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 认证 Controller
 */
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    @RateLimiter(key = "captcha", rate = 20, interval = 60, limitType = RateLimiter.LimitType.IP, message = "验证码请求过于频繁")
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
    @RateLimiter(key = "login", rate = 10, interval = 60, limitType = RateLimiter.LimitType.IP, message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                        HttpServletRequest httpRequest) {
        // ✅ 使用 IpUtils 获取真实IP
        String ip = IpUtils.getIpAddr(httpRequest);
        
        // 验证码校验（可选）：如果前端传了验证码则验证，否则跳过
        // 注：前端使用滑动验证，无需图形验证码
        if (request.getCaptchaId() != null && request.getCaptchaCode() != null) {
            boolean verified = captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!verified) {
                log.warn("验证码验证失败: username={}, ip={}", request.getUsername(), ip);
                return Result.error("验证码错误或已过期，请刷新后重试");
            }
        }

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
     * 允许未登录用户调用，避免前端循环重试
     */
    @PostMapping("/logout")
    @OperationLog(module = "认证", action = "用户登出")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        // 尝试获取用户ID，如果未登录则返回null（避免抛出异常）
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 用户未登录，允许继续执行（可能是token已过期的情况）
        }
        
        String token = httpRequest.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 如果用户已登录，执行登出逻辑；如果未登录，直接返回成功（避免前端循环重试）
        if (userId != null) {
            authService.logout(userId, token);
        }
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
