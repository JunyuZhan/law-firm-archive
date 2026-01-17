package com.lawfirm.infrastructure.config;

import com.lawfirm.infrastructure.interceptor.HttpCacheInterceptor;
import com.lawfirm.infrastructure.interceptor.MaintenanceModeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MaintenanceModeInterceptor maintenanceModeInterceptor;
    private final HttpCacheInterceptor httpCacheInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 维护模式拦截器
        registry.addInterceptor(maintenanceModeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**"
                );
        
        // HTTP 缓存拦截器（提升 API 响应性能）
        registry.addInterceptor(httpCacheInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**"  // 认证接口不需要缓存头
                );
    }
}
