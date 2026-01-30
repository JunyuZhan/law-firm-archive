<script setup lang="ts">
import type {
  CreateLaborContractCommand,
  LaborContractDTO,
  LaborContractQuery,
  UpdateLaborContractCommand,
} from '#/api/hr/contract';
import type { EmployeeDTO } from '#/api/hr/employee';

import { onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Divider,
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
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createLaborContract,
  deleteLaborContract,
  getLaborContractDetail,
  getLaborContractList,
  renewLaborContract,
  updateLaborContract,
} from '#/api/hr/contract';
import { getEmployeeList } from '#/api/hr/employee';

defineOptions({ name: 'LaborContractManagement' });

// 搜索表单
const searchForm = reactive<LaborContractQuery>({
  pageNum: 1,
  pageSize: 10,
  employeeId: undefined,
  contractNo: undefined,
  status: undefined,
});

// 合同类型选项
const contractTypeOptions = [
  { label: '固定期限', value: 'FIXED' },
  { label: '无固定期限', value: 'UNFIXED' },
  { label: '项目合同', value: 'PROJECT' },
  { label: '实习', value: 'INTERN' },
];

// 合同状态选项
const statusOptions = [
  { label: '生效中', value: 'ACTIVE' },
  { label: '已到期', value: 'EXPIRED' },
  { label: '已终止', value: 'TERMINATED' },
];

// 状态颜色
const statusColorMap: Record<string, string> = {
  ACTIVE: 'green',
  EXPIRED: 'orange',
  TERMINATED: 'red',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  ACTIVE: '生效中',
  EXPIRED: '已到期',
  TERMINATED: '已终止',
};

// 合同类型文本
const contractTypeTextMap: Record<string, string> = {
  FIXED: '固定期限',
  UNFIXED: '无固定期限',
  PROJECT: '项目合同',
  INTERN: '实习',
};

// 表格列
const columns = [
  {
    title: '合同编号',
    dataIndex: 'contractNo',
    key: 'contractNo',
    width: 140,
  },
  {
    title: '员工姓名',
    dataIndex: 'employeeName',
    key: 'employeeName',
    width: 100,
  },
  {
    title: '合同类型',
    dataIndex: 'contractType',
    key: 'contractType',
    width: 100,
  },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  {
    title: '试用期(月)',
    dataIndex: 'probationMonths',
    key: 'probationMonths',
    width: 100,
  },
  { title: '基本工资', dataIndex: 'baseSalary', key: 'baseSalary', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  {
    title: '续签次数',
    dataIndex: 'renewCount',
    key: 'renewCount',
    width: 90,
  },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<LaborContractDTO[]>([]);
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

// 新增/编辑弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const editingId = ref<null | number>(null);
const contractForm = reactive<CreateLaborContractCommand & { status?: string }>(
  {
    employeeId: undefined as unknown as number,
    contractNo: undefined,
    contractType: '',
    startDate: '',
    endDate: undefined,
    probationMonths: undefined,
    probationEndDate: undefined,
    baseSalary: undefined,
    performanceBonus: undefined,
    otherAllowance: undefined,
    signDate: undefined,
    contractFileUrl: undefined,
    remark: undefined,
    status: undefined,
  },
);

// 详情弹窗
const detailVisible = ref(false);
const currentRecord = ref<LaborContractDTO | null>(null);

// 续签弹窗
const renewVisible = ref(false);
const renewLoading = ref(false);
const renewForm = reactive({
  newStartDate: '',
  newEndDate: '',
});

// 获取合同列表
async function fetchData() {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    };
    const res = await getLaborContractList(params);
    tableData.value = res.list || [];
    pagination.total = res.total || 0;
  } catch (error) {
    console.error('获取劳动合同列表失败:', error);
    message.error('获取劳动合同列表失败');
  } finally {
    loading.value = false;
  }
}

// 获取员工列表
async function fetchEmployees() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 10_000 });
    employeeList.value = res.list || [];
  } catch (error) {
    console.error('获取员工列表失败:', error);
    employeeList.value = [];
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1;
  searchForm.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 10,
    employeeId: undefined,
    contractNo: undefined,
    status: undefined,
  });
  pagination.current = 1;
  fetchData();
}

