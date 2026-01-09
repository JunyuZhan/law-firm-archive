<script lang="ts" setup>
import type {
  WorkbenchProjectItem,
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
} from '@vben/common-ui';

import { ref, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';

import {
  WorkbenchHeader,
  WorkbenchProject,
  WorkbenchQuickNav,
  WorkbenchTodo,
  WorkbenchTrends,
} from '@vben/common-ui';
import { preferences } from '@vben/preferences';
import { useUserStore } from '@vben/stores';
import { openWindow } from '@vben/utils';

import {
  getWorkbenchStats,
  getPendingApprovals,
  getMyApprovedHistory,
  getMyInitiatedApprovals,
  getRecentProjects,
} from '#/api/workbench';
import { getMyMatters, getMyUpcomingSchedules, getMyTodoTasks } from '#/api/matter';
import type { MatterDTO, TaskDTO } from '#/api/matter/types';
import type { ScheduleDTO } from '#/api/matter/schedule';
import type { ApprovalDTO } from '#/api/workbench';

const userStore = useUserStore();
const router = useRouter();

// 统计数据
const stats = ref({
  matterCount: 0,
  clientCount: 0,
  timesheetHours: 0,
  taskCount: 0,
});

// 待审批数量
const pendingApprovalCount = ref(0);
const pendingApprovals = ref<ApprovalDTO[]>([]);

// 近期日程
const upcomingSchedules = ref<ScheduleDTO[]>([]);

// 我的项目列表
const projectItems = ref<WorkbenchProjectItem[]>([]);

// 待办任务列表
const todoItems = ref<WorkbenchTodoItem[]>([]);

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
      matterCount: data.matterCount || 0,
      clientCount: data.clientCount || 0,
      timesheetHours: data.timesheetHours || 0,
      taskCount: data.taskCount || 0,
    };
  } catch (error) {
    console.error('加载统计数据失败:', error);
  }
}

// 加载我的项目
async function loadMyMatters() {
  try {
    const res = await getMyMatters({ pageNum: 1, pageSize: 6 });
    projectItems.value = res.list.map((matter: MatterDTO) => ({
      color: getMatterColor(matter.status),
      content: matter.description || '暂无描述',
      date: matter.createdAt || '',
      group: matter.matterTypeName || '项目',
      icon: 'ion:briefcase-outline',
      title: matter.name,
      url: `/matter/detail/${matter.id}`,
    }));
  } catch (error) {
    console.error('加载我的项目失败:', error);
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
  } catch (error) {
    console.error('加载待办任务失败:', error);
  }
}

// 加载待审批数据
async function loadPendingApprovals() {
  try {
    const data = await getPendingApprovals();
    pendingApprovals.value = data || [];
    pendingApprovalCount.value = pendingApprovals.value.length;
  } catch (error) {
    console.error('加载待审批数据失败:', error);
  }
}

// 加载近期日程
async function loadUpcomingSchedules() {
  try {
    const data = await getMyUpcomingSchedules(7, 5);
    upcomingSchedules.value = data || [];
  } catch (error) {
    console.error('加载近期日程失败:', error);
  }
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

// 获取项目状态颜色
function getMatterColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: '#e18525',
    IN_PROGRESS: '#3fb27f',
    CLOSED: '#bf0c2c',
    SUSPENDED: '#999',
  };
  return colorMap[status] || '#1fdaca';
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
          content: `创建了新项目 <span class="text-primary">${project.matterName}</span>`,
          date: formatRelativeTime(project.lastUpdateTime),
          title: userStore.userInfo?.realName || '我',
          timestamp,
        });
      });
    } catch (error) {
      console.error('加载最近项目失败:', error);
    }

    // 2. 获取审批历史（取前5条）
    try {
      const approvedHistory = await getMyApprovedHistory();
      approvedHistory.slice(0, 5).forEach((approval) => {
        const statusText = approval.status === 'APPROVED' ? '审批通过了' : '拒绝了';

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
          content: `${statusText} <span class="text-primary">${applicantName}</span> 发起的 <span class="text-primary">${businessTitle}</span>`,
          date: formatRelativeTime(approval.approvedAt || approval.updatedAt),
          title: approverName,
          timestamp,
        });
      });
    } catch (error) {
      console.error('加载审批历史失败:', error);
    }

    // 3. 获取我发起的审批（取前5条，状态为已通过或已拒绝）
    try {
      const myInitiated = await getMyInitiatedApprovals();
      myInitiated
        .filter((a) => a.status === 'APPROVED' || a.status === 'REJECTED')
        .slice(0, 5)
        .forEach((approval) => {
          const statusText = approval.status === 'APPROVED' ? '已通过' : '已拒绝';
          const approverName = approval.approverName || '审批人';

          const timestamp = approval.approvedAt
            ? dayjs(approval.approvedAt).valueOf()
            : approval.updatedAt
              ? dayjs(approval.updatedAt).valueOf()
              : Date.now();

          trends.push({
            avatar: userStore.userInfo?.avatar || preferences.app.defaultAvatar,
            content: `我发起的 <span class="text-primary">${approval.businessTitle || approval.businessTypeName}</span> 被 <span class="text-primary">${approverName}</span> ${statusText}`,
            date: formatRelativeTime(approval.approvedAt || approval.updatedAt),
            title: userStore.userInfo?.realName || '我',
            timestamp,
          });
        });
    } catch (error) {
      console.error('加载我发起的审批失败:', error);
    }

    // 按时间戳排序（最新的在前）
    trends.sort((a, b) => b.timestamp - a.timestamp);

    // 取前10条，移除 timestamp 字段
    trendItems.value = trends.slice(0, 10).map(({ timestamp, ...item }) => item);

    // 如果没有数据，显示提示
    if (trendItems.value.length === 0) {
      trendItems.value = [
        {
          avatar: preferences.app.defaultAvatar,
          content: '暂无最新动态',
          date: '刚刚',
          title: '系统',
        },
      ];
    }
  } catch (error) {
    console.error('加载最新动态失败:', error);
    trendItems.value = [];
  }
}

