import request from './index'

/**
 * 档案相关API
 */
export const archiveApi = {
  // 获取档案列表
  list(params) {
    return request.get('/archives', { params })
  },

  // 获取档案详情
  getById(id) {
    return request.get(`/archives/${id}`)
  },

  // 接收档案
  receive(data) {
    return request.post('/archives/receive', data)
  },

  // 档案入库
  store(id, locationId, boxNo) {
    return request.post(`/archives/${id}/store`, null, { 
      params: { locationId, boxNo } 
    })
  },

  // 获取统计数据
  getStatistics() {
    return request.get('/archives/statistics')
  }
}

/**
 * 存放位置API
 */
export const locationApi = {
  // 获取位置列表
  list() {
    return request.get('/locations')
  },

  // 获取可用位置
  getAvailable() {
    return request.get('/locations/available')
  }
}

/**
 * 借阅API
 */
export const borrowApi = {
  // 获取借阅列表
  list(params) {
    return request.get('/borrows', { params })
  },

  // 申请借阅
  apply(data) {
    return request.post('/borrows/apply', data)
  },

  // 审批借阅
  approve(id, approved, comment) {
    return request.post(`/borrows/${id}/approve`, { approved, comment })
  },

  // 归还
  return(id, condition) {
    return request.post(`/borrows/${id}/return`, { condition })
  }
}

/**
 * 来源配置API
 */
export const sourceApi = {
  // 获取来源列表
  list() {
    return request.get('/sources')
  },

  // 启用/禁用来源
  toggle(id, enabled) {
    return request.put(`/sources/${id}/toggle`, { enabled })
  }
}
