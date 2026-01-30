package com.lawfirm.application.finance.command;

import java.math.BigDecimal;
import lombok.Data;

/** 更新提成规则命令 */
@Data
public class UpdateCommissionRuleCommand {

  /** 规则名称 */
  private String ruleName;

  /** 规则类型 */
  private String ruleType;

  /** 律所比例(%)，0表示不参与分配 */
  private BigDecimal firmRate;

  /** 主办律师比例(%)，0表示不参与分配 */
  private BigDecimal leadLawyerRate;

  /** 协办律师比例(%)，0表示无协办或不参与分配 */
  private BigDecimal assistLawyerRate;

  /** 辅助人员比例(%)，0表示无辅助或不参与分配 */
  private BigDecimal supportStaffRate;

  /** 案源人比例(%)，0表示不参与分配 */
  private BigDecimal originatorRate;

  /** 律师创建合同时是否允许修改比例 */
  private Boolean allowModify;

  /** 描述 */
  private String description;

  /** 是否默认 */
  private Boolean isDefault;

  /** 是否激活 */
  private Boolean active;
}
