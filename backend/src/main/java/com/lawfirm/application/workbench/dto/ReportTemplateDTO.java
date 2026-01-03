package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 报表模板 DTO
 */
@Data
public class ReportTemplateDTO {
    private Long id;
    private String templateNo;
    private String templateName;
    private String description;
    private String dataSource;
    private String dataSourceName;
    
    /**
     * 字段配置
     */
    private List<FieldConfig> fieldConfig;
    
    /**
     * 筛选条件配置
     */
    private List<FilterConfig> filterConfig;
    
    /**
     * 分组配置
     */
    private List<GroupConfig> groupConfig;
    
    /**
     * 排序配置
     */
    private List<SortConfig> sortConfig;
    
    /**
     * 聚合配置
     */
    private List<AggregateConfig> aggregateConfig;
    
    private String status;
    private String statusName;
    private Boolean isSystem;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 字段配置
     */
    @Data
    public static class FieldConfig {
        private String field;
        private String label;
        private String type;
        private Boolean visible;
        private Boolean sortable;
        private Integer width;
    }
    
    /**
     * 筛选条件配置
     */
    @Data
    public static class FilterConfig {
        private String field;
        private String label;
        private String type;
        private List<String> options;
        private Object defaultValue;
    }
    
    /**
     * 分组配置
     */
    @Data
    public static class GroupConfig {
        private String field;
        private String label;
    }
    
    /**
     * 排序配置
     */
    @Data
    public static class SortConfig {
        private String field;
        private String direction;
    }
    
    /**
     * 聚合配置
     */
    @Data
    public static class AggregateConfig {
        private String field;
        private String function;
        private String label;
    }
}
