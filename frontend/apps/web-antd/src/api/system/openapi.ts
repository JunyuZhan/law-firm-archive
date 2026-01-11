import { requestClient } from '#/api/request';

// ========== 类型定义 ==========

/** 推送请求 */
export interface PushRequest {
  matterId: number;
  clientId?: number;
  scopes: string[];
  pushType?: 'MANUAL' | 'AUTO' | 'UPDATE';
  validDays?: number;
}

/** 推送记录 */
export interface PushRecordDTO {
  id: number;
  matterId: number;
  matterName?: string;
  clientId: number;
  clientName?: string;
  pushType: string;
  scopes: string[];
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  externalId?: string;
  externalUrl?: string;
  errorMessage?: string;
  expiresAt?: string;
  createdAt: string;
}

/** 推送配置 */
export interface PushConfigDTO {
  id?: number;
  matterId: number;
  clientId: number;
  enabled?: boolean;
  scopes?: string[];
  autoPushOnUpdate?: boolean;
  validDays?: number;
}

/** 授权范围选项 */
export interface ScopeOption {
  value: string;
  label: string;
  description: string;
}

/** 分页查询参数 */
export interface PushQueryParams {
  matterId: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

// ========== 项目客户服务 API（数据推送）==========

/**
 * 推送项目数据到客户服务系统
 * @permission matter:clientService:create
 */
export function pushMatterData(data: PushRequest) {
  return requestClient.post<PushRecordDTO>('/matter/client-service/push', data);
}

/**
 * 获取推送记录列表
 * @permission matter:clientService:list
 */
export function getPushRecords(params: PushQueryParams) {
  return requestClient.get<{
    list: PushRecordDTO[];
    total: number;
    pageNum: number;
    pageSize: number;
  }>('/matter/client-service/records', { params });
}

/**
 * 获取推送记录详情
 * @permission matter:clientService:list
 */
export function getPushRecordById(id: number) {
  return requestClient.get<PushRecordDTO>(`/matter/client-service/records/${id}`);
}

/**
 * 获取最近一次成功推送
 * @permission matter:clientService:list
 */
export function getLatestPush(matterId: number) {
  return requestClient.get<PushRecordDTO | null>('/matter/client-service/latest', { params: { matterId } });
}

/**
 * 获取推送配置
 * @permission matter:clientService:list
 */
export function getPushConfig(matterId: number, clientId: number) {
  return requestClient.get<PushConfigDTO>('/matter/client-service/config', { params: { matterId, clientId } });
}

/**
 * 更新推送配置
 * @permission matter:clientService:create
 */
export function updatePushConfig(matterId: number, config: Partial<PushConfigDTO>) {
  return requestClient.put<PushConfigDTO>('/matter/client-service/config', config, { params: { matterId } });
}

/**
 * 获取推送统计
 * @permission matter:clientService:list
 */
export function getPushStatistics(matterId: number) {
  return requestClient.get<{
    totalPushCount: number;
    lastPushTime?: string;
    lastPushStatus?: string;
    externalUrl?: string;
  }>('/matter/client-service/statistics', { params: { matterId } });
}

/**
 * 获取可推送的数据范围选项
 * @permission matter:clientService:list
 */
export function getPushScopeOptions() {
  return requestClient.get<ScopeOption[]>('/matter/client-service/scopes');
}

// ========== 系统管理级别 API（管理员用，保留）==========

/** 创建令牌请求（保留，供客户服务系统拉取数据） */
export interface CreateTokenRequest {
  clientId: number;
  matterId?: number;
  scopes: string[];
  validDays: number;
  maxAccessCount?: number;
  ipWhitelist?: string;
  remark?: string;
}

/** 令牌信息 */
export interface ClientAccessTokenDTO {
  id: number;
  token: string;
  clientId: number;
  clientName: string;
  matterId?: number;
  matterName?: string;
  scopes: string[];
  expiresAt: string;
  maxAccessCount?: number;
  accessCount: number;
  ipWhitelist?: string;
  lastAccessIp?: string;
  lastAccessAt?: string;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  createdAt: string;
  createdBy: number;
  creatorName?: string;
  remark?: string;
  portalUrl?: string;
}

/**
 * 创建客户访问令牌（管理员）
 */
export function createAccessToken(data: CreateTokenRequest) {
  return requestClient.post<ClientAccessTokenDTO>('/system/openapi/token', data);
}

/**
 * 分页查询令牌列表（管理员）
 */
export function getTokenList(params: { clientId?: number; matterId?: number; status?: string; pageNum?: number; pageSize?: number }) {
  return requestClient.get<{
    list: ClientAccessTokenDTO[];
    total: number;
    pageNum: number;
    pageSize: number;
  }>('/system/openapi/token', { params });
}

/**
 * 撤销令牌（管理员）
 */
export function revokeToken(id: number, reason?: string) {
  return requestClient.post<void>(`/system/openapi/token/${id}/revoke`, null, {
    params: { reason },
  });
}

/**
 * 获取授权范围选项（别名，兼容旧代码）
 */
export function getScopeOptions() {
  return getPushScopeOptions();
}

/**
 * 获取指定客户的令牌列表
 */
export function getClientTokens(clientId: number) {
  return getTokenList({ clientId });
}
