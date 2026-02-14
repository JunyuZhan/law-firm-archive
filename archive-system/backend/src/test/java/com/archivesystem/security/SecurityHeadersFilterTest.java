package com.archivesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * SecurityHeadersFilter测试类.
 */
@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter securityHeadersFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        securityHeadersFilter = new SecurityHeadersFilter();
    }

    @Test
    void testDoFilterInternal_ShouldSetXFrameOptions() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetXXssProtection() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetXContentTypeOptions() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetReferrerPolicy() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetContentSecurityPolicy() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), contains("default-src 'self'"));
        verify(response).setHeader(eq("Content-Security-Policy"), contains("script-src"));
        verify(response).setHeader(eq("Content-Security-Policy"), contains("style-src"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetPermissionsPolicy() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Permissions-Policy"), contains("camera=()"));
        verify(response).setHeader(eq("Permissions-Policy"), contains("microphone=()"));
        verify(response).setHeader(eq("Permissions-Policy"), contains("geolocation=()"));
        verify(response).setHeader(eq("Permissions-Policy"), contains("payment=()"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithHttpsRequest_ShouldSetHSTS() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(true);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithHttpRequest_ShouldNotSetHSTS() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, never()).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithApiRequest_ShouldSetCacheControl() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithNonApiRequest_ShouldNotSetCacheControl() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/public/index.html");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response, never()).setHeader(eq("Cache-Control"), anyString());
        verify(response, never()).setHeader(eq("Pragma"), anyString());
        verify(response, never()).setHeader(eq("Expires"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ShouldSetAllSecurityHeaders() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(true);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(response).setHeader(eq("Content-Security-Policy"), anyString());
        verify(response).setHeader(eq("Permissions-Policy"), anyString());
        verify(response).setHeader(eq("Strict-Transport-Security"), anyString());
        verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeScriptSrc() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("script-src 'self' 'unsafe-inline' 'unsafe-eval'"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeStyleSrc() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("style-src 'self' 'unsafe-inline'"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeImgSrc() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("img-src 'self' data: blob: https:"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeFontSrc() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("font-src 'self' data:"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeConnectSrc() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("connect-src 'self' ws: wss:"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeFrameAncestors() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("frame-ancestors 'self'"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeBaseUri() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("base-uri 'self'"));
    }

    @Test
    void testDoFilterInternal_ContentSecurityPolicy_ShouldIncludeFormAction() throws ServletException, IOException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.isSecure()).thenReturn(false);

        // When
        securityHeadersFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).setHeader(eq("Content-Security-Policy"), 
                contains("form-action 'self'"));
    }

    @Test
    void testDoFilterInternal_WithDifferentApiPaths_ShouldSetCacheControl() throws ServletException, IOException {
        // Given
        String[] apiPaths = {"/api/users", "/api/archives", "/api/auth"};
        
        for (String path : apiPaths) {
            reset(request, response, filterChain);
            when(request.getRequestURI()).thenReturn(path);
            when(request.isSecure()).thenReturn(false);

            // When
            securityHeadersFilter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        }
    }
}
