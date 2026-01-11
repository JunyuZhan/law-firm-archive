<script setup lang="ts">
import type { AttendanceRecord } from '#/api/hr/attendance';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  message,
  Modal,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  checkIn,
  checkOut,
  fetchAttendanceList,
  getTodayAttendance,
} from '#/api/hr/attendance';

defineOptions({ name: 'AdminAttendance' });

const loading = ref(false);
const selectedDate = ref(dayjs());
const dataSource = ref<AttendanceRecord[]>([]);
const todayRecord = ref<AttendanceRecord | null>(null);
const detailVisible = ref(false);
const currentRecord = ref<AttendanceRecord | null>(null);

// 统计数据
const statistics = ref({
  total: 0,
  normal: 0,
  late: 0,
  early: 0,
  absent: 0,
});

const columns = [
  {
    title: '员工姓名',
    dataIndex: 'employeeName',
    key: 'employeeName',
    width: 120,
  },
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 120,
  },
  {
    title: '日期',
    dataIndex: 'attendanceDate',
    key: 'attendanceDate',
    width: 110,
  },
  {
    title: '上班打卡',
    dataIndex: 'checkInTime',
    key: 'checkInTime',
    width: 100,
  },
  {
    title: '下班打卡',
    dataIndex: 'checkOutTime',
    key: 'checkOutTime',
    width: 100,
  },
  { title: '工作时长', dataIndex: 'workHours', key: 'workHours', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    width: 150,
    ellipsis: true,
  },
  { title: '操作', key: 'action', width: 80 },
];

const statusMap: Record<string, { color: string; text: string }> = {
  NORMAL: { color: 'success', text: '正常' },
  LATE: { color: 'warning', text: '迟到' },
  EARLY_LEAVE: { color: 'warning', text: '早退' },
  ABSENT: { color: 'error', text: '缺勤' },
  LEAVE: { color: 'default', text: '请假' },
  OVERTIME: { color: 'processing', text: '加班' },
};

