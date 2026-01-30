package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 添加员工工资明细命令. */
@Data
public class AddPayrollItemCommand {

  /** 员工ID */
  @NotNull(message = "员工ID不能为空")
  private Long employeeId;

  /** 是否自动载入基本工资和提成数据 true: 自动从劳动合同和提成表载入 false: 创建空的工资明细，由财务手动填写. */
  private Boolean autoLoad = true;
}
