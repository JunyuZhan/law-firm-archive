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
     * 角色常量
     */
    public static final String ROLE_LEAD = "LEAD";
    public static final String ROLE_CO_COUNSEL = "CO_COUNSEL";
    public static final String ROLE_ORIGINATOR = "ORIGINATOR";
    public static final String ROLE_PARALEGAL = "PARALEGAL";

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

    /**
     * 判断是否为案源人
     */
    public boolean isOriginator() {
        return ROLE_ORIGINATOR.equals(this.role);
    }

    /**
     * 判断是否为承办律师
     */
    public boolean isLeadLawyer() {
        return ROLE_LEAD.equals(this.role);
    }

    /**
     * 判断是否为协办律师
     */
    public boolean isCoCounsel() {
        return ROLE_CO_COUNSEL.equals(this.role);
    }

    /**
     * 判断是否为办案人员（承办或协办）
     */
    public boolean isCaseHandler() {
        return ROLE_LEAD.equals(this.role) || ROLE_CO_COUNSEL.equals(this.role);
    }
}
