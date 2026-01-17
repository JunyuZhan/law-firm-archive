<script setup lang="ts">
import type { CreateScheduleCommand, ScheduleDTO } from '#/api/matter/schedule';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
  TimeRangePicker,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getMatterSelectOptions } from '#/api/matter';
import {
  cancelSchedule,
  createSchedule,
  deleteSchedule,
  getMyTodaySchedules,
  getSchedules,
  REMINDER_OPTIONS,
  SCHEDULE_TYPE_OPTIONS,
  updateSchedule,
} from '#/api/matter/schedule';
import { getOffDays } from '#/api/system/holiday';

defineOptions({ name: 'ScheduleManagement' });

// 响应式布局
const { isMobile } = useResponsive();

// 状态
const loading = ref(false);
const schedules = ref<ScheduleDTO[]>([]);
const todaySchedules = ref<ScheduleDTO[]>([]);
const activeTab = ref<'calendar' | 'list'>('calendar');
const offDays = ref<Set<string>>(new Set()); // 休息日集合

// 日历相关
const currentMonth = ref(dayjs());

// 获取日历视图的日期范围（固定从周日开始到周六结束）
function getCalendarRange(month: dayjs.Dayjs) {
  const monthStart = month.startOf('month');
  const monthEnd = month.endOf('month');
  // 找到该月第一天所在周的周日（day() 返回 0-6，0 是周日）
  const start = monthStart.subtract(monthStart.day(), 'day');
  // 找到该月最后一天所在周的周六
  const end = monthEnd.add(6 - monthEnd.day(), 'day');
  return { start, end };
}

const calendarDays = computed(() => {
  const { start, end } = getCalendarRange(currentMonth.value);
  const days: dayjs.Dayjs[] = [];
  let day = start;
  while (day.isBefore(end) || day.isSame(end, 'day')) {
    days.push(day);
    day = day.add(1, 'day');
  }
  return days;
});

// 弹窗相关
const modalVisible = ref(false);
const modalTitle = ref('新建日程');
const editingId = ref<null | number>(null);
const matters = ref<Array<{ id: number; matterNo: string; name: string }>>([]);

const formData = ref<
  Partial<CreateScheduleCommand> & {
    date?: dayjs.Dayjs;
    time?: [dayjs.Dayjs, dayjs.Dayjs] | null;
  }
>({
  matterId: undefined,
  title: '',
  description: '',
  location: '',
  scheduleType: 'MEETING',
  startTime: undefined,
  endTime: undefined,
  allDay: false,
  reminderMinutes: 30,
  date: undefined,
  time: null,
});

// 查询参数
const queryParams = ref({
  startTime: dayjs().startOf('month').format('YYYY-MM-DDTHH:mm:ss'),
  endTime: dayjs().endOf('month').format('YYYY-MM-DDTHH:mm:ss'),
  scheduleType: undefined as string | undefined,
});

// 表格列（响应式）
const columns = computed(() => {
  const baseColumns = [
    {
      title: '标题',
      dataIndex: 'title',
      key: 'title',
      width: isMobile.value ? 150 : 200,
      ellipsis: true,
      mobileShow: true,
    },
    {
      title: '类型',
      dataIndex: 'scheduleTypeName',
      key: 'scheduleTypeName',
      width: 80,
      mobileShow: true,
    },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
    {
      title: '地点',
      dataIndex: 'location',
      key: 'location',
      width: 150,
      ellipsis: true,
    },
    {
      title: '关联项目',
      dataIndex: 'matterName',
      key: 'matterName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      mobileShow: true,
    },
    {
      title: '操作',
      key: 'action',
      width: isMobile.value ? 100 : 150,
      fixed: 'right' as const,
      mobileShow: true,
    },
  ];

  // 移动端隐藏部分列
  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
});

// 加载日程列表
async function loadSchedules() {
  loading.value = true;
  try {
    const res = await getSchedules({
      startTime: queryParams.value.startTime,
      endTime: queryParams.value.endTime,
      scheduleType: queryParams.value.scheduleType,
      pageNum: 1,
      pageSize: 200,
    });
    schedules.value = res || [];
  } catch (error: any) {
    message.error(error.message || '加载日程失败');
  } finally {
    loading.value = false;
  }
}

// 加载今日日程
async function loadTodaySchedules() {
  try {
    const res = await getMyTodaySchedules();
    todaySchedules.value = res || [];
  } catch (error) {
    console.error('加载今日日程失败', error);
  }
}

