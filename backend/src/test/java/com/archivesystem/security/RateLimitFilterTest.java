package com.archivesystem.security;

import com.archivesystem.common.util.ClientIpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitFilter测试类.
 * @author junyuzhan
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

    private static DefaultRedisScript<Long> anyRedisScript() {
        return org.mockito.ArgumentMatchers.<DefaultRedisScript<Long>>any();
    }

    private static List<String> anyStringList() {
        return org.mockito.ArgumentMatchers.<String>anyList();
    }

    private RateLimitFilter rateLimitFilter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rateLimitFilter = new RateLimitFilter(redisTemplate, objectMapper);
        responseWriter = new StringWriter();
        ClientIpUtils.configureTrustedProxies(java.util.List.of("127.0.0.1", "::1"));
    }

    @Test
    void testDoFilterInternal_FirstRequest_ShouldSetExpiration() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), anyStringList(), anyString());
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "99");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithinLimit_ShouldAllowRequest() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(50L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "100");
        verify(response).setHeader("X-RateLimit-Remaining", "50");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ExceedLimit_ShouldBlockRequest() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(101L);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        
        String responseBody = responseWriter.toString();
        assertTrue(responseBody.contains("请求过于频繁"));
    }

    @Test
    void testDoFilterInternal_LoginEndpoint_ShouldUseLoginRateLimit() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "9");
    }

    @Test
    void testDoFilterInternal_UploadEndpoint_ShouldUseUploadRateLimit() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/files/upload");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "20");
        verify(response).setHeader("X-RateLimit-Remaining", "19");
    }

    @Test
    void testDoFilterInternal_OpenApiWriteEndpoint_ShouldUseOpenWriteRateLimit() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/open/archive/receive");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Limit", "20");
        verify(response).setHeader("X-RateLimit-Remaining", "19");
    }

    @Test
    void testDoFilterInternal_PublicBorrowAccessEndpoint_ShouldUseStricterLimit() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/open/borrow/access/token12345678");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-RateLimit-Limit", "12");
        verify(response).setHeader("X-RateLimit-Remaining", "11");
    }

    @Test
    void testDoFilterInternal_WithXForwardedFor_ShouldUseForwardedIp() throws ServletException, IOException {
        // Given
        ClientIpUtils.configureTrustedProxies(java.util.List.of("192.168.1.1"));
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then - verify the filter used the forwarded IP by checking the Redis key
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("203.0.113.1");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithXRealIp_ShouldUseRealIp() throws ServletException, IOException {
        // Given
        ClientIpUtils.configureTrustedProxies(java.util.List.of("192.168.1.1"));
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("203.0.113.2");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithProxyClientIp_ShouldUseProxyIp() throws ServletException, IOException {
        // Given
        ClientIpUtils.configureTrustedProxies(java.util.List.of("192.168.1.1"));
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("203.0.113.3");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("203.0.113.3");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithUnknownHeader_ShouldUseRemoteAddr() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("192.168.1.1");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_RedisException_ShouldAllowRequest() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenThrow(new RuntimeException("Redis error"));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_NullCount_ShouldHandleGracefully() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(null);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Remaining", "100");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ExactlyAtLimit_ShouldAllowRequest() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(100L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-RateLimit-Remaining", "0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_LoginExceedLimit_ShouldBlockWithCorrectMessage() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(11L);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        String responseBody = responseWriter.toString();
        assertTrue(responseBody.contains("10次请求"));
    }

    @Test
    void testDoFilterInternal_UploadExceedLimit_ShouldBlockWithCorrectMessage() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/files/upload");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(21L);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        String responseBody = responseWriter.toString();
        assertTrue(responseBody.contains("20次请求"));
    }

    @Test
    void testDoFilterInternal_OpenApiExceedLimit_ShouldBlockWithCorrectMessage() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/open/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(21L);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setStatus(429);
        String responseBody = responseWriter.toString();
        assertTrue(responseBody.contains("20次请求"));
    }

    @Test
    void testDoFilterInternal_RateLimitResponse_ShouldIncludeTimestamp() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(101L);
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        String responseBody = responseWriter.toString();
        assertTrue(responseBody.contains("timestamp"));
        assertTrue(responseBody.contains("success"));
        assertTrue(responseBody.contains("code"));
        assertTrue(responseBody.contains("message"));
    }

    @Test
    void testDoFilterInternal_WithEmptyXForwardedFor_ShouldUseRemoteAddr() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("192.168.1.1");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithMultipleForwardedIps_ShouldUseFirstIp() throws ServletException, IOException {
        // Given
        ClientIpUtils.configureTrustedProxies(java.util.List.of("192.168.1.1"));
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        // When
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("203.0.113.1");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithPublicRemoteAddr_ShouldIgnoreSpoofedForwardedIp() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("8.8.8.8");
        }), anyString());
    }

    @Test
    void testDoFilterInternal_WithPrivateRemoteAddrNotTrusted_ShouldIgnoreSpoofedForwardedIp() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(redisTemplate.execute(anyRedisScript(), anyStringList(), anyString())).thenReturn(1L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).execute(anyRedisScript(), argThat(args -> {
            String key = (String) ((List<?>) args).get(0);
            return key.contains("192.168.1.1");
        }), anyString());
    }
}
