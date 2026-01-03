package com.lawfirm.application.client.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新案源命令
 */
@Data
public class UpdateLeadCommand {

    private String leadName;
    private String leadType;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String sourceChannel;
    private String sourceDetail;
    private String status;
    private String priority;
    private String businessType;
    private BigDecimal estimatedAmount;
    private String description;
    private LocalDateTime nextFollowTime;
    private Long responsibleUserId;
    private String remark;
}

