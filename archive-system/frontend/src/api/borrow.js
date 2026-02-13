import request from './index'

/**
 * 提交借阅申请
 */
export function applyBorrow(data) {
  return request({
    url: '/borrows/apply',
    method: 'post',
    data
  })
}

/**
 * 获取借阅申请详情
 */
export function getBorrowDetail(id) {
  return request({
    url: `/borrows/${id}`,
    method: 'get'
  })
}

/**
 * 获取我的申请列表
 */
export function getMyBorrows(params) {
  return request({
    url: '/borrows/my',
    method: 'get',
    params
  })
}

/**
 * 取消申请
 */
export function cancelBorrow(id) {
  return request({
    url: `/borrows/${id}/cancel`,
    method: 'put'
  })
}

/**
 * 获取待审批列表
 */
export function getPendingBorrows(params) {
  return request({
    url: '/borrows/pending',
    method: 'get',
    params
  })
}

/**
 * 审批通过
 */
export function approveBorrow(id, remarks) {
  return request({
    url: `/borrows/${id}/approve`,
    method: 'put',
    params: { remarks }
  })
}

/**
 * 审批拒绝
 */
export function rejectBorrow(id, reason) {
  return request({
    url: `/borrows/${id}/reject`,
    method: 'put',
    params: { reason }
  })
}

/**
 * 借出档案
 */
export function lendArchive(id) {
  return request({
    url: `/borrows/${id}/lend`,
    method: 'put'
  })
}

/**
 * 归还档案
 */
export function returnArchive(id, remarks) {
  return request({
    url: `/borrows/${id}/return`,
    method: 'put',
    params: { remarks }
  })
}

/**
 * 续借
 */
export function renewBorrow(id, newReturnDate) {
  return request({
    url: `/borrows/${id}/renew`,
    method: 'put',
    params: { newReturnDate }
  })
}

/**
 * 获取逾期列表
 */
export function getOverdueBorrows() {
  return request({
    url: '/borrows/overdue',
    method: 'get'
  })
}

/**
 * 检查档案是否可借阅
 */
export function checkBorrowAvailable(archiveId) {
  return request({
    url: `/borrows/check/${archiveId}`,
    method: 'get'
  })
}
