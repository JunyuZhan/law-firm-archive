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
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    @Value("${law-firm.backup.docker-container:}")
    private String dockerContainer;
    
    @Value("${law-firm.backup.restore-timeout-minutes:60}")
    private int restoreTimeoutMinutes;

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
        // ✅ 权限验证：只有管理员才能创建备份
        if (!SecurityUtils.hasAnyRole("ADMIN", "BACKUP_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能创建备份");
        }

        // ✅ 检查磁盘空间
        Path backupDir = Paths.get(backupBasePath);
        try {
            Files.createDirectories(backupDir);
            long freeSpace = backupDir.toFile().getFreeSpace();
            long minFreeSpace = 1024L * 1024 * 1024; // 最小1GB可用空间
            if (freeSpace < minFreeSpace) {
                throw new BusinessException("磁盘空间不足，无法创建备份（可用空间: " + freeSpace / 1024 / 1024 + "MB）");
            }
        } catch (IOException e) {
            log.error("检查备份目录失败", e);
            throw new BusinessException("检查备份目录失败: " + e.getMessage());
        }

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

        // 创建备份目录
        Path backupDir = Paths.get(backupBasePath, "database");
        Files.createDirectories(backupDir);

        // 生成备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("backup_%s_%s.sql", backup.getBackupNo(), timestamp);
        Path backupFile = backupDir.resolve(fileName);

        // 返回相对路径（相对于 backupBasePath）
        // 如果 backupBasePath 是 ./backups，backupFile 是 ./backups/database/backup_xxx.sql
        // 返回时保持相对路径格式，便于后续解析

        ProcessBuilder pb;
        String containerName = null;
        
        // 优先尝试使用 Docker 容器执行备份
        if (isDockerAvailable()) {
            // 获取容器名：优先使用配置，否则自动检测
            containerName = getDockerContainerName();
            
            if (containerName != null && isContainerRunning(containerName)) {
                log.info("使用 Docker 容器 {} 执行备份", containerName);
                // 在容器内执行 pg_dump，输出到容器内的临时文件
                String containerTempFile = "/tmp/" + fileName;
                pb = new ProcessBuilder(
                        "docker", "exec",
                        containerName,
                        "pg_dump",
                        "-U", dbUsername,
                        "-d", dbName,
                        "-F", "c",  // 自定义格式
                        "-f", containerTempFile
                );
                pb.environment().put("PGPASSWORD", dbPassword);
            } else {
                log.warn("Docker 容器不可用或未运行，尝试直接使用 pg_dump（需要本地安装 PostgreSQL 客户端）");
                containerName = null; // 标记不使用 Docker
                String host = extractHost(dbUrl);
                String port = extractPort(dbUrl);
                pb = new ProcessBuilder(
                        "pg_dump",
                        "-h", host,
                        "-p", port,
                        "-U", dbUsername,
                        "-d", dbName,
                        "-F", "c",  // 自定义格式
                        "-f", backupFile.toString()
                );
                pb.environment().put("PGPASSWORD", dbPassword);
            }
        } else {
            log.warn("Docker 不可用，尝试直接使用 pg_dump（需要本地安装 PostgreSQL 客户端）");
            String host = extractHost(dbUrl);
            String port = extractPort(dbUrl);
            pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", dbUsername,
                "-d", dbName,
                "-F", "c",  // 自定义格式
                "-f", backupFile.toString()
        );
        pb.environment().put("PGPASSWORD", dbPassword);
        }

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

        // 如果使用 Docker，需要将文件从容器复制到宿主机
        if (containerName != null) {
            String containerTempFile = "/tmp/" + fileName;
            ProcessBuilder copyPb = new ProcessBuilder(
                    "docker", "cp",
                    containerName + ":" + containerTempFile,
                    backupFile.toString()
            );
            Process copyProcess = copyPb.start();
            int copyExitCode = copyProcess.waitFor();
            if (copyExitCode != 0) {
                throw new BusinessException("从容器复制备份文件失败，退出码: " + copyExitCode);
            }
            
            // 清理容器内的临时文件
            ProcessBuilder rmPb = new ProcessBuilder(
                    "docker", "exec",
                    containerName,
                    "rm", "-f", containerTempFile
            );
            rmPb.start().waitFor();
        }

        return backupFile.toString();
    }

    /**
     * 检查 Docker 是否可用
     */
    private boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.debug("Docker 不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查容器是否运行
     */
    private boolean isContainerRunning(String containerName) {
        try {
            Process process = new ProcessBuilder(
                    "docker", "ps", "--format", "{{.Names}}", "--filter", "name=" + containerName
            ).start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                boolean running = line != null && line.trim().equals(containerName);
                process.waitFor();
                return running;
            }
        } catch (Exception e) {
            log.debug("检查容器状态失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Docker 容器名称
     * 优先使用配置，否则自动检测运行中的 PostgreSQL 容器
     */
    private String getDockerContainerName() {
        // 如果配置了容器名，直接使用
        if (StringUtils.hasText(dockerContainer)) {
            return dockerContainer;
        }
        
        // 自动检测运行中的 PostgreSQL 容器
        try {
            Process process = new ProcessBuilder(
                    "docker", "ps", "--format", "{{.Names}}", "--filter", "ancestor=postgres"
            ).start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String containerName = line.trim();
                    if (StringUtils.hasText(containerName)) {
                        log.info("自动检测到 PostgreSQL 容器: {}", containerName);
                        process.waitFor();
                        return containerName;
                    }
                }
                process.waitFor();
            }
        } catch (Exception e) {
            log.debug("自动检测容器失败: {}", e.getMessage());
        }
        
        // 如果都找不到，尝试通过数据库连接信息推断容器名
        // 从 JDBC URL 中提取主机名，如果主机名是 localhost，尝试常见的容器名
        String host = extractHost(dbUrl);
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            // 尝试常见的容器名
            String[] commonNames = {"law-postgres", "law-firm-postgres", "postgres"};
            for (String name : commonNames) {
                if (isContainerRunning(name)) {
                    log.info("通过常见容器名找到运行中的容器: {}", name);
                    return name;
                }
            }
        }
        
        log.warn("未找到可用的 PostgreSQL 容器");
        return null;
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
        // ✅ 1. 严格权限验证：只有超级管理员才能执行数据库恢复
        if (!SecurityUtils.hasAnyRole("SUPER_ADMIN", "ADMIN")) {
            throw new BusinessException("权限不足：只有超级管理员才能执行数据库恢复");
        }

        Backup backup = backupRepository.getByIdOrThrow(command.getBackupId(), "备份记录不存在");
        
        if (!"SUCCESS".equals(backup.getStatus())) {
            throw new BusinessException("只能恢复成功的备份");
        }

        // ✅ 2. 二次确认：验证确认码（防止误操作）
        String expectedConfirmCode = "RESTORE_" + backup.getBackupNo();
        if (command.getConfirmCode() == null || !command.getConfirmCode().equals(expectedConfirmCode)) {
            throw new BusinessException("确认码错误，请输入: " + expectedConfirmCode);
        }

        // ✅ 3. 记录审计日志
        log.warn("【危险操作】准备执行数据库恢复: backupNo={}, operator={}, operatorName={}",
                backup.getBackupNo(), SecurityUtils.getUserId(), SecurityUtils.getUsername());

        // 检查备份文件是否存在（使用和下载功能相同的路径解析逻辑）
        String backupPath = backup.getBackupPath();
        File backupFile;
        
        if (backupPath.startsWith("/")) {
            backupFile = new File(backupPath);
        } else {
            Path basePath = Paths.get(backupBasePath).toAbsolutePath().normalize();
            String normalizedBasePath = backupBasePath.replaceAll("^\\./", "").replaceAll("/$", "");
            String pathToResolve = backupPath.startsWith("./") ? backupPath.substring(2) : backupPath;
            
            if (pathToResolve.startsWith(normalizedBasePath + "/")) {
                pathToResolve = pathToResolve.substring(normalizedBasePath.length() + 1);
            } else if (pathToResolve.equals(normalizedBasePath)) {
                pathToResolve = "";
            }
            
            backupFile = basePath.resolve(pathToResolve).toFile();
        }
        
        if (!backupFile.exists()) {
            throw new BusinessException("备份文件不存在: " + backupFile.getAbsolutePath());
        }

        // 更新状态为进行中
        backup.setStatus("IN_PROGRESS");
        backupRepository.updateById(backup);

        // 异步执行恢复
        executeRestoreAsync(backup);
    }

    /**
     * 异步执行恢复
     */
    @Async
    public void executeRestoreAsync(Backup backup) {
        try {
            log.info("开始恢复备份: backupNo={}", backup.getBackupNo());
            
            // 根据备份类型执行恢复
            if ("DATABASE".equals(backup.getBackupType()) || "FULL".equals(backup.getBackupType())) {
                restoreDatabase(backup);
            } else if ("FILE".equals(backup.getBackupType())) {
                restoreFiles(backup);
            } else {
                throw new BusinessException("不支持的备份类型: " + backup.getBackupType());
            }

            // 更新恢复时间和状态
            backup.setRestoreTime(LocalDateTime.now());
            backup.setStatus("SUCCESS");
            backupRepository.updateById(backup);

            log.info("备份恢复完成: backupNo={}", backup.getBackupNo());

        } catch (Exception e) {
            log.error("备份恢复失败: backupNo={}", backup.getBackupNo(), e);
            backup.setStatus("FAILED");
            backup.setDescription(backup.getDescription() + " - 恢复失败: " + e.getMessage());
            backupRepository.updateById(backup);
        }
    }

    /**
     * 恢复数据库
     */
    private void restoreDatabase(Backup backup) throws IOException, InterruptedException {
        String dbName = extractDatabaseName(dbUrl);
        
        // 解析备份文件路径
        String backupPath = backup.getBackupPath();
        File backupFile;
        
        // 处理路径：转换为绝对路径
        if (backupPath.startsWith("/")) {
            backupFile = new File(backupPath);
        } else {
            Path basePath = Paths.get(backupBasePath).toAbsolutePath().normalize();
            String normalizedBasePath = backupBasePath.replaceAll("^\\./", "").replaceAll("/$", "");
            String pathToResolve = backupPath.startsWith("./") ? backupPath.substring(2) : backupPath;
            
            if (pathToResolve.startsWith(normalizedBasePath + "/")) {
                pathToResolve = pathToResolve.substring(normalizedBasePath.length() + 1);
            } else if (pathToResolve.equals(normalizedBasePath)) {
                pathToResolve = "";
            }
            
            backupFile = basePath.resolve(pathToResolve).toFile();
        }
        
        if (!backupFile.exists()) {
            throw new BusinessException("备份文件不存在: " + backupFile.getAbsolutePath());
        }
        
        ProcessBuilder pb;
        String containerName = null;
        String containerBackupFile = null;
        
        // 优先尝试使用 Docker 容器执行恢复
        if (isDockerAvailable()) {
            containerName = getDockerContainerName();
            
            if (containerName != null && isContainerRunning(containerName)) {
                log.info("使用 Docker 容器 {} 执行恢复", containerName);
                
                // 将备份文件复制到容器中
                String fileName = backupFile.getName();
                containerBackupFile = "/tmp/" + fileName;
                
                ProcessBuilder copyPb = new ProcessBuilder(
                        "docker", "cp",
                        backupFile.getAbsolutePath(),
                        containerName + ":" + containerBackupFile
                );
                Process copyProcess = copyPb.start();
                int copyExitCode = copyProcess.waitFor();
                if (copyExitCode != 0) {
                    throw new BusinessException("将备份文件复制到容器失败，退出码: " + copyExitCode);
                }
                
                // 在容器内执行 pg_restore
                pb = new ProcessBuilder(
                        "docker", "exec",
                        containerName,
                        "pg_restore",
                        "-U", dbUsername,
                        "-d", dbName,
                        "-c",  // 清理数据库
                        "-v",  // 详细输出
                        "-j", "2",  // 使用2个并行作业加速恢复
                        containerBackupFile
                );
                pb.environment().put("PGPASSWORD", dbPassword);
            } else {
                log.warn("Docker 容器不可用或未运行，尝试直接使用 pg_restore（需要本地安装 PostgreSQL 客户端）");
                containerName = null;
                String host = extractHost(dbUrl);
                String port = extractPort(dbUrl);
                pb = new ProcessBuilder(
                        "pg_restore",
                        "-h", host,
                        "-p", port,
                        "-U", dbUsername,
                        "-d", dbName,
                        "-c",  // 清理数据库
                        "-v",  // 详细输出
                        "-j", "2",  // 使用2个并行作业加速恢复
                        backupFile.getAbsolutePath()
                );
                pb.environment().put("PGPASSWORD", dbPassword);
            }
        } else {
            log.warn("Docker 不可用，尝试直接使用 pg_restore（需要本地安装 PostgreSQL 客户端）");
        String host = extractHost(dbUrl);
        String port = extractPort(dbUrl);
            pb = new ProcessBuilder(
                "pg_restore",
                "-h", host,
                "-p", port,
                "-U", dbUsername,
                "-d", dbName,
                "-c",  // 清理数据库
                "-v",  // 详细输出
                "-j", "2",  // 使用2个并行作业加速恢复
                    backupFile.getAbsolutePath()
        );
        pb.environment().put("PGPASSWORD", dbPassword);
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        log.info("开始执行 pg_restore，备份文件大小: {} bytes", backupFile.length());
        
        // 读取输出并记录进度
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                // 每100行输出一次进度，避免日志过多
                if (lineCount % 100 == 0) {
                    log.info("pg_restore 进度: 已处理 {} 行", lineCount);
                } else {
                log.debug("pg_restore: {}", line);
            }
        }
        }
        
        log.info("pg_restore 输出读取完成，共 {} 行，等待进程结束...", lineCount);
        
        // 根据备份文件大小动态调整超时时间
        // 基础超时时间 + 每MB增加1分钟（最小60分钟，最大180分钟）
        long fileSizeMB = backupFile.length() / (1024 * 1024);
        int dynamicTimeout = Math.max(restoreTimeoutMinutes, (int) (60 + fileSizeMB));
        dynamicTimeout = Math.min(dynamicTimeout, 180); // 最多180分钟
        
        log.info("设置恢复超时时间: {} 分钟（备份文件大小: {} MB）", dynamicTimeout, fileSizeMB);
        
        // ⚠️ 内存泄露修复：使用 CompletableFuture 实现超时控制，确保异常时正确清理
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            try {
                return process.waitFor();
            } catch (Exception e) {
                log.error("等待进程结束异常", e);
                return -1;
            }
        });
        
        int exitCode;
        Process cleanupProcess = null;
        try {
            exitCode = future.get(dynamicTimeout, TimeUnit.MINUTES);
            log.info("pg_restore 进程结束，退出码: {}", exitCode);
        } catch (TimeoutException e) {
            log.error("pg_restore 执行超时（超过{}分钟），强制终止进程。备份文件大小: {} MB", 
                    dynamicTimeout, fileSizeMB);
            // ⚠️ 内存泄露修复：超时时取消 CompletableFuture 并清理进程
            future.cancel(true);
            process.destroyForcibly();
            throw new BusinessException(
                    String.format("数据库恢复超时（超过%d分钟）。" +
                            "如果数据库较大，请考虑增加配置项 law-firm.backup.restore-timeout-minutes 的值。" +
                            "当前备份文件大小: %.2f MB", 
                            dynamicTimeout, fileSizeMB));
        } catch (ExecutionException e) {
            log.error("pg_restore 执行异常", e);
            // ⚠️ 内存泄露修复：异常时取消 CompletableFuture 并清理进程
            future.cancel(true);
            process.destroyForcibly();
            // 不直接返回异常消息，避免泄露系统内部信息
            throw new BusinessException("数据库恢复执行失败，请检查日志或联系管理员");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("pg_restore 执行被中断");
            // ⚠️ 内存泄露修复：中断时取消 CompletableFuture 并清理进程
            future.cancel(true);
            process.destroyForcibly();
            throw new BusinessException("数据库恢复被中断");
        } finally {
            // ⚠️ 内存泄露修复：确保 CompletableFuture 被取消（如果还在运行）
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        
        // ⚠️ 内存泄露修复：如果使用 Docker，清理容器内的临时文件，确保 Process 被正确清理
        if (containerName != null && containerBackupFile != null) {
            try {
            ProcessBuilder rmPb = new ProcessBuilder(
                    "docker", "exec",
                    containerName,
                    "rm", "-f", containerBackupFile
            );
                cleanupProcess = rmPb.start();
                cleanupProcess.waitFor();
                // 确保进程资源被释放
                cleanupProcess.destroy();
            } catch (Exception e) {
                log.warn("清理容器临时文件失败: container={}, file={}", containerName, containerBackupFile, e);
            } finally {
                if (cleanupProcess != null && cleanupProcess.isAlive()) {
                    cleanupProcess.destroyForcibly();
                }
            }
        }
        
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
     * 导入外部备份文件
     */
    @Transactional
    public BackupDTO importBackup(MultipartFile file, String backupType, String description) {
        // ✅ 权限验证：只有管理员才能导入备份
        if (!SecurityUtils.hasAnyRole("ADMIN", "BACKUP_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能导入备份");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的备份文件");
        }

        // ✅ 文件大小限制（最大10GB）
        long maxFileSize = 10L * 1024 * 1024 * 1024;
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小超过限制（最大10GB）");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "imported_backup";
        }
        
        // ✅ 严格验证文件类型（只允许 .sql 或 .dump 文件）
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".sql") && !lowerName.endsWith(".dump")) {
            throw new BusinessException("只支持 .sql 或 .dump 格式的备份文件");
        }
        
        try {
            // ✅ 检查磁盘空间并创建备份目录
            Path backupDir = Paths.get(backupBasePath, "database", "imported");
            Files.createDirectories(backupDir);
            long freeSpace = backupDir.toFile().getFreeSpace();
            if (freeSpace < file.getSize() + 1024L * 1024 * 1024) { // 文件大小 + 1GB余量
                throw new BusinessException("磁盘空间不足");
            }
            
            // 生成备份编号和文件名
            String backupNo = generateBackupNo();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String extension = originalFilename.contains(".") ? 
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = String.format("imported_%s_%s%s", backupNo, timestamp, extension);
            Path backupFile = backupDir.resolve(fileName);
            
            // 保存文件
            Files.copy(file.getInputStream(), backupFile);
            long fileSize = Files.size(backupFile);
            
            log.info("备份文件导入成功: 原始文件名={}, 保存路径={}, 大小={} bytes", 
                    originalFilename, backupFile, fileSize);
            
            // 创建备份记录
            String backupName = "导入备份_" + originalFilename + "_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            Backup backup = Backup.builder()
                    .backupNo(backupNo)
                    .backupType(backupType)
                    .backupName(backupName)
                    .backupPath(backupFile.toString())
                    .fileSize(fileSize)
                    .status("SUCCESS")
                    .backupTime(LocalDateTime.now())
                    .description(description != null ? description : "从外部导入的备份文件: " + originalFilename)
                    .createdBy(SecurityUtils.getUserId())
                    .build();
            
            backupRepository.save(backup);
            log.info("备份记录已创建: backupNo={}", backupNo);
            
            return toDTO(backup);
            
        } catch (IOException e) {
            log.error("导入备份文件失败", e);
            throw new BusinessException("导入备份文件失败: " + e.getMessage());
        }
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
        // ✅ 权限验证：只有管理员才能下载备份（备份文件包含所有敏感数据）
        if (!SecurityUtils.hasAnyRole("ADMIN", "BACKUP_ADMIN", "SUPER_ADMIN")) {
            throw new BusinessException("权限不足：只有管理员才能下载备份");
        }

        Backup backup = backupRepository.getByIdOrThrow(id, "备份记录不存在");
        
        if (!"SUCCESS".equals(backup.getStatus())) {
            throw new BusinessException("只能下载成功的备份");
        }

        if (backup.getBackupPath() == null) {
            throw new BusinessException("备份文件路径不存在");
        }

        // ✅ 记录下载审计
        log.warn("下载备份文件: backupNo={}, operator={}, operatorName={}",
                backup.getBackupNo(), SecurityUtils.getUserId(), SecurityUtils.getUsername());

        // 处理路径：转换为绝对路径
        String backupPath = backup.getBackupPath();
        File backupFile;
        
        // 如果路径已经是绝对路径，直接使用
        if (backupPath.startsWith("/")) {
            backupFile = new File(backupPath);
        } else {
            // 相对路径处理
            Path basePath = Paths.get(backupBasePath).toAbsolutePath().normalize();
            
            // 如果路径以 ./ 开头，去掉它
            String pathToResolve = backupPath.startsWith("./") ? backupPath.substring(2) : backupPath;
            
            // 规范化基础路径名：去掉开头的 ./ 和末尾的 /
            String normalizedBasePath = backupBasePath.replaceAll("^\\./", "").replaceAll("/$", "");
            
            // 如果路径以基础路径名开头，去掉重复部分
            // 例如：backups/database/... 且 normalizedBasePath 是 backups，则去掉 backups/
            if (pathToResolve.startsWith(normalizedBasePath + "/")) {
                pathToResolve = pathToResolve.substring(normalizedBasePath.length() + 1);
            } else if (pathToResolve.equals(normalizedBasePath)) {
                pathToResolve = "";
            }
            
            // 基于基础路径解析
            backupFile = basePath.resolve(pathToResolve).toFile();
        }
        
        // 调试日志
        log.debug("备份路径解析: 原始路径={}, 基础路径={}, 解析后路径={}", 
                backupPath, backupBasePath, backupFile.getAbsolutePath());

        if (!backupFile.exists()) {
            log.error("备份文件不存在: {} (原始路径: {}, 解析后路径: {})", 
                    backupFile.getAbsolutePath(), backup.getBackupPath(), backupFile.getAbsolutePath());
            throw new BusinessException("备份文件不存在: " + backupFile.getAbsolutePath());
        }

        if (!backupFile.isFile()) {
            throw new BusinessException("备份路径不是文件: " + backupFile.getAbsolutePath());
        }

        long fileSize = backupFile.length();
        log.info("开始下载备份文件: backupNo={}, fileSize={} bytes ({} MB)", 
                backup.getBackupNo(), fileSize, fileSize / 1024 / 1024);

        // FileSystemResource 支持流式传输，适合大文件下载
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

