/**
 * 文档站点认证工具
 * 支持两种认证方式：
 * 1. 通过前端传递的JWT token认证（从前端应用跳转时传递）
 * 2. 通过文档站点自己的登录界面认证（固定用户名密码）
 */

const TOKEN_KEY = 'docs_auth_token';
const TOKEN_EXPIRY_KEY = 'docs_auth_token_expiry';
const ROLE_KEY = 'docs_auth_role';
const DEFAULT_EXPIRY_HOURS = 24; // token默认有效期24小时

function canUseStorage(): boolean {
  return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
}

/**
 * 存储token和过期时间
 */
export function storeToken(token: string, expiryHours: number = DEFAULT_EXPIRY_HOURS): void {
  if (!canUseStorage()) {
    return;
  }
  const expiryTime = Date.now() + expiryHours * 60 * 60 * 1000;
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(TOKEN_EXPIRY_KEY, expiryTime.toString());
}

export function storeRole(role: string): void {
  if (!canUseStorage()) {
    return;
  }
  localStorage.setItem(ROLE_KEY, role);
}

/**
 * 清除token
 */
export function clearToken(): void {
  if (!canUseStorage()) {
    return;
  }
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
  localStorage.removeItem(ROLE_KEY);
}

/**
 * 获取当前存储的token
 */
export function getStoredToken(): string | null {
  if (!canUseStorage()) {
    return null;
  }
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredRole(): string | null {
  if (!canUseStorage()) {
    return null;
  }
  return localStorage.getItem(ROLE_KEY);
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padLength = (4 - (normalized.length % 4)) % 4;
  const padded = normalized + '='.repeat(padLength);
  return decodeURIComponent(
    atob(padded)
      .split('')
      .map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join(''),
  );
}

function parseJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const payloadStr = decodeBase64Url(parts[1]!);
    const payload = JSON.parse(payloadStr);
    if (payload && typeof payload === 'object') {
      return payload as Record<string, unknown>;
    }
    return null;
  } catch {
    return null;
  }
}

function inferRoleFromToken(token: string): 'admin' | 'user' {
  const payload = parseJwtPayload(token);
  if (!payload) {
    return 'user';
  }

  if (payload.isAdmin === true) {
    return 'admin';
  }

  const role = payload.role;
  if (typeof role === 'string' && role.toLowerCase() === 'admin') {
    return 'admin';
  }

  const roles = payload.roles;
  if (Array.isArray(roles)) {
    const normalized = roles
      .filter((r) => typeof r === 'string')
      .map((r) => (r as string).toLowerCase());
    if (
      normalized.includes('admin') ||
      normalized.includes('administrator') ||
      normalized.includes('superadmin') ||
      normalized.includes('super_admin') ||
      normalized.includes('root')
    ) {
      return 'admin';
    }
  }

  const permissions = payload.permissions;
  if (Array.isArray(permissions)) {
    const normalized = permissions
      .filter((p) => typeof p === 'string')
      .map((p) => (p as string).toLowerCase());
    if (normalized.includes('*') || normalized.includes('all') || normalized.includes('admin')) {
      return 'admin';
    }
  }

  return 'user';
}

/**
 * 检查token是否有效（存在且未过期）
 */
export function isTokenValid(): boolean {
  if (!canUseStorage()) {
    return false;
  }
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
    const role = inferRoleFromToken(token);
    storeToken(token);
    storeRole(role);
    
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

export function isAdminAuthenticated(): boolean {
  return isTokenValid() && getStoredRole() === 'admin';
}

/**
 * 登录（文档站点自己的登录系统）
 * 
 * 密码配置方式：
 * 1. 在 frontend/docs/.env 文件中设置 VITE_DOCS_PASSWORD=你的密码
 * 2. 或在部署时通过环境变量设置
 * 3. 默认密码仅用于开发环境
 */
export function loginWithCredentials(username: string, password: string): boolean {
  // 从环境变量获取凭证，未设置时使用默认值（仅开发环境）
  const validCredentials = {
    username: (import.meta as any).env?.VITE_DOCS_USERNAME || 'admin',
    password: (import.meta as any).env?.VITE_DOCS_PASSWORD || 'lawfirm@2026'
  };

  if (username === validCredentials.username && password === validCredentials.password) {
    // 生成一个简单的token（实际应用中应该使用更安全的生成方式）
    const token = `docs_${Date.now()}_${Math.random().toString(36).substr(2)}`;
    storeToken(token);
    storeRole('admin');
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
    window.location.href = window.location.pathname.startsWith('/docs/') ? '/docs/' : '/';
  }
}
