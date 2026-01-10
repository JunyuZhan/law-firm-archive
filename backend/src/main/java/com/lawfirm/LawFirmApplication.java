package com.lawfirm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智慧律所管理系统启动类
 * 
 * @author LawFirm Team
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.lawfirm.infrastructure.persistence.mapper")
@EnableScheduling
@EnableAsync
public class LawFirmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LawFirmApplication.class, args);
        // 版本信息由 VersionInfoRunner 统一输出
    }
}

