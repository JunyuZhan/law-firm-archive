package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 案件/项目实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("matter")
public class Matter extends BaseEntity {

    /**
     * 案件编号
     */
    private String matterNo;

    /**
     * 案件名称
     */
    private String name;

    /**
     * 案件类型：LITIGATION-诉讼, NON_LITIGATION-非诉
     */
    private String matterType;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 对方当事人
     */
    private String opposingParty;

    /**
     * 对方律师姓名
     */
    private String opposingLawyerName;

    /**
     * 对方律师执业证号
     */
    private String opposingLawyerLicenseNo;

    /**
     * 对方律师所在律所
     */
    private String opposingLawyerFirm;

    /**
     * 对方律师联系电话
     */
    private String opposingLawyerPhone;

    /**
     * 对方律师邮箱
     */
    private String opposingLawyerEmail;

    /**
     * 案件描述
     */
    private String description;

    /**
     * 状态：DRAFT-草稿, PENDING-待审批, ACTIVE-进行中, SUSPENDED-暂停, CLOSED-结案, ARCHIVED-归档
     */
    private String status;

    /**
     * 案源人ID
     */
    private Long originatorId;

    /**
     * 承办律师ID（主办）
     */
    private Long leadLawyerId;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 收费方式：FIXED-固定收费, HOURLY-计时收费, CONTINGENCY-风险代理, MIXED-混合收费
     */
    private String feeType;

    /**
     * 预估收费金额
     */
    private BigDecimal estimatedFee;

    /**
     * 实际收费金额
     */
    private BigDecimal actualFee;

    /**
     * 立案日期
     */
    private LocalDate filingDate;

    /**
     * 预计结案日期
     */
    private LocalDate expectedClosingDate;

    /**
     * 实际结案日期
     */
    private LocalDate actualClosingDate;

    /**
     * 标的金额
     */
    private BigDecimal claimAmount;

    /**
     * 判决/调解结果
     */
    private String outcome;

    /**
     * 关联合同ID
     */
    private Long contractId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 利冲检查状态：PENDING-待检查, PASSED-已通过, FAILED-未通过
     */
    private String conflictStatus;
}

