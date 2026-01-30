import type {
  BackupDTO,
  BackupQuery,
  CacheStats,
  CaseTypeOption,
  ContractNumberPattern,
  ContractNumberPreview,
  ContractNumberVariable,
  CreateBackupCommand,
  CreateDepartmentCommand,
  CreateDictItemCommand,
  CreateDictTypeCommand,
  CreateHandoverCommand,
  CreateMenuCommand,
  CreateRoleCommand,
  CreateUserCommand,
  DataHandoverDTO,
  DataHandoverPreviewDTO,
  DataHandoverQuery,
  DepartmentDTO,
  DictDataDTO,
  DictTypeDTO,
  ExternalIntegrationDTO,
  ExternalIntegrationQuery,
  LogQuery,
  LoginLogDTO,
  LoginLogQuery,
  MenuDTO,
  MigrationDTO,
  OperationLogDTO,
  PageResult,
  PermissionCompareDTO,
  PermissionMatrixDTO,
  RestoreBackupCommand,
  RoleDTO,
  RolePermissionDTO,
  RoleQuery,
  SessionQuery,
  SysConfigDTO,
  UpdateDepartmentCommand,
  UpdateExternalIntegrationCommand,
  UpdateMenuCommand,
  UpdateRoleCommand,
  UpdateUserCommand,
  UserDTO,
  UserQuery,
  UserSessionDTO,
} from './types';

/**
 * 系统管理模块 API
 */
import { requestClient } from '#/api/request';

// ========== 用户管理 API ==========

/** 获取用户列表 */
export function getUserList(params: UserQuery) {
  return requestClient.get<PageResult<UserDTO>>('/system/user/list', {
    params,
  });
}

/** 获取用户选择列表（公共接口，无需特殊权限） */
export function getUserSelectOptions(params: UserQuery) {
  return requestClient.get<PageResult<UserDTO>>('/system/user/select-options', {
    params,
  });
}

/** 获取用户详情 */
export function getUserDetail(id: number) {
  return requestClient.get<UserDTO>(`/system/user/${id}`);
}

/** 创建用户 */
export function createUser(data: CreateUserCommand) {
  return requestClient.post<UserDTO>('/system/user', data);
}

/** 更新用户 */
export function updateUser(data: UpdateUserCommand) {
  return requestClient.put<UserDTO>('/system/user', data);
}

/** 删除用户 */
export function deleteUser(id: number) {
  return requestClient.delete(`/system/user/${id}`);
}

/** 批量删除用户 */
export function batchDeleteUsers(ids: number[]) {
  return requestClient.delete('/system/user/batch', { data: { ids } });
}

/** 重置密码 */
export function resetPassword(id: number, newPassword: string) {
  return requestClient.post(`/system/user/${id}/reset-password`, {
    newPassword,
  });
}

/** 修改用户状态 */
export function changeUserStatus(id: number, status: string) {
  return requestClient.put(`/system/user/${id}/status`, { status });
}

/** 导出用户列表 */
export function exportUsers(params?: UserQuery) {
  return requestClient.get('/system/user/export', {
    params,
    responseType: 'blob',
    responseReturn: 'body',
    timeout: 600_000,
  });
}

/** 批量导入用户 */
export function importUsers(
  file: File,
  onProgress?: (progress: {
    loaded: number;
    percent: number;
    total: number;
  }) => void,
) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<{
    errorMessages: string[];
    failCount: number;
    generatedPasswords?: Record<string, string>; // 用户名 -> 密码
    successCount: number;
    total: number;
  }>('/system/user/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 600_000,
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total,
        );
        onProgress({
          loaded: progressEvent.loaded,
          total: progressEvent.total,
          percent,
        });
      }
    },
  });
}

/** 下载用户导入模板 */
export function downloadUserImportTemplate() {
  return requestClient.get('/system/user/export', {
    params: { pageNum: 1, pageSize: 0 }, // 只返回表头模板
    responseType: 'blob',
    responseReturn: 'body',
  });
}

// ========== 角色管理 API ==========

/** 获取角色列表 */
export function getRoleList(params?: RoleQuery) {
  return requestClient.get<PageResult<RoleDTO>>('/system/role/list', {
    params,
  });
}

/** 获取所有角色（下拉选择用） */
export function getAllRoles() {
  return requestClient.get<RoleDTO[]>('/system/role/all');
}

/** 获取角色详情 */
export function getRoleDetail(id: number) {
  return requestClient.get<RoleDTO>(`/system/role/${id}`);
}

/** 创建角色 */
export function createRole(data: CreateRoleCommand) {
  return requestClient.post<RoleDTO>('/system/role', data);
}