// 表格变化
function handleTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  searchForm.pageNum = pag.current;
  searchForm.pageSize = pag.pageSize;
  fetchData();
}

// 新增
function handleAdd() {
  editingId.value = null;
  Object.assign(contractForm, {
    employeeId: undefined,
    contractNo: undefined,
    contractType: '',
    startDate: '',
    endDate: undefined,
    probationMonths: undefined,
    probationEndDate: undefined,
    baseSalary: undefined,
    performanceBonus: undefined,
    otherAllowance: undefined,
    signDate: undefined,
    contractFileUrl: undefined,
    remark: undefined,
    status: undefined,
  });
  modalVisible.value = true;
}

// 编辑
async function handleEdit(record: Record<string, any>) {
  editingId.value = record.id;
  try {
    const detail = await getLaborContractDetail(record.id);
    Object.assign(contractForm, {
      employeeId: detail.employeeId,
      contractNo: detail.contractNo,
      contractType: detail.contractType,
      startDate: detail.startDate ? dayjs(detail.startDate) : undefined,
      endDate: detail.endDate ? dayjs(detail.endDate) : undefined,
      probationMonths: detail.probationMonths,
      probationEndDate: detail.probationEndDate
        ? dayjs(detail.probationEndDate)
        : undefined,
      baseSalary: detail.baseSalary,
      performanceBonus: detail.performanceBonus,
      otherAllowance: detail.otherAllowance,
      signDate: detail.signDate ? dayjs(detail.signDate) : undefined,
      contractFileUrl: detail.contractFileUrl,
      remark: detail.remark,
      status: detail.status,
    });
    modalVisible.value = true;
  } catch (error) {
    console.error('获取合同详情失败:', error);
    message.error('获取合同详情失败');
  }
}

