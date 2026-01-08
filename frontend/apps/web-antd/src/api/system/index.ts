/**
 * 系统管理模块 API
 */
import { requestClient } from '#/api/request';

import type {
  CreateDepartmentCommand,
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
  MenuDTO,
  OperationLogDTO,
  PageResult,
  PermissionCompareDTO,
  PermissionMatrixDTO,
  RoleDTO,
  RolePermissionDTO,
  RoleQuery,
  SysConfigDTO,
  UpdateDepartmentCommand,
  UpdateExternalIntegrationCommand,
  UpdateMenuCommand,
  UpdateRoleCommand,
  UpdateUserCommand,
  UserDTO,
  UserQuery,
} from './types';

// ========== 用户管理 API ==========

/** 获取用户列表 */
export function getUserList(params: UserQuery) {
  return requestClient.get<PageResult<UserDTO>>('/system/user/list', { params });
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
  return requestClient.post(`/system/user/${id}/reset-password`, { newPassword });
}

/** 修改用户状态 */
export function changeUserStatus(id: number, status: string) {
  return requestClient.put(`/system/user/${id}/status`, { status });
}

// ========== 角色管理 API ==========

/** 获取角色列表 */
export function getRoleList(params?: RoleQuery) {
  return requestClient.get<PageResult<RoleDTO>>('/system/role/list', { params });
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

/** 获取字典数据 */
export function getDictDataByCode(dictCode: string) {
  return requestClient.get<DictDataDTO[]>(`/system/dict/items/code/${dictCode}`);
}

/** 创建字典类型 */
export function createDictType(data: Partial<DictTypeDTO>) {
  return requestClient.post<DictTypeDTO>('/system/dict/type', data);
}

/** 创建字典数据 */
export function createDictData(data: Partial<DictDataDTO>) {
  return requestClient.post<DictDataDTO>('/system/dict/data', data);
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

/** 更新配置 */
export function updateConfig(id: number, data: { configValue: string; description?: string }) {
  return requestClient.put(`/system/config/${id}`, data);
}

// ============ 合同编号配置相关 ============

/** 预览合同编号规则 */
export function previewContractNumber(params: {
  pattern?: string;
  prefix?: string;
  sequenceLength?: number;
  caseType?: string;
  feeType?: string;
}) {
  return requestClient.post<ContractNumberPreview[]>('/system/config/contract-number/preview', params);
}

/** 获取合同编号支持的变量 */
export function getContractNumberVariables() {
  return requestClient.get<ContractNumberVariable[]>('/system/config/contract-number/variables');
}

/** 获取推荐的合同编号规则模板 */
export function getRecommendedPatterns() {
  return requestClient.get<ContractNumberPattern[]>('/system/config/contract-number/patterns');
}

/** 获取案件类型选项 */
export function getCaseTypeOptions() {
  return requestClient.get<CaseTypeOption[]>('/system/config/contract-number/case-types');
}

// ========== 操作日志 API ==========

/** 获取操作日志列表 */
export function getOperationLogList(params: LogQuery) {
  return requestClient.get<PageResult<OperationLogDTO>>('/system/log/list', { params });
}

/** 导出操作日志 */
export function exportOperationLog(params: LogQuery) {
  return requestClient.get('/system/log/export', {
    params,
    responseType: 'blob',
  });
}

// ========== 权限矩阵管理 API ==========

/** 获取权限矩阵 */
export function getPermissionMatrix(params?: { module?: string; permissionType?: string }) {
  return requestClient.get<PermissionMatrixDTO>('/system/permission-matrix', { params });
}

/** 获取角色权限详情 */
export function getRolePermissions(roleId: number) {
  return requestClient.get<RolePermissionDTO>(`/system/permission-matrix/role/${roleId}`);
}

/** 对比角色权限 */
export function comparePermissions(roleIds: number[]) {
  return requestClient.post<PermissionCompareDTO>('/system/permission-matrix/compare', { roleIds });
}

// ========== 数据交接管理 API ==========

/** 预览用户交接数据 */
export function previewHandover(userId: number) {
  return requestClient.get<DataHandoverPreviewDTO>(`/system/data-handover/preview/${userId}`);
}

/** 获取交接单列表 */
export function getHandoverList(params: DataHandoverQuery) {
  return requestClient.get<PageResult<DataHandoverDTO>>('/system/data-handover', { params });
}

/** 获取交接单详情 */
export function getHandoverDetail(id: number) {
  return requestClient.get<DataHandoverDTO>(`/system/data-handover/${id}`);
}

/** 创建离职交接 */
export function createResignationHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>('/system/data-handover/resignation', data);
}

/** 创建项目移交 */
export function createProjectHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>('/system/data-handover/project', data);
}

/** 创建客户移交 */
export function createClientHandover(data: CreateHandoverCommand) {
  return requestClient.post<DataHandoverDTO>('/system/data-handover/client', data);
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
  return requestClient.get<PageResult<ExternalIntegrationDTO>>('/system/integration', { params });
}

/** 获取所有外部系统集成 */
export function getAllExternalIntegrations() {
  return requestClient.get<ExternalIntegrationDTO[]>('/system/integration/all');
}

/** 获取指定类型的启用集成 */
export function getEnabledIntegrations(type: string) {
  return requestClient.get<ExternalIntegrationDTO[]>('/system/integration/enabled', { params: { type } });
}

/** 获取外部系统集成详情 */
export function getExternalIntegrationDetail(id: number) {
  return requestClient.get<ExternalIntegrationDTO>(`/system/integration/${id}`);
}

/** 更新外部系统集成 */
export function updateExternalIntegration(data: UpdateExternalIntegrationCommand) {
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
  return requestClient.post<ExternalIntegrationDTO>(`/system/integration/${id}/test`, {});
}

// 导出类型
export type * from './types';
