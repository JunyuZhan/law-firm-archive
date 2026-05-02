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
import com.archivesystem.repository.BackupJobMapper;
import com.archivesystem.repository.BackupTargetMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.repository.RestoreJobMapper;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.RuntimeSecretProvider;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.LinkedHashSet;
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

    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final DateTimeFormatter BACKUP_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String MAINTENANCE_MODE_KEY = "system.runtime.maintenance.enabled";
    private static final String RESTORE_MAINTENANCE_REQUIRED_KEY = "system.restore.maintenance.mode";
    private static final String CHECKSUM_ALGORITHM = "SHA-256";
    private static final String LOCAL_VERIFY_FAILURE_PUBLIC_MESSAGE = "本地目录验证失败，请检查目录配置和权限";
    private static final String SMB_VERIFY_FAILURE_PUBLIC_MESSAGE = "SMB 目录验证失败，请检查网络、共享配置和权限";
    private static final String BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE = "校验备份集失败，请检查备份文件完整性";
    private static final String BACKUP_SET_CHECKSUM_COVERAGE_FAILURE_MESSAGE = "checksums.txt 未覆盖全部恢复文件";
    private static final String RESTORE_FAILURE_PUBLIC_MESSAGE = "恢复执行失败，请联系系统管理员查看系统日志";
    private static final Set<String> MANIFEST_SCOPE_VALUES = Set.of("DATABASE", "FILES", "CONFIG");
    private static final Set<String> RESTORE_SKIPPED_CONFIG_KEYS = Set.of(
            MAINTENANCE_MODE_KEY,
            RuntimeSecretProvider.KEY_JWT_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET
    );
    private static final Set<String> EXPORT_SKIPPED_CONFIG_KEYS = Set.of(
            RuntimeSecretProvider.KEY_JWT_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET
    );
    private static final Set<String> PRESERVED_DATABASE_TABLES = Set.of(
            "arc_backup_job",
            "arc_backup_target",
            "arc_operation_log",
            "arc_restore_job",
            "sys_api_key",
            "sys_config",
            "sys_ip_blacklist",
            "sys_role",
            "sys_security_audit",
            "sys_user",
            "sys_user_role"
    );

    private final BackupTargetMapper backupTargetMapper;
    private final BackupJobMapper backupJobMapper;
    private final RestoreJobMapper restoreJobMapper;
    private final SysConfigMapper sysConfigMapper;
    private final DigitalFileMapper digitalFileMapper;
    private final MinioService minioService;
    private final SecretCryptoService secretCryptoService;
    private final ObjectMapper objectMapper;
    private final ConfigService configService;
    private final ArchiveIndexService archiveIndexService;
    private final OperationLogService operationLogService;
    private final SmbStorageService smbStorageService;
    private final JdbcTemplate jdbcTemplate;

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
                .currentPhase(determineCurrentPhase(enabledTargetCount, verifiedTargetCount, pendingBackupJobs))
                .build();
    }

    private String determineCurrentPhase(long enabledTargetCount, long verifiedTargetCount, long pendingBackupJobs) {
        if (pendingBackupJobs > 0) {
            return "RUNNING";
        }
        if (enabledTargetCount < 1) {
            return "SETUP_REQUIRED";
        }
        if (verifiedTargetCount < enabledTargetCount) {
            return "VERIFY_REQUIRED";
        }
        return "READY";
    }

    @Override
    public List<BackupTargetResponse> getTargets() {
        return backupTargetMapper.selectList(new LambdaQueryWrapper<BackupTarget>()
                        .orderByDesc(BackupTarget::getUpdatedAt)
                        .orderByDesc(BackupTarget::getId))
                .stream()
                .map(entity -> toResponse(entity, false))
                .toList();
    }

    @Override
    public BackupTargetResponse getTarget(Long id) {
        return toResponse(getEntity(id), true);
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
        return toResponse(entity, true);
    }

    @Override
    @Transactional
    public BackupTargetResponse updateTarget(Long id, BackupTargetRequest request) {
        BackupTarget entity = getEntity(id);
        ensureNoRunningBackupJob(entity.getId());
        ensureNoRunningRestoreJobForTarget(entity.getId());
        applyRequest(entity, request, false);
        validateTarget(entity);
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.getCurrentUserId());
        entity.setVerifyStatus(BackupTarget.VERIFY_PENDING);
        entity.setVerifyMessage("配置已更新，请重新验证");
        backupTargetMapper.updateById(entity);
        return toResponse(entity, true);
    }

    @Override
    @Transactional
    public void deleteTarget(Long id) {
        BackupTarget entity = getEntity(id);
        ensureNoRunningBackupJob(entity.getId());
        ensureNoRunningRestoreJobForTarget(entity.getId());
        backupTargetMapper.deleteById(entity.getId());
    }

    @Override
    @Transactional
    public BackupTargetResponse verifyTarget(Long id) {
        BackupTarget entity = getEntity(id);
        ensureNoRunningBackupJob(entity.getId());
        ensureNoRunningRestoreJobForTarget(entity.getId());
        VerificationResult result = verify(entity);
        entity.setVerifyStatus(result.success ? BackupTarget.VERIFY_SUCCESS : BackupTarget.VERIFY_FAILED);
        entity.setVerifyMessage(result.message);
        entity.setLastVerifiedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(SecurityUtils.getCurrentUserId());
        backupTargetMapper.updateById(entity);
        return toResponse(entity, true);
    }

    @Override
    public List<BackupSetResponse> getBackupSets(Long targetId) {
        List<BackupTarget> targets;
        if (targetId != null) {
            BackupTarget target = getEntity(targetId);
            ensureTargetEnabledForRead(target);
            targets = List.of(target);
        } else {
            targets = backupTargetMapper.selectList(new LambdaQueryWrapper<BackupTarget>()
                    .eq(BackupTarget::getEnabled, true)
                    .orderByDesc(BackupTarget::getUpdatedAt)
                    .orderByDesc(BackupTarget::getId));
        }

        List<BackupSetResponse> backupSets = new ArrayList<>();
        for (BackupTarget target : targets) {
            try {
                if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
                    backupSets.addAll(readLocalBackupSets(target));
                    continue;
                }
                if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
                    backupSets.addAll(readSmbBackupSets(target));
                    continue;
                }
                throw new BusinessException("不支持的备份目标类型");
            } catch (BusinessException ex) {
                if (targetId != null) {
                    throw ex;
                }
                log.warn("读取备份目标失败，已跳过: targetId={}, targetName={}", target.getId(), target.getName(), ex);
            }
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
        if (!Boolean.TRUE.equals(target.getEnabled())) {
            throw new BusinessException("备份目标已禁用，不能执行备份");
        }
        ensureNoRunningBackupJob(target.getId());
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
            cleanupOldBackupSetsSafely(target, job);
            job.setStatus(BackupJob.STATUS_SUCCESS);
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            backupJobMapper.updateById(job);
            return job;
        } catch (Exception e) {
            log.error("执行手动备份失败: targetId={}, backupNo={}", targetId, job.getBackupNo(), e);
            cleanupFailedBackupSet(target, job);
            job.setStatus(BackupJob.STATUS_FAILED);
            job.setErrorMessage(e.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            backupJobMapper.updateById(job);
            throw new BusinessException("执行备份失败，任务编号: " + job.getBackupNo());
        }
    }

    private void cleanupOldBackupSetsSafely(BackupTarget target, BackupJob job) {
        try {
            cleanupOldBackupSets(target);
        } catch (Exception e) {
            log.warn("清理历史备份集失败，不影响本次备份结果: targetId={}, backupNo={}",
                    target == null ? null : target.getId(),
                    job == null ? null : job.getBackupNo(),
                    e);
        }
    }

    private void cleanupFailedBackupSet(BackupTarget target, BackupJob job) {
        if (target == null || job == null || !StringUtils.hasText(job.getBackupNo())) {
            return;
        }
        try {
            if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType()) && StringUtils.hasText(target.getLocalPath())) {
                deleteDirectory(Path.of(target.getLocalPath()).resolve(job.getBackupNo()));
                return;
            }
            if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
                smbStorageService.deleteDirectory(target, job.getBackupNo());
            }
        } catch (Exception cleanupException) {
            log.warn("清理失败的备份集残留失败: targetId={}, backupNo={}", target.getId(), job.getBackupNo(), cleanupException);
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
        if (!maintenanceEnabled) {
            ensureNoRunningRestoreJobForMaintenanceExit();
        }
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
        ensureNoRunningRestoreJob();
        BackupTarget target = getEntity(request.getTargetId());
        BackupSetResponse backupSet = getBackupSets(target.getId()).stream()
                .filter(item -> request.getBackupSetName().equals(item.getBackupSetName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到指定备份集"));
        ensureRestoreReady(backupSet);
        ensureRestoreScopesAvailable(request, backupSet);
        ensureMaintenanceMode();
        if (Boolean.TRUE.equals(request.getRestoreDatabase())) {
            ensureDatabaseRestoreTargetEmpty();
        }

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
        boolean rebuildIndexStarted = false;
        String currentStep = null;
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("backupSetName", backupSet.getBackupSetName());
        report.put("startedAt", restoreJob.getStartedAt());
        report.put("targetName", target.getName());
        report.put("steps", new ArrayList<Map<String, Object>>());
        try {
            preparedRestoreBackup = prepareRestoreBackup(target, backupSet);
            Path backupSetPath = preparedRestoreBackup.backupSetPath();
            if (Boolean.TRUE.equals(request.getRestoreDatabase())) {
                currentStep = "DATABASE";
                restoreDatabase(backupSetPath, backupSet);
                restoreJob.setRestoredDatabase(true);
                appendReportStep(report, "DATABASE", "SUCCESS", "数据库恢复完成");
            }
            if (Boolean.TRUE.equals(request.getRestoreFiles())) {
                currentStep = "FILES";
                restoreFiles(backupSetPath);
                restoreJob.setRestoredFiles(true);
                appendReportStep(report, "FILES", "SUCCESS", "电子文件回灌完成");
            }
            if (Boolean.TRUE.equals(request.getRestoreConfig())) {
                currentStep = "CONFIG";
                restoreConfigs(backupSetPath);
                restoreJob.setRestoredConfig(true);
                appendReportStep(report, "CONFIG", "SUCCESS", "系统配置恢复完成");
            }
            if (Boolean.TRUE.equals(request.getRebuildIndex())) {
                currentStep = "INDEX";
                rebuildIndexStarted = true;
                archiveIndexService.rebuildAllIndexes();
                restoreJob.setRebuildIndexStatus("SUCCESS");
                appendReportStep(report, "INDEX", "SUCCESS", "检索索引已重建");
            } else {
                restoreJob.setRebuildIndexStatus("SKIPPED");
                appendReportStep(report, "INDEX", "SKIPPED", "未执行索引重建");
            }

            restoreJob.setStatus(RestoreJob.STATUS_SUCCESS);
            restoreJob.setFinishedAt(LocalDateTime.now());
            report.put("status", restoreJob.getStatus());
            updateRestoreReportCommonFields(restoreJob, report);
            restoreJob.setRestoreReport(objectMapper.writeValueAsString(report));
            restoreJob.setUpdatedAt(LocalDateTime.now());
            restoreJobMapper.updateById(restoreJob);

            handleRestoreSuccessFollowUp(request, restoreJob, report);

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
            restoreJob.setRebuildIndexStatus(resolveFailedRestoreRebuildIndexStatus(
                    Boolean.TRUE.equals(request.getRebuildIndex()),
                    rebuildIndexStarted
            ));
            appendFailureStepIfPresent(report, currentStep);
            restoreJob.setRestoreReport(buildFailureReport(restoreJob, report));
            restoreJobMapper.updateById(restoreJob);

            operationLogService.log(
                    OperationLog.OBJ_SYSTEM,
                    restoreJob.getRestoreNo(),
                    null,
                    OperationLog.OP_UPDATE,
                    "系统恢复任务失败",
                    Map.of("errorMessage", RESTORE_FAILURE_PUBLIC_MESSAGE, "backupSetName", restoreJob.getBackupSetName())
            );
            throw new BusinessException("执行恢复失败，任务编号: " + restoreJob.getRestoreNo());
        }
    }

    private String resolveFailedRestoreRebuildIndexStatus(boolean rebuildRequested, boolean rebuildIndexStarted) {
        if (!rebuildRequested) {
            return "SKIPPED";
        }
        return rebuildIndexStarted ? "FAILED" : "NOT_STARTED";
    }

    private void handleRestoreSuccessFollowUp(
            RestoreExecuteRequest request,
            RestoreJob restoreJob,
            Map<String, Object> report
    ) {
        if (!Boolean.TRUE.equals(request.getExitMaintenanceAfterSuccess())) {
            return;
        }
        if (!configService.getBooleanValue(MAINTENANCE_MODE_KEY, false)) {
            return;
        }

        try {
            setRestoreMaintenanceMode(false);
            appendReportStep(report, "MAINTENANCE", "SUCCESS", "已退出维护模式");
        } catch (Exception e) {
            log.warn("恢复任务成功，但自动退出维护模式失败: restoreNo={}", restoreJob.getRestoreNo(), e);
            appendReportStep(report, "MAINTENANCE", "FAILED", "恢复已完成，但退出维护模式失败，请手动处理");
        }

        updateRestoreSuccessReport(restoreJob, report);
    }

    private void updateRestoreSuccessReport(RestoreJob restoreJob, Map<String, Object> report) {
        try {
            updateRestoreReportCommonFields(restoreJob, report);
            restoreJob.setRestoreReport(objectMapper.writeValueAsString(report));
            restoreJob.setUpdatedAt(LocalDateTime.now());
            restoreJobMapper.updateById(restoreJob);
        } catch (Exception e) {
            log.warn("更新恢复成功报告失败: restoreNo={}", restoreJob.getRestoreNo(), e);
        }
    }

    private void appendFailureStepIfPresent(Map<String, Object> report, String currentStep) {
        if (!StringUtils.hasText(currentStep)) {
            return;
        }
        appendReportStep(report, currentStep, "FAILED", buildRestoreStepFailureMessage(currentStep));
    }

    private String buildRestoreStepFailureMessage(String step) {
        return switch (step) {
            case "DATABASE" -> "数据库恢复失败";
            case "FILES" -> "电子文件回灌失败";
            case "CONFIG" -> "系统配置恢复失败";
            case "INDEX" -> "检索索引重建失败";
            default -> "恢复步骤执行失败";
        };
    }

    @Override
    public PageResult<BackupJob> getBackupJobs(Integer pageNum, Integer pageSize) {
        PageRequest pageRequest = normalizePageRequest(pageNum, pageSize);
        Page<BackupJob> page = new Page<>(pageRequest.pageNum(), pageRequest.pageSize());
        Page<BackupJob> result = backupJobMapper.selectPage(page, new LambdaQueryWrapper<BackupJob>()
                .orderByDesc(BackupJob::getCreatedAt)
                .orderByDesc(BackupJob::getId));
        return PageResult.of(result);
    }

    @Override
    public PageResult<RestoreJob> getRestoreJobs(Integer pageNum, Integer pageSize) {
        PageRequest pageRequest = normalizePageRequest(pageNum, pageSize);
        Page<RestoreJob> page = new Page<>(pageRequest.pageNum(), pageRequest.pageSize());
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
        BackupTarget candidate = new BackupTarget();
        applyRequest(candidate, request, true);
        validateTarget(candidate);
    }

    private void validateTarget(BackupTarget target) {
        if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType())) {
            if (!StringUtils.hasText(target.getLocalPath())) {
                throw new BusinessException("本地目录备份目标必须填写目录路径");
            }
            return;
        }
        if (BackupTarget.TYPE_SMB.equals(target.getTargetType())) {
            if (!StringUtils.hasText(target.getSmbHost()) || !StringUtils.hasText(target.getSmbShare())) {
                throw new BusinessException("SMB 备份目标必须填写主机和共享名称");
            }
            if (!StringUtils.hasText(target.getSmbUsername()) || !StringUtils.hasText(target.getSmbPasswordEncrypted())) {
                throw new BusinessException("SMB 备份目标必须填写用户名和密码");
            }
            return;
        }
        throw new BusinessException("不支持的备份目标类型");
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

    private void ensureRestoreScopesAvailable(RestoreExecuteRequest request, BackupSetResponse backupSet) {
        if (Boolean.TRUE.equals(request.getRestoreDatabase()) && !isDatabaseRestoreAvailable(backupSet)) {
            throw new BusinessException("该备份集未包含可恢复的数据库备份");
        }
        if (Boolean.TRUE.equals(request.getRestoreFiles()) && !Boolean.TRUE.equals(backupSet.getFilesRestorable())) {
            throw new BusinessException("该备份集未包含可恢复的电子文件索引");
        }
        if (Boolean.TRUE.equals(request.getRestoreConfig()) && !Boolean.TRUE.equals(backupSet.getConfigRestorable())) {
            throw new BusinessException("该备份集未包含可恢复的系统配置");
        }
    }

    private boolean isDatabaseRestoreAvailable(BackupSetResponse backupSet) {
        if (backupSet == null) {
            return false;
        }
        if (backupSet.getDatabaseRestorable() != null) {
            return Boolean.TRUE.equals(backupSet.getDatabaseRestorable());
        }
        return isDatabaseRestorable(backupSet.getDatabaseMode(), true);
    }

    private void ensureNoRunningRestoreJob() {
        long runningCount = restoreJobMapper.selectCount(new LambdaQueryWrapper<RestoreJob>()
                .eq(RestoreJob::getStatus, RestoreJob.STATUS_RUNNING));
        if (runningCount > 0) {
            throw new BusinessException("已有恢复任务正在执行，请等待当前任务完成后再试");
        }
    }

    private void ensureNoRunningRestoreJobForMaintenanceExit() {
        long runningCount = restoreJobMapper.selectCount(new LambdaQueryWrapper<RestoreJob>()
                .eq(RestoreJob::getStatus, RestoreJob.STATUS_RUNNING));
        if (runningCount > 0) {
            throw new BusinessException("恢复任务执行期间不能退出维护模式");
        }
    }

    private void ensureNoRunningRestoreJobForTarget(Long targetId) {
        long runningCount = restoreJobMapper.selectCount(new LambdaQueryWrapper<RestoreJob>()
                .eq(RestoreJob::getTargetId, targetId)
                .eq(RestoreJob::getStatus, RestoreJob.STATUS_RUNNING));
        if (runningCount > 0) {
            throw new BusinessException("该备份目标正被恢复任务使用，请等待恢复完成后再试");
        }
    }

    private void ensureTargetEnabledForRead(BackupTarget target) {
        if (!Boolean.TRUE.equals(target.getEnabled())) {
            throw new BusinessException("备份目标已禁用，不能作为备份或恢复来源使用");
        }
    }

    private void ensureNoRunningBackupJob(Long targetId) {
        long runningCount = backupJobMapper.selectCount(new LambdaQueryWrapper<BackupJob>()
                .eq(BackupJob::getTargetId, targetId)
                .in(BackupJob::getStatus, BackupJob.STATUS_PENDING, BackupJob.STATUS_RUNNING));
        if (runningCount > 0) {
            throw new BusinessException("该备份目标已有任务正在执行，请等待当前任务完成后再试");
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
        entity.setRemarks(trimToNull(request.getRemarks()));

        if (BackupTarget.TYPE_LOCAL.equals(request.getTargetType())) {
            entity.setLocalPath(trimToNull(request.getLocalPath()));
            entity.setSmbHost(null);
            entity.setSmbPort(null);
            entity.setSmbShare(null);
            entity.setSmbUsername(null);
            entity.setSmbPasswordEncrypted(null);
            entity.setSmbSubPath(null);
            return;
        }

        if (BackupTarget.TYPE_SMB.equals(request.getTargetType())) {
            entity.setLocalPath(null);
            if (creating || request.getSmbHost() != null) {
                entity.setSmbHost(trimToNull(request.getSmbHost()));
            }
            if (creating || request.getSmbPort() != null) {
                entity.setSmbPort(request.getSmbPort() != null ? request.getSmbPort() : 445);
            } else if (entity.getSmbPort() == null) {
                entity.setSmbPort(445);
            }
            if (creating || request.getSmbShare() != null) {
                entity.setSmbShare(trimToNull(request.getSmbShare()));
            }
            if (creating || request.getSmbUsername() != null) {
                entity.setSmbUsername(trimToNull(request.getSmbUsername()));
            }
            if (creating || request.getSmbSubPath() != null) {
                entity.setSmbSubPath(trimToNull(request.getSmbSubPath()));
            }
            if (StringUtils.hasText(request.getSmbPassword())) {
                entity.setSmbPasswordEncrypted(secretCryptoService.encrypt(request.getSmbPassword()));
            } else if (creating) {
                entity.setSmbPasswordEncrypted(null);
            }
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
            return new VerificationResult(false, LOCAL_VERIFY_FAILURE_PUBLIC_MESSAGE);
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
            return new VerificationResult(false, SMB_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private List<BackupSetResponse> readLocalBackupSets(BackupTarget target) {
        VerificationResult verificationResult = verifyLocalReadable(target);
        if (!verificationResult.success) {
            throw new BusinessException("无法读取备份目录: " + verificationResult.message);
        }
        try (var stream = Files.list(Path.of(target.getLocalPath()))) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> isBackupSetDirectoryName(path.getFileName().toString()))
                    .map(path -> readLocalBackupSetSafely(target, path))
                    .sorted(backupSetComparator())
                    .toList();
        } catch (Exception e) {
            log.warn("读取备份目录失败: path={}", target.getLocalPath(), e);
            throw new BusinessException("读取备份目录失败，请检查目录配置和访问权限");
        }
    }

    private List<BackupSetResponse> readSmbBackupSets(BackupTarget target) {
        VerificationResult verificationResult = verifySmbReadable(target);
        if (!verificationResult.success) {
            throw new BusinessException("无法读取 SMB 备份目录: " + verificationResult.message);
        }
        try {
            return smbStorageService.listDirectories(target).stream()
                    .filter(entry -> isBackupSetDirectoryName(entry.name()))
                    .map(entry -> readSmbBackupSet(target, entry))
                    .sorted(backupSetComparator())
                    .toList();
        } catch (Exception e) {
            log.warn("读取 SMB 备份目录失败: host={}, share={}", target.getSmbHost(), target.getSmbShare(), e);
            throw new BusinessException("读取 SMB 备份目录失败，请检查网络、共享配置和访问权限");
        }
    }

    private VerificationResult verifyLocalReadable(BackupTarget entity) {
        try {
            Path path = Path.of(entity.getLocalPath());
            if (!Files.exists(path)) {
                return new VerificationResult(false, "目标目录不存在");
            }
            if (!Files.isDirectory(path)) {
                return new VerificationResult(false, "目标路径不是目录");
            }
            if (!Files.isReadable(path)) {
                return new VerificationResult(false, "目标目录不可读");
            }
            return new VerificationResult(true, "本地目录可读，验证通过");
        } catch (Exception e) {
            log.warn("本地恢复目录读取失败: {}", entity.getLocalPath(), e);
            return new VerificationResult(false, LOCAL_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult verifySmbReadable(BackupTarget entity) {
        if (!StringUtils.hasText(entity.getSmbHost()) || !StringUtils.hasText(entity.getSmbShare())) {
            return new VerificationResult(false, "SMB 主机或共享名称缺失");
        }
        if (!StringUtils.hasText(entity.getSmbUsername()) || !StringUtils.hasText(entity.getSmbPasswordEncrypted())) {
            return new VerificationResult(false, "SMB 凭证未完整配置");
        }
        return new VerificationResult(true, "SMB 共享目录可读，验证通过");
    }

    private boolean isBackupSetDirectoryName(String directoryName) {
        return StringUtils.hasText(directoryName) && directoryName.startsWith("BK-");
    }

    private BackupSetResponse readLocalBackupSetSafely(BackupTarget target, Path backupSetPath) {
        try {
            return readBackupSet(target, backupSetPath);
        } catch (Exception e) {
            log.warn("读取本地备份集失败: path={}", backupSetPath, e);
            return buildUnreadableLocalBackupSetResponse(target, backupSetPath);
        }
    }

    private BackupSetResponse readBackupSet(BackupTarget target, Path backupSetPath) {
        Path manifestPath = backupSetPath.resolve("manifest.json");
        Path checksumsPath = backupSetPath.resolve("checksums.txt");
        Path databaseFilePath = backupSetPath.resolve("database").resolve("archive-system.sql");
        Path filesIndexPath = backupSetPath.resolve("files").resolve("files-index.json");
        Path configFilePath = backupSetPath.resolve("config").resolve("sys-config.json");
        boolean hasManifest = Files.exists(manifestPath);
        boolean hasChecksums = Files.exists(checksumsPath);
        Map<String, Object> manifest = hasManifest ? readManifest(manifestPath) : Map.of();
        Set<String> checksumPaths = hasChecksums ? readLocalChecksumPaths(checksumsPath) : Set.of();
        LocalDateTime createdAt = firstNonNull(parseDateTime(manifest.get("createdAt")), readFileTime(backupSetPath));
        String databaseMode = stringValue(manifest.get("databaseMode"));
        VerificationResult verificationResult = verifyBackupSet(
                backupSetPath,
                backupSetPath.getFileName() == null ? null : backupSetPath.getFileName().toString(),
                hasManifest,
                hasChecksums,
                manifest
        );

        return BackupSetResponse.builder()
                .targetId(target.getId())
                .targetName(target.getName())
                .targetType(target.getTargetType())
                .backupNo(stringValue(firstNonNull(manifest.get("backupNo"), backupSetPath.getFileName().toString())))
                .backupSetName(backupSetPath.getFileName().toString())
                .backupSetPath(backupSetPath.toString())
                .displayPath(buildBackupSetDisplayPath(target, backupSetPath.getFileName().toString()))
                .createdAt(createdAt)
                .databaseMode(databaseMode)
                .databaseRestorable(isLocalDatabaseRestorable(databaseMode, databaseFilePath, checksumPaths))
                .filesRestorable(isLocalFilesRestorable(backupSetPath, filesIndexPath, checksumPaths))
                .configRestorable(isLocalConfigRestorable(configFilePath, checksumPaths))
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
            return objectMapper.readValue(manifestPath.toFile(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("读取 manifest 失败，已回退到默认元数据: {}", manifestPath);
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
            Set<String> checksumPaths = hasChecksums ? readRemoteChecksumPaths(target, checksumsRelativePath) : Set.of();
            String databaseMode = stringValue(manifest.get("databaseMode"));
            LocalDateTime createdAt = firstNonNull(parseDateTime(manifest.get("createdAt")), entry.modifiedAt());
            VerificationResult verificationResult = verifyBackupSet(target, backupSetName, hasManifest, hasChecksums, manifest);

            return BackupSetResponse.builder()
                    .targetId(target.getId())
                    .targetName(target.getName())
                    .targetType(target.getTargetType())
                    .backupNo(stringValue(firstNonNull(manifest.get("backupNo"), backupSetName)))
                    .backupSetName(backupSetName)
                    .backupSetPath(smbStorageService.buildDisplayPath(target, backupSetName))
                    .displayPath(buildBackupSetDisplayPath(target, backupSetName))
                    .createdAt(createdAt)
                    .databaseMode(databaseMode)
                    .databaseRestorable(isRemoteDatabaseRestorable(target, backupSetName, databaseMode, checksumPaths))
                    .filesRestorable(isRemoteFilesRestorable(target, backupSetName, backupSetName + "/files/files-index.json", checksumPaths))
                    .configRestorable(isRemoteConfigRestorable(target, backupSetName + "/config/sys-config.json", checksumPaths))
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
            log.warn("读取 SMB 备份集失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return buildUnreadableBackupSetResponse(target, backupSetName, entry.modifiedAt());
        }
    }

    private BackupSetResponse buildUnreadableBackupSetResponse(BackupTarget target, String backupSetName, LocalDateTime createdAt) {
        return BackupSetResponse.builder()
                .targetId(target.getId())
                .targetName(target.getName())
                .targetType(target.getTargetType())
                .backupNo(backupSetName)
                .backupSetName(backupSetName)
                .backupSetPath(smbStorageService.buildDisplayPath(target, backupSetName))
                .displayPath(buildBackupSetDisplayPath(target, backupSetName))
                .createdAt(createdAt)
                .databaseRestorable(false)
                .filesRestorable(false)
                .configRestorable(false)
                .hasManifest(false)
                .hasChecksums(false)
                .verifyStatus("INCOMPLETE")
                .verifyMessage("读取备份集元数据失败，请检查备份文件完整性和访问权限")
                .build();
    }

    private BackupSetResponse buildUnreadableLocalBackupSetResponse(BackupTarget target, Path backupSetPath) {
        String backupSetName = backupSetPath.getFileName() == null ? backupSetPath.toString() : backupSetPath.getFileName().toString();
        return BackupSetResponse.builder()
                .targetId(target.getId())
                .targetName(target.getName())
                .targetType(target.getTargetType())
                .backupNo(backupSetName)
                .backupSetName(backupSetName)
                .backupSetPath(backupSetPath.toString())
                .displayPath(buildBackupSetDisplayPath(target, backupSetName))
                .createdAt(readFileTime(backupSetPath))
                .databaseRestorable(false)
                .filesRestorable(false)
                .configRestorable(false)
                .hasManifest(false)
                .hasChecksums(false)
                .verifyStatus("INCOMPLETE")
                .verifyMessage("读取备份集元数据失败，请检查备份文件完整性和访问权限")
                .build();
    }

    private boolean isDatabaseRestorable(String databaseMode, boolean databaseFileExists) {
        return databaseFileExists && "PG_DUMP".equals(databaseMode);
    }

    private boolean isLocalDatabaseRestorable(String databaseMode, Path databaseFilePath, Set<String> checksumPaths) {
        return isDatabaseRestorable(databaseMode, Files.exists(databaseFilePath))
                && checksumPaths.contains("database/archive-system.sql");
    }

    private boolean isRemoteDatabaseRestorable(
            BackupTarget target,
            String backupSetName,
            String databaseMode,
            Set<String> checksumPaths
    ) throws Exception {
        return isDatabaseRestorable(databaseMode, smbStorageService.exists(target, backupSetName + "/database/archive-system.sql"))
                && checksumPaths.contains("database/archive-system.sql");
    }

    private Map<String, Object> readManifest(BackupTarget target, String manifestRelativePath) {
        try (InputStream inputStream = smbStorageService.openInputStream(target, manifestRelativePath)) {
            return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("读取 SMB manifest 失败，已回退到默认元数据: {}", manifestRelativePath);
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
        job.setErrorMessage(databaseDumpResult.warning());
        return backupSetPath;
    }

    private void restoreDatabase(Path backupSetPath, BackupSetResponse backupSet) throws Exception {
        if ("PLACEHOLDER".equals(backupSet.getDatabaseMode())) {
            throw new BusinessException("该备份集数据库文件为占位文件，不能执行数据库恢复");
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
            throw new BusinessException("数据库恢复执行失败，请联系系统管理员查看系统日志");
        }
    }

    private void restoreFiles(Path backupSetPath) throws Exception {
        Path filesDir = backupSetPath.resolve("files");
        Path indexPath = filesDir.resolve("files-index.json");
        if (!Files.exists(indexPath)) {
            throw new BusinessException("未找到文件索引 files-index.json");
        }
        List<Map<String, Object>> items = objectMapper.readValue(
                indexPath.toFile(), new TypeReference<List<Map<String, Object>>>() {});
        Set<String> restoredObjects = new HashSet<>();
        List<String> missingObjects = new ArrayList<>();
        for (Map<String, Object> item : items) {
            restoreObjectIfPresent(filesDir, restoredObjects, missingObjects, item.get("storagePath"));
            restoreObjectIfPresent(filesDir, restoredObjects, missingObjects, item.get("convertedPath"));
            restoreObjectIfPresent(filesDir, restoredObjects, missingObjects, item.get("previewPath"));
            restoreObjectIfPresent(filesDir, restoredObjects, missingObjects, item.get("thumbnailPath"));
        }
        if (!missingObjects.isEmpty()) {
            throw new BusinessException("备份集缺少部分电子文件对象，请检查备份完整性");
        }
    }

    private void restoreObjectIfPresent(
            Path filesDir,
            Set<String> restoredObjects,
            List<String> missingObjects,
            Object objectPathValue
    ) throws Exception {
        String objectPath = stringValue(objectPathValue);
        if (!StringUtils.hasText(objectPath) || !restoredObjects.add(objectPath)) {
            return;
        }
        Path localFile = resolvePathWithinBase(filesDir, objectPath, "恢复对象路径");
        if (!Files.exists(localFile)) {
            log.warn("恢复文件时未找到对象备份: {}", objectPath);
            missingObjects.add(objectPath);
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
            if (config == null || RESTORE_SKIPPED_CONFIG_KEYS.contains(config.getConfigKey())) {
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
        List<SysConfig> exportableConfigs = sysConfigMapper.selectAllOrdered().stream()
                .filter(config -> config != null && !EXPORT_SKIPPED_CONFIG_KEYS.contains(config.getConfigKey()))
                .toList();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), exportableConfigs);
        return output;
    }

    private Path exportBackupConfig(Path configDir, BackupTarget target, BackupJob job) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetId", target.getId());
        payload.put("targetName", target.getName());
        payload.put("targetType", target.getTargetType());
        payload.put("backupNo", job.getBackupNo());
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
            Files.writeString(dumpFile, "-- pg_dump execution error\n-- details hidden; check server logs for diagnostics\n",
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
        List<String> failedObjectPaths = new ArrayList<>();
        for (String objectPath : objectPaths) {
            try {
                Path target = resolveObjectBackupPath(filesDir, objectPath);
                minioService.downloadToFile(objectPath, target);
                totalBytes += Files.size(target);
            } catch (BusinessException e) {
                log.warn("跳过非法备份对象路径: objectPath={}, message={}", objectPath, e.getMessage());
                failedObjectPaths.add(objectPath);
            } catch (Exception e) {
                log.warn("备份 MinIO 对象失败: objectPath={}, message={}", objectPath, e.getMessage());
                failedObjectPaths.add(objectPath);
            }
        }

        if (!failedObjectPaths.isEmpty()) {
            throw new BusinessException("部分电子文件备份失败，请联系系统管理员查看系统日志");
        }

        String indexFileName = "files-index.json";
        Path indexPath = filesDir.resolve(indexFileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), digitalFiles.stream()
                .map(file -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", file.getId());
                    item.put("archiveId", file.getArchiveId());
                    item.put("fileName", file.getFileName());
                    item.put("storagePath", normalizeIndexedObjectPath(file.getStoragePath()));
                    item.put("convertedPath", normalizeIndexedObjectPath(file.getConvertedPath()));
                    item.put("previewPath", normalizeIndexedObjectPath(file.getPreviewPath()));
                    item.put("thumbnailPath", normalizeIndexedObjectPath(file.getThumbnailPath()));
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

    private String normalizeIndexedObjectPath(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Path resolveObjectBackupPath(Path filesDir, String objectPath) {
        return resolvePathWithinBase(filesDir, objectPath, "备份对象路径");
    }

    private VerificationResult verifyBackupSet(
            Path backupSetPath,
            String backupSetName,
            boolean hasManifest,
            boolean hasChecksums,
            Map<String, Object> manifest
    ) {
        if (!hasManifest || !hasChecksums) {
            return new VerificationResult(false, "缺少 manifest 或 checksums 文件");
        }
        Path checksumsPath = backupSetPath.resolve("checksums.txt");
        try {
            List<String> lines = Files.readAllLines(checksumsPath, StandardCharsets.UTF_8);
            Map<String, String> checksumEntries = parseChecksumEntries(lines);
            if (checksumEntries == null) {
                return new VerificationResult(false, "checksums.txt 格式无效");
            }
            if (checksumEntries.isEmpty()) {
                return new VerificationResult(false, "checksums.txt 为空");
            }
            for (Map.Entry<String, String> checksumEntry : checksumEntries.entrySet()) {
                String relativePath = checksumEntry.getKey();
                String expectedHash = checksumEntry.getValue();
                Path target = resolvePathWithinBase(backupSetPath, relativePath, "备份校验路径");
                if (!Files.exists(target)) {
                    return new VerificationResult(false, "缺少备份文件: " + relativePath);
                }
                String actualHash = calculateSha256(target);
                if (!expectedHash.equalsIgnoreCase(actualHash)) {
                    return new VerificationResult(false, "校验失败: " + relativePath);
                }
            }
            VerificationResult artifactValidation = validateLocalRestoreArtifacts(
                    backupSetPath,
                    backupSetName,
                    stringValue(manifest.get("databaseMode")),
                    manifest
            );
            if (!artifactValidation.success()) {
                return artifactValidation;
            }
            VerificationResult checksumCoverageValidation = validateLocalChecksumCoverage(
                    backupSetPath,
                    checksumEntries.keySet(),
                    manifest
            );
            if (!checksumCoverageValidation.success()) {
                return checksumCoverageValidation;
            }
            return new VerificationResult(true, "备份集结构与 SHA-256 校验通过");
        } catch (Exception e) {
            log.warn("校验本地备份集失败: path={}", backupSetPath, e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult verifyBackupSet(
            BackupTarget target,
            String backupSetName,
            boolean hasManifest,
            boolean hasChecksums,
            Map<String, Object> manifest
    ) {
        if (!hasManifest || !hasChecksums) {
            return new VerificationResult(false, "缺少 manifest 或 checksums 文件");
        }
        String checksumsRelativePath = backupSetName + "/checksums.txt";
        try (InputStream checksumsInput = smbStorageService.openInputStream(target, checksumsRelativePath)) {
            List<String> lines = new String(checksumsInput.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            Map<String, String> checksumEntries = parseChecksumEntries(lines);
            if (checksumEntries == null) {
                return new VerificationResult(false, "checksums.txt 格式无效");
            }
            if (checksumEntries.isEmpty()) {
                return new VerificationResult(false, "checksums.txt 为空");
            }
            for (Map.Entry<String, String> checksumEntry : checksumEntries.entrySet()) {
                String relativePath = checksumEntry.getKey();
                String expectedHash = checksumEntry.getValue();
                String remoteFilePath = resolveSmbPathWithinBackupSet(backupSetName, relativePath);
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
            VerificationResult artifactValidation = validateSmbRestoreArtifacts(
                    target,
                    backupSetName,
                    stringValue(manifest.get("databaseMode")),
                    manifest
            );
            if (!artifactValidation.success()) {
                return artifactValidation;
            }
            VerificationResult checksumCoverageValidation = validateRemoteChecksumCoverage(
                    target,
                    backupSetName,
                    checksumEntries.keySet(),
                    manifest
            );
            if (!checksumCoverageValidation.success()) {
                return checksumCoverageValidation;
            }
            return new VerificationResult(true, "备份集结构与 SHA-256 校验通过");
        } catch (Exception e) {
            log.warn("校验 SMB 备份集失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult validateLocalRestoreArtifacts(
            Path backupSetPath,
            String backupSetName,
            String databaseMode,
            Map<String, Object> manifest
    ) {
        VerificationResult manifestValidation = validateLocalManifestBackup(
                backupSetPath.resolve("manifest.json"),
                backupSetPath.getFileName() == null ? null : backupSetPath.getFileName().toString(),
                "manifest 文件格式无效，请检查备份文件完整性",
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!manifestValidation.success()) {
            return manifestValidation;
        }
        VerificationResult databaseValidation = validateLocalDatabaseBackup(
                backupSetPath.resolve("database").resolve("archive-system.sql"),
                databaseMode
        );
        if (!databaseValidation.success()) {
            return databaseValidation;
        }
        Path filesIndexPath = backupSetPath.resolve("files").resolve("files-index.json");
        VerificationResult filesIndexValidation = validateLocalFilesIndex(backupSetPath, filesIndexPath, "文件索引格式无效，请检查备份文件完整性");
        if (!filesIndexValidation.success()) {
            return filesIndexValidation;
        }
        VerificationResult manifestStatsValidation = validateLocalManifestFileStats(
                backupSetPath,
                filesIndexPath,
                manifest,
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!manifestStatsValidation.success()) {
            return manifestStatsValidation;
        }
        Path configPath = backupSetPath.resolve("config").resolve("sys-config.json");
        VerificationResult configValidation = validateLocalConfigBackup(configPath, "系统配置备份格式无效，请检查备份文件完整性");
        if (!configValidation.success()) {
            return configValidation;
        }
        VerificationResult scopeValidation = validateManifestScopeConsistency(
                manifest,
                buildActualScopeSet(
                        Files.exists(backupSetPath.resolve("database").resolve("archive-system.sql"))
                                && StringUtils.hasText(databaseMode),
                        Files.exists(filesIndexPath),
                        Files.exists(configPath)
                ),
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!scopeValidation.success()) {
            return scopeValidation;
        }
        return validateLocalBackupConfig(
                backupSetPath.resolve("config").resolve("backup-config.json"),
                backupSetName,
                trimToNull(stringValue(manifest.get("targetName"))),
                isBackupConfigExpected(manifest)
        );
    }

    private VerificationResult validateSmbRestoreArtifacts(
            BackupTarget target,
            String backupSetName,
            String databaseMode,
            Map<String, Object> manifest
    ) {
        VerificationResult manifestValidation = validateRemoteManifestBackup(
                target,
                backupSetName + "/manifest.json",
                backupSetName,
                "manifest 文件格式无效，请检查备份文件完整性",
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!manifestValidation.success()) {
            return manifestValidation;
        }
        VerificationResult databaseValidation = validateRemoteDatabaseBackup(
                target,
                backupSetName + "/database/archive-system.sql",
                databaseMode
        );
        if (!databaseValidation.success()) {
            return databaseValidation;
        }
        VerificationResult filesIndexValidation = validateRemoteFilesIndex(
                target,
                backupSetName,
                backupSetName + "/files/files-index.json",
                "文件索引格式无效，请检查备份文件完整性"
        );
        if (!filesIndexValidation.success()) {
            return filesIndexValidation;
        }
        VerificationResult manifestStatsValidation = validateRemoteManifestFileStats(
                target,
                backupSetName,
                backupSetName + "/files/files-index.json",
                manifest,
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!manifestStatsValidation.success()) {
            return manifestStatsValidation;
        }
        VerificationResult configValidation = validateRemoteConfigBackup(
                target,
                backupSetName + "/config/sys-config.json",
                "系统配置备份格式无效，请检查备份文件完整性"
        );
        if (!configValidation.success()) {
            return configValidation;
        }
        VerificationResult scopeValidation = validateRemoteManifestScopeConsistency(
                target,
                backupSetName,
                databaseMode,
                manifest,
                "manifest 元数据无效，请检查备份文件完整性"
        );
        if (!scopeValidation.success()) {
            return scopeValidation;
        }
        return validateRemoteBackupConfig(
                target,
                backupSetName + "/config/backup-config.json",
                backupSetName,
                trimToNull(stringValue(manifest.get("targetName"))),
                isBackupConfigExpected(manifest)
        );
    }

    private VerificationResult validateLocalDatabaseBackup(Path databaseFilePath, String databaseMode) {
        return validateDatabaseBackup(Files.exists(databaseFilePath), databaseMode);
    }

    private VerificationResult validateLocalManifestBackup(
            Path manifestPath,
            String backupSetName,
            String invalidMessage,
            String invalidMetadataMessage
    ) {
        return validateLocalManifestBackup(manifestPath, backupSetName, invalidMessage, invalidMetadataMessage, true);
    }

    private VerificationResult validateLocalManifestBackup(
            Path manifestPath,
            String backupSetName,
            String invalidMessage,
            String invalidMetadataMessage,
            boolean logFailure
    ) {
        if (!Files.exists(manifestPath)) {
            return new VerificationResult(false, invalidMessage);
        }
        try {
            Map<String, Object> manifest = objectMapper.readValue(
                    manifestPath.toFile(),
                    new TypeReference<Map<String, Object>>() {}
            );
            return validateManifestEntries(manifest, backupSetName, invalidMetadataMessage);
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验 manifest 失败: path={}", manifestPath, e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateRemoteManifestBackup(
            BackupTarget target,
            String relativePath,
            String backupSetName,
            String invalidMessage,
            String invalidMetadataMessage
    ) {
        return validateRemoteManifestBackup(target, relativePath, backupSetName, invalidMessage, invalidMetadataMessage, true);
    }

    private VerificationResult validateRemoteManifestBackup(
            BackupTarget target,
            String relativePath,
            String backupSetName,
            String invalidMessage,
            String invalidMetadataMessage,
            boolean logFailure
    ) {
        try {
            if (!smbStorageService.exists(target, relativePath)) {
                return new VerificationResult(false, invalidMessage);
            }
            try (InputStream inputStream = smbStorageService.openInputStream(target, relativePath)) {
                Map<String, Object> manifest = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, Object>>() {}
                );
                return validateManifestEntries(manifest, backupSetName, invalidMetadataMessage);
            }
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验 SMB manifest 失败: relativePath={}, host={}, share={}",
                        relativePath, target.getSmbHost(), target.getSmbShare(), e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateRemoteDatabaseBackup(
            BackupTarget target,
            String relativePath,
            String databaseMode
    ) {
        try {
            return validateDatabaseBackup(smbStorageService.exists(target, relativePath), databaseMode);
        } catch (Exception e) {
            log.warn("校验 SMB 数据库备份失败: relativePath={}, host={}, share={}",
                    relativePath, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, "数据库备份元数据无效，请检查备份文件完整性");
        }
    }

    private VerificationResult validateDatabaseBackup(boolean databaseFileExists, String databaseMode) {
        if (!databaseFileExists && !StringUtils.hasText(databaseMode)) {
            return new VerificationResult(true, null);
        }
        if (!databaseFileExists) {
            return new VerificationResult(false, "数据库备份元数据无效，请检查备份文件完整性");
        }
        if (!StringUtils.hasText(databaseMode)) {
            return new VerificationResult(false, "数据库备份元数据无效，请检查备份文件完整性");
        }
        if (!"PG_DUMP".equals(databaseMode) && !"PLACEHOLDER".equals(databaseMode)) {
            return new VerificationResult(false, "数据库备份元数据无效，请检查备份文件完整性");
        }
        return new VerificationResult(true, null);
    }

    private Map<String, String> parseChecksumEntries(List<String> lines) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : lines) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) {
                return null;
            }
            String relativePath = trimToNull(parts[0]);
            String expectedHash = trimToNull(parts[1]);
            if (!StringUtils.hasText(relativePath) || !StringUtils.hasText(expectedHash)) {
                return null;
            }
            if (entries.putIfAbsent(relativePath, expectedHash) != null) {
                return null;
            }
        }
        return entries;
    }

    private VerificationResult validateLocalChecksumCoverage(
            Path backupSetPath,
            Set<String> checksummedPaths,
            Map<String, Object> manifest
    ) {
        try {
            Set<String> requiredPaths = new LinkedHashSet<>();
            requiredPaths.add("manifest.json");
            addRequiredLocalChecksumPath(requiredPaths, backupSetPath, "database/archive-system.sql");
            addRequiredLocalChecksumPath(requiredPaths, backupSetPath, "config/sys-config.json");
            if (isBackupConfigExpected(manifest) || Files.exists(backupSetPath.resolve("config").resolve("backup-config.json"))) {
                requiredPaths.add("config/backup-config.json");
            }
            appendLocalFilesChecksumCoverageRequiredPaths(requiredPaths, backupSetPath);
            return validateChecksumCoverage(checksummedPaths, requiredPaths);
        } catch (Exception e) {
            log.warn("校验本地 checksums 覆盖范围失败: path={}", backupSetPath, e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult validateRemoteChecksumCoverage(
            BackupTarget target,
            String backupSetName,
            Set<String> checksummedPaths,
            Map<String, Object> manifest
    ) {
        try {
            Set<String> requiredPaths = new LinkedHashSet<>();
            requiredPaths.add("manifest.json");
            addRequiredRemoteChecksumPath(requiredPaths, target, backupSetName, "database/archive-system.sql");
            addRequiredRemoteChecksumPath(requiredPaths, target, backupSetName, "config/sys-config.json");
            if (isBackupConfigExpected(manifest) || smbStorageService.exists(target, backupSetName + "/config/backup-config.json")) {
                requiredPaths.add("config/backup-config.json");
            }
            appendRemoteFilesChecksumCoverageRequiredPaths(requiredPaths, target, backupSetName);
            return validateChecksumCoverage(checksummedPaths, requiredPaths);
        } catch (Exception e) {
            log.warn("校验 SMB checksums 覆盖范围失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult validateLocalFilesChecksumCoverage(Path backupSetPath, Set<String> checksummedPaths) {
        try {
            Set<String> requiredPaths = new LinkedHashSet<>();
            appendLocalFilesChecksumCoverageRequiredPaths(requiredPaths, backupSetPath);
            return validateChecksumCoverage(checksummedPaths, requiredPaths);
        } catch (Exception e) {
            log.warn("校验本地文件备份 checksum 覆盖范围失败: path={}", backupSetPath, e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private VerificationResult validateRemoteFilesChecksumCoverage(
            BackupTarget target,
            String backupSetName,
            Set<String> checksummedPaths
    ) {
        try {
            Set<String> requiredPaths = new LinkedHashSet<>();
            appendRemoteFilesChecksumCoverageRequiredPaths(requiredPaths, target, backupSetName);
            return validateChecksumCoverage(checksummedPaths, requiredPaths);
        } catch (Exception e) {
            log.warn("校验 SMB 文件备份 checksum 覆盖范围失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, BACKUP_SET_VERIFY_FAILURE_PUBLIC_MESSAGE);
        }
    }

    private void addRequiredLocalChecksumPath(Set<String> requiredPaths, Path backupSetPath, String relativePath) {
        if (Files.exists(backupSetPath.resolve(relativePath.replace('/', java.io.File.separatorChar)))) {
            requiredPaths.add(relativePath);
        }
    }

    private void addRequiredRemoteChecksumPath(
            Set<String> requiredPaths,
            BackupTarget target,
            String backupSetName,
            String relativePath
    ) throws Exception {
        String fullPath = backupSetName + "/" + relativePath;
        if (smbStorageService.exists(target, fullPath)) {
            requiredPaths.add(relativePath);
        }
    }

    private void appendLocalFilesChecksumCoverageRequiredPaths(Set<String> requiredPaths, Path backupSetPath) throws Exception {
        addRequiredLocalChecksumPath(requiredPaths, backupSetPath, "files/files-index.json");
        Path filesIndexPath = backupSetPath.resolve("files").resolve("files-index.json");
        if (Files.exists(filesIndexPath)) {
            requiredPaths.addAll(readLocalReferencedObjectChecksumPaths(filesIndexPath));
        }
    }

    private void appendRemoteFilesChecksumCoverageRequiredPaths(
            Set<String> requiredPaths,
            BackupTarget target,
            String backupSetName
    ) throws Exception {
        addRequiredRemoteChecksumPath(requiredPaths, target, backupSetName, "files/files-index.json");
        String filesIndexPath = backupSetName + "/files/files-index.json";
        if (smbStorageService.exists(target, filesIndexPath)) {
            requiredPaths.addAll(readRemoteReferencedObjectChecksumPaths(target, filesIndexPath));
        }
    }

    private Set<String> readLocalReferencedObjectChecksumPaths(Path filesIndexPath) throws Exception {
        List<Map<String, Object>> items = objectMapper.readValue(
                filesIndexPath.toFile(),
                new TypeReference<List<Map<String, Object>>>() {}
        );
        return collectReferencedObjectChecksumPaths(items);
    }

    private Set<String> readRemoteReferencedObjectChecksumPaths(BackupTarget target, String filesIndexRelativePath) throws Exception {
        try (InputStream inputStream = smbStorageService.openInputStream(target, filesIndexRelativePath)) {
            List<Map<String, Object>> items = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return collectReferencedObjectChecksumPaths(items);
        }
    }

    private Set<String> collectReferencedObjectChecksumPaths(List<Map<String, Object>> items) {
        Set<String> referencedPaths = new LinkedHashSet<>();
        if (items == null) {
            return referencedPaths;
        }
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            addReferencedChecksumPath(referencedPaths, item.get("storagePath"));
            addReferencedChecksumPath(referencedPaths, item.get("convertedPath"));
            addReferencedChecksumPath(referencedPaths, item.get("previewPath"));
            addReferencedChecksumPath(referencedPaths, item.get("thumbnailPath"));
        }
        return referencedPaths;
    }

    private void addReferencedChecksumPath(Set<String> referencedPaths, Object objectPathValue) {
        String objectPath = trimToNull(stringValue(objectPathValue));
        if (StringUtils.hasText(objectPath)) {
            referencedPaths.add("files/" + objectPath.replace('\\', '/'));
        }
    }

    private VerificationResult validateChecksumCoverage(Set<String> checksummedPaths, Set<String> requiredPaths) {
        for (String requiredPath : requiredPaths) {
            if (!checksummedPaths.contains(requiredPath)) {
                return new VerificationResult(false, BACKUP_SET_CHECKSUM_COVERAGE_FAILURE_MESSAGE);
            }
        }
        return new VerificationResult(true, null);
    }

    private VerificationResult validateLocalConfigBackup(Path configPath, String invalidMessage) {
        return validateLocalConfigBackup(configPath, invalidMessage, true);
    }

    private VerificationResult validateLocalConfigBackup(Path configPath, String invalidMessage, boolean logFailure) {
        if (!Files.exists(configPath)) {
            return new VerificationResult(true, null);
        }
        try {
            List<SysConfig> configs = objectMapper.readValue(
                    configPath.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SysConfig.class)
            );
            return validateConfigEntries(configs, invalidMessage);
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验系统配置备份失败: path={}", configPath, e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateRemoteConfigBackup(
            BackupTarget target,
            String relativePath,
            String invalidMessage
    ) {
        return validateRemoteConfigBackup(target, relativePath, invalidMessage, true);
    }

    private VerificationResult validateRemoteConfigBackup(
            BackupTarget target,
            String relativePath,
            String invalidMessage,
            boolean logFailure
    ) {
        try {
            if (!smbStorageService.exists(target, relativePath)) {
                return new VerificationResult(true, null);
            }
            try (InputStream inputStream = smbStorageService.openInputStream(target, relativePath)) {
                List<SysConfig> configs = objectMapper.readValue(
                        inputStream,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SysConfig.class)
                );
                return validateConfigEntries(configs, invalidMessage);
            }
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验 SMB 系统配置备份失败: relativePath={}, host={}, share={}",
                        relativePath, target.getSmbHost(), target.getSmbShare(), e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateLocalBackupConfig(
            Path backupConfigPath,
            String backupSetName,
            String expectedTargetName,
            boolean required
    ) {
        if (!Files.exists(backupConfigPath)) {
            return required
                    ? new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性")
                    : new VerificationResult(true, null);
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    backupConfigPath.toFile(),
                    new TypeReference<Map<String, Object>>() {}
            );
            return validateBackupConfigPayload(payload, backupSetName, expectedTargetName);
        } catch (Exception e) {
            log.warn("校验备份配置元数据失败: path={}", backupConfigPath, e);
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
    }

    private VerificationResult validateRemoteBackupConfig(
            BackupTarget target,
            String relativePath,
            String backupSetName,
            String expectedTargetName,
            boolean required
    ) {
        try {
            if (!smbStorageService.exists(target, relativePath)) {
                return required
                        ? new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性")
                        : new VerificationResult(true, null);
            }
            try (InputStream inputStream = smbStorageService.openInputStream(target, relativePath)) {
                Map<String, Object> payload = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, Object>>() {}
                );
                return validateBackupConfigPayload(payload, backupSetName, expectedTargetName);
            }
        } catch (Exception e) {
            log.warn("校验 SMB 备份配置元数据失败: relativePath={}, host={}, share={}",
                    relativePath, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
    }

    private VerificationResult validateBackupConfigPayload(
            Map<String, Object> payload,
            String backupSetName,
            String expectedTargetName
    ) {
        if (payload == null) {
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
        String backupNo = trimToNull(stringValue(payload.get("backupNo")));
        if (backupNo == null || (backupSetName != null && !backupSetName.equals(backupNo))) {
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
        Long targetId = longValue(payload.get("targetId"));
        if (targetId == null || targetId < 1L) {
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
        String targetType = trimToNull(stringValue(payload.get("targetType")));
        if (targetType == null
                || (!BackupTarget.TYPE_LOCAL.equals(targetType)
                && !BackupTarget.TYPE_SMB.equals(targetType))) {
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
        String targetName = trimToNull(stringValue(payload.get("targetName")));
        if (expectedTargetName != null && !expectedTargetName.equals(targetName)) {
            return new VerificationResult(false, "备份配置元数据无效，请检查备份文件完整性");
        }
        return new VerificationResult(true, null);
    }

    private VerificationResult validateLocalFilesIndex(Path backupSetPath, Path filesIndexPath, String invalidMessage) {
        return validateLocalFilesIndex(backupSetPath, filesIndexPath, invalidMessage, true);
    }

    private VerificationResult validateLocalFilesIndex(
            Path backupSetPath,
            Path filesIndexPath,
            String invalidMessage,
            boolean logFailure
    ) {
        if (!Files.exists(filesIndexPath)) {
            return new VerificationResult(true, null);
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    filesIndexPath.toFile(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return validateFilesIndexEntries(
                    items,
                    objectPath -> Files.exists(resolvePathWithinBase(backupSetPath.resolve("files"), objectPath, "备份对象路径")),
                    invalidMessage
            );
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验文件索引失败: path={}", filesIndexPath, e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateRemoteFilesIndex(
            BackupTarget target,
            String backupSetName,
            String relativePath,
            String invalidMessage
    ) {
        return validateRemoteFilesIndex(target, backupSetName, relativePath, invalidMessage, true);
    }

    private VerificationResult validateRemoteFilesIndex(
            BackupTarget target,
            String backupSetName,
            String relativePath,
            String invalidMessage,
            boolean logFailure
    ) {
        try {
            if (!smbStorageService.exists(target, relativePath)) {
                return new VerificationResult(true, null);
            }
            try (InputStream inputStream = smbStorageService.openInputStream(target, relativePath)) {
                List<Map<String, Object>> items = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return validateFilesIndexEntries(
                        items,
                        objectPath -> smbStorageService.exists(
                                target,
                                resolveSmbPathWithinBackupSet(backupSetName, "files/" + objectPath)
                        ),
                        invalidMessage
                );
            }
        } catch (Exception e) {
            if (logFailure) {
                log.warn("校验 SMB 文件索引失败: relativePath={}, host={}, share={}",
                        relativePath, target.getSmbHost(), target.getSmbShare(), e);
            }
            return new VerificationResult(false, invalidMessage);
        }
    }

    private VerificationResult validateFilesIndexEntries(
            List<Map<String, Object>> items,
            CheckedPredicate<String> objectExistsChecker,
            String invalidMessage
    ) throws Exception {
        if (items == null) {
            return new VerificationResult(false, invalidMessage);
        }
        Set<String> referencedPaths = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                return new VerificationResult(false, invalidMessage);
            }
            collectReferencedObjectPath(referencedPaths, item.get("storagePath"));
            collectReferencedObjectPath(referencedPaths, item.get("convertedPath"));
            collectReferencedObjectPath(referencedPaths, item.get("previewPath"));
            collectReferencedObjectPath(referencedPaths, item.get("thumbnailPath"));
        }
        for (String objectPath : referencedPaths) {
            if (!objectExistsChecker.test(objectPath)) {
                return new VerificationResult(false, "文件索引引用了缺失的电子文件对象，请检查备份完整性");
            }
        }
        return new VerificationResult(true, null);
    }

    private VerificationResult validateLocalManifestFileStats(
            Path backupSetPath,
            Path filesIndexPath,
            Map<String, Object> manifest,
            String invalidMetadataMessage
    ) {
        if (!Files.exists(filesIndexPath)) {
            return new VerificationResult(true, null);
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    filesIndexPath.toFile(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return validateManifestFileStats(
                    manifest,
                    buildLocalFilesIndexStats(backupSetPath.resolve("files"), items),
                    invalidMetadataMessage
            );
        } catch (Exception e) {
            log.warn("校验本地 manifest 文件统计失败: path={}", backupSetPath, e);
            return new VerificationResult(false, invalidMetadataMessage);
        }
    }

    private VerificationResult validateRemoteManifestFileStats(
            BackupTarget target,
            String backupSetName,
            String filesIndexRelativePath,
            Map<String, Object> manifest,
            String invalidMetadataMessage
    ) {
        try {
            if (!smbStorageService.exists(target, filesIndexRelativePath)) {
                return new VerificationResult(true, null);
            }
            try (InputStream inputStream = smbStorageService.openInputStream(target, filesIndexRelativePath)) {
                List<Map<String, Object>> items = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return validateManifestFileStats(
                        manifest,
                        buildRemoteFilesIndexStats(target, backupSetName, items),
                        invalidMetadataMessage
                );
            }
        } catch (Exception e) {
            log.warn("校验 SMB manifest 文件统计失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, invalidMetadataMessage);
        }
    }

    private VerificationResult validateManifestFileStats(
            Map<String, Object> manifest,
            FilesIndexStats filesIndexStats,
            String invalidMetadataMessage
    ) {
        Long manifestFileCount = longValue(manifest.get("fileCount"));
        if (manifestFileCount != null && manifestFileCount.longValue() != filesIndexStats.fileCount()) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        Long manifestObjectCount = longValue(manifest.get("objectCount"));
        if (manifestObjectCount != null && manifestObjectCount.longValue() != filesIndexStats.objectCount()) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        Long manifestTotalBytes = longValue(manifest.get("totalBytes"));
        if (manifestTotalBytes != null && manifestTotalBytes.longValue() != filesIndexStats.totalBytes()) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        return new VerificationResult(true, null);
    }

    private FilesIndexStats buildLocalFilesIndexStats(Path filesBasePath, List<Map<String, Object>> items) throws Exception {
        Set<String> referencedPaths = collectNormalizedReferencedObjectPaths(items);
        long totalBytes = 0L;
        for (String objectPath : referencedPaths) {
            totalBytes += Files.size(resolvePathWithinBase(filesBasePath, objectPath, "备份对象路径"));
        }
        return new FilesIndexStats(countFilesIndexItems(items), (long) referencedPaths.size(), totalBytes);
    }

    private FilesIndexStats buildRemoteFilesIndexStats(
            BackupTarget target,
            String backupSetName,
            List<Map<String, Object>> items
    ) throws Exception {
        Set<String> referencedPaths = collectNormalizedReferencedObjectPaths(items);
        long totalBytes = 0L;
        for (String objectPath : referencedPaths) {
            String relativePath = resolveSmbPathWithinBackupSet(backupSetName, "files/" + objectPath);
            try (InputStream inputStream = smbStorageService.openInputStream(target, relativePath)) {
                totalBytes += readStreamSize(inputStream);
            }
        }
        return new FilesIndexStats(countFilesIndexItems(items), (long) referencedPaths.size(), totalBytes);
    }

    private Set<String> collectNormalizedReferencedObjectPaths(List<Map<String, Object>> items) {
        Set<String> referencedPaths = new LinkedHashSet<>();
        if (items == null) {
            return referencedPaths;
        }
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            collectNormalizedReferencedObjectPath(referencedPaths, item.get("storagePath"));
            collectNormalizedReferencedObjectPath(referencedPaths, item.get("convertedPath"));
            collectNormalizedReferencedObjectPath(referencedPaths, item.get("previewPath"));
            collectNormalizedReferencedObjectPath(referencedPaths, item.get("thumbnailPath"));
        }
        return referencedPaths;
    }

    private long countFilesIndexItems(List<Map<String, Object>> items) {
        if (items == null) {
            return 0L;
        }
        long fileCount = 0L;
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            fileCount++;
        }
        return fileCount;
    }

    private void collectReferencedObjectPath(Set<String> referencedPaths, Object objectPathValue) {
        String objectPath = stringValue(objectPathValue);
        if (StringUtils.hasText(objectPath)) {
            referencedPaths.add(objectPath);
        }
    }

    private void collectNormalizedReferencedObjectPath(Set<String> referencedPaths, Object objectPathValue) {
        String objectPath = trimToNull(stringValue(objectPathValue));
        if (objectPath != null) {
            referencedPaths.add(objectPath);
        }
    }

    private VerificationResult validateConfigEntries(List<SysConfig> configs, String invalidMessage) {
        if (configs == null) {
            return new VerificationResult(false, invalidMessage);
        }
        Set<String> seenKeys = new HashSet<>();
        for (SysConfig config : configs) {
            if (config == null || !StringUtils.hasText(config.getConfigKey())) {
                return new VerificationResult(false, invalidMessage);
            }
            if (!seenKeys.add(config.getConfigKey())) {
                return new VerificationResult(false, invalidMessage);
            }
            if (RESTORE_SKIPPED_CONFIG_KEYS.contains(config.getConfigKey())) {
                continue;
            }
            try {
                ConfigValueValidator.validateAndNormalizeByType(
                        config.getConfigType(),
                        config.getConfigKey(),
                        config.getConfigValue()
                );
            } catch (BusinessException ex) {
                return new VerificationResult(false, invalidMessage);
            }
        }
        return new VerificationResult(true, null);
    }

    private VerificationResult validateManifestEntries(
            Map<String, Object> manifest,
            String backupSetName,
            String invalidMetadataMessage
    ) {
        if (manifest == null) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (parseDateTime(manifest.get("createdAt")) == null) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (!isNonNegativeLongMetadata(manifest.get("fileCount"))) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (!isNonNegativeLongMetadata(manifest.get("objectCount"))) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (!isNonNegativeLongMetadata(manifest.get("totalBytes"))) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        String backupNo = trimToNull(stringValue(manifest.get("backupNo")));
        if (backupNo != null && backupSetName != null && !backupSetName.equals(backupNo)) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        String databaseFile = trimToNull(stringValue(manifest.get("databaseFile")));
        if (databaseFile != null && !"archive-system.sql".equals(databaseFile)) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        String databaseMode = trimToNull(stringValue(manifest.get("databaseMode")));
        String databaseWarning = trimToNull(stringValue(manifest.get("databaseWarning")));
        if (!validateManifestDatabaseMetadata(databaseMode, databaseWarning)) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        String filesIndex = trimToNull(stringValue(manifest.get("filesIndex")));
        if (filesIndex != null && !"files-index.json".equals(filesIndex)) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        String checksumAlgorithm = trimToNull(stringValue(manifest.get("checksumAlgorithm")));
        if (checksumAlgorithm != null && !CHECKSUM_ALGORITHM.equals(checksumAlgorithm)) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (!validateManifestScopeEntries(manifest.get("scope"))) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        if (!validateManifestConfigFiles(manifest.get("configFiles"))) {
            return new VerificationResult(false, invalidMetadataMessage);
        }
        return new VerificationResult(true, null);
    }

    private boolean validateManifestScopeEntries(Object scopeValue) {
        if (scopeValue == null) {
            return true;
        }
        if (!(scopeValue instanceof List<?> scopes)) {
            return false;
        }
        Set<String> normalizedScopes = new LinkedHashSet<>();
        for (Object item : scopes) {
            String scope = trimToNull(stringValue(item));
            if (!StringUtils.hasText(scope) || !MANIFEST_SCOPE_VALUES.contains(scope) || !normalizedScopes.add(scope)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateManifestDatabaseMetadata(String databaseMode, String databaseWarning) {
        if ("PLACEHOLDER".equals(databaseMode)) {
            return StringUtils.hasText(databaseWarning);
        }
        if ("PG_DUMP".equals(databaseMode)) {
            return databaseWarning == null;
        }
        return databaseWarning == null;
    }

    private VerificationResult validateManifestScopeConsistency(
            Map<String, Object> manifest,
            Set<String> actualScopes,
            String invalidMetadataMessage
    ) {
        Set<String> declaredScopes = readManifestScopeSet(manifest);
        if (declaredScopes == null) {
            return new VerificationResult(true, null);
        }
        return declaredScopes.equals(actualScopes)
                ? new VerificationResult(true, null)
                : new VerificationResult(false, invalidMetadataMessage);
    }

    private VerificationResult validateRemoteManifestScopeConsistency(
            BackupTarget target,
            String backupSetName,
            String databaseMode,
            Map<String, Object> manifest,
            String invalidMetadataMessage
    ) {
        try {
            return validateManifestScopeConsistency(
                    manifest,
                    buildActualScopeSet(
                            smbStorageService.exists(target, backupSetName + "/database/archive-system.sql")
                                    && StringUtils.hasText(databaseMode),
                            smbStorageService.exists(target, backupSetName + "/files/files-index.json"),
                            smbStorageService.exists(target, backupSetName + "/config/sys-config.json")
                    ),
                    invalidMetadataMessage
            );
        } catch (Exception e) {
            log.warn("校验 SMB manifest scope 失败: backupSetName={}, host={}, share={}",
                    backupSetName, target.getSmbHost(), target.getSmbShare(), e);
            return new VerificationResult(false, invalidMetadataMessage);
        }
    }

    private Set<String> readManifestScopeSet(Map<String, Object> manifest) {
        if (manifest == null) {
            return null;
        }
        Object scopeValue = manifest.get("scope");
        if (!(scopeValue instanceof List<?> scopes)) {
            return null;
        }
        Set<String> normalizedScopes = new LinkedHashSet<>();
        for (Object item : scopes) {
            String scope = trimToNull(stringValue(item));
            if (scope != null) {
                normalizedScopes.add(scope);
            }
        }
        return normalizedScopes;
    }

    private Set<String> buildActualScopeSet(boolean hasDatabaseBackup, boolean hasFilesBackup, boolean hasConfigBackup) {
        Set<String> scopes = new LinkedHashSet<>();
        if (hasDatabaseBackup) {
            scopes.add("DATABASE");
        }
        if (hasFilesBackup) {
            scopes.add("FILES");
        }
        if (hasConfigBackup) {
            scopes.add("CONFIG");
        }
        return scopes;
    }

    private boolean validateManifestConfigFiles(Object configFilesValue) {
        if (configFilesValue == null) {
            return true;
        }
        if (!(configFilesValue instanceof List<?> configFiles)) {
            return false;
        }
        Set<String> normalizedFiles = new LinkedHashSet<>();
        for (Object item : configFiles) {
            String fileName = trimToNull(stringValue(item));
            if (!StringUtils.hasText(fileName)) {
                return false;
            }
            normalizedFiles.add(fileName);
        }
        return normalizedFiles.contains("sys-config.json")
                && normalizedFiles.contains("backup-config.json");
    }

    private boolean isBackupConfigExpected(Map<String, Object> manifest) {
        if (manifest == null) {
            return false;
        }
        Object configFilesValue = manifest.get("configFiles");
        if (!(configFilesValue instanceof List<?> configFiles)) {
            return false;
        }
        for (Object item : configFiles) {
            if ("backup-config.json".equals(trimToNull(stringValue(item)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonNegativeLongMetadata(Object value) {
        if (value == null) {
            return true;
        }
        Long parsed = longValue(value);
        return parsed != null && parsed >= 0;
    }

    private boolean isLocalFilesRestorable(Path backupSetPath, Path filesIndexPath, Set<String> checksumPaths) {
        return Files.exists(filesIndexPath)
                && validateLocalFilesChecksumCoverage(backupSetPath, checksumPaths).success()
                && validateLocalFilesIndex(backupSetPath, filesIndexPath, null, false).success();
    }

    private boolean isLocalConfigRestorable(Path configPath, Set<String> checksumPaths) {
        return Files.exists(configPath)
                && checksumPaths.contains("config/sys-config.json")
                && validateLocalConfigBackup(configPath, null, false).success();
    }

    private boolean isRemoteConfigRestorable(BackupTarget target, String relativePath, Set<String> checksumPaths) {
        try {
            return smbStorageService.exists(target, relativePath)
                    && checksumPaths.contains("config/sys-config.json")
                    && validateRemoteConfigBackup(target, relativePath, null, false).success();
        } catch (Exception e) {
            log.warn("检测 SMB 系统配置恢复能力失败: relativePath={}, host={}, share={}",
                    relativePath, target.getSmbHost(), target.getSmbShare(), e);
            return false;
        }
    }

    private boolean isRemoteFilesRestorable(
            BackupTarget target,
            String backupSetName,
            String relativePath,
            Set<String> checksumPaths
    ) {
        try {
            return smbStorageService.exists(target, relativePath)
                    && validateRemoteFilesChecksumCoverage(target, backupSetName, checksumPaths).success()
                    && validateRemoteFilesIndex(target, backupSetName, relativePath, null, false).success();
        } catch (Exception e) {
            log.warn("检测 SMB 文件索引恢复能力失败: relativePath={}, host={}, share={}",
                    relativePath, target.getSmbHost(), target.getSmbShare(), e);
            return false;
        }
    }

    private Set<String> readLocalChecksumPaths(Path checksumsPath) {
        try {
            Map<String, String> checksumEntries = parseChecksumEntries(Files.readAllLines(checksumsPath, StandardCharsets.UTF_8));
            return checksumEntries == null ? Set.of() : checksumEntries.keySet();
        } catch (Exception e) {
            log.warn("读取本地 checksums 路径集合失败: path={}", checksumsPath, e);
            return Set.of();
        }
    }

    private Set<String> readRemoteChecksumPaths(BackupTarget target, String checksumsRelativePath) {
        try (InputStream inputStream = smbStorageService.openInputStream(target, checksumsRelativePath)) {
            Map<String, String> checksumEntries = parseChecksumEntries(
                    new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines().toList()
            );
            return checksumEntries == null ? Set.of() : checksumEntries.keySet();
        } catch (Exception e) {
            log.warn("读取 SMB checksums 路径集合失败: relativePath={}, host={}, share={}",
                    checksumsRelativePath, target.getSmbHost(), target.getSmbShare(), e);
            return Set.of();
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

    private long readStreamSize(InputStream inputStream) throws Exception {
        try (inputStream) {
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
            }
            return total;
        }
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
                .sorted(backupSetRetentionComparator())
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

    private String buildFailureReport(RestoreJob restoreJob, Map<String, Object> report) {
        try {
            report.put("status", RestoreJob.STATUS_FAILED);
            report.put("errorMessage", RESTORE_FAILURE_PUBLIC_MESSAGE);
            updateRestoreReportCommonFields(restoreJob, report);
            return objectMapper.writeValueAsString(report);
        } catch (Exception ex) {
            return "{\"status\":\"FAILED\",\"errorMessage\":\"" + RESTORE_FAILURE_PUBLIC_MESSAGE + "\"}";
        }
    }

    private void updateRestoreReportCommonFields(RestoreJob restoreJob, Map<String, Object> report) {
        report.put("backupSetName", restoreJob.getBackupSetName());
        report.put("startedAt", restoreJob.getStartedAt());
        report.put("finishedAt", restoreJob.getFinishedAt());
        report.put("rebuildIndexStatus", restoreJob.getRebuildIndexStatus());
        report.put("restoredDatabase", restoreJob.getRestoredDatabase());
        report.put("restoredFiles", restoreJob.getRestoredFiles());
        report.put("restoredConfig", restoreJob.getRestoredConfig());
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

    private void ensureDatabaseRestoreTargetEmpty() {
        List<String> tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_name",
                String.class
        );
        List<String> nonEmptyTables = new ArrayList<>();
        for (String tableName : tableNames) {
            if (!StringUtils.hasText(tableName) || PRESERVED_DATABASE_TABLES.contains(tableName)) {
                continue;
            }
            String existsSql = "SELECT EXISTS (SELECT 1 FROM " + quoteIdentifier(tableName) + " LIMIT 1)";
            Boolean hasRows = jdbcTemplate.queryForObject(existsSql, Boolean.class);
            if (Boolean.TRUE.equals(hasRows)) {
                nonEmptyTables.add(tableName);
            }
        }
        if (!nonEmptyTables.isEmpty()) {
            throw new BusinessException("当前数据库业务表并非空库，以下数据表仍有数据: " + String.join(", ", nonEmptyTables));
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private Path resolvePathWithinBase(Path basePath, String relativePath, String fieldName) {
        String normalizedRelativePath = normalizeBackupRelativePath(relativePath, fieldName);
        Path normalizedBasePath = basePath.toAbsolutePath().normalize();
        Path resolvedPath = normalizedBasePath.resolve(normalizedRelativePath).normalize();
        if (!resolvedPath.startsWith(normalizedBasePath)) {
            throw new BusinessException(fieldName + "超出允许目录范围: " + relativePath);
        }
        return resolvedPath;
    }

    private String resolveSmbPathWithinBackupSet(String backupSetName, String relativePath) {
        String normalizedRelativePath = normalizeBackupRelativePath(relativePath, "SMB 备份校验路径");
        String prefix = trimSlashes(backupSetName);
        return prefix + "/" + normalizedRelativePath;
    }

    private String normalizeBackupRelativePath(String relativePath, String fieldName) {
        String candidate = trimToNull(relativePath);
        if (!StringUtils.hasText(candidate)) {
            throw new BusinessException(fieldName + "不能为空");
        }
        candidate = candidate.replace('\\', '/');
        if (candidate.startsWith("/") || candidate.contains("//")) {
            throw new BusinessException(fieldName + "格式非法: " + relativePath);
        }
        Path normalizedPath = Path.of(candidate).normalize();
        if (normalizedPath.isAbsolute()) {
            throw new BusinessException(fieldName + "不能为绝对路径: " + relativePath);
        }
        String normalized = normalizedPath.toString().replace('\\', '/');
        if (!StringUtils.hasText(normalized) || ".".equals(normalized) || normalized.startsWith("../")) {
            throw new BusinessException(fieldName + "超出允许目录范围: " + relativePath);
        }
        return normalized;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String buildBackupSetDisplayPath(BackupTarget target, String backupSetName) {
        if (target != null && StringUtils.hasText(backupSetName)) {
            if (BackupTarget.TYPE_LOCAL.equals(target.getTargetType()) && StringUtils.hasText(target.getLocalPath())) {
                return Path.of(target.getLocalPath(), backupSetName).toString();
            }
            if (BackupTarget.TYPE_SMB.equals(target.getTargetType())
                    && StringUtils.hasText(target.getSmbHost())
                    && StringUtils.hasText(target.getSmbShare())) {
                return smbStorageService.buildDisplayPath(target, backupSetName);
            }
        }
        String targetLabel = target == null ? null : trimToNull(target.getName());
        if (!StringUtils.hasText(targetLabel)) {
            targetLabel = target == null ? "BACKUP" : firstNonNull(trimToNull(target.getTargetType()), "BACKUP");
        }
        return StringUtils.hasText(backupSetName) ? targetLabel + " / " + backupSetName : targetLabel;
    }

    private PageRequest normalizePageRequest(Integer pageNum, Integer pageSize) {
        long normalizedPageNum = pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum.longValue();
        long normalizedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize.longValue(), MAX_PAGE_SIZE);
        return new PageRequest(normalizedPageNum, normalizedPageSize);
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
        List<String> command = new ArrayList<>(List.of(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", datasourceUsername,
                "-d", database,
                "--data-only",
                "--no-owner",
                "--no-privileges"
        ));
        for (String tableName : PRESERVED_DATABASE_TABLES) {
            command.add("--exclude-table-data=public." + tableName);
        }
        return new PgDumpCommand(command, datasourcePassword);
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

    private BackupTargetResponse toResponse(BackupTarget entity, boolean includeConnectionDetails) {
        return BackupTargetResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .targetType(entity.getTargetType())
                .enabled(entity.getEnabled())
                .displayAddress(buildTargetDisplayAddress(entity))
                .localPath(includeConnectionDetails ? entity.getLocalPath() : null)
                .smbHost(includeConnectionDetails ? entity.getSmbHost() : null)
                .smbPort(includeConnectionDetails ? entity.getSmbPort() : null)
                .smbShare(includeConnectionDetails ? entity.getSmbShare() : null)
                .smbUsername(includeConnectionDetails ? entity.getSmbUsername() : null)
                .smbSubPath(includeConnectionDetails ? entity.getSmbSubPath() : null)
                .remarks(entity.getRemarks())
                .verifyStatus(entity.getVerifyStatus())
                .verifyMessage(entity.getVerifyMessage())
                .build();
    }

    private String buildTargetDisplayAddress(BackupTarget entity) {
        if (BackupTarget.TYPE_LOCAL.equals(entity.getTargetType())) {
            return firstNonNull(trimToNull(entity.getLocalPath()), trimToNull(entity.getName()), "LOCAL");
        }
        if (BackupTarget.TYPE_SMB.equals(entity.getTargetType())) {
            String smbHost = trimToNull(entity.getSmbHost());
            String smbShare = trimToNull(entity.getSmbShare());
            if (smbHost != null && smbShare != null) {
                Integer smbPort = entity.getSmbPort();
                String smbSubPath = trimToNull(entity.getSmbSubPath());
                if (smbPort != null && smbPort != 445) {
                    StringBuilder builder = new StringBuilder("smb://")
                            .append(smbHost)
                            .append(":")
                            .append(smbPort)
                            .append("/")
                            .append(smbShare);
                    if (smbSubPath != null) {
                        String normalizedSubPath = smbSubPath.replace('\\', '/');
                        while (normalizedSubPath.startsWith("/")) {
                            normalizedSubPath = normalizedSubPath.substring(1);
                        }
                        if (!normalizedSubPath.isEmpty()) {
                            builder.append("/").append(normalizedSubPath);
                        }
                    }
                    return builder.toString();
                }
                StringBuilder builder = new StringBuilder("\\\\")
                        .append(smbHost)
                        .append("\\")
                        .append(smbShare);
                if (smbSubPath != null) {
                    String normalizedSubPath = smbSubPath.replace('/', '\\');
                    while (normalizedSubPath.startsWith("\\")) {
                        normalizedSubPath = normalizedSubPath.substring(1);
                    }
                    if (!normalizedSubPath.isEmpty()) {
                        builder.append("\\").append(normalizedSubPath);
                    }
                }
                return builder.toString();
            }
            return firstNonNull(trimToNull(entity.getName()), "SMB");
        }
        return firstNonNull(trimToNull(entity.getName()), trimToNull(entity.getTargetType()), "BACKUP");
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

    private Comparator<BackupSetResponse> backupSetRetentionComparator() {
        return Comparator
                .comparing(this::isReadyBackupSet, Comparator.reverseOrder())
                .thenComparing(backupSetComparator());
    }

    private boolean isReadyBackupSet(BackupSetResponse backupSet) {
        return backupSet != null && "READY".equals(backupSet.getVerifyStatus());
    }

    private record VerificationResult(boolean success, String message) {
    }

    @FunctionalInterface
    private interface CheckedPredicate<T> {
        boolean test(T value) throws Exception;
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

    private record FilesIndexStats(long fileCount, long objectCount, long totalBytes) {
    }

    private record PageRequest(long pageNum, long pageSize) {
    }
}
