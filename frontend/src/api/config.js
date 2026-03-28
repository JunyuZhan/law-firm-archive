import request from '@/utils/request'

/**
 * 获取所有配置（按分组）
 */
export function getConfigsGrouped() {
  return request({
    url: '/configs',
    method: 'get'
  })
}

/**
 * 获取配置列表
 */
export function getConfigList() {
  return request({
    url: '/configs/list',
    method: 'get'
  })
}

/**
 * 按分组获取配置
 */
export function getConfigsByGroup(group) {
  return request({
    url: `/configs/group/${group}`,
    method: 'get'
  })
}

/**
 * 获取站点配置（公开接口，无需认证）
 */
export function getPublicSiteConfig() {
  return request({
    url: '/configs/public/site',
    method: 'get'
  })
}

/**
 * 获取单个配置
 */
export function getConfig(key) {
  return request({
    url: `/configs/${key}`,
    method: 'get'
  })
}

/**
 * 更新配置
 */
export function updateConfig(key, value) {
  return request({
    url: `/configs/${key}`,
    method: 'put',
    data: { value }
  })
}

/**
 * 批量更新配置
 */
export function batchUpdateConfigs(configs) {
  return request({
    url: '/configs/batch',
    method: 'put',
    data: configs
  })
}

/**
 * 创建配置
 */
export function createConfig(data) {
  return request({
    url: '/configs',
    method: 'post',
    data
  })
}

/**
 * 删除配置
 */
export function deleteConfig(key) {
  return request({
    url: `/configs/${key}`,
    method: 'delete'
  })
}

/**
 * 刷新配置缓存
 */
export function refreshConfigCache() {
  return request({
    url: '/configs/refresh',
    method: 'post'
  })
}

/**
 * 获取档案号配置
 */
export function getArchiveNoConfigs() {
  return request({
    url: '/configs/archive-no',
    method: 'get'
  })
}

/**
 * 获取保管期限配置
 */
export function getRetentionConfigs() {
  return request({
    url: '/configs/retention',
    method: 'get'
  })
}

/**
 * 获取系统参数配置
 */
export function getSystemConfigs() {
  return request({
    url: '/configs/system',
    method: 'get'
  })
}
