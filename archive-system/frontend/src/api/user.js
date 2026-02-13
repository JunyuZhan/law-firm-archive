import request from '@/utils/request'

// ========== 用户管理 ==========

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request({
    url: '/api/users',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export function getUserDetail(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'get'
  })
}

/**
 * 创建用户
 */
export function createUser(data) {
  return request({
    url: '/api/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户
 */
export function updateUser(id, data) {
  return request({
    url: `/api/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUser(id) {
  return request({
    url: `/api/users/${id}`,
    method: 'delete'
  })
}

/**
 * 重置密码
 */
export function resetPassword(id, newPassword) {
  return request({
    url: `/api/users/${id}/reset-password`,
    method: 'put',
    data: { newPassword }
  })
}

/**
 * 修改密码
 */
export function changePassword(oldPassword, newPassword) {
  return request({
    url: '/api/users/change-password',
    method: 'put',
    data: { oldPassword, newPassword }
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/api/users/${id}/status`,
    method: 'put',
    data: { status }
  })
}

/**
 * 分配角色
 */
export function assignRoles(id, roleIds) {
  return request({
    url: `/api/users/${id}/roles`,
    method: 'put',
    data: { roleIds }
  })
}

/**
 * 获取用户角色
 */
export function getUserRoles(id) {
  return request({
    url: `/api/users/${id}/roles`,
    method: 'get'
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: '/api/users/current',
    method: 'get'
  })
}

// ========== 角色管理 ==========

/**
 * 获取角色列表
 */
export function getRoleList() {
  return request({
    url: '/api/roles',
    method: 'get'
  })
}

/**
 * 获取角色详情
 */
export function getRoleDetail(id) {
  return request({
    url: `/api/roles/${id}`,
    method: 'get'
  })
}

/**
 * 创建角色
 */
export function createRole(data) {
  return request({
    url: '/api/roles',
    method: 'post',
    data
  })
}

/**
 * 更新角色
 */
export function updateRole(id, data) {
  return request({
    url: `/api/roles/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除角色
 */
export function deleteRole(id) {
  return request({
    url: `/api/roles/${id}`,
    method: 'delete'
  })
}
