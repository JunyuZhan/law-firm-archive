package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 外出登记DTO（M8-005） */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoOutRecordDTO extends BaseDTO {
  /** 记录ID */
  private Long id;

  /** 登记编号 */
  private String recordNo;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 外出时间 */
  private LocalDateTime outTime;

  /** 预计返回时间 */
  private LocalDateTime expectedReturnTime;

  /** 实际返回时间 */
  private LocalDateTime actualReturnTime;

  /** 外出地点 */
  private String location;

  /** 外出事由 */
  private String reason;

  /** 同行人员 */
  private String companions;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;
}
