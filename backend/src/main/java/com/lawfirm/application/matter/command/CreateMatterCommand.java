package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建案件命令
 */
@Data
public class CreateMatterCommand {

    @NotBlank(message = "案件名称不能为空")
    private String name;

    @NotBlank(message = "案件类型不能为空")
    private String matterType;

    /**
     * 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政等
     */
    private String caseType;

    /**
     * 代理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行等
     */
    private String litigationStage;

    /**
     * 案由代码
     */
    private String causeOfAction;

    private String businessType;

    /**
     * 主要客户ID（向后兼容）
     */
    @NotNull(message = "客户不能为空")
    private Long clientId;

    /**
     * 客户列表（支持多客户）
     */
    private List<ClientCommand> clients;

    private String opposingParty;
    
    @Data
    public static class ClientCommand {
        private Long clientId;
        private String clientRole;  // PLAINTIFF-原告, DEFENDANT-被告, THIRD_PARTY-第三人, APPLICANT-申请人, RESPONDENT-被申请人
        private Boolean isPrimary;
    }
    
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
    private LocalDate filingDate;
    private LocalDate expectedClosingDate;
    private BigDecimal claimAmount;
    private Long contractId;
    private String remark;

    /**
     * 团队成员
     */
    private List<ParticipantCommand> participants;

    @Data
    public static class ParticipantCommand {
        private Long userId;
        private String role;
        private BigDecimal commissionRate;
        private Boolean isOriginator;
    }
}

