package com.lawfirm.application.knowledge.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建案例命令
 */
@Data
public class CreateCaseLibraryCommand {
    private String title;
    private Long categoryId;
    private String caseNumber;
    private String courtName;
    private LocalDate judgeDate;
    private String caseType;
    private String causeOfAction;
    private String trialProcedure;
    private String plaintiff;
    private String defendant;
    private String caseSummary;
    private String courtOpinion;
    private String judgmentResult;
    private String caseSignificance;
    private String keywords;
    private String source;
    private Long matterId;
    private String attachmentUrl;
}
