import request from './index'

// ========== 用户管理 ==========

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

/**
 * 创建用户
 */
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户
 */
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 更新当前用户个人资料
 */
export function updateCurrentUser(data) {
  return request({
    url: '/users/current',
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

/**
 * 重置密码
 */
export function resetPassword(id, newPassword) {
  return request({
    url: `/users/${id}/reset-password`,
    method: 'put',
    data: { newPassword }
  })
}

/**
 * 修改密码
 */
export function changePassword(oldPassword, newPassword) {
  return request({
    url: '/users/change-password',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

/**
 * 分配角色
 */
export function assignRoles(id, roleIds) {
  return request({
    url: `/users/${id}/roles`,
    method: 'put',
    data: { roleIds }
  })
}

/**
 * 获取用户角色
 */
export function getUserRoles(id) {
  return request({
    url: `/users/${id}/roles`,
    method: 'get'
  })
}

/**
 * 获取账号锁定状态
 */
export function getUserLockStatus(id) {
  return request({
    url: `/users/${id}/lock-status`,
    method: 'get'
  })
}

/**
 * 解锁账号
 */
export function unlockUser(id) {
  return request({
    url: `/users/${id}/unlock`,
    method: 'put'
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: '/users/current',
    method: 'get'
  })
}

// ========== 角色管理（已移至 role.js）==========
