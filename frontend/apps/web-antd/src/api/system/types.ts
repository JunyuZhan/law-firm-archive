/**
 * 系统管理模块类型定义
 */

// ========== 用户管理 ==========
export interface UserDTO {
  id: number;
  username: string;
  realName: string;
  email?: string;
  phone?: string;
  avatarUrl?: string;
  departmentId?: number;
  departmentName?: string;
  position?: string;
  employeeNo?: string;
  lawyerLicenseNo?: string;
  joinDate?: string;
  compensationType?: string;
  compensationTypeName?: string;
  canBeOriginator?: boolean;
  status: string;
  roleIds?: number[];
  roleCodes?: string[];
  permissions?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface UserQuery {
  username?: string;
  realName?: string;
  phone?: string;
  departmentId?: number;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateUserCommand {
  username: string;
  password: string;
  realName: string;
  email?: string;
  phone?: string;
  departmentId?: number;
  position?: string;
  employeeNo?: string;
  lawyerLicenseNo?: string;
  joinDate?: string;
  compensationType?: string;
  canBeOriginator?: boolean;
  roleIds?: number[];
}

export interface UpdateUserCommand extends Partial<CreateUserCommand> {
  id: number;
}

// ========== 角色管理 ==========
export interface RoleDTO {
  id: number;
  roleCode: string;
  roleName: string;
  description?: string;
  dataScope?: string;
  status?: string;
  sortOrder?: number;
  menuIds?: number[];
  createdAt?: string;
  updatedAt?: string;
}

export interface RoleQuery {
  roleCode?: string;
  roleName?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateRoleCommand {
  roleCode: string;
  roleName: string;
  description?: string;
  dataScope?: string;
  sortOrder?: number;
  menuIds?: number[];
}

export interface UpdateRoleCommand extends Partial<CreateRoleCommand> {
  id: number;
}

// ========== 部门管理 ==========
export interface DepartmentDTO {
  id: number;
  name: string;
  parentId?: number;
  parentName?: string;
  sortOrder?: number;
  leaderId?: number;
  leaderName?: string;
  status?: string;
  children?: DepartmentDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDepartmentCommand {
  name: string;
  parentId?: number;
  sortOrder?: number;
  leaderId?: number;
}

export interface UpdateDepartmentCommand extends Partial<CreateDepartmentCommand> {
  id: number;
}

// ========== 菜单管理 ==========
export interface MenuDTO {
  id: number;
  parentId: number;
  name: string;
  path: string;
  component?: string;
  redirect?: string;
  icon?: string;
  menuType: string;
  menuTypeName?: string;
  permission?: string;
  sortOrder: number;
  visible: boolean;
  status?: string;
  isExternal?: boolean;
  isCache?: boolean;
  children?: MenuDTO[];
}

export interface CreateMenuCommand {
  parentId: number;
  name: string;
  path: string;
  component?: string;
  redirect?: string;
  icon?: string;
  menuType: string;
  permission?: string;
  sortOrder?: number;
  visible?: boolean;
  isExternal?: boolean;
  isCache?: boolean;
}

export interface UpdateMenuCommand extends Partial<CreateMenuCommand> {
  id: number;
}

// ========== 字典管理 ==========
export interface DictTypeDTO {
  id: number;
  name: string;
  code: string;
  description?: string;
  status?: string;
  isSystem?: boolean;
  items?: DictDataDTO[];
  createdAt?: string;
  updatedAt?: string;
}

export interface DictDataDTO {
  id: number;
  dictTypeId: number;
  label: string;
  value: string;
  description?: string;
  sortOrder?: number;
  status?: string;
  cssClass?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateDictTypeCommand {
  name: string;
  code: string;
  description?: string;
}

export interface CreateDictItemCommand {
  dictTypeId: number;
  label: string;
  value: string;
  description?: string;
  sortOrder?: number;
  cssClass?: string;
}

// ========== 系统配置 ==========
export interface SysConfigDTO {
  id: number;
  configKey: string;
  configValue: string;
  configName: string;
  configType?: string;
  description?: string;
}

// ============ 合同编号配置相关 ============

export interface ContractNumberPreview {
  caseType: string;
  caseTypeName: string;
  preview: string;
}

export interface ContractNumberVariable {
  name: string;
  label: string;
  description: string;
}

export interface ContractNumberPattern {
  name: string;
  pattern: string;
  example: string;
  description: string;
}

export interface CaseTypeOption {
  value: string;
  label: string;
  shortName: string;
  code: string;
}

// ========== 数据库迁移 ==========
export interface MigrationDTO {
  id?: number;
  migrationNo: string;
  version: string;
  scriptName: string;
  scriptPath: string;
  description?: string;
  status: 'FAILED' | 'PENDING' | 'ROLLED_BACK' | 'SUCCESS';
  executedAt?: string;
  executionTimeMs?: number;
  errorMessage?: string;
  executedBy?: number;
  executedByName?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ========== 操作日志 ==========
export interface OperationLogDTO {
  id: number;
  module: string;
  action: string;
  method: string;
  requestUrl: string;
  requestMethod: string;
  requestParams?: string;
  responseResult?: string;
  operatorId: number;
  operatorName: string;
  operatorIp?: string;
  operationTime: string;
  duration?: number;
  status: string;
  errorMsg?: string;
}

export interface LogQuery {
  module?: string;
  action?: string;
  operatorName?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

// ========== 权限矩阵管理 ==========

/** 权限DTO */
export interface PermissionDTO {
  permissionCode: string;
  permissionName: string;
  menuType: string;
  module?: string;
}

/** 权限矩阵单元格 */
export interface PermissionMatrixCellDTO {
  permissionCode: string;
  permissionName: string;
  menuType: string;
  hasPermission: boolean;
}

/** 权限矩阵行 */
export interface PermissionMatrixRowDTO {
  roleId: number;
  roleCode: string;
  roleName: string;
  dataScope: string;
  permissions: PermissionMatrixCellDTO[];
}

/** 权限矩阵 */
export interface PermissionMatrixDTO {
  roles: RoleDTO[];
  permissions: PermissionDTO[];
  matrix: PermissionMatrixRowDTO[];
}

/** 角色权限详情 */
export interface RolePermissionDTO {
  roleId: number;
  roleCode: string;
  roleName: string;
  dataScope: string;
  permissions: Array<{
    hasPermission: boolean;
    menuType: string;
    permissionCode: string;
    permissionName: string;
  }>;
}

/** 权限对比行 */
export interface PermissionCompareRowDTO {
  permissionCode: string;
  permissionName: string;
  menuType: string;
  module?: string;
  roleHasPermission?: Record<number, boolean>;
}

/** 权限对比结果 */
export interface PermissionCompareDTO {
  roleIds: number[];
  roles: RoleDTO[];
  permissions: PermissionCompareRowDTO[];
}

// ========== 数据交接管理 ==========

/** 数据交接记录DTO */
export interface DataHandoverDTO {
  id: number;
  handoverNo: string;
  fromUserId: number;
  fromUsername: string;
  toUserId: number;
  toUsername: string;
  handoverType: string;
  handoverTypeName?: string;
  handoverReason?: string;
  status: string;
  statusName?: string;
  matterCount: number;
  clientCount: number;
  leadCount: number;
  taskCount: number;
  submittedBy?: number;
  submittedByName?: string;
  submittedAt?: string;
  confirmedBy?: number;
  confirmedByName?: string;
  confirmedAt?: string;
  remark?: string;
  createdAt?: string;
  details?: DataHandoverDetailDTO[];
}

/** 数据交接明细DTO */
export interface DataHandoverDetailDTO {
  id: number;
  handoverId: number;
  dataType: string;
  dataTypeName?: string;
  dataId: number;
  dataNo?: string;
  dataName?: string;
  fieldName: string;
  fieldDisplayName?: string;
  oldValue?: string;
  oldUserName?: string;
  newValue?: string;
  newUserName?: string;
  status: string;
  statusName?: string;
  errorMessage?: string;
  executedAt?: string;
  createdAt?: string;
}

/** 数据交接预览DTO */
export interface DataHandoverPreviewDTO {
  userId: number;
  userName: string;
  leadMatterCount: number;
  leadMatters?: Array<{
    id: number;
    matterNo: string;
    name: string;
    status: string;
  }>;
  participantMatterCount: number;
  participantMatters?: Array<{ id: number; matterId: number; role: string }>;
  originatorMatterCount: number;
  clientCount: number;
  clients?: Array<{ clientNo: string; id: number; name: string }>;
  leadCount: number;
  leads?: Array<{
    id: number;
    leadName: string;
    leadNo: string;
    status: string;
  }>;
  taskCount: number;
  tasks?: Array<{ id: number; status: string; taskNo: string; title: string }>;
  contractParticipantCount: number;
  totalCount: number;
}

/** 数据交接查询条件 */
export interface DataHandoverQuery {
  fromUserId?: number;
  toUserId?: number;
  handoverType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 创建交接命令 */
export interface CreateHandoverCommand {
  fromUserId: number;
  toUserId: number;
  handoverType?: string;
  reason?: string;
  matterIds?: number[];
  clientIds?: number[];
  leadIds?: number[];
  taskIds?: number[];
  includeOriginator?: boolean;
  remark?: string;
}

// ========== 外部系统集成 ==========

/** 外部系统集成DTO */
export interface ExternalIntegrationDTO {
  id: number;
  integrationCode: string;
  integrationName: string;
  integrationType: string;
  description?: string;
  apiUrl?: string;
  apiKey?: string;
  hasApiSecret: boolean;
  authType: string;
  extraConfig?: Record<string, any>;
  enabled: boolean;
  lastTestTime?: string;
  lastTestResult?: string;
  lastTestMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 外部系统集成查询条件 */
export interface ExternalIntegrationQuery {
  keyword?: string;
  integrationType?: string;
  enabled?: boolean;
  pageNum?: number;
  pageSize?: number;
}

/** 更新外部系统集成命令 */
export interface UpdateExternalIntegrationCommand {
  id: number;
  apiUrl?: string;
  apiKey?: string;
  apiSecret?: string;
  authType?: string;
  extraConfig?: Record<string, any>;
  description?: string;
}

// ========== 数据库备份 ==========

/** 备份DTO */
export interface BackupDTO {
  id: number;
  backupNo: string;
  backupType: string;
  backupName: string;
  backupPath: string;
  fileSize: number;
  status: string;
  backupTime?: string;
  restoreTime?: string;
  description?: string;
  createdBy?: number;
  createdByName?: string;
  createdAt?: string;
}

/** 备份查询条件 */
export interface BackupQuery {
  backupType?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

/** 创建备份命令 */
export interface CreateBackupCommand {
  backupType: string; // FULL, INCREMENTAL, DATABASE, FILE
  description?: string;
}

/** 恢复备份命令 */
export interface RestoreBackupCommand {
  backupId: number;
  description?: string;
}

// ========== 案由管理 ==========

/** 案由DTO */
export interface CauseOfActionDTO {
  id: number;
  code: string;
  name: string;
  type: 'ADMIN' | 'CIVIL' | 'CRIMINAL';
  causeType?: 'ADMIN' | 'CIVIL' | 'CRIMINAL';
  typeName?: string;
  categoryCode?: string;
  categoryName?: string;
  parentCode?: string;
  parentName?: string;
  level: number;
  sortOrder?: number;
  description?: string;
  isLeaf?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/** 案由树节点 */
export interface CauseTreeNodeDTO {
  code: string;
  name: string;
  parentCode?: string;
  level: number;
  sortOrder?: number;
  description?: string;
  isLeaf: boolean;
  categoryCode?: string;
  categoryName?: string;
  id?: number;
  children?: CauseTreeNodeDTO[];
}

// ========== 登录日志 ==========

/** 登录日志DTO */
export interface LoginLogDTO {
  id: number;
  userId?: number;
  username: string;
  loginTime: string;
  loginIp?: string;
  loginLocation?: string;
  browser?: string;
  os?: string;
  status: string;
  statusName?: string;
  loginType?: string;
  loginTypeName?: string;
  failReason?: string;
  userAgent?: string;
}

/** 登录日志查询条件 */
export interface LoginLogQuery {
  username?: string;
  loginIp?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

// ========== 会话管理 ==========

/** 用户会话DTO */
export interface UserSessionDTO {
  id: number;
  userId: number;
  username: string;
  realName?: string;
  loginTime: string;
  loginIp?: string;
  browser?: string;
  os?: string;
  lastAccessTime?: string;
  expireTime?: string;
  status: string;
  statusName?: string;
}

/** 会话查询条件 */
export interface SessionQuery {
  username?: string;
  loginIp?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

// ========== 缓存管理 ==========

/** 缓存统计信息 */
export interface CacheStats {
  totalKeys?: number;
  usedMemory?: string;
  hitRate?: number;
  hitCount?: number;
  missCount?: number;
  configCacheSize?: number;
  menuCacheSize?: number;
  deptCacheSize?: number;
  userPermissionCacheSize?: number;
}

// ========== 通用分页结果 ==========
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}
