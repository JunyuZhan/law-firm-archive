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
}
