package com.lawfirm.infrastructure.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 超时配置
 *
 * <p>包含： 1. HTTP 客户端超时配置 2. RestTemplate 超时配置
 *
 * @author junyuzhan
 */
@Slf4j
@Configuration
public class TimeoutConfig {

  /** 连接超时时间（毫秒）. */
  private static final int CONNECT_TIMEOUT = 10_000; // 10秒

  /** 读取超时时间（毫秒）. */
  private static final int READ_TIMEOUT = 30_000; // 30秒

  /** 长超时连接时间（毫秒）. */
  private static final int LONG_CONNECT_TIMEOUT = 30_000;

  /** 长超时读取时间（毫秒）. */
  private static final int LONG_READ_TIMEOUT = 120_000;

  /** 短超时连接时间（毫秒）. */
  private static final int SHORT_CONNECT_TIMEOUT = 3_000;

  /** 短超时读取时间（毫秒）. */
  private static final int SHORT_READ_TIMEOUT = 5_000;

  /**
   * 配置默认的 RestTemplate.
   *
   * <p>使用说明： - 连接超时：10秒 - 读取超时：30秒 - 适用于一般的 HTTP 调用
   *
   * @param builder RestTemplate构建器
   * @return RestTemplate实例
   */
  @Bean
  public RestTemplate restTemplate(final RestTemplateBuilder builder) {
    RestTemplate restTemplate =
        builder
            .setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
            .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
            .build();

    log.info(
        "RestTemplate 配置完成: connectTimeout={}ms, readTimeout={}ms", CONNECT_TIMEOUT, READ_TIMEOUT);

    return restTemplate;
  }

  /**
   * 配置长时间操作的 RestTemplate.
   *
   * <p>适用场景： - AI 接口调用（可能需要较长时间） - 大文件下载 - 外部 API 调用
   *
   * @return RestTemplate实例
   */
  @Bean("longTimeoutRestTemplate")
  public RestTemplate longTimeoutRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(LONG_CONNECT_TIMEOUT); // 30秒连接超时
    factory.setReadTimeout(LONG_READ_TIMEOUT); // 2分钟读取超时

    RestTemplate restTemplate = new RestTemplate(factory);

    log.info("长超时 RestTemplate 配置完成: connectTimeout=30s, readTimeout=120s");

    return restTemplate;
  }

  /**
   * 配置短超时的 RestTemplate.
   *
   * <p>适用场景： - 健康检查 - 快速验证 - 内部服务调用
   *
   * @return RestTemplate实例
   */
  @Bean("shortTimeoutRestTemplate")
  public RestTemplate shortTimeoutRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(SHORT_CONNECT_TIMEOUT); // 3秒连接超时
    factory.setReadTimeout(SHORT_READ_TIMEOUT); // 5秒读取超时

    RestTemplate restTemplate = new RestTemplate(factory);

    log.info("短超时 RestTemplate 配置完成: connectTimeout=3s, readTimeout=5s");

    return restTemplate;
  }
}
