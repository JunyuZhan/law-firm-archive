<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Form,
  FormItem,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  approveRegularization,
  createRegularization,
  deleteRegularization,
  getRegularizationDetail,
  getRegularizationList,
} from '#/api/hr/regularization';
import type {
  ApproveRegularizationCommand,
  CreateRegularizationCommand,
  RegularizationDTO,
  RegularizationQuery,
} from '#/api/hr/regularization';
import { getEmployeeList } from '#/api/hr/employee';
import type { EmployeeDTO } from '#/api/hr/employee';

defineOptions({ name: 'RegularizationManagement' });


// 搜索表单
const searchForm = reactive<RegularizationQuery>({
  status: undefined,
  employeeId: undefined,
  keyword: undefined,
});

// 状态选项
const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
];

// 状态标签颜色
const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
};

// 表格列
const columns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo', width: 120 },
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  { title: '试用期开始', dataIndex: 'probationStartDate', key: 'probationStartDate', width: 120 },
  { title: '试用期结束', dataIndex: 'probationEndDate', key: 'probationEndDate', width: 120 },
  { title: '期望转正日期', dataIndex: 'expectedRegularDate', key: 'expectedRegularDate', width: 120 },
  { title: '申请日期', dataIndex: 'applicationDate', key: 'applicationDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '审批人', dataIndex: 'approverName', key: 'approverName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<RegularizationDTO[]>([]);
const loading = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

// 员工列表
const employeeList = ref<EmployeeDTO[]>([]);

// 新增弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const regularizationForm = reactive<CreateRegularizationCommand>({
  employeeId: undefined as number | undefined,
  probationStartDate: '',
  probationEndDate: '',
  expectedRegularDate: '',
  selfEvaluation: '',
});

// 审批弹窗
const approveModalVisible = ref(false);
const approveModalLoading = ref(false);
const currentRecord = ref<RegularizationDTO | null>(null);
const approveForm = reactive<ApproveRegularizationCommand>({
  approved: true,
  comment: '',
});

// 获取转正列表
async function fetchData() {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    };
    const res = await getRegularizationList(params);
    tableData.value = res.list || [];
    pagination.total = res.total || 0;
  } catch (error) {
    console.error('获取转正列表失败:', error);
    message.error('获取转正列表失败');
  } finally {
    loading.value = false;
  }
}

// 获取员工列表
async function fetchEmployees() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 1000 });
    employeeList.value = res.list || [];
  } catch (error) {
    console.error('获取员工列表失败:', error);
    employeeList.value = [];
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1;
  fetchData();
}

// 重置
function handleReset() {
  Object.assign(searchForm, {
    status: undefined,
    employeeId: undefined,
    keyword: undefined,
  });
  pagination.current = 1;
  fetchData();
}

// 表格变化
function handleTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchData();
}

// 新增转正申请
function handleAdd() {
  Object.assign(regularizationForm, {
    employeeId: undefined,
    probationStartDate: '',
    probationEndDate: '',
    expectedRegularDate: '',
    selfEvaluation: '',
  });
  modalVisible.value = true;
}

// 提交转正申请
async function handleSubmit() {
  if (!regularizationForm.employeeId) {
    message.warning('请选择员工');
    return;
  }

  modalLoading.value = true;
  try {
    await createRegularization({
      ...regularizationForm,
      employeeId: regularizationForm.employeeId,
    });
    message.success('提交成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '提交失败');
  } finally {
    modalLoading.value = false;
  }
}

// 审批转正申请
function handleApprove(record: RegularizationDTO) {
  currentRecord.value = record;
  approveForm.approved = true;
  approveForm.comment = '';
  approveModalVisible.value = true;
}

// 拒绝转正申请
function handleReject(record: RegularizationDTO) {
  currentRecord.value = record;
  approveForm.approved = false;
  approveForm.comment = '';
  approveModalVisible.value = true;
}

// 提交审批
async function handleApproveSubmit() {
  if (!currentRecord.value) return;
  if (!approveForm.comment) {
    message.warning('请填写审批意见');
    return;
  }

  approveModalLoading.value = true;
  try {
    await approveRegularization(currentRecord.value.id, approveForm);
    message.success('审批成功');
    approveModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '审批失败');
  } finally {
    approveModalLoading.value = false;
  }
}

