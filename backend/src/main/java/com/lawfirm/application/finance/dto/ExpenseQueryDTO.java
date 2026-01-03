package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 费用报销查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseQueryDTO extends PageQuery {

    private String expenseNo;
    private Long matterId;
    private Long applicantId;
    private String status;
    private String expenseType;
    private String expenseCategory;
}

