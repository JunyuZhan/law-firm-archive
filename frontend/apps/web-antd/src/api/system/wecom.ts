/**
 * 企业微信相关API
 */

import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface UserWecomDTO {
  id: number;
  userId: number;
  wecomUserid: null | string;
  wecomMobile: null | string;
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
  return requestClient.get<null | UserWecomDTO>('/system/wecom/my');
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
  return requestClient.post('/system/wecom/unbind');
}

/**
 * 测试企业微信机器人连接
 */
export function testWecomBot() {
  return requestClient.post<{ message: string; success: boolean }>(
    '/system/wecom/test',
  );
}

/**
 * 获取企业微信推送是否启用
 */
export function getWecomStatus() {
  return requestClient.get<{ enabled: boolean }>('/system/wecom/status');
}