/** 更新角色 */
export function updateRole(data: UpdateRoleCommand) {
  return requestClient.put<RoleDTO>('/system/role', data);
}

/** 删除角色 */
export function deleteRole(id: number) {
  return requestClient.delete(`/system/role/${id}`);
}

// ========== 部门管理 API ==========

/** 获取部门树 */
export function getDepartmentTree() {
  return requestClient.get<DepartmentDTO[]>('/system/department/tree');
}

/** 获取部门树（公共接口，无需特殊权限） */
export function getDepartmentTreePublic() {
  return requestClient.get<DepartmentDTO[]>('/system/department/tree-public');
}

/** 获取部门详情 */
export function getDepartmentDetail(id: number) {
  return requestClient.get<DepartmentDTO>(`/system/department/${id}`);
}

/** 创建部门 */
export function createDepartment(data: CreateDepartmentCommand) {
  return requestClient.post<DepartmentDTO>('/system/department', data);
}

/** 更新部门 */
export function updateDepartment(data: UpdateDepartmentCommand) {
  return requestClient.put<DepartmentDTO>('/system/department', data);
}

/** 删除部门 */
export function deleteDepartment(id: number) {
  return requestClient.delete(`/system/department/${id}`);
}

// ========== 菜单管理 API ==========

/** 获取菜单树 */
export function getMenuTree() {
  return requestClient.get<MenuDTO[]>('/system/menu/tree');
}

/** 获取菜单详情 */
export function getMenuDetail(id: number) {
  return requestClient.get<MenuDTO>(`/system/menu/${id}`);
}

/** 创建菜单 */
export function createMenu(data: CreateMenuCommand) {
  return requestClient.post<MenuDTO>('/system/menu', data);
}

/** 更新菜单 */
export function updateMenu(data: UpdateMenuCommand) {
  return requestClient.put<MenuDTO>(`/system/menu/${data.id}`, data);
}

/** 删除菜单 */
export function deleteMenu(id: number) {
  return requestClient.delete(`/system/menu/${id}`);
}

/** 获取角色菜单ID */
export function getRoleMenuIds(roleId: number) {
  return requestClient.get<number[]>(`/system/menu/role/${roleId}`);
}

/** 分配角色菜单 */
export function assignRoleMenus(roleId: number, menuIds: number[]) {
  return requestClient.put(`/system/menu/role/${roleId}`, menuIds);
}

// ========== 字典管理 API ==========

/** 获取字典类型列表 */
export function getDictTypeList() {
  return requestClient.get<DictTypeDTO[]>('/system/dict/types');
}

/** 获取字典类型详情（含字典项） */
export function getDictTypeWithItems(id: number) {
  return requestClient.get<DictTypeDTO>(`/system/dict/types/${id}`);
}

/** 根据编码获取字典项 */
export function getDictDataByCode(dictCode: string) {
  return requestClient.get<DictDataDTO[]>(
    `/system/dict/items/code/${dictCode}`,
  );
}

/** 根据类型ID获取字典项列表 */
export function getDictItemsByTypeId(typeId: number) {
  return requestClient.get<DictDataDTO[]>(`/system/dict/types/${typeId}/items`);
}

/** 创建字典类型 */
export function createDictType(data: CreateDictTypeCommand) {
  return requestClient.post<DictTypeDTO>('/system/dict/types', data);
}

/** 更新字典类型 */
export function updateDictType(id: number, data: CreateDictTypeCommand) {
  return requestClient.put<DictTypeDTO>(`/system/dict/types/${id}`, data);
}

/** 删除字典类型 */
export function deleteDictType(id: number) {
  return requestClient.delete(`/system/dict/types/${id}`);
}

/** 创建字典项 */
export function createDictItem(data: CreateDictItemCommand) {
  return requestClient.post<DictDataDTO>('/system/dict/items', data);
}

/** 更新字典项 */
export function updateDictItem(id: number, data: CreateDictItemCommand) {
  return requestClient.put<DictDataDTO>(`/system/dict/items/${id}`, data);
}

/** 删除字典项 */
export function deleteDictItem(id: number) {
  return requestClient.delete(`/system/dict/items/${id}`);
}

/** 启用/禁用字典项 */
export function toggleDictItemStatus(id: number) {
  return requestClient.post(`/system/dict/items/${id}/toggle`);
}

// ========== 系统配置 API ==========

/** 获取系统配置列表 */
export function getSysConfigList() {
  return requestClient.get<SysConfigDTO[]>('/system/config');
}

/** 获取配置值 */
export function getConfigValue(configKey: string) {
  return requestClient.get<SysConfigDTO>(`/system/config/key/${configKey}`);
}

