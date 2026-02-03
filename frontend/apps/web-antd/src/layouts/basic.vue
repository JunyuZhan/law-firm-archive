<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';

import type { NotificationDTO } from '#/api/system/notification';

import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { AuthenticationLoginExpiredModal } from '@vben/common-ui';
import { useWatermark } from '@vben/hooks';
import { BookOpenText } from '@vben/icons';
import {
  BasicLayout,
  LockScreen,
  Notification,
  UserDropdown,
} from '@vben/layouts';
import { preferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

import { message } from 'ant-design-vue';

import {
  deleteNotification as apiDeleteNotification,
  deleteReadNotifications as apiDeleteReadNotifications,
  markAllAsRead as apiMarkAllAsRead,
  markAsRead as apiMarkAsRead,
  getMyNotifications,
} from '#/api/system/notification';
import FloatingTimer from '#/components/timer/FloatingTimer.vue';
import VersionUpdateBanner from '#/components/VersionUpdateBanner/index.vue';
import { useIdleTimeout } from '#/hooks/useIdleTimeout';
import { $t } from '#/locales';
import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';

const notifications = ref<NotificationItem[]>([]);
let pollingTimer: null | ReturnType<typeof setInterval> = null;
// 用于跟踪上次未读数量（未来功能扩展用）
// let _previousUnreadCount = 0;

// 已弹窗提醒过的通知ID集合（存储在localStorage，避免重复弹窗）
const NOTIFIED_IDS_KEY = 'lawfirm_notified_notification_ids';

function getNotifiedIds(): Set<string> {
  try {
    const stored = localStorage.getItem(NOTIFIED_IDS_KEY);
    return stored ? new Set(JSON.parse(stored)) : new Set();
  } catch {
    return new Set();
  }
}

function addNotifiedId(id: number | string) {
  const ids = getNotifiedIds();
  ids.add(String(id));
  // 只保留最近100个，避免localStorage过大
  const arr = [...ids].slice(-100);
  localStorage.setItem(NOTIFIED_IDS_KEY, JSON.stringify(arr));
}

function hasBeenNotified(id: number | string): boolean {
  return getNotifiedIds().has(String(id));
}

// 通知音效 - 使用 Web Audio API
let audioContext: AudioContext | null = null;
let hasUserInteracted = false;

// 初始化通知音效 - 监听用户交互以启用音效
function initNotificationSound() {
  // 监听用户交互事件，一旦有交互就标记为可以播放音效
  const enableAudio = () => {
    hasUserInteracted = true;
    // 如果 AudioContext 已创建但被暂停，尝试恢复
    if (audioContext && audioContext.state === 'suspended') {
      audioContext.resume().catch(() => {});
    }
  };

  // 只需要第一次交互
  const events = ['click', 'touchstart', 'keydown'];
  const removeListeners = () => {
    events.forEach((event) => {
      document.removeEventListener(event, handleInteraction);
    });
  };

  const handleInteraction = () => {
    enableAudio();
    removeListeners();
  };

  events.forEach((event) => {
    document.addEventListener(event, handleInteraction, { once: true });
  });
}

// 播放通知音效 - 使用 Web Audio API 生成清脆的提示音
async function playNotificationSound() {
  // 如果用户还没有与页面交互，不播放音效（浏览器安全限制）
  if (!hasUserInteracted) {
    console.warn('等待用户交互后才能播放通知音效');
    return;
  }

  try {
    // 懒加载 AudioContext
    if (!audioContext) {
      const AudioContextClass =
        window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextClass) {
        console.warn('浏览器不支持 AudioContext');
        return;
      }
      audioContext = new AudioContextClass();
    }

    const ctx = audioContext;

    // 确保 AudioContext 处于运行状态
    if (ctx.state === 'suspended') {
      await ctx.resume();
    }

    const now = ctx.currentTime;

    // 创建振荡器产生声音
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();

    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    // 设置音调 - 使用两个音符创建悦耳的提示音
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(880, now); // A5
    oscillator.frequency.setValueAtTime(1100, now + 0.1); // C#6

    // 设置音量包络 - 淡入淡出
    gainNode.gain.setValueAtTime(0, now);
    gainNode.gain.linearRampToValueAtTime(0.3, now + 0.02);
    gainNode.gain.linearRampToValueAtTime(0.2, now + 0.1);
    gainNode.gain.linearRampToValueAtTime(0, now + 0.3);

    oscillator.start(now);
    oscillator.stop(now + 0.3);

    console.warn('通知提示音已播放');
  } catch (error) {
    console.warn('播放通知音效失败:', error);
  }
}

