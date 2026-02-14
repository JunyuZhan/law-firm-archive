package com.archivesystem.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testAddToBlacklist_ValidToken() {
        String token = "valid.jwt.token";
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 3600000)); // 1 hour from now

        when(jwtUtils.parseToken(token)).thenReturn(claims);

        tokenBlacklistService.addToBlacklist(token);

        verify(valueOperations).set(anyString(), eq("blacklisted"), any(Duration.class));
    }

    @Test
    void testAddToBlacklist_ExpiredToken() {
        String token = "expired.jwt.token";
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 3600000)); // 1 hour ago

        when(jwtUtils.parseToken(token)).thenReturn(claims);

        tokenBlacklistService.addToBlacklist(token);

        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void testAddToBlacklist_InvalidToken() {
        String token = "invalid.jwt.token";

        when(jwtUtils.parseToken(token)).thenThrow(new RuntimeException("Invalid token"));

        assertDoesNotThrow(() -> tokenBlacklistService.addToBlacklist(token));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void testIsBlacklisted_TokenInBlacklist() {
        String token = "blacklisted.jwt.token";
        String tokenId = String.valueOf(token.hashCode());

        when(redisTemplate.hasKey("token:blacklist:" + tokenId)).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertTrue(result);
    }

    @Test
    void testIsBlacklisted_TokenNotInBlacklist() {
        String token = "valid.jwt.token";
        String tokenId = String.valueOf(token.hashCode());

        when(redisTemplate.hasKey("token:blacklist:" + tokenId)).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertFalse(result);
    }

    @Test
    void testIsBlacklisted_ExceptionHandling() {
        String token = "problematic.jwt.token";

        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertFalse(result);
    }

    @Test
    void testBlacklistUserTokens() {
        Long userId = 123L;
        long durationSeconds = 3600L;

        tokenBlacklistService.blacklistUserTokens(userId, durationSeconds);

        verify(valueOperations).set(
            eq("token:user_blacklist:" + userId),
            anyString(),
            eq(Duration.ofSeconds(durationSeconds))
        );
    }

    @Test
    void testIsUserBlacklisted_UserIsBlacklisted() {
        Long userId = 123L;
        long tokenIssuedAt = System.currentTimeMillis() - 10000; // 10 seconds ago
        long blacklistTime = System.currentTimeMillis() - 5000; // 5 seconds ago

        when(valueOperations.get("token:user_blacklist:" + userId))
            .thenReturn(String.valueOf(blacklistTime));

        boolean result = tokenBlacklistService.isUserBlacklisted(userId, tokenIssuedAt);

        assertTrue(result);
    }

    @Test
    void testIsUserBlacklisted_UserNotBlacklisted() {
        Long userId = 123L;
        long tokenIssuedAt = System.currentTimeMillis();

        when(valueOperations.get("token:user_blacklist:" + userId)).thenReturn(null);

        boolean result = tokenBlacklistService.isUserBlacklisted(userId, tokenIssuedAt);

        assertFalse(result);
    }

    @Test
    void testIsUserBlacklisted_TokenIssuedAfterBlacklist() {
        Long userId = 123L;
        long tokenIssuedAt = System.currentTimeMillis();
        long blacklistTime = System.currentTimeMillis() - 10000; // 10 seconds ago

        when(valueOperations.get("token:user_blacklist:" + userId))
            .thenReturn(String.valueOf(blacklistTime));

        boolean result = tokenBlacklistService.isUserBlacklisted(userId, tokenIssuedAt);

        assertFalse(result);
    }
}
