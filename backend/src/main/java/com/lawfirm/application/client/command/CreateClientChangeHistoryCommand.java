package com.lawfirm.application.client.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建企业变更历史命令（M2-014）
 */
@Data
public class CreateClientChangeHistoryCommand {
    private Long clientId;
    private String changeType;
    private LocalDate changeDate;
    private String beforeValue;
    private String afterValue;
    private String changeDescription;
    private String registrationAuthority;
    private String registrationNumber;
    private String attachmentUrl;
}

