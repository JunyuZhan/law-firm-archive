package com.lawfirm.application.matter.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 工时汇总DTO
 */
@Data
public class TimesheetSummaryDTO {

    private Long userId;
    private String userName;
    
    private Long matterId;
    private String matterName;
    
    private Integer year;
    private Integer month;
    
    /**
     * 总工时
     */
    private BigDecimal totalHours;
    
    /**
     * 计费工时
     */
    private BigDecimal billableHours;
    
    /**
     * 非计费工时
     */
    private BigDecimal nonBillableHours;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 草稿数
     */
    private Integer draftCount;
    
    /**
     * 已提交数
     */
    private Integer submittedCount;
    
    /**
     * 已批准数
     */
    private Integer approvedCount;
    
    /**
     * 已拒绝数
     */
    private Integer rejectedCount;
}
