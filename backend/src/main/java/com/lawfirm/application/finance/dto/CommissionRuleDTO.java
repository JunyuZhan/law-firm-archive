package com.lawfirm.application.finance.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 提成规则 DTO */
@Data
public class CommissionRuleDTO {

  /** 规则ID */
  private Long id;

  /** 规则编码 */
  private String ruleCode;

  /** 规则名称 */
  private String ruleName;

  /** 规则类型 */
  private String ruleType;

  /** 律所比例（%） */
  private BigDecimal firmRate;

  /** 主办律师比例（%） */
  private BigDecimal leadLawyerRate;

  /** 协办律师比例（%） */
  private BigDecimal assistLawyerRate;

  /** 辅助人员比例（%） */
  private BigDecimal supportStaffRate;

  /** 案源人比例（%） */
  private BigDecimal originatorRate;

  /** 是否允许修改 */
  private Boolean allowModify;

  /** 描述 */
  private String description;

  /** 是否默认 */
  private Boolean isDefault;

  /** 是否激活 */
  private Boolean active;
}
