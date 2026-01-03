package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新案件命令
 */
@Data
public class UpdateMatterCommand {

    @NotNull(message = "案件ID不能为空")
    private Long id;

    private String name;
    private String matterType;
    private String businessType;
    private Long clientId;
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
    private Long originatorId;
    private Long leadLawyerId;
    private Long departmentId;
    private String feeType;
    private BigDecimal estimatedFee;
    private BigDecimal actualFee;
    private LocalDate filingDate;
    private LocalDate expectedClosingDate;
    private LocalDate actualClosingDate;
    private BigDecimal claimAmount;
    private String outcome;
    private Long contractId;
    private String remark;
}

