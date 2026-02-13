package com.archivesystem.controller;

import com.archivesystem.common.Result;
import com.archivesystem.dto.auth.LoginRequest;
import com.archivesystem.dto.auth.LoginResponse;
import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.security.JwtUtils;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    /**
     * 用户登录.
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 生成Token
            String accessToken = jwtUtils.generateAccessToken(
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getUserType()
            );
            String refreshToken = jwtUtils.generateRefreshToken(userDetails.getId());

            // 更新最后登录时间
            User user = userMapper.selectById(userDetails.getId());
            user.setLastLoginAt(LocalDateTime.now());
            userMapper.updateById(user);

            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(86400L) // 24小时
                    .userId(userDetails.getId())
                    .username(userDetails.getUsername())
                    .realName(userDetails.getRealName())
                    .userType(userDetails.getUserType())
                    .build();

            log.info("用户登录成功: {}", request.getUsername());
            return Result.success("登录成功", response);

        } catch (DisabledException e) {
            log.warn("用户已禁用: {}", request.getUsername());
            return Result.error("1003", "用户已被禁用");
        } catch (BadCredentialsException e) {
            log.warn("登录失败，密码错误: {}", request.getUsername());
            return Result.error("1004", "用户名或密码错误");
        }
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
     * 登出（客户端清除Token即可）.
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        // JWT是无状态的，登出只需要客户端清除Token
        // 如果需要服务端控制，可以维护Token黑名单
        return Result.success("登出成功", null);
    }
}
