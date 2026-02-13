package com.lawfirm.application.knowledge.command;

import lombok.Data;

/** 创建案例学习笔记命令（M10-013） */
@Data
public class CreateCaseStudyNoteCommand {
  /** 案例ID */
  private Long caseId;

  /** 笔记内容 */
  private String noteContent;

  /** 关键点 */
  private String keyPoints;

  /** 个人见解 */
  private String personalInsights;
}
