package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预收款核销记录DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentUsageDTO extends BaseDTO {

    private Long prepaymentId;
    private String prepaymentNo;
    private Long feeId;
    private String feeNo;
    private String feeName;
    private Long matterId;
    private String matterNo;
    private String matterName;
    private BigDecimal amount;
    private LocalDateTime usageTime;
    private Long operatorId;
    private String operatorName;
    private String remark;
}
