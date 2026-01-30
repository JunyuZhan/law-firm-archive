import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    username: string;
    password: string;
    sliderVerifyToken?: string; // 滑块验证凭证（必须）
    captchaId?: string; // 图形验证码ID（失败多次后需要）
    captchaCode?: string; // 图形验证码答案（失败多次后需要）
    permitRequestId?: string; // 许可码请求ID（异地登录后需要）
    permitCode?: string; // 许可码（异地登录后需要，联系管理员获取）
  }

  /** 异地登录响应 */
  export interface NewLocationResponse {
    requestId: string; // 许可码请求ID
    currentLocation: string; // 当前登录位置
    message: string; // 提示信息
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    userId: number;
    username: string;
    realName: string;
    roles: string[];
    permissions: string[];
  }

  /** 刷新Token请求参数 */
  export interface RefreshTokenParams {
    refreshToken: string;
  }

  /** 用户信息 */
  export interface UserInfo {
    userId: number;
    username: string;
    realName: string;
    email?: string;
    departmentId?: number;
    compensationType?: string;
    roles: string[];
    permissions: string[];
    avatar?: string;
    homePath?: string;
  }

  /** 图形验证码结果 */
  export interface CaptchaResult {
    captchaId: string;
    captchaUrl: string;
  }

  /** 滑块验证令牌结果 */
  export interface SliderTokenResult {
    tokenId: string;
    targetPosition: number;
    expireSeconds: number;
  }

  /** 滑块验证请求参数 */
  export interface SliderVerifyParams {
    tokenId: string;
    slideTime: number;
    slideTrack?: number[];
  }

  /** 滑块验证结果 */
  export interface SliderVerifyResult {
    success: boolean;
    message: string;
    verifyToken?: string;
  }

  /** 登录状态检查结果 */
  export interface LoginStatusResult {
    locked: boolean;
    lockRemainingMinutes: number;
    captchaRequired: boolean;
    failCount: number;
    message?: string;
  }
}

/**
 * 获取图形验证码
 * 使用 baseRequestClient 避免触发 401 重试逻辑（登录前调用）
 */
export async function getCaptchaApi() {
  return baseRequestClient.get<AuthApi.CaptchaResult>('/auth/captcha');
}

/**
 * 获取滑块验证令牌
 * 使用 baseRequestClient 避免触发 401 重试逻辑（登录前调用）
 */
export async function getSliderTokenApi() {
  return baseRequestClient.get<AuthApi.SliderTokenResult>('/auth/slider/token');
}

/**
 * 验证滑块操作
 * 使用 baseRequestClient 避免触发 401 重试逻辑（登录前调用）
 */
export async function verifySliderApi(data: AuthApi.SliderVerifyParams) {
  return baseRequestClient.post<AuthApi.SliderVerifyResult>(
    '/auth/slider/verify',
    data,
  );
}

/**
 * 检查登录状态（是否锁定、是否需要验证码）
 * 使用 baseRequestClient 避免触发 401 重试逻辑（登录前调用）
 */
export async function checkLoginStatusApi(username: string) {
  return baseRequestClient.get<AuthApi.LoginStatusResult>(
    '/auth/login/status',
    {
      params: { username },
    },
  );
}

/**
 * 登录
 * 使用 baseRequestClient 避免触发 401 重试逻辑
 */
export async function loginApi(data: AuthApi.LoginParams) {
  return baseRequestClient.post<AuthApi.LoginResult>('/auth/login', data);
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  // 从localStorage获取refreshToken
  const refreshToken = localStorage.getItem('refreshToken') || '';

  return baseRequestClient.post<AuthApi.LoginResult>('/auth/refresh', {
    refreshToken,
  });
}

/**
 * 退出登录
 * 使用 baseRequestClient 避免触发 401 重试逻辑，防止循环调用
 */
export async function logoutApi() {
  return baseRequestClient.post('/auth/logout');
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  // 从登录时已获取的权限中返回，或重新获取用户信息
  const userInfo = await getUserInfoApi();
  return userInfo.permissions || [];
}

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  return requestClient.get<AuthApi.UserInfo>('/auth/info');
}
