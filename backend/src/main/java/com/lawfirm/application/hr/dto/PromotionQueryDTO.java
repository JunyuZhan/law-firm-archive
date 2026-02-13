package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 晋升申请查询参数. */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromotionQueryDTO extends PageQuery {
  /** 关键词. */
  private String keyword;

  /** 状态. */
  private String status;

  /** 员工ID. */
  private Long employeeId;

  /** 部门ID. */
  private Long departmentId;
}
