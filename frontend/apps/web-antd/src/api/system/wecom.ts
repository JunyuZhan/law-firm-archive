/**
 * 企业微信相关API
 */

import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface UserWecomDTO {
  id: number;
  userId: number;
  wecomUserid: string | null;
  wecomMobile: string | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface BindWecomCommand {
  userId: number;
  wecomUserid?: string;
  wecomMobile?: string;
}

// ==================== API方法 ====================

/**
 * 获取当前用户的企业微信绑定信息
 */
export function getMyWecomBinding() {
  return requestClient.get<UserWecomDTO | null>('/system/wecom/my');
}

/**
 * 绑定企业微信
 */
export function bindWecom(data: BindWecomCommand) {
  return requestClient.post<UserWecomDTO>('/system/wecom/bind', data);
}

/**
 * 解绑企业微信
 */
export function unbindWecom() {
  return requestClient.post<void>('/system/wecom/unbind');
}

/**
 * 测试企业微信机器人连接
 */
export function testWecomBot() {
  return requestClient.post<{ success: boolean; message: string }>(
    '/system/wecom/test',
  );
}

/**
 * 获取企业微信推送是否启用
 */
export function getWecomStatus() {
  return requestClient.get<{ enabled: boolean }>('/system/wecom/status');
}
