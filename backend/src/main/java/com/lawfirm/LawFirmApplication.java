package com.lawfirm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智慧律所管理系统启动类
 *
 * @author junyuzhan
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.lawfirm.infrastructure.persistence.mapper")
@EnableScheduling
@EnableAsync
@SuppressWarnings("checkstyle:FinalClass") // Spring Boot @Configuration classes cannot be final
public class LawFirmApplication {

  /** 默认构造函数（Spring Boot 需要） */
  public LawFirmApplication() {
    // Spring Boot 需要可见的构造函数来创建代理
  }

  /**
   * 应用入口
   *
   * @param args 启动参数
   */
  public static void main(final String[] args) {
    SpringApplication.run(LawFirmApplication.class, args);
    // 版本信息由 VersionInfoRunner 统一输出
  }
}
