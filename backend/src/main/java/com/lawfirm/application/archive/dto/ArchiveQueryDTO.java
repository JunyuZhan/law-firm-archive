package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 档案查询条件. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveQueryDTO extends PageQuery {

  /** 档案号 */
  private String archiveNo;

  /** 档案名称 */
  private String archiveName;

  /** 案件编号 */
  private String matterNo;

  /** 案件名称 */
  private String matterName;

  /** 客户名称 */
  private String clientName;

  /** 档案类型 */
  private String archiveType;

  /** 状态 */
  private String status;

  /** 库位ID */
  private Long locationId;

  /** 结案日期-开始 */
  private LocalDate caseCloseDateFrom;

  /** 结案日期-结束 */
  private LocalDate caseCloseDateTo;
}
