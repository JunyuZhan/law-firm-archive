package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_invoice")
public class Invoice extends BaseEntity {

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 关联收费ID
     */
    private Long feeId;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 发票类型：SPECIAL-增值税专用发票, NORMAL-增值税普通发票, ELECTRONIC-电子发票
     */
    private String invoiceType;

    /**
     * 发票抬头
     */
    private String title;

    /**
     * 税号
     */
    private String taxNo;

    /**
     * 开票金额
     */
    private BigDecimal amount;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 开票内容
     */
    private String content;

    /**
     * 开票日期
     */
    private LocalDate invoiceDate;

    /**
     * 状态：PENDING-待开票, ISSUED-已开票, CANCELLED-已作废, RED-已红冲
     */
    private String status;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 开票人ID
     */
    private Long issuerId;

    /**
     * 发票文件URL（向后兼容字段）
     */
    private String fileUrl;

    /**
     * MinIO桶名称，默认law-firm
     */
    private String bucketName;

    /**
     * 存储路径：invoice/M_{matterId}/{YYYY-MM}/发票文件/
     */
    private String storagePath;

    /**
     * 物理文件名：20260127_uuid_发票.pdf（支持超长文件名，最大1000字符）
     */
    private String physicalName;

    /**
     * 文件Hash值（SHA-256），用于去重和校验
     */
    private String fileHash;

    /**
     * 备注
     */
    private String remark;
}

