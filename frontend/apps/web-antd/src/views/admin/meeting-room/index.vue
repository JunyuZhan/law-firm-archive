<script setup lang="ts">
import type { MeetingBooking, MeetingRoom } from '#/api/hr/types';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  bookMeetingRoom,
  cancelBooking,
  createMeetingRoom,
  deleteMeetingRoom,
  fetchBookingList,
  fetchMeetingRoomList,
  updateMeetingRoom,
  updateMeetingRoomStatus,
} from '#/api/hr/meeting-room';
import { usePermission } from '#/hooks/usePermission';

defineOptions({ name: 'AdminMeetingRoom' });

const { hasPermission } = usePermission();

// 是否有管理权限（行政角色）
const canManage = computed(() => hasPermission('admin:meeting:manage'));

const loading = ref(false);
const roomList = ref<MeetingRoom[]>([]);
const bookingList = ref<MeetingBooking[]>([]);
const selectedRoom = ref<number | undefined>(undefined);
const selectedDate = ref(dayjs());
const activeTab = ref('booking');

// 预约相关
const bookingModalVisible = ref(false);
const detailVisible = ref(false);
const currentBooking = ref<MeetingBooking | null>(null);

const bookingForm = ref({
  roomId: undefined as number | undefined,
  title: '',
  startTime: undefined as any,
  endTime: undefined as any,
  description: '',
});

// 会议室管理相关
const roomModalVisible = ref(false);
const editingRoomId = ref<null | number>(null);

const roomForm = ref({
  name: '',
  location: '',
  capacity: 10,
  equipment: '',
});

const bookingColumns = [
  { title: '会议室', dataIndex: 'roomName', key: 'roomName', width: 120 },
  {
    title: '会议主题',
    dataIndex: 'title',
    key: 'title',
    width: 200,
    ellipsis: true,
  },
  { title: '预约人', dataIndex: 'userName', key: 'userName', width: 100 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120 },
];

