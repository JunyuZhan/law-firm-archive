package com.lawfirm.infrastructure.config;

import com.lawfirm.common.util.VersionUtils;
import com.lawfirm.infrastructure.notification.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 应用启动时显示版本信息
 */
@Slf4j
@Component
public class VersionInfoRunner implements ApplicationRunner {

    private final Environment environment;
    private final AlertService alertService;

    public VersionInfoRunner(Environment environment, AlertService alertService) {
        this.environment = environment;
        this.alertService = alertService;
    }

    @Override
    public void run(ApplicationArguments args) {
        VersionUtils.VersionInfo versionInfo = VersionUtils.getVersionInfo();
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";
        
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        log.info("");
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                               ║");
        log.info("║                   智慧律所管理系统启动成功                                      ║");
        log.info("║              Law Firm Management System Started Successfully                  ║");
        log.info("║                                                                               ║");
        log.info("║   版本号 (Version)    : {}                                        ║", String.format("%-50s", versionInfo.getVersion()));
        log.info("║   构建时间 (Build Time): {}                                        ║", String.format("%-50s", versionInfo.getBuildTime()));
        log.info("║   Git 提交 (Commit)    : {}                                        ║", String.format("%-50s", versionInfo.getGitCommit()));
        log.info("║   运行环境 (Profile)   : {}                                        ║", String.format("%-50s", profile));
        log.info("║   启动时间 (Start Time): {}                                        ║", String.format("%-50s", startTime));
        log.info("║                                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.info("");
        
        // 同时在控制台输出（用于 Docker 日志查看）
        System.out.println("");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                               ║");
        System.out.println("║                   智慧律所管理系统启动成功                                      ║");
        System.out.println("║              Law Firm Management System Started Successfully                  ║");
        System.out.println("║                                                                               ║");
        System.out.printf("║   版本号 (Version)    : %-50s║%n", versionInfo.getVersion());
        System.out.printf("║   构建时间 (Build Time): %-50s║%n", versionInfo.getBuildTime());
        System.out.printf("║   Git 提交 (Commit)    : %-50s║%n", versionInfo.getGitCommit());
        System.out.printf("║   运行环境 (Profile)   : %-50s║%n", profile);
        System.out.printf("║   启动时间 (Start Time): %-50s║%n", startTime);
        System.out.println("║                                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println("");
        
        // 发送服务启动通知邮件
        try {
            alertService.sendServiceStartNotification();
        } catch (Exception e) {
            log.debug("发送服务启动通知失败（可能邮件服务未配置）: {}", e.getMessage());
        }
    }
}

