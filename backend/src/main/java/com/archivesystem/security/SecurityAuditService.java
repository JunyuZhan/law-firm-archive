package com.archivesystem.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 安全审计服务.
 * 记录安全相关事件
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final JdbcTemplate jdbcTemplate;

    // 事件类型常量
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String EVENT_ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";
    public static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String EVENT_TOKEN_BLACKLISTED = "TOKEN_BLACKLISTED";
    public static final String EVENT_API_KEY_USED = "API_KEY_USED";
    public static final String EVENT_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String EVENT_SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";

    /**
     * 异步记录安全事件.
     */
    @Async
    public void logSecurityEvent(String eventType, Long userId, String username, 
                                  String ipAddress, String userAgent, Map<String, Object> details) {
        try {
            String detailsJson = details != null ? toJson(details) : null;
            
            jdbcTemplate.update(
                "INSERT INTO sys_security_audit (event_type, user_id, username, ip_address, user_agent, details) " +
                "VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                eventType, userId, username, ipAddress, userAgent, detailsJson
            );
            
            log.debug("安全事件已记录: type={}, user={}, ip={}", eventType, username, ipAddress);
        } catch (Exception e) {
            log.error("记录安全事件失败: type={}, user={}", eventType, username, e);
        }
    }

    /**
     * 记录登录成功事件.
     */
    public void logLoginSuccess(Long userId, String username, String ipAddress, String userAgent) {
        logSecurityEvent(EVENT_LOGIN_SUCCESS, userId, username, ipAddress, userAgent, null);
    }

    /**
     * 记录登录失败事件.
     */
    public void logLoginFailed(String username, String ipAddress, String userAgent, String reason) {
        logSecurityEvent(EVENT_LOGIN_FAILED, null, username, ipAddress, userAgent, 
                Map.of("reason", reason));
    }

    /**
     * 记录账号锁定事件.
     */
    public void logAccountLocked(String username, String ipAddress, int failedAttempts) {
        logSecurityEvent(EVENT_ACCOUNT_LOCKED, null, username, ipAddress, null,
                Map.of("failedAttempts", failedAttempts));
    }

    /**
     * 记录可疑活动.
     */
    public void logSuspiciousActivity(String description, String ipAddress, 
                                       String userAgent, Map<String, Object> details) {
        var fullDetails = new java.util.HashMap<>(details != null ? details : Map.of());
        fullDetails.put("description", description);
        
        logSecurityEvent(EVENT_SUSPICIOUS_ACTIVITY, null, null, ipAddress, userAgent, fullDetails);
        log.warn("检测到可疑活动: {} from IP: {}", description, ipAddress);
    }

    /**
     * 简单的JSON转换.
     */
    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
