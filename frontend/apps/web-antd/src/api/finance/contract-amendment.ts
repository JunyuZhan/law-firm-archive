import { requestClient } from '#/api/request';

export interface ContractAmendment {
  id: number;
  amendmentNo: string;
  contractId: number;
  amendmentType: string;
  beforeSnapshot: string;
  afterSnapshot: string;
  amendmentReason: string;
  lawyerAmendedBy: number;
  lawyerAmendedAt: string;
  status: string;
  financeHandledBy: number;
  financeHandledAt: string;
  financeRemark: string;
  affectsPayments: boolean;
  affectedPaymentIds: string;
}

/**
 * 获取待处理的变更记录
 */
export function getPendingAmendments() {
  return requestClient.get<ContractAmendment[]>(
    '/finance/contract-amendments/pending',
  );
}

/**
 * 获取合同的变更记录
 */
export function getAmendmentsByContractId(contractId: number) {
  return requestClient.get<ContractAmendment[]>(
    `/finance/contract-amendments/contract/${contractId}`,
  );
}

/**
 * 同步变更到财务数据
 */
export function syncAmendment(amendmentId: number, remark?: string) {
  return requestClient.post(
    `/finance/contract-amendments/${amendmentId}/sync`,
    null,
    {
      params: { remark },
    },
  );
}

/**
 * 忽略变更
 */
export function ignoreAmendment(amendmentId: number, remark: string) {
  return requestClient.post(
    `/finance/contract-amendments/${amendmentId}/ignore`,
    null,
    {
      params: { remark },
    },
  );
}
