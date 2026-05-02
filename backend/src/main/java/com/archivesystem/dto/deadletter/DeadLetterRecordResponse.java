package com.archivesystem.dto.deadletter;

import com.archivesystem.entity.DeadLetterRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 死信记录响应DTO.
 */
@Value
@Builder
public class DeadLetterRecordResponse {

    Long id;
    String errorMessage;
    String status;

    public static DeadLetterRecordResponse from(DeadLetterRecord record) {
        if (record == null) {
            return null;
        }

        return DeadLetterRecordResponse.builder()
                .id(record.getId())
                .errorMessage(record.getErrorMessage())
                .status(record.getStatus())
                .build();
    }
}
