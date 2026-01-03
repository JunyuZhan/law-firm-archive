package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户股东信息实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_client_shareholder")
public class ClientShareholder extends BaseEntity {

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 股东名称
     */
    private String shareholderName;

    /**
     * 股东类型：INDIVIDUAL-个人, ENTERPRISE-企业
     */
    private String shareholderType;

    /**
     * 个人股东身份证号
     */
    private String idCard;

    /**
     * 企业股东统一社会信用代码
     */
    private String creditCode;

    /**
     * 持股比例（百分比）
     */
    private BigDecimal shareholdingRatio;

    /**
     * 投资金额
     */
    private BigDecimal investmentAmount;

    /**
     * 投资日期
     */
    private LocalDate investmentDate;

    /**
     * 职务
     */
    private String position;

    /**
     * 备注
     */
    private String remark;
}

