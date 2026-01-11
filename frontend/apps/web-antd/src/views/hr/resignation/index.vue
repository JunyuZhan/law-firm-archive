<script setup lang="ts">
import type { EmployeeDTO } from '#/api/hr/employee';
import type {
  ApproveResignationCommand,
  CreateResignationCommand,
  ResignationDTO,
  ResignationQuery,
} from '#/api/hr/resignation';

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

import { getEmployeeList } from '#/api/hr/employee';
import {
  approveResignation,
  completeResignationHandover,
  createResignation,
  deleteResignation,
  getResignationDetail,
  getResignationList,
} from '#/api/hr/resignation';

defineOptions({ name: 'ResignationManagement' });

// 搜索表单
const searchForm = reactive<ResignationQuery>({
  status: undefined,
  employeeId: undefined,
  keyword: undefined,
});

// 状态选项
const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已交接', value: 'HANDOVER_COMPLETED' },
];

// 离职类型选项
const resignationTypeOptions = [
  { label: '主动离职', value: 'VOLUNTARY' },
  { label: '被动离职', value: 'INVOLUNTARY' },
  { label: '合同到期', value: 'CONTRACT_EXPIRED' },
  { label: '退休', value: 'RETIREMENT' },
];

// 状态标签颜色
const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'blue',
  REJECTED: 'red',
  HANDOVER_COMPLETED: 'green',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  HANDOVER_COMPLETED: '已交接',
};

// 交接状态颜色
const handoverStatusColorMap: Record<string, string> = {
  PENDING: 'orange',
  IN_PROGRESS: 'blue',
  COMPLETED: 'green',
};

