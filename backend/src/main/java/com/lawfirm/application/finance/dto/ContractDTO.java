package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDTO extends BaseDTO {

    private String contractNo;
    private String name;
    private String contractType;
    private String contractTypeName;
    private Long clientId;
    private String clientName;
    private Long matterId;
    private String matterName;
    private String feeType;
    private String feeTypeName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String currency;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String statusName;
    private Long signerId;
    private String signerName;
    private Long departmentId;
    private String departmentName;
    private String paymentTerms;
    private String fileUrl;
    private String remark;
}

