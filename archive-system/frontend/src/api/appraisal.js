import request from './index'

/**
 * 发起鉴定
 */
export function createAppraisal(data) {
  return request({
    url: '/appraisals',
    method: 'post',
    data
  })
}

/**
 * 获取鉴定详情
 */
export function getAppraisalDetail(id) {
  return request({
    url: `/appraisals/${id}`,
    method: 'get'
  })
}

/**
 * 获取鉴定列表
 */
export function getAppraisalList(params) {
  return request({
    url: '/appraisals',
    method: 'get',
    params
  })
}

/**
 * 获取待审批列表
 */
export function getPendingAppraisals(params) {
  return request({
    url: '/appraisals/pending',
    method: 'get',
    params
  })
}

/**
 * 获取档案的鉴定历史
 */
export function getArchiveAppraisals(archiveId) {
  return request({
    url: `/appraisals/archive/${archiveId}`,
    method: 'get'
  })
}

/**
 * 审批通过
 */
export function approveAppraisal(id, comment) {
  return request({
    url: `/appraisals/${id}/approve`,
    method: 'put',
    params: { comment }
  })
}

/**
 * 审批拒绝
 * @param {number} id - 鉴定记录ID
 * @param {object} data - 拒绝原因对象 { comment: string }
 */
export function rejectAppraisal(id, data) {
  return request({
    url: `/appraisals/${id}/reject`,
    method: 'put',
    data
  })
}

/**
 * 获取即将到期档案
 */
export function getExpiringArchives(days = 90) {
  return request({
    url: '/retention/expiring',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取已到期档案
 */
export function getExpiredArchives() {
  return request({
    url: '/retention/expired',
    method: 'get'
  })
}

/**
 * 延长保管期限
 */
export function extendRetention(archiveId, data) {
  return request({
    url: `/retention/${archiveId}/extend`,
    method: 'put',
    data
  })
}
