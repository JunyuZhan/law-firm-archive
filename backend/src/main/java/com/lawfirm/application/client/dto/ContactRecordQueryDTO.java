package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 联系记录查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContactRecordQueryDTO extends PageQuery {

    private Long clientId;
    private Long contactId;
    private String contactMethod;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean followUpReminder; // 是否设置提醒
}

