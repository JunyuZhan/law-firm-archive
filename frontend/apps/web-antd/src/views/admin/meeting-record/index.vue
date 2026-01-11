<script setup lang="ts">
import type {
  CreateMeetingRecordCommand,
  MeetingRecordDTO,
} from '#/api/admin/meeting-record';
import type { MeetingRoom } from '#/api/hr/types';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Textarea,
} from 'ant-design-vue';

import {
  createMeetingRecord,
  createMeetingRecordFromBooking,
  getMeetingRecordDetail,
  getMeetingRecordsByDateRange,
  getMeetingRecordsByRoom,
} from '#/api/admin/meeting-record';
import { getAvailableMeetingRooms } from '#/api/hr/meeting-room';

defineOptions({ name: 'MeetingRecordManagement' });

const { RangePicker } = DatePicker;

// 搜索表单
const searchForm = reactive({
  startDate: '',
  endDate: '',
  roomId: undefined as number | undefined,
});

// 表格列
const columns = [
  { title: '记录编号', dataIndex: 'recordNo', key: 'recordNo', width: 120 },
  { title: '会议室', dataIndex: 'roomName', key: 'roomName', width: 120 },
  { title: '会议主题', dataIndex: 'title', key: 'title', width: 200 },
  {
    title: '会议日期',
    dataIndex: 'meetingDate',
    key: 'meetingDate',
    width: 120,
  },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 100 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 100 },
  {
    title: '组织人',
    dataIndex: 'organizerName',
    key: 'organizerName',
    width: 100,
  },
  {
    title: '参会人员',
    dataIndex: 'attendees',
    key: 'attendees',
    ellipsis: true,
  },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<MeetingRecordDTO[]>([]);
const loading = ref(false);

// 新增弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const recordForm = reactive<CreateMeetingRecordCommand>({
  bookingId: undefined,
  roomId: undefined as unknown as number,
  title: '',
  meetingDate: '',
  startTime: '',
  endTime: '',
  content: '',
  decisions: '',
  actionItems: '',
  attachmentUrl: '',
});

// 详情弹窗
const detailModalVisible = ref(false);
const detailData = ref<MeetingRecordDTO | null>(null);

// 会议室列表
const roomList = ref<MeetingRoom[]>([]);

// 获取会议记录
async function fetchData() {
  loading.value = true;
  try {
    let data: MeetingRecordDTO[];
    if (searchForm.roomId) {
      data = await getMeetingRecordsByRoom(searchForm.roomId);
    } else if (searchForm.startDate && searchForm.endDate) {
      data = await getMeetingRecordsByDateRange(
        searchForm.startDate,
        searchForm.endDate,
      );
    } else {
      // 默认查询最近一个月
      const endDate = new Date();
      const startDate = new Date();
      startDate.setMonth(startDate.getMonth() - 1);
      data = await getMeetingRecordsByDateRange(
        startDate.toISOString().split('T')[0] || '',
        endDate.toISOString().split('T')[0] || '',
      );
    }
    tableData.value = data || [];
  } catch (error) {
    console.error('获取会议记录失败:', error);
    message.error('获取会议记录失败');
  } finally {
    loading.value = false;
  }
}

// 获取会议室列表
async function fetchRooms() {
  try {
    const data = await getAvailableMeetingRooms();
    roomList.value = data || [];
  } catch (error) {
    console.error('获取会议室列表失败:', error);
    roomList.value = [];
  }
}

// 搜索
function handleSearch() {
  fetchData();
}

// 重置
function handleReset() {
  searchForm.startDate = '';
  searchForm.endDate = '';
  searchForm.roomId = undefined;
  fetchData();
}

// 日期范围变化
function handleDateRangeChange(dates: any) {
  if (dates && dates.length === 2) {
    searchForm.startDate = dates[0].format('YYYY-MM-DD');
    searchForm.endDate = dates[1].format('YYYY-MM-DD');
  } else {
    searchForm.startDate = '';
    searchForm.endDate = '';
  }
}

// 新增会议记录
function handleAdd() {
  Object.assign(recordForm, {
    bookingId: undefined,
    roomId: undefined,
    title: '',
    meetingDate: '',
    startTime: '',
    endTime: '',
    content: '',
    decisions: '',
    actionItems: '',
    attachmentUrl: '',
  });
  modalVisible.value = true;
  fetchRooms();
}

