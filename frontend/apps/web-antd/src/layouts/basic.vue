<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';

import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

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

import { $t } from '#/locales';
import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';
import {
  getMyNotifications,
  markAsRead as apiMarkAsRead,
  markAllAsRead as apiMarkAllAsRead,
  deleteNotification as apiDeleteNotification,
  type NotificationDTO,
} from '#/api/system/notification';
import { getApplicationDetail } from '#/api/admin/letter';
import { getApprovalDetail } from '#/api/workbench';
import { getSealApplicationDetail } from '#/api/document/seal';

const notifications = ref<NotificationItem[]>([]);
let pollingTimer: ReturnType<typeof setInterval> | null = null;
let previousUnreadCount = 0;

// 通知音效 - 使用 Web Audio API
let audioContext: AudioContext | null = null;

// 初始化通知音效
function initNotificationSound() {
  // 延迟初始化 AudioContext，等待用户交互后再创建
  // 这是因为现代浏览器要求用户交互后才能创建 AudioContext
}

// 播放通知音效 - 使用 Web Audio API 生成清脆的提示音
function playNotificationSound() {
  try {
    // 懒加载 AudioContext
    if (!audioContext) {
      audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
    
    const ctx = audioContext;
    const now = ctx.currentTime;
    
    // 创建振荡器产生声音
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);
    
    // 设置音调 - 使用两个音符创建悦耳的提示音
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(880, now);       // A5
    oscillator.frequency.setValueAtTime(1100, now + 0.1); // C#6
    
    // 设置音量包络 - 淡入淡出
    gainNode.gain.setValueAtTime(0, now);
    gainNode.gain.linearRampToValueAtTime(0.3, now + 0.02);
    gainNode.gain.linearRampToValueAtTime(0.2, now + 0.1);
    gainNode.gain.linearRampToValueAtTime(0, now + 0.3);
    
    oscillator.start(now);
    oscillator.stop(now + 0.3);
    
    console.debug('通知提示音已播放');
  } catch (e) {
    console.debug('播放通知音效失败:', e);
  }
}

// 显示浏览器通知
async function showBrowserNotification(title: string, body: string) {
  if (!('Notification' in window)) return;
  
  if (Notification.permission === 'granted') {
    new Notification(title, {
      body,
      icon: '/favicon.ico',
      tag: 'lawfirm-notification',
    });
  } else if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission();
    if (permission === 'granted') {
      new Notification(title, {
        body,
        icon: '/favicon.ico',
        tag: 'lawfirm-notification',
      });
    }
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

// 处理通知点击（特殊处理LETTER和APPROVAL类型）
async function handleNotificationClick(item: NotificationItem) {
  const notification = item.rawData as NotificationDTO;
  const currentUserId = userStore.userInfo?.id;
  
  // 如果是LETTER类型，需要先获取申请详情，然后跳转到项目详情页
  if (notification.businessType === 'LETTER' && notification.businessId) {
    try {
      // 获取出函申请详情
      const application = await getApplicationDetail(notification.businessId);
      if (application && application.matterId) {
        // 跳转到项目详情页（申请人查看自己申请的状态）
        await router.push(`/matter/detail/${application.matterId}`);
      } else {
        message.warning('无法获取出函申请信息');
      }
    } catch (error: any) {
      console.error('获取出函申请详情失败:', error);
      message.error('获取出函申请信息失败，请稍后重试');
    }
    return;
  }
  
  // 如果是APPROVAL类型（审批通知），需要区分申请人和审批人
  if (notification.businessType === 'APPROVAL' && notification.businessId) {
    try {
      // 获取审批记录详情
      const approval = await getApprovalDetail(notification.businessId);
      
      if (!approval) {
        message.warning('无法获取审批记录信息');
        return;
      }
      
      // 判断当前用户是申请人还是审批人
      const isApplicant = currentUserId === approval.applicantId;
      const isApprover = currentUserId === approval.approverId;
      
      if (isApplicant) {
        // 申请人收到审批结果通知，跳转到业务详情页
        // 根据业务类型决定跳转页面
        if (approval.businessType === 'SEAL_APPLICATION' && approval.businessId) {
          // 用印申请：获取申请详情，跳转到项目详情页
          try {
            const sealApplication = await getSealApplicationDetail(approval.businessId);
            if (sealApplication && sealApplication.matterId) {
              await router.push(`/matter/detail/${sealApplication.matterId}`);
            } else {
              // 如果没有关联项目，跳转到用印申请列表页
              await router.push('/document/seal-apply');
            }
          } catch (error: any) {
            console.error('获取用印申请详情失败:', error);
            // 降级：跳转到用印申请列表页
            await router.push('/document/seal-apply');
          }
        } else if (approval.businessType === 'CONTRACT' && approval.businessId) {
          // 合同审批：跳转到合同详情页
          await router.push(`/matter/contract?id=${approval.businessId}`);
        } else {
          // 其他类型：跳转到审批中心查看详情
          await router.push(`/workbench/approval`);
        }
      } else if (isApprover && approval.status === 'PENDING') {
        // 审批人收到待审批通知，跳转到审批中心
        await router.push('/workbench/approval');
      } else {
        // 其他情况：跳转到审批中心
        await router.push('/workbench/approval');
      }
    } catch (error: any) {
      console.error('获取审批记录详情失败:', error);
      message.error('获取审批记录信息失败，请稍后重试');
    }
    return;
  }
  
  // 其他类型由通知组件默认处理（通过link属性）
}

// 检查路由是否存在
function checkRouteExists(path: string): boolean {
  try {
    const route = router.resolve(path);
    // 如果路由解析后是404页面，说明路由不存在
    return route.name !== 'FallbackNotFound';
  } catch {
    return false;
  }
}

// 检查用户是否有权限访问路由
function checkRoutePermission(path: string): boolean {
  // 获取路由对应的权限码
  const permissionMap: Record<string, string[]> = {
    '/admin/letter': ['admin:letter:list'],
    '/matter/contract': ['matter:contract:list'],
    '/workbench/approval': ['workbench:approval:list'],
    '/hr/payroll': ['hr:payroll:list'],
  };
  
  // 对于动态路由（如 /matter/detail/:id），不需要权限检查，因为已经在路由守卫中处理
  if (path.startsWith('/matter/detail/')) {
    return true;
  }
  
  const requiredPermissions = permissionMap[path.split('?')[0]]; // 移除查询参数
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true; // 如果没有配置权限要求，默认允许访问
  }
  
  // 检查用户是否有任一权限
  return requiredPermissions.some(permission => 
    accessStore.accessCodes.includes(permission)
  );
}

