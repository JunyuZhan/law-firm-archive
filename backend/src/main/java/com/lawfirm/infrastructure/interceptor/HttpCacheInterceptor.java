package com.lawfirm.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 缓存控制拦截器
 * 
 * 为不同类型的 API 设置适当的 Cache-Control 头，
 * 配合前端 Service Worker 和浏览器缓存，提升性能。
 * 
 * 缓存策略：
 * - 字典/配置等静态数据：较长缓存（1小时）
 * - 菜单/部门数据：中等缓存（10分钟）
 * - 业务列表数据：短缓存 + stale-while-revalidate
 * - 敏感数据：不缓存
 * 
 * @author system
 * @since 2026-01-17
 */
@Slf4j
@Component
public class HttpCacheInterceptor implements HandlerInterceptor {

    // 缓存配置：路径前缀 -> 缓存时间（秒）
    private static final Map<String, CacheConfig> CACHE_CONFIGS = Map.ofEntries(
        // 静态数据：长缓存
        Map.entry("/api/dict/", new CacheConfig(3600, true)),     // 字典：1小时
        Map.entry("/api/config/", new CacheConfig(1800, true)),   // 配置：30分钟
        Map.entry("/api/causes/", new CacheConfig(3600, true)),   // 案由：1小时
        Map.entry("/api/public/", new CacheConfig(1800, true)),   // 公共：30分钟
        
        // 菜单/部门：中等缓存
        Map.entry("/api/menu/", new CacheConfig(600, true)),      // 菜单：10分钟
        Map.entry("/api/department/", new CacheConfig(600, true)), // 部门：10分钟
        
        // 业务数据：短缓存 + stale-while-revalidate
        Map.entry("/api/matter", new CacheConfig(60, 120)),    // 案件：1分钟，过期后可用2分钟
        Map.entry("/api/crm", new CacheConfig(60, 120)),       // 客户：1分钟
        Map.entry("/api/document", new CacheConfig(60, 120)),  // 文档：1分钟
        Map.entry("/api/finance", new CacheConfig(30, 60)),    // 财务：30秒（更敏感）
        Map.entry("/api/task", new CacheConfig(60, 120)),      // 任务：1分钟
        Map.entry("/api/timesheet", new CacheConfig(60, 120)), // 工时：1分钟
        Map.entry("/api/archive", new CacheConfig(120, 300)),  // 档案：2分钟
        Map.entry("/api/knowledge", new CacheConfig(300, 600)) // 知识库：5分钟
    );

    // 不缓存的敏感 API
    private static final String[] NO_CACHE_PATTERNS = {
        "/api/auth/",
        "/api/admin/user",
        "/api/hr/payroll",
        "/api/hr/employee"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理 GET 请求
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            // 非 GET 请求不缓存
            response.setHeader("Cache-Control", "no-store");
            return true;
        }

        String path = request.getRequestURI();

        // 检查是否为不缓存的 API
        for (String pattern : NO_CACHE_PATTERNS) {
            if (path.startsWith(pattern)) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                return true;
            }
        }

        // 查找匹配的缓存配置
        for (Map.Entry<String, CacheConfig> entry : CACHE_CONFIGS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                CacheConfig config = entry.getValue();
                setCacheHeaders(response, config);
                return true;
            }
        }

        // 默认：私有短缓存
        response.setHeader("Cache-Control", "private, max-age=30");
        return true;
    }

    private void setCacheHeaders(HttpServletResponse response, CacheConfig config) {
        StringBuilder cacheControl = new StringBuilder();
        
        if (config.isPublic) {
            // 公共数据可以被代理缓存
            cacheControl.append("public, ");
        } else {
            // 私有数据只能被浏览器缓存
            cacheControl.append("private, ");
        }
        
        cacheControl.append("max-age=").append(config.maxAge);
        
        if (config.staleWhileRevalidate > 0) {
            // 允许在后台重新验证时使用过期数据
            cacheControl.append(", stale-while-revalidate=").append(config.staleWhileRevalidate);
        }
        
        response.setHeader("Cache-Control", cacheControl.toString());
        
        // 添加 Vary 头，确保不同用户的缓存隔离
        response.setHeader("Vary", "Authorization, Accept-Language");
    }

    /**
     * 缓存配置
     */
    private static class CacheConfig {
        final int maxAge;                    // 最大缓存时间（秒）
        final int staleWhileRevalidate;      // 过期后仍可使用的时间（秒）
        final boolean isPublic;              // 是否为公共缓存

        CacheConfig(int maxAge, boolean isPublic) {
            this.maxAge = maxAge;
            this.staleWhileRevalidate = 0;
            this.isPublic = isPublic;
        }

        CacheConfig(int maxAge, int staleWhileRevalidate) {
            this.maxAge = maxAge;
            this.staleWhileRevalidate = staleWhileRevalidate;
            this.isPublic = false;  // 业务数据默认私有
        }
    }
}
