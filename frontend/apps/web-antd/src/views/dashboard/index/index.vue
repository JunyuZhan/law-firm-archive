<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  Card,
  Row,
  Col,
  Button,
  List,
  Avatar,
  Tag,
  Badge,
  Empty,
} from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { requestClient } from '#/api/request';
import { getPendingApprovals } from '#/api/workbench';

defineOptions({ name: 'Dashboard' });

const router = useRouter();

// 统计数据
const stats = ref({
  matterCount: 0,
  clientCount: 0,
  timesheetHours: 0,
  taskCount: 0,
});

// 待办任务
const pendingTasks = ref<any[]>([]);

// 最近项目
const recentMatters = ref<any[]>([]);

// 待审批数量
const pendingApprovalCount = ref(0);

// 当前时间问候语
const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 9) return '早上好';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  if (hour < 22) return '晚上好';
  return '夜深了';
});

// 日期格式化
const formattedDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', { 
    weekday: 'long', 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  });
});

// 加载统计数据
async function loadStats() {
  try {
    const res = await requestClient.get('/workbench/stats');
    stats.value = {
      matterCount: res.matterCount || 0,
      clientCount: res.clientCount || 0,
      timesheetHours: res.timesheetHours || 0,
      taskCount: res.taskCount || 0,
    };
  } catch (e) {
    console.error('加载统计数据失败:', e);
  }
}

// 加载待办任务
async function loadPendingTasks() {
  try {
    const res = await requestClient.get('/tasks/my', { params: { status: 'PENDING', pageSize: 5 } });
    pendingTasks.value = res.list || [];
  } catch (e) {
    console.error('加载待办任务失败:', e);
  }
}

// 加载最近项目
async function loadRecentMatters() {
  try {
    const res = await requestClient.get('/matter/my', { params: { pageSize: 5 } });
    recentMatters.value = res.list || [];
  } catch (e) {
    console.error('加载最近项目失败:', e);
  }
}

// 加载待审批数量
async function loadPendingApprovals() {
  try {
    const data = await getPendingApprovals();
    pendingApprovalCount.value = data?.length || 0;
  } catch (e) {
    console.error('加载待审批数据失败:', e);
  }
}

// 跳转到审批中心
function goToApproval() {
  router.push('/dashboard/approval');
}

// 获取项目状态颜色
function getStatusColor(status: string) {
  const map: Record<string, string> = {
    IN_PROGRESS: '#1890ff',
    PENDING: '#faad14',
    COMPLETED: '#52c41a',
    CLOSED: '#8c8c8c',
  };
  return map[status] || '#1890ff';
}

onMounted(() => {
  loadStats();
  loadPendingTasks();
  loadRecentMatters();
  loadPendingApprovals();
});
</script>

