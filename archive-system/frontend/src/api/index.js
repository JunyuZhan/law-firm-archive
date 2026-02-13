import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// Token刷新状态
let isRefreshing = false
let refreshSubscribers = []

// 添加刷新订阅者
const subscribeTokenRefresh = (callback) => {
  refreshSubscribers.push(callback)
}

// 通知所有订阅者
const onRefreshed = (token) => {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.success === false) {
      // 业务错误
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async error => {
    const originalRequest = error.config
    
    // 401 未授权 - 尝试刷新Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新中，等待刷新完成
        return new Promise(resolve => {
          subscribeTokenRefresh(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      const refreshToken = sessionStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post('/api/auth/refresh', refreshToken, {
            headers: { 'Content-Type': 'text/plain' }
          })
          
          if (res.data.success) {
            const { accessToken } = res.data.data
            localStorage.setItem('accessToken', accessToken)
            onRefreshed(accessToken)
            originalRequest.headers.Authorization = `Bearer ${accessToken}`
            return request(originalRequest)
          }
        } catch (refreshError) {
          console.error('Token刷新失败', refreshError)
        } finally {
          isRefreshing = false
        }
      }
      
      // 刷新失败，清除登录状态并跳转登录页
      localStorage.removeItem('accessToken')
      sessionStorage.removeItem('refreshToken')
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
      return Promise.reject(error)
    }
    
    // 403 禁止访问
    if (error.response?.status === 403) {
      ElMessage.error('无权限访问')
      return Promise.reject(error)
    }
    
    // 其他错误
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
