package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.dto.backup.RestoreExecuteRequest;
import com.archivesystem.dto.backup.RestoreMaintenanceStatus;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.entity.BackupTarget;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.entity.RestoreJob;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BackupJobMapper;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.repository.RestoreJobMapper;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ArchiveIndexService;
import com.archivesystem.service.BackupService;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import com.archivesystem.service.OperationLogService;
import com.archivesystem.service.SmbStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 备份恢复中心服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String MAINTENANCE_MODE_KEY = "system.runtime.maintenance.enabled";
    private static final String RESTORE_MAINTENANCE_REQUIRED_KEY = "system.restore.maintenance.mode";
    private static final String CHECKSUM_ALGORITHM = "SHA-256";

    private final BackupTargetMapper backupTargetMapper;
    private final BackupJobMapper backupJobMapper;
    private final RestoreJobMapper restoreJobMapper;
    private final SysConfigMapper sysConfigMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final ArchiveMapper archiveMapper;
    private final MinioService minioService;
    private final SecretCryptoService secretCryptoService;
    private final ObjectMapper objectMapper;
    private final ConfigService configService;
    private final ArchiveIndexService archiveIndexService;
    private final OperationLogService operationLogService;
    private final SmbStorageService smbStorageService;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Override
    public BackupOverview getOverview() {
        long enabledTargetCount = backupTargetMapper.selectCount(new LambdaQueryWrapper<BackupTarget>()
                .eq(BackupTarget::getEnabled, true));
        long verifiedTargetCount = backupTargetMapper.selectCount(new LambdaQueryWrapper<BackupTarget>()
                .eq(BackupTarget::getEnabled, true)
                .eq(BackupTarget::getVerifyStatus, BackupTarget.VERIFY_SUCCESS));
        long pendingBackupJobs = backupJobMapper.selectCount(new LambdaQueryWrapper<BackupJob>()
                .in(BackupJob::getStatus, BackupJob.STATUS_PENDING, BackupJob.STATUS_RUNNING));
        long recentBackupFailures = backupJobMapper.selectCount(new LambdaQueryWrapper<BackupJob>()
                .eq(BackupJob::getStatus, BackupJob.STATUS_FAILED)
                .ge(BackupJob::getCreatedAt, LocalDateTime.now().minusDays(7)));
        long recentRestoreFailures = restoreJobMapper.selectCount(new LambdaQueryWrapper<RestoreJob>()
                .eq(RestoreJob::getStatus, RestoreJob.STATUS_FAILED)
                .ge(RestoreJob::getCreatedAt, LocalDateTime.now().minusDays(7)));

        return BackupOverview.builder()
                .enabledTargetCount(enabledTargetCount)
                .verifiedTargetCount(verifiedTargetCount)
                .pendingBackupJobs(pendingBackupJobs)
                .recentBackupFailures(recentBackupFailures)
                .recentRestoreFailures(recentRestoreFailures)
                .currentPhase("FOUNDATION")
                .build();
    }

    @Override
    public List<BackupTargetResponse> getTargets() {
        return backupTargetMapper.selectList(new LambdaQueryWrapper<BackupTarget>()
                        .orderByDesc(BackupTarget::getUpdatedAt)
                        .orderByDesc(BackupTarget::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BackupTargetResponse getTarget(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public BackupTargetResponse createTarget(BackupTargetRequest request) {
        validateRequest(request);
        BackupTarget entity = new BackupTarget();
        applyRequest(entity, request, true);
        entity.setVerifyStatus(BackupTarget.VERIFY_PENDING);
        entity.setVerifyMessage("尚未验证");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setCreatedBy(SecurityUtils.getCurrentUserId());
        entity.setUpdatedBy(SecurityUtils.getCurrentUserId());
        backupTargetMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public BackupTargetResponse updateTarget(Long id, BackupTargetRequest request) {
        validateRequest(request);
        BackupTarget entity = getEntity(id);
        applyRequest(entity, request, false);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.getCurrentUserId());
        entity.setVerifyStatus(BackupTarget.VERIFY_PENDING);
        entity.setVerifyMessage("配置已更新，请重新验证");
        backupTargetMapper.updateById(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteTarget(Long id) {
        BackupTarget entity = getEntity(id);
        backupTargetMapper.deleteById(entity.getId());
    }

    @Override
    @Transactional
    public BackupTargetResponse verifyTarget(Long id) {
        BackupTarget entity = getEntity(id);
        VerificationResult result = verify(entity);
        entity.setVerifyStatus(result.success ? BackupTarget.VERIFY_SUCCESS : BackupTarget.VERIFY_FAILED);
        entity.setVerifyMessage(result.message);
        entity.setLastVerifiedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.getCurrentUserId());
        backupTargetMapper.updateById(entity);
        return toResponse(entity);
    }

    @Override
    public List<BackupSetResponse> getBackupSets(Long targetId) {
        List<BackupTarget> targets;
        if (targetId != null) {
            targets = List.of(getEntity(targetId));
        } else {
            targets = backupTargetMapper.selectList(new LambdaQueryWrapper<BackupTarget>()
                    .eq(BackupTarget::getEnabled, true)
                    .orderByDesc(BackupTarget::getUpdatedAt)
                    .orderByDesc(BackupTarget::getId));
        }

        List<BackupSetResponse> backupSets = new ArrayList<>();
        for (BackupTarget target : targets) {
            if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
                backupSets.addAll(readLocalBackupSets(target));
                continue;
            }
            if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
                backupSets.addAll(readSmbBackupSets(target));
                continue;
            }
            throw new BusinessException("不支持的备份目标类型");
        }

        return backupSets.stream()
                .sorted(backupSetComparator())
                .toList();
    }

    @Override
    @Transactional
    public BackupJob runManualBackup(Long targetId) {
        return runBackup(targetId, BackupJob.TRIGGER_MANUAL);
    }

    @Override
    @Transactional
    public BackupJob runScheduledBackup(Long targetId) {
        return runBackup(targetId, BackupJob.TRIGGER_SCHEDULED);
    }

    @Transactional
    protected BackupJob runBackup(Long targetId, String triggerType) {
        BackupTarget target = getEntity(targetId);
        VerificationResult verificationResult = verify(target);
        if (!verificationResult.success) {
            throw new BusinessException("备份目标验证未通过: " + verificationResult.message);
        }

        BackupJob job = BackupJob.builder()
                .backupNo("BK-" + BACKUP_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8))
                .targetId(target.getId())
                .targetName(target.getName())
                .triggerType(triggerType)
                .backupScope("DATABASE,FILES,CONFIG")
                .status(BackupJob.STATUS_RUNNING)
                .startedAt(LocalDateTime.now())
                .operatorId(SecurityUtils.getCurrentUserId())
                .operatorName(firstNonNull(SecurityUtils.getCurrentRealName(), BackupJob.TRIGGER_SCHEDULED.equals(triggerType) ? "SYSTEM_SCHEDULER" : null))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        backupJobMapper.insert(job);

        try {
            executeBackup(target, job);
            cleanupOldBackupSets(target);
            job.setStatus(BackupJob.STATUS_SUCCESS);
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            backupJobMapper.updateById(job);
            return job;
        } catch (Exception e) {
            log.error("执行手动备份失败: targetId={}, backupNo={}", targetId, job.getBackupNo(), e);
            job.setStatus(BackupJob.STATUS_FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            backupJobMapper.updateById(job);
            throw new BusinessException("执行备份失败: " + e.getMessage());
        }
    }

    @Override
    public RestoreMaintenanceStatus getRestoreMaintenanceStatus() {
        boolean enabled = configService.getBooleanValue(MAINTENANCE_MODE_KEY, false);
        boolean restoreRequiresMaintenance = configService.getBooleanValue(RESTORE_MAINTENANCE_REQUIRED_KEY, true);
        return RestoreMaintenanceStatus.builder()
                .enabled(enabled)
                .restoreRequiresMaintenance(restoreRequiresMaintenance)
                .message(enabled ? "系统当前处于维护模式" : "系统当前处于正常服务模式")
                .build();
    }

    @Override
    @Transactional
    public RestoreMaintenanceStatus setRestoreMaintenanceMode(Boolean enabled) {
        boolean maintenanceEnabled = Boolean.TRUE.equals(enabled);
        configService.saveConfig(
                MAINTENANCE_MODE_KEY,
                String.valueOf(maintenanceEnabled),
                SysConfig.GROUP_SYSTEM,
                "系统维护模式开关",
                SysConfig.TYPE_BOOLEAN,
                true,
                74
        );
        operationLogService.log(
                OperationLog.OBJ_SYSTEM,
                "restore-maintenance",
                null,
                OperationLog.OP_UPDATE,
                maintenanceEnabled ? "开启恢复维护模式" : "关闭恢复维护模式",
                Map.of("enabled", maintenanceEnabled)
        );
        return getRestoreMaintenanceStatus();
    }

    @Override
    public RestoreJob runRestore(RestoreExecuteRequest request) {
        validateRestoreRequest(request);
        BackupTarget target = getEntity(request.getTargetId());
        BackupSetResponse backupSet = getBackupSets(target.getId()).stream()
                .filter(item -> request.getBackupSetName().equals(item.getBackupSetName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到指定备份集"));
        ensureRestoreReady(backupSet);
        ensureMaintenanceMode();

        RestoreJob restoreJob = RestoreJob.builder()
                .restoreNo("RS-" + BACKUP_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().substring(0, 8))
                .sourceType(target.getTargetType())
                .targetId(target.getId())
                .targetName(target.getName())
                .backupSetName(backupSet.getBackupSetName())
                .status(RestoreJob.STATUS_RUNNING)
                .verifyStatus(backupSet.getVerifyStatus())
                .restoredDatabase(false)
                .restoredFiles(false)
                .restoredConfig(false)
                .rebuildIndexStatus("PENDING")
                .restoreReport(null)
                .startedAt(LocalDateTime.now())
                .operatorId(SecurityUtils.getCurrentUserId())
                .operatorName(SecurityUtils.getCurrentRealName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        restoreJobMapper.insert(restoreJob);

        operationLogService.log(
                OperationLog.OBJ_SYSTEM,
                restoreJob.getRestoreNo(),
                null,
                OperationLog.OP_UPDATE,
                "启动系统恢复任务",
                Map.of(
                        "targetId", target.getId(),
                        "backupSetName", backupSet.getBackupSetName(),
                        "restoreDatabase", Boolean.TRUE.equals(request.getRestoreDatabase()),
                        "restoreFiles", Boolean.TRUE.equals(request.getRestoreFiles()),
                        "restoreConfig", Boolean.TRUE.equals(request.getRestoreConfig()),
                        "rebuildIndex", Boolean.TRUE.equals(request.getRebuildIndex())
                )
        );

        PreparedRestoreBackup preparedRestoreBackup = null;
        try {
            preparedRestoreBackup = prepareRestoreBackup(target, backupSet);
            Path backupSetPath = preparedRestoreBackup.backupSetPath();
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("backupSetName", backupSet.getBackupSetName());
            report.put("startedAt", restoreJob.getStartedAt());
            report.put("targetName", target.getName());
            report.put("steps", new ArrayList<Map<String, Object>>());
            if (Boolean.TRUE.equals(request.getRestoreDatabase())) {
                restoreDatabase(backupSetPath, backupSet);
                restoreJob.setRestoredDatabase(true);
                appendReportStep(report, "DATABASE", "SUCCESS", "数据库恢复完成");
            }
            if (Boolean.TRUE.equals(request.getRestoreFiles())) {
                restoreFiles(backupSetPath);
                restoreJob.setRestoredFiles(true);
                appendReportStep(report, "FILES", "SUCCESS", "电子文件回灌完成");
            }
            if (Boolean.TRUE.equals(request.getRestoreConfig())) {
                restoreConfigs(backupSetPath);
                restoreJob.setRestoredConfig(true);
                appendReportStep(report, "CONFIG", "SUCCESS", "系统配置恢复完成");
            }
            if (Boolean.TRUE.equals(request.getRebuildIndex())) {
                archiveIndexService.rebuildAllIndexes();
                restoreJob.setRebuildIndexStatus("SUCCESS");
                appendReportStep(report, "INDEX", "SUCCESS", "检索索引已重建");
            } else {
                restoreJob.setRebuildIndexStatus("SKIPPED");
                appendReportStep(report, "INDEX", "SKIPPED", "未执行索引重建");
            }

            restoreJob.setStatus(RestoreJob.STATUS_SUCCESS);
            restoreJob.setFinishedAt(LocalDateTime.now());
            report.put("finishedAt", restoreJob.getFinishedAt());
            report.put("status", restoreJob.getStatus());
            restoreJob.setRestoreReport(objectMapper.writeValueAsString(report));
            restoreJob.setUpdatedAt(LocalDateTime.now());
            restoreJobMapper.updateById(restoreJob);

            if (Boolean.TRUE.equals(request.getExitMaintenanceAfterSuccess())) {
                setRestoreMaintenanceMode(false);
            }

            operationLogService.log(
                    OperationLog.OBJ_SYSTEM,
                    restoreJob.getRestoreNo(),
                    null,
                    OperationLog.OP_UPDATE,
                    "系统恢复任务完成",
                    Map.of("status", restoreJob.getStatus(), "backupSetName", restoreJob.getBackupSetName())
            );
            cleanupPreparedRestoreBackup(preparedRestoreBackup);
            return restoreJob;
        } catch (Exception e) {
            cleanupPreparedRestoreBackup(preparedRestoreBackup);
            log.error("执行恢复失败: restoreNo={}", restoreJob.getRestoreNo(), e);
            restoreJob.setStatus(RestoreJob.STATUS_FAILED);
            restoreJob.setErrorMessage(e.getMessage());
            restoreJob.setFinishedAt(LocalDateTime.now());
            restoreJob.setUpdatedAt(LocalDateTime.now());
            restoreJob.setRebuildIndexStatus("FAILED");
            restoreJob.setRestoreReport(buildFailureReport(restoreJob, e));
            restoreJobMapper.updateById(restoreJob);

            operationLogService.log(
                    OperationLog.OBJ_SYSTEM,
                    restoreJob.getRestoreNo(),
                    null,
                    OperationLog.OP_UPDATE,
                    "系统恢复任务失败",
                    Map.of("errorMessage", e.getMessage(), "backupSetName", restoreJob.getBackupSetName())
            );
            throw new BusinessException("执行恢复失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<BackupJob> getBackupJobs(Integer pageNum, Integer pageSize) {
        Page<BackupJob> page = new Page<>(pageNum, pageSize);
        Page<BackupJob> result = backupJobMapper.selectPage(page, new LambdaQueryWrapper<BackupJob>()
                .orderByDesc(BackupJob::getCreatedAt)
                .orderByDesc(BackupJob::getId));
        return PageResult.of(result);
    }

    @Override
    public PageResult<RestoreJob> getRestoreJobs(Integer pageNum, Integer pageSize) {
        Page<RestoreJob> page = new Page<>(pageNum, pageSize);
        Page<RestoreJob> result = restoreJobMapper.selectPage(page, new LambdaQueryWrapper<RestoreJob>()
                .orderByDesc(RestoreJob::getCreatedAt)
                .orderByDesc(RestoreJob::getId));
        return PageResult.of(result);
    }

    private BackupTarget getEntity(Long id) {
        BackupTarget entity = backupTargetMapper.selectById(id);
        if (entity == null) {
            throw NotFoundException.of("备份目标", id);
        }
        return entity;
    }

    private void validateRequest(BackupTargetRequest request) {
        if (BackupTarget.TYPE_LOCAL.equals(request.getTargetType())) {
            if (!StringUtils.hasText(request.getLocalPath())) {
                throw new BusinessException("本地目录备份目标必须填写目录路径");
            }
        } else if (BackupTarget.TYPE_SMB.equals(request.getTargetType())) {
            if (!StringUtils.hasText(request.getSmbHost()) || !StringUtils.hasText(request.getSmbShare())) {
                throw new BusinessException("SMB 备份目标必须填写主机和共享名称");
            }
        } else {
            throw new BusinessException("不支持的备份目标类型");
        }
    }

    private void validateRestoreRequest(RestoreExecuteRequest request) {
        if (!"RESTORE".equalsIgnoreCase(trimToNull(request.getConfirmationText()))) {
            throw new BusinessException("恢复确认口令不正确，请输入 RESTORE");
        }
        if (!Boolean.TRUE.equals(request.getRestoreDatabase())
                && !Boolean.TRUE.equals(request.getRestoreFiles())
                && !Boolean.TRUE.equals(request.getRestoreConfig())) {
            throw new BusinessException("至少需要选择一个恢复范围");
        }
    }

    private void ensureRestoreReady(BackupSetResponse backupSet) {
        if (!"READY".equals(backupSet.getVerifyStatus())) {
            throw new BusinessException("备份集结构不完整，不能执行恢复");
        }
    }

    private void ensureMaintenanceMode() {
        boolean restoreRequiresMaintenance = configService.getBooleanValue(RESTORE_MAINTENANCE_REQUIRED_KEY, true);
        boolean maintenanceEnabled = configService.getBooleanValue(MAINTENANCE_MODE_KEY, false);
        if (restoreRequiresMaintenance && !maintenanceEnabled) {
            throw new BusinessException("执行恢复前必须先进入维护模式");
        }
    }

    private void applyRequest(BackupTarget entity, BackupTargetRequest request, boolean creating) {
        entity.setName(request.getName());
        entity.setTargetType(request.getTargetType());
        entity.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        entity.setLocalPath(trimToNull(request.getLocalPath()));
        entity.setSmbHost(trimToNull(request.getSmbHost()));
        entity.setSmbPort(request.getSmbPort() != null ? request.getSmbPort() : 445);
        entity.setSmbShare(trimToNull(request.getSmbShare()));
        entity.setSmbUsername(trimToNull(request.getSmbUsername()));
        entity.setSmbSubPath(trimToNull(request.getSmbSubPath()));
        entity.setRemarks(trimToNull(request.getRemarks()));
        if (StringUtils.hasText(request.getSmbPassword())) {
            entity.setSmbPasswordEncrypted(secretCryptoService.encrypt(request.getSmbPassword()));
        } else if (creating && BackupTarget.TYPE_SMB.equals(request.getTargetType())) {
            entity.setSmbPasswordEncrypted(null);
        }
    }

    private VerificationResult verify(BackupTarget entity) {
        if (BackupTarget.TYPE_LOCAL.equals(entity.getTargetType())) {
            return verifyLocal(entity);
        }
        if (BackupTarget.TYPE_SMB.equals(entity.getTargetType())) {
            return verifySmb(entity);
        }
        return new VerificationResult(false, "未知目标类型");
    }

    private VerificationResult verifyLocal(BackupTarget entity) {
        try {
            Path path = Path.of(entity.getLocalPath());
            Files.createDirectories(path);
            if (!Files.isDirectory(path)) {
                return new VerificationResult(false, "目标路径不是目录");
            }
            if (!Files.isWritable(path)) {
                return new VerificationResult(false, "目标目录不可写");
            }
            return new VerificationResult(true, "本地目录可写，验证通过");
        } catch (Exception e) {
            log.warn("本地备份目录验证失败: {}", entity.getLocalPath(), e);
            return new VerificationResult(false, "本地目录验证失败: " + e.getMessage());
        }
    }

    private VerificationResult verifySmb(BackupTarget entity) {
        if (!StringUtils.hasText(entity.getSmbHost()) || !StringUtils.hasText(entity.getSmbShare())) {
            return new VerificationResult(false, "SMB 主机或共享名称缺失");
        }
        if (!StringUtils.hasText(entity.getSmbUsername()) || !StringUtils.hasText(entity.getSmbPasswordEncrypted())) {
            return new VerificationResult(false, "SMB 凭证未完整配置");
        }
        try {
            smbStorageService.verifyWritable(entity);
            return new VerificationResult(true, "SMB 共享目录可读写，验证通过");
        } catch (Exception e) {
            log.warn("SMB 备份目标校验失败: host={}, share={}", entity.getSmbHost(), entity.getSmbShare(), e);
            return new VerificationResult(false, "SMB 目录验证失败: " + e.getMessage());
        }
    }

    private List<BackupSetResponse> readLocalBackupSets(BackupTarget target) {
        VerificationResult verificationResult = verifyLocal(target);
        if (!verificationResult.success) {
            throw new BusinessException("无法读取备份目录: " + verificationResult.message);
        }
        try (var stream = Files.list(Path.of(target.getLocalPath()))) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> readBackupSet(target, path))
                    .sorted(backupSetComparator())
                    .toList();
        } catch (Exception e) {
            throw new BusinessException("读取备份目录失败: " + e.getMessage());
        }
    }

    private List<BackupSetResponse> readSmbBackupSets(BackupTarget target) {
        VerificationResult verificationResult = verifySmb(target);
        if (!verificationResult.success) {
            throw new BusinessException("无法读取 SMB 备份目录: " + verificationResult.message);
        }
        try {
            return smbStorageService.listDirectories(target).stream()
                    .map(entry -> readSmbBackupSet(target, entry))
                    .sorted(backupSetComparator())
                    .toList();
        } catch (Exception e) {
            throw new BusinessException("读取 SMB 备份目录失败: " + e.getMessage());
        }
    }

    private BackupSetResponse readBackupSet(BackupTarget target, Path backupSetPath) {
        Path manifestPath = backupSetPath.resolve("manifest.json");
        Path checksumsPath = backupSetPath.resolve("checksums.txt");
        boolean hasManifest = Files.exists(manifestPath);
        boolean hasChecksums = Files.exists(checksumsPath);
        Map<String, Object> manifest = hasManifest ? readManifest(manifestPath) : Map.of();

        LocalDateTime createdAt = firstNonNull(parseDateTime(manifest.get("createdAt")), readFileTime(backupSetPath));
        VerificationResult verificationResult = verifyBackupSet(backupSetPath, hasManifest, hasChecksums);

        return BackupSetResponse.builder()
                .targetId(target.getId())
                .targetName(target.getName())
                .targetType(target.getTargetType())
                .backupNo(stringValue(firstNonNull(manifest.get("backupNo"), backupSetPath.getFileName().toString())))
                .backupSetName(backupSetPath.getFileName().toString())
                .backupSetPath(backupSetPath.toString())
                .createdAt(createdAt)
                .databaseMode(stringValue(manifest.get("databaseMode")))
                .fileCount(longValue(manifest.get("fileCount")))
                .objectCount(longValue(manifest.get("objectCount")))
                .totalBytes(longValue(manifest.get("totalBytes")))
                .filesIndex(stringValue(manifest.get("filesIndex")))
                .hasManifest(hasManifest)
                .hasChecksums(hasChecksums)
                .verifyStatus(verificationResult.success ? "READY" : "INCOMPLETE")
                .verifyMessage(verificationResult.message)
                .build();
    }

    private Map<String, Object> readManifest(Path manifestPath) {
        try {
            return objectMapper.readValue(manifestPath.toFile(), Map.class);
        } catch (Exception e) {
            log.warn("读取 manifest 失败: {}", manifestPath, e);
            return Map.of();
        }
    }

    private BackupSetResponse readSmbBackupSet(BackupTarget target, SmbStorageService.RemoteDirectoryEntry entry) {
        String backupSetName = entry.name();
        String manifestRelativePath = backupSetName + "/manifest.json";
        String checksumsRelativePath = backupSetName + "/checksums.txt";
        try {
            boolean hasManifest = smbStorageService.exists(target, manifestRelativePath);
            boolean hasChecksums = smbStorageService.exists(target, checksumsRelativePath);
            Map<String, Object> manifest = hasManifest ? readManifest(target, manifestRelativePath) : Map.of();
            LocalDateTime createdAt = firstNonNull(parseDateTime(manifest.get("createdAt")), entry.modifiedAt());
            VerificationResult verificationResult = verifyBackupSet(target, backupSetName, hasManifest, hasChecksums);

            return BackupSetResponse.builder()
                    .targetId(target.getId())
                    .targetName(target.getName())
                    .targetType(target.getTargetType())
                    .backupNo(stringValue(firstNonNull(manifest.get("backupNo"), backupSetName)))
                    .backupSetName(backupSetName)
                    .backupSetPath(smbStorageService.buildDisplayPath(target, backupSetName))
                    .createdAt(createdAt)
                    .databaseMode(stringValue(manifest.get("databaseMode")))
                    .fileCount(longValue(manifest.get("fileCount")))
                    .objectCount(longValue(manifest.get("objectCount")))
                    .totalBytes(longValue(manifest.get("totalBytes")))
                    .filesIndex(stringValue(manifest.get("filesIndex")))
                    .hasManifest(hasManifest)
                    .hasChecksums(hasChecksums)
                    .verifyStatus(verificationResult.success ? "READY" : "INCOMPLETE")
                    .verifyMessage(verificationResult.message)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("读取 SMB 备份集失败: " + backupSetName + ", " + e.getMessage());
        }
    }

    private Map<String, Object> readManifest(BackupTarget target, String manifestRelativePath) {
        try (InputStream inputStream = smbStorageService.openInputStream(target, manifestRelativePath)) {
            return objectMapper.readValue(inputStream, Map.class);
        } catch (Exception e) {
            log.warn("读取 SMB manifest 失败: {}", manifestRelativePath, e);
            return Map.of();
        }
    }

    private void executeBackup(BackupTarget target, BackupJob job) throws Exception {
        if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
            Path backupSetPath = buildBackupSet(Path.of(target.getLocalPath()), target, job);
            job.setBackupSetPath(backupSetPath.toString());
            return;
        }
        if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
            Path tempRoot = Files.createTempDirectory("archive-smb-backup-");
            try {
                Path backupSetPath = buildBackupSet(tempRoot, target, job);
                smbStorageService.uploadDirectory(target, job.getBackupNo(), backupSetPath);
                job.setBackupSetPath(smbStorageService.buildDisplayPath(target, job.getBackupNo()));
            } finally {
                deleteDirectory(tempRoot);
            }
            return;
        }
        throw new BusinessException("不支持的备份目标类型");
    }

    private Path buildBackupSet(Path rootPath, BackupTarget target, BackupJob job) throws Exception {
        Path backupSetPath = rootPath.resolve(job.getBackupNo());
        Path databaseDir = backupSetPath.resolve("database");
        Path filesDir = backupSetPath.resolve("files");
        Path configDir = backupSetPath.resolve("config");
        Files.createDirectories(databaseDir);
        Files.createDirectories(filesDir);
        Files.createDirectories(configDir);

        Path configJson = exportSystemConfigs(configDir);
        Path backupConfigJson = exportBackupConfig(configDir, target, job);
        DatabaseDumpResult databaseDumpResult = exportDatabaseDump(databaseDir);
        FileBackupResult fileBackupResult = exportDigitalFiles(filesDir);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("backupNo", job.getBackupNo());
        manifest.put("createdAt", LocalDateTime.now().toString());
        manifest.put("scope", List.of("DATABASE", "FILES", "CONFIG"));
        manifest.put("targetName", target.getName());
        manifest.put("databaseFile", databaseDumpResult.fileName());
        manifest.put("databaseMode", databaseDumpResult.mode());
        manifest.put("databaseWarning", databaseDumpResult.warning());
        manifest.put("checksumAlgorithm", CHECKSUM_ALGORITHM);
        manifest.put("configFiles", List.of(configJson.getFileName().toString(), backupConfigJson.getFileName().toString()));
        manifest.put("fileCount", fileBackupResult.fileCount());
        manifest.put("objectCount", fileBackupResult.objectCount());
        manifest.put("totalBytes", fileBackupResult.totalBytes());
        manifest.put("filesIndex", fileBackupResult.indexFileName());
        Path manifestPath = backupSetPath.resolve("manifest.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);

        List<String> checksumLines = buildChecksumLines(backupSetPath);
        Files.writeString(backupSetPath.resolve("checksums.txt"), String.join(System.lineSeparator(), checksumLines),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        job.setFileCount(fileBackupResult.fileCount());
        job.setTotalBytes(fileBackupResult.totalBytes());
        return backupSetPath;
    }

    private void restoreDatabase(Path backupSetPath, BackupSetResponse backupSet) throws Exception {
        if ("PLACEHOLDER".equals(backupSet.getDatabaseMode())) {
            throw new BusinessException("该备份集数据库文件为占位文件，不能执行数据库恢复");
        }
        long archiveCount = archiveMapper.selectCount(new LambdaQueryWrapper<>());
        long digitalFileCount = digitalFileMapper.selectCount(new LambdaQueryWrapper<>());
        if (archiveCount > 0 || digitalFileCount > 0) {
            throw new BusinessException("当前数据库并非空库，已拒绝执行全库恢复");
        }

        Path sqlFile = backupSetPath.resolve("database").resolve("archive-system.sql");
        if (!Files.exists(sqlFile)) {
            throw new BusinessException("未找到数据库备份文件");
        }

        PgRestoreCommand command = buildPsqlRestoreCommand(sqlFile);
        if (command == null) {
            throw new BusinessException("未识别 PostgreSQL 连接信息，无法执行数据库恢复");
        }

        ProcessBuilder builder = new ProcessBuilder(command.command());
        if (StringUtils.hasText(command.password())) {
            builder.environment().put("PGPASSWORD", command.password());
        }
        Path restoreLog = backupSetPath.resolve("database").resolve("restore-output.log");
        builder.redirectOutput(restoreLog.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new BusinessException("psql 恢复失败，请查看 restore-output.log");
        }
    }

    private void restoreFiles(Path backupSetPath) throws Exception {
        Path filesDir = backupSetPath.resolve("files");
        Path indexPath = filesDir.resolve("files-index.json");
        if (!Files.exists(indexPath)) {
            throw new BusinessException("未找到文件索引 files-index.json");
        }
        List<Map<String, Object>> items = objectMapper.readValue(indexPath.toFile(), List.class);
        Set<String> restoredObjects = new HashSet<>();
        for (Map<String, Object> item : items) {
            restoreObjectIfPresent(filesDir, restoredObjects, item.get("storagePath"));
            restoreObjectIfPresent(filesDir, restoredObjects, item.get("convertedPath"));
            restoreObjectIfPresent(filesDir, restoredObjects, item.get("previewPath"));
            restoreObjectIfPresent(filesDir, restoredObjects, item.get("thumbnailPath"));
        }
    }

    private void restoreObjectIfPresent(Path filesDir, Set<String> restoredObjects, Object objectPathValue) throws Exception {
        String objectPath = stringValue(objectPathValue);
        if (!StringUtils.hasText(objectPath) || !restoredObjects.add(objectPath)) {
            return;
        }
        Path localFile = filesDir.resolve(objectPath);
        if (!Files.exists(localFile)) {
            log.warn("恢复文件时未找到对象备份: {}", objectPath);
            return;
        }
        String contentType = Files.probeContentType(localFile);
        try (var inputStream = Files.newInputStream(localFile)) {
            minioService.upload(objectPath, inputStream, Files.size(localFile), contentType);
        }
    }

    private void restoreConfigs(Path backupSetPath) throws Exception {
        Path configPath = backupSetPath.resolve("config").resolve("sys-config.json");
        if (!Files.exists(configPath)) {
            throw new BusinessException("未找到系统配置备份文件");
        }
        List<SysConfig> configs = objectMapper.readValue(
                configPath.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SysConfig.class)
        );
        for (SysConfig config : configs) {
            if (MAINTENANCE_MODE_KEY.equals(config.getConfigKey())) {
                continue;
            }
            configService.saveConfig(
                    config.getConfigKey(),
                    config.getConfigValue(),
                    config.getConfigGroup(),
                    config.getDescription(),
                    config.getConfigType(),
                    config.getEditable(),
                    config.getSortOrder()
            );
        }
    }

    private Path exportSystemConfigs(Path configDir) throws Exception {
        Path output = configDir.resolve("sys-config.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), sysConfigMapper.selectAllOrdered());
        return output;
    }

    private Path exportBackupConfig(Path configDir, BackupTarget target, BackupJob job) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetId", target.getId());
        payload.put("targetName", target.getName());
        payload.put("targetType", target.getTargetType());
        payload.put("backupNo", job.getBackupNo());
        payload.put("datasourceUrl", datasourceUrl);
        payload.put("minioBucket", minioService.getBucketName());
        Path output = configDir.resolve("backup-config.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), payload);
        return output;
    }

    private DatabaseDumpResult exportDatabaseDump(Path databaseDir) throws Exception {
        Path dumpFile = databaseDir.resolve("archive-system.sql");
        PgDumpCommand command = buildPgDumpCommand();
        if (command == null) {
            Files.writeString(dumpFile, "-- pg_dump unavailable in current runtime\n", StandardCharsets.UTF_8);
            return new DatabaseDumpResult(dumpFile.getFileName().toString(), "PLACEHOLDER", "未识别 PostgreSQL 连接信息，已生成占位数据库文件");
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command.command());
            if (StringUtils.hasText(command.password())) {
                builder.environment().put("PGPASSWORD", command.password());
            }
            builder.redirectOutput(dumpFile.toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new DatabaseDumpResult(dumpFile.getFileName().toString(), "PLACEHOLDER", "pg_dump 执行失败，已写入占位文件");
            }
            return new DatabaseDumpResult(dumpFile.getFileName().toString(), "PG_DUMP", null);
        } catch (Exception e) {
            Files.writeString(dumpFile, "-- pg_dump execution error\n" + e.getMessage(),
                    StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            return new DatabaseDumpResult(dumpFile.getFileName().toString(), "PLACEHOLDER", "pg_dump 不可用，已写入失败信息");
        }
    }

    private FileBackupResult exportDigitalFiles(Path filesDir) throws Exception {
        List<DigitalFile> digitalFiles = digitalFileMapper.selectList(new LambdaQueryWrapper<DigitalFile>()
                .eq(DigitalFile::getDeleted, false)
                .orderByAsc(DigitalFile::getId));
        Set<String> objectPaths = new TreeSet<>();
        for (DigitalFile file : digitalFiles) {
            addPath(objectPaths, file.getStoragePath());
            addPath(objectPaths, file.getConvertedPath());
            addPath(objectPaths, file.getPreviewPath());
            addPath(objectPaths, file.getThumbnailPath());
        }

        long totalBytes = 0L;
        for (String objectPath : objectPaths) {
            Path target = filesDir.resolve(objectPath);
            try {
                minioService.downloadToFile(objectPath, target);
                totalBytes += Files.size(target);
            } catch (Exception e) {
                log.warn("备份 MinIO 对象失败: objectPath={}, message={}", objectPath, e.getMessage());
            }
        }

        String indexFileName = "files-index.json";
        Path indexPath = filesDir.resolve(indexFileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), digitalFiles.stream()
                .map(file -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", file.getId());
                    item.put("archiveId", file.getArchiveId());
                    item.put("fileName", file.getFileName());
                    item.put("storagePath", file.getStoragePath());
                    item.put("convertedPath", file.getConvertedPath());
                    item.put("previewPath", file.getPreviewPath());
                    item.put("thumbnailPath", file.getThumbnailPath());
                    item.put("fileSize", file.getFileSize());
                    return item;
                })
                .collect(Collectors.toList()));

        return new FileBackupResult((long) digitalFiles.size(), (long) objectPaths.size(), totalBytes, indexFileName);
    }

    private void addPath(Set<String> paths, String value) {
        if (StringUtils.hasText(value)) {
            paths.add(value.trim());
        }
    }

    private VerificationResult verifyBackupSet(Path backupSetPath, boolean hasManifest, boolean hasChecksums) {
        if (!hasManifest || !hasChecksums) {
            return new VerificationResult(false, "缺少 manifest 或 checksums 文件");
        }
        Path checksumsPath = backupSetPath.resolve("checksums.txt");
        try {
            List<String> lines = Files.readAllLines(checksumsPath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return new VerificationResult(false, "checksums.txt 为空");
            }
            for (String line : lines) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    return new VerificationResult(false, "checksums.txt 格式无效");
                }
                String relativePath = parts[0];
                String expectedHash = parts[1];
                Path target = backupSetPath.resolve(relativePath);
                if (!Files.exists(target)) {
                    return new VerificationResult(false, "缺少备份文件: " + relativePath);
                }
                String actualHash = calculateSha256(target);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    return new VerificationResult(false, "校验失败: " + relativePath);
                }
            }
            return new VerificationResult(true, "备份集结构与 SHA-256 校验通过");
        } catch (Exception e) {
            return new VerificationResult(false, "校验备份集失败: " + e.getMessage());
        }
    }

    private VerificationResult verifyBackupSet(BackupTarget target, String backupSetName, boolean hasManifest, boolean hasChecksums) {
        if (!hasManifest || !hasChecksums) {
            return new VerificationResult(false, "缺少 manifest 或 checksums 文件");
        }
        String checksumsRelativePath = backupSetName + "/checksums.txt";
        try (InputStream checksumsInput = smbStorageService.openInputStream(target, checksumsRelativePath)) {
            List<String> lines = new String(checksumsInput.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            if (lines.isEmpty()) {
                return new VerificationResult(false, "checksums.txt 为空");
            }
            for (String line : lines) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    return new VerificationResult(false, "checksums.txt 格式无效");
                }
                String relativePath = parts[0];
                String expectedHash = parts[1];
                String remoteFilePath = backupSetName + "/" + relativePath;
                if (!smbStorageService.exists(target, remoteFilePath)) {
                    return new VerificationResult(false, "缺少备份文件: " + relativePath);
                }
                try (InputStream fileInput = smbStorageService.openInputStream(target, remoteFilePath)) {
                    String actualHash = calculateSha256(fileInput);
                    if (!expectedHash.equalsIgnoreCase(actualHash)) {
                        return new VerificationResult(false, "校验失败: " + relativePath);
                    }
                }
            }
            return new VerificationResult(true, "备份集结构与 SHA-256 校验通过");
        } catch (Exception e) {
            return new VerificationResult(false, "校验备份集失败: " + e.getMessage());
        }
    }

    private List<String> buildChecksumLines(Path backupSetPath) throws Exception {
        try (var paths = Files.walk(backupSetPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !"checksums.txt".equals(path.getFileName().toString()))
                    .sorted()
                    .map(path -> {
                        String relativePath = backupSetPath.relativize(path).toString().replace('\\', '/');
                        try {
                            return relativePath + "|" + calculateSha256(path);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private String calculateSha256(Path path) throws Exception {
        try (var inputStream = Files.newInputStream(path)) {
            return calculateSha256(inputStream);
        }
    }

    private String calculateSha256(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (inputStream) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (byte b : digest.digest()) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private void cleanupOldBackupSets(BackupTarget target) {
        Integer keepCount = configService.getIntValue("system.backup.keep.count", 7);
        if (keepCount == null || keepCount < 1) {
            keepCount = 7;
        }
        List<BackupSetResponse> backupSets = getBackupSets(target.getId());
        if (backupSets.size() <= keepCount) {
            return;
        }
        backupSets.stream()
                .skip(keepCount)
                .forEach(set -> {
                    try {
                        if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
                            deleteDirectory(Path.of(set.getBackupSetPath()));
                        } else if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
                            smbStorageService.deleteDirectory(target, set.getBackupSetName());
                        }
                    } catch (Exception e) {
                        log.warn("清理历史备份集失败: {}", set.getBackupSetPath(), e);
                    }
                });
    }

    private PreparedRestoreBackup prepareRestoreBackup(BackupTarget target, BackupSetResponse backupSet) throws Exception {
        if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
            return new PreparedRestoreBackup(Path.of(backupSet.getBackupSetPath()), null);
        }
        if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
            Path localBackupSetPath = smbStorageService.downloadDirectoryToTemp(target, backupSet.getBackupSetName());
            return new PreparedRestoreBackup(localBackupSetPath, localBackupSetPath.getParent());
        }
        throw new BusinessException("不支持的恢复源类型");
    }

    private void cleanupPreparedRestoreBackup(PreparedRestoreBackup preparedRestoreBackup) {
        if (preparedRestoreBackup == null || preparedRestoreBackup.cleanupRoot() == null) {
            return;
        }
        try {
            deleteDirectory(preparedRestoreBackup.cleanupRoot());
        } catch (Exception e) {
            log.warn("清理恢复临时目录失败: {}", preparedRestoreBackup.cleanupRoot(), e);
        }
    }

    private void deleteDirectory(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private void appendReportStep(Map<String, Object> report, String step, String status, String message) {
        ((List<Map<String, Object>>) report.get("steps")).add(Map.of(
                "step", step,
                "status", status,
                "message", message,
                "time", LocalDateTime.now().toString()
        ));
    }

    private String buildFailureReport(RestoreJob restoreJob, Exception e) {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("backupSetName", restoreJob.getBackupSetName());
            report.put("startedAt", restoreJob.getStartedAt());
            report.put("finishedAt", LocalDateTime.now());
            report.put("status", RestoreJob.STATUS_FAILED);
            report.put("errorMessage", e.getMessage());
            report.put("rebuildIndexStatus", restoreJob.getRebuildIndexStatus());
            return objectMapper.writeValueAsString(report);
        } catch (Exception ex) {
            return "{\"status\":\"FAILED\",\"errorMessage\":\"" + e.getMessage() + "\"}";
        }
    }

    private LocalDateTime readFileTime(Path path) {
        try {
            FileTime fileTime = Files.getLastModifiedTime(path);
            return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private PgDumpCommand buildPgDumpCommand() {
        if (!StringUtils.hasText(datasourceUrl) || !datasourceUrl.startsWith("jdbc:postgresql://")) {
            return null;
        }
        String raw = datasourceUrl.substring("jdbc:postgresql://".length());
        String[] hostAndDb = raw.split("/", 2);
        if (hostAndDb.length < 2) {
            return null;
        }
        String host = hostAndDb[0];
        String port = "5432";
        if (host.contains(":")) {
            String[] hostPort = host.split(":", 2);
            host = hostPort[0];
            port = hostPort[1];
        }
        String database = hostAndDb[1];
        if (database.contains("?")) {
            database = database.substring(0, database.indexOf('?'));
        }
        return new PgDumpCommand(List.of(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", datasourceUsername,
                "-d", database,
                "--no-owner",
                "--no-privileges"
        ), datasourcePassword);
    }

    private PgRestoreCommand buildPsqlRestoreCommand(Path sqlFile) {
        if (!StringUtils.hasText(datasourceUrl) || !datasourceUrl.startsWith("jdbc:postgresql://")) {
            return null;
        }
        String raw = datasourceUrl.substring("jdbc:postgresql://".length());
        String[] hostAndDb = raw.split("/", 2);
        if (hostAndDb.length < 2) {
            return null;
        }
        String host = hostAndDb[0];
        String port = "5432";
        if (host.contains(":")) {
            String[] hostPort = host.split(":", 2);
            host = hostPort[0];
            port = hostPort[1];
        }
        String database = hostAndDb[1];
        if (database.contains("?")) {
            database = database.substring(0, database.indexOf('?'));
        }
        return new PgRestoreCommand(List.of(
                "psql",
                "-h", host,
                "-p", port,
                "-U", datasourceUsername,
                "-d", database,
                "-v", "ON_ERROR_STOP=1",
                "-f", sqlFile.toAbsolutePath().toString()
        ), datasourcePassword);
    }

    private BackupTargetResponse toResponse(BackupTarget entity) {
        return BackupTargetResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .targetType(entity.getTargetType())
                .enabled(entity.getEnabled())
                .localPath(entity.getLocalPath())
                .smbHost(entity.getSmbHost())
                .smbPort(entity.getSmbPort())
                .smbShare(entity.getSmbShare())
                .smbUsername(entity.getSmbUsername())
                .hasSmbPassword(StringUtils.hasText(entity.getSmbPasswordEncrypted()))
                .smbSubPath(entity.getSmbSubPath())
                .remarks(entity.getRemarks())
                .verifyStatus(entity.getVerifyStatus())
                .verifyMessage(entity.getVerifyMessage())
                .lastVerifiedAt(entity.getLastVerifiedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Comparator<BackupSetResponse> backupSetComparator() {
        return Comparator
                .comparing(BackupSetResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BackupSetResponse::getBackupNo, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private record VerificationResult(boolean success, String message) {
    }

    private record PreparedRestoreBackup(Path backupSetPath, Path cleanupRoot) {
    }

    private record PgDumpCommand(List<String> command, String password) {
    }

    private record PgRestoreCommand(List<String> command, String password) {
    }

    private record DatabaseDumpResult(String fileName, String mode, String warning) {
    }

    private record FileBackupResult(Long fileCount, Long objectCount, Long totalBytes, String indexFileName) {
    }
}