// 显示浏览器通知
async function showBrowserNotification(title: string, body: string) {
  try {
    // 检查浏览器是否支持 Notification API
    if (!('Notification' in window)) return;

    // 检查 Notification 对象是否完整可用
    if (Notification.permission === undefined) return;

    if (Notification.permission === 'granted') {
      // eslint-disable-next-line no-new
      new Notification(title, {
        body,
        icon: '/favicon.ico',
        tag: 'lawfirm-notification',
      });
    } else if (Notification.permission !== 'denied') {
      // 检查 requestPermission 是否可用
      if (typeof Notification.requestPermission !== 'function') {
        console.warn('Notification.requestPermission 不可用');
        return;
      }
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        // eslint-disable-next-line no-new
        new Notification(title, {
          body,
          icon: '/favicon.ico',
          tag: 'lawfirm-notification',
        });
      }
    }
  } catch (error) {
    console.warn('浏览器通知功能不可用:', error);
  }
}

// 获取通知图标
function getNotificationAvatar(type: string) {
  const avatarMap: Record<string, string> = {
    SYSTEM: 'https://avatar.vercel.sh/system?text=系统',
    APPROVAL: 'https://avatar.vercel.sh/approval?text=审批',
    TASK: 'https://avatar.vercel.sh/task?text=任务',
    REMINDER: 'https://avatar.vercel.sh/reminder?text=提醒',
    MATTER: 'https://avatar.vercel.sh/matter?text=项目',
    CONTRACT: 'https://avatar.vercel.sh/contract?text=合同',
  };
  return avatarMap[type] || 'https://avatar.vercel.sh/default?text=通知';
}

// 获取业务链接
function getBusinessLink(notification: NotificationDTO): string | undefined {
  if (!notification.businessType || !notification.businessId) return undefined;

  const linkMap: Record<string, (id?: number) => string> = {
    // LETTER类型：使用特殊标记，点击时异步获取申请详情后跳转到项目详情页
    LETTER: (id) => `letter:${id}`, // 特殊标记，不会直接跳转
    MATTER: (id) => `/matter/detail/${id}`,
    CONTRACT: (id) => `/matter/contract${id ? `?id=${id}` : ''}`,
    TASK: (id) => `/matter/detail/${id}`,
    // APPROVAL类型：使用特殊标记，点击时根据申请人和审批人身份决定跳转
    APPROVAL: (id) => `approval:${id}`, // 特殊标记，不会直接跳转
    DEADLINE: (id) => `/matter/detail/${id}`, // 期限关联项目
    SCHEDULE: (id) => `/matter/detail/${id}`, // 日程关联项目
    PAYROLL: (id) => `/hr/payroll${id ? `?id=${id}` : ''}`, // 工资通知
  };

  const linkGenerator = linkMap[notification.businessType];
  if (!linkGenerator) return undefined;

  return linkGenerator(notification.businessId);
}

// 处理通知点击 - 简化逻辑，只标记为已读，不跳转
async function handleNotificationClick(
  item: NotificationItem & { rawData?: NotificationDTO },
) {
  // 点击通知时，只标记为已读，不进行跳转
  // 如果需要查看详情，用户可以从相应的菜单入口进入
  if (!item.isRead && item.id) {
    await markRead(item.id);
  }
}

// 安全跳转（已废弃，使用 handleNotificationClick 处理特殊类型）
// async function safeNavigate(link: string | undefined) {
//   if (!link) {
//     message.warning('该通知暂不支持跳转');
//     return;
//   }
//
//   // 移除查询参数检查路由
//   const pathWithoutQuery = link.split('?')[0] || link;
//
//   // 检查路由是否存在
//   if (!checkRouteExists(pathWithoutQuery)) {
//     message.warning('无法跳转到该页面，页面不存在或未配置');
//     return;
//   }
//
//   // 检查权限
//   if (!checkRoutePermission(pathWithoutQuery)) {
//     message.warning('您没有权限访问该页面');
//     return;
//   }
//
//   // 执行跳转
//   try {
//     await router.push(link);
//   } catch (error: any) {
//     console.error('路由跳转失败:', error);
//     // 如果跳转失败，尝试跳转到404页面或显示错误提示
//     if (error.name === 'NavigationFailure' || error.message?.includes('404')) {
//       message.warning('无法跳转到该页面，可能没有权限或页面不存在');
//     } else {
//       message.error('跳转失败，请稍后重试');
//     }
//   }
// }

