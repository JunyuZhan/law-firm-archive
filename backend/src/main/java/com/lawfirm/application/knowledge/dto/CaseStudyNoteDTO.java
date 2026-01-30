package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案例学习笔记DTO（M10-013）. */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseStudyNoteDTO extends BaseDTO {

  /** 案例ID */
  private Long caseId;

  /** 案例标题 */
  private String caseTitle;

  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 笔记内容 */
  private String noteContent;

  /** 关键要点 */
  private String keyPoints;

  /** 个人见解 */
  private String personalInsights;
}
