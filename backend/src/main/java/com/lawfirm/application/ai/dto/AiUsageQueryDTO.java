package com.lawfirm.application.ai.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/** AI使用记录查询DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiUsageQueryDTO extends PageQuery {

  /** 用户ID（管理员查询所有，普通用户只能查自己的） */
  private Long userId;

  /** 部门ID */
  private Long departmentId;

  /** 集成编码（如AI_DEEPSEEK） */
  private String integrationCode;

  /** 模型名称 */
  private String modelName;

  /** 请求类型 */
  private String requestType;

  /** 业务类型 */
  private String businessType;

  /** 是否成功 */
  private Boolean success;

  /** 开始日期 */
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate startDate;

  /** 结束日期 */
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate endDate;

  /** 创建时间起始（用于前端查询） */
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAtFrom;

  /** 创建时间结束（用于前端查询） */
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAtTo;

  /** 关键字搜索（用户名、模型名等） */
  private String keyword;
}