const roomColumns = [
  { title: '会议室名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '位置', dataIndex: 'location', key: 'location', width: 150 },
  { title: '容纳人数', dataIndex: 'capacity', key: 'capacity', width: 100 },
  {
    title: '设备',
    dataIndex: 'equipment',
    key: 'equipment',
    width: 200,
    ellipsis: true,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 180 },
];

const statusMap: Record<string, { color: string; text: string }> = {
  BOOKED: { color: 'processing', text: '已预约' },
  IN_PROGRESS: { color: 'success', text: '进行中' },
  COMPLETED: { color: 'default', text: '已结束' },
  CANCELLED: { color: 'default', text: '已取消' },
};

const roomStatusMap: Record<string, { color: string; text: string }> = {
  AVAILABLE: { color: 'success', text: '可用' },
  IN_USE: { color: 'processing', text: '使用中' },
  MAINTENANCE: { color: 'warning', text: '维护中' },
};

async function loadRooms() {
  try {
    const res = await fetchMeetingRoomList();
    roomList.value = res || [];
  } catch (error: any) {
    message.error(error.message || '加载会议室失败');
  }
}

async function loadBookings() {
  loading.value = true;
  try {
    const res = await fetchBookingList({
      roomId: selectedRoom.value,
      startTime: selectedDate.value.format('YYYY-MM-DD'),
      pageNum: 1,
      pageSize: 50,
    });
    bookingList.value = res.list || [];
  } catch (error: any) {
    message.error(error.message || '加载预约失败');
  } finally {
    loading.value = false;
  }
}

function handleRoomSelect(roomId: number) {
  selectedRoom.value = selectedRoom.value === roomId ? undefined : roomId;
  loadBookings();
}

function handleDateChange(date: any) {
  selectedDate.value = date || dayjs();
  loadBookings();
}

// ========== 预约功能 ==========
function handleAddBooking() {
  bookingForm.value = {
    roomId: selectedRoom.value,
    title: '',
    startTime: undefined,
    endTime: undefined,
    description: '',
  };
  bookingModalVisible.value = true;
}

async function handleSubmitBooking() {
  if (
    !bookingForm.value.roomId ||
    !bookingForm.value.title ||
    !bookingForm.value.startTime ||
    !bookingForm.value.endTime
  ) {
    message.error('请填写必填项');
    return;
  }
  try {
    await bookMeetingRoom({
      roomId: bookingForm.value.roomId,
      title: bookingForm.value.title,
      startTime: bookingForm.value.startTime.format('YYYY-MM-DD HH:mm:ss'),
      endTime: bookingForm.value.endTime.format('YYYY-MM-DD HH:mm:ss'),
    });
    message.success('预约成功');
    bookingModalVisible.value = false;
    loadBookings();
  } catch (error: any) {
    message.error(error.message || '预约失败');
  }
}

function handleViewBooking(record: Record<string, any>) {
  currentBooking.value = record as MeetingBooking;
  detailVisible.value = true;
}

async function handleCancelBooking(id: number) {
  try {
    await cancelBooking(id);
    message.success('已取消预约');
    loadBookings();
  } catch (error: any) {
    message.error(error.message || '取消失败');
  }
}

// ========== 会议室管理功能 ==========
function handleAddRoom() {
  editingRoomId.value = null;
  roomForm.value = {
    name: '',
    location: '',
    capacity: 10,
    equipment: '',
  };
  roomModalVisible.value = true;
}

function handleEditRoom(record: Record<string, any>) {
  editingRoomId.value = record.id;
  roomForm.value = {
    name: record.name,
    location: record.location || '',
    capacity: record.capacity,
    equipment: record.equipment || '',
  };
  roomModalVisible.value = true;
}

async function handleSubmitRoom() {
  if (!roomForm.value.name) {
    message.error('请填写会议室名称');
    return;
  }
  try {
    if (editingRoomId.value) {
      await updateMeetingRoom(editingRoomId.value, roomForm.value);
      message.success('更新成功');
    } else {
      await createMeetingRoom(roomForm.value);
      message.success('创建成功');
    }
    roomModalVisible.value = false;
    loadRooms();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  }
}

async function handleDeleteRoom(id: number) {
  try {
    await deleteMeetingRoom(id);
    message.success('删除成功');
    loadRooms();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

async function handleToggleRoomStatus(record: Record<string, any>) {
  const newStatus =
    record.status === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE';
  try {
    await updateMeetingRoomStatus(record.id, newStatus);
    message.success('状态更新成功');
    loadRooms();
  } catch (error: any) {
    message.error(error.message || '更新失败');
  }
}

function formatDateTime(time?: string) {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-';
}

function getRoomStatus(room: MeetingRoom) {
  if (room.status === 'MAINTENANCE') return 'MAINTENANCE';
  const now = dayjs();
  const hasActiveBooking = bookingList.value.some(
    (b) =>
      b.roomId === room.id &&
      b.status === 'IN_PROGRESS' &&
      dayjs(b.startTime).isBefore(now) &&
      dayjs(b.endTime).isAfter(now),
  );
  return hasActiveBooking ? 'IN_USE' : 'AVAILABLE';
}

onMounted(() => {
  loadRooms();
  loadBookings();
});
</script>

<template>
  <Page title="会议室预约" description="预约会议室和管理会议室">
    <Tabs v-model:active-key="activeTab">
      <Tabs.TabPane key="booking" tab="预约会议室">
        <Row :gutter="16">
          <!-- 会议室列表 -->
          <Col :span="6">
            <Card title="会议室列表" size="small">
              <div
                v-for="room in roomList"
                :key="room.id"
                :style="{
                  marginBottom: '12px',
                  padding: '12px',
                  border:
                    selectedRoom === room.id
                      ? '2px solid #1890ff'
                      : '1px solid #f0f0f0',
                  borderRadius: '4px',
                  cursor:
                    room.status === 'MAINTENANCE' ? 'not-allowed' : 'pointer',
                  background:
                    selectedRoom === room.id
                      ? '#e6f7ff'
                      : room.status === 'MAINTENANCE'
                        ? '#f5f5f5'
                        : '#fff',
                  opacity: room.status === 'MAINTENANCE' ? 0.6 : 1,
                }"
                @click="
                  room.status !== 'MAINTENANCE' && handleRoomSelect(room.id)
                "
              >
                <div style="margin-bottom: 4px; font-weight: 500">
                  {{ room.name }}
                </div>
                <div style="margin-bottom: 4px; font-size: 12px; color: #999">
                  容纳 {{ room.capacity }} 人 |
                  {{ room.location || '未设置位置' }}
                </div>
                <Tag
                  :color="roomStatusMap[getRoomStatus(room)]?.color"
                  size="small"
                >
                  {{ roomStatusMap[getRoomStatus(room)]?.text }}
                </Tag>
              </div>
              <div
                v-if="roomList.length === 0"
                style="padding: 20px; color: #999; text-align: center"
              >
                暂无会议室
              </div>
            </Card>
          </Col>

          <!-- 预约列表 -->
          <Col :span="18">
            <Card size="small">
              <template #extra>
                <Space>
                  <DatePicker
                    :value="selectedDate"
                    @change="handleDateChange"
                  />
                  <Button type="primary" @click="handleAddBooking">
                    <Plus />预约会议室
                  </Button>
                </Space>
              </template>
              <Table
                :columns="bookingColumns"
                :data-source="bookingList"
                :loading="loading"
                :pagination="{ pageSize: 10 }"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template
                    v-if="
                      column.key === 'startTime' || column.key === 'endTime'
                    "
                  >
                    {{ formatDateTime(record[column.key as string]) }}
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <Tag :color="statusMap[record.status]?.color || 'default'">
                      {{ statusMap[record.status]?.text || record.status }}
                    </Tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <Space>
                      <a @click="handleViewBooking(record)">查看</a>
                      <Popconfirm
                        v-if="record.status === 'BOOKED'"
                        title="确定要取消此预约吗？"
                        @confirm="handleCancelBooking(record.id)"
                      >
                        <a style="color: #ff4d4f">取消</a>
                      </Popconfirm>
                    </Space>
                  </template>
                </template>
              </Table>
            </Card>
          </Col>
        </Row>
      </Tabs.TabPane>

      <!-- 会议室管理 Tab - 仅管理员可见 -->
      <Tabs.TabPane v-if="canManage" key="manage" tab="会议室管理">
        <Card size="small">
          <template #extra>
            <Button type="primary" @click="handleAddRoom">
              <Plus />添加会议室
            </Button>
          </template>
          <Table
            :columns="roomColumns"
            :data-source="roomList"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag
                  :color="roomStatusMap[record.status || 'AVAILABLE']?.color"
                >
                  {{ roomStatusMap[record.status || 'AVAILABLE']?.text }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <a @click="handleEditRoom(record)">编辑</a>
                  <a @click="handleToggleRoomStatus(record)">
                    {{ record.status === 'MAINTENANCE' ? '启用' : '维护' }}
                  </a>
                  <Popconfirm
                    title="确定删除此会议室？"
                    @confirm="handleDeleteRoom(record.id)"
                  >
                    <a style="color: #ff4d4f">删除</a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </Tabs.TabPane>
    </Tabs>

    <!-- 预约弹窗 -->
    <Modal
      v-model:open="bookingModalVisible"
      title="预约会议室"
      @ok="handleSubmitBooking"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="会议室" required>
          <Select
            v-model:value="bookingForm.roomId"
            placeholder="选择会议室"
            :options="
              roomList
                .filter((r) => r.status !== 'MAINTENANCE')
                .map((r) => ({
                  label: `${r.name} (${r.capacity}人)`,
                  value: r.id,
                }))
            "
          />
        </FormItem>
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
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="结束时间" required>
          <DatePicker
            v-model:value="bookingForm.endTime"
            show-time
            format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="会议说明">
          <Textarea
            v-model:value="bookingForm.description"
            :rows="2"
            placeholder="会议说明（可选）"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 预约详情弹窗 -->
    <Modal v-model:open="detailVisible" title="预约详情" :footer="null">
      <Descriptions v-if="currentBooking" :column="2" bordered size="small">
        <DescriptionsItem label="会议室">
          {{ currentBooking.roomName }}
        </DescriptionsItem>
        <DescriptionsItem label="预约人">
          {{ currentBooking.userName }}
        </DescriptionsItem>
        <DescriptionsItem label="会议主题" :span="2">
          {{ currentBooking.title }}
        </DescriptionsItem>
        <DescriptionsItem label="开始时间">
          {{ formatDateTime(currentBooking.startTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="结束时间">
          {{ formatDateTime(currentBooking.endTime) }}
        </DescriptionsItem>
        <DescriptionsItem label="状态" :span="2">
          <Tag :color="statusMap[currentBooking.status]?.color">
            {{ statusMap[currentBooking.status]?.text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem
          v-if="currentBooking.description"
          label="会议说明"
          :span="2"
        >
          {{ currentBooking.description }}
        </DescriptionsItem>
      </Descriptions>
    </Modal>

    <!-- 会议室管理弹窗 -->
    <Modal
      v-model:open="roomModalVisible"
      :title="editingRoomId ? '编辑会议室' : '添加会议室'"
      @ok="handleSubmitRoom"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="名称" required>
          <Input v-model:value="roomForm.name" placeholder="如：大会议室A" />
        </FormItem>
        <FormItem label="位置">
          <Input v-model:value="roomForm.location" placeholder="如：3楼东侧" />
        </FormItem>
        <FormItem label="容纳人数">
          <InputNumber
            v-model:value="roomForm.capacity"
            :min="1"
            :max="200"
            style="width: 120px"
          />
        </FormItem>
        <FormItem label="设备">
          <Input
            v-model:value="roomForm.equipment"
            placeholder="如：投影仪、视频会议系统"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
