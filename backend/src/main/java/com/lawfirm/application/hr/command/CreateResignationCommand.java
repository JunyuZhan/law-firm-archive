package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 创建离职申请命令 */
@Data
public class CreateResignationCommand {

  /** 员工ID */
  @NotNull(message = "员工ID不能为空")
  private Long employeeId;

  /** 离职类型 */
  @NotNull(message = "离职类型不能为空")
  private String resignationType;

  /** 离职日期 */
  @NotNull(message = "离职日期不能为空")
  private LocalDate resignationDate;

  /** 最后工作日 */
  @NotNull(message = "最后工作日不能为空")
  private LocalDate lastWorkDate;

  /** 离职原因 */
  private String reason;

  /** 交接人ID */
  private Long handoverPersonId;

  /** 交接说明 */
  private String handoverNote;
}
