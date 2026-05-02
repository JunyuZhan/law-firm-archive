package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.entity.SysConfig;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class ConfigValueValidator {

    private static final String UPLOAD_ALLOWED_TYPES_KEY = "system.upload.allowed.types";
    private static final String SMTP_HOST_KEY = "system.mail.smtp.host";
    private static final String SMTP_PORT_KEY = "system.mail.smtp.port";
    private static final String BACKUP_CRON_KEY = "system.backup.cron";
    private static final String MINIO_PROXY_PREFIX_KEY = "system.storage.minio.proxy-prefix";
    private static final String ARCHIVE_NO_DATE_FORMAT_KEY = "archive.no.date.format";
    private static final String ARCHIVE_NO_SEQ_DIGITS_KEY = "archive.no.seq.digits";
    private static final String ARCHIVE_NO_PREFIX_KEY_PREFIX = "archive.no.prefix.";
    private static final String UPLOAD_MAX_SIZE_KEY = "system.upload.max.size";
    private static final String BACKUP_KEEP_COUNT_KEY = "system.backup.keep.count";
    private static final Set<String> HTTP_URL_CONFIG_KEYS = Set.of(
            "system.upgrade.registry_base_url",
            "system.upgrade.dist_center_latest_json_url"
    );
    private static final Set<String> EMAIL_CONFIG_KEYS = Set.of(
            "system.mail.from",
            "system.notify.admin.emails"
    );
    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ConfigValueValidator() {
    }

    static String validateAndNormalizeByType(String configType, String key, String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            if (SysConfig.TYPE_NUMBER.equalsIgnoreCase(configType) || SysConfig.TYPE_BOOLEAN.equalsIgnoreCase(configType)) {
                throw new BusinessException("配置项值不能为空: " + key);
            }
            if (requiresNonBlankStringValue(key)) {
                throw new BusinessException("配置项值不能为空: " + key);
            }
            return value;
        }

        if (SysConfig.TYPE_NUMBER.equalsIgnoreCase(configType)) {
            try {
                int parsed = Integer.parseInt(trimmed);
                if (ARCHIVE_NO_SEQ_DIGITS_KEY.equals(key) && parsed <= 0) {
                    throw new BusinessException("档案号序号位数必须大于 0");
                }
                if (UPLOAD_MAX_SIZE_KEY.equals(key) && parsed <= 0) {
                    throw new BusinessException("上传文件大小限制必须大于 0");
                }
                if (BACKUP_KEEP_COUNT_KEY.equals(key) && parsed <= 0) {
                    throw new BusinessException("备份保留份数必须大于 0");
                }
                if (SMTP_PORT_KEY.equals(key) && (parsed < 1 || parsed > 65535)) {
                    throw new BusinessException("SMTP 端口必须在 1-65535 之间");
                }
                return String.valueOf(parsed);
            } catch (NumberFormatException e) {
                throw new BusinessException("配置项必须为整数: " + key);
            }
        }

        if (SysConfig.TYPE_BOOLEAN.equalsIgnoreCase(configType)) {
            String normalized = trimmed.toLowerCase(Locale.ROOT);
            if ("true".equals(normalized) || "1".equals(normalized)) {
                return "true";
            }
            if ("false".equals(normalized) || "0".equals(normalized)) {
                return "false";
            }
            throw new BusinessException("配置项必须为布尔值: " + key);
        }

        if (UPLOAD_ALLOWED_TYPES_KEY.equals(key)) {
            return normalizeAllowedUploadTypes(trimmed);
        }

        if (SMTP_HOST_KEY.equals(key)) {
            return normalizeSmtpHost(trimmed);
        }

        if (BACKUP_CRON_KEY.equals(key)) {
            return normalizeCronExpression(key, trimmed);
        }

        if (MINIO_PROXY_PREFIX_KEY.equals(key)) {
            return normalizeProxyPrefix(key, trimmed);
        }

        if (ARCHIVE_NO_DATE_FORMAT_KEY.equals(key)) {
            return normalizeDateFormat(key, trimmed);
        }

        if (key != null && key.startsWith(ARCHIVE_NO_PREFIX_KEY_PREFIX)) {
            return normalizeArchiveNoPrefix(key, trimmed);
        }

        if (HTTP_URL_CONFIG_KEYS.contains(key)) {
            return normalizeHttpUrlConfig(key, trimmed);
        }

        if (EMAIL_CONFIG_KEYS.contains(key)) {
            return normalizeEmailConfig(key, trimmed);
        }

        return value;
    }

    private static String normalizeAllowedUploadTypes(String value) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String ext = item.trim().toLowerCase(Locale.ROOT);
            if (ext.isEmpty()) {
                continue;
            }
            if (!ext.matches("^[a-z0-9]{1,16}$")) {
                throw new BusinessException("允许上传的文件类型格式不正确");
            }
            normalized.add(ext);
        }
        if (normalized.isEmpty()) {
            throw new BusinessException("允许上传的文件类型不能为空");
        }
        return String.join(",", normalized);
    }

    private static String normalizeSmtpHost(String value) {
        if (value.contains("://")) {
            throw new BusinessException("SMTP 服务器只需填写主机名或 IP，不要包含协议头");
        }
        if (value.contains("/") || value.contains("?") || value.contains("#")) {
            throw new BusinessException("SMTP 服务器格式不正确");
        }
        if (value.isBlank()) {
            throw new BusinessException("SMTP 服务器不能为空");
        }
        return value;
    }

    private static String normalizeCronExpression(String key, String value) {
        if (value.isBlank()) {
            throw new BusinessException("配置项不能为空: " + key);
        }
        try {
            CronExpression.parse(value);
            return value;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Cron 表达式格式不正确: " + key);
        }
    }

    private static String normalizeProxyPrefix(String key, String value) {
        if (!value.startsWith("/")) {
            throw new BusinessException("配置项必须以 / 开头: " + key);
        }
        if (value.contains("://")) {
            throw new BusinessException("配置项必须为站内相对路径: " + key);
        }
        if (value.contains("?") || value.contains("#")) {
            throw new BusinessException("配置项不能包含查询参数或锚点: " + key);
        }
        if (value.contains("//")) {
            throw new BusinessException("配置项路径格式不正确: " + key);
        }
        if ("/".equals(value)) {
            throw new BusinessException("配置项不能为根路径 /: " + key);
        }
        String normalized = value.endsWith("/") && value.length() > 1
                ? value.substring(0, value.length() - 1)
                : value;
        if (normalized.contains("/../") || normalized.endsWith("/..") || normalized.contains("/./")) {
            throw new BusinessException("配置项路径格式不正确: " + key);
        }
        return normalized;
    }

    private static String normalizeDateFormat(String key, String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(value);
            LocalDate.now().format(formatter);
            return value;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("日期格式不正确: " + key);
        }
    }

    private static String normalizeArchiveNoPrefix(String key, String value) {
        if (value.isBlank()) {
            throw new BusinessException("档案号前缀不能为空: " + key);
        }
        return value;
    }

    private static boolean requiresNonBlankStringValue(String key) {
        if (key == null) {
            return false;
        }
        return UPLOAD_ALLOWED_TYPES_KEY.equals(key)
                || BACKUP_CRON_KEY.equals(key)
                || MINIO_PROXY_PREFIX_KEY.equals(key)
                || ARCHIVE_NO_DATE_FORMAT_KEY.equals(key)
                || key.startsWith(ARCHIVE_NO_PREFIX_KEY_PREFIX);
    }

    private static String normalizeHttpUrlConfig(String key, String value) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(value).build(true).toUri();
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new BusinessException("配置项必须为 http:// 或 https:// 地址: " + key);
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new BusinessException("配置项缺少主机名: " + key);
            }
            return value;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("配置项 URL 格式不正确: " + key);
        }
    }

    private static String normalizeEmailConfig(String key, String value) {
        if ("system.notify.admin.emails".equals(key)) {
            Set<String> emails = new LinkedHashSet<>();
            for (String item : value.split(",")) {
                String email = item.trim();
                if (email.isEmpty()) {
                    continue;
                }
                validateEmail(key, email);
                emails.add(email);
            }
            if (emails.isEmpty()) {
                throw new BusinessException("额外通知邮箱不能为空");
            }
            return String.join(",", emails);
        }

        validateEmail(key, value);
        return value;
    }

    private static void validateEmail(String key, String value) {
        if (!SIMPLE_EMAIL_PATTERN.matcher(value).matches()) {
            throw new BusinessException("配置项邮箱格式不正确: " + key);
        }
    }
}
