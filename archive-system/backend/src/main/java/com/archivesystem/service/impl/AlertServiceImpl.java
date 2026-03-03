package com.archivesystem.service.impl;

import com.archivesystem.dto.alert.AlertMessage;
import com.archivesystem.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警通知服务实现.
 * 支持邮件和钉钉机器人通知.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private JavaMailSender mailSender;

    @Value("${alert.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${alert.email.from:}")
    private String emailFrom;

    @Value("${alert.email.to:}")
    private String emailTo;

    @Value("${alert.dingtalk.enabled:false}")
    private boolean dingtalkEnabled;

    @Value("${alert.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${alert.dingtalk.secret:}")
    private String dingtalkSecret;

    @Override
    @Async
    public boolean send(AlertMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }

        boolean success = true;

        // 发送邮件
        if (emailEnabled && emailTo != null && !emailTo.isBlank()) {
            try {
                sendEmail(message, List.of(emailTo.split(",")));
            } catch (Exception e) {
                log.error("邮件告警发送失败", e);
                success = false;
            }
        }

        // 发送钉钉
        if (dingtalkEnabled && dingtalkWebhook != null && !dingtalkWebhook.isBlank()) {
            try {
                sendDingtalk(message);
            } catch (Exception e) {
                log.error("钉钉告警发送失败", e);
                success = false;
            }
        }

        // 如果都没启用，至少记录日志
        if (!emailEnabled && !dingtalkEnabled) {
            log.warn("告警通知未配置，仅记录日志: {}", message.toText());
        }

        return success;
    }

    @Override
    @Async
    public boolean send(AlertMessage message, List<String> receivers) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }

        // 发送邮件到指定接收者
        if (emailEnabled && receivers != null && !receivers.isEmpty()) {
            try {
                sendEmail(message, receivers);
                return true;
            } catch (Exception e) {
                log.error("邮件告警发送失败", e);
                return false;
            }
        }

        return send(message);
    }

    @Override
    public void alertArchiveProcessFailed(Long archiveId, String archiveNo, String reason) {
        AlertMessage message = AlertMessage.builder()
                .level(AlertMessage.Level.ERROR)
                .type(AlertMessage.Type.ARCHIVE_PROCESS_FAILED)
                .title("档案处理失败")
                .content(reason)
                .archiveId(archiveId)
                .archiveNo(archiveNo)
                .createdAt(LocalDateTime.now())
                .build();
        send(message);
    }

    @Override
    public void alertArchiveExpiring(Long archiveId, String archiveNo, int daysUntilExpire) {
        AlertMessage.Level level = daysUntilExpire <= 7 
                ? AlertMessage.Level.WARNING 
                : AlertMessage.Level.INFO;
        
        AlertMessage message = AlertMessage.builder()
                .level(level)
                .type(AlertMessage.Type.ARCHIVE_EXPIRING)
                .title("档案即将过期")
                .content(String.format("档案将于 %d 天后过期，请及时处理", daysUntilExpire))
                .archiveId(archiveId)
                .archiveNo(archiveNo)
                .createdAt(LocalDateTime.now())
                .build();
        send(message);
    }

    @Override
    public void alertDeadLetter(Long recordId, String sourceType, String sourceId, String error) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("recordId", recordId);
        extra.put("sourceType", sourceType);
        extra.put("sourceId", sourceId);

        AlertMessage message = AlertMessage.builder()
                .level(AlertMessage.Level.ERROR)
                .type(AlertMessage.Type.DEAD_LETTER)
                .title("消息处理失败")
                .content(error)
                .extra(extra)
                .createdAt(LocalDateTime.now())
                .build();
        send(message);
    }

    @Override
    public void alertSystemError(String error) {
        AlertMessage message = AlertMessage.builder()
                .level(AlertMessage.Level.CRITICAL)
                .type(AlertMessage.Type.SYSTEM_ERROR)
                .title("系统错误")
                .content(error)
                .createdAt(LocalDateTime.now())
                .build();
        send(message);
    }

    /**
     * 发送邮件.
     */
    private void sendEmail(AlertMessage message, List<String> receivers) {
        if (mailSender == null) {
            log.warn("邮件发送器未配置");
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(emailFrom);
        mail.setTo(receivers.toArray(new String[0]));
        mail.setSubject("[档案系统告警] " + message.getTitle());
        mail.setText(message.toText());

        mailSender.send(mail);
        log.info("邮件告警已发送: to={}, title={}", receivers, message.getTitle());
    }

    /**
     * 发送钉钉消息.
     */
    private void sendDingtalk(AlertMessage message) {
        try {
            String webhookUrl = buildDingtalkUrl();
            
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "markdown");
            
            Map<String, String> markdown = new HashMap<>();
            markdown.put("title", message.getTitle());
            markdown.put("text", buildDingtalkMarkdown(message));
            body.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            restTemplate.postForObject(webhookUrl, request, String.class);
            log.info("钉钉告警已发送: title={}", message.getTitle());
            
        } catch (Exception e) {
            log.error("钉钉告警发送失败", e);
        }
    }

    /**
     * 构建钉钉 Webhook URL（如果配置了密钥，需要签名）.
     */
    private String buildDingtalkUrl() {
        if (dingtalkSecret == null || dingtalkSecret.isBlank()) {
            return dingtalkWebhook;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + dingtalkSecret;
            
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    dingtalkSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(
                    stringToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String sign = java.net.URLEncoder.encode(
                    java.util.Base64.getEncoder().encodeToString(signData), 
                    java.nio.charset.StandardCharsets.UTF_8);
            
            return dingtalkWebhook + "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("钉钉签名计算失败", e);
            return dingtalkWebhook;
        }
    }

    /**
     * 构建钉钉 Markdown 内容.
     */
    private String buildDingtalkMarkdown(AlertMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(getLevelEmoji(message.getLevel())).append(" ").append(message.getTitle()).append("\n\n");
        sb.append("> **时间**: ").append(message.getCreatedAt()).append("\n\n");
        
        if (message.getArchiveNo() != null) {
            sb.append("> **档案号**: ").append(message.getArchiveNo()).append("\n\n");
        }
        
        sb.append("**详情**: ").append(message.getContent()).append("\n");
        
        return sb.toString();
    }

    private String getLevelEmoji(AlertMessage.Level level) {
        return switch (level) {
            case INFO -> "ℹ️";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
            case CRITICAL -> "🔴";
        };
    }

    /**
     * 注入邮件发送器（可选依赖）.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
}
