package com.lawfirm.infrastructure.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.system.service.SysConfigAppService;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统维护模式拦截器
 * 当系统处于维护模式时，阻止非管理员用户访问
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    private final SysConfigAppService configAppService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查维护模式是否启用
        String maintenanceEnabled = configAppService.getConfigValue("sys.maintenance.enabled");
        if (!"true".equalsIgnoreCase(maintenanceEnabled)) {
            return true; // 维护模式未启用，继续处理
        }

        // 维护模式已启用，检查是否为管理员或维护相关接口
        String requestUri = request.getRequestURI();
        
        // 允许访问的接口（维护模式下的特殊接口）
        if (isMaintenanceAllowedUri(requestUri)) {
            return true;
        }

        // 检查是否为管理员
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                // 尝试获取当前用户，如果是管理员则允许访问
                if (SecurityUtils.isAdmin()) {
                    log.debug("维护模式下允许管理员访问: URI={}, 用户={}", requestUri, SecurityUtils.getUsername());
                    return true;
                }
            }
        } catch (Exception e) {
            // 如果获取用户信息失败（如未登录），继续执行拦截逻辑
            log.debug("维护模式下无法获取用户信息: {}", e.getMessage());
        }

        // 允许访问系统配置接口（包括维护模式相关接口），以便管理员可以关闭维护模式
        // 注意：这个接口本身有权限控制，只有管理员才能访问
        if (requestUri.contains("/system/config") && 
            (requestUri.contains("/key/sys.maintenance") || requestUri.contains("/maintenance"))) {
            return true;
        }

        // 其他请求返回维护提示
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        
        String maintenanceMessage = configAppService.getConfigValue("sys.maintenance.message");
        if (maintenanceMessage == null || maintenanceMessage.isEmpty()) {
            maintenanceMessage = "系统正在维护中，请稍后再试";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", maintenanceMessage);
        data.put("data", null);
        data.put("maintenance", true);

        Result<Object> result = Result.error("503", maintenanceMessage);
        result.setData(null);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(result));
            writer.flush();
        }

        log.info("维护模式拦截: URI={}, IP={}", requestUri, getClientIp(request));
        return false;
    }

    /**
     * 判断是否为维护模式下允许访问的URI
     */
    private boolean isMaintenanceAllowedUri(String uri) {
        // 健康检查接口
        if (uri.contains("/actuator/health")) {
            return true;
        }
        // 登录接口（允许管理员登录）
        if (uri.contains("/auth/login")) {
            return true;
        }
        // 错误页面
        if (uri.contains("/error")) {
            return true;
        }
        return false;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

