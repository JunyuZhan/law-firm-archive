package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 生成文档命令
 */
@Data
public class GenerateDocumentCommand {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 案件ID
     */
    @NotNull(message = "案件ID不能为空")
    private Long matterId;

    /**
     * 文档名称
     */
    private String documentName;

    /**
     * 额外变量（用于覆盖或补充系统自动收集的变量）
     */
    private Map<String, Object> extraVariables;
}

