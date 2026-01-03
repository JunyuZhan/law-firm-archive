package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户联系记录 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientContactRecordDTO extends BaseDTO {

    private Long clientId;
    private String clientName;
    private Long contactId;
    private String contactPerson;
    private String contactMethod;
    private String contactMethodName;
    private LocalDateTime contactDate;
    private Integer contactDuration;
    private String contactLocation;
    private String contactContent;
    private String contactResult;
    private LocalDate nextFollowUpDate;
    private Boolean followUpReminder;
    private Long createdBy;
    private String createdByName;
}

