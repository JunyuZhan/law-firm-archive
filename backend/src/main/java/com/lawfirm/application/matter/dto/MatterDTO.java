package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 案件 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterDTO extends BaseDTO {

    private String matterNo;
    private String name;
    private String matterType;
    private String matterTypeName;
    private String businessType;
    private Long clientId;
    private String clientName;
    private String opposingParty;
    
    /**
     * 对方律师信息
     */
    private String opposingLawyerName;
    private String opposingLawyerLicenseNo;
    private String opposingLawyerFirm;
    private String opposingLawyerPhone;
    private String opposingLawyerEmail;
    
    private String description;
    private String status;
    private String statusName;
    private Long originatorId;
    private String originatorName;
    private Long leadLawyerId;
    private String leadLawyerName;
    private Long departmentId;
    private String departmentName;
    private String feeType;
    private String feeTypeName;
    private BigDecimal estimatedFee;
    private BigDecimal actualFee;
    private LocalDate filingDate;
    private LocalDate expectedClosingDate;
    private LocalDate actualClosingDate;
    private BigDecimal claimAmount;
    private String outcome;
    private Long contractId;
    private String remark;
    private String conflictStatus;
    
    /**
     * 团队成员列表
     */
    private List<MatterParticipantDTO> participants;
}

