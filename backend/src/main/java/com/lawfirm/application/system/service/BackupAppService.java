package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.BackupCommand;
import com.lawfirm.application.system.command.RestoreCommand;
import com.lawfirm.application.system.dto.BackupDTO;
import com.lawfirm.application.system.dto.BackupQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Backup;
import com.lawfirm.domain.system.repository.BackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 系统备份应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupAppService {

    private final BackupRepository backupRepository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${law-firm.backup.path:./backups}")
    private String backupBasePath;

    /**
     * 分页查询备份记录
     */
    public PageResult<BackupDTO> listBackups(BackupQueryDTO query) {
        LambdaQueryWrapper<Backup> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getBackupType())) {
            wrapper.eq(Backup::getBackupType, query.getBackupType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Backup::getStatus, query.getStatus());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(Backup::getBackupTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(Backup::getBackupTime, query.getEndTime());
        }
        
        wrapper.orderByDesc(Backup::getBackupTime);

        Page<Backup> page = backupRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<BackupDTO> dtos = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取备份详情
     */
    public BackupDTO getBackupById(Long id) {
        Backup backup = backupRepository.getByIdOrThrow(id, "备份记录不存在");
        return toDTO(backup);
    }

    /**
     * 创建备份
     */
    @Transactional
    public BackupDTO createBackup(BackupCommand command) {
        // 生成备份编号
        String backupNo = generateBackupNo();
        
        // 创建备份记录
        Backup backup = Backup.builder()
                .backupNo(backupNo)
                .backupType(command.getBackupType())
                .backupName(generateBackupName(command.getBackupType()))
                .status("PENDING")
                .backupTime(LocalDateTime.now())
                .description(command.getDescription())
                .createdBy(SecurityUtils.getUserId())
                .build();
        
        backupRepository.save(backup);

        // 异步执行备份
        executeBackupAsync(backup);

        return toDTO(backup);
    }

    /**
     * 异步执行备份
     */
    @Async
    public void executeBackupAsync(Backup backup) {
        try {
            backup.setStatus("PENDING");
            backupRepository.updateById(backup);

            String backupPath = null;
            long fileSize = 0;

            switch (backup.getBackupType()) {
                case "DATABASE":
                case "FULL":
                    backupPath = backupDatabase(backup);
                    break;
                case "FILE":
                    backupPath = backupFiles(backup);
                    break;
                default:
                    throw new BusinessException("不支持的备份类型: " + backup.getBackupType());
            }

            // 更新备份记录
            if (backupPath != null) {
                File backupFile = new File(backupPath);
                if (backupFile.exists()) {
                    fileSize = backupFile.length();
                    backup.setBackupPath(backupPath);
                    backup.setFileSize(fileSize);
                    backup.setStatus("SUCCESS");
                } else {
                    backup.setStatus("FAILED");
                    backup.setDescription(backup.getDescription() + " - 备份文件未生成");
                }
            } else {
                backup.setStatus("FAILED");
                backup.setDescription(backup.getDescription() + " - 备份失败");
            }

            backupRepository.updateById(backup);
            log.info("备份完成: backupNo={}, status={}, fileSize={}", 
                    backup.getBackupNo(), backup.getStatus(), fileSize);

        } catch (Exception e) {
            log.error("备份执行失败: backupNo={}", backup.getBackupNo(), e);
            backup.setStatus("FAILED");
            backup.setDescription(backup.getDescription() + " - " + e.getMessage());
            backupRepository.updateById(backup);
        }
    }

    /**
     * 备份数据库
     */
    private String backupDatabase(Backup backup) throws IOException, InterruptedException {
        // 解析数据库连接信息
        String dbName = extractDatabaseName(dbUrl);
        String host = extractHost(dbUrl);
        String port = extractPort(dbUrl);

        // 创建备份目录
        Path backupDir = Paths.get(backupBasePath, "database");
        Files.createDirectories(backupDir);

        // 生成备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("backup_%s_%s.sql", backup.getBackupNo(), timestamp);
        Path backupFile = backupDir.resolve(fileName);

        // 执行pg_dump命令
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", dbUsername,
                "-d", dbName,
                "-F", "c",  // 自定义格式
                "-f", backupFile.toString()
        );

        // 设置环境变量（密码）
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        
        // 读取输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("pg_dump: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new BusinessException("数据库备份失败，退出码: " + exitCode);
        }

        return backupFile.toString();
    }

    /**
     * 备份文件（MinIO文件列表备份）
     */
    private String backupFiles(Backup backup) throws IOException {
        // 创建备份目录
        Path backupDir = Paths.get(backupBasePath, "files");
        Files.createDirectories(backupDir);

        // 生成备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("files_backup_%s_%s.json", backup.getBackupNo(), timestamp);
        Path backupFile = backupDir.resolve(fileName);

        // 这里应该调用MinIO服务获取文件列表并备份
        // 目前先创建一个占位文件
        try (FileWriter writer = new FileWriter(backupFile.toFile())) {
            writer.write("{\n");
            writer.write("  \"backupNo\": \"" + backup.getBackupNo() + "\",\n");
            writer.write("  \"backupTime\": \"" + LocalDateTime.now() + "\",\n");
            writer.write("  \"note\": \"文件备份功能需要集成MinIO服务\"\n");
            writer.write("}\n");
        }

        return backupFile.toString();
    }

    /**
     * 恢复备份
     */
    @Transactional
    public void restoreBackup(RestoreCommand command) {
        Backup backup = backupRepository.getByIdOrThrow(command.getBackupId(), "备份记录不存在");
        
        if (!"SUCCESS".equals(backup.getStatus())) {
            throw new BusinessException("只能恢复成功的备份");
        }

        if (backup.getBackupPath() == null || !new File(backup.getBackupPath()).exists()) {
            throw new BusinessException("备份文件不存在");
        }

        try {
            // 根据备份类型执行恢复
            if ("DATABASE".equals(backup.getBackupType()) || "FULL".equals(backup.getBackupType())) {
                restoreDatabase(backup);
            } else if ("FILE".equals(backup.getBackupType())) {
                restoreFiles(backup);
            } else {
                throw new BusinessException("不支持的备份类型: " + backup.getBackupType());
            }

            // 更新恢复时间
            backup.setRestoreTime(LocalDateTime.now());
            backupRepository.updateById(backup);

            log.info("备份恢复完成: backupNo={}", backup.getBackupNo());

        } catch (Exception e) {
            log.error("备份恢复失败: backupNo={}", backup.getBackupNo(), e);
            throw new BusinessException("备份恢复失败: " + e.getMessage());
        }
    }

    /**
     * 恢复数据库
     */
    private void restoreDatabase(Backup backup) throws IOException, InterruptedException {
        String dbName = extractDatabaseName(dbUrl);
        String host = extractHost(dbUrl);
        String port = extractPort(dbUrl);

        // 执行pg_restore命令
        ProcessBuilder pb = new ProcessBuilder(
                "pg_restore",
                "-h", host,
                "-p", port,
                "-U", dbUsername,
                "-d", dbName,
                "-c",  // 清理数据库
                backup.getBackupPath()
        );

        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        
        // 读取输出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("pg_restore: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new BusinessException("数据库恢复失败，退出码: " + exitCode);
        }
    }

    /**
     * 恢复文件
     */
    private void restoreFiles(Backup backup) {
        // 文件恢复需要集成MinIO服务
        log.warn("文件恢复功能需要集成MinIO服务: backupNo={}", backup.getBackupNo());
    }

    /**
     * 删除备份
     */
    @Transactional
    public void deleteBackup(Long id) {
        Backup backup = backupRepository.getByIdOrThrow(id, "备份记录不存在");
        
        // 删除备份文件
        if (backup.getBackupPath() != null) {
            File backupFile = new File(backup.getBackupPath());
            if (backupFile.exists()) {
                boolean deleted = backupFile.delete();
                if (!deleted) {
                    log.warn("删除备份文件失败: {}", backup.getBackupPath());
                }
            }
        }

        // 软删除备份记录
        backupRepository.softDelete(id);
        log.info("备份已删除: backupNo={}", backup.getBackupNo());
    }

    /**
     * 下载备份文件
     */
    public Resource downloadBackup(Long id) {
        Backup backup = backupRepository.getByIdOrThrow(id, "备份记录不存在");
        
        if (!"SUCCESS".equals(backup.getStatus())) {
            throw new BusinessException("只能下载成功的备份");
        }

        if (backup.getBackupPath() == null) {
            throw new BusinessException("备份文件路径不存在");
        }

        File backupFile = new File(backup.getBackupPath());
        if (!backupFile.exists()) {
            throw new BusinessException("备份文件不存在");
        }

        return new FileSystemResource(backupFile);
    }

    // ========== 工具方法 ==========

    /**
     * 生成备份编号
     */
    private String generateBackupNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BK" + timestamp + random;
    }

    /**
     * 生成备份名称
     */
    private String generateBackupName(String backupType) {
        String typeName = switch (backupType) {
            case "FULL" -> "全量备份";
            case "INCREMENTAL" -> "增量备份";
            case "DATABASE" -> "数据库备份";
            case "FILE" -> "文件备份";
            default -> "备份";
        };
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return typeName + "_" + timestamp;
    }

    /**
     * 从JDBC URL提取数据库名
     */
    private String extractDatabaseName(String jdbcUrl) {
        // jdbc:postgresql://host:port/database
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash == -1) return "law_firm_dev";
        String dbPart = jdbcUrl.substring(lastSlash + 1);
        int questionMark = dbPart.indexOf('?');
        return questionMark == -1 ? dbPart : dbPart.substring(0, questionMark);
    }

    /**
     * 从JDBC URL提取主机
     */
    private String extractHost(String jdbcUrl) {
        // jdbc:postgresql://host:port/database
        int start = jdbcUrl.indexOf("//") + 2;
        int end = jdbcUrl.indexOf(':', start);
        if (end == -1) end = jdbcUrl.indexOf('/', start);
        return end == -1 ? "localhost" : jdbcUrl.substring(start, end);
    }

    /**
     * 从JDBC URL提取端口
     */
    private String extractPort(String jdbcUrl) {
        // jdbc:postgresql://host:port/database
        int colon = jdbcUrl.indexOf(':', jdbcUrl.indexOf("//") + 2);
        if (colon == -1) return "5432";
        int end = jdbcUrl.indexOf('/', colon);
        return end == -1 ? "5432" : jdbcUrl.substring(colon + 1, end);
    }

    /**
     * Entity转DTO
     */
    private BackupDTO toDTO(Backup backup) {
        BackupDTO dto = new BackupDTO();
        BeanUtils.copyProperties(backup, dto);
        return dto;
    }
}

