package com.lawfirm.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境安全配置验证器
 * 
 * 在系统启动时检查关键安全配置，防止使用不安全的默认值部署到生产环境。
 * 
 * 检查项目：
 * 1. 数据库密码 - 不能使用默认开发密码
 * 2. JWT密钥 - 必须设置强随机密钥
 * 3. MinIO密钥 - 不能使用默认的 minioadmin
 */
@Slf4j
@Component
@Order(1)  // 最先执行，确保在其他组件初始化前检查
public class SecurityConfigValidator implements ApplicationRunner {

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${minio.access-key:}")
    private String minioAccessKey;

    @Value("${minio.secret-key:}")
    private String minioSecretKey;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // 不安全的默认值列表
    private static final List<String> UNSAFE_DB_PASSWORDS = List.of(
            "dev_password_123",
            "password",
            "123456",
            "root",
            "admin"
    );

    private static final List<String> UNSAFE_JWT_SECRETS = List.of(
            "your-256-bit-secret-key-here-change-in-production",
            "secret",
            "jwt-secret",
            "your-secret-key"
    );

    @Override
    public void run(ApplicationArguments args) {
        boolean isProduction = "prod".equalsIgnoreCase(activeProfile) 
                || "production".equalsIgnoreCase(activeProfile);

        if (!isProduction) {
            log.info("当前环境: {} - 跳过生产环境安全检查", activeProfile);
            return;
        }

        log.info("========================================");
        log.info("🔒 生产环境安全配置检查");
        log.info("========================================");

        List<String> errors = new ArrayList<>();

        // 1. 检查数据库密码
        if (dbPassword == null || dbPassword.isEmpty()) {
            errors.add("❌ 数据库密码未设置，请通过环境变量 DB_PASSWORD 配置");
        } else if (UNSAFE_DB_PASSWORDS.stream().anyMatch(p -> p.equalsIgnoreCase(dbPassword))) {
            errors.add("❌ 数据库密码使用了不安全的默认值，请通过环境变量 DB_PASSWORD 设置强密码");
        }

        // 2. 检查JWT密钥
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            errors.add("❌ JWT密钥未设置，请通过环境变量 JWT_SECRET 配置");
        } else if (UNSAFE_JWT_SECRETS.stream().anyMatch(s -> jwtSecret.toLowerCase().contains(s.toLowerCase()))) {
            errors.add("❌ JWT密钥使用了不安全的默认值，请通过环境变量 JWT_SECRET 设置强随机密钥");
            errors.add("   💡 生成命令: openssl rand -base64 32");
        } else if (jwtSecret.length() < 32) {
            errors.add("❌ JWT密钥长度不足（至少32字符/256位），请设置更强的密钥");
        }

        // 3. 检查MinIO密钥
        if ("minioadmin".equalsIgnoreCase(minioAccessKey) || "minioadmin".equalsIgnoreCase(minioSecretKey)) {
            errors.add("❌ MinIO使用了默认密钥 minioadmin，请通过环境变量 MINIO_ACCESS_KEY 和 MINIO_SECRET_KEY 配置");
        }

        // 输出检查结果
        if (!errors.isEmpty()) {
            log.error("========================================");
            log.error("🚨 生产环境配置检查失败！发现 {} 个安全问题：", errors.size());
            log.error("========================================");
            for (String error : errors) {
                log.error(error);
            }
            log.error("========================================");
            log.error("⚠️  使用不安全的配置可能导致数据泄露、系统被入侵等严重后果");
            log.error("⚠️  请设置正确的环境变量后重新启动");
            log.error("========================================");
            
            // 生产环境强制阻止启动
            throw new IllegalStateException("生产环境配置不安全，系统拒绝启动。请修复上述安全问题后重试。");
        }

        log.info("✅ 数据库密码 - 已正确配置");
        log.info("✅ JWT密钥 - 已正确配置（长度: {}）", jwtSecret.length());
        log.info("✅ MinIO密钥 - 已正确配置");
        log.info("========================================");
        log.info("🎉 生产环境安全配置检查通过！");
        log.info("========================================");
    }
}

