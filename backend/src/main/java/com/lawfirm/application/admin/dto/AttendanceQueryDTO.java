package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 考勤查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceQueryDTO extends PageQuery {
  /** 用户ID */
  private Long userId;

  /** 开始日期 */
  private LocalDate startDate;

  /** 结束日期 */
  private LocalDate endDate;

  /** 状态 */
  private String status;
}
