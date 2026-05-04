/**
 * 前端安全工具类
 * 提供XSS防护、安全存储等功能
 */

// ===== XSS防护 =====

/**
 * HTML转义，防止XSS攻击
 * @param {string} str - 需要转义的字符串
 * @returns {string} 转义后的安全字符串
 */
export function escapeHtml(str) {
  if (!str) return ''
  const escapeMap = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;',
    '`': '&#x60;',
    '=': '&#x3D;'
  }
  return String(str).replace(/[&<>"'`=/]/g, char => escapeMap[char])
}

/**
 * 转义正则表达式特殊字符，防止正则注入
 * @param {string} str - 需要转义的字符串
 * @returns {string} 转义后的安全字符串
 */
export function escapeRegExp(str) {
  if (!str) return ''
  return String(str).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 检测字符串是否包含XSS攻击代码
 * @param {string} str - 需要检测的字符串
 * @returns {boolean} 是否包含危险内容
 */
export function containsXss(str) {
  if (!str) return false
  const xssPatterns = [
    /<script\b[^>]*>[\s\S]*?<\/script>/gi,
    /javascript:/gi,
    /on\w+\s*=/gi,
    /<iframe\b[^>]*>/gi,
    /<object\b[^>]*>/gi,
    /<embed\b[^>]*>/gi,
    /eval\s*\(/gi,
    /expression\s*\(/gi
  ]
  return xssPatterns.some(pattern => pattern.test(str))
}

/**
 * 清理可能包含XSS的字符串
 * @param {string} str - 需要清理的字符串
 * @returns {string} 清理后的字符串
 */
export function sanitizeInput(str) {
  if (!str) return ''
  return String(str)
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]+on\w+\s*=\s*["'][^"']*["'][^>]*>/gi, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
}

// ===== 安全存储 =====

/**
 * 安全的Token存储
 * 使用sessionStorage替代localStorage以减少XSS风险
 * 页面关闭时自动清除
 */
export const secureStorage = {
  // 内存中的敏感数据（最安全，但刷新会丢失）
  _memoryStore: new Map(),
  
  /**
   * 存储访问令牌
   * 注意：为了在刷新后保持登录状态，仍使用localStorage
   * 但会在空闲时自动清除
   */
  setAccessToken(token) {
    if (!token) return
    localStorage.setItem('accessToken', token)
    this._setTokenExpiry()
  },
  
  /**
   * 获取访问令牌
   */
  getAccessToken() {
    const token = localStorage.getItem('accessToken')
    if (token) {
      if (this._isTokenExpired()) {
        this.clearAccessToken()
        return null
      }

      const payload = parseJwtPayload(token)
      if (payload?.exp && Date.now() >= Number(payload.exp) * 1000) {
        this.clearAccessToken()
        return null
      }
    }
    return token
  },
  
  /**
   * 存储刷新令牌（使用sessionStorage，关闭浏览器即失效）
   */
  setRefreshToken(token) {
    if (!token) return
    sessionStorage.setItem('refreshToken', token)
  },
  
  /**
   * 获取刷新令牌
   */
  getRefreshToken() {
    return sessionStorage.getItem('refreshToken')
  },
  
  /**
   * 清除所有Token
   */
  clearTokens() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('tokenExpiry')
    sessionStorage.removeItem('refreshToken')
    this._memoryStore.clear()
  },

  /**
   * 仅清除访问令牌，保留刷新令牌用于静默续期
   */
  clearAccessToken() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('tokenExpiry')
  },
  
  /**
   * 设置Token过期时间（用于空闲超时）
   */
  _setTokenExpiry() {
    // 30分钟无操作自动过期
    const expiry = Date.now() + 30 * 60 * 1000
    localStorage.setItem('tokenExpiry', String(expiry))
  },
  
  /**
   * 检查Token是否已过期
   */
  _isTokenExpired() {
    const expiry = localStorage.getItem('tokenExpiry')
    if (!expiry) return false
    return Date.now() > parseInt(expiry, 10)
  },
  
  /**
   * 刷新Token活动时间（在用户操作时调用）
   */
  refreshActivity() {
    if (this.getAccessToken()) {
      this._setTokenExpiry()
    }
  },
  
  /**
   * 存储敏感数据到内存（最安全）
   */
  setSecure(key, value) {
    this._memoryStore.set(key, value)
  },
  
  /**
   * 获取敏感数据
   */
  getSecure(key) {
    return this._memoryStore.get(key)
  },
  
  /**
   * 删除敏感数据
   */
  removeSecure(key) {
    this._memoryStore.delete(key)
  }
}

/**
 * 解析 JWT 载荷（仅用于前端展示与路由兜底，不作为安全校验依据）
 * @param {string} token - JWT Token
 * @returns {object|null} 解析后的 payload
 */
export function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const segments = token.split('.')
  if (segments.length < 2) return null

  try {
    const base64 = segments[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), '=')
    const json = decodeURIComponent(
      atob(padded)
        .split('')
        .map(char => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

// ===== URL安全 =====

/**
 * 验证URL是否安全
 * @param {string} url - 需要验证的URL
 * @returns {boolean} 是否安全
 */
export function isValidUrl(url) {
  if (!url) return false
  try {
    const parsed = new URL(url, window.location.origin)
    // 只允许http和https协议
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return false
    }
    // 防止JavaScript协议绕过
    if (url.toLowerCase().includes('javascript:')) {
      return false
    }
    return true
  } catch {
    return false
  }
}

/**
 * 安全的重定向
 * @param {string} url - 目标URL
 */
export function safeRedirect(url) {
  if (!isValidUrl(url)) {
    console.warn('尝试重定向到不安全的URL:', url)
    return
  }
  // 使用replace而不是assign，防止在历史记录中留下重定向来源
  window.location.replace(url)
}

// ===== CSRF防护 =====

/**
 * 生成CSRF Token
 * @returns {string} CSRF Token
 */
export function generateCsrfToken() {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('')
}

// ===== 密码安全 =====

/**
 * 验证密码强度
 * @param {string} password - 密码
 * @returns {object} 验证结果
 */
export function validatePasswordStrength(password) {
  const result = {
    isValid: false,
    score: 0,
    messages: []
  }
  
  if (!password) {
    result.messages.push('密码不能为空')
    return result
  }
  
  // 长度检查
  if (password.length < 8) {
    result.messages.push('密码长度至少8位')
  } else {
    result.score += 1
  }
  
  // 大写字母
  if (/[A-Z]/.test(password)) {
    result.score += 1
  } else {
    result.messages.push('建议包含大写字母')
  }
  
  // 小写字母
  if (/[a-z]/.test(password)) {
    result.score += 1
  } else {
    result.messages.push('建议包含小写字母')
  }
  
  // 数字
  if (/\d/.test(password)) {
    result.score += 1
  } else {
    result.messages.push('建议包含数字')
  }
  
  // 特殊字符
  if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
    result.score += 1
  } else {
    result.messages.push('建议包含特殊字符')
  }
  
  result.isValid = result.score >= 3 && password.length >= 8
  
  return result
}

// ===== 自动清理 =====

/**
 * 设置空闲超时监听器
 */
export function setupIdleTimeout(timeoutMinutes = 30, onTimeout) {
  let idleTimer
  
  const resetTimer = () => {
    clearTimeout(idleTimer)
    secureStorage.refreshActivity()
    idleTimer = setTimeout(() => {
      if (onTimeout && typeof onTimeout === 'function') {
        onTimeout()
      }
    }, timeoutMinutes * 60 * 1000)
  }
  
  // 监听用户活动
  const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click']
  events.forEach(event => {
    document.addEventListener(event, resetTimer, { passive: true })
  })
  
  resetTimer()
  
  // 返回清理函数
  return () => {
    clearTimeout(idleTimer)
    events.forEach(event => {
      document.removeEventListener(event, resetTimer)
    })
  }
}

// ===== 初始化 =====

/**
 * 初始化安全模块
 */
export function initSecurity() {
  // 禁用右键菜单（可选，防止源码泄露）
  // document.addEventListener('contextmenu', e => e.preventDefault())
  
  // 监听存储变化，防止XSS窃取Token
  window.addEventListener('storage', (e) => {
    if (e.key === 'accessToken' && e.oldValue && !e.newValue) {
      // accessToken 被其他标签页清除时，优先保留本标签页的 refresh token 续期机会
      if (!secureStorage.getRefreshToken()) {
        window.location.href = '/login'
      }
    }
  })
  
  // 页面卸载时的清理
  window.addEventListener('beforeunload', () => {
    // 如果用户没有勾选"记住我"，清除Token
    // 实际实现可根据需求调整
  })
  
}

export default {
  escapeHtml,
  escapeRegExp,
  containsXss,
  sanitizeInput,
  secureStorage,
  isValidUrl,
  safeRedirect,
  generateCsrfToken,
  validatePasswordStrength,
  setupIdleTimeout,
  initSecurity
}
