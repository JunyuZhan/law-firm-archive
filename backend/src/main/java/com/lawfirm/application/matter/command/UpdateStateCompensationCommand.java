package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新国家赔偿信息命令
 */
@Data
public class UpdateStateCompensationCommand {

    @NotNull(message = "ID不能为空")
    private Long id;

    // ========== 赔偿义务机关 ==========

    private String obligorOrgName;
    private String obligorOrgType;

    // ========== 致损行为 ==========

    private String caseSource;
    private String damageDescription;

    // ========== 刑事赔偿特有字段 ==========

    private Boolean criminalCaseTerminated;
    private String criminalCaseNo;
    private String compensationCommittee;

    // ========== 程序日期 ==========

    private LocalDate applicationDate;
    private LocalDate acceptanceDate;
    private LocalDate decisionDate;
    private LocalDate reconsiderationDate;
    private LocalDate reconsiderationDecisionDate;
    private LocalDate committeeAppDate;
    private LocalDate committeeDecisionDate;
    private LocalDate adminLitigationFilingDate;
    private String adminLitigationCourtName;

    // ========== 赔偿请求 ==========

    private BigDecimal claimAmount;
    private String compensationItems;

    // ========== 决定结果 ==========

    private String decisionResult;
    private BigDecimal approvedAmount;
    private String paymentStatus;
    private LocalDate paymentDate;

    private String remark;
}
