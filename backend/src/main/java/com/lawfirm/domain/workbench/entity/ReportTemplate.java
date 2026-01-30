package com.lawfirm.domain.workbench.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 自定义报表模板实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("workbench_report_template")
public class ReportTemplate extends BaseEntity {

  /** 模板编号 */
  private String templateNo;

  /** 模板名称 */
  private String templateName;

  /** 模板描述 */
  private String description;

  /** 数据源：MATTER-案件, CLIENT-客户, FINANCE-财务, TIMESHEET-工时, EMPLOYEE-员工 */
  private String dataSource;

  /** 字段配置（JSON数组） */
  private String fieldConfig;

  /** 筛选条件配置（JSON数组） */
  private String filterConfig;

  /** 分组配置（JSON数组） */
  private String groupConfig;

  /** 排序配置（JSON数组） */
  private String sortConfig;

  /** 聚合配置（JSON数组） */
  private String aggregateConfig;

  /** 状态：ACTIVE-启用, INACTIVE-停用 */
  private String status;

  /** 是否系统内置模板 */
  private Boolean isSystem;

  /** 创建人姓名 */
  private String createdByName;
}
