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
    
    /**
     * 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政
     */
    private String caseType;
    private String caseTypeName;
    
    /**
     * 案由代码
     */
    private String causeOfAction;
    
    /**
     * 案由名称
     */
    private String causeOfActionName;
    
    /**
     * 主要客户ID（向后兼容）
     */
    private Long clientId;
    
    /**
     * 主要客户名称
     */
    private String clientName;
    
    /**
     * 客户列表（多客户支持）
     */
    private List<MatterClientDTO> clients;
    
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
    private String contractNo;
    private BigDecimal contractAmount;
    private String remark;
    private String conflictStatus;
    
    /**
     * 团队成员列表
     */
    private List<MatterParticipantDTO> participants;
}