// 删除转正申请
function handleDelete(record: RegularizationDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除转正申请"${record.applicationNo || record.id}"吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteRegularization(record.id);
        message.success('删除成功');
        fetchData();
      } catch (error: any) {
        message.error(error?.message || '删除失败');
      }
    },
  });
}

// 查看详情
const detailModalVisible = ref(false);
const detailData = ref<RegularizationDTO | null>(null);

async function handleView(record: RegularizationDTO) {
  try {
    const detail = await getRegularizationDetail(record.id);
    detailData.value = detail;
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error?.message || '获取详情失败');
  }
}


onMounted(() => {
  fetchEmployees();
  fetchData();
});
</script>

<template>
  <Page title="转正管理" description="员工转正申请与审批">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Form layout="inline" :model="searchForm" @finish="handleSearch">
          <FormItem label="员工">
            <Select
              v-model:value="searchForm.employeeId"
              placeholder="请选择员工"
              allow-clear
              show-search
              :filter-option="(input: string, option: any) => option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0"
              style="width: 150px"
            >
              <Select.Option v-for="emp in employeeList" :key="emp.id" :value="emp.id">
                {{ emp.realName || emp.name || '-' }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem label="状态">
            <Select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
              <Select.Option v-for="item in statusOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem>
            <Space>
              <Button type="primary" html-type="submit">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新建转正申请</Button>
            </Space>
          </FormItem>
        </Form>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
            <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <template v-if="record.status === 'PENDING'">
                <a @click="handleApprove(record)">通过</a>
                <a style="color: #ff4d4f" @click="handleReject(record)">拒绝</a>
                <a style="color: #ff4d4f" @click="handleDelete(record)">删除</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增转正申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建转正申请"
      :confirm-loading="modalLoading"
      width="600px"
      @ok="handleSubmit"
    >
      <Form :model="regularizationForm" layout="vertical">
        <FormItem label="员工" required>
          <Select
            v-model:value="regularizationForm.employeeId"
            placeholder="请选择员工"
            show-search
            :filter-option="(input: string, option: any) => option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0"
          >
            <Select.Option v-for="emp in employeeList" :key="emp.id" :value="emp.id">
              {{ emp.name }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="试用期开始日期">
          <DatePicker
            v-model:value="regularizationForm.probationStartDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="试用期结束日期">
          <DatePicker
            v-model:value="regularizationForm.probationEndDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="期望转正日期">
          <DatePicker
            v-model:value="regularizationForm.expectedRegularDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="自我评价">
          <Textarea v-model:value="regularizationForm.selfEvaluation" placeholder="请输入自我评价" :rows="4" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveModalVisible"
      :title="approveForm.approved ? '审批通过' : '审批拒绝'"
      :confirm-loading="approveModalLoading"
      @ok="handleApproveSubmit"
    >
      <Form :model="approveForm" layout="vertical">
        <FormItem label="审批意见" required>
          <Textarea v-model:value="approveForm.comment" placeholder="请输入审批意见" :rows="4" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal v-model:open="detailModalVisible" title="转正申请详情" width="600px" :footer="null">
      <div v-if="detailData" style="line-height: 2">
        <p><strong>申请编号:</strong> {{ detailData.applicationNo || '-' }}</p>
        <p><strong>员工姓名:</strong> {{ detailData.employeeName || '-' }}</p>
        <p><strong>试用期开始:</strong> {{ detailData.probationStartDate || '-' }}</p>
        <p><strong>试用期结束:</strong> {{ detailData.probationEndDate || '-' }}</p>
        <p><strong>期望转正日期:</strong> {{ detailData.expectedRegularDate || '-' }}</p>
        <p><strong>自我评价:</strong> {{ detailData.selfEvaluation || '-' }}</p>
        <p><strong>状态:</strong> {{ detailData.statusName || '-' }}</p>
        <p v-if="detailData.comment"><strong>审批意见:</strong> {{ detailData.comment }}</p>
      </div>
    </Modal>
  </Page>
</template>

