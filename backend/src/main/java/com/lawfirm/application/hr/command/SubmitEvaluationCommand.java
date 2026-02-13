package com.lawfirm.application.hr.command;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 提交评价命令. */
@Data
public class SubmitEvaluationCommand {

  /** 任务ID. */
  private Long taskId;

  /** 员工ID. */
  private Long employeeId;

  /** 评价类型. */
  private String evaluationType;

  /** 评价意见. */
  private String comment;

  /** 优点. */
  private String strengths;

  /** 改进建议. */
  private String improvements;

  /** 评分项列表. */
  private List<ScoreItem> scores;

  /** 评分项. */
  @Data
  public static class ScoreItem {
    /** 指标ID. */
    private Long indicatorId;

    /** 得分. */
    private BigDecimal score;

    /** 评价. */
    private String comment;
  }
}
