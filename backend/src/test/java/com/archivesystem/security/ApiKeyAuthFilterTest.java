package com.archivesystem.security;

import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiKeyAuthFilterTest {

    @Mock
    private ExternalSourceMapper externalSourceMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiKeyEnabled", true);
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiKeyHeader", "X-API-Key");
    }

    @Test
    void testWhitelistPath_ShouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/open/borrow/access/test-token");

        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testNonOpenApiPath_ShouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/archives");

        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testOpenApiWithoutApiKey_ShouldNotCallFilterChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/open/archive/receive");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"code\":\"401\",\"message\":\"缺少API Key\"}");

        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(stringWriter.toString().contains("\"code\":\"401\""));
        assertTrue(stringWriter.toString().contains("缺少API Key"));
    }

    @Test
    void testOpenApiWithValidApiKey_ShouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/open/archive/receive");
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key");
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(request.getHeader("X-Signature")).thenReturn("valid-signature");
        
        ExternalSource source = new ExternalSource();
        source.setId(1L);
        source.setSourceCode("TEST");
        source.setApiKey("valid-api-key");
        source.setEnabled(true);
        
        when(valueOperations.get(anyString())).thenReturn(null);
        when(externalSourceMapper.selectOne(any())).thenReturn(source);

        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(request).setAttribute("externalSource", source);
    }

    @Test
    void testOpenApiWithApiKeyDisabled_ShouldPassThrough() throws Exception {
        ReflectionTestUtils.setField(apiKeyAuthFilter, "apiKeyEnabled", false);
        when(request.getRequestURI()).thenReturn("/api/open/archive/receive");

        apiKeyAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testGenerateApiKey_ShouldReturnValidKey() {
        String apiKey = apiKeyAuthFilter.generateApiKey();
        
        assertNotNull(apiKey);
        assertTrue(apiKey.length() > 20);
    }

    @Test
    void testClearApiKeyCache() {
        when(redisTemplate.delete(anyString())).thenReturn(true);
        
        assertDoesNotThrow(() -> apiKeyAuthFilter.clearApiKeyCache("test-api-key"));
    }
}
