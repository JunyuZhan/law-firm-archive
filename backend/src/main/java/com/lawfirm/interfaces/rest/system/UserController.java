package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateUserCommand;
import com.lawfirm.application.system.command.UpdateUserCommand;
import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.dto.UserQueryDTO;
import com.lawfirm.application.system.service.UserAppService;
import com.lawfirm.application.system.service.UserRoleChangeService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.FileValidator;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用户管理 Controller
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;
    private final UserRoleChangeService userRoleChangeService;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @RequirePermission("sys:user:list")
    public Result<PageResult<UserDTO>> listUsers(UserQueryDTO query) {
        PageResult<UserDTO> result = userAppService.listUsers(query);
        return Result.success(result);
    }

    /**
     * 获取用户选择列表（公共接口，用于选择案源人、负责律师等场景）
     * 所有登录用户都可以访问，无需特殊权限
     */
    @GetMapping("/select-options")
    public Result<PageResult<UserDTO>> getUserSelectOptions(UserQueryDTO query) {
        // 默认只查询激活状态的用户
        if (query.getStatus() == null) {
            query.setStatus("ACTIVE");
        }
        PageResult<UserDTO> result = userAppService.listUsers(query);
        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @RequirePermission("sys:user:list")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        UserDTO user = userAppService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @RequirePermission("sys:user:create")
    @OperationLog(module = "用户管理", action = "创建用户")
    public Result<UserDTO> createUser(@RequestBody @Valid CreateUserCommand command) {
        UserDTO user = userAppService.createUser(command);
        return Result.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping
    @RequirePermission("sys:user:update")
    @OperationLog(module = "用户管理", action = "更新用户")
    public Result<UserDTO> updateUser(@RequestBody @Valid UpdateUserCommand command) {
        UserDTO user = userAppService.updateUser(command);
        return Result.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @RequirePermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userAppService.deleteUser(id);
        return Result.success();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @RequirePermission("sys:user:delete")
    @OperationLog(module = "用户管理", action = "批量删除用户")
    public Result<Void> deleteUsers(@RequestBody @Valid BatchDeleteRequest request) {
        userAppService.deleteUsers(request.getIds());
        return Result.success();
    }

    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    @RequirePermission("sys:user:update")
    @OperationLog(module = "用户管理", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id,
                                       @RequestBody @Valid ResetPasswordRequest request) {
        userAppService.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("sys:user:update")
    @OperationLog(module = "用户管理", action = "修改用户状态")
    public Result<Void> changeStatus(@PathVariable Long id,
                                      @RequestBody @Valid ChangeStatusRequest request) {
        userAppService.changeStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 导出用户列表（M1-016，P2）
     */
    @GetMapping("/export")
    @RequirePermission("sys:user:list")
    @Operation(summary = "导出用户列表", description = "将用户列表导出为Excel文件")
    @OperationLog(module = "用户管理", action = "导出用户")
    public ResponseEntity<InputStreamResource> exportUsers(UserQueryDTO query) throws IOException {
        java.io.InputStream inputStream = userAppService.exportUsers(query);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=用户列表.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 批量导入用户（M1-016，P2）
     */
    @PostMapping("/import")
    @RequirePermission("sys:user:create")
    @Operation(summary = "批量导入用户", description = "从Excel文件批量导入用户信息")
    @OperationLog(module = "用户管理", action = "批量导入用户")
    public Result<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        // ✅ 安全验证：验证上传的Excel文件
        FileValidator.ValidationResult validationResult = FileValidator.validate(file);
        if (!validationResult.isValid()) {
            return Result.error(validationResult.getErrorMessage());
        }
        Map<String, Object> result = userAppService.importUsers(file);
        return Result.success(result);
    }

    /**
     * 检查用户角色变更前的待处理事项
     */
    @GetMapping("/{userId}/role-change-check")
    @RequirePermission("sys:user:list")
    @Operation(summary = "检查角色变更", description = "检查用户角色变更前的待处理业务")
    public Result<UserRoleChangeService.RoleChangeCheckResult> checkRoleChange(
            @PathVariable Long userId,
            @RequestParam List<Long> newRoleIds) {
        UserRoleChangeService.RoleChangeCheckResult result = userRoleChangeService.checkRoleChange(userId, newRoleIds);
        return Result.success(result);
    }

    /**
     * 执行角色变更
     */
    @PostMapping("/{userId}/change-role")
    @RequirePermission("sys:user:list")
    @Operation(summary = "变更用户角色", description = "执行用户角色变更，记录变更历史并清除权限缓存")
    @OperationLog(module = "用户管理", action = "变更用户角色")
    public Result<Void> changeUserRole(
            @PathVariable Long userId,
            @RequestBody @Valid ChangeRoleRequest request) {
        userRoleChangeService.changeUserRole(userId, request.getRoleIds(), request.getReason());
        return Result.success();
    }

    /**
     * 清除用户权限缓存
     */
    @PostMapping("/{userId}/clear-cache")
    @RequirePermission("sys:user:list")
    @Operation(summary = "清除权限缓存", description = "清除用户的Token和权限缓存，强制用户重新登录")
    @OperationLog(module = "用户管理", action = "清除权限缓存")
    public Result<Void> clearUserPermissionCache(@PathVariable Long userId) {
        userRoleChangeService.clearUserPermissionCache(userId);
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class BatchDeleteRequest {
        @NotEmpty(message = "用户ID列表不能为空")
        private List<Long> ids;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    @Data
    public static class ChangeStatusRequest {
        @NotBlank(message = "状态不能为空")
        private String status;
    }

    @Data
    public static class ChangeRoleRequest {
        @NotEmpty(message = "角色ID列表不能为空")
        private List<Long> roleIds;
        
        private String reason;
    }
}
