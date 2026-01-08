/**
 * 考勤记录类型定义
 */
export interface AttendanceRecord {
  id: number;
  userId: number;
  username?: string;
  realName?: string;
  attendanceDate: string;
  checkInTime?: string;
  checkOutTime?: string;
  status: 'NORMAL' | 'LATE' | 'EARLY_LEAVE' | 'ABSENT' | 'LEAVE';
  workHours?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 请假类型
 */
export interface LeaveType {
  id: number;
  name: string;
  code: string;
  maxDays?: number;
  needApproval: boolean;
  description?: string;
}

/**
 * 请假申请类型定义
 */
export interface LeaveApplication {
  id: number;
  userId: number;
  username?: string;
  realName?: string;
  leaveTypeId: number;
  leaveTypeName?: string;
  startDate: string;
  endDate: string;
  leaveDays: number;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
  approverId?: number;
  approverName?: string;
  approveTime?: string;
  approveComment?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 假期余额
 */
export interface LeaveBalance {
  leaveTypeId: number;
  leaveTypeName: string;
  totalDays: number;
  usedDays: number;
  remainingDays: number;
}

/**
 * 会议室类型定义
 */
export interface MeetingRoom {
  id: number;
  name: string;
  location?: string;
  capacity: number;
  equipment?: string;
  status: 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';
  description?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 会议室预约类型定义
 */
export interface MeetingBooking {
  id: number;
  roomId: number;
  roomName?: string;
  userId: number;
  userName?: string;
  title: string;
  startTime: string;
  endTime: string;
  participants?: string;
  status: 'BOOKED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  description?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 分页查询参数
 */
export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

/**
 * 分页响应数据
 */
export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/**
 * 考勤查询参数
 */
export interface AttendanceQueryParams extends PageParams {
  userId?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
}

/**
 * 请假查询参数
 */
export interface LeaveQueryParams extends PageParams {
  userId?: number;
  leaveTypeId?: number;
  status?: string;
  startDate?: string;
  endDate?: string;
}

/**
 * 会议室查询参数
 */
export interface MeetingRoomQueryParams extends PageParams {
  name?: string;
  status?: string;
}

/**
 * 会议室预约查询参数
 */
export interface MeetingBookingQueryParams extends PageParams {
  roomId?: number;
  userId?: number;
  startTime?: string;
  endTime?: string;
  status?: string;
}
