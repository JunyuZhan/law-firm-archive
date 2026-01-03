package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 请假申请DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveApplicationDTO extends BaseDTO {
    private Long id;
    private String applicationNo;
    private Long userId;
    private String userName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal duration;
    private String reason;
    private String attachmentUrl;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private LocalDateTime createdAt;
}
