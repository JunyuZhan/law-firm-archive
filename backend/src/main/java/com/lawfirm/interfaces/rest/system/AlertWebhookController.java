package com.lawfirm.interfaces.rest.system;

import com.lawfirm.infrastructure.notification.AlertService;
import com.lawfirm.infrastructure.notification.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Alertmanager Webhook 接口
 * 接收 Prometheus Alertmanager 发送的告警通知
 * 通过系统配置的邮件服务发送告警邮件
 */
@Slf4j
@RestController
@RequestMapping("/system/alert")
@RequiredArgsConstructor
public class AlertWebhookController {

    private final EmailService emailService;
    // AlertService 保留用于将来扩展（如发送到其他通知渠道）
    @SuppressWarnings("unused")
    private final AlertService alertService;

    /**
     * 接收 Alertmanager Webhook 请求
     * Alertmanager 会将告警以 JSON 格式 POST 到此端点
     */
    @PostMapping("/webhook")
    public Map<String, Object> receiveAlert(@RequestBody AlertmanagerPayload payload) {
        log.info("收到 Alertmanager 告警: status={}, alertCount={}", 
                payload.getStatus(), payload.getAlerts() != null ? payload.getAlerts().size() : 0);
        
        if (payload.getAlerts() == null || payload.getAlerts().isEmpty()) {
            return Map.of("status", "ok", "message", "No alerts to process");
        }
        
        int processed = 0;
        int failed = 0;
        
        for (Alert alert : payload.getAlerts()) {
            try {
                processAlert(alert, payload.getStatus());
                processed++;
            } catch (Exception e) {
                log.error("处理告警失败: alertname={}", alert.getLabels().get("alertname"), e);
                failed++;
            }
        }
        
        return Map.of(
                "status", "ok",
                "processed", processed,
                "failed", failed
        );
    }

    /**
     * 处理单个告警
     */
    private void processAlert(Alert alert, String status) {
        String alertname = alert.getLabels().getOrDefault("alertname", "Unknown");
        String severity = alert.getLabels().getOrDefault("severity", "warning");
        String summary = alert.getAnnotations().getOrDefault("summary", alertname);
        
        // 构建邮件内容
        String subject;
        if ("resolved".equals(status)) {
            subject = "[已恢复] " + summary;
        } else {
            subject = "[" + getSeverityLabel(severity) + "] " + summary;
        }
        
        String content = buildAlertEmailContent(alert, status);
        
        // 发送告警邮件
        emailService.sendAlertToAdmins(subject, content);
        
        log.info("告警邮件已发送: alertname={}, severity={}, status={}", alertname, severity, status);
    }

    /**
     * 构建告警邮件内容
     */
    private String buildAlertEmailContent(Alert alert, String status) {
        String alertname = alert.getLabels().getOrDefault("alertname", "Unknown");
        String severity = alert.getLabels().getOrDefault("severity", "warning");
        String summary = alert.getAnnotations().getOrDefault("summary", alertname);
        String description = alert.getAnnotations().getOrDefault("description", "");
        String instance = alert.getLabels().getOrDefault("instance", "-");
        
        boolean isResolved = "resolved".equals(status);
        String statusColor = isResolved ? "#52c41a" : getSeverityColor(severity);
        String statusLabel = isResolved ? "已恢复" : "告警中";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
        html.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>");
        
        // 标题区域
        html.append("<div style='background:").append(statusColor)
                .append(";color:white;padding:15px 20px;border-radius:5px 5px 0 0;'>");
        html.append("<h2 style='margin:0;'>").append(summary).append("</h2>");
        html.append("<span style='font-size:12px;opacity:0.9;'>状态: ").append(statusLabel);
        html.append(" | 级别: ").append(getSeverityLabel(severity)).append("</span>");
        html.append("</div>");
        
        // 内容区域
        html.append("<div style='border:1px solid #e8e8e8;border-top:none;padding:20px;'>");
        
        if (!description.isEmpty()) {
            html.append("<p style='margin-top:0;'>").append(description).append("</p>");
        }
        
        // 详细信息表格
        html.append("<table style='width:100%;border-collapse:collapse;margin:15px 0;'>");
        html.append("<tr><td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;width:120px;'>告警名称</td>");
        html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;'>").append(alertname).append("</td></tr>");
        html.append("<tr><td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;'>实例</td>");
        html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;'>").append(instance).append("</td></tr>");
        html.append("<tr><td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;'>开始时间</td>");
        html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;'>").append(alert.getStartsAt()).append("</td></tr>");
        
        if (isResolved && alert.getEndsAt() != null) {
            html.append("<tr><td style='padding:8px;border-bottom:1px solid #f0f0f0;color:#666;'>恢复时间</td>");
            html.append("<td style='padding:8px;border-bottom:1px solid #f0f0f0;'>").append(alert.getEndsAt()).append("</td></tr>");
        }
        
        html.append("</table>");
        
        // 页脚
        html.append("<div style='margin-top:20px;padding-top:15px;border-top:1px solid #f0f0f0;color:#999;font-size:12px;'>");
        html.append("此邮件由智慧律所管理系统 Prometheus 监控自动发送。");
        html.append("</div>");
        
        html.append("</div></div></body></html>");
        return html.toString();
    }

    private String getSeverityLabel(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "严重";
            case "error" -> "错误";
            case "warning" -> "警告";
            case "info" -> "信息";
            default -> severity;
        };
    }

    private String getSeverityColor(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "#cf1322";
            case "error" -> "#ff4d4f";
            case "warning" -> "#faad14";
            case "info" -> "#1890ff";
            default -> "#666666";
        };
    }

    // ==================== Alertmanager Payload 数据结构 ====================

    @Data
    public static class AlertmanagerPayload {
        private String status;  // firing | resolved
        private String receiver;
        private List<Alert> alerts;
        private Map<String, String> groupLabels;
        private Map<String, String> commonLabels;
        private Map<String, String> commonAnnotations;
        private String externalURL;
    }

    @Data
    public static class Alert {
        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private String startsAt;
        private String endsAt;
        private String generatorURL;
        private String fingerprint;
    }
}
