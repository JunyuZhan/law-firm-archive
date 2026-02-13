import request from '@/utils/request'

/**
 * 查询操作日志
 */
export function getOperationLogs(params) {
  return request({
    url: '/api/operation-logs',
    method: 'get',
    params
  })
}

/**
 * 根据档案ID查询日志
 */
export function getLogsByArchive(archiveId) {
  return request({
    url: `/api/operation-logs/archive/${archiveId}`,
    method: 'get'
  })
}

/**
 * 根据对象查询日志
 */
export function getLogsByObject(objectType, objectId) {
  return request({
    url: `/api/operation-logs/object/${objectType}/${objectId}`,
    method: 'get'
  })
}

/**
 * 获取操作统计
 */
export function getLogStatistics(params) {
  return request({
    url: '/api/operation-logs/statistics',
    method: 'get',
    params
  })
}

/**
 * 导出日志
 */
export function exportLogs(params) {
  return request({
    url: '/api/operation-logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
