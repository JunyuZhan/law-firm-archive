/**
 * 文档站点认证工具
 * 支持两种认证方式：
 * 1. 通过前端传递的JWT token认证（从前端应用跳转时传递）
 * 2. 通过文档站点自己的登录界面认证（固定用户名密码）
 */

const TOKEN_KEY = 'docs_auth_token';
const TOKEN_EXPIRY_KEY = 'docs_auth_token_expiry';
const DEFAULT_EXPIRY_HOURS = 24; // token默认有效期24小时

/**
 * 存储token和过期时间
 */
export function storeToken(token: string, expiryHours: number = DEFAULT_EXPIRY_HOURS): void {
  const expiryTime = Date.now() + expiryHours * 60 * 60 * 1000;
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(TOKEN_EXPIRY_KEY, expiryTime.toString());
}

/**
 * 清除token
 */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
}

/**
 * 获取当前存储的token
 */
export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * 检查token是否有效（存在且未过期）
 */
export function isTokenValid(): boolean {
  const token = getStoredToken();
  if (!token) {
    return false;
  }

  const expiryStr = localStorage.getItem(TOKEN_EXPIRY_KEY);
  if (!expiryStr) {
    return false;
  }

  const expiryTime = parseInt(expiryStr, 10);
  if (isNaN(expiryTime) || Date.now() > expiryTime) {
    clearToken(); // 清除过期的token
    return false;
  }

  return true;
}

/**
 * 处理URL中的token参数
 * 从前端应用跳转时，token会作为URL参数传递
 */
export function handleTokenFromURL(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');

  if (token) {
    // 存储token，有效期24小时
    storeToken(token);
    
    // 清除URL中的token参数（避免泄露）
    const newUrl = window.location.pathname + window.location.hash;
    window.history.replaceState({}, '', newUrl);
    
    return true;
  }
  
  return false;
}

/**
 * 检查当前用户是否已认证
 */
export function isAuthenticated(): boolean {
  return isTokenValid();
}

/**
 * 登录（文档站点自己的登录系统）
 */
export function loginWithCredentials(username: string, password: string): boolean {
  // 简单的固定凭证验证
  // 生产环境中应该使用更安全的验证方式
  const validCredentials = {
    username: 'admin',
    password: 'lawfirm2026'
  };

  if (username === validCredentials.username && password === validCredentials.password) {
    // 生成一个简单的token（实际应用中应该使用更安全的生成方式）
    const token = `docs_${Date.now()}_${Math.random().toString(36).substr(2)}`;
    storeToken(token);
    return true;
  }
  
  return false;
}

/**
 * 登出
 */
export function logout(): void {
  clearToken();
  // 刷新页面以清除状态
  if (typeof window !== 'undefined') {
    window.location.href = '/';
  }
}
