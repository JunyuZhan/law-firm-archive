package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建国家赔偿信息命令
 */
@Data
public class CreateStateCompensationCommand {

    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    // ========== 赔偿义务机关 ==========

    /**
     * 赔偿义务机关名称
     */
    private String obligorOrgName;

    /**
     * 义务机关类型
     */
    private String obligorOrgType;

    // ========== 致损行为 ==========

    /**
     * 致损行为类型
     */
    private String caseSource;

    /**
     * 损害情况描述
     */
    private String damageDescription;

    // ========== 刑事赔偿特有字段 ==========

    /**
     * 刑事诉讼是否终结（刑事赔偿必填）
     */
    private Boolean criminalCaseTerminated;

    /**
     * 原刑事案件编号
     */
    private String criminalCaseNo;

    /**
     * 受理的赔偿委员会
     */
    private String compensationCommittee;

    // ========== 程序日期 ==========

    /**
     * 赔偿申请日（2年时效）
     */
    private LocalDate applicationDate;

    /**
     * 受理日
     */
    private LocalDate acceptanceDate;

    /**
     * 赔偿义务机关决定日（2个月期限）
     */
    private LocalDate decisionDate;

    /**
     * 复议/复核申请日（30日期限）
     */
    private LocalDate reconsiderationDate;

    /**
     * 复议决定日
     */
    private LocalDate reconsiderationDecisionDate;

    /**
     * 赔偿委员会申请日
     */
    private LocalDate committeeAppDate;

    /**
     * 赔偿委员会决定日
     */
    private LocalDate committeeDecisionDate;

    /**
     * 行政赔偿诉讼立案日
     */
    private LocalDate adminLitigationFilingDate;

    /**
     * 行政诉讼法院
     */
    private String adminLitigationCourtName;

    // ========== 赔偿请求 ==========

    /**
     * 请求赔偿总额
     */
    private BigDecimal claimAmount;

    /**
     * 赔偿项目明细（JSON格式字符串）
     */
    private String compensationItems;

    // ========== 决定结果 ==========

    /**
     * 决定结果
     */
    private String decisionResult;

    /**
     * 决定赔偿金额
     */
    private BigDecimal approvedAmount;

    /**
     * 支付状态
     */
    private String paymentStatus;

    /**
     * 支付日期
     */
    private LocalDate paymentDate;

    /**
     * 备注
     */
    private String remark;
}
