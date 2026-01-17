package com.lawfirm.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Web工具类
 * 
 * 提供获取当前请求上下文的通用方法
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
public class WebUtils {

    private WebUtils() {
    }

    /**
     * 获取当前请求对象
     * 
     * @return HttpServletRequest
     * @throws IllegalStateException 如果不在Web请求上下文中
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("当前线程中不存在Request上下文");
        }
        return attributes.getRequest();
    }

    /**
     * 获取当前请求对象（安全版本，可能返回null）
     */
    public static HttpServletRequest getRequestOrNull() {
        ServletRequestAttributes attributes = getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前响应对象
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("当前线程中不存在Response上下文");
        }
        return attributes.getResponse();
    }

    /**
     * 获取当前Session
     */
    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    /**
     * 获取当前Session（不自动创建）
     */
    public static HttpSession getSessionOrNull() {
        HttpServletRequest request = getRequestOrNull();
        return request != null ? request.getSession(false) : null;
    }

    /**
     * 获取请求URL（完整路径）
     */
    public static String getRequestUrl() {
        HttpServletRequest request = getRequest();
        return request.getRequestURL().toString();
    }

    /**
     * 获取请求URI（不含域名）
     */
    public static String getRequestUri() {
        return getRequest().getRequestURI();
    }

    /**
     * 获取请求方法（GET/POST/PUT/DELETE等）
     */
    public static String getRequestMethod() {
        return getRequest().getMethod();
    }

    /**
     * 获取请求参数
     */
    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 获取请求参数（带默认值）
     */
    public static String getParameter(String name, String defaultValue) {
        String value = getRequest().getParameter(name);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取所有请求参数
     */
    public static Map<String, String[]> getParameterMap() {
        return getRequest().getParameterMap();
    }

    /**
     * 获取请求头
     */
    public static String getHeader(String name) {
        return getRequest().getHeader(name);
    }

    /**
     * 获取所有请求头
     */
    public static Map<String, String> getHeaderMap() {
        HttpServletRequest request = getRequest();
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /**
     * 获取客户端IP地址
     * （委托给 IpUtils 实现）
     */
    public static String getClientIp() {
        return IpUtils.getIpAddr(getRequest());
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent() {
        return getHeader("User-Agent");
    }

    /**
     * 获取Referer
     */
    public static String getReferer() {
        return getHeader("Referer");
    }

    /**
     * 获取Content-Type
     */
    public static String getContentType() {
        return getRequest().getContentType();
    }

    /**
     * 获取请求体长度
     */
    public static int getContentLength() {
        return getRequest().getContentLength();
    }

    /**
     * 判断是否Ajax请求
     */
    public static boolean isAjaxRequest() {
        String requestedWith = getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    /**
     * 判断是否JSON请求
     */
    public static boolean isJsonRequest() {
        String contentType = getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * 判断是否POST请求
     */
    public static boolean isPostRequest() {
        return "POST".equalsIgnoreCase(getRequestMethod());
    }

    /**
     * 判断是否GET请求
     */
    public static boolean isGetRequest() {
        return "GET".equalsIgnoreCase(getRequestMethod());
    }

    /**
     * 获取请求域名
     */
    public static String getServerName() {
        return getRequest().getServerName();
    }

    /**
     * 获取请求端口
     */
    public static int getServerPort() {
        return getRequest().getServerPort();
    }

    /**
     * 获取请求协议（http/https）
     */
    public static String getScheme() {
        return getRequest().getScheme();
    }

    /**
     * 获取完整基础URL（如 https://example.com:8080）
     */
    public static String getBaseUrl() {
        HttpServletRequest request = getRequest();
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        
        // 只有非标准端口才添加
        if (("http".equals(scheme) && serverPort != 80) ||
            ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        
        return url.toString();
    }

    /**
     * 获取上下文路径
     */
    public static String getContextPath() {
        return getRequest().getContextPath();
    }

    /**
     * 设置响应内容类型
     */
    public static void setContentType(String contentType) {
        getResponse().setContentType(contentType);
    }

    /**
     * 设置响应头
     */
    public static void setHeader(String name, String value) {
        getResponse().setHeader(name, value);
    }

    /**
     * 写入JSON响应
     */
    public static void writeJson(String json) throws IOException {
        HttpServletResponse response = getResponse();
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
            writer.flush();
        }
    }

    /**
     * 设置下载响应头
     */
    public static void setDownloadHeader(String fileName) {
        HttpServletResponse response = getResponse();
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    /**
     * 获取请求属性
     */
    public static Object getAttribute(String name) {
        return getRequest().getAttribute(name);
    }

    /**
     * 设置请求属性
     */
    public static void setAttribute(String name, Object value) {
        getRequest().setAttribute(name, value);
    }

    /**
     * 获取ServletRequestAttributes
     */
    private static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return (ServletRequestAttributes) attributes;
        }
        return null;
    }
}

