package com.archivesystem.controller;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.Result;
import com.archivesystem.entity.User;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器.
 */
@Tag(name = "用户管理", description = "用户CRUD、角色分配等接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "创建用户")
    @PostMapping
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
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "分页查询用户")
    @GetMapping
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
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(currentUserId, request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request.getRoleIds());
        return Result.success();
    }

    @Operation(summary = "获取用户角色")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
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
        private String status;
    }

    @Data
    public static class AssignRolesRequest {
        private List<Long> roleIds;
    }
}
