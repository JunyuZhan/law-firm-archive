package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateUserCommand;
import com.lawfirm.application.system.command.UpdateUserCommand;
import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.dto.UserQueryDTO;
import com.lawfirm.application.system.service.UserAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
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
        Map<String, Object> result = userAppService.importUsers(file);
        return Result.success(result);
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
}
