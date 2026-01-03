package com.lawfirm.application.knowledge.command;

import lombok.Data;

/**
 * 创建案例学习笔记命令（M10-013）
 */
@Data
public class CreateCaseStudyNoteCommand {
    private Long caseId;
    private String noteContent;
    private String keyPoints;
    private String personalInsights;
}

