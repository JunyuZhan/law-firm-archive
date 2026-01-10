package com.lawfirm.infrastructure.notification;

import com.lawfirm.application.system.service.SysConfigAppService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 邮件发送服务
 * 支持从数据库配置动态读取 SMTP 设置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SysConfigAppService configAppService;

    /**
     * 检查邮件服务是否启用
     */
    public boolean isEnabled() {
        String enabled = configAppService.getConfigValue("notification.email.enabled");
        return "true".equalsIgnoreCase(enabled);
    }

    /**
     * 获取动态配置的邮件发送器
     */
    private JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        String host = configAppService.getConfigValue("notification.email.smtp.host");
        String portStr = configAppService.getConfigValue("notification.email.smtp.port");
        String username = configAppService.getConfigValue("notification.email.smtp.username");
        String password = configAppService.getConfigValue("notification.email.smtp.password");
        
        if (host == null || host.isEmpty()) {
            throw new IllegalStateException("邮件服务器地址未配置");
        }
        
        mailSender.setHost(host);
        mailSender.setPort(portStr != null ? Integer.parseInt(portStr) : 25);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        
        // SSL 配置（如果端口是 465）
        int port = mailSender.getPort();
        if (port == 465) {
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        return mailSender;
    }

    /**
     * 发送简单文本邮件
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!isEnabled()) {
            log.warn("邮件服务未启用，跳过发送: to={}, subject={}", to, subject);
            return;
        }
        
        try {
            JavaMailSender mailSender = getMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            String from = configAppService.getConfigValue("notification.email.smtp.username");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送 HTML 格式邮件
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!isEnabled()) {
            log.warn("邮件服务未启用，跳过发送: to={}, subject={}", to, subject);
            return;
        }
        
        try {
            JavaMailSender mailSender = getMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            
            String from = configAppService.getConfigValue("notification.email.smtp.username");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步发送告警邮件
     */
    @Async
    public void sendAlertEmailAsync(String to, String subject, String content) {
        try {
            sendHtmlEmail(to, subject, content);
        } catch (Exception e) {
            log.error("异步发送告警邮件失败", e);
        }
    }

    /**
     * 发送告警邮件给所有管理员
     */
    public void sendAlertToAdmins(String subject, String content) {
        String adminEmails = configAppService.getConfigValue("notification.email.admin.recipients");
        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn("未配置管理员邮箱，无法发送告警: subject={}", subject);
            return;
        }
        
        String[] emails = adminEmails.split(",");
        for (String email : emails) {
            String trimmedEmail = email.trim();
            if (!trimmedEmail.isEmpty()) {
                sendAlertEmailAsync(trimmedEmail, subject, content);
            }
        }
    }

    /**
     * 测试邮件配置
     */
    public boolean testConnection(String testEmail) {
        try {
            sendSimpleEmail(testEmail, 
                    "[智慧律所] 邮件配置测试", 
                    "这是一封测试邮件，如果您收到此邮件，说明 SMTP 配置正确。\n\n系统时间: " + java.time.LocalDateTime.now());
            return true;
        } catch (Exception e) {
            log.error("邮件配置测试失败", e);
            return false;
        }
    }
}

