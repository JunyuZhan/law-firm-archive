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

export function searchArchives(params, config = {}) {
  return request({
    url: '/archives/search',
    method: 'get',
    params,
    ...config
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
 * 补充上传（补充文件和纸质档案信息）
 */
export function supplementArchive(id, data) {
  return request({
    url: `/archives/${id}/supplement`,
    method: 'post',
    data
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

export function approveArchive(id) {
  return request({
    url: `/archives/${id}/approve`,
    method: 'put'
  })
}

/**
 * 上传文件
 * @param {File} file - 文件对象
 * @param {number|null} archiveId - 档案ID（可选）
 * @param {string|null} fileCategory - 文件分类（可选）
 * @param {object|null} extraData - 额外表单参数
 * @param {function|null} onProgress - 进度回调，接收 progressEvent 对象
 */
export function uploadFile(file, archiveId, fileCategory, extraData, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  if (fileCategory) {
    formData.append('fileCategory', fileCategory)
  }
  if (extraData) {
    Object.entries(extraData).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        formData.append(key, value)
      }
    })
  }

  const url = archiveId ? `/archives/${archiveId}/files` : '/files/upload'

  return request({
    url,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: onProgress
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
 * 获取文件预览信息（包含URL、预览类型、是否转换等）
 */
export function getFilePreviewInfo(fileId) {
  return request({
    url: `/files/${fileId}/preview-info`,
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

/**
 * 获取档案打包下载链接（下载所有文件为ZIP）
 */
export function getArchiveDownloadUrl(archiveId) {
  return request({
    url: `/archives/${archiveId}/download-zip`,
    method: 'get'
  })
}

// ========== 存放位置管理 ==========
export const locationApi = {
  /**
   * 获取位置列表
   */
  list(params) {
    return request({
      url: '/locations',
      method: 'get',
      params
    })
  },

  /**
   * 获取位置详情
   */
  get(id) {
    return request({
      url: `/locations/${id}`,
      method: 'get'
    })
  },

  /**
   * 创建位置
   */
  create(data) {
    return request({
      url: '/locations',
      method: 'post',
      data
    })
  },

  /**
   * 更新位置
   */
  update(id, data) {
    return request({
      url: `/locations/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除位置
   */
  delete(id) {
    return request({
      url: `/locations/${id}`,
      method: 'delete'
    })
  },

  /**
   * 获取所有库房
   */
  getRooms() {
    return request({
      url: '/locations/rooms',
      method: 'get'
    })
  },

  /**
   * 获取可用位置
   */
  getAvailable() {
    return request({
      url: '/locations/available',
      method: 'get'
    })
  }
}

// ========== 档案来源管理 ==========
export const sourceApi = {
  /**
   * 获取来源列表
   */
  list(params) {
    return request({
      url: '/sources',
      method: 'get',
      params
    })
  },

  /**
   * 获取来源详情
   */
  get(id) {
    return request({
      url: `/sources/${id}`,
      method: 'get'
    })
  },

  /**
   * 创建来源
   */
  create(data) {
    return request({
      url: '/sources',
      method: 'post',
      data
    })
  },

  /**
   * 更新来源
   */
  update(id, data) {
    return request({
      url: `/sources/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 切换启用状态
   */
  toggle(id, enabled) {
    return request({
      url: `/sources/${id}/toggle`,
      method: 'put',
      params: { enabled }
    })
  },

  /**
   * 测试连接
   */
  test(id) {
    return request({
      url: `/sources/${id}/test`,
      method: 'post'
    })
  },

  /**
   * 删除来源
   */
  delete(id) {
    return request({
      url: `/sources/${id}`,
      method: 'delete'
    })
  }
}
