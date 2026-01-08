/**
 * 工作台模块类型定义
 */

// 工作台统计数据
export interface WorkbenchStatsDTO {
  matterCount: number;      // 我的项目数
  clientCount: number;      // 我的客户数
  timesheetHours: number;   // 本月工时
  taskCount: number;        // 待办任务数
}

