package com.lawfirm.application.document.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** 文档审计统计DTO（M5-044） */
@Data
public class DocumentAuditStatisticsDTO {

  /** 按用户统计. */
  private List<Map<String, Object>> byUser;

  /** 按文档统计. */
  private List<Map<String, Object>> byDocument;

  /** 按操作类型统计. */
  private List<Map<String, Object>> byActionType;

  /** 按时间统计（趋势）. */
  private List<Map<String, Object>> byDate;

  /** 总访问次数. */
  private Long totalAccessCount;
}