/** 获取系统版本信息 */
export function getVersionInfo() {
  return requestClient.get<{
    buildTime: string;
    buildVersion?: string; // 构建版本号（如 1.0.0-SNAPSHOT）
    gitCommit: string;
    javaVendor: string;
    javaVersion: string;
    osName: string;
    osVersion: string;
    profile: string;
    serverTime: string;
    version: string; // 显示版本号（优先数据库配置，支持简单格式如 0.4）
  }>('/system/config/version');
}

/** 创建配置 */
export function createConfig(data: {
  configKey: string;
  configName: string;
  configType?: string;
  configValue: string;
  description?: string;
}) {
  return requestClient.post<SysConfigDTO>('/system/config', data);
}

/** 更新配置 */
export function updateConfig(
  id: number,
  data: { configValue: string; description?: string },
) {
  return requestClient.put(`/system/config/${id}`, data);
}

/** 删除配置 */
export function deleteConfig(id: number) {
  return requestClient.delete(`/system/config/${id}`);
}

/** 获取维护模式状态 */
export function getMaintenanceStatus() {
  return requestClient.get<{ enabled: boolean; message: string }>(
    '/system/config/maintenance/status',
  );
}

/** 开启维护模式 */
export function enableMaintenanceMode(message?: string) {
  return requestClient.post(
    '/system/config/maintenance/enable',
    message ? { message } : {},
  );
}

/** 关闭维护模式 */
export function disableMaintenanceMode() {
  return requestClient.post('/system/config/maintenance/disable');
}

// ============ 合同编号配置相关 ============

/** 预览合同编号规则 */
export function previewContractNumber(params: {
  caseType?: string;
  feeType?: string;
  pattern?: string;
  prefix?: string;
  sequenceLength?: number;
}) {
  return requestClient.post<ContractNumberPreview[]>(
    '/system/config/contract-number/preview',
    params,
  );
}

/** 获取合同编号支持的变量 */
export function getContractNumberVariables() {
  return requestClient.get<ContractNumberVariable[]>(
    '/system/config/contract-number/variables',
  );
}

/** 获取推荐的合同编号规则模板 */
export function getRecommendedPatterns() {
  return requestClient.get<ContractNumberPattern[]>(
    '/system/config/contract-number/patterns',
  );
}

/** 获取案件类型选项 */
export function getCaseTypeOptions() {
  return requestClient.get<CaseTypeOption[]>(
    '/system/config/contract-number/case-types',
  );
}

// ========== 操作日志 API ==========

/** 获取操作日志列表 */
export function getOperationLogList(params: LogQuery) {
  return requestClient.get<PageResult<OperationLogDTO>>(
    '/admin/operation-logs',
    { params },
  );
}

/** 导出操作日志 */
export function exportOperationLog(data: LogQuery) {
  return requestClient.post('/admin/operation-logs/export', data, {
    responseType: 'blob',
    responseReturn: 'body', // 直接返回响应体，不经过拦截器处理
    timeout: 600_000, // 10分钟超时（支持大文件导出）
  });
}

/** 获取日志统计信息 */
export function getLogStatistics(params?: {
  endTime?: string;
  startTime?: string;
}) {
  return requestClient.get<Record<string, any>>(
    '/admin/operation-logs/statistics',
    { params },
  );
}

/** 清理历史日志 */
export function cleanOldLogs(keepDays: number) {
  return requestClient.delete('/admin/operation-logs/clean', {
    params: { keepDays },
  });
}

// ========== 权限矩阵管理 API ==========

/** 获取权限矩阵 */
export function getPermissionMatrix(params?: {
  module?: string;
  permissionType?: string;
}) {
  return requestClient.get<PermissionMatrixDTO>('/system/permission-matrix', {
    params,
  });
}

/** 获取角色权限详情 */
export function getRolePermissions(roleId: number) {
  return requestClient.get<RolePermissionDTO>(
    `/system/permission-matrix/role/${roleId}`,
  );
}

/** 对比角色权限 */
export function comparePermissions(roleIds: number[]) {
  return requestClient.post<PermissionCompareDTO>(
    '/system/permission-matrix/compare',
    { roleIds },
  );
}

// ========== 数据交接管理 API ==========

/** 预览用户交接数据 */
export function previewHandover(userId: number) {
  return requestClient.get<DataHandoverPreviewDTO>(
    `/system/data-handover/preview/${userId}`,
  );
}

/** 获取交接单列表 */
export function getHandoverList(params: DataHandoverQuery) {
  return requestClient.get<PageResult<DataHandoverDTO>>(
    '/system/data-handover',
    { params },
  );
}