// 获取问候语（根据时间动态显示）
function getGreeting(): { greeting: string; action: string } {
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
function navTo(nav: WorkbenchProjectItem | WorkbenchQuickNavItem) {
  if (nav.url?.startsWith('http')) {
    openWindow(nav.url);
    return;
  }
  if (nav.url?.startsWith('/')) {
    router.push(nav.url).catch((error) => {
      console.error('Navigation failed:', error);
    });
  } else {
    console.warn(`Unknown URL for navigation item: ${nav.title} -> ${nav.url}`);
  }
}

onMounted(() => {
  loadStats();
  loadMyMatters();
  loadTodoTasks();
  loadPendingApprovals();
  loadUpcomingSchedules();
  loadTrends();
});

// 页面激活时刷新数据（用于 keep-alive 场景）
onActivated(() => {
  loadStats();
  loadMyMatters();
  loadTodoTasks();
  loadPendingApprovals();
  loadUpcomingSchedules();
  loadTrends();
});
</script>

<template>
  <div class="p-5">
    <WorkbenchHeader
      :avatar="userStore.userInfo?.avatar || preferences.app.defaultAvatar"
      :matter-count="stats.matterCount"
      :client-count="stats.clientCount"
      :task-count="stats.taskCount"
    >
      <template #title>
        {{ getGreeting().greeting }}, {{ userStore.userInfo?.realName }}, {{ getGreeting().action }}
      </template>
      <template #description>
        您有 {{ stats.taskCount }} 个待办任务，本月工时 {{ stats.timesheetHours.toFixed(1) }} 小时
        <span v-if="pendingApprovalCount > 0" class="ml-3">
          ，<span class="text-orange-500 font-medium cursor-pointer hover:underline" @click="goToApproval">{{ pendingApprovalCount }} 个待审批</span>
        </span>
      </template>
    </WorkbenchHeader>

    <!-- 待审批提醒卡片 -->
    <div 
      v-if="pendingApprovalCount > 0" 
      class="mt-4 p-4 bg-orange-50 border border-orange-200 rounded-lg flex items-center justify-between cursor-pointer hover:bg-orange-100 transition-colors"
      @click="goToApproval"
    >
      <div class="flex items-center">
        <div class="w-10 h-10 bg-orange-500 rounded-full flex items-center justify-center mr-4">
          <span class="text-white text-lg">📋</span>
        </div>
        <div>
          <div class="font-medium text-orange-800">您有 {{ pendingApprovalCount }} 个待审批事项</div>
          <div class="text-sm text-orange-600">点击前往审批中心处理</div>
        </div>
      </div>
      <div class="text-orange-500 text-2xl">→</div>
    </div>

    <!-- 近期日程提醒 -->
    <div 
      v-if="upcomingSchedules.length > 0" 
      class="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg"
    >
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center">
          <span class="text-xl mr-2">📅</span>
          <span class="font-medium text-blue-800">近期日程</span>
        </div>
        <span 
          class="text-sm text-blue-600 cursor-pointer hover:underline"
          @click="router.push('/workbench/schedule')"
        >
          查看全部 →
        </span>
      </div>
      <div class="space-y-2">
        <div 
          v-for="schedule in upcomingSchedules" 
          :key="schedule.id"
          class="flex items-center p-2 bg-white rounded hover:bg-blue-100 cursor-pointer transition-colors"
          @click="router.push('/workbench/schedule')"
        >
          <div 
            class="w-2 h-2 rounded-full mr-3"
            :style="{ backgroundColor: getScheduleTypeColor(schedule.scheduleType) }"
          ></div>
          <span class="text-sm font-medium text-gray-600 w-20">
            {{ formatScheduleTime(schedule) }}
          </span>
          <span 
            class="text-xs px-2 py-0.5 rounded mr-2"
            :style="{ 
              backgroundColor: getScheduleTypeColor(schedule.scheduleType) + '20',
              color: getScheduleTypeColor(schedule.scheduleType)
            }"
          >
            {{ schedule.scheduleTypeName }}
          </span>
          <span class="text-sm text-gray-800 flex-1 truncate">{{ schedule.title }}</span>
          <span v-if="schedule.location" class="text-xs text-gray-500 ml-2">
            📍 {{ schedule.location }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-5 flex flex-col lg:flex-row">
      <div class="mr-4 w-full lg:w-3/5">
        <WorkbenchProject :items="projectItems" title="我的项目" @click="navTo" />
        <WorkbenchTrends :items="trendItems" class="mt-5" title="最新动态" />
      </div>
      <div class="w-full lg:w-2/5">
        <WorkbenchQuickNav
          :items="quickNavItems"
          class="mt-5 lg:mt-0"
          title="快捷导航"
          @click="navTo"
        />
        <WorkbenchTodo :items="todoItems" class="mt-5" title="待办事项" />
      </div>
    </div>
  </div>
</template>
