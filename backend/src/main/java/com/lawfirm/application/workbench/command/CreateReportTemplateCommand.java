package com.lawfirm.application.workbench.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建报表模板命令
 */
@Data
public class CreateReportTemplateCommand {
    
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    private String description;
    
    @NotBlank(message = "数据源不能为空")
    private String dataSource;
    
    @NotNull(message = "字段配置不能为空")
    private List<Map<String, Object>> fieldConfig;
    
    private List<Map<String, Object>> filterConfig;
    
    private List<Map<String, Object>> groupConfig;
    
    private List<Map<String, Object>> sortConfig;
    
    private List<Map<String, Object>> aggregateConfig;
}
