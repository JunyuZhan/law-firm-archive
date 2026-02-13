package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案件查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MatterQueryDTO extends PageQuery {

  /** 案件名称（模糊）. */
  private String name;

  /** 案件编号（模糊）. */
  private String matterNo;

  /** 客户ID. */
  private Long clientId;

  /** 主办律师ID. */
  private Long leadLawyerId;

  /** 部门ID. */
  private Long departmentId;

  /** 案件类型. */
  private String matterType;

  /** 状态. */
  private String status;

  /** 案件ID列表（用于权限过滤）. */
  private java.util.List<Long> matterIds;

  /** 我参与的案件（当前用户）. */
  private Boolean myMatters;

  /** 创建时间开始. */
  private LocalDateTime createdAtFrom;

  /** 创建时间结束. */
  private LocalDateTime createdAtTo;
}
