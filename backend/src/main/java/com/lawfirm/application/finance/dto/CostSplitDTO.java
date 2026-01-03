package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本分摊 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CostSplitDTO extends BaseDTO {

    private Long expenseId;
    private String expenseNo;
    private String expenseDescription;
    private String expenseType;
    private LocalDate expenseDate;

    private Long matterId;
    private String matterName; // 项目名称

    private BigDecimal splitAmount;
    private BigDecimal splitRatio;
    private String splitMethod;
    private LocalDate splitDate;
    private Long splitBy;
    private String splitByName; // 操作人姓名

    private String remark;
}

