/**
 * 国家赔偿案件 API
 */
import { requestClient } from '#/api/request';

// 国家赔偿数据类型
export interface StateCompensationDTO {
  id: number;
  matterId: number;
  obligorOrgName?: string;
  obligorOrgType?: string;
  caseSource?: string;
  damageDescription?: string;
  criminalCaseTerminated?: boolean;
  criminalCaseNo?: string;
  compensationCommittee?: string;
  applicationDate?: string;
  acceptanceDate?: string;
  decisionDate?: string;
  reconsiderationDate?: string;
  reconsiderationDecisionDate?: string;
  committeeAppDate?: string;
  committeeDecisionDate?: string;
  adminLitigationFilingDate?: string;
  adminLitigationCourtName?: string;
  claimAmount?: number;
  compensationItems?: string; // JSON 字符串
  decisionResult?: string;
  approvedAmount?: number;
  paymentStatus?: string;
  paymentDate?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

// 创建/更新命令
export interface StateCompensationCommand {
  id?: number;
  matterId: number;
  obligorOrgName?: string;
  obligorOrgType?: string;
  caseSource?: string;
  damageDescription?: string;
  criminalCaseTerminated?: boolean;
  criminalCaseNo?: string;
  compensationCommittee?: string;
  applicationDate?: string;
  acceptanceDate?: string;
  decisionDate?: string;
  reconsiderationDate?: string;
  reconsiderationDecisionDate?: string;
  committeeAppDate?: string;
  committeeDecisionDate?: string;
  adminLitigationFilingDate?: string;
  adminLitigationCourtName?: string;
  claimAmount?: number;
  compensationItems?: string; // JSON 字符串
  decisionResult?: string;
  approvedAmount?: number;
  paymentStatus?: string;
  paymentDate?: string;
  remark?: string;
}

/**
 * 获取案件的国家赔偿信息
 */
export function getStateCompensation(
  matterId: number,
): Promise<null | StateCompensationDTO> {
  return requestClient.get(`/matter/${matterId}/state-compensation`);
}

/**
 * 创建国家赔偿信息
 */
export function createStateCompensation(
  command: StateCompensationCommand,
): Promise<StateCompensationDTO> {
  return requestClient.post(
    `/matter/${command.matterId}/state-compensation`,
    command,
  );
}

/**
 * 更新国家赔偿信息
 */
export function updateStateCompensation(
  id: number,
  command: StateCompensationCommand,
): Promise<StateCompensationDTO> {
  return requestClient.put(
    `/matter/${command.matterId}/state-compensation/${id}`,
    command,
  );
}

/**
 * 删除国家赔偿信息
 */
export function deleteStateCompensation(
  matterId: number,
  id: number,
): Promise<void> {
  return requestClient.delete(`/matter/${matterId}/state-compensation/${id}`);
}
