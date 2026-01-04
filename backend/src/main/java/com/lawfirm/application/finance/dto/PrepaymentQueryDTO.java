package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预收款查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentQueryDTO extends PageQuery {

    private String prepaymentNo;
    private Long clientId;
    private Long contractId;
    private Long matterId;
    private String status;
}
