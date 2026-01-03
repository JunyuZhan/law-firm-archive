package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提成查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommissionQueryDTO extends PageQuery {

    private Long paymentId;
    private Long matterId;
    private Long userId;
    private String status;
}