// ========== 数据库迁移管理 API ==========

/** 扫描迁移脚本 */
export function scanMigrationScripts() {
  return requestClient.get<MigrationDTO[]>('/system/migration/scan');
}

/** 获取迁移记录列表 */
export function getMigrationList(params: {
  pageNum?: number;
  pageSize?: number;
}) {
  return requestClient.get<PageResult<MigrationDTO>>('/system/migration/list', {
    params,
  });
}

/** 获取迁移详情 */
export function getMigrationDetail(id: number) {
  return requestClient.get<MigrationDTO>(`/system/migration/${id}`);
}

/** 执行迁移脚本 */
export function executeMigration(version: string) {
  return requestClient.post<MigrationDTO>(
    `/system/migration/execute/${version}`,
  );
}

/** 获取交接单详情 */
export function getHandoverDetail(id: number) {
  return requestClient.get<DataHandoverDTO>(`/system/data-handover/${id}`);
}

/** 创建离职交接 */
export function createResignationHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>(
    '/system/data-handover/resignation',
    data,
  );
}

/** 创建项目移交 */
export function createProjectHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>(
    '/system/data-handover/project',
    data,
  );
}

/** 创建客户移交 */
export function createClientHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>(
    '/system/data-handover/client',
    data,
  );
}

/** 确认交接 */
export function confirmHandover(id: number) {
  return requestClient.post(`/system/data-handover/${id}/confirm`, {});
}

/** 取消交接 */
export function cancelHandover(id: number, reason?: string) {
  return requestClient.post(`/system/data-handover/${id}/cancel`, { reason });
}

// ========== 外部系统集成 API ==========

/** 获取外部系统集成列表 */
export function getExternalIntegrationList(params: ExternalIntegrationQuery) {
  return requestClient.get<PageResult<ExternalIntegrationDTO>>(
    '/system/integration',
    { params },
  );
}

/** 获取所有外部系统集成 */
export function getAllExternalIntegrations() {
  return requestClient.get<ExternalIntegrationDTO[]>('/system/integration/all');
}

/** 获取指定类型的启用集成 */
export function getEnabledIntegrations(type: string) {
  return requestClient.get<ExternalIntegrationDTO[]>(
    '/system/integration/enabled',
    { params: { type } },
  );
}

/** 获取外部系统集成详情 */
export function getExternalIntegrationDetail(id: number) {
  return requestClient.get<ExternalIntegrationDTO>(`/system/integration/${id}`);
}

/** 创建外部系统集成 */
export function createExternalIntegration(
  data: Omit<UpdateExternalIntegrationCommand, 'id'>,
) {
  return requestClient.post<ExternalIntegrationDTO>(
    '/system/integration',
    data,
  );
}

/** 更新外部系统集成 */
export function updateExternalIntegration(
  data: UpdateExternalIntegrationCommand,
) {
  return requestClient.put(`/system/integration/${data.id}`, data);
}

/** 启用外部系统集成 */
export function enableExternalIntegration(id: number) {
  return requestClient.post(`/system/integration/${id}/enable`, {});
}

/** 禁用外部系统集成 */
export function disableExternalIntegration(id: number) {
  return requestClient.post(`/system/integration/${id}/disable`, {});
}

/** 测试外部系统连接 */
export function testExternalIntegration(id: number) {
  return requestClient.post<ExternalIntegrationDTO>(
    `/system/integration/${id}/test`,
    {},
  );
}

// ========== 数据库备份 API ==========

/** 获取备份列表 */
export function getBackupList(params: BackupQuery) {
  return requestClient.get<PageResult<BackupDTO>>('/system/backup/list', {
    params,
  });
}

/** 获取备份详情 */
export function getBackupDetail(id: number) {
  return requestClient.get<BackupDTO>(`/system/backup/${id}`);
}

/** 创建备份 */
export function createBackup(data: CreateBackupCommand) {
  return requestClient.post<BackupDTO>('/system/backup', data);
}

/** 恢复备份 */
export function restoreBackup(data: RestoreBackupCommand) {
  return requestClient.post('/system/backup/restore', data);
}

/** 删除备份 */
export function deleteBackup(id: number) {
  return requestClient.delete(`/system/backup/${id}`);
}

/** 下载备份文件 */
export function downloadBackup(
  id: number,
  onProgress?: (progress: {
    loaded: number;
    percent: number;
    total: number;
  }) => void,
) {
  return requestClient.get(`/system/backup/${id}/download`, {
    responseType: 'blob',
    responseReturn: 'body',
    timeout: 600_000, // 10分钟超时
    onDownloadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total,
        );
        onProgress({
          loaded: progressEvent.loaded,
          total: progressEvent.total,
          percent,
        });
      }
    },
  });
}

