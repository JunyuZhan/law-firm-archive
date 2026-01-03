package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本归集 DTO
 */
@Data
public class CostAllocationDTO {

    private Long id;
    private Long matterId;
    private String matterName;
    private Long expenseId;
    private String expenseNo;
    private String expenseDescription;
    private String expenseType;
    private LocalDate expenseDate;
    private BigDecimal allocatedAmount;
    private LocalDate allocationDate;
    private Long allocatedBy;
    private String allocatedByName;
    private String remark;
    private LocalDate createdAt;
}

