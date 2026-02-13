package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 创建利冲检查命令 */
@Data
public class CreateConflictCheckCommand {

  /** 案件ID */
  @NotNull(message = "案件ID不能为空")
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 备注 */
  private String remark;

  /** 检查当事人列表 */
  @NotEmpty(message = "检查当事人不能为空")
  private List<PartyCommand> parties;

  /** 当事人命令 */
  @Data
  public static class PartyCommand {
    /** 当事人名称 */
    private String partyName;

    /** 当事人类型：CLIENT-委托人, OPPOSING-对方当事人, RELATED-关联方 */
    private String partyType;

    /** 证件号码 */
    private String idNumber;
  }
}
