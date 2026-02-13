import request from './index'

/**
 * 申请销毁
 */
export function applyDestruction(data) {
  return request({
    url: '/destructions/apply',
    method: 'post',
    data
  })
}

/**
 * 批量申请销毁
 */
export function batchApplyDestruction(data) {
  return request({
    url: '/destructions/batch-apply',
    method: 'post',
    data
  })
}

/**
 * 获取销毁记录详情
 */
export function getDestructionDetail(id) {
  return request({
    url: `/destructions/${id}`,
    method: 'get'
  })
}

/**
 * 获取销毁记录列表
 */
export function getDestructionList(params) {
  return request({
    url: '/destructions',
    method: 'get',
    params
  })
}

/**
 * 获取待审批列表
 */
export function getPendingDestructions(params) {
  return request({
    url: '/destructions/pending',
    method: 'get',
    params
  })
}

/**
 * 获取待执行列表
 */
export function getApprovedDestructions(params) {
  return request({
    url: '/destructions/approved',
    method: 'get',
    params
  })
}

/**
 * 获取档案的销毁记录
 */
export function getArchiveDestructions(archiveId) {
  return request({
    url: `/destructions/archive/${archiveId}`,
    method: 'get'
  })
}

/**
 * 审批通过
 */
export function approveDestruction(id, comment) {
  return request({
    url: `/destructions/${id}/approve`,
    method: 'put',
    params: { comment }
  })
}

/**
 * 审批拒绝
 */
export function rejectDestruction(id, comment) {
  return request({
    url: `/destructions/${id}/reject`,
    method: 'put',
    params: { comment }
  })
}

/**
 * 执行销毁
 */
export function executeDestruction(id, remarks) {
  return request({
    url: `/destructions/${id}/execute`,
    method: 'put',
    params: { remarks }
  })
}

/**
 * 批量执行销毁
 */
export function batchExecuteDestruction(data) {
  return request({
    url: '/destructions/batch-execute',
    method: 'put',
    data
  })
}
