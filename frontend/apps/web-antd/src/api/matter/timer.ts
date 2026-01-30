import { requestClient } from '#/api/request';

// ==================== 类型定义 ====================

export interface TimerSessionDTO {
  id?: number;
  userId?: number;
  matterId: number;
  matterName?: string;
  taskId?: number;
  taskName?: string;
  description?: string;
  status: 'IDLE' | 'PAUSED' | 'RUNNING' | 'STOPPED';
  startTime?: string;
  pauseTime?: string;
  totalSeconds?: number;
  pausedSeconds?: number;
  elapsedSeconds?: number;
}

export interface StartTimerCommand {
  matterId: number;
  taskId?: number;
  description?: string;
}

// ==================== API ====================

/** 开始计时 */
export function startTimer(data: StartTimerCommand) {
  return requestClient.post<TimerSessionDTO>('/timer/start', data);
}

/** 暂停计时 */
export function pauseTimer() {
  return requestClient.post<TimerSessionDTO>('/timer/pause');
}

/** 继续计时 */
export function resumeTimer() {
  return requestClient.post<TimerSessionDTO>('/timer/resume');
}

/** 停止计时并保存工时记录 */
export function stopTimer() {
  return requestClient.post('/timer/stop');
}

/** 获取当前计时器状态 */
export function getTimerStatus() {
  return requestClient.get<TimerSessionDTO>('/timer/status');
}
