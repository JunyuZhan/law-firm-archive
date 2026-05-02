package com.archivesystem.config;

import com.archivesystem.security.ApiKeyAuthFilter;
import com.archivesystem.security.JwtAuthenticationFilter;
import com.archivesystem.security.RateLimitFilter;
import com.archivesystem.security.SecurityHeadersFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityConfig测试类.
 * @author junyuzhan
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private RateLimitFilter rateLimitFilter;

    @Mock
    private SecurityHeadersFilter securityHeadersFilter;

    @Mock
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:3001");
    }

    @Test
    void testPasswordEncoder_ShouldReturnBCryptPasswordEncoder() {
        // When
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        // Then
        assertNotNull(encoder);
        assertTrue(encoder.getClass().getSimpleName().contains("BCrypt"));
        
        // 验证密码编码功能
        String rawPassword = "testPassword123";
        String encoded = encoder.encode(rawPassword);
        assertNotNull(encoded);
        assertTrue(encoder.matches(rawPassword, encoded));
        assertFalse(encoder.matches("wrongPassword", encoded));
    }

    @Test
    void testAuthenticationManager_ShouldReturnAuthenticationManager() throws Exception {
        // Given
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(mockManager);

        // When
        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        // Then
        assertNotNull(result);
        assertEquals(mockManager, result);
        verify(authenticationConfiguration).getAuthenticationManager();
    }

    @Test
    void testCorsConfigurationSource_ShouldReturnValidConfiguration() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        // Then
        assertNotNull(source);
        var config = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(config);
        
        // 验证允许的方法
        assertNotNull(config.getAllowedMethods());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("PATCH"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
        
        // 验证允许的请求头
        assertNotNull(config.getAllowedHeaders());
        assertTrue(config.getAllowedHeaders().contains("Authorization"));
        assertTrue(config.getAllowedHeaders().contains("Content-Type"));
        assertTrue(config.getAllowedHeaders().contains("X-API-Key"));
        
        // 验证暴露的响应头
        assertNotNull(config.getExposedHeaders());
        assertTrue(config.getExposedHeaders().contains("X-RateLimit-Limit"));
        assertTrue(config.getExposedHeaders().contains("X-RateLimit-Remaining"));
        
        // 验证其他配置
        assertTrue(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void testCorsConfigurationSource_ShouldAllowLocalhost() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());

        // Then
        assertNotNull(config.getAllowedOriginPatterns());
        assertTrue(config.getAllowedOriginPatterns().stream()
                .anyMatch(pattern -> pattern.contains("localhost")));
        assertTrue(config.getAllowedOriginPatterns().stream()
                .anyMatch(pattern -> pattern.contains("127.0.0.1")));
    }

    @Test
    void testCorsConfigurationSource_ShouldNotIncludePrivateNetworkWildcards() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());

        // Then - CORS不再包含内网通配符模式，防止后端暴露公网时被利用
        assertNotNull(config.getAllowedOriginPatterns());
        // 只允许localhost和127.0.0.1及环境变量配置的域名
        assertFalse(config.getAllowedOriginPatterns().stream()
                .anyMatch(pattern -> pattern.contains("192.168")));
        assertFalse(config.getAllowedOriginPatterns().stream()
                .anyMatch(pattern -> pattern.contains("10.")));
        assertFalse(config.getAllowedOriginPatterns().stream()
                .anyMatch(pattern -> pattern.contains("172.16")));
    }

    @Test
    void testPasswordEncoder_ShouldProduceDifferentHashesForSamePassword() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "testPassword123";

        // When
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        // Then
        assertNotEquals(hash1, hash2, "BCrypt应该为相同密码生成不同的哈希值");
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }

    @Test
    void testPasswordEncoder_ShouldHandleSpecialCharacters() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "P@ssw0rd!#$%^&*()";

        // When
        String encoded = encoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testPasswordEncoder_ShouldHandleUnicodeCharacters() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "密码123パスワード";

        // When
        String encoded = encoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testPasswordEncoder_ShouldHandleEmptyPassword() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "";

        // When
        String encoded = encoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testPasswordEncoder_ShouldHandleLongPassword() {
        // Given
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String password = "a".repeat(100);

        // When
        String encoded = encoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testCorsConfiguration_ShouldHaveCorrectMaxAge() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());

        // Then
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void testCorsConfiguration_ShouldAllowCredentials() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());

        // Then
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void testCorsConfiguration_ShouldIncludeSecurityHeaders() {
        // When
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        var config = source.getCorsConfiguration(new MockHttpServletRequest());

        // Then
        assertTrue(config.getAllowedHeaders().contains("X-CSRF-Token"));
        assertTrue(config.getExposedHeaders().contains("X-Request-Id"));
    }
}
