package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.config.SystemMailSenderFactory;
import com.archivesystem.dto.alert.AlertMessage;
import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.service.AlertService;
import com.archivesystem.service.ConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 告警通知服务实现.
 * 支持邮件（控制台 SMTP / spring.mail + alert.email.*）和钉钉机器人通知.
 *
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private static final String KEY_NOTIFY_EMAIL = "system.notify.email.enabled";
    private static final String KEY_NOTIFY_SYSTEM_EVENTS = "system.notify.system.events.email.enabled";
    private static final String KEY_ADMIN_EXTRA_EMAILS = "system.notify.admin.emails";

    private static final long SYSTEM_EVENT_MAIL_COOLDOWN_MS = 120_000L;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ConfigService configService;
    private final SystemMailSenderFactory systemMailSenderFactory;
    private final UserMapper userMapper;

    @Value("${alert.email.enabled:false}")
    private boolean envEmailEnabled;

    @Value("${alert.email.from:}")
    private String envEmailFrom;

    @Value("${alert.email.to:}")
    private String envEmailTo;

    @Value("${alert.dingtalk.enabled:false}")
    private boolean dingtalkEnabled;

    @Value("${alert.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${alert.dingtalk.secret:}")
    private String dingtalkSecret;

    private JavaMailSender springConfiguredMailSender;

    private final AtomicLong lastSystemEventMailMs = new AtomicLong(0);

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.springConfiguredMailSender = mailSender;
    }

    @Override
    @Async
    public boolean send(AlertMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        List<String> to = resolveMailRecipients();
        return sendInternal(message, to);
    }

    @Override
    @Async
    public boolean send(AlertMessage message, List<String> receivers) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        List<String> to = (receivers != null && !receivers.isEmpty())
                ? normalizeRecipients(receivers, "指定收件人")
                : resolveMailRecipients();
        return sendInternal(message, to);
    }

    private boolean sendInternal(AlertMessage message, List<String> to) {
        boolean success = true;

        if (isNotifyEmailEnabled() && to != null && !to.isEmpty()) {
            try {
                dispatchEmail(message, to);
            } catch (Exception e) {
                log.error("邮件告警发送失败", e);
                success = false;
            }
        }

        if (dingtalkEnabled && dingtalkWebhook != null && !dingtalkWebhook.isBlank()) {
            try {
                sendDingtalk(message);
            } catch (Exception e) {
                log.error("钉钉告警发送失败", e);
                success = false;
            }
        }

        if (!isNotifyEmailEnabled() && !dingtalkEnabled) {
            log.warn("告警通知未启用，仅记录日志: {}", message.toText());
        } else if (isNotifyEmailEnabled() && (to == null || to.isEmpty())) {
            log.warn("邮件通知已启用但无收件人，仅记录日志: {}", message.toText());
        }

        return success;
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
        Map<String, Object> extra = new java.util.HashMap<>();
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

    @Override
    @Async
    public void notifySystemEvent(String title, String detail) {
        if (!isNotifyEmailEnabled() || !configService.getBooleanValue(KEY_NOTIFY_SYSTEM_EVENTS, false)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSystemEventMailMs.get() < SYSTEM_EVENT_MAIL_COOLDOWN_MS) {
            log.debug("系统事件邮件节流，跳过: {}", title);
            return;
        }
        lastSystemEventMailMs.set(now);

        List<String> to = resolveMailRecipients();
        if (to.isEmpty()) {
            log.warn("系统事件邮件未发送（无管理员收件邮箱）: {}", title);
            return;
        }

        AlertMessage message = AlertMessage.builder()
                .level(AlertMessage.Level.ERROR)
                .type(AlertMessage.Type.SYSTEM_EVENT)
                .title(title != null ? title : "系统事件")
                .content(truncate(detail, 4000))
                .createdAt(LocalDateTime.now())
                .build();
        sendInternal(message, to);
    }

    @Override
    public void sendTestMail(String overrideTo) {
        JavaMailSender sender = resolveJavaMailSender();
        if (sender == null) {
            throw new BusinessException("未配置邮件发送：请在「规则与运行参数」中填写 SMTP 服务器，或在部署环境配置 spring.mail.*");
        }
        List<String> to;
        if (StringUtils.hasText(overrideTo)) {
            String normalized = normalizeEmail(overrideTo, "测试收件人");
            if (normalized == null) {
                throw new BusinessException("测试收件人邮箱格式不正确");
            }
            to = List.of(normalized);
        } else {
            to = resolveMailRecipients();
        }
        if (to.isEmpty()) {
            throw new BusinessException("无收件人：请为系统/安全/审计管理员账号维护邮箱，或填写「额外通知邮箱」");
        }
        AlertMessage message = AlertMessage.builder()
                .level(AlertMessage.Level.INFO)
                .type(AlertMessage.Type.SYSTEM_EVENT)
                .title("邮件配置测试")
                .content("这是一封来自档案管理系统的测试邮件。收到即表示 SMTP 与收件人配置可用。")
                .createdAt(LocalDateTime.now())
                .build();
        dispatchEmail(message, to);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "\n…(已截断)";
    }

    private boolean isNotifyEmailEnabled() {
        return envEmailEnabled || configService.getBooleanValue(KEY_NOTIFY_EMAIL, false);
    }

    private JavaMailSender resolveJavaMailSender() {
        JavaMailSender fromDb = systemMailSenderFactory.createFromDatabaseConfig();
        if (fromDb != null) {
            return fromDb;
        }
        return springConfiguredMailSender;
    }

    /**
     * 管理员收件人：环境变量 alert.email.to + 库内三员邮箱 + system.notify.admin.emails.
     */
    private List<String> resolveMailRecipients() {
        Set<String> emails = new LinkedHashSet<>();
        if (StringUtils.hasText(envEmailTo)) {
            for (String part : envEmailTo.split(",")) {
                addEmail(emails, part, "环境通知邮箱");
            }
        }
        String extra = configService.getValue(KEY_ADMIN_EXTRA_EMAILS);
        if (StringUtils.hasText(extra)) {
            for (String part : extra.split(",")) {
                addEmail(emails, part, "额外通知邮箱");
            }
        }
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, User.STATUS_ACTIVE)
                .in(User::getUserType,
                        User.TYPE_SYSTEM_ADMIN,
                        User.TYPE_SECURITY_ADMIN,
                        User.TYPE_AUDIT_ADMIN));
        for (User u : admins) {
            addEmail(emails, u.getEmail(), "管理员邮箱");
        }
        return new ArrayList<>(emails);
    }

    private List<String> normalizeRecipients(List<String> receivers, String sourceName) {
        Set<String> emails = new LinkedHashSet<>();
        for (String receiver : receivers) {
            addEmail(emails, receiver, sourceName);
        }
        return new ArrayList<>(emails);
    }

    private void addEmail(Set<String> sink, String raw, String sourceName) {
        String normalized = normalizeEmail(raw, sourceName);
        if (normalized != null) {
            sink.add(normalized);
        }
    }

    private String normalizeEmail(String raw, String sourceName) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String email = raw.trim();
        if (email.isEmpty()) {
            return null;
        }
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            return address.getAddress();
        } catch (Exception e) {
            log.warn("忽略无效邮箱: source={}, value={}", sourceName, email);
            return null;
        }
    }

    private void dispatchEmail(AlertMessage message, List<String> receivers) {
        JavaMailSender sender = resolveJavaMailSender();
        if (sender == null) {
            log.warn("邮件发送器未配置");
            return;
        }
        String from = resolveFromAddress(sender);
        if (!StringUtils.hasText(from)) {
            log.warn("邮件未发送：发件人地址未配置");
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from.trim());
        mail.setTo(receivers.toArray(new String[0]));
        mail.setSubject("[档案系统] " + message.getTitle());
        mail.setText(message.toText());

        sender.send(mail);
        log.info("邮件已发送: to={}, title={}", receivers, message.getTitle());
    }

    private String resolveFromAddress(JavaMailSender sender) {
        String cf = systemMailSenderFactory.resolveFromAddress();
        if (StringUtils.hasText(cf)) {
            return cf.trim();
        }
        if (StringUtils.hasText(envEmailFrom)) {
            return envEmailFrom.trim();
        }
        if (sender instanceof JavaMailSenderImpl impl && StringUtils.hasText(impl.getUsername())) {
            return impl.getUsername().trim();
        }
        return null;
    }

    private void sendDingtalk(AlertMessage message) {
        try {
            String webhookUrl = buildDingtalkUrl();

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("msgtype", "markdown");

            Map<String, String> markdown = new java.util.HashMap<>();
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

            String separator = dingtalkWebhook.contains("?")
                    ? (dingtalkWebhook.endsWith("?") || dingtalkWebhook.endsWith("&") ? "" : "&")
                    : "?";
            return dingtalkWebhook + separator + "timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            log.error("钉钉签名计算失败", e);
            return dingtalkWebhook;
        }
    }

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
}
