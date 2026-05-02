package com.archivesystem.dto.backup;

import com.archivesystem.entity.RestoreJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 恢复任务响应 DTO，避免暴露内部操作人标识和审计元数据.
 */
@Value
@Builder
public class RestoreJobResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    String restoreNo;
    String targetName;
    String backupSetName;
    String status;
    String statusMessage;
    boolean followUpRequired;
    String verifyStatus;
    String rebuildIndexStatus;
    String restoreReport;
    LocalDateTime createdAt;

    public static RestoreJobResponse from(RestoreJob job) {
        if (job == null) {
            return null;
        }

        boolean followUpRequired = isFollowUpRequired(job);
        return RestoreJobResponse.builder()
                .restoreNo(job.getRestoreNo())
                .targetName(job.getTargetName())
                .backupSetName(job.getBackupSetName())
                .status(job.getStatus())
                .statusMessage(buildStatusMessage(job))
                .followUpRequired(followUpRequired)
                .verifyStatus(job.getVerifyStatus())
                .rebuildIndexStatus(job.getRebuildIndexStatus())
                .restoreReport(job.getRestoreReport())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private static String buildStatusMessage(RestoreJob job) {
        String status = job.getStatus();
        if (RestoreJob.STATUS_SUCCESS.equals(status)) {
            if (hasReportFailedStep(job, "MAINTENANCE")) {
                return "恢复任务已完成，但退出维护模式失败，请手动处理";
            }
            if ("SKIPPED".equals(job.getRebuildIndexStatus())) {
                return "恢复任务已完成，未执行索引重建";
            }
            return "恢复任务已完成";
        }
        if (RestoreJob.STATUS_FAILED.equals(status)) {
            List<String> completedSteps = new ArrayList<>();
            if (Boolean.TRUE.equals(job.getRestoredDatabase())) {
                completedSteps.add("数据库恢复");
            }
            if (Boolean.TRUE.equals(job.getRestoredFiles())) {
                completedSteps.add("电子文件恢复");
            }
            if (Boolean.TRUE.equals(job.getRestoredConfig())) {
                completedSteps.add("系统配置恢复");
            }
            if ("SUCCESS".equals(job.getRebuildIndexStatus())) {
                completedSteps.add("索引重建");
            }
            if (!completedSteps.isEmpty()) {
                return "恢复执行失败，已完成：" + String.join("、", completedSteps);
            }
            return "恢复执行失败，请联系系统管理员查看系统日志";
        }
        if (RestoreJob.STATUS_RUNNING.equals(status)) {
            return "恢复任务执行中";
        }
        if (RestoreJob.STATUS_PENDING.equals(status)) {
            return "恢复任务等待执行";
        }
        return "恢复任务状态未知";
    }

    private static boolean isFollowUpRequired(RestoreJob job) {
        return RestoreJob.STATUS_SUCCESS.equals(job.getStatus()) && hasReportFailedStep(job, "MAINTENANCE");
    }

    private static boolean hasReportFailedStep(RestoreJob job, String step) {
        if (job == null || job.getRestoreReport() == null || step == null) {
            return false;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(job.getRestoreReport());
            JsonNode steps = root.path("steps");
            if (!steps.isArray()) {
                return false;
            }
            for (JsonNode item : steps) {
                if (step.equals(item.path("step").asText()) && "FAILED".equals(item.path("status").asText())) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
