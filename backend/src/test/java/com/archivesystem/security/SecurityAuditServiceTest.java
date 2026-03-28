package com.archivesystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityAuditServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SecurityAuditService securityAuditService;

    @BeforeEach
    void setUp() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void testLogLoginSuccess() {
        securityAuditService.logLoginSuccess(1L, "testuser", "127.0.0.1", "Mozilla/5.0");

        verify(jdbcTemplate).update(
                contains("INSERT INTO sys_security_audit"),
                eq(SecurityAuditService.EVENT_LOGIN_SUCCESS),
                eq(1L),
                eq("testuser"),
                eq("127.0.0.1"),
                eq("Mozilla/5.0"),
                isNull()
        );
    }

    @Test
    void testLogLoginFailed() {
        securityAuditService.logLoginFailed("baduser", "192.168.1.1", "Mozilla/5.0", "密码错误");

        verify(jdbcTemplate).update(
                contains("INSERT INTO sys_security_audit"),
                eq(SecurityAuditService.EVENT_LOGIN_FAILED),
                isNull(),
                eq("baduser"),
                eq("192.168.1.1"),
                eq("Mozilla/5.0"),
                contains("密码错误")
        );
    }

    @Test
    void testLogAccountLocked() {
        securityAuditService.logAccountLocked("lockeduser", "10.0.0.1", 5);

        verify(jdbcTemplate).update(
                contains("INSERT INTO sys_security_audit"),
                eq(SecurityAuditService.EVENT_ACCOUNT_LOCKED),
                isNull(),
                eq("lockeduser"),
                eq("10.0.0.1"),
                isNull(),
                contains("5")
        );
    }

    @Test
    void testLogSuspiciousActivity() {
        securityAuditService.logSuspiciousActivity(
                "多次登录失败", "192.168.1.100", "Mozilla/5.0", 
                Map.of("attempts", 10));

        verify(jdbcTemplate).update(
                contains("INSERT INTO sys_security_audit"),
                eq(SecurityAuditService.EVENT_SUSPICIOUS_ACTIVITY),
                isNull(),
                isNull(),
                eq("192.168.1.100"),
                eq("Mozilla/5.0"),
                argThat(arg -> arg != null && arg.toString().contains("多次登录失败"))
        );
    }

    @Test
    void testLogSecurityEvent_WithDetails() {
        Map<String, Object> details = Map.of("action", "test", "count", 5);
        
        securityAuditService.logSecurityEvent(
                "CUSTOM_EVENT", 1L, "admin", "127.0.0.1", "TestAgent", details);

        verify(jdbcTemplate).update(
                contains("INSERT INTO sys_security_audit"),
                eq("CUSTOM_EVENT"),
                eq(1L),
                eq("admin"),
                eq("127.0.0.1"),
                eq("TestAgent"),
                argThat(arg -> arg != null && arg.toString().contains("test"))
        );
    }
}
