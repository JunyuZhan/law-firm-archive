package com.archivesystem.dto.backup;

import lombok.Builder;
import lombok.Value;

/**
 * 备份目标摘要响应.
 */
@Value
@Builder
public class BackupTargetSummaryResponse {

    Long id;
    String name;
    String targetType;
    Boolean enabled;
    String displayAddress;
    String verifyStatus;
    String verifyMessage;

    public static BackupTargetSummaryResponse from(BackupTargetResponse response) {
        if (response == null) {
            return null;
        }

        return BackupTargetSummaryResponse.builder()
                .id(response.getId())
                .name(response.getName())
                .targetType(response.getTargetType())
                .enabled(response.getEnabled())
                .displayAddress(response.getDisplayAddress())
                .verifyStatus(response.getVerifyStatus())
                .verifyMessage(response.getVerifyMessage())
                .build();
    }
}
