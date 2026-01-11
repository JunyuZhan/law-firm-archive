import { requestClient } from '../request';

export interface CommissionRule {
  id?: number;
  ruleCode: string;
  ruleName: string;
  ruleType?: string;
  firmRate: number; // 律所比例(%)，0表示不参与分配
  leadLawyerRate: number; // 主办律师比例(%)，0表示不参与分配
  assistLawyerRate: number; // 协办律师比例(%)，0表示无协办或不参与分配
  supportStaffRate: number; // 辅助人员比例(%)，0表示无辅助或不参与分配
  originatorRate: number; // 案源人比例(%)，0表示不参与分配
  allowModify: boolean; // 律师创建合同时是否允许修改比例
  description?: string;
  isDefault: boolean;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CommissionRuleQuery {
  page?: number;
  size?: number;
  name?: string;
  status?: string;
}

export interface CommissionRuleListResponse {
  records: CommissionRule[];
  total: number;
  current: number;
  size: number;
}

export const commissionRuleApi = {
  /**
   * 获取提成规则列表
   */
  getList: (params?: CommissionRuleQuery) => {
    return requestClient.get<CommissionRule[]>('/finance/commission/rules', {
      params,
    });
  },

  /**
   * 获取提成规则详情
   */
  getById: (id: number) => {
    return requestClient.get<CommissionRule>(`/finance/commission/rules/${id}`);
  },

  /**
   * 创建提成规则
   */
  create: (data: Omit<CommissionRule, 'createdAt' | 'id' | 'updatedAt'>) => {
    return requestClient.post<CommissionRule>(
      '/finance/commission/rules',
      data,
    );
  },

  /**
   * 更新提成规则
   */
  update: (id: number, data: Partial<CommissionRule>) => {
    return requestClient.put<CommissionRule>(
      `/finance/commission/rules/${id}`,
      data,
    );
  },

  /**
   * 删除提成规则
   */
  delete: (id: number) => {
    return requestClient.delete(`/finance/commission/rules/${id}`);
  },

  /**
   * 设置默认规则
   */
  setDefault: (id: number) => {
    return requestClient.put(`/finance/commission/rules/${id}/set-default`);
  },

  /**
   * 切换规则状态
   */
  toggle: (id: number) => {
    return requestClient.put(`/finance/commission/rules/${id}/toggle`);
  },

  /**
   * 获取默认规则
   */
  getDefault: () => {
    return requestClient.get<CommissionRule>(
      '/finance/commission/rules/default',
    );
  },

  /**
   * 获取启用的规则列表（公共接口，用于律师创建合同时选择）
   * 无需特殊权限
   */
  getActiveRules: () => {
    return requestClient.get<CommissionRule[]>(
      '/finance/commission/rules/active',
    );
  },
};
