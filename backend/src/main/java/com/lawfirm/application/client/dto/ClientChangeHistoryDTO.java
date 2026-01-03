package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 企业变更历史DTO（M2-014）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientChangeHistoryDTO extends BaseDTO {
    private Long clientId;
    private String clientName;
    private String changeType;
    private String changeTypeName;
    private LocalDate changeDate;
    private String beforeValue;
    private String afterValue;
    private String changeDescription;
    private String registrationAuthority;
    private String registrationNumber;
    private String attachmentUrl;
}

