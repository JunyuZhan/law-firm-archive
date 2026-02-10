package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.CreateUserCommand;
import com.lawfirm.application.system.command.UpdateUserCommand;
import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.dto.UserQueryDTO;
import com.lawfirm.application.system.service.LoginLockService;
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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 用户管理 Controller */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

  /** 用户应用服务 */
  private final UserAppService userAppService;

  /** 用户角色变更服务 */
  private final UserRoleChangeService userRoleChangeService;

  /** 登录锁定服务 */
  private final LoginLockService loginLockService;

  /**
   * 分页查询用户列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("sys:user:list")
  public Result<PageResult<UserDTO>> listUsers(final UserQueryDTO query) {
    PageResult<UserDTO> result = userAppService.listUsers(query);
    return Result.success(result);
  }

  /**
   * 获取用户选择列表（公共接口，用于选择案源人、负责律师等场景） 所有登录用户都可以访问，无需特殊权限
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/select-options")
  public Result<PageResult<UserDTO>> getUserSelectOptions(final UserQueryDTO query) {
    // 默认只查询激活状态的用户
    if (query.getStatus() == null) {
      query.setStatus("ACTIVE");
    }
    PageResult<UserDTO> result = userAppService.listUsers(query);
    return Result.success(result);
  }

  /**
   * 获取用户详情
   *
   * @param id 用户ID
   * @return 用户详情
   */
  @GetMapping("/{id}")
  @RequirePermission("sys:user:list")
  public Result<UserDTO> getUser(@PathVariable final Long id) {
    UserDTO user = userAppService.getUserById(id);
    return Result.success(user);
  }

  /**
   * 创建用户
   *
   * @param command 创建命令
   * @return 用户详情
   */
  @PostMapping
  @RequirePermission("sys:user:create")
  @OperationLog(module = "用户管理", action = "创建用户")
  public Result<UserDTO> createUser(@RequestBody @Valid final CreateUserCommand command) {
    UserDTO user = userAppService.createUser(command);
    return Result.success(user);
  }

  /**
   * 更新用户
   *
   * @param command 更新命令
   * @return 用户详情
   */
  @PutMapping
  @RequirePermission("sys:user:update")
  @OperationLog(module = "用户管理", action = "更新用户")
  public Result<UserDTO> updateUser(@RequestBody @Valid final UpdateUserCommand command) {
    UserDTO user = userAppService.updateUser(command);
    return Result.success(user);
  }

  /**
   * 删除用户
   *
   * @param id 用户ID
   * @return 无返回
   */
  @DeleteMapping("/{id}")
  @RequirePermission("sys:user:delete")
  @OperationLog(module = "用户管理", action = "删除用户")
  public Result<Void> deleteUser(@PathVariable final Long id) {
    userAppService.deleteUser(id);
    return Result.success();
  }

  /**
   * 批量删除用户
   *
   * @param request 批量删除请求
   * @return 无返回
   */
  @DeleteMapping("/batch")
  @RequirePermission("sys:user:delete")
  @OperationLog(module = "用户管理", action = "批量删除用户")
  public Result<Void> deleteUsers(@RequestBody @Valid final BatchDeleteRequest request) {
    userAppService.deleteUsers(request.getIds());
    return Result.success();
  }

  /**
   * 重置密码
   *
   * @param id 用户ID
   * @param request 重置密码请求
   * @return 无返回
   */
  @PostMapping("/{id}/reset-password")
  @RequirePermission("sys:user:update")
  @OperationLog(module = "用户管理", action = "重置密码", saveParams = false)
  public Result<Void> resetPassword(
      @PathVariable final Long id, @RequestBody @Valid final ResetPasswordRequest request) {
    userAppService.resetPassword(id, request.getNewPassword());
    return Result.success();
  }

  /**
   * 解锁用户账户（清除登录失败记录和锁定状态）
   *
   * @param id 用户ID
   * @return 无返回
   */
  @PostMapping("/{id}/unlock")
  @RequirePermission("sys:user:update")
  @OperationLog(module = "用户管理", action = "解锁用户账户")
  public Result<Void> unlockUser(@PathVariable final Long id) {
    // 获取用户信息
    UserDTO user = userAppService.getUserById(id);
    if (user == null) {
      return Result.error("用户不存在");
    }

    // 解锁账户（清除失败次数和锁定状态）
    loginLockService.unlockAccount(user.getUsername());
    return Result.success();
  }

  /**
   * 获取用户登录锁定状态
   *
   * @param id 用户ID
   * @return 锁定状态
   */
  @GetMapping("/{id}/lock-status")
  @RequirePermission("sys:user:list")
  public Result<LoginLockService.LockStatus> getLockStatus(@PathVariable final Long id) {
    UserDTO user = userAppService.getUserById(id);
    if (user == null) {
      return Result.error("用户不存在");
    }

    LoginLockService.LockStatus status = loginLockService.checkLockStatus(user.getUsername());
    return Result.success(status);
  }

  /**
   * 修改用户状态
   *
   * @param id 用户ID
   * @param request 状态修改请求
   * @return 无返回
   */
  @PutMapping("/{id}/status")
  @RequirePermission("sys:user:update")
  @OperationLog(module = "用户管理", action = "修改用户状态")
  public Result<Void> changeStatus(
      @PathVariable final Long id, @RequestBody @Valid final ChangeStatusRequest request) {
    userAppService.changeStatus(id, request.getStatus());
    return Result.success();
  }

  /**
   * 导出用户列表（M1-016，P2）
   *
   * @param query 查询条件
   * @return Excel文件流
   * @throws IOException IO异常
   */
  @GetMapping("/export")
  @RequirePermission("sys:user:list")
  @Operation(summary = "导出用户列表", description = "将用户列表导出为Excel文件")
  @OperationLog(module = "用户管理", action = "导出用户")
  public ResponseEntity<InputStreamResource> exportUsers(final UserQueryDTO query)
      throws IOException {
    java.io.InputStream inputStream = userAppService.exportUsers(query);
    InputStreamResource resource = new InputStreamResource(inputStream);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=用户列表.xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  /**
   * 批量导入用户（M1-016，P2）
   *
   * @param file 导入文件
   * @return 导入结果
   * @throws IOException IO异常
   */
  @PostMapping("/import")
  @RequirePermission("sys:user:create")
  @Operation(summary = "批量导入用户", description = "从Excel文件批量导入用户信息")
  @OperationLog(module = "用户管理", action = "批量导入用户")
  public Result<Map<String, Object>> importUsers(@RequestParam("file") final MultipartFile file)
      throws IOException {
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
   *
   * @param userId 用户ID
   * @param newRoleIds 新角色ID列表
   * @return 检查结果
   */
  @GetMapping("/{userId}/role-change-check")
  @RequirePermission("sys:user:list")
  @Operation(summary = "检查角色变更", description = "检查用户角色变更前的待处理业务")
  public Result<UserRoleChangeService.RoleChangeCheckResult> checkRoleChange(
      @PathVariable final Long userId, @RequestParam final List<Long> newRoleIds) {
    UserRoleChangeService.RoleChangeCheckResult result =
        userRoleChangeService.checkRoleChange(userId, newRoleIds);
    return Result.success(result);
  }

  /**
   * 执行角色变更
   *
   * @param userId 用户ID
   * @param request 变更请求
   * @return 无返回
   */
  @PostMapping("/{userId}/change-role")
  @RequirePermission("sys:user:list")
  @Operation(summary = "变更用户角色", description = "执行用户角色变更，记录变更历史并清除权限缓存")
  @OperationLog(module = "用户管理", action = "变更用户角色")
  public Result<Void> changeUserRole(
      @PathVariable final Long userId, @RequestBody @Valid final ChangeRoleRequest request) {
    userRoleChangeService.changeUserRole(userId, request.getRoleIds(), request.getReason());
    return Result.success();
  }

  /**
   * 清除用户权限缓存
   *
   * @param userId 用户ID
   * @return 无返回
   */
  @PostMapping("/{userId}/clear-cache")
  @RequirePermission("sys:user:list")
  @Operation(summary = "清除权限缓存", description = "清除用户的Token和权限缓存，强制用户重新登录")
  @OperationLog(module = "用户管理", action = "清除权限缓存")
  public Result<Void> clearUserPermissionCache(@PathVariable final Long userId) {
    userRoleChangeService.clearUserPermissionCache(userId);
    return Result.success();
  }

  // ========== Request DTOs ==========

  /** 批量删除请求 */
  @Data
  public static class BatchDeleteRequest {
    /** 用户ID列表 */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> ids;
  }

  /** 重置密码请求 */
  @Data
  public static class ResetPasswordRequest {
    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
  }

  /** 变更状态请求 */
  @Data
  public static class ChangeStatusRequest {
    /** 状态 */
    @NotBlank(message = "状态不能为空")
    private String status;
  }

  /** 变更角色请求 */
  @Data
  public static class ChangeRoleRequest {
    /** 角色ID列表 */
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;

    /** 变更原因 */
    private String reason;
  }
}
