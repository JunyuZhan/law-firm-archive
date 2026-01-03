package com.lawfirm.application.hr.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建考核任务命令
 */
@Data
public class CreatePerformanceTaskCommand {

    private String name;
    private String periodType;
    private Integer year;
    private Integer period;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate selfEvalDeadline;
    private LocalDate peerEvalDeadline;
    private LocalDate supervisorEvalDeadline;
    private String description;
    private String remarks;
}
