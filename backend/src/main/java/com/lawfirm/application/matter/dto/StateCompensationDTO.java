package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 国家赔偿信息 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StateCompensationDTO extends BaseDTO {

    private Long matterId;
    private String matterNo;
    private String matterName;

    // ========== 赔偿义务机关 ==========

    private String obligorOrgName;
    private String obligorOrgType;
    private String obligorOrgTypeName;

    // ========== 致损行为 ==========

    private String caseSource;
    private String caseSourceName;
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
    private String decisionResultName;
    private BigDecimal approvedAmount;
    private String paymentStatus;
    private String paymentStatusName;
    private LocalDate paymentDate;

    private String remark;
}
