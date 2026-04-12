import request from './index'

/**
 * 获取概览统计
 */
export function getOverview() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}

/**
 * 按档案类型统计
 */
export function countByType() {
  return request({
    url: '/statistics/by-type',
    method: 'get'
  })
}

/**
 * 按保管期限统计
 */
export function countByRetention() {
  return request({
    url: '/statistics/by-retention',
    method: 'get'
  })
}

/**
 * 按状态统计
 */
export function countByStatus() {
  return request({
    url: '/statistics/by-status',
    method: 'get'
  })
}

/**
 * 月度趋势统计
 */
export function getTrend(year) {
  return request({
    url: '/statistics/trend',
    method: 'get',
    params: { year }
  })
}

/**
 * 借阅统计
 */
export function getBorrowStats() {
  return request({
    url: '/statistics/borrow',
    method: 'get'
  })
}

/**
 * 存储统计
 */
export function getStorageStats() {
  return request({
    url: '/statistics/storage',
    method: 'get'
  })
}

/**
 * 扫描批次统计
 */
export function getScanBatchStats(keyword) {
  return request({
    url: '/statistics/scan-batches',
    method: 'get',
    params: { keyword }
  })
}
