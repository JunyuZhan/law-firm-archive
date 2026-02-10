package com.lawfirm.infrastructure.notification;

import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.util.VersionUtils;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 系统运行报告服务.
 *
 * <p>定期发送系统运行状态报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemReportService {

  /** 邮件服务 */
  private final EmailService emailService;

  /** 告警服务 */
  private final AlertService alertService;

  /** 系统配置服务 */
  private final SysConfigAppService configAppService;

  /** JDBC模板 */
  private final JdbcTemplate jdbcTemplate;

  /** 日期时间格式化器 */
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** 日期格式化器 */
  private static final DateTimeFormatter DATE_ONLY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /** 磁盘空间告警阈值 */
  private static final long DISK_ALERT_THRESHOLD = 80;

  /** 内存使用告警阈值 */
  private static final int MEMORY_ALERT_THRESHOLD = 90;

  /** 字节转MB的除数 */
  private static final long BYTES_TO_MB = 1024 * 1024;

  /** 字节转GB的除数 */
  private static final long BYTES_TO_GB = 1024 * 1024 * 1024;

  /** 百分比计算基数 */
  private static final int PERCENTAGE_BASE = 100;

  /** 小时数（一天） */
  private static final int HOURS_PER_DAY = 24;

  /** 每天早上8点发送日报. */
  @Scheduled(cron = "0 0 8 * * ?")
  public void sendDailyReport() {
    String enabled = configAppService.getConfigValue("notification.report.daily.enabled");
    if (!"true".equalsIgnoreCase(enabled)) {
      log.debug("每日报告未启用");
      return;
    }

    log.info("开始生成每日系统报告...");
    try {
      String report = generateDailyReport();
      String subject = "[系统日报] 智慧律所运行报告 - " + LocalDateTime.now().format(DATE_ONLY_FORMAT);
      emailService.sendAlertToAdmins(subject, report);
      log.info("每日系统报告发送成功");
    } catch (Exception e) {
      log.error("发送每日系统报告失败", e);
    }
  }

  /** 每周一早上9点发送周报. */
  @Scheduled(cron = "0 0 9 ? * MON")
  public void sendWeeklyReport() {
    String enabled = configAppService.getConfigValue("notification.report.weekly.enabled");
    if (!"true".equalsIgnoreCase(enabled)) {
      log.debug("每周报告未启用");
      return;
    }

    log.info("开始生成每周系统报告...");
    try {
      String report = generateWeeklyReport();
      String subject = "[系统周报] 智慧律所运行报告 - " + LocalDateTime.now().format(DATE_ONLY_FORMAT);
      emailService.sendAlertToAdmins(subject, report);
      log.info("每周系统报告发送成功");
    } catch (Exception e) {
      log.error("发送每周系统报告失败", e);
    }
  }

  /** 每小时检查系统健康状态 */
  @Scheduled(cron = "0 0 * * * ?")
  public void checkSystemHealth() {
    log.debug("检查系统健康状态...");

    // 检查磁盘空间
    checkDiskSpace();

    // 检查内存使用
    checkMemoryUsage();
  }

  /**
   * 生成每日报告
   *
   * @return HTML格式的报告内容
   */
  public String generateDailyReport() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime yesterday = now.minusDays(1);

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
    html.append("<div style='font-family:Arial,sans-serif;max-width:700px;margin:0 auto;'>");

    // 标题
    html.append(
        "<div style='background:#1890ff;color:white;padding:20px;border-radius:5px 5px 0 0;text-align:center;'>");
    html.append("<h1 style='margin:0;'>📊 系统运行日报</h1>");
    html.append("<p style='margin:5px 0 0;opacity:0.9;'>")
        .append(now.format(DATE_ONLY_FORMAT))
        .append("</p>");
    html.append("</div>");

    html.append("<div style='border:1px solid #e8e8e8;border-top:none;padding:20px;'>");

    // 系统信息
    html.append(buildSection("🖥️ 系统信息", getSystemInfo()));

    // 运行状态
    html.append(buildSection("⚙️ 运行状态", getRunningStatus()));

    // 登录统计
    html.append(buildSection("🔐 登录统计 (24小时)", getLoginStats(yesterday, now)));

    // 操作统计
    html.append(buildSection("📝 操作统计 (24小时)", getOperationStats(yesterday, now)));

    // 存储状态
    html.append(buildSection("💾 存储状态", getStorageStats()));

    // 页脚
    html.append(
        "<div style='margin-top:20px;padding-top:15px;border-top:1px solid #f0f0f0;"
            + "color:#999;font-size:12px;text-align:center;'>");
    html.append("此报告由智慧律所管理系统自动生成<br>");
    html.append("生成时间: ").append(now.format(DATE_FORMAT));
    html.append("</div>");

    html.append("</div></div></body></html>");
    return html.toString();
  }

  /**
   * 生成每周报告
   *
   * @return HTML格式的报告内容
   */
  public String generateWeeklyReport() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime weekAgo = now.minusDays(7);

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
    html.append("<div style='font-family:Arial,sans-serif;max-width:700px;margin:0 auto;'>");

    // 标题
    html.append(
        "<div style='background:#722ed1;color:white;padding:20px;border-radius:5px 5px 0 0;text-align:center;'>");
    html.append("<h1 style='margin:0;'>📊 系统运行周报</h1>");
    html.append("<p style='margin:5px 0 0;opacity:0.9;'>")
        .append(weekAgo.format(DATE_ONLY_FORMAT))
        .append(" ~ ")
        .append(now.format(DATE_ONLY_FORMAT))
        .append("</p>");
    html.append("</div>");

    html.append("<div style='border:1px solid #e8e8e8;border-top:none;padding:20px;'>");

    // 系统信息
    html.append(buildSection("🖥️ 系统信息", getSystemInfo()));

    // 登录统计
    html.append(buildSection("🔐 登录统计 (本周)", getLoginStats(weekAgo, now)));

    // 操作统计
    html.append(buildSection("📝 操作统计 (本周)", getOperationStats(weekAgo, now)));

    // 存储状态
    html.append(buildSection("💾 存储状态", getStorageStats()));

    // 页脚
    html.append(
        "<div style='margin-top:20px;padding-top:15px;border-top:1px solid #f0f0f0;"
            + "color:#999;font-size:12px;text-align:center;'>");
    html.append("此报告由智慧律所管理系统自动生成<br>");
    html.append("生成时间: ").append(now.format(DATE_FORMAT));
    html.append("</div>");

    html.append("</div></div></body></html>");
    return html.toString();
  }

  /**
   * 构建报告区块
   *
   * @param title 区块标题
   * @param data 数据数组
   * @return HTML字符串
   */
  private String buildSection(final String title, final String[][] data) {
    StringBuilder html = new StringBuilder();
    html.append("<div style='margin-bottom:20px;'>");
    html.append("<h3 style='color:#333;border-bottom:2px solid #1890ff;padding-bottom:5px;'>")
        .append(title)
        .append("</h3>");
    html.append("<table style='width:100%;border-collapse:collapse;'>");
    for (String[] row : data) {
      html.append("<tr>");
      html.append(
              "<td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;width:150px;'>")
          .append(row[0])
          .append("</td>");
      html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;font-weight:500;'>")
          .append(row[1])
          .append("</td>");
      html.append("</tr>");
    }
    html.append("</table></div>");
    return html.toString();
  }

  /**
   * 获取系统信息.
   *
   * @return 系统信息数组
   */
  private String[][] getSystemInfo() {
    VersionUtils.VersionInfo versionInfo = VersionUtils.getVersionInfo();
    String dbVersion = configAppService.getConfigValue("sys.version");

    return new String[][] {
      {"系统版本", dbVersion != null ? dbVersion : versionInfo.getVersion()},
      {"构建版本", versionInfo.getVersion()},
      {"Git提交", versionInfo.getGitCommit()},
      {"Java版本", System.getProperty("java.version")},
      {"操作系统", System.getProperty("os.name") + " " + System.getProperty("os.version")}
    };
  }

  /**
   * 获取运行状态.
   *
   * @return 运行状态数组
   */
  private String[][] getRunningStatus() {
    RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    long uptime = runtimeMXBean.getUptime();
    Duration duration = Duration.ofMillis(uptime);

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed() / BYTES_TO_MB;
    long heapMax = memoryMXBean.getHeapMemoryUsage().getMax() / BYTES_TO_MB;
    // 防止除零（heapMax 为 -1 表示未定义，为 0 表示无限制或错误）
    int heapPercent = heapMax > 0 ? (int) (heapUsed * PERCENTAGE_BASE / heapMax) : 0;

    return new String[][] {
      {"运行时长", formatDuration(duration)},
      {"堆内存使用", heapUsed + " MB / " + (heapMax > 0 ? heapMax + " MB" : "未限制") + " (" + heapPercent + "%)"},
      {"线程数", String.valueOf(Thread.activeCount())},
      {"处理器核心", String.valueOf(Runtime.getRuntime().availableProcessors())}
    };
  }

  /**
   * 获取登录统计
   *
   * @param from 开始时间
   * @param to 结束时间
   * @return 统计数据
   */
  private String[][] getLoginStats(final LocalDateTime from, final LocalDateTime to) {
    try {
      Integer successCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM sys_login_log WHERE login_time >= ? AND login_time < ? AND status = 'SUCCESS'",
              Integer.class,
              from,
              to);
      Integer failCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM sys_login_log WHERE login_time >= ? AND login_time < ? AND status = 'FAILURE'",
              Integer.class,
              from,
              to);
      Integer lockedCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM sys_login_log WHERE login_time >= ? "
                  + "AND login_time < ? AND failure_reason LIKE '%锁定%'",
              Integer.class, from, to);
      Integer distinctUsers =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(DISTINCT username) FROM sys_login_log "
                  + "WHERE login_time >= ? AND login_time < ? AND status = 'SUCCESS'",
              Integer.class,
              from,
              to);

      return new String[][] {
        {"登录成功", successCount != null ? successCount + " 次" : "0 次"},
        {"登录失败", failCount != null ? failCount + " 次" : "0 次"},
        {"账户锁定", lockedCount != null ? lockedCount + " 次" : "0 次"},
        {"活跃用户", distinctUsers != null ? distinctUsers + " 人" : "0 人"}
      };
    } catch (Exception e) {
      log.warn("获取登录统计失败", e);
      return new String[][] {{"统计数据", "暂无数据"}};
    }
  }

  /**
   * 获取操作统计
   *
   * @param from 开始时间
   * @param to 结束时间
   * @return 统计数据
   */
  private String[][] getOperationStats(final LocalDateTime from, final LocalDateTime to) {
    try {
      Integer totalOps =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM sys_operation_log WHERE operation_time >= ? AND operation_time < ?",
              Integer.class,
              from,
              to);
      Integer errorOps =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM sys_operation_log "
                  + "WHERE operation_time >= ? AND operation_time < ? AND status = 'FAILURE'",
              Integer.class,
              from,
              to);

      return new String[][] {
        {"操作总数", totalOps != null ? totalOps + " 次" : "0 次"},
        {"操作失败", errorOps != null ? errorOps + " 次" : "0 次"},
        {
          "成功率",
          totalOps != null && totalOps > 0
              ? String.format(
                  "%.2f%%", (totalOps - (errorOps != null ? errorOps : 0)) * 100.0 / totalOps)
              : "100%"
        }
      };
    } catch (Exception e) {
      log.warn("获取操作统计失败", e);
      return new String[][] {{"统计数据", "暂无数据"}};
    }
  }

  /**
   * 获取存储统计.
   *
   * @return 存储统计数组
   */
  private String[][] getStorageStats() {
    File root = new File("/");
    long totalSpace = root.getTotalSpace() / BYTES_TO_GB;
    long freeSpace = root.getFreeSpace() / BYTES_TO_GB;
    long usedSpace = totalSpace - freeSpace;
    int usedPercent = totalSpace > 0 ? (int) (usedSpace * PERCENTAGE_BASE / totalSpace) : 0;

    return new String[][] {
      {"磁盘总空间", totalSpace + " GB"},
      {"已使用", usedSpace + " GB (" + usedPercent + "%)"},
      {"剩余空间", freeSpace + " GB"}
    };
  }

  /** 检查磁盘空间. */
  private void checkDiskSpace() {
    File root = new File("/");
    long totalSpace = root.getTotalSpace();
    long freeSpace = root.getFreeSpace();
    long usedPercent = totalSpace > 0 ? (totalSpace - freeSpace) * PERCENTAGE_BASE / totalSpace : 0;

    if (usedPercent > DISK_ALERT_THRESHOLD) {
      log.warn("磁盘空间使用率过高: {}%", usedPercent);
      alertService.sendDiskSpaceAlert("/", usedPercent, freeSpace / BYTES_TO_MB);
    }
  }

  /** 检查内存使用. */
  private void checkMemoryUsage() {
    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
    long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
    // 防止除零（heapMax 为 -1 表示未定义，为 0 表示无限制）
    if (heapMax <= 0) {
      log.debug("JVM 堆内存上限未定义，跳过内存使用率检查");
      return;
    }
    int usedPercent = (int) (heapUsed * PERCENTAGE_BASE / heapMax);

    if (usedPercent > MEMORY_ALERT_THRESHOLD) {
      log.warn("JVM 堆内存使用率过高: {}%", usedPercent);
      // 可以在这里添加内存告警
    }
  }

  /**
   * 格式化时长.
   *
   * @param duration 时长
   * @return 格式化后的字符串
   */
  private String formatDuration(final Duration duration) {
    long days = duration.toDays();
    long hours = duration.toHours() % HOURS_PER_DAY;
    final int minutesPerHour = 60;
    long minutes = duration.toMinutes() % minutesPerHour;

    if (days > 0) {
      return String.format("%d天 %d小时 %d分钟", days, hours, minutes);
    } else if (hours > 0) {
      return String.format("%d小时 %d分钟", hours, minutes);
    } else {
      return String.format("%d分钟", minutes);
    }
  }
}
