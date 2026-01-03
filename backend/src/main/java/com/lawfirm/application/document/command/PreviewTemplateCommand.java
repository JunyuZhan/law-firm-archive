package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 模板预览命令（M5-035）
 */
@Data
public class PreviewTemplateCommand {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 案件ID（用于收集变量值）
     */
    private Long matterId;

    /**
     * 预览变量值（如果提供，将使用这些值而不是从案件收集）
     */
    private Map<String, Object> previewVariables;
}

