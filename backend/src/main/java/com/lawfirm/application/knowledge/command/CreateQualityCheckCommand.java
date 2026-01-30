package com.lawfirm.application.knowledge.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 创建质量检查命令（M10-031） */
@Data
public class CreateQualityCheckCommand {
  /** 项目ID */
  private Long matterId;

  /** 检查日期 */
  private LocalDate checkDate;

  /** 检查类型 */
  private String checkType;

  /** 检查摘要 */
  private String checkSummary;

  /** 检查详情列表 */
  private List<CheckDetailCommand> details;

  /** 检查详情命令 */
  @Data
  public static class CheckDetailCommand {
    /** 标准ID */
    private Long standardId;

    /** 检查结果 */
    private String checkResult;

    /** 得分 */
    private BigDecimal score;

    /** 最高分 */
    private BigDecimal maxScore;

    /** 检查发现 */
    private String findings;

    /** 建议 */
    private String suggestions;
  }
}
