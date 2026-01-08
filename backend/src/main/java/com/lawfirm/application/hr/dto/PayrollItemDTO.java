package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工资明细 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollItemDTO extends BaseDTO {

    private Long payrollSheetId;
    private Long employeeId;
    private Long userId;
    private String employeeNo;
    private String employeeName;
    private BigDecimal grossAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private String confirmStatus;
    private String confirmStatusName;
    private LocalDateTime confirmedAt;
    private String confirmComment;
    private LocalDateTime confirmDeadline; // 确认截止时间
    
    // 收入项列表
    private List<PayrollIncomeDTO> incomes;
    
    // 扣减项列表
    private List<PayrollDeductionDTO> deductions;
}

