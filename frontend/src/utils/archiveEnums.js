/**
 * 档案系统枚举映射工具
 * 统一管理各类枚举的显示名称和样式
 */

// ========== 档案类型 ==========
export const ARCHIVE_TYPES = {
  DOCUMENT: '文书档案',
  SCIENCE: '科技档案',
  ACCOUNTING: '会计档案',
  PERSONNEL: '人事档案',
  SPECIAL: '专业档案',
  AUDIOVISUAL: '声像档案',
  LITIGATION: '诉讼档案',
  NON_LITIGATION: '非诉档案'
}

export const getArchiveTypeName = (type) => {
  return ARCHIVE_TYPES[type] || type || '-'
}

export const getArchiveTypeOptions = () => {
  return Object.entries(ARCHIVE_TYPES).map(([value, label]) => ({ value, label }))
}

// ========== 档案形式 ==========
export const ARCHIVE_FORMS = {
  ELECTRONIC: '电子档案',
  PHYSICAL: '纸质档案',
  HYBRID: '混合档案'
}

export const ARCHIVE_FORM_TYPE = {
  ELECTRONIC: 'primary',
  PHYSICAL: 'warning',
  HYBRID: 'success'
}

export const getArchiveFormName = (form) => {
  return ARCHIVE_FORMS[form] || form || '电子档案'
}

export const getArchiveFormType = (form) => {
  return ARCHIVE_FORM_TYPE[form] || 'primary'
}

export const getArchiveFormOptions = () => {
  return Object.entries(ARCHIVE_FORMS).map(([value, label]) => ({ value, label }))
}

// ========== 档案状态 ==========
export const ARCHIVE_STATUS = {
  DRAFT: '草稿',
  RECEIVED: '已接收',
  CATALOGING: '整理中',
  STORED: '已归档',
  BORROWED: '借出中',
  DESTROYED: '已销毁'
}

export const ARCHIVE_STATUS_TYPE = {
  DRAFT: 'info',
  RECEIVED: 'warning',
  CATALOGING: '',
  STORED: 'success',
  BORROWED: 'danger',
  DESTROYED: 'info'
}

export const getStatusName = (status) => {
  return ARCHIVE_STATUS[status] || status || '-'
}

export const getStatusType = (status) => {
  return ARCHIVE_STATUS_TYPE[status] || ''
}

// ========== 保管期限 ==========
export const RETENTION_PERIODS = {
  PERMANENT: '永久',
  Y30: '30年',
  Y15: '15年',
  Y10: '10年',
  Y5: '5年'
}

export const getRetentionName = (code) => {
  return RETENTION_PERIODS[code] || code || '-'
}

export const getRetentionOptions = () => {
  return Object.entries(RETENTION_PERIODS).map(([value, label]) => ({ value, label }))
}

// ========== 密级 ==========
export const SECURITY_LEVELS = {
  PUBLIC: '公开',
  INTERNAL: '内部',
  CONFIDENTIAL: '秘密',
  SECRET: '机密'
}

export const getSecurityName = (level) => {
  return SECURITY_LEVELS[level] || level || '内部'
}

export const getSecurityOptions = () => {
  return Object.entries(SECURITY_LEVELS).map(([value, label]) => ({ value, label }))
}

// ========== 来源类型 ==========
export const SOURCE_TYPES = {
  LAW_FIRM: '律所系统',
  MANUAL: '手动录入',
  IMPORT: '批量导入',
  TRANSFER: '移交'
}

export const getSourceName = (source) => {
  return SOURCE_TYPES[source] || source || '-'
}

// ========== 借阅状态 ==========
export const BORROW_STATUS = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  BORROWED: '借出中',
  RETURNED: '已归还',
  CANCELLED: '已取消',
  OVERDUE: '已逾期'
}

export const BORROW_STATUS_TYPE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  BORROWED: 'primary',
  RETURNED: 'info',
  CANCELLED: 'info',
  OVERDUE: 'danger'
}

export const getBorrowStatusName = (status) => {
  return BORROW_STATUS[status] || status || '-'
}

