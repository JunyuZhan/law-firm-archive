package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 合同参与人 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractParticipantDTO extends BaseDTO {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 角色
     */
    private String role;
    
    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 提成比例
     */
    private BigDecimal commissionRate;

    /**
     * 备注
     */
    private String remark;
}
