/**
 * 报表中心 API
 */
import { requestClient } from '#/api/request';

export interface ReportDTO {
  id: number;
  reportNo: string;
  reportName: string;
  reportType: string;
  reportTypeName?: string;
  format: string;
  formatName?: string;
  status: string;
  statusName?: string;
  fileUrl?: string;
  fileSize?: number;
  generatedBy?: number;
  generatedByName?: string;
  generatedAt?: string;
  parameters?: Record<string, any>;
  createdAt?: string;
}

export interface ReportQuery {
  pageNum?: number;
  pageSize?: number;
  reportType?: string;
  status?: string;
}

export interface AvailableReport {
  type: string;
  name: string;
  description: string;
  formats: string[];
}

export interface GenerateReportCommand {
  reportType: string;
  reportName?: string;
  format: 'EXCEL' | 'PDF';
  parameters?: Record<string, any>;
}

/** 获取可用报表列表 */
export function getAvailableReports() {
  return requestClient.get<AvailableReport[]>('/workbench/report/available');
}

/** 获取报表列表 */
export function getReportList(params: ReportQuery) {
  return requestClient.get<{ records: ReportDTO[]; total: number }>('/workbench/report', { params });
}

/** 获取报表详情 */
export function getReportDetail(id: number) {
  return requestClient.get<ReportDTO>(`/workbench/report/${id}`);
}

/** 生成报表 */
export function generateReport(data: GenerateReportCommand) {
  return requestClient.post<ReportDTO>('/workbench/report/generate', data);
}

/** 获取报表下载URL */
export function getReportDownloadUrl(id: number) {
  return requestClient.get<string>(`/workbench/report/${id}/download-url`);
}

/** 删除报表 */
export function deleteReport(id: number) {
  return requestClient.delete(`/workbench/report/${id}`);
}

