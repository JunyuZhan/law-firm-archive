package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收费记录 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeDTO extends BaseDTO {

    private String feeNo;
    private Long contractId;
    private String contractName;
    private Long matterId;
    private String matterName;
    private Long clientId;
    private String clientName;
    private String feeType;
    private String feeTypeName;
    private String feeName;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String currency;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private String status;
    private String statusName;
    private Long responsibleId;
    private String responsibleName;
    private String remark;
    
    /**
     * 收款记录
     */
    private List<PaymentDTO> payments;
}

