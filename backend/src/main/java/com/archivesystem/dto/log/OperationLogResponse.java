package com.archivesystem.dto.log;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志响应 DTO.
 */
@Data
@Builder
public class OperationLogResponse {
    private Long id;
    private Long archiveId;
    private String objectType;
    private String objectId;
    private String operationType;
    private String operationDesc;
    private Long operatorId;
    private String operatorName;
    private String operatorIp;
    private String operatorUa;
    private LocalDateTime operatedAt;
    private Map<String, Object> operationDetail;
}