// 格式化时间
function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60_000);
  const hours = Math.floor(diff / 3_600_000);
  const days = Math.floor(diff / 86_400_000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
}

// 加载通知 - 只加载未读消息
async function loadNotifications(_isInitial = false) {
  try {
    // 只加载未读消息，避免已读消息重复出现
    const res = await getMyNotifications({
      pageNum: 1,
      pageSize: 20,
      isRead: false, // 只加载未读消息
    });

    if (res && res.records) {
      const newNotifications = res.records.map((n: NotificationDTO) => ({
        id: n.id,
        avatar: getNotificationAvatar(n.type),
        date: formatDate(n.createdAt),
        isRead: n.isRead,
        message: n.content,
        title: n.title,
        link: getBusinessLink(n),
        rawData: n,
      }));

      // 找出真正的"新"通知：未读且未弹窗提醒过的
      const trulyNewNotifications = newNotifications.filter(
        (n: NotificationItem) => !n.isRead && !hasBeenNotified(n.id),
      );

      // 只对真正的新通知播放提示音和弹窗
      if (trulyNewNotifications.length > 0) {
        playNotificationSound();

        // 显示浏览器通知
        const latestNew = trulyNewNotifications[0];
        if (latestNew) {
          showBrowserNotification(
            latestNew.title || '新通知',
            latestNew.message || '您有新的通知',
          );
          // 标记为已弹窗提醒
          addNotifiedId(latestNew.id);
        }
      }

      notifications.value = newNotifications;
    }
  } catch (error) {
    // 静默处理错误，避免影响用户体验
    console.warn('加载通知失败:', error);
  }
}

// 启动轮询
function startPolling() {
  // 每30秒刷新一次通知（更频繁以便及时发现新通知）
  pollingTimer = setInterval(() => loadNotifications(false), 30_000);
}

// 停止轮询
function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

// 请求通知权限
async function requestNotificationPermission() {
  try {
    // 检查浏览器是否完整支持 Notification API
    if (
      !('Notification' in window) ||
      Notification.permission === undefined ||
      typeof Notification.requestPermission !== 'function'
    ) {
      return;
    }

    if (Notification.permission === 'default') {
      await Notification.requestPermission();
    }
  } catch (error) {
    console.warn('请求通知权限失败:', error);
  }
}

onMounted(() => {
  initNotificationSound();
  loadNotifications(true); // 初始加载
  startPolling();
  requestNotificationPermission();
});

onUnmounted(() => {
  stopPolling();
});

const router = useRouter();
const userStore = useUserStore();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const { destroyWatermark, updateWatermark } = useWatermark();
const showDot = computed(() =>
  notifications.value.some((item) => !item.isRead),
);

// 无活动自动登出（30分钟无操作）
useIdleTimeout({
  timeout: 30 * 60 * 1000, // 30分钟
  warningTime: 5 * 60 * 1000, // 提前5分钟警告
  onTimeout: async () => {
    message.warning('由于长时间未操作，系统已自动登出');
    await authStore.logout(false);
  },
});

// 用户手册地址
// 开发模式：http://localhost:6173/（无 base 路径）
// 生产模式：由环境变量 VITE_USER_MANUAL_URL 配置，默认 /docs/
const USER_MANUAL_URL =
  import.meta.env.VITE_USER_MANUAL_URL ||
  (import.meta.env.DEV ? 'http://localhost:6173/' : '/docs/');

const menus = computed(() => [
  {
    handler: () => {
      router.push({ name: 'Profile' });
    },
    icon: 'lucide:user',
    text: $t('page.auth.profile'),
  },
  {
    handler: () => {
      const token = accessStore.accessToken;
      const url = new URL(USER_MANUAL_URL, window.location.origin);
      if (token) {
        url.searchParams.set('token', token);
      }
      openWindow(url.toString(), {
        target: '_blank',
      });
    },
    icon: BookOpenText,
    text: '用户手册',
  },
]);

