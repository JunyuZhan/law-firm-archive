<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Page } from '@vben/common-ui';
import dayjs from 'dayjs';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  DatePicker,
  InputNumber,
  Textarea,
  Row,
  Col,
  Modal,
} from 'ant-design-vue';
import {
  getContractList,
} from '#/api/finance';
import { getClientSelectOptions } from '#/api/client';
import { getMatterSelectOptions } from '#/api/matter';
import { getUserSelectOptions, getDepartmentTreePublic } from '#/api/system';
import type { ContractDTO, ContractQuery } from '#/api/finance/types';
import type { ClientDTO } from '#/api/client/types';
import type { MatterDTO } from '#/api/matter/types';
import type { UserDTO, DepartmentDTO } from '#/api/system/types';

defineOptions({ name: 'FinanceContract' });

// 状态
const loading = ref(false);
const dataSource = ref<ContractDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const clients = ref<ClientDTO[]>([]);
const matters = ref<MatterDTO[]>([]);
const users = ref<UserDTO[]>([]);
const departments = ref<DepartmentDTO[]>([]);

// 查询参数
const queryParams = reactive<ContractQuery>({
  pageNum: 1,
  pageSize: 10,
});

// 表单数据（只读展示）
const formData = reactive<Partial<ContractDTO> & {
  signDate?: any;
  effectiveDate?: any;
  expiryDate?: any;
}>({
  id: undefined,
  name: '',
  clientId: undefined,
  matterId: undefined,
  contractType: 'SERVICE',
  feeType: 'FIXED',
  totalAmount: undefined,
  currency: 'CNY',
  signDate: undefined,
  effectiveDate: undefined,
  expiryDate: undefined,
  signerId: undefined as number | undefined,
  departmentId: undefined as number | undefined,
  paymentTerms: '',
  remark: '',
});

