package com.lawfirm.infrastructure.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator 监控配置
 *
 * <p>功能： 1. 应用性能监控（APM） 2. 自定义指标收集 3. @Timed 注解支持
 *
 * <p>使用示例： - @Timed(value = "api.login", description = "登录接口耗时") -
 * meterRegistry.counter("business.matter.created").increment();
 *
 * @author junyuzhan
 */
@Slf4j
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class ActuatorConfig {

  /** 应用名称 */
  @Value("${spring.application.name:law-firm}")
  private String applicationName;

  /**
   * 自定义 MeterRegistry 添加通用标签.
   *
   * @return MeterRegistry定制器
   */
  @Bean
  public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
    return registry ->
        registry
            .config()
            .commonTags(
                Tags.of(
                    "application",
                    applicationName,
                    "env",
                    System.getProperty("spring.profiles.active", "dev")));
  }

  /**
   * 启用 @Timed 注解.
   *
   * <p>使用方式： @Timed(value = "api.method.name", description = "接口描述") public Result methodName() {
   * ... }
   *
   * @param registry MeterRegistry
   * @return TimedAspect实例
   */
  @Bean
  public TimedAspect timedAspect(final MeterRegistry registry) {
    log.info("启用 @Timed 注解支持");
    return new TimedAspect(registry);
  }
}