const avatar = computed(() => {
  return userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
});

async function handleLogout() {
  await authStore.logout(false);
}

async function handleNoticeClear() {
  // 清除所有当前显示的消息（包括已读和未读）
  if (notifications.value.length === 0) {
    message.info('没有消息需要清除');
    return;
  }

  const totalCount = notifications.value.length;

  try {
    // 先批量删除已读消息
    const readNotifications = notifications.value.filter((item) => item.isRead);
    if (readNotifications.length > 0) {
      try {
        await apiDeleteReadNotifications();
      } catch (error) {
        console.warn('批量删除已读通知失败，尝试逐个删除:', error);
        // 如果批量删除失败，尝试逐个删除已读消息
        await Promise.all(
          readNotifications.map((item) =>
            apiDeleteNotification(Number(item.id)).catch((error_) => {
              console.warn(`删除已读通知失败 (id: ${item.id}):`, error_);
            }),
          ),
        );
      }
    }

    // 删除未读消息（逐个删除）
    const unreadNotifications = notifications.value.filter(
      (item) => !item.isRead,
    );
    if (unreadNotifications.length > 0) {
      await Promise.all(
        unreadNotifications.map((item) =>
          apiDeleteNotification(Number(item.id)).catch((error) => {
            console.warn(`删除未读通知失败 (id: ${item.id}):`, error);
          }),
        ),
      );
    }

    // 清空本地列表
    notifications.value = [];

    message.success(`已清除 ${totalCount} 条消息`);

    // 重新加载通知列表，确保数据同步
    await loadNotifications(false);
  } catch (error) {
    console.error('清除消息失败:', error);
    message.error('清除消息失败，请稍后重试');
  }
}

async function markRead(id: number | string) {
  const item = notifications.value.find((item) => item.id === id);
  if (item) {
    item.isRead = true;
    try {
      await apiMarkAsRead(Number(id));
    } catch (error) {
      console.warn('标记已读失败:', error);
    }
  }
}

async function remove(id: number | string) {
  notifications.value = notifications.value.filter((item) => item.id !== id);
  try {
    await apiDeleteNotification(Number(id));
  } catch (error) {
    console.warn('删除通知失败:', error);
  }
}

async function handleMakeAll() {
  notifications.value.forEach((item) => (item.isRead = true));
  try {
    await apiMarkAllAsRead();
  } catch (error) {
    console.warn('全部标记已读失败:', error);
  }
}
watch(
  () => ({
    enable: preferences.app.watermark,
    content: preferences.app.watermarkContent,
  }),
  async ({ enable, content }) => {
    if (enable) {
      await updateWatermark({
        content:
          content ||
          `${userStore.userInfo?.username} - ${userStore.userInfo?.realName}`,
      });
    } else {
      destroyWatermark();
    }
  },
  {
    immediate: true,
  },
);
</script>

<template>
  <BasicLayout @clear-preferences-and-logout="handleLogout">
    <template #user-dropdown>
      <UserDropdown
        :avatar
        :menus
        :text="userStore.userInfo?.realName"
        :description="userStore.userInfo?.email || ''"
        @logout="handleLogout"
      />
    </template>
    <template #notification>
      <Notification
        :dot="showDot"
        :notifications="notifications"
        @clear="handleNoticeClear"
        @read="(item) => item.id && markRead(item.id)"
        @remove="(item) => item.id && remove(item.id)"
        @make-all="handleMakeAll"
        @click="handleNotificationClick"
      />
    </template>
    <template #extra>
      <AuthenticationLoginExpiredModal
        v-model:open="accessStore.loginExpired"
        :avatar
      >
        <LoginForm />
      </AuthenticationLoginExpiredModal>
    </template>
    <template #lock-screen>
      <LockScreen :avatar @to-login="handleLogout" />
    </template>
  </BasicLayout>

  <!-- 悬浮计时器 -->
  <FloatingTimer />

  <!-- 版本更新提示 -->
  <VersionUpdateBanner />
</template>
