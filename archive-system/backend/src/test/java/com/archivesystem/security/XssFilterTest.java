package com.archivesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * XssFilter测试类.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class XssFilterTest {

    private XssFilter xssFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        xssFilter = new XssFilter();
    }

    @Test
    void testDoFilterInternal_WithMultipartFormData_ShouldSkipFiltering() throws ServletException, IOException {
        // Given
        when(request.getContentType()).thenReturn("multipart/form-data");

        // When
        xssFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithNormalRequest_ShouldWrapRequest() throws ServletException, IOException {
        // Given
        when(request.getContentType()).thenReturn("application/json");

        // When
        xssFilter.doFilterInternal(request, response, filterChain);

        // Then
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertNotEquals(request, requestCaptor.getValue());
    }

    @Test
    void testDoFilterInternal_WithNullContentType_ShouldWrapRequest() throws ServletException, IOException {
        // Given
        when(request.getContentType()).thenReturn(null);

        // When
        xssFilter.doFilterInternal(request, response, filterChain);

        // Then
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
        assertNotEquals(request, requestCaptor.getValue());
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanScriptTag() {
        // Given
        when(request.getParameter("input")).thenReturn("<script>alert('XSS')</script>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanJavascriptProtocol() {
        // Given
        when(request.getParameter("url")).thenReturn("javascript:alert('XSS')");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanOnloadEvent() {
        // Given
        when(request.getParameter("html")).thenReturn("<img src='x' onload='alert(1)'>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanOnerrorEvent() {
        // Given
        when(request.getParameter("html")).thenReturn("<img src='x' onerror='alert(1)'>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanOnclickEvent() {
        // Given
        when(request.getParameter("html")).thenReturn("<div onclick='alert(1)'>Click</div>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanEvalExpression() {
        // Given
        when(request.getParameter("code")).thenReturn("eval('alert(1)')");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_ShouldCleanVbscript() {
        // Given
        when(request.getParameter("url")).thenReturn("vbscript:msgbox('XSS')");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameter_WithNullValue_ShouldReturnNull() {
        // Given
        when(request.getParameter("input")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameterValues_ShouldCleanAllValues() {
        // Given
        String[] values = {"<script>alert(1)</script>", "normal text", "<img onerror='alert(2)'>"};
        when(request.getParameterValues("inputs")).thenReturn(values);
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetParameterValues_WithNullValues_ShouldReturnNull() {
        // Given
        when(request.getParameterValues("inputs")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetHeader_ShouldCleanHeaderValue() {
        // Given
        when(request.getHeader("X-Custom")).thenReturn("<script>alert('XSS')</script>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_GetHeader_WithNullValue_ShouldReturnNull() {
        // Given
        when(request.getHeader("X-Custom")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_ShouldEscapeHtmlCharacters() {
        // Given
        when(request.getParameter("html")).thenReturn("<div>Test</div>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_ShouldEscapeQuotes() {
        // Given
        when(request.getParameter("text")).thenReturn("Test \"quoted\" and 'single' text");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_ShouldEscapeSlash() {
        // Given
        when(request.getParameter("path")).thenReturn("path/to/file");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithEmptyString_ShouldReturnEmpty() {
        // Given
        when(request.getParameter("input")).thenReturn("");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithComplexXssPayload_ShouldClean() {
        // Given
        String complexPayload = "<script>var x='<img src=x onerror=alert(1)>';</script>";
        when(request.getParameter("payload")).thenReturn(complexPayload);
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithOnmouseoverEvent_ShouldClean() {
        // Given
        when(request.getParameter("html")).thenReturn("<div onmouseover='alert(1)'>Hover</div>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithOnfocusEvent_ShouldClean() {
        // Given
        when(request.getParameter("html")).thenReturn("<input onfocus='alert(1)'>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithOnblurEvent_ShouldClean() {
        // Given
        when(request.getParameter("html")).thenReturn("<input onblur='alert(1)'>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithExpressionFunction_ShouldClean() {
        // Given
        when(request.getParameter("css")).thenReturn("width: expression(alert(1))");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void testXssRequestWrapper_WithSrcAttribute_ShouldClean() {
        // Given
        when(request.getParameter("html")).thenReturn("<img src='javascript:alert(1)'>");
        when(request.getContentType()).thenReturn("application/json");

        // When & Then
        assertDoesNotThrow(() -> xssFilter.doFilterInternal(request, response, filterChain));
    }
}
