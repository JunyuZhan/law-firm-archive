import { onMounted, onUnmounted, ref } from 'vue';

import { Modal } from 'ant-design-vue';

export interface IdleTimeoutOptions {
  /**
   * 无活动超时时间（毫秒）
   * 默认 30 分钟
   */
  timeout?: number;
  /**
   * 超时前警告时间（毫秒）
   * 默认 5 分钟（会在超时前5分钟显示警告）
   */
  warningTime?: number;
  /**
   * 超时回调
   */
  onTimeout?: () => void;
  /**
   * 警告回调
   */
  onWarning?: (remainingTime: number) => void;
  /**
   * 是否启用
   * 默认 true
   */
  enabled?: boolean;
}

// 默认值：15分钟超时，2分钟警告
const DEFAULT_TIMEOUT = 15 * 60 * 1000; // 15分钟
const DEFAULT_WARNING_TIME = 2 * 60 * 1000; // 2分钟

/**
 * 用户无活动检测 Hook
 *
 * 检测用户活动（鼠标移动、键盘输入、点击、滚动等），
 * 在指定时间内无活动时触发超时回调（自动登出）
 */
export function useIdleTimeout(options: IdleTimeoutOptions = {}) {
  const {
    timeout = DEFAULT_TIMEOUT,
    warningTime = DEFAULT_WARNING_TIME,
    onTimeout,
    onWarning,
    enabled = true,
  } = options;

  const isIdle = ref(false);
  const isWarned = ref(false);
  const remainingTime = ref(timeout);

  let timeoutTimer: ReturnType<typeof setTimeout> | null = null;
  let warningTimer: ReturnType<typeof setTimeout> | null = null;
  let countdownInterval: ReturnType<typeof setInterval> | null = null;
  let lastActivityTime = Date.now();
  let warningModalInstance: ReturnType<typeof Modal.warning> | null = null;

  // 用户活动事件列表
  const activityEvents = [
    'mousedown',
    'mousemove',
    'keydown',
    'scroll',
    'touchstart',
    'click',
    'wheel',
  ];

  /**
   * 重置所有计时器
   */
  function resetTimers() {
    if (timeoutTimer) {
      clearTimeout(timeoutTimer);
      timeoutTimer = null;
    }
    if (warningTimer) {
      clearTimeout(warningTimer);
      warningTimer = null;
    }
    if (countdownInterval) {
      clearInterval(countdownInterval);
      countdownInterval = null;
    }

    // 关闭警告弹窗
    if (warningModalInstance) {
      warningModalInstance.destroy();
      warningModalInstance = null;
    }

    isIdle.value = false;
    isWarned.value = false;
    remainingTime.value = timeout;
  }

  /**
   * 启动倒计时显示
   */
  function startCountdown() {
    const endTime = lastActivityTime + timeout;

    countdownInterval = setInterval(() => {
      const now = Date.now();
      const remaining = Math.max(0, endTime - now);
      remainingTime.value = remaining;

      // Modal.warning 不支持动态更新内容，所以我们在触发时显示固定内容
      // 这里只更新 remainingTime 用于外部显示
    }, 1000);
  }

  /**
   * 显示警告弹窗
   */
  function showWarningModal() {
    if (warningModalInstance) return;

    const remainingMinutes = Math.ceil(warningTime / 60000);

    warningModalInstance = Modal.warning({
      title: '会话即将过期',
      content: `您已经 ${Math.floor((timeout - warningTime) / 60000)} 分钟没有操作了。如果在 ${remainingMinutes} 分钟内没有任何操作，系统将自动登出。`,
      okText: '继续使用',
      centered: true,
      onOk: () => {
        // 用户点击确认，重置活动时间
        handleActivity();
        warningModalInstance = null;
      },
    });
  }

  /**
   * 启动计时器
   */
  function startTimers() {
    if (!enabled) return;

    lastActivityTime = Date.now();

    // 设置警告计时器（超时前 warningTime 时间触发）
    const warningDelay = timeout - warningTime;
    if (warningDelay > 0 && warningTime > 0) {
      warningTimer = setTimeout(() => {
        isWarned.value = true;
        showWarningModal();
        onWarning?.(warningTime);
        startCountdown();
      }, warningDelay);
    }

    // 设置超时计时器
    timeoutTimer = setTimeout(() => {
      isIdle.value = true;
      resetTimers();
      onTimeout?.();
    }, timeout);
  }

  /**
   * 处理用户活动
   */
  function handleActivity() {
    if (!enabled) return;

    // 重置计时器
    resetTimers();
    startTimers();
  }

  /**
   * 使用节流函数减少事件处理频率
   */
  let throttleTimer: ReturnType<typeof setTimeout> | null = null;
  const throttledHandleActivity = () => {
    if (throttleTimer) return;

    throttleTimer = setTimeout(() => {
      handleActivity();
      throttleTimer = null;
    }, 1000); // 1秒内只处理一次
  };

  /**
   * 添加事件监听
   */
  function addEventListeners() {
    activityEvents.forEach((event) => {
      document.addEventListener(event, throttledHandleActivity, {
        passive: true,
      });
    });

    // 监听页面可见性变化
    document.addEventListener('visibilitychange', handleVisibilityChange);
  }

  /**
   * 移除事件监听
   */
  function removeEventListeners() {
    activityEvents.forEach((event) => {
      document.removeEventListener(event, throttledHandleActivity);
    });
    document.removeEventListener('visibilitychange', handleVisibilityChange);

    if (throttleTimer) {
      clearTimeout(throttleTimer);
      throttleTimer = null;
    }
  }

  /**
   * 处理页面可见性变化
   * 当页面重新可见时，检查是否已超时
   */
  function handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
      const now = Date.now();
      const elapsed = now - lastActivityTime;

      if (elapsed >= timeout) {
        // 已超时，触发登出
        isIdle.value = true;
        resetTimers();
        onTimeout?.();
      } else if (elapsed >= timeout - warningTime && !isWarned.value) {
        // 在警告时间内，显示警告
        isWarned.value = true;
        showWarningModal();
        onWarning?.(timeout - elapsed);
      }
    }
  }

  /**
   * 手动重置（可用于用户主动操作后延长会话）
   */
  function reset() {
    handleActivity();
  }

  /**
   * 暂停检测
   */
  function pause() {
    resetTimers();
    removeEventListeners();
  }

  /**
   * 恢复检测
   */
  function resume() {
    addEventListeners();
    startTimers();
  }

  onMounted(() => {
    if (enabled) {
      addEventListeners();
      startTimers();
    }
  });

  onUnmounted(() => {
    resetTimers();
    removeEventListeners();
  });

  return {
    isIdle,
    isWarned,
    remainingTime,
    reset,
    pause,
    resume,
  };
}

/**
 * 格式化剩余时间
 */
export function formatRemainingTime(ms: number): string {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);

  if (minutes > 0) {
    return `${minutes}分${seconds}秒`;
  }
  return `${seconds}秒`;
}