async function loadData() {
  loading.value = true;
  try {
    const date = selectedDate.value.format('YYYY-MM-DD');
    const res = await fetchAttendanceList({ date, pageNum: 1, pageSize: 100 });
    dataSource.value = res.list || [];

    // 计算统计
    const stats = { total: 0, normal: 0, late: 0, early: 0, absent: 0 };
    for (const record of dataSource.value) {
      stats.total++;
      switch (record.status) {
        case 'ABSENT': {
          {
            stats.absent++;
            // No default
          }
          break;
        }
        case 'EARLY_LEAVE': {
          stats.early++;
          break;
        }
        case 'LATE': {
          stats.late++;
          break;
        }
        case 'NORMAL': {
          stats.normal++;
          break;
        }
      }
    }
    statistics.value = stats;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadTodayAttendance() {
  try {
    const res = await getTodayAttendance();
    todayRecord.value = res;
  } catch (error) {
    console.error('获取今日考勤失败', error);
  }
}

async function handleCheckIn() {
  try {
    await checkIn();
    message.success('上班打卡成功');
    loadTodayAttendance();
    if (selectedDate.value.isSame(dayjs(), 'day')) {
      loadData();
    }
  } catch (error: any) {
    message.error(error.message || '打卡失败');
  }
}

async function handleCheckOut() {
  try {
    await checkOut();
    message.success('下班打卡成功');
    loadTodayAttendance();
    if (selectedDate.value.isSame(dayjs(), 'day')) {
      loadData();
    }
  } catch (error: any) {
    message.error(error.message || '打卡失败');
  }
}

function handleDateChange(date: any) {
  selectedDate.value = date || dayjs();
  loadData();
}

function handleView(record: Record<string, any>) {
  currentRecord.value = record as AttendanceRecord;
  detailVisible.value = true;
}

function formatTime(time?: string) {
  return time ? dayjs(time).format('HH:mm:ss') : '-';
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

function formatWorkHours(record: Record<string, any>) {
  if (!record.checkInTime || !record.checkOutTime) return '-';
  const checkIn = dayjs(record.checkInTime);
  const checkOut = dayjs(record.checkOutTime);
  const hours = checkOut.diff(checkIn, 'hour', true);
  return `${hours.toFixed(1)} 小时`;
}

onMounted(() => {
  loadData();
  loadTodayAttendance();
});
</script>

<template>
  <Page title="考勤管理" description="查看员工考勤记录">
    <!-- 今日打卡卡片 -->
    <Card style="margin-bottom: 16px">
      <Row :gutter="16" align="middle">
        <Col :span="12">
          <div style="margin-bottom: 8px; font-size: 16px; font-weight: 500">
            今日考勤
          </div>
          <div v-if="todayRecord">
            <Space>
              <span>上班: {{ formatTime(todayRecord.checkInTime) }}</span>
              <span>下班: {{ formatTime(todayRecord.checkOutTime) }}</span>
              <Tag :color="statusMap[todayRecord.status]?.color">
                {{ statusMap[todayRecord.status]?.text || todayRecord.status }}
              </Tag>
            </Space>
          </div>
          <div v-else style="color: #999">暂无打卡记录</div>
        </Col>
        <Col :span="12" style="text-align: right">
          <Space>
            <Button
              type="primary"
              :disabled="!!todayRecord?.checkInTime"
              @click="handleCheckIn"
            >
              上班打卡
            </Button>
            <Button
              :disabled="
                !todayRecord?.checkInTime || !!todayRecord?.checkOutTime
              "
              @click="handleCheckOut"
            >
              下班打卡
            </Button>
          </Space>
        </Col>
      </Row>
    </Card>

    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :span="6">
        <Card>
          <Statistic title="今日出勤" :value="statistics.normal" suffix="人" />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="迟到"
            :value="statistics.late"
            suffix="人"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="早退"
            :value="statistics.early"
            suffix="人"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="缺勤"
            :value="statistics.absent"
            suffix="人"
            :value-style="{ color: '#ff4d4f' }"
          />
        </Card>
      </Col>
    </Row>

    <!-- 考勤列表 -->
    <Card>
      <template #extra>
        <Space>
          <DatePicker :value="selectedDate" @change="handleDateChange" />
        </Space>
      </template>
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{ pageSize: 20 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'attendanceDate'">
            {{ formatDate(record.attendanceDate) }}
          </template>
          <template v-else-if="column.key === 'checkInTime'">
            {{ formatTime(record.checkInTime) }}
          </template>
          <template v-else-if="column.key === 'checkOutTime'">
            {{ formatTime(record.checkOutTime) }}
          </template>
          <template v-else-if="column.key === 'workHours'">
            {{ formatWorkHours(record) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ statusMap[record.status]?.text || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="handleView(record)">详情</a>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 详情弹窗 -->
    <Modal v-model:open="detailVisible" title="考勤详情" :footer="null">
      <Descriptions v-if="currentRecord" :column="2" bordered size="small">
        <DescriptionsItem label="员工姓名">
          {{ currentRecord.employeeName }}
        </DescriptionsItem>
        <DescriptionsItem label="部门">
          {{ currentRecord.departmentName }}
        </DescriptionsItem>
        <DescriptionsItem label="日期">
          {{ formatDate(currentRecord.attendanceDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentRecord.status]?.color">
            {{ statusMap[currentRecord.status]?.text || currentRecord.status }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="上班打卡">
          {{ formatTime(currentRecord.checkInTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="下班打卡">
          {{ formatTime(currentRecord.checkOutTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="工作时长">
          {{ formatWorkHours(currentRecord) }}
        </DescriptionsItem>
        <DescriptionsItem label="打卡地点">
          {{ currentRecord.location || '-' }}
        </DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.remark" label="备注" :span="2">
          {{ currentRecord.remark }}
        </DescriptionsItem>
      </Descriptions>
    </Modal>
  </Page>
</template>
