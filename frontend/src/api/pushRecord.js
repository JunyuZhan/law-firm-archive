import request from './index'

/**
 * 分页查询推送记录
 */
export function getPushRecordList(params) {
  return request({
    url: '/push-records',
    method: 'get',
    params
  })
}

/**
 * 获取推送记录详情
 */
export function getPushRecordDetail(id) {
  return request({
    url: `/push-records/${id}`,
    method: 'get'
  })
}

/**
 * 获取推送统计数据
 */
export function getPushRecordStatistics() {
  return request({
    url: '/push-records/statistics',
    method: 'get'
  })
}

/**
 * 获取待处理的推送记录
 */
export function getPendingPushRecords() {
  return request({
    url: '/push-records/pending',
    method: 'get'
  })
}

/**
 * 获取失败的推送记录
 */
export function getFailedPushRecords() {
  return request({
    url: '/push-records/failed',
    method: 'get'
  })
}

/**
 * 重试推送
 */
export function retryPushRecord(id) {
  return request({
    url: `/push-records/${id}/retry`,
    method: 'post'
  })
}
