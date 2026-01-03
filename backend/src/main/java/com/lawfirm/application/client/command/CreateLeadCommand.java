package com.lawfirm.application.client.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建案源命令
 */
@Data
public class CreateLeadCommand {

    @NotBlank(message = "案源名称不能为空")
    private String leadName;

    private String leadType;  // INDIVIDUAL, ENTERPRISE
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String sourceChannel;
    private String sourceDetail;
    private String priority;  // HIGH, NORMAL, LOW
    private String businessType;  // LITIGATION, NON_LITIGATION
    private BigDecimal estimatedAmount;
    private String description;
    private LocalDateTime nextFollowTime;
    private Long originatorId;
    private Long responsibleUserId;
    private String remark;
}