// 加载休息日数据
async function loadOffDays() {
  try {
    const { start, end } = getCalendarRange(currentMonth.value);
    const res = await getOffDays(
      start.format('YYYY-MM-DD'),
      end.format('YYYY-MM-DD'),
    );
    offDays.value = new Set(res.offDays || []);
  } catch (error) {
    console.error('加载休息日数据失败', error);
  }
}

// 判断是否为休息日
function isOffDay(day: dayjs.Dayjs): boolean {
  return offDays.value.has(day.format('YYYY-MM-DD'));
}

// 加载项目列表
async function loadMatters() {
  try {
    const res = await getMatterSelectOptions({
      pageNum: 1,
      pageSize: 500,
      status: 'ACTIVE',
    });
    matters.value = (res.list || []).map((m: any) => ({
      id: m.id,
      name: m.name,
      matterNo: m.matterNo || '',
    }));
  } catch (error) {
    console.error('加载项目列表失败', error);
  }
}

// 获取某天的日程（按开始时间排序）
function getSchedulesForDay(day: dayjs.Dayjs) {
  return schedules.value
    .filter((s) => {
      const start = dayjs(s.startTime);
      return start.isSame(day, 'day');
    })
    .sort((a, b) => {
      // 全天日程排在最前面
      if (a.allDay && !b.allDay) return -1;
      if (!a.allDay && b.allDay) return 1;
      // 按开始时间排序
      return dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf();
    });
}

// 日程类型颜色
function getTypeColor(type: string) {
  const opt = SCHEDULE_TYPE_OPTIONS.find((o) => o.value === type);
  return opt?.color || '#722ed1';
}

// 状态标签
function getStatusTag(status: string) {
  const map: Record<string, { color: string; text: string }> = {
    ACTIVE: { color: 'processing', text: '进行中' },
    COMPLETED: { color: 'success', text: '已完成' },
    CANCELLED: { color: 'default', text: '已取消' },
  };
  return map[status] || { color: 'default', text: status };
}

// 切换月份
function changeMonth(delta: number) {
  currentMonth.value = currentMonth.value.add(delta, 'month');
  queryParams.value.startTime = currentMonth.value
    .startOf('month')
    .format('YYYY-MM-DDTHH:mm:ss');
  queryParams.value.endTime = currentMonth.value
    .endOf('month')
    .format('YYYY-MM-DDTHH:mm:ss');
  loadSchedules();
  loadOffDays();
}

// 回到今天
function goToToday() {
  currentMonth.value = dayjs();
  queryParams.value.startTime = currentMonth.value
    .startOf('month')
    .format('YYYY-MM-DDTHH:mm:ss');
  queryParams.value.endTime = currentMonth.value
    .endOf('month')
    .format('YYYY-MM-DDTHH:mm:ss');
  loadSchedules();
}

// 打开新建弹窗
function handleAdd(day?: dayjs.Dayjs) {
  editingId.value = null;
  modalTitle.value = '新建日程';
  formData.value = {
    matterId: undefined,
    title: '',
    description: '',
    location: '',
    scheduleType: 'MEETING',
    allDay: false,
    reminderMinutes: 30,
    date: day || dayjs(),
    time: [dayjs().hour(9).minute(0), dayjs().hour(10).minute(0)] as [
      dayjs.Dayjs,
      dayjs.Dayjs,
    ],
  };
  loadMatters();
  modalVisible.value = true;
}

// 打开编辑弹窗
function handleEdit(record: ScheduleDTO) {
  editingId.value = record.id;
  modalTitle.value = '编辑日程';
  const start = dayjs(record.startTime);
  const end = record.endTime ? dayjs(record.endTime) : start.add(1, 'hour');
  formData.value = {
    matterId: record.matterId,
    title: record.title,
    description: record.description,
    location: record.location,
    scheduleType: record.scheduleType,
    allDay: record.allDay,
    reminderMinutes: record.reminderMinutes,
    date: start,
    time: [start, end] as [dayjs.Dayjs, dayjs.Dayjs],
  };
  loadMatters();
  modalVisible.value = true;
}

