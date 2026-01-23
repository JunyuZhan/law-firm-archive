package com.lawfirm.infrastructure.notification;

import com.lawfirm.common.util.VersionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系统告警服务
 * 用于发送各类告警通知（入侵检测、系统故障、运行异常等）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 告警级别
     */
    public enum AlertLevel {
        INFO("信息", "#1890ff"),
        WARNING("警告", "#faad14"),
        ERROR("错误", "#ff4d4f"),
        CRITICAL("严重", "#cf1322");

        private final String name;
        private final String color;

        AlertLevel(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() { return name; }
        public String getColor() { return color; }
    }

    /**
     * 发送登录失败告警（暴力破解检测）
     */
    public void sendLoginFailureAlert(String username, String ip, int failCount) {
        String subject = "[安全告警] 检测到登录异常 - " + username;
        String content = buildAlertHtml(
                AlertLevel.WARNING,
                "登录失败告警",
                String.format("检测到用户 <strong>%s</strong> 在短时间内登录失败 <strong>%d</strong> 次，可能存在暴力破解行为。", 
                        username, failCount),
                new String[][]{
                        {"用户名", username},
                        {"来源IP", ip},
                        {"失败次数", String.valueOf(failCount)},
                        {"告警时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "建议检查该账户是否安全，必要时临时锁定账户或封禁IP。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送账户锁定告警
     */
    public void sendAccountLockedAlert(String username, String ip, String reason) {
        String subject = "[安全告警] 账户已被锁定 - " + username;
        String content = buildAlertHtml(
                AlertLevel.ERROR,
                "账户锁定告警",
                String.format("账户 <strong>%s</strong> 已被系统自动锁定。", username),
                new String[][]{
                        {"用户名", username},
                        {"来源IP", ip},
                        {"锁定原因", reason},
                        {"锁定时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "该账户需要管理员手动解锁后才能登录。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送异地登录告警
     */
    public void sendNewLocationLoginAlert(String username, String ip, String location) {
        String subject = "[安全告警] 检测到异地登录 - " + username;
        String content = buildAlertHtml(
                AlertLevel.WARNING,
                "异地登录告警",
                String.format("检测到用户 <strong>%s</strong> 从新位置登录。", username),
                new String[][]{
                        {"用户名", username},
                        {"来源IP", ip},
                        {"登录位置", location},
                        {"检测时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "该用户需要输入管理员许可码才能登录。如非本人操作，请检查账户安全。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送异地登录成功告警（许可码验证通过）
     */
    public void sendNewLocationLoginSuccessAlert(String username, String ip, String location) {
        String subject = "[安全通知] 异地登录成功 - " + username;
        String content = buildAlertHtml(
                AlertLevel.INFO,
                "异地登录成功通知",
                String.format("用户 <strong>%s</strong> 已通过许可码验证，从新位置成功登录。", username),
                new String[][]{
                        {"用户名", username},
                        {"来源IP", ip},
                        {"登录位置", location},
                        {"登录时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "该位置已自动添加为用户的常用登录地点。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送异常IP访问告警
     */
    public void sendSuspiciousIpAlert(String ip, int requestCount, String details) {
        String subject = "[安全告警] 检测到异常IP访问 - " + ip;
        String content = buildAlertHtml(
                AlertLevel.WARNING,
                "异常IP访问告警",
                String.format("检测到来自IP <strong>%s</strong> 的异常访问行为。", ip),
                new String[][]{
                        {"来源IP", ip},
                        {"请求次数", String.valueOf(requestCount)},
                        {"异常详情", details},
                        {"检测时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "建议检查该IP的访问日志，必要时添加IP黑名单。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送系统错误告警
     */
    public void sendSystemErrorAlert(String errorType, String errorMessage, String stackTrace) {
        String subject = "[系统告警] 系统错误 - " + errorType;
        String content = buildAlertHtml(
                AlertLevel.ERROR,
                "系统错误告警",
                String.format("系统发生错误: <strong>%s</strong>", errorType),
                new String[][]{
                        {"错误类型", errorType},
                        {"错误信息", errorMessage},
                        {"发生时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "<pre style='background:#f5f5f5;padding:10px;overflow:auto;max-height:300px;'>" + 
                        escapeHtml(stackTrace) + "</pre>"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送数据库连接告警
     */
    public void sendDatabaseAlert(String message) {
        String subject = "[系统告警] 数据库连接异常";
        String content = buildAlertHtml(
                AlertLevel.CRITICAL,
                "数据库告警",
                "检测到数据库连接异常，请立即检查！",
                new String[][]{
                        {"告警信息", message},
                        {"告警时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "请立即检查数据库服务状态和网络连接。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送磁盘空间告警
     */
    public void sendDiskSpaceAlert(String path, long usedPercent, long freeSpaceMB) {
        AlertLevel level = usedPercent > 90 ? AlertLevel.CRITICAL : AlertLevel.WARNING;
        String subject = "[系统告警] 磁盘空间不足 - " + usedPercent + "%";
        String content = buildAlertHtml(
                level,
                "磁盘空间告警",
                String.format("磁盘空间使用率已达 <strong>%d%%</strong>，请及时清理。", usedPercent),
                new String[][]{
                        {"路径", path},
                        {"使用率", usedPercent + "%"},
                        {"剩余空间", freeSpaceMB + " MB"},
                        {"检测时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "建议清理日志文件、临时文件或扩展存储空间。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送备份失败告警
     */
    public void sendBackupFailureAlert(String backupType, String errorMessage) {
        String subject = "[系统告警] 备份失败 - " + backupType;
        String content = buildAlertHtml(
                AlertLevel.ERROR,
                "备份失败告警",
                String.format("<strong>%s</strong> 备份失败，请检查。", backupType),
                new String[][]{
                        {"备份类型", backupType},
                        {"错误信息", errorMessage},
                        {"失败时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                "请检查备份配置和存储空间，并手动执行备份。"
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送服务启动通知
     */
    public void sendServiceStartNotification() {
        if (!emailService.isEnabled()) {
            return;
        }
        
        VersionUtils.VersionInfo versionInfo = VersionUtils.getVersionInfo();
        String subject = "[系统通知] 智慧律所系统已启动";
        String content = buildAlertHtml(
                AlertLevel.INFO,
                "服务启动通知",
                "智慧律所管理系统已成功启动。",
                new String[][]{
                        {"系统版本", versionInfo.getVersion()},
                        {"构建时间", versionInfo.getBuildTime()},
                        {"Git提交", versionInfo.getGitCommit()},
                        {"启动时间", LocalDateTime.now().format(DATE_FORMAT)},
                        {"Java版本", System.getProperty("java.version")},
                        {"操作系统", System.getProperty("os.name")}
                },
                null
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 发送维护模式通知
     */
    public void sendMaintenanceModeNotification(boolean enabled, String message) {
        String subject = "[系统通知] 维护模式" + (enabled ? "已开启" : "已关闭");
        String content = buildAlertHtml(
                enabled ? AlertLevel.WARNING : AlertLevel.INFO,
                "维护模式通知",
                enabled ? "系统已进入维护模式，非管理员用户无法访问。" : "系统已退出维护模式，恢复正常访问。",
                new String[][]{
                        {"状态", enabled ? "维护中" : "正常运行"},
                        {"维护信息", message != null ? message : "-"},
                        {"操作时间", LocalDateTime.now().format(DATE_FORMAT)}
                },
                null
        );
        emailService.sendAlertToAdmins(subject, content);
    }

    /**
     * 构建告警邮件HTML内容
     */
    private String buildAlertHtml(AlertLevel level, String title, String summary, 
                                   String[][] details, String additionalInfo) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        html.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>");
        
        // 标题区域
        html.append("<div style='background:").append(level.getColor())
                .append(";color:white;padding:15px 20px;border-radius:5px 5px 0 0;'>");
        html.append("<h2 style='margin:0;'>").append(title).append("</h2>");
        html.append("<span style='font-size:12px;opacity:0.9;'>级别: ").append(level.getName()).append("</span>");
        html.append("</div>");
        
        // 内容区域
        html.append("<div style='border:1px solid #e8e8e8;border-top:none;padding:20px;'>");
        html.append("<p style='margin-top:0;'>").append(summary).append("</p>");
        
        // 详细信息表格
        if (details != null && details.length > 0) {
            html.append("<table style='width:100%;border-collapse:collapse;margin:15px 0;'>");
            for (String[] row : details) {
                html.append("<tr>");
                html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;width:120px;'>")
                        .append(row[0]).append("</td>");
                html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;'>")
                        .append(row[1]).append("</td>");
                html.append("</tr>");
            }
            html.append("</table>");
        }
        
        // 附加信息
        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            html.append("<div style='background:#f9f9f9;padding:10px;border-radius:5px;margin-top:15px;'>");
            html.append(additionalInfo);
            html.append("</div>");
        }
        
        // 页脚
        html.append("<div style='margin-top:20px;padding-top:15px;border-top:1px solid #f0f0f0;color:#999;font-size:12px;'>");
        html.append("此邮件由智慧律所管理系统自动发送，请勿回复。");
        html.append("</div>");
        
        html.append("</div></div></body></html>");
        return html.toString();
    }

    /**
     * HTML转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

