package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 创建借阅申请命令. */
@Data
public class CreateBorrowCommand {

  /** 档案ID */
  @NotNull(message = "档案ID不能为空")
  private Long archiveId;

  /** 借阅原因 */
  @NotBlank(message = "借阅原因不能为空")
  private String borrowReason;

  /** 借阅日期 */
  @NotNull(message = "借阅日期不能为空")
  private LocalDate borrowDate;

  /** 预计归还日期 */
  @NotNull(message = "预计归还日期不能为空")
  private LocalDate expectedReturnDate;
}
