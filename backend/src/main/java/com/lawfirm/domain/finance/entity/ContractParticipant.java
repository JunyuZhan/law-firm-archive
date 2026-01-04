package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 合同参与人实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("contract_participant")
public class ContractParticipant extends BaseEntity {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：LEAD-承办律师, CO_COUNSEL-协办律师, ORIGINATOR-案源人, PARALEGAL-律师助理
     */
    private String role;

    /**
     * 提成比例（百分比）
     */
    private BigDecimal commissionRate;

    /**
     * 备注
     */
    private String remark;
}
