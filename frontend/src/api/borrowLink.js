import request from './index'
import axios from 'axios'

const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const publicRequest = axios.create({
  baseURL: apiBaseURL,
  timeout: 30000,
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN'
})

/**
 * 公开访问档案（无需认证）
 */
export function accessArchive(token) {
  return publicRequest.get(`/open/borrow/access/${token}`)
    .then(res => res.data)
}

/**
 * 记录下载（无需认证）
 */
export function recordDownload(token, fileId) {
  return publicRequest.post(`/open/borrow/access/${token}/download/${fileId}`)
    .then(res => res.data)
}

/**
 * 获取公开借阅文件预览链接
 */
export function getBorrowFilePreviewUrl(token, fileId) {
  return publicRequest.get(`/open/borrow/access/${token}/preview/${fileId}`)
    .then(res => res.data)
}

/**
 * 获取公开借阅文件下载链接
 */
export function getBorrowFileDownloadUrl(token, fileId) {
  return publicRequest.get(`/open/borrow/access/${token}/download-url/${fileId}`)
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
