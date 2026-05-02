package com.archivesystem.dto.backup;

import com.archivesystem.entity.BackupJob;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 备份任务响应DTO.
 */
@Value
@Builder
public class BackupJobResponse {

    private static final Set<String> DATABASE_BACKUP_WARNING_MESSAGES = Set.of(
            "未识别 PostgreSQL 连接信息，已生成占位数据库文件",
            "pg_dump 执行失败，已写入占位文件",
            "pg_dump 不可用，已写入失败信息"
    );

    String backupNo;
    String targetName;
    String status;
    String statusMessage;
    boolean followUpRequired;

    public static BackupJobResponse from(BackupJob job) {
        if (job == null) {
            return null;
        }

        boolean followUpRequired = isFollowUpRequired(job);

        return BackupJobResponse.builder()
                .backupNo(job.getBackupNo())
                .targetName(job.getTargetName())
                .status(job.getStatus())
                .statusMessage(buildStatusMessage(job))
                .followUpRequired(followUpRequired)
                .build();
    }

    private static String buildStatusMessage(BackupJob job) {
        String status = job.getStatus();
        if (BackupJob.STATUS_SUCCESS.equals(status)) {
            if (isFollowUpRequired(job)) {
                return "备份任务已完成，但数据库备份不可用，请检查系统环境";
            }
            if (job.getFileCount() != null) {
                return "备份任务已完成，已备份" + job.getFileCount() + "个文件记录";
            }
            return "备份任务已完成";
        }
        if (BackupJob.STATUS_FAILED.equals(status)) {
            return "备份执行失败，请联系系统管理员查看系统日志";
        }
        if (BackupJob.STATUS_RUNNING.equals(status)) {
            return "备份任务执行中";
        }
        if (BackupJob.STATUS_PENDING.equals(status)) {
            return "备份任务等待执行";
        }
        return "备份任务状态未知";
    }

    private static boolean isFollowUpRequired(BackupJob job) {
        return job != null
                && BackupJob.STATUS_SUCCESS.equals(job.getStatus())
                && isDatabaseBackupUnavailableWarning(job.getErrorMessage());
    }

    private static boolean isDatabaseBackupUnavailableWarning(String errorMessage) {
        return StringUtils.hasText(errorMessage)
                && DATABASE_BACKUP_WARNING_MESSAGES.contains(errorMessage.trim());
    }
}
