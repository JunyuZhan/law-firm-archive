package com.archivesystem.security;

import com.archivesystem.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 维护模式写入拦截过滤器.
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private static final String MAINTENANCE_MODE_KEY = "system.runtime.maintenance.enabled";

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> WRITE_WHITELIST = List.of(
            "/auth/**",
            "/restores/**",
            "/actuator/health",
            "/actuator/info"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!configService.getBooleanValue(MAINTENANCE_MODE_KEY, false) || isReadOnlyRequest(request) || isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("维护模式拦截写请求: method={}, uri={}", request.getMethod(), request.getRequestURI());
        response.setStatus(503);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "success", false,
                "code", "503",
                "message", "系统当前处于维护模式，已暂停业务写入",
                "data", Map.of("maintenance", true)
        )));
    }

    private boolean isReadOnlyRequest(HttpServletRequest request) {
        String method = request.getMethod();
        return HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method);
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithinApplication = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            pathWithinApplication = requestUri.substring(contextPath.length());
        }
        final String normalizedPath = pathWithinApplication;
        return WRITE_WHITELIST.stream().anyMatch(pattern ->
                pathMatcher.match(pattern, requestUri) || pathMatcher.match(pattern, normalizedPath));
    }
}
