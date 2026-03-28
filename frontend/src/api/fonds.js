import request from './index'

/**
 * 全宗管理 API
 */

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
 * 分页查询全宗
 */
export function getFondsPage(params) {
  return request({
    url: '/fonds/page',
    method: 'get',
    params
  })
}

/**
 * 获取全宗详情
 */
export function getFondsDetail(id) {
  return request({
    url: `/fonds/${id}`,
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

/**
 * 获取全宗统计
 */
export function getFondsStatistics(id) {
  return request({
    url: `/fonds/${id}/statistics`,
    method: 'get'
  })
}
