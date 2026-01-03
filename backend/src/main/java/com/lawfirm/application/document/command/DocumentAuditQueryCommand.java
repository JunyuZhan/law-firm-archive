package com.lawfirm.application.document.command;

import lombok.Data;

/**
 * 文档审计查询命令（M5-044~M5-045）
 */
@Data
public class DocumentAuditQueryCommand {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作类型
     */
    private String actionType;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}

