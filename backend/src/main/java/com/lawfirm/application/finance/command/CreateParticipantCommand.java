package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 创建合同参与人命令 */
@Data
public class CreateParticipantCommand {

  /** 合同ID */
  @NotNull(message = "合同ID不能为空")
  private Long contractId;

  /** 用户ID */
  @NotNull(message = "用户ID不能为空")
  private Long userId;

  /** 角色 */
  @NotBlank(message = "角色不能为空")
  private String role;

  /** 提成比例 */
  private BigDecimal commissionRate;

  /** 备注 */
  private String remark;
}
