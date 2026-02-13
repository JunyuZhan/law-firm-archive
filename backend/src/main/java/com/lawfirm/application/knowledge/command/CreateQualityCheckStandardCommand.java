package com.lawfirm.application.knowledge.command;

import java.math.BigDecimal;
import lombok.Data;

/** 创建质量检查标准命令（M10-030） */
@Data
public class CreateQualityCheckStandardCommand {
  /** 标准名称 */
  private String standardName;

  /** 类别 */
  private String category;

  /** 描述 */
  private String description;

  /** 检查项（JSON格式） */
  private String checkItems;

  /** 适用项目类型 */
  private String applicableMatterTypes;

  /** 权重 */
  private BigDecimal weight;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序号 */
  private Integer sortOrder;
}
