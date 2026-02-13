package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 成本归集命令 */
@Data
public class AllocateCostCommand {

  /** 项目ID */
  @NotNull(message = "项目ID不能为空")
  private Long matterId;

  /** 费用ID列表（要归集的费用ID列表） */
  @NotNull(message = "费用ID列表不能为空")
  private List<Long> expenseIds;
}