export const getBorrowStatusType = (status) => {
  return BORROW_STATUS_TYPE[status] || ''
}

// ========== 鉴定状态 ==========
export const APPRAISAL_STATUS = {
  PENDING: '待鉴定',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  EXTENDED: '延长保管'
}

export const getAppraisalStatusName = (status) => {
  return APPRAISAL_STATUS[status] || status || '-'
}

// ========== 销毁状态 ==========
export const DESTRUCTION_STATUS = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  EXECUTED: '已销毁'
}

export const DESTRUCTION_STATUS_TYPE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  EXECUTED: 'info'
}

export const getDestructionStatusName = (status) => {
  return DESTRUCTION_STATUS[status] || status || '-'
}

export const getDestructionStatusType = (status) => {
  return DESTRUCTION_STATUS_TYPE[status] || ''
}

// ========== 推送状态 ==========
export const PUSH_STATUS = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  SUCCESS: '成功',
  FAILED: '失败',
  PARTIAL: '部分成功'
}

export const PUSH_STATUS_TYPE = {
  PENDING: 'warning',
  PROCESSING: '',
  SUCCESS: 'success',
  FAILED: 'danger',
  PARTIAL: 'warning'
}

export const getPushStatusName = (status) => {
  return PUSH_STATUS[status] || status || '-'
}

export const getPushStatusType = (status) => {
  return PUSH_STATUS_TYPE[status] || ''
}

export const getPushStatusOptions = () => {
  return Object.entries(PUSH_STATUS).map(([value, label]) => ({ value, label }))
}

// ========== 全宗类型 ==========
export const FONDS_TYPES = {
  INTERNAL: '内部全宗',
  EXTERNAL: '外部全宗',
  HISTORICAL: '历史全宗'
}

export const FONDS_TYPE_TAG = {
  INTERNAL: 'primary',
  EXTERNAL: 'success',
  HISTORICAL: 'info'
}

export const getFondsTypeName = (type) => {
  return FONDS_TYPES[type] || type || '-'
}

export const getFondsTypeTag = (type) => {
  return FONDS_TYPE_TAG[type] || 'default'
}

export const getFondsTypeOptions = () => {
  return Object.entries(FONDS_TYPES).map(([value, label]) => ({ value, label }))
}

// ========== 用户角色 ==========
export const USER_ROLES = {
  SYSTEM_ADMIN: '系统管理员',
  SECURITY_ADMIN: '安全管理员',
  AUDIT_ADMIN: '审计管理员',
  ARCHIVIST: '档案管理员',
  USER: '普通用户'
}

export const getUserRoleName = (role) => {
  return USER_ROLES[role] || role || '-'
}

// ========== 文件分类 ==========
export const FILE_CATEGORIES = {
  COVER: '封面',
  CATALOG: '目录',
  MAIN: '正文',
  ATTACHMENT: '附件'
}

// 分类图标
export const FILE_CATEGORY_ICONS = {
  COVER: 'Picture',      // 封面用图片图标
  CATALOG: 'List',       // 目录用列表图标
  MAIN: 'Document',      // 正文用文档图标
  ATTACHMENT: 'Paperclip' // 附件用回形针图标
}

// 分类颜色
export const FILE_CATEGORY_COLORS = {
  COVER: '#e6a23c',     // 橙色
  CATALOG: '#409eff',   // 蓝色
  MAIN: '#67c23a',      // 绿色
  ATTACHMENT: '#909399' // 灰色
}

// 分类排序顺序（用于展示时排序）
export const FILE_CATEGORY_ORDER = ['COVER', 'CATALOG', 'MAIN', 'ATTACHMENT']

export const getFileCategoryName = (category) => {
  return FILE_CATEGORIES[category] || '其他'
}

export const getFileCategoryIcon = (category) => {
  return FILE_CATEGORY_ICONS[category] || 'Document'
}

export const getFileCategoryColor = (category) => {
  return FILE_CATEGORY_COLORS[category] || '#909399'
}

export const getFileCategoryOptions = () => {
  return FILE_CATEGORY_ORDER.map(value => ({ 
    value, 
    label: FILE_CATEGORIES[value] 
  }))
}
