package com.lawfirm.infrastructure.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XSS过滤器
 * 
 * 功能：
 * - 过滤请求参数中的XSS脚本
 * - 过滤请求头中的XSS脚本
 * - 可配置排除路径
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public class XssFilter implements Filter {

    /**
     * 排除的路径前缀（不进行XSS过滤）
     */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/health",
        "/actuator",
        "/doc.html",
        "/swagger-ui",
        "/v3/api-docs",
        "/webjars"
    );

    /**
     * 需要跳过的Content-Type
     */
    private static final List<String> SKIP_CONTENT_TYPES = Arrays.asList(
        "application/json",
        "multipart/form-data"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XssFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 检查是否在排除路径中
        if (isExcludePath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查Content-Type是否需要跳过
        String contentType = httpRequest.getContentType();
        if (contentType != null && shouldSkipContentType(contentType)) {
            // JSON请求体由Jackson处理，这里只过滤参数和头
            chain.doFilter(new XssHttpServletRequestWrapper(httpRequest, false), response);
            return;
        }

        // 使用XSS包装器
        chain.doFilter(new XssHttpServletRequestWrapper(httpRequest, true), response);
    }

    @Override
    public void destroy() {
        log.info("XssFilter destroyed");
    }

    /**
     * 检查是否是排除路径
     */
    private boolean isExcludePath(String path) {
        if (path == null) {
            return false;
        }
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 检查是否需要跳过的Content-Type
     */
    private boolean shouldSkipContentType(String contentType) {
        return SKIP_CONTENT_TYPES.stream().anyMatch(contentType::contains);
    }

    /**
     * XSS请求包装器
     */
    private static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        /**
         * 是否过滤请求体（保留用于未来扩展）
         */
        @SuppressWarnings("unused")
        private final boolean filterBody;

        /**
         * 危险的脚本模式
         */
        private static final Pattern[] DANGEROUS_PATTERNS = {
            // Script标签
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // Eval表达式
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // JavaScript协议
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            // VBScript协议
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            // onXXX事件处理器
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // Expression
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // src属性中的javascript
            Pattern.compile("src\\s*=\\s*['\"]?\\s*javascript:", Pattern.CASE_INSENSITIVE),
            // iframe
            Pattern.compile("<iframe(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
        };

        public XssHttpServletRequestWrapper(HttpServletRequest request, boolean filterBody) {
            super(request);
            this.filterBody = filterBody;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanXss(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleanedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanedValues[i] = cleanXss(values[i]);
            }
            return cleanedValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return cleanXss(value);
        }

        /**
         * 清理XSS脚本
         * 
         * 注意：只移除危险的脚本模式，不进行 HTML 转义
         * HTML 转义应该在输出到 HTML 页面时进行，由前端框架处理
         */
        private String cleanXss(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            String cleaned = value;

            // 使用正则表达式移除危险模式
            for (Pattern pattern : DANGEROUS_PATTERNS) {
                cleaned = pattern.matcher(cleaned).replaceAll("");
            }

            // 注意：不在这里进行 HTML 转义
            // HTML 转义应该在输出到前端 HTML 页面时进行
            // cleaned = HtmlUtils.htmlEscape(cleaned);

            // 如果值被修改，记录日志
            if (!value.equals(cleaned)) {
                log.warn("XSS attack detected and cleaned. Original: [{}], Cleaned: [{}]", 
                    truncate(value, 100), truncate(cleaned, 100));
            }

            return cleaned;
        }

        /**
         * 截断字符串用于日志
         */
        private String truncate(String str, int maxLength) {
            if (str == null) {
                return null;
            }
            if (str.length() <= maxLength) {
                return str;
            }
            return str.substring(0, maxLength) + "...";
        }
    }
}

