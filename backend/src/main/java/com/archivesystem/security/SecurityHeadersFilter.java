package com.archivesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全响应头过滤器.
 * 添加各种安全响应头以防止常见的Web攻击
 * @author junyuzhan
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 防止点击劫持 - 只允许同源iframe嵌入
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        
        // 启用浏览器XSS过滤器
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // 防止MIME类型嗅探
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // 限制Referer信息泄露
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // 内容安全策略 - 防止XSS和数据注入攻击
        response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' ws: wss:; " +
                "frame-ancestors 'self'; " +
                "base-uri 'self'; " +
                "form-action 'self'");
        
        // 权限策略 - 限制浏览器功能访问
        response.setHeader("Permissions-Policy", 
                "camera=(), microphone=(), geolocation=(), payment=()");
        
        // 严格传输安全（仅在HTTPS时有效）
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
        }
        
        // 缓存控制 - API响应不应被缓存
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }
}
