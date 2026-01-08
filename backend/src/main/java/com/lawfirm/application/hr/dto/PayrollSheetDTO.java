package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工资表 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSheetDTO extends BaseDTO {

    private String payrollNo;
    private Integer payrollYear;
    private Integer payrollMonth;
    private String status;
    private String statusName;
    private Integer totalEmployees;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalDeductionAmount;
    private BigDecimal totalNetAmount;
    private Integer confirmedCount;
    private LocalDateTime submittedAt;
    private Long submittedBy;
    private String submittedByName;
    private LocalDateTime financeConfirmedAt;
    private Long financeConfirmedBy;
    private String financeConfirmedByName;
    private LocalDateTime issuedAt;
    private Long issuedBy;
    private String issuedByName;
    private String paymentMethod;
    private String paymentMethodName;
    private String paymentVoucherUrl;
    private String remark;
    private LocalDateTime autoConfirmDeadline;
    
    // 审批相关字段
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private String approvedByName;
    private String approvalComment;
    
    // 工资明细列表（详情时使用）
    private List<PayrollItemDTO> items;
    
    // 未确认的员工列表（包含拒绝理由）
    private List<PayrollItemDTO> rejectedItems;
}

