import request from './index'
import axios from 'axios'

/**
 * 公开访问档案（无需认证）
 */
export function accessArchive(token) {
  return axios.get(`/api/open/borrow/access/${token}`)
    .then(res => res.data)
}

/**
 * 记录下载（无需认证）
 */
export function recordDownload(token, fileId) {
  return axios.post(`/api/open/borrow/access/${token}/download/${fileId}`)
    .then(res => res.data)
}

/**
 * 获取借阅链接列表（管理接口，需认证）
 */
export function getBorrowLinks(params) {
  return request({
    url: '/borrow-links',
    method: 'get',
    params
  })
}

/**
 * 获取链接详情
 */
export function getBorrowLinkDetail(id) {
  return request({
    url: `/borrow-links/${id}`,
    method: 'get'
  })
}

/**
 * 获取档案的有效链接
 */
export function getActiveLinksForArchive(archiveId) {
  return request({
    url: `/borrow-links/archive/${archiveId}`,
    method: 'get'
  })
}

/**
 * 为借阅申请生成链接
 */
export function generateLink(borrowId, expireDays = 7, allowDownload = true) {
  return request({
    url: '/borrow-links/generate',
    method: 'post',
    params: { borrowId, expireDays, allowDownload }
  })
}

/**
 * 撤销链接
 */
export function revokeLink(id, reason) {
  return request({
    url: `/borrow-links/${id}/revoke`,
    method: 'post',
    params: { reason }
  })
}

/**
 * 获取链接统计
 */
export function getLinkStats() {
  return request({
    url: '/borrow-links/stats',
    method: 'get'
  })
}

/**
 * 更新过期链接状态
 */
export function updateExpiredLinks() {
  return request({
    url: '/borrow-links/update-expired',
    method: 'post'
  })
}
