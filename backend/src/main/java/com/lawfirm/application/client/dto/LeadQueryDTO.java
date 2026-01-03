package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案源查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeadQueryDTO extends PageQuery {

    private String leadName;
    private String status;
    private Long originatorId;
    private Long responsibleUserId;
    private String sourceChannel;
}

