package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.dto.backup.BackupOverview;
import com.archivesystem.dto.backup.BackupSetResponse;
import com.archivesystem.dto.backup.BackupTargetRequest;
import com.archivesystem.dto.backup.BackupTargetResponse;
import com.archivesystem.dto.backup.RestoreExecuteRequest;
import com.archivesystem.dto.backup.RestoreMaintenanceStatus;
import com.archivesystem.entity.BackupJob;
import com.archivesystem.entity.RestoreJob;

import java.util.List;

/**
 * 备份恢复中心服务.
 * @author junyuzhan
 */
public interface BackupService {

    BackupOverview getOverview();

    List<BackupTargetResponse> getTargets();

    BackupTargetResponse getTarget(Long id);

    BackupTargetResponse createTarget(BackupTargetRequest request);

    BackupTargetResponse updateTarget(Long id, BackupTargetRequest request);

    void deleteTarget(Long id);

    BackupTargetResponse verifyTarget(Long id);

    List<BackupSetResponse> getBackupSets(Long targetId);

    BackupJob runManualBackup(Long targetId);

    BackupJob runScheduledBackup(Long targetId);

    RestoreMaintenanceStatus getRestoreMaintenanceStatus();

    RestoreMaintenanceStatus setRestoreMaintenanceMode(Boolean enabled);

    RestoreJob runRestore(RestoreExecuteRequest request);

    PageResult<BackupJob> getBackupJobs(Integer pageNum, Integer pageSize);

    PageResult<RestoreJob> getRestoreJobs(Integer pageNum, Integer pageSize);
}
