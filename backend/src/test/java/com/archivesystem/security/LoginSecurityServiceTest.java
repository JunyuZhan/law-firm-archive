package com.archivesystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginSecurityServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginSecurityService loginSecurityService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testIsAccountLocked_WhenLocked() {
        String username = "testuser";
        when(redisTemplate.hasKey("login:locked:" + username)).thenReturn(true);

        boolean result = loginSecurityService.isAccountLocked(username);

        assertTrue(result);
        verify(redisTemplate).hasKey("login:locked:" + username);
    }

    @Test
    void testIsAccountLocked_WhenNotLocked() {
        String username = "testuser";
        when(redisTemplate.hasKey("login:locked:" + username)).thenReturn(false);

        boolean result = loginSecurityService.isAccountLocked(username);

        assertFalse(result);
    }

    @Test
    void testIsIpLocked_WhenLocked() {
        String ip = "192.168.1.1";
        when(valueOperations.get("login:ip_failed:" + ip)).thenReturn("15");

        boolean result = loginSecurityService.isIpLocked(ip);

        assertTrue(result);
    }

    @Test
    void testIsIpLocked_WhenNotLocked() {
        String ip = "192.168.1.1";
        when(valueOperations.get("login:ip_failed:" + ip)).thenReturn("5");

        boolean result = loginSecurityService.isIpLocked(ip);

        assertFalse(result);
    }

    @Test
    void testIsIpLocked_WhenNoRecord() {
        String ip = "192.168.1.1";
        when(valueOperations.get("login:ip_failed:" + ip)).thenReturn(null);

        boolean result = loginSecurityService.isIpLocked(ip);

        assertFalse(result);
    }

    @Test
    void testGetRemainingLockoutTime_WhenLocked() {
        String username = "testuser";
        when(redisTemplate.getExpire("login:locked:" + username)).thenReturn(1800L);

        long result = loginSecurityService.getRemainingLockoutTime(username);

        assertEquals(1800L, result);
    }

    @Test
    void testGetRemainingLockoutTime_WhenNotLocked() {
        String username = "testuser";
        when(redisTemplate.getExpire("login:locked:" + username)).thenReturn(-1L);

        long result = loginSecurityService.getRemainingLockoutTime(username);

        assertEquals(0L, result);
    }

    @Test
    void testRecordFailedAttempt_FirstAttempt() {
        String username = "testuser";
        String ip = "192.168.1.1";
        
        when(valueOperations.increment("login:failed:" + username)).thenReturn(1L);
        when(valueOperations.increment("login:ip_failed:" + ip)).thenReturn(1L);

        int result = loginSecurityService.recordFailedAttempt(username, ip);

        assertEquals(1, result);
        verify(redisTemplate).expire(eq("login:failed:" + username), any(Duration.class));
        verify(redisTemplate).expire(eq("login:ip_failed:" + ip), any(Duration.class));
    }

    @Test
    void testRecordFailedAttempt_MultipleAttempts() {
        String username = "testuser";
        String ip = "192.168.1.1";
        
        when(valueOperations.increment("login:failed:" + username)).thenReturn(3L);
        when(valueOperations.increment("login:ip_failed:" + ip)).thenReturn(3L);

        int result = loginSecurityService.recordFailedAttempt(username, ip);

        assertEquals(3, result);
    }

    @Test
    void testRecordFailedAttempt_TriggersLock() {
        String username = "testuser";
        String ip = "192.168.1.1";
        
        when(valueOperations.increment("login:failed:" + username)).thenReturn(5L);
        when(valueOperations.increment("login:ip_failed:" + ip)).thenReturn(5L);

        int result = loginSecurityService.recordFailedAttempt(username, ip);

        assertEquals(5, result);
        verify(valueOperations).set(eq("login:locked:" + username), eq("locked"), any(Duration.class));
    }

    @Test
    void testClearFailedAttempts() {
        String username = "testuser";

        loginSecurityService.clearFailedAttempts(username);

        verify(redisTemplate).delete("login:failed:" + username);
    }

    @Test
    void testUnlockAccount() {
        String username = "testuser";

        loginSecurityService.unlockAccount(username);

        verify(redisTemplate).delete("login:locked:" + username);
        verify(redisTemplate).delete("login:failed:" + username);
    }

    @Test
    void testGetRemainingAttempts_NoFailures() {
        String username = "testuser";
        when(valueOperations.get("login:failed:" + username)).thenReturn(null);

        int result = loginSecurityService.getRemainingAttempts(username);

        assertEquals(5, result);
    }

    @Test
    void testGetRemainingAttempts_SomeFailures() {
        String username = "testuser";
        when(valueOperations.get("login:failed:" + username)).thenReturn("2");

        int result = loginSecurityService.getRemainingAttempts(username);

        assertEquals(3, result);
    }

    @Test
    void testGetRemainingAttempts_MaxFailures() {
        String username = "testuser";
        when(valueOperations.get("login:failed:" + username)).thenReturn("5");

        int result = loginSecurityService.getRemainingAttempts(username);

        assertEquals(0, result);
    }

    @Test
    void testGetRemainingAttempts_ExceedsMax() {
        String username = "testuser";
        when(valueOperations.get("login:failed:" + username)).thenReturn("10");

        int result = loginSecurityService.getRemainingAttempts(username);

        assertEquals(0, result);
    }
}
