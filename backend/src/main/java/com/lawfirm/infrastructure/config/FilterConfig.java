package com.lawfirm.infrastructure.config;

import com.lawfirm.infrastructure.filter.ThreadLocalCleanupFilter;
import com.lawfirm.infrastructure.filter.TraceIdFilter;
import com.lawfirm.infrastructure.filter.XssFilter;
import com.lawfirm.infrastructure.security.CallbackSecurityFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 配置类
 *
 * <p>注册系统级过滤器：
 * <ul>
 *   <li>TraceIdFilter: 请求追踪（优先级最高）</li>
 *   <li>XssFilter: XSS 防护</li>
 *   <li>CallbackSecurityFilter: 回调接口安全验证</li>
 *   <li>ThreadLocalCleanupFilter: ThreadLocal 清理（优先级最低）</li>
 * </ul>
 *
 * @author junyuzhan
 */
@Configuration
public class FilterConfig {

  /**
   * 注册 TraceId 过滤器 优先级最高，确保所有请求都有 traceId.
   *
   * @return TraceId 过滤器注册Bean
   */
  @Bean
  public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
    FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new TraceIdFilter());
    registration.addUrlPatterns("/*");
    registration.setName("traceIdFilter");
    registration.setOrder(0); // 最高优先级
    return registration;
  }

  /**
   * 注册 XSS 过滤器 防止跨站脚本攻击.
   *
   * @return XSS 过滤器注册Bean
   */
  @Bean
  public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
    FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new XssFilter());
    registration.addUrlPatterns("/*");
    registration.setName("xssFilter");
    registration.setOrder(1);
    return registration;
  }

  /**
   * 注册回调接口安全过滤器 验证客户服务系统的回调请求 IP 白名单.
   *
   * @param callbackSecurityFilter 回调安全过滤器实例
   * @return 回调安全过滤器注册Bean
   */
  @Bean
  public FilterRegistrationBean<CallbackSecurityFilter> callbackSecurityFilterRegistration(
      final CallbackSecurityFilter callbackSecurityFilter) {
    FilterRegistrationBean<CallbackSecurityFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(callbackSecurityFilter);
    registration.addUrlPatterns("/open/client/*");
    registration.setName("callbackSecurityFilter");
    registration.setOrder(2); // 在 TraceId 和 XSS 过滤器之后，JWT 过滤器之前
    return registration;
  }

  /**
   * 注册 ThreadLocal 清理过滤器.
   * 
   * <p>确保每个请求结束后清理 ThreadLocal 缓存，防止内存泄漏。
   * 优先级最低，确保在所有业务逻辑执行完毕后清理。
   *
   * @return ThreadLocal 清理过滤器注册Bean
   */
  @Bean
  public FilterRegistrationBean<ThreadLocalCleanupFilter> threadLocalCleanupFilterRegistration() {
    FilterRegistrationBean<ThreadLocalCleanupFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new ThreadLocalCleanupFilter());
    registration.addUrlPatterns("/*");
    registration.setName("threadLocalCleanupFilter");
    registration.setOrder(Integer.MAX_VALUE); // 最低优先级，确保最后执行清理
    return registration;
  }
}
