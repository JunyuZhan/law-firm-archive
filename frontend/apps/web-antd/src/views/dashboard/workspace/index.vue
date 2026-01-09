<script lang="ts" setup>
import type {
  WorkbenchQuickNavItem,
  WorkbenchTodoItem,
  WorkbenchTrendItem,
} from '@vben/common-ui';

import { ref, computed, onMounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';

import {
  WorkbenchHeader,
  WorkbenchQuickNav,
  WorkbenchTodo,
} from '@vben/common-ui';
import {
  Card,
  Row,
  Col,
  Statistic,
  Timeline,
  TimelineItem,
  List,
  ListItem,
  ListItemMeta,
  Avatar,
  Tag,
  Empty,
} from 'ant-design-vue';
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
import { getMyUpcomingSchedules, getMyTodoTasks } from '#/api/matter';
import type { TaskDTO } from '#/api/matter/types';
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

// 周视图数据
const weekDays = computed(() => {
  const days = [];
  const today = dayjs().startOf('day');
  const weekLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  
  for (let i = 0; i < 7; i++) {
    const date = today.add(i, 'day');
    const dateStr = date.format('YYYY-MM-DD');
    
    // 筛选这一天的日程
    const daySchedules = upcomingSchedules.value.filter(s => {
      const scheduleDate = dayjs(s.startTime).format('YYYY-MM-DD');
      return scheduleDate === dateStr;
    });
    
    days.push({
      date: dateStr,
      dateNum: date.format('D'),
      label: i === 0 ? '今天' : weekLabels[date.day()],
      isToday: i === 0,
      schedules: daySchedules,
    });
  }
  
  return days;
});

// 今日日程
const todaySchedules = computed(() => {
  const today = dayjs().format('YYYY-MM-DD');
  return upcomingSchedules.value.filter(s => {
    const scheduleDate = dayjs(s.startTime).format('YYYY-MM-DD');
    return scheduleDate === today;
  });
});

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

    // 取前5条，移除 timestamp 字段（限制显示数量，避免过长）
    trendItems.value = trends.slice(0, 5).map(({ timestamp, ...item }) => item);

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
  loadTodoTasks();
  loadPendingApprovals();
  loadUpcomingSchedules();
  loadTrends();
});

// 页面激活时刷新数据（用于 keep-alive 场景）
onActivated(() => {
  loadStats();
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

    <!-- 统计卡片 -->
    <Row :gutter="[16, 16]" class="mt-4">
      <Col :xs="12" :sm="12" :md="6">
        <Card :bordered="false" hoverable class="stat-card stat-card-blue" @click="router.push('/matter/my')">
          <Statistic title="我的项目" :value="stats.matterCount" :valueStyle="{ color: '#1890ff' }" />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6">
        <Card :bordered="false" hoverable class="stat-card stat-card-green" @click="router.push('/crm/client')">
          <Statistic title="我的客户" :value="stats.clientCount" :valueStyle="{ color: '#52c41a' }" />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6">
        <Card :bordered="false" hoverable class="stat-card stat-card-orange" @click="router.push('/matter/timesheet')">
          <Statistic title="本月工时" :value="stats.timesheetHours" :precision="1" suffix="h" :valueStyle="{ color: '#fa8c16' }" />
        </Card>
      </Col>
      <Col :xs="12" :sm="12" :md="6">
        <Card :bordered="false" hoverable class="stat-card stat-card-purple" @click="router.push('/matter/task')">
          <Statistic title="待办任务" :value="stats.taskCount" :valueStyle="{ color: '#722ed1' }" />
        </Card>
      </Col>
    </Row>

    <Row :gutter="[16, 16]" class="mt-4">
      <!-- 左侧：最近日程 + 待办事项 -->
      <Col :xs="24" :lg="14">
        <!-- 最近日程 - 使用 Timeline 组件 -->
        <Card :bordered="false" class="mb-4">
          <template #title>
            <div class="flex items-center justify-between">
              <span>📅 最近日程</span>
              <a @click="router.push('/workbench/schedule')">查看全部 →</a>
            </div>
          </template>
          <Timeline v-if="upcomingSchedules.length > 0">
            <TimelineItem 
              v-for="schedule in upcomingSchedules.slice(0, 5)" 
              :key="schedule.id"
              :color="getScheduleTypeColor(schedule.scheduleType)"
            >
              <div class="cursor-pointer hover:bg-gray-50 p-2 -m-2 rounded" @click="router.push('/workbench/schedule')">
                <div class="flex items-center gap-2 mb-1">
                  <Tag :color="getScheduleTypeColor(schedule.scheduleType)" size="small">
                    {{ schedule.scheduleTypeName }}
                  </Tag>
                  <span class="text-gray-500 text-xs">{{ formatScheduleTime(schedule) }}</span>
                </div>
                <div class="font-medium">{{ schedule.title }}</div>
                <div v-if="schedule.location" class="text-gray-500 text-xs mt-1">
                  📍 {{ schedule.location }}
                </div>
              </div>
            </TimelineItem>
          </Timeline>
          <Empty v-else description="暂无近期日程" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </Card>

        <!-- 待办事项 -->
        <WorkbenchTodo :items="todoItems" title="待办事项" />
      </Col>

      <!-- 右侧：快捷导航 + 最新动态 -->
      <Col :xs="24" :lg="10">
        <WorkbenchQuickNav
          :items="quickNavItems"
          title="快捷导航"
          @click="navTo"
        />
        
        <!-- 最新动态 - 使用 List 组件 -->
        <Card :bordered="false" class="mt-4">
          <template #title>
            <div class="flex items-center justify-between">
              <span>📰 最新动态</span>
              <a @click="router.push('/workbench/approval')">更多 →</a>
            </div>
          </template>
          <List v-if="trendItems.length > 0" :dataSource="trendItems" size="small">
            <template #renderItem="{ item }">
              <ListItem>
                <ListItemMeta :description="item.date">
                  <template #avatar>
                    <Avatar :src="item.avatar" />
                  </template>
                  <template #title>{{ item.title }}</template>
                  <template #description>
                    <div class="text-xs" v-html="item.content"></div>
                    <div class="text-gray-400 text-xs mt-1">{{ item.date }}</div>
                  </template>
                </ListItemMeta>
              </ListItem>
            </template>
          </List>
          <Empty v-else description="暂无最新动态" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
        </Card>
      </Col>
    </Row>
  </div>
</template>

<style scoped>
.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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
</style>