<template>
  <Page auto-content-height>
    <div class="dashboard-container">
      <!-- 顶部欢迎区域 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <div class="welcome-text">
            <h1 class="welcome-title">{{ greeting }}，欢迎回来！</h1>
            <p class="welcome-date">{{ formattedDate }}</p>
          </div>
          <div class="welcome-actions">
            <Badge :count="pendingApprovalCount" :offset="[-5, 5]">
              <Button 
                type="primary" 
                size="large"
                class="approval-btn"
                @click="goToApproval"
              >
                <span class="btn-icon">📋</span>
                审批中心
              </Button>
            </Badge>
          </div>
        </div>
      </div>

      <!-- 统计卡片区域 -->
      <Row :gutter="[20, 20]" class="stats-row">
        <Col :xs="12" :sm="12" :md="6" :lg="6">
          <div class="stat-card stat-card-blue" @click="router.push('/matter/my')">
            <div class="stat-icon">📁</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.matterCount }}</div>
              <div class="stat-label">我的项目</div>
            </div>
            <div class="stat-trend">进行中</div>
          </div>
      </Col>
        <Col :xs="12" :sm="12" :md="6" :lg="6">
          <div class="stat-card stat-card-green" @click="router.push('/crm/client')">
            <div class="stat-icon">👥</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.clientCount }}</div>
              <div class="stat-label">我的客户</div>
            </div>
            <div class="stat-trend">负责中</div>
          </div>
      </Col>
        <Col :xs="12" :sm="12" :md="6" :lg="6">
          <div class="stat-card stat-card-orange" @click="router.push('/matter/timesheet')">
            <div class="stat-icon">⏱️</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.timesheetHours.toFixed(1) }}</div>
              <div class="stat-label">本月工时</div>
            </div>
            <div class="stat-trend">小时</div>
          </div>
      </Col>
        <Col :xs="12" :sm="12" :md="6" :lg="6">
          <div class="stat-card stat-card-red" @click="router.push('/matter/task')">
            <div class="stat-icon">✅</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.taskCount }}</div>
              <div class="stat-label">待办任务</div>
            </div>
            <div class="stat-trend">待处理</div>
          </div>
      </Col>
    </Row>

      <!-- 待审批提醒 -->
      <div v-if="pendingApprovalCount > 0" class="approval-alert" @click="goToApproval">
        <div class="alert-icon">🔔</div>
        <div class="alert-content">
          <div class="alert-title">您有 <span class="highlight">{{ pendingApprovalCount }}</span> 个待审批事项</div>
          <div class="alert-desc">点击前往审批中心处理</div>
        </div>
        <div class="alert-arrow">→</div>
      </div>

    <!-- 内容区域 -->
      <Row :gutter="[20, 20]" class="content-row">
      <!-- 待办任务 -->
        <Col :xs="24" :md="12">
          <Card class="content-card" :bordered="false">
            <template #title>
              <div class="card-title">
                <span class="card-title-icon">📋</span>
                <span>待办任务</span>
              </div>
            </template>
          <template #extra>
              <Button type="link" @click="router.push('/matter/task')">查看全部 →</Button>
          </template>
            <div class="task-list">
              <Empty v-if="pendingTasks.length === 0" description="暂无待办任务，休息一下吧 ☕" />
              <List v-else :data-source="pendingTasks" :split="false">
            <template #renderItem="{ item }">
                  <div class="task-item">
                    <div class="task-priority" :class="item.priority?.toLowerCase() || 'medium'"></div>
                    <div class="task-content">
                      <div class="task-title">{{ item.title }}</div>
                      <div class="task-meta">{{ item.matterName || '通用任务' }}</div>
                    </div>
                    <Tag v-if="item.dueDate" :color="new Date(item.dueDate) < new Date() ? 'error' : 'processing'" size="small">
                    {{ item.dueDate }}
                  </Tag>
                  </div>
            </template>
          </List>
            </div>
        </Card>
      </Col>

      <!-- 最近项目 -->
        <Col :xs="24" :md="12">
          <Card class="content-card" :bordered="false">
            <template #title>
              <div class="card-title">
                <span class="card-title-icon">📂</span>
                <span>最近项目</span>
              </div>
            </template>
          <template #extra>
              <Button type="link" @click="router.push('/matter/list')">查看全部 →</Button>
          </template>
            <div class="project-list">
              <Empty v-if="recentMatters.length === 0" description="暂无项目，开始创建吧 🚀" />
              <List v-else :data-source="recentMatters" :split="false">
            <template #renderItem="{ item }">
                  <div class="project-item" @click="router.push(`/matter/detail/${item.id}`)">
                    <Avatar 
                      :style="{ backgroundColor: getStatusColor(item.status) }"
                      class="project-avatar"
                    >
                      {{ item.name?.charAt(0) || 'P' }}
                    </Avatar>
                    <div class="project-content">
                      <div class="project-title">{{ item.name }}</div>
                      <div class="project-meta">
                        <span class="project-no" v-if="item.matterNo">{{ item.matterNo }}</span>
                        <span class="project-client">{{ item.clientName || '未关联客户' }}</span>
                        <Tag :color="getStatusColor(item.status)" size="small">
                    {{ item.statusName || item.status }}
                  </Tag>
                      </div>
                    </div>
                  </div>
            </template>
          </List>
            </div>
        </Card>
      </Col>
    </Row>

    <!-- 快捷操作 -->
      <Card class="quick-actions-card" :bordered="false">
        <template #title>
          <div class="card-title">
            <span class="card-title-icon">⚡</span>
            <span>快捷操作</span>
          </div>
        </template>
        <div class="quick-actions">
          <div class="quick-action-item" @click="router.push('/matter/list')">
            <div class="action-icon action-icon-blue">📝</div>
            <div class="action-text">新建项目</div>
          </div>
          <div class="quick-action-item" @click="router.push('/crm/client')">
            <div class="action-icon action-icon-green">👤</div>
            <div class="action-text">新建客户</div>
          </div>
          <div class="quick-action-item" @click="router.push('/matter/timesheet')">
            <div class="action-icon action-icon-orange">⏰</div>
            <div class="action-text">记录工时</div>
          </div>
          <div class="quick-action-item" @click="router.push('/matter/task')">
            <div class="action-icon action-icon-purple">✏️</div>
            <div class="action-text">创建任务</div>
          </div>
          <div class="quick-action-item" @click="router.push('/finance/contract')">
            <div class="action-icon action-icon-cyan">📄</div>
            <div class="action-text">财务合同</div>
          </div>
          <div class="quick-action-item" @click="goToApproval">
            <div class="action-icon action-icon-red">
              <Badge :count="pendingApprovalCount" :offset="[0, 0]" size="small">
                📋
              </Badge>
            </div>
            <div class="action-text">审批中心</div>
          </div>
        </div>
    </Card>
    </div>
  </Page>
</template>

<style scoped>


