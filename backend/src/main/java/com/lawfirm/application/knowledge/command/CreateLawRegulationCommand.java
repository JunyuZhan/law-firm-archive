package com.lawfirm.application.knowledge.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建法规命令
 */
@Data
public class CreateLawRegulationCommand {
    private String title;
    private Long categoryId;
    private String docNumber;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String content;
    private String summary;
    private String keywords;
    private String source;
    private String attachmentUrl;
}
