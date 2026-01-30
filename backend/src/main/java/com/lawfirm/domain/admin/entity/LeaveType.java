package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 请假类型实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("leave_type")
public class LeaveType extends BaseEntity {

  /** 类型名称 */
  private String name;

  /** 类型编码 */
  private String code;

  /** 是否带薪 */
  private Boolean paid;

  /** 年度限额(天) */
  private BigDecimal annualLimit;

  /** 是否需要审批 */
  private Boolean needApproval;

  /** 描述 */
  private String description;

  /** 排序 */
  private Integer sortOrder;

  /** 是否启用 */
  private Boolean enabled;

  // 常用类型编码
  /** 编码：年假 */
  public static final String CODE_ANNUAL = "ANNUAL";

  /** 编码：事假 */
  public static final String CODE_PERSONAL = "PERSONAL";

  /** 编码：病假 */
  public static final String CODE_SICK = "SICK";

  /** 编码：调休 */
  public static final String CODE_COMPENSATORY = "COMPENSATORY";
}
