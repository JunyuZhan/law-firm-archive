package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.service.UserAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心 Controller
 * 用于用户管理自己的信息，不需要管理员权限
 */
@Tag(name = "个人中心", description = "用户个人信息管理接口")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserAppService userAppService;

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserDTO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserDTO user = userAppService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 更新个人信息
     * 注意：姓名和用户名不允许自己修改
     */
    @Operation(summary = "更新个人信息")
    @PutMapping("/update")
    @OperationLog(module = "个人中心", action = "更新个人信息")
    public Result<UserDTO> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserDTO user = userAppService.updateProfile(
            userId,
            request.getEmail(),
            request.getPhone(),
            request.getIntroduction()
        );
        return Result.success(user);
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    @OperationLog(module = "个人中心", action = "修改密码")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userAppService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class UpdateProfileRequest {
        private String realName;
        private String email;
        private String phone;
        private String introduction;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}

