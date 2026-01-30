package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 更新合同参与人命令. */
@Data
public class UpdateParticipantCommand {

  /** 参与人ID. */
  @NotNull(message = "参与人ID不能为空")
  private Long id;

  /** 角色. */
  private String role;

  /** 提成比例. */
  private BigDecimal commissionRate;

  /** 备注. */
  private String remark;
}
