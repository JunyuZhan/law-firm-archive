package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 费用报销实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_expense")
public class Expense extends BaseEntity {

    /**
     * 报销单号
     */
    private String expenseNo;

    /**
     * 关联项目ID（可选，公共费用可为空）
     */
    private Long matterId;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 费用类型：TRAVEL-差旅费, MEAL-餐费, ACCOMMODATION-住宿费, TRANSPORT-交通费, MATERIAL-材料费, OTHER-其他
     */
    private String expenseType;

    /**
     * 费用分类：CASE_COST-办案成本, OFFICE_COST-办公费用, OTHER-其他
     */
    private String expenseCategory;

    /**
     * 费用发生日期
     */
    private LocalDate expenseDate;

    /**
     * 费用金额
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 费用说明
     */
    private String description;

    /**
     * 供应商/商户名称
     */
    private String vendorName;

    /**
     * 发票号
     */
    private String invoiceNo;

    /**
     * 发票附件URL（向后兼容字段）
     */
    private String invoiceUrl;

    /**
     * MinIO桶名称，默认law-firm
     */
    private String bucketName;

    /**
     * 存储路径：expense/M_{matterId}/{YYYY-MM}/费用凭证/
     */
    private String storagePath;

    /**
     * 物理文件名：20260127_uuid_发票.jpg（支持超长文件名，最大1000字符）
     */
    private String physicalName;

    /**
     * 文件Hash值（SHA-256），用于去重和校验
     */
    private String fileHash;

    /**
     * 状态：PENDING-待审批, APPROVED-已审批, REJECTED-已驳回, PAID-已支付
     */
    private String status;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 支付人ID
     */
    private Long paidBy;

    /**
     * 支付方式：CASH-现金, BANK_TRANSFER-银行转账, ALIPAY-支付宝, WECHAT-微信
     */
    private String paymentMethod;

    /**
     * 是否已归集到项目成本
     */
    private Boolean isCostAllocation;

    /**
     * 归集到的项目ID
     */
    private Long allocatedToMatterId;

    /**
     * 备注
     */
    private String remark;
}

