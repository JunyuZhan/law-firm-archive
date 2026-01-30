package com.lawfirm.application.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 发展规划里程碑 DTO. */
@Data
public class DevelopmentMilestoneDTO {
  /** 主键ID. */
  private Long id;

  /** 规划ID. */
  private Long planId;

  /** 里程碑名称. */
  private String milestoneName;

  /** 描述. */
  private String description;

  /** 目标日期. */
  private LocalDate targetDate;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 完成日期. */
  private LocalDate completedDate;

  /** 完成说明. */
  private String completionNote;

  /** 排序号. */
  private Integer sortOrder;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