// 表格列
const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 130 },
  { title: '合同名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: '客户', dataIndex: 'clientName', key: 'clientName', width: 150 },
  { title: '项目', dataIndex: 'matterName', key: 'matterName', width: 180, ellipsis: true },
  { title: '合同类型', dataIndex: 'contractTypeName', key: 'contractTypeName', width: 100 },
  { title: '合同金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '已收金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 120 },
  { title: '签订日期', dataIndex: 'signDate', key: 'signDate', width: 120 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

// 合同类型选项
const contractTypeOptions = [
  { label: '服务合同', value: 'SERVICE' },
  { label: '顾问合同', value: 'CONSULTING' },
  { label: '代理合同', value: 'AGENCY' },
  { label: '其他', value: 'OTHER' },
];

// 收费类型选项
const feeTypeOptions = [
  { label: '固定收费', value: 'FIXED' },
  { label: '计时收费', value: 'HOURLY' },
  { label: '风险代理', value: 'CONTINGENCY' },
  { label: '混合收费', value: 'MIXED' },
];

// 货币选项
const currencyOptions = [
  { label: '人民币', value: 'CNY' },
  { label: '美元', value: 'USD' },
  { label: '欧元', value: 'EUR' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getContractList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

// 加载选项数据
async function loadOptions() {
  const [clientRes, matterRes, userRes, deptRes] = await Promise.all([
    getClientSelectOptions({ pageNum: 1, pageSize: 1000 }),
    getMatterSelectOptions({ pageNum: 1, pageSize: 1000 }),
    getUserSelectOptions({ pageNum: 1, pageSize: 1000 }),
    getDepartmentTreePublic(),
  ]);
  clients.value = clientRes.list;
  matters.value = matterRes.list;
  users.value = userRes.list;
  departments.value = deptRes;
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  Object.assign(queryParams, {
    contractNo: undefined,
    name: undefined,
    clientId: undefined,
    matterId: undefined,
    contractType: undefined,
    feeType: undefined,
    status: undefined,
    pageNum: 1,
  });
  fetchData();
}

// 查看详情（财务模块：只读）
function handleView(record: ContractDTO) {
  formData.id = record.id;
  formData.name = record.name || '';
  formData.clientId = record.clientId;
  formData.matterId = record.matterId;
  formData.contractType = record.contractType || 'SERVICE';
  formData.feeType = record.feeType || 'FIXED';
  formData.totalAmount = record.totalAmount;
  formData.currency = record.currency || 'CNY';
  formData.signDate = record.signDate ? dayjs(record.signDate) : undefined;
  formData.effectiveDate = record.effectiveDate ? dayjs(record.effectiveDate) : undefined;
  formData.expiryDate = record.expiryDate ? dayjs(record.expiryDate) : undefined;
  formData.signerId = record.signerId;
  formData.departmentId = record.departmentId;
  formData.paymentTerms = record.paymentTerms || '';
  formData.remark = record.remark || '';
  modalVisible.value = true;
}

// 分页变化
function handleTableChange(pagination: any) {
  queryParams.pageNum = pagination.current;
  queryParams.pageSize = pagination.pageSize;
  fetchData();
}

// 格式化金额
function formatMoney(value?: number) {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString()}`;
}

onMounted(() => {
  fetchData();
  loadOptions();
});
</script>

<template>
  <Page title="合同管理" description="管理项目收费合同">
    <Card class="mb-4">
      <Form :model="queryParams" @finish="handleSearch">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <FormItem label="合同编号">
              <Input v-model:value="queryParams.contractNo" placeholder="请输入" allow-clear />
            </FormItem>
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <FormItem label="合同名称">
              <Input v-model:value="queryParams.name" placeholder="请输入" allow-clear />
            </FormItem>
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <FormItem label="客户">
              <Select
                v-model:value="queryParams.clientId"
                placeholder="请选择"
                allow-clear
                show-search
                :filter-option="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
                style="width: 100%"
              >
                <Select.Option v-for="c in clients" :key="c.id" :value="c.id" :label="c.name">
                  {{ c.name }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
            <FormItem label="状态">
              <Select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 100%">
                <Select.Option value="DRAFT">草稿</Select.Option>
                <Select.Option value="PENDING">待审批</Select.Option>
                <Select.Option value="ACTIVE">生效中</Select.Option>
                <Select.Option value="TERMINATED">已终止</Select.Option>
                <Select.Option value="COMPLETED">已完成</Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :xs="24" :sm="12" :md="8" :lg="6" :xl="8">
            <FormItem>
              <Space>
                <Button type="primary" html-type="submit">查询</Button>
                <Button @click="handleReset">重置</Button>
              </Space>
            </FormItem>
          </Col>
        </Row>
      </Form>
    </Card>

    <Card>
      <!-- 财务模块：合同只读，不显示新增按钮 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: (page, size) => {
            queryParams.pageNum = page;
            queryParams.pageSize = size;
            fetchData();
          },
        }"
        :scroll="{ x: 1400 }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount'">
            {{ formatMoney((record as ContractDTO).totalAmount) }}
          </template>
          <template v-if="column.key === 'paidAmount'">
            {{ formatMoney((record as ContractDTO).paidAmount) }}
          </template>
          <template v-if="column.key === 'statusName'">
            <span>{{ (record as ContractDTO).statusName }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <!-- 财务模块：合同只读，只显示查看按钮 -->
            <Button
              type="link"
              size="small"
              @click="handleView(record as ContractDTO)"
            >
              查看
            </Button>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 查看详情弹窗（只读） -->
    <Modal
      v-model:open="modalVisible"
      title="合同详情"
      width="700px"
      :footer="null"
    >
      <Form
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <FormItem label="合同名称">
          <Input v-model:value="formData.name" :disabled="true" />
        </FormItem>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="客户">
              <Select
                v-model:value="formData.clientId"
                :disabled="true"
              >
                <Select.Option v-for="c in clients" :key="c.id" :value="c.id" :label="c.name">
                  {{ c.name }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="项目">
              <Select
                v-model:value="formData.matterId"
                :disabled="true"
                allow-clear
              >
                <Select.Option v-for="m in matters" :key="m.id" :value="m.id" :label="m.name">
                  {{ m.name }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="合同类型">
              <Select v-model:value="formData.contractType" :disabled="true">
                <Select.Option v-for="opt in contractTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="收费类型">
              <Select v-model:value="formData.feeType" :disabled="true">
                <Select.Option v-for="opt in feeTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="合同金额">
              <InputNumber
                v-model:value="formData.totalAmount"
                :disabled="true"
                style="width: 100%"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="货币">
              <Select v-model:value="formData.currency" :disabled="true">
                <Select.Option v-for="opt in currencyOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="签订日期">
              <DatePicker v-model:value="formData.signDate" :disabled="true" style="width: 100%" format="YYYY-MM-DD" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="生效日期">
              <DatePicker v-model:value="formData.effectiveDate" :disabled="true" style="width: 100%" format="YYYY-MM-DD" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="到期日期">
              <DatePicker v-model:value="formData.expiryDate" :disabled="true" style="width: 100%" format="YYYY-MM-DD" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="签订人">
              <Select
                v-model:value="formData.signerId"
                :disabled="true"
                allow-clear
              >
                <Select.Option v-for="u in users" :key="u.id" :value="u.id" :label="u.realName">
                  {{ u.realName }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <FormItem label="付款条件">
          <Textarea v-model:value="formData.paymentTerms" :disabled="true" :rows="2" />
        </FormItem>
        <FormItem label="备注">
          <Textarea v-model:value="formData.remark" :disabled="true" :rows="3" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
