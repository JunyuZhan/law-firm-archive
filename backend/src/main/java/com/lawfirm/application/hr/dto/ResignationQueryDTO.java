package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 离职申请查询 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResignationQueryDTO extends PageQuery {

  /** 员工ID */
  private Long employeeId;

  /** 状态 */
  private String status;
}