// 安全跳转
async function safeNavigate(link: string | undefined) {
  if (!link) {
    message.warning('该通知暂不支持跳转');
    return;
  }
  
  // 移除查询参数检查路由
  const pathWithoutQuery = link.split('?')[0];
  
  // 检查路由是否存在
  if (!checkRouteExists(pathWithoutQuery)) {
    message.warning('无法跳转到该页面，页面不存在或未配置');
    return;
  }
  
  // 检查权限
  if (!checkRoutePermission(pathWithoutQuery)) {
    message.warning('您没有权限访问该页面');
    return;
  }
  
  // 执行跳转
  try {
    await router.push(link);
  } catch (error: any) {
    console.error('路由跳转失败:', error);
    // 如果跳转失败，尝试跳转到404页面或显示错误提示
    if (error.name === 'NavigationFailure' || error.message?.includes('404')) {
      message.warning('无法跳转到该页面，可能没有权限或页面不存在');
    } else {
      message.error('跳转失败，请稍后重试');
    }
  }
}

// 格式化时间
function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
}

// 加载通知
async function loadNotifications(isInitial = false) {
  try {
    const res = await getMyNotifications({ pageNum: 1, pageSize: 20 });
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
      
      // 计算未读数量
      const currentUnreadCount = newNotifications.filter((n: NotificationItem) => !n.isRead).length;
      
      // 如果有新的未读通知且不是初始加载，播放提示音
      if (!isInitial && currentUnreadCount > previousUnreadCount) {
        playNotificationSound();
        
        // 获取最新的未读通知并显示浏览器通知
        const latestUnread = newNotifications.find((n: NotificationItem) => !n.isRead);
        if (latestUnread) {
          showBrowserNotification(
            latestUnread.title || '新通知',
            latestUnread.message || '您有新的通知'
          );
        }
      }
      
      previousUnreadCount = currentUnreadCount;
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
  pollingTimer = setInterval(() => loadNotifications(false), 30000);
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
  if ('Notification' in window && Notification.permission === 'default') {
    try {
      await Notification.requestPermission();
    } catch (e) {
      console.debug('请求通知权限失败:', e);
    }
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

// 用户手册地址（docs 应用部署后的地址，开发时指向本地 6173 端口）
const USER_MANUAL_URL = import.meta.env.VITE_USER_MANUAL_URL || 'http://localhost:6173/';

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
      openWindow(USER_MANUAL_URL, {
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
  // 清空本地列表
  notifications.value = [];
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
</template>
