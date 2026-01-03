package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时记录DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TimesheetDTO extends BaseDTO {

    private Long id;
    private String timesheetNo;
    
    private Long matterId;
    private String matterName;
    private String matterNo;
    
    private Long userId;
    private String userName;
    
    private LocalDate workDate;
    private BigDecimal hours;
    
    private String workType;
    private String workTypeName;
    private String workContent;
    
    private Boolean billable;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
    
    private String status;
    private String statusName;
    
    private LocalDateTime submittedAt;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
