<script setup lang="ts">
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
  Space,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  getCurrentGoOut,
  getGoOutRecordsByDateRange,
  getMyGoOutRecords,
  registerGoOut,
  registerReturn,
} from '#/api/admin/go-out';
import type { GoOutCommand, GoOutRecordDTO } from '#/api/admin/go-out';

defineOptions({ name: 'GoOutManagement' });

const { RangePicker } = DatePicker;

// 搜索表单
const searchForm = reactive({
  startDate: '',
  endDate: '',
});

// 状态标签颜色
const statusColorMap: Record<string, string> = {
  OUT: 'blue',
  RETURNED: 'green',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  OUT: '外出中',
  RETURNED: '已返回',
};

// 表格列
const columns = [
  { title: '记录编号', dataIndex: 'recordNo', key: 'recordNo', width: 120 },
  { title: '姓名', dataIndex: 'userName', key: 'userName', width: 100 },
  { title: '外出时间', dataIndex: 'outTime', key: 'outTime', width: 160 },
  { title: '预计返回时间', dataIndex: 'expectedReturnTime', key: 'expectedReturnTime', width: 160 },
  { title: '实际返回时间', dataIndex: 'actualReturnTime', key: 'actualReturnTime', width: 160 },
  { title: '外出地点', dataIndex: 'location', key: 'location', width: 150 },
  { title: '外出事由', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<GoOutRecordDTO[]>([]);
const loading = ref(false);

// 新增弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const goOutForm = reactive<GoOutCommand>({
  outTime: '',
  expectedReturnTime: '',
  location: '',
  reason: '',
  companions: '',
});

// 获取外出记录
async function fetchData() {
  loading.value = true;
  try {
    let data: GoOutRecordDTO[];
    if (searchForm.startDate && searchForm.endDate) {
      data = await getGoOutRecordsByDateRange(searchForm.startDate, searchForm.endDate);
    } else {
      data = await getMyGoOutRecords();
    }
    tableData.value = data || [];
  } catch (error) {
    console.error('获取外出记录失败:', error);
    message.error('获取外出记录失败');
  } finally {
    loading.value = false;
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

// 外出登记
function handleAdd() {
  Object.assign(goOutForm, {
    outTime: '',
    expectedReturnTime: '',
    location: '',
    reason: '',
    companions: '',
  });
  modalVisible.value = true;
}

// 提交外出登记
async function handleSubmit() {
  if (!goOutForm.outTime) {
    message.warning('请选择外出时间');
    return;
  }
  if (!goOutForm.reason) {
    message.warning('请填写外出事由');
    return;
  }

  modalLoading.value = true;
  try {
    await registerGoOut(goOutForm);
    message.success('登记成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '登记失败');
  } finally {
    modalLoading.value = false;
  }
}

// 登记返回
async function handleReturn(record: GoOutRecordDTO) {
  Modal.confirm({
    title: '确认返回',
    content: `确定要登记返回吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await registerReturn(record.id);
        message.success('返回登记成功');
        fetchData();
      } catch (error: any) {
        message.error(error?.message || '操作失败');
      }
    },
  });
}

// 查看当前外出
async function handleViewCurrent() {
  try {
    const data = await getCurrentGoOut();
    tableData.value = data || [];
  } catch (error: any) {
    message.error(error?.message || '获取失败');
  }
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="外出管理" description="外出登记与返回">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Form layout="inline" :model="searchForm" @finish="handleSearch">
          <FormItem label="日期范围">
            <RangePicker style="width: 240px" @change="handleDateRangeChange" />
          </FormItem>
          <FormItem>
            <Space>
              <Button type="primary" html-type="submit">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button @click="handleViewCurrent">当前外出</Button>
              <Button type="primary" @click="handleAdd">外出登记</Button>
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
          <template v-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a v-if="record.status === 'OUT'" @click="handleReturn(record)">登记返回</a>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 外出登记弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="外出登记"
      :confirm-loading="modalLoading"
      width="600px"
      @ok="handleSubmit"
    >
      <Form :model="goOutForm" layout="vertical">
        <FormItem label="外出时间" required>
          <DatePicker
            v-model:value="goOutForm.outTime"
            show-time
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </FormItem>
        <FormItem label="预计返回时间">
          <DatePicker
            v-model:value="goOutForm.expectedReturnTime"
            show-time
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </FormItem>
        <FormItem label="外出地点">
          <Input v-model:value="goOutForm.location" placeholder="请输入外出地点" />
        </FormItem>
        <FormItem label="外出事由" required>
          <Textarea v-model:value="goOutForm.reason" placeholder="请输入外出事由" :rows="4" />
        </FormItem>
        <FormItem label="同行人员">
          <Input v-model:value="goOutForm.companions" placeholder="请输入同行人员" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

