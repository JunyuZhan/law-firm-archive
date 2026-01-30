package com.lawfirm.application.hr.dto;

import lombok.Data;

/** 职级查询 DTO. */
@Data
public class CareerLevelQueryDTO {
  /** 页码. */
  private Integer pageNum = 1;

  /** 每页大小. */
  private Integer pageSize = 10;

  /** 关键词. */
  private String keyword;

  /** 分类. */
  private String category;

  /** 状态. */
  private String status;
}
