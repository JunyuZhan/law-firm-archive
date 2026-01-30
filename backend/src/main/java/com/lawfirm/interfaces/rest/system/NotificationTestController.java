package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.notification.AlertService;
import com.lawfirm.infrastructure.notification.EmailService;
import com.lawfirm.infrastructure.notification.SystemReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通知测试接口 用于测试邮件配置和告警功能 */
@Tag(name = "通知测试")
@RestController
@RequestMapping("/system/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {

  /** 磁盘使用率阈值：85% */
  private static final int DISK_USAGE_THRESHOLD = 85;

  /** 磁盘总空间（MB）：15000MB */
  private static final long DISK_TOTAL_SPACE_MB = 15000;

  /** 邮件服务 */
  private final EmailService emailService;

  /** 告警服务 */
  private final AlertService alertService;

  /** 系统报告服务 */
  private final SystemReportService systemReportService;

  /**
   * 测试邮件配置
   *
   * @param params 参数，包含email字段
   * @return 测试结果
   */
  @Operation(summary = "测试邮件配置")
  @PostMapping("/test-email")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "通知测试", action = "测试邮件配置")
  public Result<String> testEmailConfig(@RequestBody final Map<String, String> params) {
    String testEmail = params.get("email");
    if (testEmail == null || testEmail.isEmpty()) {
      return Result.error("请提供测试邮箱地址");
    }

    try {
      boolean success = emailService.testConnection(testEmail);
      if (success) {
        return Result.success("测试邮件已发送到 " + testEmail + "，请查收");
      } else {
        return Result.error("邮件发送失败，请检查 SMTP 配置");
      }
    } catch (Exception e) {
      log.error("测试邮件发送失败", e);
      return Result.error("邮件发送失败: " + e.getMessage());
    }
  }

  /**
   * 检查邮件服务状态
   *
   * @return 邮件服务状态
   */
  @Operation(summary = "检查邮件服务状态")
  @GetMapping("/email-status")
  @RequirePermission("sys:config:list")
  public Result<Map<String, Object>> getEmailStatus() {
    Map<String, Object> status = new java.util.HashMap<>();
    status.put("enabled", emailService.isEnabled());
    return Result.success(status);
  }

  /**
   * 发送测试告警
   *
   * @param params 参数，包含type字段（login/locked/disk/backup）
   * @return 发送结果
   */
  @Operation(summary = "发送测试告警")
  @PostMapping("/test-alert")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "通知测试", action = "发送测试告警")
  public Result<String> sendTestAlert(@RequestBody final Map<String, String> params) {
    String alertType = params.getOrDefault("type", "info");

    try {
      switch (alertType) {
        case "login":
          alertService.sendLoginFailureAlert("测试用户", "127.0.0.1", 5);
          break;
        case "locked":
          alertService.sendAccountLockedAlert("测试用户", "127.0.0.1", "连续登录失败5次");
          break;
        case "disk":
          alertService.sendDiskSpaceAlert("/", DISK_USAGE_THRESHOLD, DISK_TOTAL_SPACE_MB);
          break;
        case "backup":
          alertService.sendBackupFailureAlert("数据库备份", "测试告警：备份连接超时");
          break;
        default:
          alertService.sendServiceStartNotification();
      }
      return Result.success("测试告警已发送");
    } catch (Exception e) {
      log.error("发送测试告警失败", e);
      return Result.error("告警发送失败: " + e.getMessage());
    }
  }

  /**
   * 生成系统报告预览
   *
   * @param type 报告类型（daily/weekly）
   * @return 报告内容
   */
  @Operation(summary = "生成系统报告预览")
  @GetMapping("/report-preview")
  @RequirePermission("sys:config:list")
  public Result<String> previewSystemReport(
      @RequestParam(defaultValue = "daily") final String type) {
    try {
      String report;
      if ("weekly".equals(type)) {
        report = systemReportService.generateWeeklyReport();
      } else {
        report = systemReportService.generateDailyReport();
      }
      return Result.success(report);
    } catch (Exception e) {
      log.error("生成系统报告预览失败", e);
      return Result.error("生成报告失败: " + e.getMessage());
    }
  }

  /**
   * 立即发送系统报告
   *
   * @param params 参数，包含type字段（daily/weekly）
   * @return 发送结果
   */
  @Operation(summary = "立即发送系统报告")
  @PostMapping("/send-report")
  @RequirePermission("sys:config:update")
  @OperationLog(module = "通知测试", action = "发送系统报告")
  public Result<String> sendSystemReport(@RequestBody final Map<String, String> params) {
    String type = params.getOrDefault("type", "daily");

    try {
      if ("weekly".equals(type)) {
        systemReportService.sendWeeklyReport();
      } else {
        systemReportService.sendDailyReport();
      }
      return Result.success("系统报告已发送");
    } catch (Exception e) {
      log.error("发送系统报告失败", e);
      return Result.error("报告发送失败: " + e.getMessage());
    }
  }
}
