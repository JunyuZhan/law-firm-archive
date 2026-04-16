package com.archivesystem.config;

import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 根据 sys_config 中的 SMTP 项构建 {@link JavaMailSender}（与 spring.mail 环境配置二选一或并存）.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemMailSenderFactory {

    public static final String KEY_SMTP_HOST = "system.mail.smtp.host";
    public static final String KEY_SMTP_PORT = "system.mail.smtp.port";
    public static final String KEY_SMTP_SSL = "system.mail.smtp.ssl";
    public static final String KEY_SMTP_USERNAME = "system.mail.smtp.username";
    public static final String KEY_SMTP_PASSWORD = "system.mail.smtp.password";
    public static final String KEY_MAIL_FROM = "system.mail.from";

    private final ConfigService configService;
    private final SecretCryptoService secretCryptoService;

    /**
     * 若已配置 SMTP 主机则构建发送器，否则返回 null.
     */
    public JavaMailSender createFromDatabaseConfig() {
        String host = trimToNull(configService.getValue(KEY_SMTP_HOST));
        if (host == null) {
            return null;
        }
        int port = configService.getIntValue(KEY_SMTP_PORT, 587);
        boolean tlsPreferred = configService.getBooleanValue(KEY_SMTP_SSL, true);
        String username = trimToNull(configService.getValue(KEY_SMTP_USERNAME));
        String passwordEnc = configService.getValue(KEY_SMTP_PASSWORD);
        String password = decryptSmtpPassword(passwordEnc);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (username != null) {
            sender.setUsername(username);
        }
        if (password != null) {
            sender.setPassword(password);
        }

        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", String.valueOf(username != null));
        if (port == 465) {
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.starttls.enable", "false");
        } else {
            p.put("mail.smtp.ssl.enable", "false");
            p.put("mail.smtp.starttls.enable", tlsPreferred ? "true" : "false");
        }
        return sender;
    }

    public String resolveFromAddress() {
        String from = trimToNull(configService.getValue(KEY_MAIL_FROM));
        if (from != null) {
            return from;
        }
        String username = trimToNull(configService.getValue(KEY_SMTP_USERNAME));
        return username;
    }

    private String decryptSmtpPassword(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        try {
            return secretCryptoService.decrypt(stored.trim());
        } catch (Exception e) {
            log.warn("SMTP 密码按明文使用（非加密存储或密钥已轮换），建议重新保存邮件配置以加密存储");
            return stored.trim();
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
