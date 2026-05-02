package com.archivesystem.dto.backup;

import com.archivesystem.entity.RestoreJob;
import lombok.Builder;
import lombok.Value;

/**
 * 恢复任务启动响应 DTO.
 */
@Value
@Builder
public class RestoreJobSubmitResponse {

    String restoreNo;
    String targetName;
    String backupSetName;
    String status;
    String statusMessage;
    boolean followUpRequired;

    public static RestoreJobSubmitResponse from(RestoreJob job) {
        if (job == null) {
            return null;
        }

        RestoreJobResponse response = RestoreJobResponse.from(job);

        return RestoreJobSubmitResponse.builder()
                .restoreNo(job.getRestoreNo())
                .targetName(job.getTargetName())
                .backupSetName(job.getBackupSetName())
                .status(job.getStatus())
                .statusMessage(response.getStatusMessage())
                .followUpRequired(response.isFollowUpRequired())
                .build();
    }
}
