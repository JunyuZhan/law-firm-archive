/**
 * 统计中心 API
 */
import { requestClient } from '#/api/request';

export interface RevenueStats {
  totalRevenue: number | string; // BigDecimal可能序列化为字符串
  monthlyRevenue: number | string;
  yearlyRevenue: number | string;
  pendingRevenue: number | string;
  growthRate: number | string;
  trends: RevenueTrend[];
}

export interface RevenueTrend {
  period: string;
  amount: number | string; // BigDecimal可能序列化为字符串
}

export interface MatterStats {
  totalMatters: number;
  activeMatters: number;
  completedMatters: number;
  statusCount: Record<string, number>;
  typeCount: Record<string, number>;
}

export interface ClientStats {
  totalClients: number;
  formalClients: number;
  potentialClients: number;
  newClientsThisMonth: number;
  typeCount: Record<string, number>;
}

export interface LawyerPerformance {
  lawyerId: number;
  lawyerName: string;
  matterCount: number;
  revenue: number | string; // BigDecimal可能序列化为字符串
  commission: number | string; // BigDecimal可能序列化为字符串
  hours: number;
  rank: number;
}

/** 获取收入统计 */
export function getRevenueStats() {
  return requestClient.get<RevenueStats>('/workbench/statistics/revenue');
}

/** 获取项目统计 */
export function getMatterStats() {
  return requestClient.get<MatterStats>('/workbench/statistics/matter');
}

/** 获取客户统计 */
export function getClientStats() {
  return requestClient.get<ClientStats>('/workbench/statistics/client');
}

/** 获取律师业绩排行 */
export function getLawyerPerformanceRanking(limit = 10) {
  return requestClient.get<LawyerPerformance[]>(
    '/workbench/statistics/lawyer-performance',
    {
      params: { limit },
    },
  );
}
