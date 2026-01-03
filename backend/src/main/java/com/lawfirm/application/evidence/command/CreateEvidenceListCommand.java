package com.lawfirm.application.evidence.command;

import lombok.Data;

import java.util.List;

/**
 * 创建证据清单命令
 */
@Data
public class CreateEvidenceListCommand {
    private Long matterId;
    private String name;
    private String listType;
    private List<Long> evidenceIds;
}
