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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心 Controller 用于用户管理自己的信息，不需要管理员权限 */
@Tag(name = "个人中心", description = "用户个人信息管理接口")
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

  /** 用户应用服务 */
  private final UserAppService userAppService;

  /**
   * 获取当前用户信息
   *
   * @return 当前用户信息
   */
  @Operation(summary = "获取当前用户信息")
  @GetMapping("/info")
  public Result<UserDTO> getCurrentUser() {
    Long userId = SecurityUtils.getCurrentUserId();
    UserDTO user = userAppService.getUserById(userId);
    return Result.success(user);
  }

  /**
   * 更新个人信息 注意：姓名和用户名不允许自己修改
   *
   * @param request 更新个人信息请求
   * @return 更新后的用户信息
   */
  @Operation(summary = "更新个人信息")
  @PutMapping("/update")
  @OperationLog(module = "个人中心", action = "更新个人信息")
  public Result<UserDTO> updateProfile(@RequestBody @Valid final UpdateProfileRequest request) {
    Long userId = SecurityUtils.getCurrentUserId();
    UserDTO user =
        userAppService.updateProfile(
            userId, request.getEmail(), request.getPhone(), request.getIntroduction());
    return Result.success(user);
  }

  /**
   * 修改密码
   *
   * @param request 修改密码请求
   * @return 空结果
   */
  @Operation(summary = "修改密码")
  @PostMapping("/change-password")
  @OperationLog(module = "个人中心", action = "修改密码", saveParams = false)
  public Result<Void> changePassword(@RequestBody @Valid final ChangePasswordRequest request) {
    Long userId = SecurityUtils.getCurrentUserId();
    userAppService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
    return Result.success();
  }

  // ========== Request DTOs ==========

  /** 更新个人资料请求 */
  @Data
  public static class UpdateProfileRequest {
    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 个人简介 */
    private String introduction;
  }

  /** 修改密码请求 */
  @Data
  public static class ChangePasswordRequest {
    /** 旧密码 */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
  }
}
