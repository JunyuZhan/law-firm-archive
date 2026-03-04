import request from './index'

/**
 * 获取存放位置列表
 */
export function getLocationList(params) {
  return request({
    url: '/locations',
    method: 'get',
    params
  })
}

/**
 * 获取可用的存放位置列表
 */
export function getAvailableLocations() {
  return request({
    url: '/locations/available',
    method: 'get'
  })
}

/**
 * 获取存放位置详情
 */
export function getLocationDetail(id) {
  return request({
    url: `/locations/${id}`,
    method: 'get'
  })
}

/**
 * 创建存放位置
 */
export function createLocation(data) {
  return request({
    url: '/locations',
    method: 'post',
    data
  })
}

/**
 * 更新存放位置
 */
export function updateLocation(id, data) {
  return request({
    url: `/locations/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除存放位置
 */
export function deleteLocation(id) {
  return request({
    url: `/locations/${id}`,
    method: 'delete'
  })
}
