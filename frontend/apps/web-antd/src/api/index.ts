/**
 * API 模块统一导出
 *
 * 注意：为避免类型重复导出冲突，PageResult 只从 client/types 导出一次
 */

export * from './admin';

// 客户模块（排除 PageResult）
export {
  applyConflictCheck,
  approveConflictCheck,
  convertLeadToClient,
  createClient,
  createLead,
  deleteClient,
  deleteLead,
  getClientDetail,
  // 函数
  getClientList,
  getClientSelectOptions,
  getConflictCheckDetail,
  getConflictCheckList,
  getLeadDetail,
  getLeadList,
  rejectConflictCheck,
  updateClient,
  updateLead,
} from './client';

// 共享类型（只导出一次）
export type { PageResult } from './client/types';
export type {
  ApplyConflictCheckCommand,
  ClientDTO,
  ClientQuery,
  ClientSimpleDTO,
  ConflictCheckDTO,
  ConflictCheckQuery,
  CreateClientCommand,
  CreateLeadCommand,
  LeadDTO,
  LeadQuery,
  UpdateClientCommand,
} from './client/types';

// 核心API
export * from './core';
// 财务模块
export * from './finance';
// 行政后勤模块
export * from './hr';
// 项目模块（排除 PageResult 和重复的 Contract 函数）
export {
  applyCloseMatter,
  approveCloseMatter,
  approveTimesheet,
  changeMatterStatus,
  changeTaskStatus,
  createMatter,
  createTask,
  createTimesheet,
  deleteMatter,
  deleteTask,
  deleteTimesheet,
  getMatterDetail,
  // Matter
  getMatterList,
  getMatterTaskStats,
  getMatterTimeline,
  getMyMatters,
  getMyTimesheets,
  getMyTodoTasks,
  getTaskDetail,
  // Task
  getTaskList,
  getTimesheetDetail,
  // Timesheet
  getTimesheetList,
  submitTimesheet,
  updateMatter,
  updateTask,
  updateTimesheet,
} from './matter';
// 使用别名重导出合同相关函数，避免与 finance 模块冲突
export {
  approveContract as approveMatterContract,
  createContract as createMatterContract,
  getContractDetail as getMatterContractDetail,
  getMyContracts as getMatterContracts,
  rejectContract as rejectMatterContract,
  submitContract as submitMatterContract,
  updateContract as updateMatterContract,
} from './matter';

export * from './matter/deadline';

export * from './matter/schedule';
export type {
  CreateMatterCommand,
  CreateTaskCommand,
  CreateTimesheetCommand,
  MatterDTO,
  MatterQuery,
  MatterSimpleDTO,
  MatterTimelineDTO,
  TaskDTO,
  TaskQuery,
  TimesheetDTO,
  TimesheetQuery,
  UpdateMatterCommand,
} from './matter/types';

// OCR识别模块
export * from './ocr';

// 工作台模块
export * from './workbench';
