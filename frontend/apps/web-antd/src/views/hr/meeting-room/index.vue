<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { MeetingBooking, MeetingRoom } from '#/api/hr/types';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Space,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  bookMeetingRoom,
  cancelBooking,
  createMeetingRoom,
  deleteMeetingRoom,
  fetchBookingList,
  fetchMeetingRoomList,
} from '#/api/hr/meeting-room';

defineOptions({ name: 'MeetingRoomManagement' });

// ==================== 状态定义 ====================

const roomModalVisible = ref(false);
const roomModalLoading = ref(false);
const bookingModalVisible = ref(false);
const bookingModalLoading = ref(false);

const roomForm = reactive({
  id: undefined as number | undefined,
  name: '',
  location: '',
  capacity: 10,
  equipment: '',
  description: '',
});

const bookingForm = reactive({
  roomId: undefined as number | undefined,
  title: '',
  startTime: '',
  endTime: '',
  participants: '',
  description: '',
});

// ==================== 常量选项 ====================

const roomStatusColorMap: Record<string, string> = {
  AVAILABLE: 'green',
  IN_USE: 'blue',
  MAINTENANCE: 'orange',
};

const roomStatusTextMap: Record<string, string> = {
  AVAILABLE: '可用',
  IN_USE: '使用中',
  MAINTENANCE: '维护中',
};

const bookingStatusColorMap: Record<string, string> = {
  BOOKED: 'blue',
  IN_PROGRESS: 'green',
  COMPLETED: 'default',
  CANCELLED: 'red',
};

const bookingStatusTextMap: Record<string, string> = {
  BOOKED: '已预约',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
};

// ==================== 会议室表格配置 ====================

const roomColumns: VxeGridProps['columns'] = [
  { title: '会议室名称', field: 'name', width: 150 },
  { title: '位置', field: 'location', width: 150 },
  { title: '容纳人数', field: 'capacity', width: 100 },
  { title: '设备', field: 'equipment', minWidth: 200, showOverflow: true },
  {
    title: '状态',
    field: 'status',
    width: 100,
    slots: { default: 'roomStatus' },
  },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right',
    slots: { default: 'roomAction' },
  },
];

async function loadRoomData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const res = (await fetchMeetingRoomList({
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  })) as any;
  return {
    items: res.list || res || [],
    total: res.total || (Array.isArray(res) ? res.length : 0),
  };
}

const [RoomGrid, roomGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: roomColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadRoomData } },
  },
});

// ==================== 预约表格配置 ====================

const bookingColumns: VxeGridProps['columns'] = [
  { title: '会议室', field: 'roomName', width: 120 },
  { title: '会议主题', field: 'title', minWidth: 150 },
  { title: '预约人', field: 'userName', width: 100 },
  { title: '开始时间', field: 'startTime', width: 150 },
  { title: '结束时间', field: 'endTime', width: 150 },
  {
    title: '状态',
    field: 'status',
    width: 100,
    slots: { default: 'bookingStatus' },
  },
  {
    title: '操作',
    field: 'action',
    width: 100,
    fixed: 'right',
    slots: { default: 'bookingAction' },
  },
];

async function loadBookingData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const res = await fetchBookingList({
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  });
  return { items: res.list || [], total: res.total || 0 };
}

const [BookingGrid, bookingGridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: bookingColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadBookingData } },
  },
});

// ==================== 会议室操作 ====================

function handleAddRoom() {
  Object.assign(roomForm, {
    id: undefined,
    name: '',
    location: '',
    capacity: 10,
    equipment: '',
    description: '',
  });
  roomModalVisible.value = true;
}

function handleEditRoom(row: MeetingRoom) {
  Object.assign(roomForm, {
    id: row.id,
    name: row.name,
    location: row.location,
    capacity: row.capacity,
    equipment: row.equipment,
    description: row.description,
  });
  roomModalVisible.value = true;
}

async function handleSaveRoom() {
  if (!roomForm.name) {
    message.warning('请输入会议室名称');
    return;
  }

  roomModalLoading.value = true;
  try {
    await createMeetingRoom(roomForm);
    message.success('保存成功');
    roomModalVisible.value = false;
    roomGridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '保存失败');
  } finally {
    roomModalLoading.value = false;
  }
}

async function handleDeleteRoom(row: MeetingRoom) {
  try {
    await deleteMeetingRoom(row.id);
    message.success('删除成功');
    roomGridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '删除失败');
  }
}

// ==================== 预约操作 ====================

function handleBookRoom(row: MeetingRoom) {
  Object.assign(bookingForm, {
    roomId: row.id,
    title: '',
    startTime: '',
    endTime: '',
    participants: '',
    description: '',
  });
  bookingModalVisible.value = true;
}