// 提交会议记录
async function handleSubmit() {
  if (!recordForm.roomId) {
    message.warning('请选择会议室');
    return;
  }
  if (!recordForm.title) {
    message.warning('请输入会议主题');
    return;
  }
  if (!recordForm.meetingDate) {
    message.warning('请选择会议日期');
    return;
  }
  if (!recordForm.startTime) {
    message.warning('请选择开始时间');
    return;
  }

  modalLoading.value = true;
  try {
    await (recordForm.bookingId
      ? createMeetingRecordFromBooking(recordForm.bookingId, recordForm)
      : createMeetingRecord(recordForm));
    message.success('创建成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '创建失败');
  } finally {
    modalLoading.value = false;
  }
}

// 查看详情
async function handleView(record: Record<string, any>) {
  try {
    const detail = await getMeetingRecordDetail(record.id);
    detailData.value = detail;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error?.message || '获取详情失败');
  }
}

onMounted(() => {
  fetchRooms();
  fetchData();
});
</script>

<template>
  <Page title="会议记录" description="会议记录管理">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Form layout="inline" :model="searchForm" @finish="handleSearch">
          <FormItem label="日期范围">
            <RangePicker style="width: 240px" @change="handleDateRangeChange" />
          </FormItem>
          <FormItem label="会议室">
            <Select
              v-model:value="searchForm.roomId"
              placeholder="请选择会议室"
              allow-clear
              style="width: 150px"
            >
              <Select.Option
                v-for="room in roomList"
                :key="room.id"
                :value="room.id"
              >
                {{ room.name }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem>
            <Space>
              <Button type="primary" html-type="submit">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新建会议记录</Button>
            </Space>
          </FormItem>
        </Form>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a @click="handleView(record)">查看</a>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增会议记录弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建会议记录"
      :confirm-loading="modalLoading"
      width="700px"
      @ok="handleSubmit"
    >
      <Form :model="recordForm" layout="vertical">
        <FormItem label="会议室" required>
          <Select v-model:value="recordForm.roomId" placeholder="请选择会议室">
            <Select.Option
              v-for="room in roomList"
              :key="room.id"
              :value="room.id"
            >
              {{ room.name }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="会议主题" required>
          <Input
            v-model:value="recordForm.title"
            placeholder="请输入会议主题"
          />
        </FormItem>
        <FormItem label="会议日期" required>
          <DatePicker
            v-model:value="recordForm.meetingDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="开始时间" required>
          <Input
            v-model:value="recordForm.startTime"
            placeholder="例如：09:00"
          />
        </FormItem>
        <FormItem label="结束时间">
          <Input v-model:value="recordForm.endTime" placeholder="例如：10:00" />
        </FormItem>
        <FormItem label="会议内容">
          <Textarea
            v-model:value="recordForm.content"
            placeholder="请输入会议内容"
            :rows="4"
          />
        </FormItem>
        <FormItem label="会议决议">
          <Textarea
            v-model:value="recordForm.decisions"
            placeholder="请输入会议决议"
            :rows="3"
          />
        </FormItem>
        <FormItem label="行动计划">
          <Textarea
            v-model:value="recordForm.actionItems"
            placeholder="请输入行动计划"
            :rows="3"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="会议记录详情"
      width="700px"
      :footer="null"
    >
      <div v-if="detailData" style="line-height: 2">
        <p><strong>记录编号:</strong> {{ detailData.recordNo || '-' }}</p>
        <p><strong>会议室:</strong> {{ detailData.roomName || '-' }}</p>
        <p><strong>会议主题:</strong> {{ detailData.title || '-' }}</p>
        <p><strong>会议日期:</strong> {{ detailData.meetingDate || '-' }}</p>
        <p><strong>开始时间:</strong> {{ detailData.startTime || '-' }}</p>
        <p><strong>结束时间:</strong> {{ detailData.endTime || '-' }}</p>
        <p><strong>组织人:</strong> {{ detailData.organizerName || '-' }}</p>
        <p><strong>参会人员:</strong> {{ detailData.attendees || '-' }}</p>
        <p v-if="detailData.content">
          <strong>会议内容:</strong> {{ detailData.content }}
        </p>
        <p v-if="detailData.decisions">
          <strong>会议决议:</strong> {{ detailData.decisions }}
        </p>
        <p v-if="detailData.actionItems">
          <strong>行动计划:</strong> {{ detailData.actionItems }}
        </p>
      </div>
    </Modal>
  </Page>
</template>
