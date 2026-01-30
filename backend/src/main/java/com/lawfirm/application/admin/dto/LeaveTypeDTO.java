package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 请假类型DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveTypeDTO extends BaseDTO {
  /** 类型ID */
  private Long id;

  /** 类型名称 */
  private String name;

  /** 类型编码 */
  private String code;

  /** 是否带薪 */
  private Boolean paid;

  /** 年度限额（天） */
  private BigDecimal annualLimit;

  /** 是否需要审批 */
  private Boolean needApproval;

  /** 描述 */
  private String description;

  /** 排序 */
  private Integer sortOrder;

  /** 是否启用 */
  private Boolean enabled;
}
