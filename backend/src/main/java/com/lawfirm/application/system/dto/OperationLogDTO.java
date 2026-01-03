package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogDTO extends BaseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String ipAddress;
    private Long executionTime;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
