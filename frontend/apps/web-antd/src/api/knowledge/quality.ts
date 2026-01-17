import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

// 质量检查
export interface QualityCheckDTO {
  id: number;
  checkNo: string;
  matterId: number;
  matterName?: string;
  checkerId: number;
  checkerName?: string;
  checkDate?: string;
  checkType: string;
  checkTypeName?: string;
  status: string;
  statusName?: string;
  totalScore?: number;
  qualified?: boolean;
  checkSummary?: string;
  details?: QualityCheckDetailDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface QualityCheckDetailDTO {
  id: number;
  checkId: number;
  standardId: number;
  standardName?: string;
  checkResult: string;
  score?: number;
  maxScore?: number;
  findings?: string;
  suggestions?: string;
}

export interface CreateQualityCheckCommand {
  matterId: number;
  checkDate?: string;
  checkType: string;
  checkSummary?: string;
  details?: CreateCheckDetailCommand[];
}

export interface CreateCheckDetailCommand {
  standardId: number;
  checkResult: string;
  score?: number;
  maxScore?: number;
  findings?: string;
  suggestions?: string;
}

// 质量检查标准
export interface QualityCheckStandardDTO {
  id: number;
  standardNo?: string;
  standardName: string;
  category: string;
  categoryName?: string;
  description?: string;
  checkPoints?: string;
  maxScore?: number;
  weight?: number;
  enabled?: boolean;
  sortOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateQualityCheckStandardCommand {
  standardName: string;
  category: string;
  description?: string;
  checkPoints?: string;
  maxScore?: number;
  weight?: number;
}

// 问题整改
export interface QualityIssueDTO {
  id: number;
  issueNo?: string;
  matterId: number;
  matterName?: string;
  checkId?: number;
  checkNo?: string;
  issueType: string;
  issueTypeName?: string;
  severity: string;
  severityName?: string;
  description: string;
  responsible?: string;
  deadline?: string;
  status: string;
  statusName?: string;
  resolution?: string;
  resolvedAt?: string;
  resolvedBy?: number;
  resolvedByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateQualityIssueCommand {
  matterId: number;
  checkId?: number;
  issueType: string;
  severity: string;
  description: string;
  responsible?: string;
  deadline?: string;
}

// 风险预警
export interface RiskWarningDTO {
  id: number;
  warningNo?: string;
  matterId: number;
  matterName?: string;
  riskType: string;
  riskTypeName?: string;
  riskLevel: string;
  riskLevelName?: string;
  description: string;
  suggestedAction?: string;
  status: string;
  statusName?: string;
  acknowledgedBy?: number;
  acknowledgedByName?: string;
  acknowledgedAt?: string;
  resolvedBy?: number;
  resolvedByName?: string;
  resolvedAt?: string;
  resolution?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateRiskWarningCommand {
  matterId: number;
  riskType: string;
  riskLevel: string;
  description: string;
  suggestedAction?: string;
}

// 选项常量
export const CHECK_TYPE_OPTIONS = [
  { label: '常规检查', value: 'ROUTINE' },
  { label: '随机检查', value: 'RANDOM' },
  { label: '专项检查', value: 'SPECIAL' },
];

export const CHECK_STATUS_OPTIONS = [
  { label: '进行中', value: 'IN_PROGRESS', color: 'blue' },
  { label: '已完成', value: 'COMPLETED', color: 'green' },
];

export const CHECK_RESULT_OPTIONS = [
  { label: '通过', value: 'PASS', color: 'green' },
  { label: '不通过', value: 'FAIL', color: 'red' },
  { label: '部分通过', value: 'PARTIAL', color: 'orange' },
];

export const STANDARD_CATEGORY_OPTIONS = [
  { label: '文档规范', value: 'DOCUMENT' },
  { label: '流程规范', value: 'PROCESS' },
  { label: '服务质量', value: 'SERVICE' },
  { label: '合规检查', value: 'COMPLIANCE' },
];

export const ISSUE_TYPE_OPTIONS = [
  { label: '文档缺失', value: 'DOC_MISSING' },
  { label: '流程违规', value: 'PROCESS_VIOLATION' },
  { label: '质量问题', value: 'QUALITY_ISSUE' },
  { label: '合规问题', value: 'COMPLIANCE_ISSUE' },
  { label: '其他', value: 'OTHER' },
];

export const ISSUE_SEVERITY_OPTIONS = [
  { label: '轻微', value: 'LOW', color: 'default' },
  { label: '一般', value: 'MEDIUM', color: 'orange' },
  { label: '严重', value: 'HIGH', color: 'red' },
  { label: '紧急', value: 'CRITICAL', color: 'magenta' },
];

export const ISSUE_STATUS_OPTIONS = [
  { label: '待处理', value: 'PENDING', color: 'default' },
  { label: '处理中', value: 'IN_PROGRESS', color: 'blue' },
  { label: '已解决', value: 'RESOLVED', color: 'green' },
  { label: '已关闭', value: 'CLOSED', color: 'default' },
];

export const RISK_TYPE_OPTIONS = [
  { label: '法律风险', value: 'LEGAL' },
  { label: '合规风险', value: 'COMPLIANCE' },
  { label: '时效风险', value: 'DEADLINE' },
  { label: '质量风险', value: 'QUALITY' },
  { label: '财务风险', value: 'FINANCIAL' },
  { label: '其他', value: 'OTHER' },
];

export const RISK_LEVEL_OPTIONS = [
  { label: '低', value: 'LOW', color: 'default' },
  { label: '中', value: 'MEDIUM', color: 'orange' },
  { label: '高', value: 'HIGH', color: 'red' },
  { label: '极高', value: 'CRITICAL', color: 'magenta' },
];

export const WARNING_STATUS_OPTIONS = [
  { label: '待确认', value: 'PENDING', color: 'default' },
  { label: '已确认', value: 'ACKNOWLEDGED', color: 'blue' },
  { label: '已解决', value: 'RESOLVED', color: 'green' },
  { label: '已关闭', value: 'CLOSED', color: 'default' },
];

// ==================== 质量检查 API ====================

/** 创建质量检查 */
export function createQualityCheck(data: CreateQualityCheckCommand) {
  return requestClient.post<QualityCheckDTO>('/knowledge/quality-check', data);
}

/** 获取检查详情 */
export function getQualityCheckById(id: number) {
  return requestClient.get<QualityCheckDTO>(`/knowledge/quality-check/${id}`);
}

/** 获取项目的所有检查 */
export function getQualityChecksByMatterId(matterId: number) {
  return requestClient.get<QualityCheckDTO[]>(
    `/knowledge/quality-check/matter/${matterId}`,
  );
}

/** 获取进行中的检查 */
export function getInProgressChecks() {
  return requestClient.get<QualityCheckDTO[]>(
    '/knowledge/quality-check/in-progress',
  );
}

// ==================== 质量检查标准 API ====================

/** 获取所有启用的检查标准 */
export function getEnabledStandards() {
  return requestClient.get<QualityCheckStandardDTO[]>(
    '/knowledge/quality-standard/enabled',
  );
}

/** 按分类查询检查标准 */
export function getStandardsByCategory(category: string) {
  return requestClient.get<QualityCheckStandardDTO[]>(
    `/knowledge/quality-standard/category/${category}`,
  );
}

/** 获取检查标准详情 */
export function getStandardById(id: number) {
  return requestClient.get<QualityCheckStandardDTO>(
    `/knowledge/quality-standard/${id}`,
  );
}

/** 创建检查标准 */
export function createStandard(data: CreateQualityCheckStandardCommand) {
  return requestClient.post<QualityCheckStandardDTO>(
    '/knowledge/quality-standard',
    data,
  );
}

/** 更新检查标准 */
export function updateStandard(
  id: number,
  data: CreateQualityCheckStandardCommand,
) {
  return requestClient.put<QualityCheckStandardDTO>(
    `/knowledge/quality-standard/${id}`,
    data,
  );
}

/** 删除检查标准 */
export function deleteStandard(id: number) {
  return requestClient.delete(`/knowledge/quality-standard/${id}`);
}

// ==================== 问题整改 API ====================

/** 获取待整改的问题 */
export function getPendingIssues() {
  return requestClient.get<QualityIssueDTO[]>(
    '/knowledge/quality-issue/pending',
  );
}

/** 创建问题 */
export function createIssue(data: CreateQualityIssueCommand) {
  return requestClient.post<QualityIssueDTO>('/knowledge/quality-issue', data);
}

/** 更新问题状态 */
export function updateIssueStatus(
  id: number,
  status: string,
  resolution?: string,
) {
  return requestClient.put<QualityIssueDTO>(
    `/knowledge/quality-issue/${id}/status`,
    null,
    {
      params: { status, resolution },
    },
  );
}

/** 获取问题详情 */
export function getIssueById(id: number) {
  return requestClient.get<QualityIssueDTO>(`/knowledge/quality-issue/${id}`);
}

/** 获取项目的所有问题 */
export function getIssuesByMatterId(matterId: number) {
  return requestClient.get<QualityIssueDTO[]>(
    `/knowledge/quality-issue/matter/${matterId}`,
  );
}

// ==================== 风险预警 API ====================

/** 创建风险预警 */
export function createWarning(data: CreateRiskWarningCommand) {
  return requestClient.post<RiskWarningDTO>('/knowledge/risk-warning', data);
}

/** 确认预警 */
export function acknowledgeWarning(id: number) {
  return requestClient.post<RiskWarningDTO>(
    `/knowledge/risk-warning/${id}/acknowledge`,
  );
}

/** 解决预警 */
export function resolveWarning(id: number) {
  return requestClient.post<RiskWarningDTO>(
    `/knowledge/risk-warning/${id}/resolve`,
  );
}

/** 关闭预警 */
export function closeWarning(id: number) {
  return requestClient.post<RiskWarningDTO>(
    `/knowledge/risk-warning/${id}/close`,
  );
}

/** 获取预警详情 */
export function getWarningById(id: number) {
  return requestClient.get<RiskWarningDTO>(`/knowledge/risk-warning/${id}`);
}

/** 获取项目的所有预警 */
export function getWarningsByMatterId(matterId: number) {
  return requestClient.get<RiskWarningDTO[]>(
    `/knowledge/risk-warning/matter/${matterId}`,
  );
}

/** 获取活跃的预警 */
export function getActiveWarnings() {
  return requestClient.get<RiskWarningDTO[]>('/knowledge/risk-warning/active');
}

/** 获取高风险预警 */
export function getHighRiskWarnings() {
  return requestClient.get<RiskWarningDTO[]>(
    '/knowledge/risk-warning/high-risk',
  );
}
