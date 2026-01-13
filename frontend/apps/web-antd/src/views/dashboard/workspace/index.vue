<script lang="ts" setup>
import type {
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
} from '@vben/common-ui';

import type { ScheduleDTO } from '#/api/matter/schedule';
import type { TaskDTO } from '#/api/matter/types';
import type { ApprovalDTO } from '#/api/workbench';

import { computed, onActivated, onDeactivated, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  WorkbenchHeader,
  WorkbenchQuickNav,
  WorkbenchTodo,
} from '@vben/common-ui';
import { preferences } from '@vben/preferences';
import { useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

import {
  Avatar,
  Card,
  Col,
  Empty,
  List,
  ListItem,
  ListItemMeta,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Tag,
  Timeline,
  TimelineItem,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getMyTodoTasks, getMyUpcomingSchedules } from '#/api/matter';
import type { DeadlineDTO } from '#/api/matter/deadline';
import { getMyUpcomingDeadlines } from '#/api/matter/deadline';
import type { AnnouncementDTO } from '#/api/system/announcement';
import { getValidAnnouncements, getAnnouncementById, ANNOUNCEMENT_TYPE_OPTIONS } from '#/api/system/announcement';

// HTML转义函数，防止XSS攻击
function escapeHtml(text: string): string {
  if (!text) return '';
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return text.replace(/[&<>"']/g, (m) => map[m] || m);
}
import {
  getMyApprovedHistory,
  getMyInitiatedApprovals,
  getPendingApprovals,
  getRecentProjects,
  getWorkbenchStats,
} from '#/api/workbench';

const userStore = useUserStore();
const router = useRouter();

// 最新动态展开状态
const trendsExpanded = ref(false);
const allTrendItems = ref<WorkbenchTrendItem[]>([]);

// 统计数据
const stats = ref({
  // 通用
  taskCount: 0,
  roleType: 'LAWYER' as 'LAWYER' | 'FINANCE' | 'ADMIN_STAFF',
  // 律师相关
  matterCount: 0,
  clientCount: 0,
  timesheetHours: 0,
  // 财务相关
  pendingPaymentCount: 0,
  pendingInvoiceCount: 0,
  pendingExpenseCount: 0,
  monthlyReceivedAmount: 0,
  // 行政相关
  pendingLetterCount: 0,
  pendingSealCount: 0,
  pendingLeaveCount: 0,
  pendingAssetCount: 0,
});

// 待审批数量
const pendingApprovalCount = ref(0);
const pendingApprovals = ref<ApprovalDTO[]>([]);

// 近期日程
const upcomingSchedules = ref<ScheduleDTO[]>([]);

// 即将到期的期限
const upcomingDeadlines = ref<DeadlineDTO[]>([]);

// 待办任务列表
const todoItems = ref<WorkbenchTodoItem[]>([]);

// 系统公告
const announcements = ref<AnnouncementDTO[]>([]);

// 公告详情弹窗
const announcementDetailVisible = ref(false);
const announcementDetailLoading = ref(false);
const currentAnnouncement = ref<AnnouncementDTO | null>(null);

// 公告滚动相关
const currentAnnouncementIndex = ref(0);
let announcementTimer: ReturnType<typeof setInterval> | null = null;

// 开始公告滚动
function startAnnouncementScroll() {
  if (announcementTimer) {
    clearInterval(announcementTimer);
  }
  if (announcements.value.length > 1) {
    announcementTimer = setInterval(() => {
      currentAnnouncementIndex.value = (currentAnnouncementIndex.value + 1) % announcements.value.length;
    }, 4000); // 每4秒滚动一次
  }
}

// 停止公告滚动
function stopAnnouncementScroll() {
  if (announcementTimer) {
    clearInterval(announcementTimer);
    announcementTimer = null;
  }
}

// 快捷导航
const quickNavItems: WorkbenchQuickNavItem[] = [
  {
    color: '#1fdaca',
    icon: 'ion:home-outline',
    title: '首页',
    url: '/',
  },
  {
    color: '#ff6b6b',
    icon: 'ion:checkbox-outline',
    title: '审批中心',
    url: '/dashboard/approval',
  },
  {
    color: '#bf0c2c',
    icon: 'ion:people-outline',
    title: '客户管理',
    url: '/crm/client',
  },
  {
    color: '#e18525',
    icon: 'ion:briefcase-outline',
    title: '项目管理',
    url: '/matter/list',
  },
  {
    color: '#3fb27f',
    icon: 'ion:document-text-outline',
    title: '财务管理',
    url: '/finance/contract',
  },
  {
    color: '#00d8ff',
    icon: 'ion:checkmark-circle-outline',
    title: '任务管理',
    url: '/matter/task',
  },
];

// 最新动态
const trendItems = ref<WorkbenchTrendItem[]>([]);

// 加载统计数据
async function loadStats() {
  try {
    const data = await getWorkbenchStats();
    stats.value = {
      // 通用
      taskCount: data.taskCount || 0,
      roleType: data.roleType || 'LAWYER',
      // 律师相关
      matterCount: data.matterCount || 0,
      clientCount: data.clientCount || 0,
      timesheetHours: data.timesheetHours || 0,
      // 财务相关
      pendingPaymentCount: data.pendingPaymentCount || 0,
      pendingInvoiceCount: data.pendingInvoiceCount || 0,
      pendingExpenseCount: data.pendingExpenseCount || 0,
      monthlyReceivedAmount: data.monthlyReceivedAmount || 0,
      // 行政相关
      pendingLetterCount: data.pendingLetterCount || 0,
      pendingSealCount: data.pendingSealCount || 0,
      pendingLeaveCount: data.pendingLeaveCount || 0,
      pendingAssetCount: data.pendingAssetCount || 0,
    };
  } catch {
    // 静默处理
  }
}

// 加载待办任务
async function loadTodoTasks() {
  try {
    const tasks = await getMyTodoTasks();
    todoItems.value = tasks.map((task: TaskDTO) => ({
      completed: task.status === 'COMPLETED',
      content: task.description || '暂无描述',
      date: task.dueDate || task.createdAt || '',
      title: task.title,
    }));
  } catch {
    // 静默处理
  }
}

// 加载待审批数据
async function loadPendingApprovals() {
  try {
    const data = await getPendingApprovals();
    pendingApprovals.value = data || [];
    pendingApprovalCount.value = pendingApprovals.value.length;
  } catch {
    // 静默处理
  }
}

// 加载近期日程
async function loadUpcomingSchedules() {
  try {
    const data = await getMyUpcomingSchedules(7, 5);
    upcomingSchedules.value = data || [];
  } catch {
    // 静默处理
  }
}

// 加载即将到期的期限
async function loadUpcomingDeadlines() {
  try {
    const data = await getMyUpcomingDeadlines(14, 5);
    upcomingDeadlines.value = data || [];
  } catch {
    // 静默处理
  }
}

// 格式化期限剩余天数
function formatDeadlineDays(deadline: DeadlineDTO) {
  const today = dayjs();
  const deadlineDate = dayjs(deadline.deadlineDate);
  const days = deadlineDate.diff(today, 'day');
  if (days < 0) return '已过期';
  if (days === 0) return '今天到期';
  if (days === 1) return '明天到期';
  return `${days}天后到期`;
}

// 获取期限紧急程度颜色
function getDeadlineColor(deadline: DeadlineDTO) {
  const today = dayjs();
  const deadlineDate = dayjs(deadline.deadlineDate);
  const days = deadlineDate.diff(today, 'day');
  if (days <= 0) return '#ff4d4f'; // 红色 - 已过期或今天
  if (days <= 3) return '#fa8c16'; // 橙色 - 3天内
  if (days <= 7) return '#faad14'; // 黄色 - 7天内
  return '#52c41a'; // 绿色 - 7天以上
}

// 格式化日程时间
function formatScheduleTime(schedule: ScheduleDTO) {
  const start = dayjs(schedule.startTime);
  const today = dayjs().startOf('day');
  const tomorrow = today.add(1, 'day');

  let dateLabel = '';
  if (start.isSame(today, 'day')) {
    dateLabel = '今天';
  } else if (start.isSame(tomorrow, 'day')) {
    dateLabel = '明天';
  } else {
    dateLabel = start.format('MM/DD');
  }

  if (schedule.allDay) {
    return `${dateLabel} 全天`;
  }
  return `${dateLabel} ${start.format('HH:mm')}`;
}

// 获取日程类型颜色
function getScheduleTypeColor(type: string) {
  const colorMap: Record<string, string> = {
    COURT: '#f5222d',
    MEETING: '#1890ff',
    DEADLINE: '#fa8c16',
    APPOINTMENT: '#52c41a',
    OTHER: '#722ed1',
  };
  return colorMap[type] || '#1890ff';
}

// 跳转到审批中心
function goToApproval() {
  router.push('/dashboard/approval');
}

// 格式化相对时间
function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '未知时间';

  const date = dayjs(dateStr);
  const now = dayjs();
  const diffMinutes = now.diff(date, 'minute');
  const diffHours = now.diff(date, 'hour');
  const diffDays = now.diff(date, 'day');

  if (diffMinutes < 1) return '刚刚';
  if (diffMinutes < 60) return `${diffMinutes}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays < 7) return `${diffDays}天前`;

  return date.format('MM/DD HH:mm');
}

// 加载最新动态
async function loadTrends() {
  try {
    // 使用扩展类型，包含原始时间戳用于排序
    interface TrendItemWithTimestamp extends WorkbenchTrendItem {
      timestamp: number;
    }
    const trends: TrendItemWithTimestamp[] = [];

    // 1. 获取最近项目（取前5条）
    try {
      const recentProjects = await getRecentProjects();
      recentProjects.slice(0, 5).forEach((project) => {
        const timestamp = project.lastUpdateTime
          ? dayjs(project.lastUpdateTime).valueOf()
          : Date.now();
        trends.push({
          avatar: userStore.userInfo?.avatar || preferences.app.defaultAvatar,
          content: `创建了新项目 <span class="text-primary">${escapeHtml(project.matterName || '')}</span>`,
          date: formatRelativeTime(project.lastUpdateTime),
          title: userStore.userInfo?.realName || '我',
          timestamp,
        });
      });
    } catch {
      // 加载最近项目失败，静默处理
    }

    // 2. 获取审批历史（取前5条）
    try {
      const approvedHistory = await getMyApprovedHistory();
      approvedHistory.slice(0, 5).forEach((approval) => {
        const statusText =
          approval.status === 'APPROVED' ? '审批通过了' : '拒绝了';

        // 权限安全：显示审批人信息（如果是管理员看到其他用户的审批）
        // 如果审批人是当前用户，显示"我"；否则显示审批人姓名
        const isCurrentUserApprover =
          String(approval.approverId) === String(userStore.userInfo?.userId);
        const approverName = isCurrentUserApprover
          ? userStore.userInfo?.realName || '我'
          : approval.approverName || '用户';
        // ApprovalDTO 中没有 approverAvatar 字段，使用默认头像
        const approverAvatar = isCurrentUserApprover
          ? userStore.userInfo?.avatar || preferences.app.defaultAvatar
          : preferences.app.defaultAvatar;

        // 动态内容：明确显示审批人和申请人的关系
        const businessTitle =
          approval.businessTitle || approval.businessTypeName || '审批事项';
        const applicantName = approval.applicantName || '用户';

        const timestamp = approval.approvedAt
          ? dayjs(approval.approvedAt).valueOf()
          : approval.updatedAt
            ? dayjs(approval.updatedAt).valueOf()
            : Date.now();

        trends.push({
          avatar: approverAvatar,
          content: `${statusText} <span class="text-primary">${escapeHtml(applicantName)}</span> 发起的 <span class="text-primary">${escapeHtml(businessTitle)}</span>`,
          date: formatRelativeTime(approval.approvedAt || approval.updatedAt),
          title: approverName,
          timestamp,
        });
      });
    } catch {
      // 加载审批历史失败，静默处理
    }

    // 3. 获取我发起的审批（取前5条，状态为已通过或已拒绝）
    try {
      const myInitiated = await getMyInitiatedApprovals();
      myInitiated
        .filter((a) => a.status === 'APPROVED' || a.status === 'REJECTED')
        .slice(0, 5)
        .forEach((approval) => {
          const statusText =
            approval.status === 'APPROVED' ? '已通过' : '已拒绝';
          const approverName = approval.approverName || '审批人';

          const timestamp = approval.approvedAt
            ? dayjs(approval.approvedAt).valueOf()
            : approval.updatedAt
              ? dayjs(approval.updatedAt).valueOf()
              : Date.now();

          trends.push({
            avatar: userStore.userInfo?.avatar || preferences.app.defaultAvatar,
            content: `我发起的 <span class="text-primary">${escapeHtml(approval.businessTitle || approval.businessTypeName || '')}</span> 被 <span class="text-primary">${escapeHtml(approverName)}</span> ${statusText}`,
            date: formatRelativeTime(approval.approvedAt || approval.updatedAt),
            title: userStore.userInfo?.realName || '我',
            timestamp,
          });
        });
    } catch {
      // 加载我发起的审批失败，静默处理
    }

    // 按时间戳排序（最新的在前）
    trends.sort((a, b) => b.timestamp - a.timestamp);

    // 保存所有动态（移除 timestamp 字段）
    allTrendItems.value = trends.map(({ timestamp, ...item }) => item);

    // 默认显示前5条
    trendItems.value = allTrendItems.value.slice(0, 5);

    // 如果没有数据，显示提示
    if (allTrendItems.value.length === 0) {
      allTrendItems.value = [
        {
          avatar: preferences.app.defaultAvatar,
          content: '暂无最新动态',
          date: '刚刚',
          title: '系统',
        },
      ];
      trendItems.value = allTrendItems.value;
    }
  } catch {
    trendItems.value = [];
    allTrendItems.value = [];
  }
}

// 展开/收起动态
function toggleTrends() {
  trendsExpanded.value = !trendsExpanded.value;
  trendItems.value = trendsExpanded.value
    ? allTrendItems.value
    : allTrendItems.value.slice(0, 5);
}

// 是否有更多动态可展开
const hasMoreTrends = computed(() => allTrendItems.value.length > 5);

// 获取问候语（根据时间动态显示）
function getGreeting(): { action: string; greeting: string } {
  const hour = dayjs().hour();
  if (hour >= 5 && hour < 12) {
    return { greeting: '早安', action: '开始您一天的工作吧！' };
  } else if (hour >= 12 && hour < 14) {
    return { greeting: '午安', action: '继续您的工作吧！' };
  } else if (hour >= 14 && hour < 18) {
    return { greeting: '下午好', action: '继续您的工作吧！' };
  } else if (hour >= 18 && hour < 22) {
    return { greeting: '晚上好', action: '辛苦了，继续加油！' };
  } else {
    return { greeting: '夜深了', action: '注意休息，保重身体！' };
  }
}

// 导航方法
function navTo(nav: WorkbenchQuickNavItem) {
  if (nav.url?.startsWith('http')) {
    openWindow(nav.url);
    return;
  }
  if (nav.url?.startsWith('/')) {
    router.push(nav.url).catch(() => {
      // 导航失败，静默处理
    });
  }
}

// 加载系统公告
async function loadAnnouncements() {
  try {
    announcements.value = await getValidAnnouncements(10);
    // 加载完成后开始滚动
    startAnnouncementScroll();
  } catch {
    // 加载公告失败，静默处理
  }
}

// 获取公告类型颜色
function getAnnouncementTypeColor(type: string): string {
  const option = ANNOUNCEMENT_TYPE_OPTIONS.find((opt) => opt.value === type);
  return option?.color || 'default';
}

// 获取公告类型名称
function getAnnouncementTypeName(type: string): string {
  const option = ANNOUNCEMENT_TYPE_OPTIONS.find((opt) => opt.value === type);
  return option?.label || type;
}

// 查看公告详情
async function handleViewAnnouncement(announcement: AnnouncementDTO) {
  announcementDetailVisible.value = true;
  announcementDetailLoading.value = true;
  try {
    const detail = await getAnnouncementById(announcement.id);
    currentAnnouncement.value = detail;
  } catch {
    // 如果获取详情失败，使用列表数据
    currentAnnouncement.value = announcement;
  } finally {
    announcementDetailLoading.value = false;
  }
}

// 格式化日期时间
function formatDateTime(date: string | null | undefined): string {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}

onMounted(() => {
  loadStats();
  loadTodoTasks();
  loadPendingApprovals();
  loadUpcomingSchedules();
  loadUpcomingDeadlines();
  loadTrends();
  loadAnnouncements();
});

// 页面激活时刷新数据（用于 keep-alive 场景）
onActivated(() => {
  loadStats();
  loadTodoTasks();
  loadPendingApprovals();
  loadUpcomingSchedules();
  loadUpcomingDeadlines();
  loadTrends();
  loadAnnouncements();
});

// 页面失活时停止滚动
onDeactivated(() => {
  stopAnnouncementScroll();
});

// 组件卸载时清理定时器
onUnmounted(() => {
  stopAnnouncementScroll();
});
</script>

<template>
  <div class="p-5">
    <!-- WorkbenchHeader 容器，包含公告 -->
    <div class="workbench-header-wrapper">
      <WorkbenchHeader
        :avatar="userStore.userInfo?.avatar || preferences.app.defaultAvatar"
      >
        <template #title>
          {{ getGreeting().greeting }}, {{ userStore.userInfo?.realName }},
          {{ getGreeting().action }}
        </template>
        <template #description>
          <template v-if="stats.roleType === 'LAWYER'">
            您有 {{ stats.taskCount }} 个待办任务，本月工时
            {{ stats.timesheetHours.toFixed(1) }} 小时
          </template>
          <template v-else-if="stats.roleType === 'FINANCE'">
            您有 {{ stats.taskCount }} 个待办任务，本月已收
            ¥{{ stats.monthlyReceivedAmount.toLocaleString() }}
          </template>
          <template v-else>
            您有 {{ stats.taskCount }} 个待办任务
          </template>
          <span v-if="pendingApprovalCount > 0" class="ml-3">
            ，<span
              class="cursor-pointer font-medium text-orange-500 hover:underline"
              @click="goToApproval"
              >{{ pendingApprovalCount }} 个待审批</span
            >
          </span>
        </template>
      </WorkbenchHeader>

      <!-- 独立的公告容器 -->
      <div
        v-if="announcements.length > 0"
        class="announcement-container"
        @mouseenter="stopAnnouncementScroll"
        @mouseleave="startAnnouncementScroll"
      >
        <span class="announcement-icon">📢</span>
        <div class="announcement-scroll-container">
          <transition name="announcement-slide" mode="out-in">
            <div
              :key="currentAnnouncementIndex"
              class="announcement-item"
              @click="handleViewAnnouncement(announcements[currentAnnouncementIndex]!)"
            >
              <Tag
                :color="getAnnouncementTypeColor(announcements[currentAnnouncementIndex]!.type)"
                size="small"
              >
                {{ getAnnouncementTypeName(announcements[currentAnnouncementIndex]!.type) }}
              </Tag>
              <span class="announcement-title">{{ announcements[currentAnnouncementIndex]!.title }}</span>
            </div>
          </transition>
        </div>
      </div>
    </div>

    <!-- 待审批提醒卡片 -->
    <div
      v-if="pendingApprovalCount > 0"
      class="mt-4 flex cursor-pointer items-center justify-between rounded-lg border border-orange-200 bg-orange-50 p-4 transition-colors hover:bg-orange-100"
      @click="goToApproval"
    >
      <div class="flex items-center">
        <div
          class="mr-4 flex h-10 w-10 items-center justify-center rounded-full bg-orange-500"
        >
          <span class="text-lg text-white">📋</span>
        </div>
        <div>
          <div class="font-medium text-orange-800">
            您有 {{ pendingApprovalCount }} 个待审批事项
          </div>
          <div class="text-sm text-orange-600">点击前往审批中心处理</div>
        </div>
      </div>
      <div class="text-2xl text-orange-500">→</div>
    </div>

    <!-- 统计卡片 - 根据角色类型显示不同内容 -->
    <!-- 律师/团队负责人/主任 统计卡片 -->
    <Row v-if="stats.roleType === 'LAWYER'" :gutter="[16, 16]" class="mt-4">
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-blue"
          @click="router.push('/matter/my')"
        >
          <Statistic
            title="我的项目"
            :value="stats.matterCount"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-green"
          @click="router.push('/crm/client')"
        >
          <Statistic
            title="我的客户"
            :value="stats.clientCount"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-orange"
          @click="router.push('/matter/timesheet')"
        >
          <Statistic
            title="本月工时"
            :value="stats.timesheetHours"
            suffix="小时"
            :precision="1"
            :value-style="{ color: '#fa8c16' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-purple"
          @click="router.push('/matter/task')"
        >
          <Statistic
            title="待办任务"
            :value="stats.taskCount"
            :value-style="{ color: '#722ed1' }"
          />
        </Card>
      </Col>
    </Row>

    <!-- 财务 统计卡片 -->
    <Row v-else-if="stats.roleType === 'FINANCE'" :gutter="[16, 16]" class="mt-4">
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-blue"
          @click="router.push('/finance/payment')"
        >
          <Statistic
            title="待确认收款"
            :value="stats.pendingPaymentCount"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-green"
          @click="router.push('/finance/invoice')"
        >
          <Statistic
            title="待开票"
            :value="stats.pendingInvoiceCount"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-orange"
          @click="router.push('/finance/expense')"
        >
          <Statistic
            title="待审批报销"
            :value="stats.pendingExpenseCount"
            :value-style="{ color: '#fa8c16' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-purple"
          @click="router.push('/finance/payment')"
        >
          <Statistic
            title="本月已收"
            :value="stats.monthlyReceivedAmount"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#722ed1' }"
          />
        </Card>
      </Col>
    </Row>

    <!-- 行政 统计卡片 -->
    <Row v-else-if="stats.roleType === 'ADMIN_STAFF'" :gutter="[16, 16]" class="mt-4">
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-blue"
          @click="router.push('/admin/letter')"
        >
          <Statistic
            title="待处理出函"
            :value="stats.pendingLetterCount"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-green"
          @click="router.push('/document/seal-apply')"
        >
          <Statistic
            title="待处理用印"
            :value="stats.pendingSealCount"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-orange"
          @click="router.push('/admin/leave')"
        >
          <Statistic
            title="待审批请假"
            :value="stats.pendingLeaveCount"
            :value-style="{ color: '#fa8c16' }"
          />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card
          :bordered="false"
          hoverable
          class="stat-card stat-card-purple"
          @click="router.push('/admin/asset')"
        >
          <Statistic
            title="待处理资产"
            :value="stats.pendingAssetCount"
            :value-style="{ color: '#722ed1' }"
          />
        </Card>
      </Col>
    </Row>

    <!-- 默认统计卡片（兜底） -->
    <Row v-else :gutter="[16, 16]" class="mt-4">
      <Col :xs="12" :sm="12" :md="6" :lg="6">
        <Card :bordered="false" hoverable class="stat-card stat-card-purple">
          <Statistic
            title="待办任务"
            :value="stats.taskCount"
            :value-style="{ color: '#722ed1' }"
          />
        </Card>
      </Col>
    </Row>

    <!-- 快捷导航 + 即将到期的期限 + 最近日程 -->
    <Row :gutter="[16, 16]" class="mt-4">
      <Col :xs="24" :md="12" :lg="8">
        <WorkbenchQuickNav
          :items="quickNavItems"
          title="快捷导航"
          @click="navTo"
        />
      </Col>
      <Col :xs="24" :md="12" :lg="8">
        <Card :bordered="false" class="deadline-card">
          <template #title>
            <div class="flex items-center justify-between">
              <span>⏰ 即将到期</span>
            </div>
          </template>
          <div class="deadline-content">
            <Timeline v-if="upcomingDeadlines.length > 0">
              <TimelineItem
                v-for="deadline in upcomingDeadlines"
                :key="deadline.id"
                :color="getDeadlineColor(deadline)"
              >
                <div
                  class="-m-2 cursor-pointer rounded p-2 hover:bg-gray-50"
                  @click="router.push(`/matter/detail/${deadline.matterId}`)"
                >
                  <div class="mb-1 flex items-center gap-2">
                    <Tag :color="getDeadlineColor(deadline)" size="small">
                      {{ formatDeadlineDays(deadline) }}
                    </Tag>
                    <span class="text-xs text-gray-500">{{
                      deadline.deadlineDate
                    }}</span>
                  </div>
                  <div class="font-medium">{{ deadline.deadlineName }}</div>
                  <div class="mt-1 text-xs text-gray-500">
                    📁 {{ deadline.matterName }}
                  </div>
                </div>
              </TimelineItem>
            </Timeline>
            <Empty
              v-else
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
              description="暂无即将到期的期限"
            />
          </div>
        </Card>
      </Col>
      <Col :xs="24" :md="24" :lg="8">
        <!-- 最近日程 - 使用 Timeline 组件 -->
        <Card :bordered="false" class="schedule-card">
          <template #title>
            <div class="flex items-center justify-between">
              <span>📅 最近日程</span>
              <a @click="router.push('/workbench/schedule')">查看全部 →</a>
            </div>
          </template>
          <div class="schedule-content">
            <Timeline v-if="upcomingSchedules.length > 0">
              <TimelineItem
                v-for="schedule in upcomingSchedules.slice(0, 4)"
                :key="schedule.id"
                :color="getScheduleTypeColor(schedule.scheduleType)"
              >
                <div
                  class="-m-2 cursor-pointer rounded p-2 hover:bg-gray-50"
                  @click="router.push('/workbench/schedule')"
                >
                  <div class="mb-1 flex items-center gap-2">
                    <Tag
                      :color="getScheduleTypeColor(schedule.scheduleType)"
                      size="small"
                    >
                      {{ schedule.scheduleTypeName }}
                    </Tag>
                    <span class="text-xs text-gray-500">{{
                      formatScheduleTime(schedule)
                    }}</span>
                  </div>
                  <div class="font-medium">{{ schedule.title }}</div>
                  <div
                    v-if="schedule.location"
                    class="mt-1 text-xs text-gray-500"
                  >
                    📍 {{ schedule.location }}
                  </div>
                </div>
              </TimelineItem>
            </Timeline>
            <Empty
              v-else
              description="暂无近期日程"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />
          </div>
        </Card>
      </Col>
    </Row>

    <Row :gutter="[16, 16]" class="mt-4">
      <!-- 左侧：待办事项 -->
      <Col :xs="24" :lg="14">
        <!-- 待办事项 -->
        <WorkbenchTodo :items="todoItems" title="待办事项" />
      </Col>

      <!-- 右侧：最新动态 -->
      <Col :xs="24" :lg="10">
        <!-- 最新动态 - 使用 List 组件 -->
        <Card :bordered="false">
          <template #title>
            <span>📰 最新动态</span>
          </template>
          <List
            v-if="trendItems.length > 0"
            :data-source="trendItems"
            size="small"
          >
            <template #renderItem="{ item }">
              <ListItem>
                <ListItemMeta :description="item.date">
                  <template #avatar>
                    <Avatar :src="item.avatar" />
                  </template>
                  <template #title>{{ item.title }}</template>
                  <template #description>
                    <div class="text-xs" v-html="item.content"></div>
                    <div class="mt-1 text-xs text-gray-400">
                      {{ item.date }}
                    </div>
                  </template>
                </ListItemMeta>
              </ListItem>
            </template>
          </List>
          <Empty
            v-else
            description="暂无最新动态"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
          />
          <!-- 展开/收起按钮 -->
          <div
            v-if="hasMoreTrends"
            class="mt-3 border-t border-gray-100 pt-3 text-center"
          >
            <a class="text-sm text-primary" @click="toggleTrends">
              {{
                trendsExpanded
                  ? '收起 ↑'
                  : `展开更多 (${allTrendItems.length - 5}条) ↓`
              }}
            </a>
          </div>
        </Card>
      </Col>
    </Row>

    <!-- 公告详情弹窗 -->
    <Modal
      v-model:open="announcementDetailVisible"
      title="公告详情"
      :footer="null"
      width="600px"
    >
      <Spin :spinning="announcementDetailLoading">
        <template v-if="currentAnnouncement">
          <div class="mb-4">
            <Space>
              <Tag :color="getAnnouncementTypeColor(currentAnnouncement.type)">
                {{ currentAnnouncement.typeName || getAnnouncementTypeName(currentAnnouncement.type) }}
              </Tag>
            </Space>
          </div>

          <h2 class="mb-4 text-xl font-semibold">{{ currentAnnouncement.title }}</h2>

          <div class="mb-4 text-sm text-gray-500">
            <Space split="|">
              <span>发布人：{{ currentAnnouncement.publisherName || '-' }}</span>
              <span>发布时间：{{ formatDateTime(currentAnnouncement.publishedAt) }}</span>
              <span v-if="currentAnnouncement.viewCount !== undefined">浏览量：{{ currentAnnouncement.viewCount }}</span>
            </Space>
          </div>

          <div class="rounded bg-gray-50 p-4 leading-relaxed whitespace-pre-wrap">
            {{ currentAnnouncement.content }}
          </div>

          <div v-if="currentAnnouncement.expiredAt" class="mt-4 text-xs text-gray-400">
            过期时间：{{ formatDateTime(currentAnnouncement.expiredAt) }}
          </div>
        </template>
      </Spin>
    </Modal>
  </div>
</template>

<style scoped>
.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgb(0 0 0 / 15%);
  transform: translateY(-2px);
}

.stat-card-blue {
  border-left: 3px solid #1890ff;
}

.stat-card-green {
  border-left: 3px solid #52c41a;
}

.stat-card-orange {
  border-left: 3px solid #fa8c16;
}

.stat-card-purple {
  border-left: 3px solid #722ed1;
}

/* 最近日程卡片 - 与快捷导航高度一致 */
.schedule-card {
  height: 291px;
}

.schedule-card .schedule-content {
  height: 211px;
  padding-top: 12px;
  overflow-y: auto;
}

/* 即将到期期限卡片 - 与快捷导航高度一致 */
.deadline-card {
  height: 291px;
}

.deadline-card .deadline-content {
  height: 211px;
  padding-top: 12px;
  overflow-y: auto;
}

/* WorkbenchHeader 容器 */
.workbench-header-wrapper {
  position: relative;
}

/* 公告独立容器 */
.announcement-container {
  position: absolute;
  top: 50%;
  right: 380px;
  display: flex;
  gap: 8px;
  align-items: center;
  transform: translateY(-50%);
}

.announcement-icon {
  flex-shrink: 0;
  opacity: 0.7;
}

.announcement-scroll-container {
  max-width: 300px;
  height: 24px;
  overflow: hidden;
  line-height: 24px;
}

.announcement-item {
  display: flex;
  gap: 6px;
  align-items: center;
  cursor: pointer;
  opacity: 0.85;
}

.announcement-item:hover {
  opacity: 1;
}

.announcement-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-title:hover {
  text-decoration: underline;
}

/* 响应式调整 */
@media (max-width: 1400px) {
  .announcement-container {
    right: 320px;
  }

  .announcement-scroll-container {
    max-width: 220px;
  }
}

@media (max-width: 1200px) {
  .announcement-container {
    right: 280px;
  }

  .announcement-scroll-container {
    max-width: 180px;
  }
}

@media (max-width: 992px) {
  .announcement-container {
    position: static;
    justify-content: center;
    padding: 8px 16px;
    margin-top: -8px;
    transform: none;
    border-top: 1px dashed rgb(0 0 0 / 6%);
  }

  .announcement-scroll-container {
    max-width: 100%;
  }
}

/* 公告滚动动画 */
.announcement-slide-enter-active,
.announcement-slide-leave-active {
  transition: all 0.5s ease;
}

.announcement-slide-enter-from {
  opacity: 0;
  transform: translateY(100%);
}

.announcement-slide-leave-to {
  opacity: 0;
  transform: translateY(-100%);
}
</style>