// 表格列
const columns = [
  {
    title: '申请编号',
    dataIndex: 'applicationNo',
    key: 'applicationNo',
    width: 120,
  },
  {
    title: '员工姓名',
    dataIndex: 'employeeName',
    key: 'employeeName',
    width: 100,
  },
  {
    title: '离职类型',
    dataIndex: 'resignationTypeName',
    key: 'resignationType',
    width: 100,
  },
  {
    title: '离职日期',
    dataIndex: 'resignationDate',
    key: 'resignationDate',
    width: 120,
  },
  {
    title: '最后工作日',
    dataIndex: 'lastWorkDate',
    key: 'lastWorkDate',
    width: 120,
  },
  {
    title: '交接人',
    dataIndex: 'handoverPersonName',
    key: 'handoverPersonName',
    width: 100,
  },
  {
    title: '交接状态',
    dataIndex: 'handoverStatusName',
    key: 'handoverStatus',
    width: 100,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  {
    title: '审批人',
    dataIndex: 'approverName',
    key: 'approverName',
    width: 100,
  },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<ResignationDTO[]>([]);
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
const resignationForm = reactive<CreateResignationCommand>({
  employeeId: undefined as unknown as number,
  resignationType: '',
  resignationDate: '',
  lastWorkDate: '',
  reason: '',
  handoverPersonId: undefined,
  handoverNote: '',
});

// 审批弹窗
const approveModalVisible = ref(false);
const approveModalLoading = ref(false);
const currentRecord = ref<null | ResignationDTO>(null);
const approveForm = reactive<ApproveResignationCommand>({
  approved: true,
  comment: '',
});

// 交接弹窗
const handoverModalVisible = ref(false);
const handoverModalLoading = ref(false);
const handoverNote = ref('');

// 获取离职列表
async function fetchData() {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    };
    const res = await getResignationList(params);
    tableData.value = res.list || [];
    pagination.total = res.total || 0;
  } catch (error) {
    console.error('获取离职列表失败:', error);
    message.error('获取离职列表失败');
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

// 新增离职申请
function handleAdd() {
  Object.assign(resignationForm, {
    employeeId: undefined,
    resignationType: '',
    resignationDate: '',
    lastWorkDate: '',
    reason: '',
    handoverPersonId: undefined,
    handoverNote: '',
  });
  modalVisible.value = true;
}

// 提交离职申请
async function handleSubmit() {
  if (!resignationForm.employeeId) {
    message.warning('请选择员工');
    return;
  }
  if (!resignationForm.resignationType) {
    message.warning('请选择离职类型');
    return;
  }
  if (!resignationForm.resignationDate) {
    message.warning('请选择离职日期');
    return;
  }
  if (!resignationForm.lastWorkDate) {
    message.warning('请选择最后工作日');
    return;
  }

  modalLoading.value = true;
  try {
    await createResignation({
      ...resignationForm,
      employeeId: resignationForm.employeeId,
      resignationType: resignationForm.resignationType,
      resignationDate: resignationForm.resignationDate,
      lastWorkDate: resignationForm.lastWorkDate,
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

// 审批离职申请
function handleApprove(record: Record<string, any>) {
  currentRecord.value = record as ResignationDTO;
  approveForm.approved = true;
  approveForm.comment = '';
  approveModalVisible.value = true;
}

// 拒绝离职申请
function handleReject(record: Record<string, any>) {
  currentRecord.value = record as ResignationDTO;
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
    await approveResignation(currentRecord.value.id, approveForm);
    message.success('审批成功');
    approveModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '审批失败');
  } finally {
    approveModalLoading.value = false;
  }
}

// 完成交接
function handleCompleteHandover(record: Record<string, any>) {
  currentRecord.value = record as ResignationDTO;
  handoverNote.value = record.handoverNote || '';
  handoverModalVisible.value = true;
}

// 提交交接
async function handleHandoverSubmit() {
  if (!currentRecord.value) return;

  handoverModalLoading.value = true;
  try {
    await completeResignationHandover(
      currentRecord.value.id,
      handoverNote.value,
    );
    message.success('交接完成');
    handoverModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '操作失败');
  } finally {
    handoverModalLoading.value = false;
  }
}

// 删除离职申请
function handleDelete(record: Record<string, any>) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除离职申请"${record.applicationNo || record.id}"吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteResignation(record.id);
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
const detailData = ref<null | ResignationDTO>(null);

async function handleView(record: Record<string, any>) {
  try {
    const detail = await getResignationDetail(record.id);
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
  <Page title="离职管理" description="员工离职申请与审批">
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
              :filter-option="
                (input: string, option: any) =>
                  option.children[0].children
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              style="width: 150px"
            >
              <Select.Option
                v-for="emp in employeeList"
                :key="emp.id"
                :value="emp.id"
              >
                {{ emp.realName || '-' }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem label="状态">
            <Select
              v-model:value="searchForm.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <Select.Option
                v-for="item in statusOptions"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </Select.Option>
            </Select>
          </FormItem>
          <FormItem>
            <Space>
              <Button type="primary" html-type="submit">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新建离职申请</Button>
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
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'handoverStatus'">
            <Tag
              v-if="record.handoverStatus"
              :color="handoverStatusColorMap[record.handoverStatus]"
            >
              {{ record.handoverStatusName || record.handoverStatus }}
            </Tag>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <template v-if="record.status === 'PENDING'">
                <a @click="handleApprove(record)">通过</a>
                <a style="color: #ff4d4f" @click="handleReject(record)">拒绝</a>
                <a style="color: #ff4d4f" @click="handleDelete(record)">删除</a>
              </template>
              <template
                v-else-if="
                  record.status === 'APPROVED' &&
                  record.handoverStatus !== 'COMPLETED'
                "
              >
                <a @click="handleCompleteHandover(record)">完成交接</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增离职申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建离职申请"
      :confirm-loading="modalLoading"
      width="600px"
      @ok="handleSubmit"
    >
      <Form :model="resignationForm" layout="vertical">
        <FormItem label="员工" required>
          <Select
            v-model:value="resignationForm.employeeId"
            placeholder="请选择员工"
            show-search
            :filter-option="
              (input: string, option: any) =>
                option.children[0].children
                  .toLowerCase()
                  .includes(input.toLowerCase())
            "
          >
            <Select.Option
              v-for="emp in employeeList"
              :key="emp.id"
              :value="emp.id"
            >
              {{ emp.realName || '-' }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="离职类型" required>
          <Select
            v-model:value="resignationForm.resignationType"
            placeholder="请选择离职类型"
          >
            <Select.Option
              v-for="item in resignationTypeOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="离职日期" required>
          <DatePicker
            v-model:value="resignationForm.resignationDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="最后工作日" required>
          <DatePicker
            v-model:value="resignationForm.lastWorkDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="离职原因">
          <Textarea
            v-model:value="resignationForm.reason"
            placeholder="请输入离职原因"
            :rows="4"
          />
        </FormItem>
        <FormItem label="交接人">
          <Select
            v-model:value="resignationForm.handoverPersonId"
            placeholder="请选择交接人"
            allow-clear
            show-search
            :filter-option="
              (input: string, option: any) =>
                option.children[0].children
                  .toLowerCase()
                  .includes(input.toLowerCase())
            "
          >
            <Select.Option
              v-for="emp in employeeList"
              :key="emp.id"
              :value="emp.id"
            >
              {{ emp.realName || '-' }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="交接备注">
          <Textarea
            v-model:value="resignationForm.handoverNote"
            placeholder="请输入交接备注"
            :rows="3"
          />
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
          <Textarea
            v-model:value="approveForm.comment"
            placeholder="请输入审批意见"
            :rows="4"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 完成交接弹窗 -->
    <Modal
      v-model:open="handoverModalVisible"
      title="完成交接"
      :confirm-loading="handoverModalLoading"
      @ok="handleHandoverSubmit"
    >
      <Form layout="vertical">
        <FormItem label="交接备注">
          <Textarea
            v-model:value="handoverNote"
            placeholder="请输入交接备注"
            :rows="4"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="离职申请详情"
      width="600px"
      :footer="null"
    >
      <div v-if="detailData" style="line-height: 2">
        <p><strong>申请编号:</strong> {{ detailData.applicationNo || '-' }}</p>
        <p><strong>员工姓名:</strong> {{ detailData.employeeName || '-' }}</p>
        <p>
          <strong>离职类型:</strong> {{ detailData.resignationTypeName || '-' }}
        </p>
        <p>
          <strong>离职日期:</strong> {{ detailData.resignationDate || '-' }}
        </p>
        <p><strong>最后工作日:</strong> {{ detailData.lastWorkDate || '-' }}</p>
        <p><strong>离职原因:</strong> {{ detailData.reason || '-' }}</p>
        <p>
          <strong>交接人:</strong> {{ detailData.handoverPersonName || '-' }}
        </p>
        <p>
          <strong>交接状态:</strong> {{ detailData.handoverStatusName || '-' }}
        </p>
        <p><strong>状态:</strong> {{ detailData.statusName || '-' }}</p>
        <p v-if="detailData.comment">
          <strong>审批意见:</strong> {{ detailData.comment }}
        </p>
        <p v-if="detailData.handoverNote">
          <strong>交接备注:</strong> {{ detailData.handoverNote }}
        </p>
      </div>
    </Modal>
  </Page>
</template>
