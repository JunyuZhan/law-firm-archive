import request from './index'

/**
 * 获取分类树
 */
export function getCategoryTree(archiveType) {
  return request({
    url: '/categories/tree',
    method: 'get',
    params: { archiveType }
  })
}

/**
 * 获取分类详情
 */
export function getCategoryDetail(id) {
  return request({
    url: `/categories/${id}`,
    method: 'get'
  })
}

/**
 * 创建分类
 */
export function createCategory(data) {
  return request({
    url: '/categories',
    method: 'post',
    data
  })
}

/**
 * 更新分类
 */
export function updateCategory(id, data) {
  return request({
    url: `/categories/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除分类
 */
export function deleteCategory(id) {
  return request({
    url: `/categories/${id}`,
    method: 'delete'
  })
}

/**
 * 移动分类
 */
export function moveCategory(id, newParentId) {
  return request({
    url: `/categories/${id}/move`,
    method: 'put',
    params: { newParentId }
  })
}

/**
 * 获取分类统计
 */
export function getCategoryStatistics(id) {
  return request({
    url: `/categories/${id}/statistics`,
    method: 'get'
  })
}

// ===== 全宗API =====

/**
 * 获取全宗列表
 */
export function getFondsList() {
  return request({
    url: '/fonds',
    method: 'get'
  })
}

/**
 * 创建全宗
 */
export function createFonds(data) {
  return request({
    url: '/fonds',
    method: 'post',
    data
  })
}

/**
 * 更新全宗
 */
export function updateFonds(id, data) {
  return request({
    url: `/fonds/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除全宗
 */
export function deleteFonds(id) {
  return request({
    url: `/fonds/${id}`,
    method: 'delete'
  })
}
