import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface LetterTemplateDTO {
  id: number;
  templateNo: string;
  name: string;
  letterType: string;
  letterTypeName: string;
  content: string;
  description?: string;
  status: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface LetterApplicationDTO {
  id: number;
  applicationNo: string;
  templateId: number;
  templateName?: string;
  matterId: number;
  matterName?: string;
  matterNo?: string;
  clientId?: number;
  clientName?: string;
  applicantId: number;
  applicantName: string;
  departmentId?: number;
  departmentName?: string;
  letterType: string;
  letterTypeName: string;
  targetUnit: string;
  targetContact?: string;
  targetPhone?: string;
  targetAddress?: string;
  purpose: string;
  lawyerIds?: string;
  lawyerNames?: string;
  content?: string;
  copies: number;
  expectedDate?: string;
  status: string;
  statusName: string;
  assignedApproverId?: number;
  assignedApproverName?: string;
  approvedBy?: number;
  approverName?: string;
  approvedAt?: string;
  approvalComment?: string;
  printedBy?: number;
  printerName?: string;
  printedAt?: string;
  receivedBy?: number;
  receiverName?: string;
  receivedAt?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLetterApplicationCommand {
  templateId: number;
  matterId: number;
  targetUnit: string;
  targetContact?: string;
  targetPhone?: string;
  targetAddress?: string;
  purpose: string;
  lawyerIds?: number[];
  approverId?: number;
  copies?: number;
  expectedDate?: string;
  remark?: string;
}

// ==================== 模板管理 ====================

/** 获取启用的模板列表（需要 admin:letter:list 权限） */
export function getActiveTemplates() {
  return requestClient.get<LetterTemplateDTO[]>('/admin/letter/template/list');
}

/** 获取启用的模板列表（公共接口，用于律师申请出函） */
export function getActiveTemplatesPublic() {
  return requestClient.get<LetterTemplateDTO[]>(
    '/admin/letter/template/active',
  );
}

/** 获取所有模板（管理员） */
export function getAllTemplates() {
  return requestClient.get<LetterTemplateDTO[]>('/admin/letter/template/all');
}

/** 获取模板详情 */
export function getTemplateDetail(id: number) {
  return requestClient.get<LetterTemplateDTO>(`/admin/letter/template/${id}`);
}

/** 创建模板 */
export function createTemplate(data: {
  content: string;
  description?: string;
  letterType: string;
  name: string;
}) {
  return requestClient.post<LetterTemplateDTO>('/admin/letter/template', null, {
    params: data,
  });
}

/** 更新模板 */
export function updateTemplate(
  id: number,
  data: {
    content?: string;
    description?: string;
    letterType?: string;
    name?: string;
  },
) {
  return requestClient.put<LetterTemplateDTO>(
    `/admin/letter/template/${id}`,
    null,
    {
      params: data,
    },
  );
}

/** 启用/停用模板 */
export function toggleTemplateStatus(id: number) {
  return requestClient.post<void>(`/admin/letter/template/${id}/toggle`);
}

/** 删除模板 */
export function deleteTemplate(id: number) {
  return requestClient.delete<void>(`/admin/letter/template/${id}`);
}

// ==================== 出函申请 ====================

/** 创建出函申请 */
export function createLetterApplication(data: CreateLetterApplicationCommand) {
  return requestClient.post<LetterApplicationDTO>(
    '/admin/letter/application',
    data,
  );
}

/** 获取申请详情 */
export function getApplicationDetail(id: number) {
  return requestClient.get<LetterApplicationDTO>(
    `/admin/letter/application/${id}`,
  );
}

/** 我的申请列表 */
export function getMyApplications() {
  return requestClient.get<LetterApplicationDTO[]>(
    '/admin/letter/application/my',
  );
}

/** 项目的出函记录 */
export function getApplicationsByMatter(matterId: number) {
  return requestClient.get<LetterApplicationDTO[]>(
    `/admin/letter/application/matter/${matterId}`,
  );
}

/** 取消申请 */
export function cancelApplication(id: number) {
  return requestClient.post<void>(`/admin/letter/application/${id}/cancel`);
}

/** 待审批列表 */
export function getPendingApprovalList() {
  return requestClient.get<LetterApplicationDTO[]>(
    '/admin/letter/application/pending-approval',
  );
}

/** 审批通过 */
export function approveApplication(id: number, comment?: string) {
  return requestClient.post<void>(
    `/admin/letter/application/${id}/approve`,
    null,
    {
      params: { comment },
    },
  );
}

/** 审批拒绝 */
export function rejectApplication(id: number, comment: string) {
  return requestClient.post<void>(
    `/admin/letter/application/${id}/reject`,
    null,
    {
      params: { comment },
    },
  );
}

/** 退回修改 */
export function returnApplication(id: number, comment: string) {
  return requestClient.post<void>(
    `/admin/letter/application/${id}/return`,
    null,
    {
      params: { comment },
    },
  );
}

/** 更新申请（被退回后修改） */
export function updateApplication(
  id: number,
  data: CreateLetterApplicationCommand,
) {
  return requestClient.put<LetterApplicationDTO>(
    `/admin/letter/application/${id}`,
    data,
  );
}

/** 重新提交申请 */
export function resubmitApplication(
  id: number,
  data: CreateLetterApplicationCommand,
) {
  return requestClient.post<LetterApplicationDTO>(
    `/admin/letter/application/${id}/resubmit`,
    data,
  );
}

/** 重新提交审批（仅改变状态） */
export function submitForApproval(id: number) {
  return requestClient.post<void>(`/admin/letter/application/${id}/submit`);
}

// ==================== 行政操作 ====================

/** 获取全部申请列表（行政管理） */
export function getAllApplications(params?: {
  applicationNo?: string;
  endDate?: string;
  matterName?: string;
  startDate?: string;
  status?: string;
}) {
  return requestClient.get<LetterApplicationDTO[]>(
    '/admin/letter/application/all',
    { params },
  );
}

/** 待打印列表 */
export function getPendingPrintList() {
  return requestClient.get<LetterApplicationDTO[]>(
    '/admin/letter/application/pending-print',
  );
}

/** 确认打印 */
export function confirmPrint(id: number) {
  return requestClient.post<void>(`/admin/letter/application/${id}/print`);
}

/** 确认领取 */
export function confirmReceive(id: number) {
  return requestClient.post<void>(`/admin/letter/application/${id}/receive`);
}

/** 更新函件内容（行政人员） */
export function updateLetterContent(id: number, content: string) {
  return requestClient.put<LetterApplicationDTO>(
    `/admin/letter/application/${id}/content`,
    null,
    {
      params: { content },
    },
  );
}

// ==================== 二维码相关 ====================

/** 获取函件验证二维码（Base64） */
export function getLetterQrCode(id: number, size?: number) {
  return requestClient.get<{
    applicationNo: string;
    qrCodeBase64: string;
    verificationUrl: string;
  }>(`/admin/letter/application/${id}/qrcode`, {
    params: { size: size || 200 },
  });
}

/** 下载函件验证二维码图片 */
export function downloadLetterQrCodeImage(id: number, size?: number) {
  return requestClient.get<Blob>(
    `/admin/letter/application/${id}/qrcode/image`,
    {
      params: { size: size || 200 },
      responseType: 'blob',
    },
  );
}