/** 导入外部备份文件 */
export function importBackup(
  file: File,
  backupType: string = 'DATABASE',
  description?: string,
  onProgress?: (progress: {
    loaded: number;
    percent: number;
    total: number;
  }) => void,
) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('backupType', backupType);
  if (description) {
    formData.append('description', description);
  }

  return requestClient.post<BackupDTO>('/system/backup/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    timeout: 600_000, // 10分钟超时（支持大文件）
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total,
        );
        onProgress({
          loaded: progressEvent.loaded,
          total: progressEvent.total,
          percent,
        });
      }
    },
  });
}

// ======================== 邮件通知 API ========================

/** 测试邮件配置 */
export function testEmailConfig(email: string) {
  return requestClient.post<string>('/system/notification/test-email', {
    email,
  });
}

/** 获取邮件服务状态 */
export function getEmailStatus() {
  return requestClient.get<{ enabled: boolean }>(
    '/system/notification/email-status',
  );
}

/** 发送测试告警 */
export function sendTestAlert(type: string) {
  return requestClient.post<string>('/system/notification/test-alert', {
    type,
  });
}

/** 生成系统报告预览 */
export function previewSystemReport(type: 'daily' | 'weekly' = 'daily') {
  return requestClient.get<string>('/system/notification/report-preview', {
    params: { type },
  });
}

/** 立即发送系统报告 */
export function sendSystemReport(type: 'daily' | 'weekly' = 'daily') {
  return requestClient.post<string>('/system/notification/send-report', {
    type,
  });
}

// ========== 登录日志 API ==========

/** 获取登录日志列表 */
export function getLoginLogList(params: LoginLogQuery) {
  return requestClient.get<PageResult<LoginLogDTO>>('/system/login-log', {
    params,
  });
}

/** 获取登录日志详情 */
export function getLoginLogDetail(id: number) {
  return requestClient.get<LoginLogDTO>(`/system/login-log/${id}`);
}

/** 获取用户最近登录记录 */
export function getRecentLoginLogs(userId: number, limit: number = 10) {
  return requestClient.get<LoginLogDTO[]>(
    `/system/login-log/users/${userId}/recent`,
    { params: { limit } },
  );
}

/** 统计登录失败次数 */
export function countLoginFailure(username: string) {
  return requestClient.get<number>('/system/login-log/failure-count', {
    params: { username },
  });
}

// ========== 会话管理 API ==========

/** 获取会话列表 */
export function getSessionList(params: SessionQuery) {
  return requestClient.get<PageResult<UserSessionDTO>>('/system/sessions/list', {
    params,
  });
}

/** 获取我的活跃会话 */
export function getMySessions() {
  return requestClient.get<UserSessionDTO[]>('/system/sessions/my-sessions');
}

/** 登出指定会话 */
export function logoutSession(id: number) {
  return requestClient.post(`/system/sessions/${id}/logout`, {});
}

/** 强制下线会话 */
export function forceLogoutSession(id: number, reason?: string) {
  return requestClient.post(`/system/sessions/${id}/force-logout`, null, {
    params: { reason },
  });
}

/** 强制下线用户所有会话 */
export function forceLogoutUser(userId: number, reason?: string) {
  return requestClient.post(
    `/system/sessions/user/${userId}/force-logout`,
    null,
    { params: { reason } },
  );
}

// ========== 缓存管理 API ==========

/** 获取缓存统计 */
export function getCacheStats() {
  return requestClient.get<CacheStats>('/api/admin/cache/stats');
}

/** 清除所有缓存 */
export function clearAllCache() {
  return requestClient.delete('/api/admin/cache/all');
}

/** 清除配置缓存 */
export function clearConfigCache() {
  return requestClient.delete('/api/admin/cache/config');
}

/** 清除菜单缓存 */
export function clearMenuCache() {
  return requestClient.delete('/api/admin/cache/menu');
}

/** 清除部门缓存 */
export function clearDeptCache() {
  return requestClient.delete('/api/admin/cache/dept');
}

/** 清除指定配置缓存 */
export function clearConfigCacheByKey(key: string) {
  return requestClient.delete(`/api/admin/cache/config/${key}`);
}

/** 清除指定用户菜单缓存 */
export function clearUserMenuCache(userId: number) {
  return requestClient.delete(`/api/admin/cache/menu/user/${userId}`);
}

// ========== 客户门户令牌 API ==========

// 导出公告管理API
export * from './announcement';

// 导出案由管理API
export * from './cause-of-action';

// 导出类型
export type * from './types';
