package com.lawfirm.application.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    // 前端字段映射 - 为了兼容前端期望的字段名
    @JsonProperty("action")
    public String getAction() {
        return operationType;
    }

    @JsonProperty("operatorName")
    public String getOperatorName() {
        return userName;
    }

    @JsonProperty("operatorIp")
    public String getOperatorIp() {
        return ipAddress;
    }

    @JsonProperty("duration")
    public Long getDuration() {
        return executionTime;
    }

    @JsonProperty("operationTime")
    public String getOperationTime() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    @JsonProperty("errorMsg")
    public String getErrorMsg() {
        return errorMessage;
    }
}
