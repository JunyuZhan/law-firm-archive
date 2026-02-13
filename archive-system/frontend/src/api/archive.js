import request from './index'

/**
 * 查询档案列表
 */
export function getArchiveList(params) {
  return request({
    url: '/archives',
    method: 'get',
    params
  })
}

/**
 * 获取档案详情
 */
export function getArchiveDetail(id) {
  return request({
    url: `/archives/${id}`,
    method: 'get'
  })
}

/**
 * 根据档案号获取详情
 */
export function getArchiveByNo(archiveNo) {
  return request({
    url: `/archives/no/${archiveNo}`,
    method: 'get'
  })
}

/**
 * 创建档案
 */
export function createArchive(data) {
  return request({
    url: '/archives',
    method: 'post',
    data
  })
}

/**
 * 更新档案
 */
export function updateArchive(id, data) {
  return request({
    url: `/archives/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除档案
 */
export function deleteArchive(id) {
  return request({
    url: `/archives/${id}`,
    method: 'delete'
  })
}

/**
 * 更新档案状态
 */
export function updateArchiveStatus(id, status) {
  return request({
    url: `/archives/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 上传文件
 */
export function uploadFile(file, archiveId, fileCategory, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  if (fileCategory) {
    formData.append('fileCategory', fileCategory)
  }

  const url = archiveId ? `/archives/${archiveId}/files` : '/files/upload'

  return request({
    url,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      if (onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

/**
 * 批量上传文件
 */
export function uploadFiles(files, fileCategory) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (fileCategory) {
    formData.append('fileCategory', fileCategory)
  }

  return request({
    url: '/files/upload/batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取文件下载链接
 */
export function getFileDownloadUrl(fileId) {
  return request({
    url: `/archives/files/${fileId}/download-url`,
    method: 'get'
  })
}

/**
 * 获取文件预览链接
 */
export function getFilePreviewUrl(fileId) {
  return request({
    url: `/archives/files/${fileId}/preview-url`,
    method: 'get'
  })
}

/**
 * 删除文件
 */
export function deleteFile(fileId) {
  return request({
    url: `/archives/files/${fileId}`,
    method: 'delete'
  })
}
