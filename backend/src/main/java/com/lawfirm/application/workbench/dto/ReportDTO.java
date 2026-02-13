package com.lawfirm.application.workbench.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/** 报表 DTO. */
@Data
public class ReportDTO {
  /** 主键ID. */
  private Long id;

  /** 报表编号. */
  private String reportNo;

  /** 报表名称. */
  private String reportName;

  /** 报表类型：REVENUE-收入报表, MATTER-案件报表, CLIENT-客户报表等. */
  private String reportType;

  /** 报表类型名称. */
  private String reportTypeName;

  /** 格式：EXCEL, PDF. */
  private String format;

  /** 状态：GENERATING-生成中, COMPLETED-已完成, FAILED-失败. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 报表文件下载地址. */
  private String fileUrl;

  /** 文件大小（字节）. */
  private Long fileSize;

  /** 报表参数（JSON）. */
  private Map<String, Object> parameters;

  /** 生成时间. */
  private LocalDateTime generatedAt;

  /** 生成人ID. */
  private Long generatedBy;

  /** 生成人姓名. */
  private String generatedByName;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
