import type { PageResult } from '../matter/types';

/**
 * 报表模板管理 API
 */
import { requestClient } from '#/api/request';

// ========== 类型定义 ==========
export interface FieldConfig {
  field: string;
  label?: string;
  type?: string;
  visible?: boolean;
  sortable?: boolean;
  width?: number;
}

export interface FilterConfig {
  field: string;
  label?: string;
  type?: string;
  options?: string[];
  defaultValue?: any;
}

export interface GroupConfig {
  field: string;
  label?: string;
}

export interface SortConfig {
  field: string;
  direction?: string;
}

export interface AggregateConfig {
  field: string;
  function?: string;
  label?: string;
}

export interface ReportTemplateDTO {
  id: number;
  templateNo?: string;
  templateName: string;
  description?: string;
  dataSource: string;
  dataSourceName?: string;
  fieldConfig?: FieldConfig[];
  filterConfig?: FilterConfig[];
  groupConfig?: GroupConfig[];
  sortConfig?: SortConfig[];
  aggregateConfig?: AggregateConfig[];
  status?: string;
  statusName?: string;
  isSystem?: boolean;
  createdBy?: number;
  createdByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReportTemplateQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  dataSource?: string;
  status?: string;
}

export interface CreateReportTemplateCommand {
  templateName: string;
  description?: string;
  dataSource: string;
  fieldConfig: Record<string, any>[];
  filterConfig?: Record<string, any>[];
  groupConfig?: Record<string, any>[];
  sortConfig?: Record<string, any>[];
  aggregateConfig?: Record<string, any>[];
}

export interface GenerateReportByTemplateCommand {
  parameters?: Record<string, any>;
  format?: string;
}

// ========== API 函数 ==========

/** 分页查询报表模板 */
export function getReportTemplateList(params: ReportTemplateQuery) {
  return requestClient.get<PageResult<ReportTemplateDTO>>(
    '/workbench/report-template',
    { params },
  );
}

/** 获取模板详情 */
export function getReportTemplateDetail(id: number) {
  return requestClient.get<ReportTemplateDTO>(
    `/workbench/report-template/${id}`,
  );
}

/** 创建报表模板 */
export function createReportTemplate(data: CreateReportTemplateCommand) {
  return requestClient.post<ReportTemplateDTO>(
    '/workbench/report-template',
    data,
  );
}

/** 更新报表模板 */
export function updateReportTemplate(
  id: number,
  data: CreateReportTemplateCommand,
) {
  return requestClient.put<ReportTemplateDTO>(
    `/workbench/report-template/${id}`,
    data,
  );
}

/** 删除报表模板 */
export function deleteReportTemplate(id: number) {
  return requestClient.delete(`/workbench/report-template/${id}`);
}

/** 启用模板 */
export function enableReportTemplate(id: number) {
  return requestClient.post(`/workbench/report-template/${id}/enable`);
}

/** 停用模板 */
export function disableReportTemplate(id: number) {
  return requestClient.post(`/workbench/report-template/${id}/disable`);
}

/** 根据模板生成报表 */
export function generateReportByTemplate(
  id: number,
  data?: GenerateReportByTemplateCommand,
) {
  return requestClient.post<any>(
    `/workbench/report-template/${id}/generate`,
    data?.parameters,
    {
      params: { format: data?.format || 'EXCEL' },
    },
  );
}

/** 获取可用数据源列表 */
export function getReportDataSources() {
  return requestClient.get<Record<string, any>[]>(
    '/workbench/report-template/data-sources',
  );
}

/** 获取数据源可用字段 */
export function getReportDataSourceFields(dataSource: string) {
  return requestClient.get<Record<string, any>[]>(
    `/workbench/report-template/data-sources/${dataSource}/fields`,
  );
}
