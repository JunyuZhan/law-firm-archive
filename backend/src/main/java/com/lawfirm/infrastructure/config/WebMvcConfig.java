package com.lawfirm.infrastructure.config;

import com.lawfirm.infrastructure.interceptor.HttpCacheInterceptor;
import com.lawfirm.infrastructure.interceptor.MaintenanceModeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置.
 *
 * @author system
 * @since 2026-01-17
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  /** 维护模式拦截器. */
  private final MaintenanceModeInterceptor maintenanceModeInterceptor;

  /** HTTP缓存拦截器. */
  private final HttpCacheInterceptor httpCacheInterceptor;

  /** 预检请求缓存时间（秒）. */
  private static final long MAX_AGE_SECONDS = 3600L;

  /**
   * 添加CORS映射配置.
   *
   * @param registry CORS注册器
   */
  @Override
  public void addCorsMappings(final CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("*") // 允许所有来源（生产环境建议配置具体域名）
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        .allowedHeaders("*")
        .allowCredentials(false) // 如果使用JWT，不需要credentials
        .maxAge(MAX_AGE_SECONDS); // 预检请求缓存时间
  }

  /**
   * 添加拦截器配置.
   *
   * @param registry 拦截器注册器
   */
  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    // 维护模式拦截器
    registry
        .addInterceptor(maintenanceModeInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**");

    // HTTP 缓存拦截器（提升 API 响应性能）
    registry
        .addInterceptor(httpCacheInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns(
            "/api/auth/**" // 认证接口不需要缓存头
            );
  }
}
