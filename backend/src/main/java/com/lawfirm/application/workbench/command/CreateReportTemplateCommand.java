package com.lawfirm.application.workbench.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 创建报表模板命令. */
@Data
public class CreateReportTemplateCommand {

  /** 模板名称. */
  @NotBlank(message = "模板名称不能为空")
  private String templateName;

  /** 描述. */
  private String description;

  /** 数据源. */
  @NotBlank(message = "数据源不能为空")
  private String dataSource;

  /** 字段配置. */
  @NotNull(message = "字段配置不能为空")
  private List<Map<String, Object>> fieldConfig;

  /** 筛选条件配置. */
  private List<Map<String, Object>> filterConfig;

  /** 分组配置. */
  private List<Map<String, Object>> groupConfig;

  /** 排序配置. */
  private List<Map<String, Object>> sortConfig;

  /** 聚合配置. */
  private List<Map<String, Object>> aggregateConfig;
}
