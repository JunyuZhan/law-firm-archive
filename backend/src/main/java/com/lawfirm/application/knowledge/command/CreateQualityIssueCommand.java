package com.lawfirm.application.knowledge.command;

import java.time.LocalDate;
import lombok.Data;

/** 创建问题整改命令（M10-032） */
@Data
public class CreateQualityIssueCommand {
  /** 检查ID */
  private Long checkId;

  /** 项目ID */
  private Long matterId;

  /** 问题类型 */
  private String issueType;

  /** 问题描述 */
  private String issueDescription;

  /** 负责人ID */
  private Long responsibleUserId;

  /** 优先级 */
  private String priority;

  /** 截止日期 */
  private LocalDate dueDate;
}
