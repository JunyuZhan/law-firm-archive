package com.archivesystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 档案管理系统启动类.
 * 
 * <p>独立部署的档案管理系统，用于：
 * <ul>
 *   <li>接收律所管理系统的归档档案</li>
 *   <li>提供档案管理服务（入库、借阅、检索等）</li>
 *   <li>支持多来源档案收集</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.archivesystem.repository")
@EnableAsync
@EnableScheduling
public class ArchiveSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchiveSystemApplication.class, args);
    }
}
