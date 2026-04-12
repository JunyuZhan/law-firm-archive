package com.archivesystem.dto.backup;

import lombok.Builder;
import lombok.Data;

/**
 * 备份恢复概览.
 * @author junyuzhan
 */
@Data
@Builder
public class BackupOverview {
    private long enabledTargetCount;
    private long verifiedTargetCount;
    private long pendingBackupJobs;
    private long recentBackupFailures;
    private long recentRestoreFailures;
    private String currentPhase;
}
