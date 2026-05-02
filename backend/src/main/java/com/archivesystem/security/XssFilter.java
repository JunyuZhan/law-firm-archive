package com.archivesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS攻击防护过滤器.
 * 对请求参数和JSON请求体进行XSS过滤
 * @author junyuzhan
 */
@Slf4j
@Component
@Order(2)
public class XssFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 文件上传请求不进行XSS过滤
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        filterChain.doFilter(new XssRequestWrapper(request), response);
    }

    /**
     * XSS请求包装器.
     * 覆盖参数、请求头和JSON请求体的XSS过滤
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        // 通用事件处理器模式：匹配所有 on*="..." 形式
        private static final Pattern ON_EVENT_PATTERN = 
                Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
        // javascript: / vbscript: 协议
        private static final Pattern JS_PROTOCOL_PATTERN = 
                Pattern.compile("(javascript|vbscript)\\s*:", Pattern.CASE_INSENSITIVE);
        // eval() / expression() 调用
        private static final Pattern DANGEROUS_CALL_PATTERN = 
                Pattern.compile("(eval|expression)\\s*\\(", Pattern.CASE_INSENSITIVE);

        // 缓存已清理的请求体
        private byte[] cachedBody;

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) {
                return null;
            }

            int count = values.length;
            String[] encodedValues = new String[count];
            for (int i = 0; i < count; i++) {
                encodedValues[i] = cleanXss(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return value != null ? cleanXss(value) : null;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return value != null ? cleanXss(value) : null;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(getCleanedBody()), StandardCharsets.UTF_8));
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() throws IOException {
            byte[] body = getCleanedBody();
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new jakarta.servlet.ServletInputStream() {
                @Override
                public int read() { return bais.read(); }
                @Override
                public boolean isFinished() { return bais.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private byte[] getCleanedBody() throws IOException {
            if (cachedBody != null) {
                return cachedBody;
            }
            // 仅对JSON请求体进行XSS过滤
            String contentType = getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = super.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                // JSON请求体仅移除危险模式，不进行HTML实体转义（避免破坏JSON结构）
                cachedBody = cleanXssForJson(sb.toString()).getBytes(StandardCharsets.UTF_8);
            } else {
                // 非JSON请求体，读取原始字节不过滤
                cachedBody = super.getInputStream().readAllBytes();
            }
            return cachedBody;
        }

        /**
         * 清除XSS攻击代码.
         * 策略：仅使用HTML实体转义，不使用正则移除（避免嵌套绕过）
         */
        private String cleanXss(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            String cleaned = value;
            // 移除危险模式（事件处理器、脚本协议、危险函数调用）
            cleaned = ON_EVENT_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = JS_PROTOCOL_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = DANGEROUS_CALL_PATTERN.matcher(cleaned).replaceAll("");

            // HTML实体转义特殊字符（核心防护，不可移除）
            cleaned = cleaned.replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");

            return cleaned;
        }

        /**
         * JSON请求体XSS清理.
         * 仅移除危险模式，不进行HTML实体转义（避免破坏JSON引号和结构）
         */
        private String cleanXssForJson(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            String cleaned = value;
            // 移除危险模式（事件处理器、脚本协议、危险函数调用）
            cleaned = ON_EVENT_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = JS_PROTOCOL_PATTERN.matcher(cleaned).replaceAll("");
            cleaned = DANGEROUS_CALL_PATTERN.matcher(cleaned).replaceAll("");

            // 不对引号进行HTML实体转义，避免破坏JSON结构
            // JSON值中的<和>转义不影响JSON解析
            cleaned = cleaned.replace("<", "\\u003c")
                    .replace(">", "\\u003e");

            return cleaned;
        }
    }
}
