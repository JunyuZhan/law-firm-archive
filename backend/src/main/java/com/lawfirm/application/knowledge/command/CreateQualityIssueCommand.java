package com.lawfirm.application.knowledge.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建问题整改命令（M10-032）
 */
@Data
public class CreateQualityIssueCommand {
    private Long checkId;
    private Long matterId;
    private String issueType;
    private String issueDescription;
    private Long responsibleUserId;
    private String priority;
    private LocalDate dueDate;
}

