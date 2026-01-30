<script setup lang="ts">
import type { LoginLogDTO } from '#/api/system/types';

import { onMounted, ref } from 'vue';

import {
  Card,
  Descriptions,
  DescriptionsItem,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { getLoginLogDetail, getLoginLogList } from '#/api/system';

defineOptions({ name: 'LoginLogTab' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<LoginLogDTO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const detailVisible = ref(false);
const detailData = ref<LoginLogDTO | null>(null);
const detailLoading = ref(false);

// 查询条件
const queryParams = ref({
  username: '',
  status: undefined as string | undefined,
});

// 表格列
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '登录时间', dataIndex: 'loginTime', key: 'loginTime', width: 180 },
  { title: 'IP地址', dataIndex: 'loginIp', key: 'loginIp', width: 140 },
  { title: '登录地点', dataIndex: 'loginLocation', key: 'loginLocation', width: 140 },
  { title: '浏览器', dataIndex: 'browser', key: 'browser', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 80 },
];

// ==================== 方法 ====================

async function fetchData() {
  loading.value = true;
  try {
    const params: Record<string, any> = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    };
    if (queryParams.value.username) {
      params.username = queryParams.value.username;
    }
    if (queryParams.value.status) {
      params.status = queryParams.value.status;
    }
    const res = await getLoginLogList(params);
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch {
    // 忽略错误
  } finally {
    loading.value = false;
  }
}

async function handleViewDetail(row: LoginLogDTO) {
  detailLoading.value = true;
  detailVisible.value = true;
  try {
    detailData.value = await getLoginLogDetail(row.id);
  } catch {
    detailData.value = row;
  } finally {
    detailLoading.value = false;
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

// ==================== 生命周期 ====================

onMounted(() => {
  fetchData();
});
</script>

<template>
  <div>
    <!-- 查询表单 -->
    <Card size="small" style="margin-bottom: 16px">
      <Space wrap>
        <Input
          v-model:value="queryParams.username"
          placeholder="用户名"
          style="width: 150px"
          allow-clear
          @press-enter="handleSearch"
        />
        <Select
          v-model:value="queryParams.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
          @change="handleSearch"
        >
          <Select.Option value="SUCCESS">成功</Select.Option>
          <Select.Option value="FAILED">失败</Select.Option>
        </Select>
        <a @click="handleSearch">查询</a>
      </Space>
    </Card>

    <!-- 表格 -->
    <Table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="{
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showTotal: (t: number) => `共 ${t} 条`,
        onChange: handlePageChange,
      }"
      row-key="id"
      size="small"
    >
      <template #bodyCell="{ column, record: rawRecord }">
        <template v-if="column.key === 'loginTime'">
          {{ formatTime((rawRecord as LoginLogDTO).loginTime) }}
        </template>
        <template v-else-if="column.key === 'status'">
          <Tag :color="(rawRecord as LoginLogDTO).status === 'SUCCESS' ? 'success' : 'error'">
            {{ (rawRecord as LoginLogDTO).status === 'SUCCESS' ? '成功' : '失败' }}
          </Tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="handleViewDetail(rawRecord as LoginLogDTO)">详情</a>
        </template>
      </template>
    </Table>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="登录日志详情"
      :footer="null"
      width="600px"
    >
      <Descriptions v-if="detailData" :column="2" bordered size="small" :loading="detailLoading">
        <DescriptionsItem label="用户名">{{ detailData.username }}</DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="detailData.status === 'SUCCESS' ? 'success' : 'error'">
            {{ detailData.status === 'SUCCESS' ? '成功' : '失败' }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="登录时间" :span="2">{{ formatTime(detailData.loginTime) }}</DescriptionsItem>
        <DescriptionsItem label="IP地址">{{ detailData.loginIp || '-' }}</DescriptionsItem>
        <DescriptionsItem label="登录地点">{{ detailData.loginLocation || '-' }}</DescriptionsItem>
        <DescriptionsItem label="浏览器">{{ detailData.browser || '-' }}</DescriptionsItem>
        <DescriptionsItem label="操作系统">{{ detailData.os || '-' }}</DescriptionsItem>
        <DescriptionsItem v-if="detailData.failReason" label="失败原因" :span="2">
          <span style="color: #ff4d4f">{{ detailData.failReason }}</span>
        </DescriptionsItem>
      </Descriptions>
    </Modal>
  </div>
</template>
