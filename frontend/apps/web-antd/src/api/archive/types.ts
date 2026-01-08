/**
 * 档案管理模块类型定义
 */

export interface ArchiveDTO {
  id: number;
  archiveNo: string;
  archiveName?: string;
  name?: string; // 兼容旧字段
  matterId?: number;
  matterNo?: string;
  matterName?: string;
  archiveType?: string;
  archiveTypeName?: string;
  clientName?: string;
  mainLawyerName?: string;
  caseCloseDate?: string;
  volumeCount?: number;
  pageCount?: number;
  catalog?: string;
  locationId?: number;
  locationName?: string;
  boxNo?: string;
  archiveDate?: string;
  retentionPeriod?: string;
  retentionPeriodName?: string;
  retentionExpireDate?: string;
  hasElectronic?: boolean;
  electronicUrl?: string;
  status?: string;
  statusName?: string;
  storedBy?: number;
  storedByName?: string;
  storedAt?: string;
  // 迁移相关
  migrateDate?: string;
  migrateReason?: string;
  migrateApproverId?: number;
  migrateTarget?: string;
  filesDeleted?: boolean;
  // 快照
  archiveSnapshot?: string;
  description?: string;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ArchiveQuery {
  archiveNo?: string;
  archiveName?: string;
  name?: string;
  matterId?: number;
  matterNo?: string;
  matterName?: string;
  clientName?: string;
  archiveType?: string;
  locationId?: number;
  status?: string;
  caseCloseDateFrom?: string;
  caseCloseDateTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateArchiveCommand {
  matterId: number;
  archiveName?: string;
  name?: string;
  archiveType?: string;
  volumeCount?: number;
  pageCount?: number;
  catalog?: string;
  retentionPeriod?: string;
  hasElectronic?: boolean;
  electronicUrl?: string;
  remarks?: string;
  description?: string;
  selectedDataSourceIds?: number[];
}

export interface StoreArchiveCommand {
  archiveId: number;
  locationId: number;
  boxNo?: string;
}

/**
 * 归档预检查结果
 */
export interface ArchiveCheckResult {
  matterId: number;
  passed: boolean;
  missingItems: string[];
  warnings: string[];
}

/**
 * 归档数据快照
 */
export interface ArchiveDataSnapshot {
  matterId: number;
  matterNo: string;
  matterName: string;
  collectedAt: string;
  matterInfo: Record<string, any>;
  clientInfo: Record<string, any>[];
  participants: Record<string, any>[];
  contractInfo: Record<string, any>[];
  feeRecords: Record<string, any>[];
  paymentRecords: Record<string, any>[];
  expenseRecords: Record<string, any>[];
  timesheets: Record<string, any>[];
  documents: Record<string, any>[];
  dossierItems: Record<string, any>[];
  evidences: Record<string, any>[];
  approvals: Record<string, any>[];
  sealApplications: Record<string, any>[];
  letterApplications: Record<string, any>[];
  conflictChecks: Record<string, any>[];
  deadlines: Record<string, any>[];
  tasks: Record<string, any>[];
  schedules: Record<string, any>[];
  qualityChecks: Record<string, any>[];
  riskWarnings: Record<string, any>[];
  statistics: Record<string, number>;
}

/**
 * 归档数据源配置
 */
export interface ArchiveDataSource {
  id: number;
  source_name: string;
  source_table: string;
  source_type: string;
  dossier_folder?: string;
  is_enabled: boolean;
  is_required: boolean;
  sort_order: number;
  description?: string;
}

/**
 * 迁移申请
 */
export interface MigrateRequest {
  reason: string;
  migrateTarget: string;
}

/**
 * 迁移审批
 */
export interface ApproveMigrateRequest {
  approved: boolean;
  comment?: string;
  deleteFiles?: boolean;
}

