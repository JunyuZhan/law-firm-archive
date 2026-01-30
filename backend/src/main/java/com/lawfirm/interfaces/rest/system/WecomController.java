package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.UserWecom;
import com.lawfirm.infrastructure.notification.WecomNotificationChannel;
import com.lawfirm.infrastructure.persistence.mapper.UserWecomMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 企业微信配置控制器 */
@Tag(name = "企业微信配置", description = "企业微信绑定与测试")
@RestController
@RequestMapping("/system/wecom")
@RequiredArgsConstructor
public class WecomController {

  /** 企业微信用户Mapper */
  private final UserWecomMapper userWecomMapper;

  /** 企业微信通知渠道 */
  @Autowired(required = false)
  private WecomNotificationChannel wecomChannel;

  /**
   * 获取当前用户的企业微信绑定信息
   *
   * @return 企业微信绑定信息
   */
  @Operation(summary = "获取当前用户的企业微信绑定信息")
  @GetMapping("/my")
  @PreAuthorize("isAuthenticated()")
  public Result<UserWecom> getMyBinding() {
    Long userId = SecurityUtils.getUserId();
    UserWecom binding = userWecomMapper.selectByUserId(userId);
    return Result.success(binding);
  }

  /**
   * 绑定企业微信
   *
   * @param command 绑定企业微信命令
   * @return 企业微信绑定信息
   */
  @Operation(summary = "绑定企业微信")
  @PostMapping("/bind")
  @PreAuthorize("isAuthenticated()")
  public Result<UserWecom> bindWecom(@RequestBody final BindWecomCommand command) {
    Long userId = SecurityUtils.getUserId();

    UserWecom binding =
        UserWecom.builder()
            .userId(userId)
            .wecomUserid(command.getWecomUserid())
            .wecomMobile(command.getWecomMobile())
            .enabled(true)
            .build();

    userWecomMapper.insertOrUpdate(binding);

    return Result.success(userWecomMapper.selectByUserId(userId));
  }

  /**
   * 解绑企业微信
   *
   * @return 空结果
   */
  @Operation(summary = "解绑企业微信")
  @PostMapping("/unbind")
  @PreAuthorize("isAuthenticated()")
  public Result<Void> unbindWecom() {
    Long userId = SecurityUtils.getUserId();
    UserWecom binding = userWecomMapper.selectByUserId(userId);

    if (binding != null) {
      binding.setEnabled(false);
      binding.setWecomUserid(null);
      binding.setWecomMobile(null);
      userWecomMapper.insertOrUpdate(binding);
    }

    return Result.success();
  }

  /**
   * 测试企业微信机器人连接
   *
   * @return 测试结果
   */
  @Operation(summary = "测试企业微信机器人连接")
  @PostMapping("/test")
  @PreAuthorize("hasAuthority('system:config:manage')")
  public Result<Map<String, Object>> testWecomBot() {
    Map<String, Object> result = new HashMap<>();

    if (wecomChannel == null) {
      result.put("success", false);
      result.put("message", "企业微信通知渠道未初始化");
      return Result.success(result);
    }

    boolean success = wecomChannel.testConnection();
    result.put("success", success);
    result.put("message", success ? "连接成功！测试消息已发送到群聊。" : "连接失败，请检查Webhook地址是否正确。");

    return Result.success(result);
  }

  /**
   * 获取企业微信推送状态
   *
   * @return 企业微信推送状态
   */
  @Operation(summary = "获取企业微信推送状态")
  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  public Result<Map<String, Object>> getWecomStatus() {
    Map<String, Object> result = new HashMap<>();
    result.put("enabled", wecomChannel != null && wecomChannel.isEnabled());
    return Result.success(result);
  }

  /** 绑定企业微信命令 */
  @Data
  public static class BindWecomCommand {
    /** 企业微信用户ID. */
    private String wecomUserid;

    /** 企业微信手机号. */
    private String wecomMobile;
  }
}
