/**
 * 工作台模块类型定义
 */

// 工作台统计数据
export interface WorkbenchStatsDTO {
  // 通用字段
  taskCount: number; // 待办任务数
  roleType: 'LAWYER' | 'FINANCE' | 'ADMIN_STAFF'; // 角色类型

  // 律师/团队负责人/主任相关
  matterCount?: number; // 我的项目数
  clientCount?: number; // 我的客户数
  timesheetHours?: number; // 本月工时

  // 财务相关
  pendingPaymentCount?: number; // 待确认收款数
  pendingInvoiceCount?: number; // 待开票数
  pendingExpenseCount?: number; // 待审批报销数
  monthlyReceivedAmount?: number; // 本月已收金额

  // 行政相关
  pendingLetterCount?: number; // 待处理出函数
  pendingSealCount?: number; // 待处理用印数
  pendingLeaveCount?: number; // 待审批请假数
  pendingAssetCount?: number; // 待处理资产领用数
}
