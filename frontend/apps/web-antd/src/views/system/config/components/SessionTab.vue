<script setup lang="ts">
import type { UserSessionDTO } from '#/api/system/types';

import { onMounted, onUnmounted, ref } from 'vue';

import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  forceLogoutSession,
  forceLogoutUser,
  getSessionList,
} from '#/api/system';

defineOptions({ name: 'SessionTab' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<UserSessionDTO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const forceLogoutReason = ref('');
const forceLogoutModalVisible = ref(false);
const forceLogoutTarget = ref<null | { id: number; type: 'session' | 'user' }>(
  null,
);
const forceLogoutLoading = ref(false);

let autoRefreshTimer: null | ReturnType<typeof setInterval> = null;

// 查询条件
const queryParams = ref({
  username: '',
});

// 表格列
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 100 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 170 },
  { title: 'IP地址', dataIndex: 'loginIp', key: 'loginIp', width: 130 },
  { title: '浏览器', dataIndex: 'browser', key: 'browser', width: 100 },
  {
    title: '最后访问',
    dataIndex: 'lastAccessTime',
    key: 'lastAccessTime',
    width: 170,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 },
];

// ==================== 方法 ====================

async function fetchData() {
  loading.value = true;
  try {
    const res = await getSessionList({
      ...queryParams.value,
      status: 'ACTIVE', // 只查询活跃会话
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    });
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error) {
    console.error('获取会话列表失败:', error);
    message.error('加载会话列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function openForceLogoutModal(id: number, type: 'session' | 'user') {
  forceLogoutTarget.value = { id, type };
  forceLogoutReason.value = '';
  forceLogoutModalVisible.value = true;
}

async function confirmForceLogout() {
  if (!forceLogoutTarget.value) return;

  forceLogoutLoading.value = true;
  try {
    const { id, type } = forceLogoutTarget.value;
    const reason = forceLogoutReason.value || '管理员强制下线';

    if (type === 'session') {
      await forceLogoutSession(id, reason);
      message.success('已强制下线该会话');
    } else {
      await forceLogoutUser(id, reason);
      message.success('已强制下线该用户的所有会话');
    }

    forceLogoutModalVisible.value = false;
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  } finally {
    forceLogoutLoading.value = false;
  }
}

function handleSearch() {
  currentPage.value = 1;
  fetchData();
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page;
  pageSize.value = size;
  fetchData();
}

function formatTime(time: string | undefined) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
}

function startAutoRefresh() {
  if (autoRefreshTimer) return;
  autoRefreshTimer = setInterval(() => {
    fetchData();
  }, 30_000);
}

function stopAutoRefresh() {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchData();
  startAutoRefresh();
});

onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<template>
  <div>
    <Alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="在线用户列表每30秒自动刷新。如发现异常登录，可强制下线。"
    />

    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :xs="24" :sm="8">
        <Card size="small">
          <Statistic
            title="当前在线用户"
            :value="total"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :xs="24" :sm="16">
        <Card size="small" style="height: 100%">
          <Space>
            <Input
              v-model:value="queryParams.username"
              placeholder="用户名"
              style="width: 150px"
              allow-clear
              @press-enter="handleSearch"
            />
            <Button type="primary" @click="handleSearch">查询</Button>
            <Button @click="fetchData">刷新列表</Button>
          </Space>
        </Card>
      </Col>
    </Row>

    <!-- 表格 -->
    <Table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="{
        current: currentPage,
        pageSize,
        total,
        showSizeChanger: true,
        showTotal: (t: number) => `共 ${t} 条`,
        onChange: handlePageChange,
      }"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'loginTime'">
          {{ formatTime(record.loginTime) }}
        </template>
        <template v-else-if="column.key === 'lastAccessTime'">
          {{ formatTime(record.lastAccessTime) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag color="success">{{ record.statusName || '在线' }}</Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <Space>
            <Popconfirm
              title="确定要强制下线该会话吗？"
              @confirm="openForceLogoutModal(record.id, 'session')"
            >
              <a style="color: #ff4d4f">下线</a>
            </Popconfirm>
            <a
              style="color: #ff4d4f"
              @click="openForceLogoutModal(record.userId, 'user')"
              >下线全部</a
            >
          </Space>
        </template>
      </template>
    </Table>

    <!-- 强制下线弹窗 -->
    <Modal
      v-model:open="forceLogoutModalVisible"
      :title="
        forceLogoutTarget?.type === 'user'
          ? '强制下线用户所有会话'
          : '强制下线会话'
      "
      :confirm-loading="forceLogoutLoading"
      @ok="confirmForceLogout"
    >
      <div style="margin-bottom: 16px">
        {{
          forceLogoutTarget?.type === 'user'
            ? '确定要强制下线该用户的所有会话吗？'
            : '确定要强制下线该会话吗？'
        }}
      </div>
      <div>
        <label style="display: block; margin-bottom: 8px"
          >下线原因（可选）：</label
        >
        <Input
          v-model:value="forceLogoutReason"
          placeholder="请输入下线原因"
          allow-clear
        />
      </div>
    </Modal>
  </div>
</template>