// 查看详情
async function handleView(record: Record<string, any>) {
  try {
    currentRecord.value = await getLaborContractDetail(record.id);
    detailVisible.value = true;
  } catch (error) {
    console.error('获取合同详情失败:', error);
    message.error('获取合同详情失败');
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await deleteLaborContract(id);
    message.success('删除成功');
    fetchData();
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
}

// 保存
async function handleSave() {
  if (!contractForm.employeeId && !editingId.value) {
    message.warning('请选择员工');
    return;
  }
  if (!contractForm.contractType) {
    message.warning('请选择合同类型');
    return;
  }
  if (!contractForm.startDate) {
    message.warning('请选择开始日期');
    return;
  }

  modalLoading.value = true;
  try {
    const formData: any = {
      ...contractForm,
      startDate: contractForm.startDate
        ? dayjs(contractForm.startDate).format('YYYY-MM-DD')
        : undefined,
      endDate: contractForm.endDate
        ? dayjs(contractForm.endDate).format('YYYY-MM-DD')
        : undefined,
      probationEndDate: contractForm.probationEndDate
        ? dayjs(contractForm.probationEndDate).format('YYYY-MM-DD')
        : undefined,
      signDate: contractForm.signDate
        ? dayjs(contractForm.signDate).format('YYYY-MM-DD')
        : undefined,
    };

    if (editingId.value) {
      const updateData: UpdateLaborContractCommand = { ...formData };
      await updateLaborContract(editingId.value, updateData);
      message.success('更新成功');
    } else {
      await createLaborContract(formData);
      message.success('创建成功');
    }
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    console.error('保存失败:', error);
    message.error(error?.message || '保存失败');
  } finally {
    modalLoading.value = false;
  }
}

// 续签
function handleRenew(record: Record<string, any>) {
  currentRecord.value = record as LaborContractDTO;
  // 默认新合同开始日期为原合同结束日期次日
  const oldEndDate = record.endDate ? dayjs(record.endDate) : dayjs();
  renewForm.newStartDate = oldEndDate.add(1, 'day').format('YYYY-MM-DD');
  renewForm.newEndDate = oldEndDate.add(1, 'year').format('YYYY-MM-DD');
  renewVisible.value = true;
}

// 提交续签
async function handleRenewSubmit() {
  if (!currentRecord.value) return;
  if (!renewForm.newStartDate) {
    message.warning('请选择新合同开始日期');
    return;
  }
  if (!renewForm.newEndDate) {
    message.warning('请选择新合同结束日期');
    return;
  }

  renewLoading.value = true;
  try {
    await renewLaborContract(
      currentRecord.value.id,
      renewForm.newStartDate,
      renewForm.newEndDate,
    );
    message.success('续签成功');
    renewVisible.value = false;
    fetchData();
  } catch (error: any) {
    console.error('续签失败:', error);
    message.error(error?.message || '续签失败');
  } finally {
    renewLoading.value = false;
  }
}

// 格式化金额
function formatMoney(value: null | number | undefined): string {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

onMounted(() => {
  fetchEmployees();
  fetchData();
});
</script>

<template>
  <Page title="劳动合同管理" description="管理员工劳动合同信息">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Select
              v-model:value="searchForm.employeeId"
              placeholder="员工"
              allow-clear
              show-search
              style="width: 100%"
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
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Input
              v-model:value="searchForm.contractNo"
              placeholder="合同编号"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <Select
              v-model:value="searchForm.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
            >
              <Select.Option
                v-for="item in statusOptions"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </Select.Option>
            </Select>
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="10">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新建劳动合同</Button>
            </Space>
          </Col>
        </Row>
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
          <template v-if="column.key === 'contractType'">
            {{
              contractTypeTextMap[record.contractType] ||
              record.contractTypeName ||
              record.contractType
            }}
          </template>
          <template v-else-if="column.key === 'startDate'">
            {{
              record.startDate
                ? dayjs(record.startDate).format('YYYY-MM-DD')
                : '-'
            }}
          </template>
          <template v-else-if="column.key === 'endDate'">
            {{
              record.endDate ? dayjs(record.endDate).format('YYYY-MM-DD') : '-'
            }}
          </template>
          <template v-else-if="column.key === 'baseSalary'">
            {{ formatMoney(record.baseSalary) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'renewCount'">
            {{ record.renewCount || 0 }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button type="link" size="small" @click="handleView(record)">
                查看
              </Button>
              <Button type="link" size="small" @click="handleEdit(record)">
                编辑
              </Button>
              <Button
                v-if="record.status === 'ACTIVE'"
                type="link"
                size="small"
                @click="handleRenew(record)"
              >
                续签
              </Button>
              <Popconfirm
                title="确定要删除该合同吗？"
                @confirm="handleDelete(record.id)"
              >
                <Button type="link" size="small" danger>删除</Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑劳动合同' : '新建劳动合同'"
      :width="700"
      :confirm-loading="modalLoading"
      @ok="handleSave"
      @cancel="modalVisible = false"
    >
      <Form :model="contractForm" layout="vertical">
        <FormItem label="员工" :required="!editingId">
          <Select
            v-model:value="contractForm.employeeId"
            placeholder="请选择员工"
            :disabled="!!editingId"
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
              {{ emp.realName || '-' }} ({{ emp.employeeNo || '-' }})
            </Select.Option>
          </Select>
        </FormItem>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="合同编号">
              <Input
                v-model:value="contractForm.contractNo"
                placeholder="留空自动生成"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="合同类型" required>
              <Select
                v-model:value="contractForm.contractType"
                placeholder="请选择合同类型"
              >
                <Select.Option
                  v-for="item in contractTypeOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="开始日期" required>
              <DatePicker
                v-model:value="contractForm.startDate"
                style="width: 100%"
                placeholder="请选择开始日期"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="结束日期">
              <DatePicker
                v-model:value="contractForm.endDate"
                style="width: 100%"
                placeholder="无固定期限可不填"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="签订日期">
              <DatePicker
                v-model:value="contractForm.signDate"
                style="width: 100%"
                placeholder="请选择签订日期"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="试用期(月)">
              <InputNumber
                v-model:value="contractForm.probationMonths"
                :min="0"
                :max="12"
                style="width: 100%"
                placeholder="请输入试用期月数"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="试用期结束日期">
              <DatePicker
                v-model:value="contractForm.probationEndDate"
                style="width: 100%"
                placeholder="请选择试用期结束日期"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem v-if="editingId" label="合同状态">
              <Select
                v-model:value="contractForm.status"
                placeholder="请选择状态"
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
          </Col>
        </Row>

        <Divider>薪资信息</Divider>

        <Row :gutter="16">
          <Col :span="8">
            <FormItem label="基本工资">
              <InputNumber
                v-model:value="contractForm.baseSalary"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="请输入基本工资"
                :formatter="
                  (value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                "
                :parser="(value) => value!.replace(/¥\s?|(,*)/g, '')"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="绩效奖金">
              <InputNumber
                v-model:value="contractForm.performanceBonus"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="请输入绩效奖金"
                :formatter="
                  (value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                "
                :parser="(value) => value!.replace(/¥\s?|(,*)/g, '')"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="其他津贴">
              <InputNumber
                v-model:value="contractForm.otherAllowance"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="请输入其他津贴"
                :formatter="
                  (value) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
                "
                :parser="(value) => value!.replace(/¥\s?|(,*)/g, '')"
              />
            </FormItem>
          </Col>
        </Row>

        <FormItem label="合同附件URL">
          <Input
            v-model:value="contractForm.contractFileUrl"
            placeholder="请输入合同附件URL"
          />
        </FormItem>

        <FormItem label="备注">
          <Textarea
            v-model:value="contractForm.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="劳动合同详情"
      :width="700"
      :footer="null"
    >
      <Descriptions v-if="currentRecord" :column="2" bordered>
        <DescriptionsItem label="合同编号">
          {{ currentRecord.contractNo || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="员工姓名">
          {{ currentRecord.employeeName || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="合同类型">
          {{
            contractTypeTextMap[currentRecord.contractType || ''] ||
            currentRecord.contractTypeName ||
            '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="合同状态">
          <Tag :color="statusColorMap[currentRecord.status || '']">
            {{
              statusTextMap[currentRecord.status || ''] ||
              currentRecord.statusName ||
              '-'
            }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="开始日期">
          {{
            currentRecord.startDate
              ? dayjs(currentRecord.startDate).format('YYYY-MM-DD')
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="结束日期">
          {{
            currentRecord.endDate
              ? dayjs(currentRecord.endDate).format('YYYY-MM-DD')
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="签订日期">
          {{
            currentRecord.signDate
              ? dayjs(currentRecord.signDate).format('YYYY-MM-DD')
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="试用期(月)">
          {{ currentRecord.probationMonths ?? '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="试用期结束日期">
          {{
            currentRecord.probationEndDate
              ? dayjs(currentRecord.probationEndDate).format('YYYY-MM-DD')
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="续签次数">
          {{ currentRecord.renewCount || 0 }}
        </DescriptionsItem>
        <DescriptionsItem label="基本工资">
          {{ formatMoney(currentRecord.baseSalary) }}
        </DescriptionsItem>
        <DescriptionsItem label="绩效奖金">
          {{ formatMoney(currentRecord.performanceBonus) }}
        </DescriptionsItem>
        <DescriptionsItem label="其他津贴">
          {{ formatMoney(currentRecord.otherAllowance) }}
        </DescriptionsItem>
        <DescriptionsItem label="合同附件">
          <a
            v-if="currentRecord.contractFileUrl"
            :href="currentRecord.contractFileUrl"
            target="_blank"
          >
            查看附件
          </a>
          <span v-else>-</span>
        </DescriptionsItem>
        <DescriptionsItem label="备注" :span="2">
          {{ currentRecord.remark || '-' }}
        </DescriptionsItem>
      </Descriptions>
    </Modal>

    <!-- 续签弹窗 -->
    <Modal
      v-model:open="renewVisible"
      title="续签劳动合同"
      :confirm-loading="renewLoading"
      @ok="handleRenewSubmit"
      @cancel="renewVisible = false"
    >
      <div v-if="currentRecord" style="margin-bottom: 16px">
        <p><strong>员工：</strong>{{ currentRecord.employeeName }}</p>
        <p><strong>原合同编号：</strong>{{ currentRecord.contractNo }}</p>
        <p>
          <strong>原合同结束日期：</strong>
          {{
            currentRecord.endDate
              ? dayjs(currentRecord.endDate).format('YYYY-MM-DD')
              : '-'
          }}
        </p>
      </div>
      <Form layout="vertical">
        <FormItem label="新合同开始日期" required>
          <DatePicker
            v-model:value="renewForm.newStartDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="新合同结束日期" required>
          <DatePicker
            v-model:value="renewForm.newEndDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
