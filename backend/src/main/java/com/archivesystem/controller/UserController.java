package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.User;
import com.archivesystem.security.LoginSecurityService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器.
 * @author junyuzhan
 */
@Tag(name = "用户管理", description = "用户CRUD、角色分配等接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LoginSecurityService loginSecurityService;

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<User> create(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType());
        user.setDepartment(request.getDepartment());

        User created = userService.create(user);
        return Result.success(created);
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType());
        user.setDepartment(request.getDepartment());

        User updated = userService.update(id, user);
        return Result.success(updated);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<PageResult<User>> query(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(userService.query(keyword, userType, status, pageNum, pageSize));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(currentUserId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @Operation(summary = "解锁用户账号", description = "解锁因登录失败次数过多而被锁定的账号")
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Void> unlockAccount(@PathVariable Long id) {
        User user = userService.getById(id);
        loginSecurityService.unlockAccount(user.getUsername());
        return Result.success("账号已解锁", null);
    }

    @Operation(summary = "检查账号锁定状态")
    @GetMapping("/{id}/lock-status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Result<Object> getLockStatus(@PathVariable Long id) {
        User user = userService.getById(id);
        boolean locked = loginSecurityService.isAccountLocked(user.getUsername());
        long remainingTime = loginSecurityService.getRemainingLockoutTime(user.getUsername());
        int remainingAttempts = loginSecurityService.getRemainingAttempts(user.getUsername());
        
        return Result.success(java.util.Map.of(
                "locked", locked,
                "remainingLockoutSeconds", remainingTime,
                "remainingAttempts", remainingAttempts
        ));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public Result<User> getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return Result.success(userService.getById(currentUserId));
    }

    // ========== Request DTOs ==========

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String realName;
        private String email;
        private String phone;
        private String userType;
        private String department;
    }

    @Data
    public static class UpdateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        private String realName;
        private String email;
        private String phone;
        private String userType;
        private String department;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class AssignRolesRequest {
        @NotNull(message = "角色ID列表不能为空")
        private List<Long> roleIds;
    }
}