// 提交表单
async function handleSubmit() {
  if (!formData.value.title) {
    message.error('请输入日程标题');
    return;
  }
  if (!formData.value.date) {
    message.error('请选择日期');
    return;
  }

  try {
    const date = dayjs(formData.value.date);
    let startTime: string;
    let endTime: string | undefined;

    if (formData.value.allDay) {
      startTime = date.startOf('day').format('YYYY-MM-DDTHH:mm:ss');
      endTime = date.endOf('day').format('YYYY-MM-DDTHH:mm:ss');
    } else if (formData.value.time && formData.value.time.length >= 2) {
      const startHour = dayjs(formData.value.time[0]);
      const endHour = dayjs(formData.value.time[1]);
      startTime = date
        .hour(startHour.hour())
        .minute(startHour.minute())
        .format('YYYY-MM-DDTHH:mm:ss');
      endTime = date
        .hour(endHour.hour())
        .minute(endHour.minute())
        .format('YYYY-MM-DDTHH:mm:ss');
    } else {
      startTime = date.hour(9).minute(0).format('YYYY-MM-DDTHH:mm:ss');
      endTime = date.hour(10).minute(0).format('YYYY-MM-DDTHH:mm:ss');
    }

    if (editingId.value) {
      // 更新
      await updateSchedule(editingId.value, {
        title: formData.value.title,
        description: formData.value.description,
        location: formData.value.location,
        startTime,
        endTime,
        reminderMinutes: formData.value.reminderMinutes,
      });
      message.success('日程更新成功');
    } else {
      // 创建
      const data: CreateScheduleCommand = {
        matterId: formData.value.matterId,
        title: formData.value.title!,
        description: formData.value.description,
        location: formData.value.location,
        scheduleType: formData.value.scheduleType!,
        startTime,
        endTime,
        allDay: formData.value.allDay,
        reminderMinutes: formData.value.reminderMinutes,
      };
      await createSchedule(data);
      message.success('日程创建成功');
    }

    modalVisible.value = false;
    loadSchedules();
    loadTodaySchedules();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 取消日程
async function handleCancel(id: number) {
  try {
    await cancelSchedule(id);
    message.success('日程已取消');
    loadSchedules();
    loadTodaySchedules();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 删除日程
async function handleDelete(id: number) {
  try {
    await deleteSchedule(id);
    message.success('日程已删除');
    loadSchedules();
    loadTodaySchedules();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 格式化时间
function formatTime(time: string) {
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

onMounted(() => {
  loadSchedules();
  loadTodaySchedules();
  loadOffDays();
});
</script>

<template>
  <Page title="日程管理" description="管理您的日程安排，包括开庭、会议、约见等">
    <div class="schedule-page">
      <!-- 今日日程提醒 -->
      <Card
        v-if="todaySchedules.length > 0"
        title="今日日程"
        size="small"
        style="margin-bottom: 16px"
      >
        <div class="today-schedules">
          <div v-for="s in todaySchedules" :key="s.id" class="today-item">
            <Tag
              :color="getTypeColor(s.scheduleType)"
              style="margin-right: 8px"
            >
              {{ s.scheduleTypeName || s.scheduleType }}
            </Tag>
            <span class="today-time">{{
              dayjs(s.startTime).format('HH:mm')
            }}</span>
            <span class="today-title">{{ s.title }}</span>
            <span v-if="s.location" class="today-location"
              >📍 {{ s.location }}</span
            >
          </div>
        </div>
      </Card>

      <Card>
        <Tabs v-model:active-key="activeTab">
          <TabPane key="calendar">
            <template #tab>
              <span>📅 日历视图</span>
            </template>
          </TabPane>
          <TabPane key="list">
            <template #tab>
              <span>📋 列表视图</span>
            </template>
          </TabPane>
        </Tabs>

        <!-- 日历视图 -->
        <div v-if="activeTab === 'calendar'" class="calendar-view">
          <div class="calendar-header">
            <Space>
              <Button @click="changeMonth(-1)">上月</Button>
              <Button @click="goToToday">今天</Button>
              <Button @click="changeMonth(1)">下月</Button>
            </Space>
            <span class="current-month">{{
              currentMonth.format('YYYY年MM月')
            }}</span>
            <Button type="primary" @click="handleAdd()">新建日程</Button>
          </div>

          <div class="calendar-grid">
            <div class="weekday-header">
              <div
                v-for="day in ['日', '一', '二', '三', '四', '五', '六']"
                :key="day"
                class="weekday"
              >
                {{ day }}
              </div>
            </div>
            <div class="days-grid">
              <div
                v-for="day in calendarDays"
                :key="day.format('YYYY-MM-DD')"
                class="day-cell"
                :class="{
                  'other-month': !day.isSame(currentMonth, 'month'),
                  today: day.isSame(dayjs(), 'day'),
                  'off-day': isOffDay(day),
                }"
                @click="handleAdd(day)"
              >
                <div class="day-number">
                  {{ day.date() }}
                  <span v-if="isOffDay(day)" class="off-day-badge">休</span>
                </div>
                <div class="day-schedules">
                  <div
                    v-for="s in getSchedulesForDay(day).slice(0, 3)"
                    :key="s.id"
                    class="schedule-item"
                    :style="{ backgroundColor: getTypeColor(s.scheduleType) }"
                    @click.stop="handleEdit(s)"
                  >
                    <span v-if="!s.allDay" class="schedule-time">{{
                      dayjs(s.startTime).format('HH:mm')
                    }}</span>
                    {{ s.title }}
                  </div>
                  <div
                    v-if="getSchedulesForDay(day).length > 3"
                    class="more-schedules"
                  >
                    +{{ getSchedulesForDay(day).length - 3 }} 更多
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 列表视图 -->
        <div v-if="activeTab === 'list'" class="list-view">
          <div style="margin-bottom: 16px">
            <Space>
              <Select
                v-model:value="queryParams.scheduleType"
                placeholder="日程类型"
                style="width: 120px"
                allow-clear
                :options="SCHEDULE_TYPE_OPTIONS"
                @change="loadSchedules"
              />
              <Button type="primary" @click="handleAdd()">新建日程</Button>
            </Space>
          </div>

          <Table
            :columns="columns"
            :data-source="schedules"
            :loading="loading"
            :pagination="{ showSizeChanger: true, showQuickJumper: true }"
            :scroll="{ x: 1200 }"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'scheduleTypeName'">
                <Tag :color="getTypeColor(record.scheduleType)">
                  {{ record.scheduleTypeName || record.scheduleType }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'startTime'">
                {{ formatTime(record.startTime) }}
              </template>
              <template v-else-if="column.key === 'endTime'">
                {{ record.endTime ? formatTime(record.endTime) : '-' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="getStatusTag(record.status).color">
                  {{ getStatusTag(record.status).text }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button
                    type="link"
                    size="small"
                    @click="handleEdit(record as ScheduleDTO)"
                  >
                    编辑
                  </Button>
                  <Popconfirm
                    v-if="record.status === 'ACTIVE'"
                    title="确定要取消这个日程吗？"
                    @confirm="handleCancel(record.id)"
                  >
                    <Button type="link" size="small">取消</Button>
                  </Popconfirm>
                  <Popconfirm
                    title="确定要删除这个日程吗？"
                    @confirm="handleDelete(record.id)"
                  >
                    <Button type="link" size="small" danger>删除</Button>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </div>
      </Card>
    </div>

    <!-- 新建/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="isMobile ? '100%' : '550px'"
      :centered="isMobile"
      @ok="handleSubmit"
    >
      <Form
        :label-col="isMobile ? { span: 24 } : { span: 5 }"
        :wrapper-col="isMobile ? { span: 24 } : { span: 18 }"
        :layout="isMobile ? 'vertical' : 'horizontal'"
      >
        <FormItem label="日程标题" required>
          <Input v-model:value="formData.title" placeholder="如：XX案开庭" />
        </FormItem>
        <FormItem label="日程类型" required>
          <Select
            v-model:value="formData.scheduleType"
            :options="SCHEDULE_TYPE_OPTIONS"
          />
        </FormItem>
        <FormItem label="日期" required>
          <DatePicker
            v-model:value="formData.date"
            style="width: 100%"
            format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="全天">
          <Checkbox v-model:checked="formData.allDay">全天日程</Checkbox>
        </FormItem>
        <FormItem v-show="formData.allDay !== true" label="时间">
          <TimeRangePicker
            v-model:value="formData.time"
            format="HH:mm"
            style="width: 100%"
            :placeholder="['开始时间', '结束时间']"
          />
        </FormItem>
        <FormItem label="地点">
          <Input
            v-model:value="formData.location"
            placeholder="如：XX法院第3法庭"
          />
        </FormItem>
        <FormItem label="关联项目">
          <Select
            v-model:value="formData.matterId"
            placeholder="选择关联项目（可选）"
            allow-clear
            show-search
            :filter-option="
              (input: string, option: any) =>
                option.label.toLowerCase().includes(input.toLowerCase())
            "
            :options="
              matters.map((m) => ({
                label: `[${m.matterNo}] ${m.name}`,
                value: m.id,
              }))
            "
          />
        </FormItem>
        <FormItem label="提前提醒">
          <Select
            v-model:value="formData.reminderMinutes"
            :options="REMINDER_OPTIONS"
          />
        </FormItem>
        <FormItem label="备注">
          <Textarea
            v-model:value="formData.description"
            :rows="2"
            placeholder="备注信息"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped lang="less">
.schedule-page {
  .today-schedules {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .today-item {
      display: flex;
      align-items: center;
      padding: 8px 12px;
      background: #f6ffed;
      border-radius: 4px;
      font-size: 13px;

      .today-time {
        font-weight: 500;
        margin-right: 8px;
        color: #1890ff;
      }

      .today-title {
        margin-right: 12px;
      }

      .today-location {
        color: #8c8c8c;
        font-size: 12px;
      }
    }
  }

  .calendar-view {
    .calendar-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .current-month {
        font-size: 18px;
        font-weight: 600;
      }
    }

    .calendar-grid {
      border: 1px solid #e8e8e8;
      border-radius: 4px;

      .weekday-header {
        display: grid;
        grid-template-columns: repeat(7, 1fr);
        background: #fafafa;
        border-bottom: 1px solid #e8e8e8;

        .weekday {
          padding: 12px;
          text-align: center;
          font-weight: 500;
          color: #595959;
        }
      }

      .days-grid {
        display: grid;
        grid-template-columns: repeat(7, 1fr);

        .day-cell {
          min-height: 100px;
          padding: 8px;
          border-right: 1px solid #f0f0f0;
          border-bottom: 1px solid #f0f0f0;
          cursor: pointer;
          transition: background 0.2s;

          &:nth-child(7n) {
            border-right: none;
          }

          &:hover {
            background: #f5f5f5;
          }

          &.other-month {
            background: #fafafa;
            .day-number {
              color: #bfbfbf;
            }
          }

          &.today {
            background: #e6f7ff;
            .day-number {
              background: #1890ff;
              color: #fff;
              border-radius: 50%;
              width: 24px;
              height: 24px;
              display: flex;
              align-items: center;
              justify-content: center;
            }
          }

          &.off-day {
            background: #fff7e6;

            &:hover {
              background: #fff1e0;
            }
          }

          .day-number {
            font-size: 14px;
            margin-bottom: 4px;
            display: flex;
            align-items: center;
            gap: 4px;

            .off-day-badge {
              font-size: 10px;
              color: #fa8c16;
              background: #fff7e6;
              border: 1px solid #ffd591;
              border-radius: 2px;
              padding: 0 3px;
              line-height: 1.4;
            }
          }

          .day-schedules {
            .schedule-item {
              font-size: 11px;
              color: #fff;
              padding: 2px 4px;
              border-radius: 2px;
              margin-bottom: 2px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
              cursor: pointer;

              &:hover {
                opacity: 0.8;
              }

              .schedule-time {
                font-weight: 600;
                margin-right: 4px;
                opacity: 0.9;
              }
            }

            .more-schedules {
              font-size: 11px;
              color: #8c8c8c;
              padding: 2px 4px;
            }
          }
        }
      }
    }
  }

  .list-view {
    margin-top: 16px;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .schedule-page {
    .today-schedules {
      flex-direction: column;
      gap: 8px;

      .today-item {
        flex-wrap: wrap;
        padding: 8px;

        .today-title {
          width: 100%;
          margin: 4px 0;
        }

        .today-location {
          width: 100%;
        }
      }
    }

    .calendar-view {
      .calendar-header {
        flex-direction: column;
        gap: 12px;
        align-items: stretch;

        .current-month {
          text-align: center;
        }
      }

      .calendar-grid {
        .weekday-header .weekday {
          padding: 8px 2px;
          font-size: 12px;
        }

        .days-grid .day-cell {
          min-height: 60px;
          padding: 4px;

          .day-number {
            font-size: 12px;
          }

          .day-schedules .schedule-item {
            font-size: 10px;
            padding: 1px 2px;

            .schedule-time {
              display: none;
            }
          }

          .day-schedules .more-schedules {
            font-size: 10px;
          }
        }
      }
    }

    .list-view {
      :deep(.ant-table) {
        font-size: 13px;
      }

      :deep(.ant-btn-link) {
        padding: 0 4px;
        font-size: 12px;
      }
    }
  }
}
</style>
