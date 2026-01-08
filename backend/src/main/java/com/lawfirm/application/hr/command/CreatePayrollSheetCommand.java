package com.lawfirm.application.hr.command;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建工资表命令
 */
@Data
public class CreatePayrollSheetCommand {

    @NotNull(message = "工资年份不能为空")
    private Integer payrollYear;

    @NotNull(message = "工资月份不能为空")
    private Integer payrollMonth;

    /**
     * 生成方式：AUTO-自动汇总, MANUAL-手动创建
     */
    private String generateType;

    /**
     * 自动确认截止时间（超过此时间未确认的工资明细将自动确认）
     * 如果不设置，默认提交后7天自动确认
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime autoConfirmDeadline;
}

