package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import org.apache.ibatis.type.Alias;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 委托合同实体
 */
@Alias("FinanceContract")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_contract")
public class Contract extends BaseEntity {

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String name;

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 合同类型：SERVICE-服务合同, RETAINER-常年法顾, LITIGATION-诉讼代理, NON_LITIGATION-非诉项目
     */
    private String contractType;

    /**
     * 收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费
     */
    private String feeType;

    /**
     * 合同金额
     */
    private BigDecimal totalAmount;

    /**
     * 已收金额
     */
    private BigDecimal paidAmount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 签约日期
     */
    private LocalDate signDate;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 到期日期
     */
    private LocalDate expiryDate;

    /**
     * 合同状态：DRAFT-草稿, PENDING-待审批, ACTIVE-生效中, REJECTED-已拒绝, TERMINATED-已终止, COMPLETED-已完成, EXPIRED-已过期
     */
    private String status;

    /**
     * 签约人ID（负责律师）
     */
    private Long signerId;

    /**
     * 所属部门ID
     */
    private Long departmentId;

    /**
     * 合同内容（基于模板生成）
     */
    private String content;

    /**
     * 合同文件URL
     */
    private String fileUrl;

    /**
     * 付款条款
     */
    private String paymentTerms;

    /**
     * 备注
     */
    private String remark;

    // ========== 扩展字段（合同模块完善）==========

    /**
     * 案件类型：CIVIL-民事, CRIMINAL-刑事, ADMINISTRATIVE-行政, BANKRUPTCY-破产, IP-知识产权, ARBITRATION-仲裁, ENFORCEMENT-执行, LEGAL_COUNSEL-法律顾问, SPECIAL_SERVICE-专项服务
     */
    private String caseType;

    /**
     * 案由代码（对应前端案由常量的code）
     */
    private String causeOfAction;

    /**
     * 审理阶段：FIRST_INSTANCE-一审, SECOND_INSTANCE-二审, RETRIAL-再审, EXECUTION-执行, NON_LITIGATION-非诉
     */
    private String trialStage;

    /**
     * 标的金额
     */
    private BigDecimal claimAmount;

    /**
     * 管辖法院
     */
    private String jurisdictionCourt;

    /**
     * 对方当事人
     */
    private String opposingParty;

    /**
     * 利冲审查状态：PENDING-待审查, PASSED-已通过, FAILED-未通过, NOT_REQUIRED-无需审查
     */
    private String conflictCheckStatus;

    /**
     * 归档状态：NOT_ARCHIVED-未归档, ARCHIVED-已归档, DESTROYED-已销毁
     */
    private String archiveStatus;

    /**
     * 预支差旅费
     */
    private BigDecimal advanceTravelFee;

    /**
     * 风险代理比例（0-100）
     */
    private BigDecimal riskRatio;

    /**
     * 印章使用记录（JSON格式）
     */
    private String sealRecord;

    // ========== 提成分配方案 ==========

    /**
     * 提成规则ID（选择的预设方案）
     */
    private Long commissionRuleId;

    /**
     * 律所比例（%），可以被修改
     */
    private BigDecimal firmRate;

    /**
     * 主办律师比例（%）
     */
    private BigDecimal leadLawyerRate;

    /**
     * 协办律师比例（%）
     */
    private BigDecimal assistLawyerRate;

    /**
     * 辅助人员比例（%）
     */
    private BigDecimal supportStaffRate;

    /**
     * 案源人比例（%）
     */
    private BigDecimal originatorRate;

    /**
     * 案情摘要（用于审批表）
     */
    private String caseSummary;
}
