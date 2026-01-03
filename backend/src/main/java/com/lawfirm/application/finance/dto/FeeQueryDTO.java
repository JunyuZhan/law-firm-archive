package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 收费记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeQueryDTO extends PageQuery {

    private String feeNo;
    private Long contractId;
    private Long matterId;
    private Long clientId;
    private String feeType;
    private String status;
    private LocalDate plannedDateFrom;
    private LocalDate plannedDateTo;
    private Long responsibleId;
}

