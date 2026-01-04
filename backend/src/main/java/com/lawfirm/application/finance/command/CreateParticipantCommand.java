package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建合同参与人命令
 */
@Data
public class CreateParticipantCommand {

    @NotNull(message = "合同ID不能为空")
    private Long contractId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 提成比例
     */
    private BigDecimal commissionRate;

    /**
     * 备注
     */
    private String remark;
}
