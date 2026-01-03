package com.lawfirm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class LawFirmApplication {

    public static void main(String[] args) {
        SpringApplication.run(LawFirmApplication.class, args);
        System.out.println("""
            ╔════════════════════════════════════════════════════════════╗
            ║                                                            ║
            ║          智慧律所管理系统启动成功！                          ║
            ║          Law Firm Management System Started                ║
            ║                                                            ║
            ║          API文档: http://localhost:8080/api/swagger-ui     ║
            ║                                                            ║
            ╚════════════════════════════════════════════════════════════╝
            """);
    }
}

