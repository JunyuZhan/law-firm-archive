package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 假期余额DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveBalanceDTO extends BaseDTO {
  /** 余额ID */
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 请假类型ID */
  private Long leaveTypeId;

  /** 请假类型名称 */
  private String leaveTypeName;

  /** 年度 */
  private Integer year;

  /** 总天数 */
  private BigDecimal totalDays;

  /** 已用天数 */
  private BigDecimal usedDays;

  /** 剩余天数 */
  private BigDecimal remainingDays;
}
