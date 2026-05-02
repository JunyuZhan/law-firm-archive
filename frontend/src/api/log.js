import request from './index'

/**
 * 查询操作日志
 */
export function getOperationLogs(params) {
  return request({
    url: '/operation-logs',
    method: 'get',
    params
  })
}

/**
 * 获取操作日志详情
 */
export function getOperationLogDetail(id) {
  return request({
    url: `/operation-logs/${id}`,
    method: 'get'
  })
}

/**
 * 根据档案ID查询日志
 */
export function getLogsByArchive(archiveId) {
  return request({
    url: `/operation-logs/archive/${archiveId}`,
    method: 'get'
  })
}

/**
 * 根据对象查询日志
 */
export function getLogsByObject(objectType, objectId) {
  return request({
    url: `/operation-logs/object/${objectType}/${objectId}`,
    method: 'get'
  })
}

/**
 * 获取操作统计
 */
export function getLogStatistics(params) {
  return request({
    url: '/operation-logs/statistics',
    method: 'get',
    params
  })
}

/**
 * 导出日志
 */
export function exportLogs(params) {
  return request({
    url: '/operation-logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
