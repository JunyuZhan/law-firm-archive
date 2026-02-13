package com.lawfirm.application.workbench.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 报表模板 DTO. */
@Data
public class ReportTemplateDTO {
  /** 主键ID. */
  private Long id;

  /** 模板编号. */
  private String templateNo;

  /** 模板名称. */
  private String templateName;

  /** 描述. */
  private String description;

  /** 数据源. */
  private String dataSource;

  /** 数据源名称. */
  private String dataSourceName;

  /** 字段配置. */
  private List<FieldConfig> fieldConfig;

  /** 筛选条件配置. */
  private List<FilterConfig> filterConfig;

  /** 分组配置. */
  private List<GroupConfig> groupConfig;

  /** 排序配置. */
  private List<SortConfig> sortConfig;

  /** 聚合配置. */
  private List<AggregateConfig> aggregateConfig;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 是否系统内置. */
  private Boolean isSystem;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建人名称. */
  private String createdByName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;

  /** 字段配置. */
  @Data
  public static class FieldConfig {
    /** 字段名. */
    private String field;

    /** 标签. */
    private String label;

    /** 类型. */
    private String type;

    /** 是否可见. */
    private Boolean visible;

    /** 是否可排序. */
    private Boolean sortable;

    /** 宽度. */
    private Integer width;
  }

  /** 筛选条件配置. */
  @Data
  public static class FilterConfig {
    /** 字段名. */
    private String field;

    /** 标签. */
    private String label;

    /** 类型. */
    private String type;

    /** 选项列表. */
    private List<String> options;

    /** 默认值. */
    private Object defaultValue;
  }

  /** 分组配置. */
  @Data
  public static class GroupConfig {
    /** 字段名. */
    private String field;

    /** 标签. */
    private String label;
  }

  /** 排序配置. */
  @Data
  public static class SortConfig {
    /** 字段名. */
    private String field;

    /** 排序方向. */
    private String direction;
  }

  /** 聚合配置. */
  @Data
  public static class AggregateConfig {
    /** 字段名. */
    private String field;

    /** 聚合函数. */
    private String function;

    /** 标签. */
    private String label;
  }
}
