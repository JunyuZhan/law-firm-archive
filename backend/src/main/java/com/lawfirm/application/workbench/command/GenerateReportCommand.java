package com.lawfirm.application.workbench.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 生成报表命令
 */
@Data
public class GenerateReportCommand {
    /**
     * 报表类型
     */
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表格式（EXCEL, PDF）
     */
    @NotBlank(message = "报表格式不能为空")
    private String format;

    /**
     * 报表参数（如日期范围、筛选条件等）
     */
    private Map<String, Object> parameters;
}

