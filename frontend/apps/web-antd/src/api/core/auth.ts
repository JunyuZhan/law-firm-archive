import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    username: string;
    password: string;
    captchaId?: string;
    captchaCode?: string;
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

  /** 验证码结果 */
  export interface CaptchaResult {
    captchaId: string;
    captchaImage: string;
  }
}

/**
 * 获取验证码
 */
export async function getCaptchaApi() {
  return requestClient.get<AuthApi.CaptchaResult>('/auth/captcha');
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  return requestClient.post<AuthApi.LoginResult>('/auth/login', data);
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
 */
export async function logoutApi() {
  return requestClient.post('/auth/logout');
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
