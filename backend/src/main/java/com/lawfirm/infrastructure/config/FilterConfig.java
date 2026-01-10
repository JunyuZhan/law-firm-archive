package com.lawfirm.infrastructure.config;

import com.lawfirm.infrastructure.filter.TraceIdFilter;
import com.lawfirm.infrastructure.filter.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter 配置类
 * 
 * 注册系统级过滤器：
 * - TraceIdFilter: 请求追踪（优先级最高）
 * - XssFilter: XSS 防护
 * 
 * @author system
 */
@Configuration
public class FilterConfig {

    /**
     * 注册 TraceId 过滤器
     * 优先级最高，确保所有请求都有 traceId
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(0);  // 最高优先级
        return registration;
    }

    /**
     * 注册 XSS 过滤器
     * 防止跨站脚本攻击
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
}

