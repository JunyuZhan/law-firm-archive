import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { secureStorage, sanitizeInput } from '@/utils/security'

// 创建axios实例
const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const request = axios.create({
  baseURL: apiBaseURL,
  timeout: 30000,
  // 安全配置
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

// Token刷新状态
let isRefreshing = false
let refreshSubscribers = []

// 添加刷新订阅者
const subscribeTokenRefresh = (resolve, reject) => {
  refreshSubscribers.push({ resolve, reject })
}

// 通知所有订阅者
const onRefreshed = (token) => {
  refreshSubscribers.forEach(({ resolve }) => resolve(token))
  refreshSubscribers = []
}

// 刷新失败时拒绝所有排队请求，避免请求永久挂起
const onRefreshFailed = (error) => {
  refreshSubscribers.forEach(({ reject }) => reject(error))
  refreshSubscribers = []
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 使用安全存储获取Token
    const token = secureStorage.getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 刷新用户活动时间（防止空闲超时）
    secureStorage.refreshActivity()
    
    // 对发送的字符串数据进行安全检查（可选）
    // 注意：这里不对所有数据进行sanitize，因为可能影响正常业务
    // 只是添加一个警告日志
    if (config.data && typeof config.data === 'object') {
      checkRequestData(config.data)
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 检查请求数据是否包含可疑内容
function checkRequestData(data, path = '') {
  if (!data) return
  
  for (const [key, value] of Object.entries(data)) {
    const currentPath = path ? `${path}.${key}` : key
    if (typeof value === 'string' && value.length > 0) {
      // 检测潜在的XSS攻击
      if (/<script|javascript:|on\w+\s*=/i.test(value)) {
        console.warn(`[Security] 检测到可疑内容: ${currentPath}`)
      }
    } else if (typeof value === 'object' && value !== null) {
      checkRequestData(value, currentPath)
    }
  }
}

async function unwrapBlobBusinessError(blob) {
  if (!(blob instanceof Blob)) {
    return null
  }

  const contentType = blob.type || ''
  if (!contentType.includes('application/json')) {
    return null
  }

  try {
    return JSON.parse(await blob.text())
  } catch {
    return null
  }
}

async function extractBlobErrorMessage(blob) {
  const blobError = await unwrapBlobBusinessError(blob)
  if (!blobError) {
    return null
  }

  return sanitizeInput(blobError.message) || null
}

// 响应拦截器
request.interceptors.response.use(
  async response => {
    const res = response.data
    if (response.config?.responseType === 'blob') {
      const blobError = await unwrapBlobBusinessError(res)
      if (blobError?.success === false) {
        const message = sanitizeInput(blobError.message) || '请求失败'
        ElMessage.error(message)
        return Promise.reject(new Error(message))
      }
      return res
    }

    if (res.success === false) {
      // 业务错误
      ElMessage.error(sanitizeInput(res.message) || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async error => {
    const originalRequest = error.config
    const blobErrorMessage = await extractBlobErrorMessage(error.response?.data)
    
    // 429 请求过于频繁
    if (error.response?.status === 429) {
      ElMessage.warning(blobErrorMessage || error.response.data?.message || '请求过于频繁，请稍后重试')
      return Promise.reject(error)
    }
    
    // 401 一律视为会话可能失效；403 仅在令牌缺失/刚过期场景下兜底续期一次
    const status = error.response?.status
    const activeAccessToken = secureStorage.getAccessToken()
    const currentRefreshToken = secureStorage.getRefreshToken()
    const hadBearer = !!(originalRequest?.headers?.Authorization)
    const canTryRefresh = (
      status === 401 ||
      (status === 403 && (!activeAccessToken || hadBearer))
    ) && !!currentRefreshToken && !originalRequest._retry

    if (canTryRefresh) {
      if (isRefreshing) {
        // 正在刷新中，等待刷新完成
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh(token => {
            originalRequest.headers = originalRequest.headers || {}
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          }, reject)
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      if (currentRefreshToken) {
        try {
          const res = await axios.post(`${apiBaseURL}/auth/refresh`, currentRefreshToken, {
            headers: { 'Content-Type': 'text/plain' }
          })
          
          if (res.data.success) {
            const { accessToken, refreshToken: newRefreshToken } = res.data.data
            secureStorage.setAccessToken(accessToken)
            if (newRefreshToken) {
              secureStorage.setRefreshToken(newRefreshToken)
            }
            onRefreshed(accessToken)
            originalRequest.headers = originalRequest.headers || {}
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            return request(originalRequest)
          }

          onRefreshFailed(new Error(res.data?.message || '登录已过期，请重新登录'))
        } catch (refreshError) {
          console.error('Token刷新失败', refreshError)
          onRefreshFailed(refreshError)
        } finally {
          isRefreshing = false
        }
      }
      
      // 刷新失败，清除登录状态并跳转登录页
      secureStorage.clearTokens()
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }
    
    // 403 禁止访问（未走上方刷新分支时，视为真实无权限）
    if (error.response?.status === 403) {
      ElMessage.error(blobErrorMessage || '无权限访问')
      return Promise.reject(error)
    }
    
    // 其他错误
    const message = blobErrorMessage || sanitizeInput(error.response?.data?.message) || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
