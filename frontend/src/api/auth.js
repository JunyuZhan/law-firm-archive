import request from './index'

/**
 * 用户登录
 * @param {Object} data - { username, password }
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 刷新Token
 * @param {string} refreshToken
 */
export function refreshToken(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: refreshToken,
    headers: { 'Content-Type': 'text/plain' }
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取系统初始化状态
 */
export function getBootstrapStatus() {
  return request({
    url: '/auth/bootstrap/status',
    method: 'get'
  })
}

/**
 * 首次初始化 admin 密码
 */
export function initializeBootstrap(password) {
  return request({
    url: '/auth/bootstrap/initialize',
    method: 'post',
    data: { password }
  })
}
