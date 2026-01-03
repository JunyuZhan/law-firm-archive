package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 批量生成文档命令（M5-034）
 */
@Data
public class BatchGenerateDocumentCommand {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 案件ID列表
     */
    @NotEmpty(message = "案件列表不能为空")
    private List<Long> matterIds;

    /**
     * 文档名称模板（可以使用变量，如"${matter.name}-授权委托书"）
     */
    private String documentNameTemplate;

    /**
     * 额外变量（用于所有文档）
     */
    private Map<String, Object> extraVariables;
}

