package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 国家赔偿案件业务信息实体
 * 用于存储行政国家赔偿和刑事国家赔偿的扩展业务信息
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter_state_compensation")
public class MatterStateCompensation extends BaseEntity {

    /**
     * 关联案件ID
     */
    @TableField("matter_id")
    private Long matterId;

    // ========== 赔偿义务机关 ==========

    /**
     * 赔偿义务机关名称
     */
    @TableField("obligor_org_name")
    private String obligorOrgName;

    /**
     * 义务机关类型
     * PUBLIC_SECURITY-公安机关, PROCURATORATE-检察机关, COURT-审判机关,
     * PRISON-监狱管理机关, ADMIN_ORGAN-行政机关, OTHER-其他
     */
    @TableField("obligor_org_type")
    private String obligorOrgType;

    // ========== 致损行为 ==========

    /**
     * 致损行为类型
     * ILLEGAL_DETENTION-违法拘留, ILLEGAL_COERCIVE-违法采取强制措施,
     * WRONGFUL_CONVICT-错误判决等
     */
    @TableField("case_source")
    private String caseSource;

    /**
     * 损害情况描述
     */
    @TableField("damage_description")
    private String damageDescription;

    // ========== 刑事赔偿特有字段 ==========

    /**
     * 刑事诉讼是否终结（刑事赔偿必填）
     */
    @TableField("criminal_case_terminated")
    private Boolean criminalCaseTerminated;

    /**
     * 原刑事案件编号
     */
    @TableField("criminal_case_no")
    private String criminalCaseNo;

    /**
     * 受理的赔偿委员会
     */
    @TableField("compensation_committee")
    private String compensationCommittee;

    // ========== 程序日期 ==========

    /**
     * 赔偿申请日（2年时效）
     */
    @TableField("application_date")
    private LocalDate applicationDate;

    /**
     * 受理日
     */
    @TableField("acceptance_date")
    private LocalDate acceptanceDate;

    /**
     * 赔偿义务机关决定日（2个月期限）
     */
    @TableField("decision_date")
    private LocalDate decisionDate;

    /**
     * 复议/复核申请日（30日期限）
     */
    @TableField("reconsideration_date")
    private LocalDate reconsiderationDate;

    /**
     * 复议决定日
     */
    @TableField("reconsideration_decision_date")
    private LocalDate reconsiderationDecisionDate;

    /**
     * 赔偿委员会申请日
     */
    @TableField("committee_app_date")
    private LocalDate committeeAppDate;

    /**
     * 赔偿委员会决定日
     */
    @TableField("committee_decision_date")
    private LocalDate committeeDecisionDate;

    /**
     * 行政赔偿诉讼立案日
     */
    @TableField("admin_litigation_filing_date")
    private LocalDate adminLitigationFilingDate;

    /**
     * 行政诉讼法院
     */
    @TableField("admin_litigation_court_name")
    private String adminLitigationCourtName;

    // ========== 赔偿请求 ==========

    /**
     * 请求赔偿总额
     */
    @TableField("claim_amount")
    private BigDecimal claimAmount;

    /**
     * 赔偿项目明细（JSONB格式）
     * 示例：[{"type": "人身自由赔偿", "days": 100, "daily_amount": 436.89, "amount": 43689},
     *       {"type": "精神损害抚慰金", "amount": 50000}]
     */
    @TableField("compensation_items")
    private String compensationItems;

    // ========== 决定结果 ==========

    /**
     * 决定结果
     * GRANTED-全部支持, DENIED-不予赔偿, PARTIAL_GRANTED-部分支持
     */
    @TableField("decision_result")
    private String decisionResult;

    /**
     * 决定赔偿金额
     */
    @TableField("approved_amount")
    private BigDecimal approvedAmount;

    /**
     * 支付状态
     * UNPAID-未支付, PAID-已支付, PARTIAL_PAID-部分支付
     */
    @TableField("payment_status")
    private String paymentStatus;

    /**
     * 支付日期
     */
    @TableField("payment_date")
    private LocalDate paymentDate;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    // ========== 枚举常量 ==========

    /**
     * 义务机关类型枚举
     */
    public static final class ObligorOrgType {
        public static final String PUBLIC_SECURITY = "PUBLIC_SECURITY";
        public static final String PROCURATORATE = "PROCURATORATE";
        public static final String COURT = "COURT";
        public static final String PRISON = "PRISON";
        public static final String ADMIN_ORGAN = "ADMIN_ORGAN";
        public static final String OTHER = "OTHER";
    }

    /**
     * 致损行为类型枚举
     */
    public static final class DamageCauseType {
        public static final String ILLEGAL_DETENTION = "ILLEGAL_DETENTION";
        public static final String ILLEGAL_COERCIVE = "ILLEGAL_COERCIVE";
        public static final String ILLEGAL_WEAPON = "ILLEGAL_WEAPON";
        public static final String ILLEGAL_SEARCH = "ILLEGAL_SEARCH";
        public static final String WRONGFUL_CONVICT = "WRONGFUL_CONVICT";
        public static final String ILLEGAL_DETENTION_PROPERTY = "ILLEGAL_DETENTION_PROPERTY";
        public static final String ILLEGAL_ADMIN_PUNISHMENT = "ILLEGAL_ADMIN_PUNISHMENT";
        public static final String OTHER = "OTHER";
    }

    /**
     * 决定结果枚举
     */
    public static final class DecisionResult {
        public static final String GRANTED = "GRANTED";
        public static final String DENIED = "DENIED";
        public static final String PARTIAL_GRANTED = "PARTIAL_GRANTED";
    }

    /**
     * 支付状态枚举
     */
    public static final class PaymentStatus {
        public static final String UNPAID = "UNPAID";
        public static final String PAID = "PAID";
        public static final String PARTIAL_PAID = "PARTIAL_PAID";
    }
}
