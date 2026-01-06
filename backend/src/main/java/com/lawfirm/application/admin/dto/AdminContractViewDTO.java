package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 行政合同视图DTO（只读）
 * 
 * Requirements: 5.2, 5.3
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminContractViewDTO extends BaseDTO {

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String name;

    /**
     * 委托人ID
     */
    private Long clientId;

    /**
     * 委托人名称
     */
    private String clientName;

    /**
     * 对方当事人
     */
    private String opposingParty;

    /**
     * 案件类型
     */
    private String caseType;

    /**
     * 案件类型名称
     */
    private String caseTypeName;

    /**
     * 案由
     */
    private String causeOfAction;

    /**
     * 承办律师ID
     */
    private Long leadLawyerId;

    /**
     * 承办律师姓名
     */
    private String leadLawyerName;

    /**
     * 律师费金额
     */
    private BigDecimal totalAmount;

    /**
     * 签约日期
     */
    private LocalDate signDate;

    /**
     * 管辖法院（诉讼类型）
     */
    private String jurisdictionCourt;

    /**
     * 审理阶段（诉讼类型）
     */
    private String trialStage;

    /**
     * 审理阶段名称
     */
    private String trialStageName;

    /**
     * 案由名称（用于显示）
     */
    private String causeOfActionName;

    /**
     * 合同状态
     */
    private String status;
}
