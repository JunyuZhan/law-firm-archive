package com.lawfirm.application.archive.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建借阅申请命令
 */
@Data
public class CreateBorrowCommand {

    @NotNull(message = "档案ID不能为空")
    private Long archiveId;

    @NotBlank(message = "借阅原因不能为空")
    private String borrowReason;

    @NotNull(message = "借阅日期不能为空")
    private LocalDate borrowDate;

    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectedReturnDate;
}

