<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { AttendanceRecord } from '#/api/hr/types';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  message,
  Row,
  Select,
  Space,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  checkIn,
  checkOut,
  fetchAttendanceList,
  getTodayAttendance,
} from '#/api/hr/attendance';

defineOptions({ name: 'AttendanceManagement' });

const { RangePicker } = DatePicker;

// ==================== 状态定义 ====================

const searchForm = reactive({
  startDate: '',
  endDate: '',
  status: undefined as string | undefined,
});

const todayAttendance = ref<AttendanceRecord | null>(null);
const checkInLoading = ref(false);
const checkOutLoading = ref(false);

// ==================== 常量选项 ====================

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '正常', value: 'NORMAL' },
  { label: '迟到', value: 'LATE' },
  { label: '早退', value: 'EARLY_LEAVE' },
  { label: '缺勤', value: 'ABSENT' },
  { label: '请假', value: 'LEAVE' },
];

const statusColorMap: Record<string, string> = {
  NORMAL: 'green',
  LATE: 'orange',
  EARLY_LEAVE: 'orange',
  ABSENT: 'red',
  LEAVE: 'blue',
};

const statusTextMap: Record<string, string> = {
  NORMAL: '正常',
  LATE: '迟到',
  EARLY_LEAVE: '早退',
  ABSENT: '缺勤',
  LEAVE: '请假',
};

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '姓名', field: 'realName', width: 100 },
  { title: '日期', field: 'attendanceDate', width: 120 },
  { title: '签到时间', field: 'checkInTime', width: 120 },
  { title: '签退时间', field: 'checkOutTime', width: 120 },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '工作时长(小时)', field: 'workHours', width: 120 },
  { title: '备注', field: 'remark', minWidth: 150, showOverflow: true },
];

async function loadData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const params = {
    ...searchForm,
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  };
  const res = await fetchAttendanceList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== 数据加载 ====================

async function fetchTodayAttendance() {
  try {
    todayAttendance.value = await getTodayAttendance();
  } catch {
    todayAttendance.value = null;
  }
}

// ==================== 签到签退 ====================

async function handleCheckIn() {
  checkInLoading.value = true;
  try {
    await checkIn();
    message.success('签到成功');
    await fetchTodayAttendance();
    gridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '签到失败');
  } finally {
    checkInLoading.value = false;
  }
}

async function handleCheckOut() {
  checkOutLoading.value = true;
  try {
    await checkOut();
    message.success('签退成功');
    await fetchTodayAttendance();
    gridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '签退失败');
  } finally {
    checkOutLoading.value = false;
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  Object.assign(searchForm, { startDate: '', endDate: '', status: undefined });
  gridApi.reload();
}

function handleDateRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    searchForm.startDate = dates[0].format('YYYY-MM-DD');
    searchForm.endDate = dates[1].format('YYYY-MM-DD');
  } else {
    searchForm.startDate = '';
    searchForm.endDate = '';
  }
}

onMounted(() => {
  fetchTodayAttendance();
});
</script>

<template>
  <Page title="考勤管理" description="员工考勤记录、签到签退">
    <div class="space-y-4 p-4">
      <!-- 今日考勤卡片 -->
      <Card title="今日考勤">
        <div class="flex flex-wrap items-center gap-8">
          <div>
            <span class="text-gray-500">签到时间：</span>
            <span class="font-medium">{{
              todayAttendance?.checkInTime || '未签到'
            }}</span>
          </div>
          <div>
            <span class="text-gray-500">签退时间：</span>
            <span class="font-medium">{{
              todayAttendance?.checkOutTime || '未签退'
            }}</span>
          </div>
          <div>
            <span class="text-gray-500">状态：</span>
            <Tag
              v-if="todayAttendance?.status"
              :color="statusColorMap[todayAttendance.status]"
            >
              {{ statusTextMap[todayAttendance.status] }}
            </Tag>
            <span v-else>-</span>
          </div>
          <Space>
            <Button
              type="primary"
              :loading="checkInLoading"
              :disabled="!!todayAttendance?.checkInTime"
              @click="handleCheckIn"
            >
              签到
            </Button>
            <Button
              :loading="checkOutLoading"
              :disabled="
                !todayAttendance?.checkInTime || !!todayAttendance?.checkOutTime
              "
              @click="handleCheckOut"
            >
              签退
            </Button>
          </Space>
        </div>
      </Card>

      <!-- 搜索区域 -->
      <Card>
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="8">
            <RangePicker style="width: 100%" @change="handleDateRangeChange" />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="searchForm.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="10">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </Card>

      <!-- 表格区域 -->
      <Card title="考勤记录">
        <Grid>
          <template #status="{ row }">
            <Tag :color="statusColorMap[row.status]">
              {{ statusTextMap[row.status] }}
            </Tag>
          </template>
        </Grid>
      </Card>
    </div>
  </Page>
</template>
