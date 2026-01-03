package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用印申请DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealApplicationDTO extends BaseDTO {

    private Long id;
    private String applicationNo;
    
    private Long applicantId;
    private String applicantName;
    private Long departmentId;
    private String departmentName;
    
    private Long sealId;
    private String sealName;
    private String sealType;
    
    private Long matterId;
    private String matterName;
    
    private String documentName;
    private String documentType;
    private Integer copies;
    private String usePurpose;
    
    private LocalDate expectedUseDate;
    private LocalDate actualUseDate;
    
    private String status;
    private String statusName;
    
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    
    private Long usedBy;
    private String usedByName;
    private LocalDateTime usedAt;
    private String useRemark;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
