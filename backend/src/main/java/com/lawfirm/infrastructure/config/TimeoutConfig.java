package com.lawfirm.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 超时配置
 * 
 * 包含：
 * 1. HTTP 客户端超时配置
 * 2. RestTemplate 超时配置
 * 
 * @author Kiro-1
 */
@Slf4j
@Configuration
public class TimeoutConfig {

    /**
     * 连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 10_000;  // 10秒

    /**
     * 读取超时时间（毫秒）
     */
    private static final int READ_TIMEOUT = 30_000;  // 30秒

    /**
     * 写入超时时间（毫秒）
     */
    private static final int WRITE_TIMEOUT = 30_000;  // 30秒

    /**
     * 配置默认的 RestTemplate
     * 
     * 使用说明：
     * - 连接超时：10秒
     * - 读取超时：30秒
     * - 适用于一般的 HTTP 调用
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
                .build();
        
        log.info("RestTemplate 配置完成: connectTimeout={}ms, readTimeout={}ms",
                CONNECT_TIMEOUT, READ_TIMEOUT);
        
        return restTemplate;
    }

    /**
     * 配置长时间操作的 RestTemplate
     * 
     * 适用场景：
     * - AI 接口调用（可能需要较长时间）
     * - 大文件下载
     * - 外部 API 调用
     */
    @Bean("longTimeoutRestTemplate")
    public RestTemplate longTimeoutRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);  // 30秒连接超时
        factory.setReadTimeout(120_000);    // 2分钟读取超时
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        log.info("长超时 RestTemplate 配置完成: connectTimeout=30s, readTimeout=120s");
        
        return restTemplate;
    }

    /**
     * 配置短超时的 RestTemplate
     * 
     * 适用场景：
     * - 健康检查
     * - 快速验证
     * - 内部服务调用
     */
    @Bean("shortTimeoutRestTemplate")
    public RestTemplate shortTimeoutRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);   // 3秒连接超时
        factory.setReadTimeout(5_000);      // 5秒读取超时
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        log.info("短超时 RestTemplate 配置完成: connectTimeout=3s, readTimeout=5s");
        
        return restTemplate;
    }
}

