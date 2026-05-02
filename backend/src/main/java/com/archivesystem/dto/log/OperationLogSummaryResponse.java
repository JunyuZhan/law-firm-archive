package com.archivesystem.dto.log;

import com.archivesystem.entity.OperationLog;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 操作日志摘要响应 DTO.
 */
@Value
@Builder
public class OperationLogSummaryResponse {

    Long id;
    String objectType;
    String objectId;
    String operationType;
    String operationDesc;
    String operatorName;
    String operatorIp;
    LocalDateTime operatedAt;

    public static OperationLogSummaryResponse from(OperationLog log) {
        if (log == null) {
            return null;
        }

        return OperationLogSummaryResponse.builder()
                .id(log.getId())
                .objectType(log.getObjectType())
                .objectId(log.getObjectId())
                .operationType(log.getOperationType())
                .operationDesc(log.getOperationDesc())
                .operatorName(log.getOperatorName())
                .operatorIp(log.getOperatorIp())
                .operatedAt(log.getOperatedAt())
                .build();
    }
}