async function handleSubmitBooking() {
  if (!bookingForm.title) {
    message.warning('请输入会议主题');
    return;
  }
  if (!bookingForm.startTime || !bookingForm.endTime) {
    message.warning('请选择会议时间');
    return;
  }
  if (!bookingForm.roomId) {
    message.warning('请选择会议室');
    return;
  }

  bookingModalLoading.value = true;
  try {
    await bookMeetingRoom({
      roomId: bookingForm.roomId,
      title: bookingForm.title,
      startTime: bookingForm.startTime,
      endTime: bookingForm.endTime,
    });
    message.success('预约成功');
    bookingModalVisible.value = false;
    bookingGridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '预约失败');
  } finally {
    bookingModalLoading.value = false;
  }
}

async function handleCancelBooking(row: MeetingBooking) {
  try {
    await cancelBooking(row.id);
    message.success('取消成功');
    bookingGridApi.reload();
  } catch (error: any) {
    message.error(error?.message || '取消失败');
  }
}

onMounted(() => {});
</script>

<template>
  <Page title="会议室管理" description="会议室信息管理与预约">
    <div class="space-y-4 p-4">
      <!-- 会议室列表 -->
      <Card title="会议室列表">
        <template #extra>
          <Button type="primary" @click="handleAddRoom">
            <Plus class="size-4" />新增会议室
          </Button>
        </template>
        <RoomGrid>
          <template #roomStatus="{ row }">
            <Tag :color="roomStatusColorMap[row.status]">
              {{ roomStatusTextMap[row.status] }}
            </Tag>
          </template>
          <template #roomAction="{ row }">
            <Space>
              <a
                :class="{
                  'cursor-not-allowed opacity-50': row.status !== 'AVAILABLE',
                }"
                @click="row.status === 'AVAILABLE' && handleBookRoom(row)"
                >预约</a
              >
              <a @click="handleEditRoom(row)">编辑</a>
              <a style="color: red" @click="handleDeleteRoom(row)">删除</a>
            </Space>
          </template>
        </RoomGrid>
      </Card>

      <!-- 预约记录 -->
      <Card title="预约记录">
        <BookingGrid>
          <template #bookingStatus="{ row }">
            <Tag :color="bookingStatusColorMap[row.status]">
              {{ bookingStatusTextMap[row.status] }}
            </Tag>
          </template>
          <template #bookingAction="{ row }">
            <a
              v-if="row.status === 'BOOKED'"
              style="color: red"
              @click="handleCancelBooking(row)"
              >取消</a
            >
          </template>
        </BookingGrid>
      </Card>

      <!-- 会议室弹窗 -->
      <Modal
        v-model:open="roomModalVisible"
        :title="roomForm.id ? '编辑会议室' : '新增会议室'"
        :confirm-loading="roomModalLoading"
        @ok="handleSaveRoom"
      >
        <Form :model="roomForm" layout="vertical">
          <FormItem label="会议室名称" required>
            <Input
              v-model:value="roomForm.name"
              placeholder="请输入会议室名称"
            />
          </FormItem>
          <FormItem label="位置">
            <Input v-model:value="roomForm.location" placeholder="请输入位置" />
          </FormItem>
          <FormItem label="容纳人数">
            <InputNumber
              v-model:value="roomForm.capacity"
              :min="1"
              style="width: 100%"
            />
          </FormItem>
          <FormItem label="设备">
            <Input
              v-model:value="roomForm.equipment"
              placeholder="如：投影仪、白板、视频会议设备"
            />
          </FormItem>
          <FormItem label="描述">
            <Input.TextArea v-model:value="roomForm.description" :rows="3" />
          </FormItem>
        </Form>
      </Modal>

      <!-- 预约弹窗 -->
      <Modal
        v-model:open="bookingModalVisible"
        title="预约会议室"
        :confirm-loading="bookingModalLoading"
        @ok="handleSubmitBooking"
      >
        <Form :model="bookingForm" layout="vertical">
          <FormItem label="会议主题" required>
            <Input
              v-model:value="bookingForm.title"
              placeholder="请输入会议主题"
            />
          </FormItem>
          <FormItem label="开始时间" required>
            <DatePicker
              v-model:value="bookingForm.startTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              placeholder="选择开始时间"
              style="width: 100%"
            />
          </FormItem>
          <FormItem label="结束时间" required>
            <DatePicker
              v-model:value="bookingForm.endTime"
              show-time
              format="YYYY-MM-DD HH:mm"
              placeholder="选择结束时间"
              style="width: 100%"
            />
          </FormItem>
          <FormItem label="参会人员">
            <Input
              v-model:value="bookingForm.participants"
              placeholder="请输入参会人员，多人用逗号分隔"
            />
          </FormItem>
          <FormItem label="会议描述">
            <Input.TextArea v-model:value="bookingForm.description" :rows="3" />
          </FormItem>
        </Form>
      </Modal>
    </div>
  </Page>
</template>