@media (max-width: 992px) {
  .quick-actions {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 576px) {
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}

.dashboard-container {
  min-height: 100%;
  padding: 0;
}

/* 欢迎区域 */
.welcome-section {
  position: relative;
  padding: 32px;
  margin-bottom: 24px;
  overflow: hidden;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
}

.welcome-section::before {
  position: absolute;
  top: -50%;
  right: -20%;
  width: 60%;
  height: 200%;
  content: '';
  background: rgb(255 255 255 / 10%);
  border-radius: 50%;
  transform: rotate(-15deg);
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 1px;
}

.welcome-date {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.approval-btn {
  display: flex;
  gap: 8px;
  align-items: center;
  height: 44px;
  font-size: 15px;
  border-radius: 22px;
  box-shadow: 0 4px 12px rgb(0 0 0 / 15%);
}

.btn-icon {
  font-size: 18px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 24px;
  overflow: hidden;
  cursor: pointer;
  background: white;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  box-shadow: 0 12px 24px rgb(0 0 0 / 10%);
  transform: translateY(-4px);
}

.stat-card::after {
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  content: '';
  border-radius: 50%;
  opacity: 0.1;
  transform: translate(30%, -30%);
}

.stat-card-blue::after { background: #1890ff; }

.stat-card-green::after { background: #52c41a; }

.stat-card-orange::after { background: #faad14; }

.stat-card-red::after { background: #ff4d4f; }

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  font-size: 32px;
  border-radius: 12px;
}

.stat-card-blue .stat-icon { background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%); }

.stat-card-green .stat-icon { background: linear-gradient(135deg, #f6ffed 0%, #b7eb8f 100%); }

.stat-card-orange .stat-icon { background: linear-gradient(135deg, #fffbe6 0%, #ffe58f 100%); }

.stat-card-red .stat-icon { background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card-blue .stat-value { color: #1890ff; }

.stat-card-green .stat-value { color: #52c41a; }

.stat-card-orange .stat-value { color: #faad14; }

.stat-card-red .stat-value { color: #ff4d4f; }

.stat-label {
  margin-top: 4px;
  font-size: 14px;
  color: #8c8c8c;
}

.stat-trend {
  padding: 4px 8px;
  font-size: 12px;
  color: #bfbfbf;
  background: #fafafa;
  border-radius: 4px;
}

/* 待审批提醒 */
.approval-alert {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px 24px;
  margin-bottom: 24px;
  cursor: pointer;
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.approval-alert:hover {
  box-shadow: 0 4px 12px rgb(250 173 20 / 20%);
  transform: translateX(4px);
}

.alert-icon {
  font-size: 24px;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 15px;
  font-weight: 500;
  color: #ad6800;
}

.alert-title .highlight {
  font-size: 18px;
  font-weight: 700;
  color: #fa541c;
}

.alert-desc {
  margin-top: 2px;
  font-size: 13px;
  color: #d48806;
}

.alert-arrow {
  font-size: 20px;
  color: #d48806;
}

/* 内容卡片 */
.content-row {
  margin-bottom: 24px;
}

.content-card {
  height: 100%;
  border-radius: 16px;
}

.content-card :deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 600;
}

.card-title-icon {
  font-size: 18px;
}

/* 任务列表 */
.task-list {
  min-height: 200px;
}

.task-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.task-item:hover {
  background: #f0f0f0;
}

.task-priority {
  width: 4px;
  height: 32px;
  border-radius: 2px;
}

.task-priority.high { background: #ff4d4f; }

.task-priority.medium { background: #faad14; }

.task-priority.low { background: #52c41a; }

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
}

.task-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 项目列表 */
.project-list {
  min-height: 200px;
}

.project-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  background: #fafafa;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.project-item:hover {
  background: #f0f0f0;
  transform: translateX(4px);
}

.project-avatar {
  font-size: 14px;
  font-weight: 600;
}

.project-content {
  flex: 1;
  min-width: 0;
}

.project-title {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
}

.project-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
}

.project-no {
  padding: 2px 6px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #8c8c8c;
  background: #f0f0f0;
  border-radius: 4px;
}

.project-client {
  font-size: 12px;
  color: #8c8c8c;
}

/* 快捷操作 */
.quick-actions-card {
  border-radius: 16px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  padding: 20px 12px;
  cursor: pointer;
  background: #fafafa;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.quick-action-item:hover {
  background: #f0f0f0;
  transform: translateY(-2px);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 24px;
  border-radius: 12px;
}

.action-icon-blue { background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%); }

.action-icon-green { background: linear-gradient(135deg, #f6ffed 0%, #b7eb8f 100%); }

.action-icon-orange { background: linear-gradient(135deg, #fffbe6 0%, #ffe58f 100%); }

.action-icon-purple { background: linear-gradient(135deg, #f9f0ff 0%, #d3adf7 100%); }

.action-icon-cyan { background: linear-gradient(135deg, #e6fffb 0%, #87e8de 100%); }

.action-icon-red { background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%); }

.action-text {
  font-size: 13px;
  font-weight: 500;
  color: #595959;
}
</style>
