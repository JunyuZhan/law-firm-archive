<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type {
  ClientDTO,
  ClientQuery,
  CreateClientCommand,
  UpdateClientCommand,
} from '#/api/client/types';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon, Plus } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Textarea,
  Tooltip,
  Upload,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  batchDeleteClients,
  convertToFormal,
  createClient,
  deleteClient,
  getClientList,
  quickConflictCheck,
  updateClient,
} from '#/api/client';
import {
  OCR_DISABLED,
  OCR_DISABLED_MESSAGE,
  recognizeBusinessLicense,
  recognizeIdCard,
} from '#/api/ocr';
import { UserTreeSelect } from '#/components/UserTreeSelect';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'CrmClient' });

const router = useRouter();
const route = useRoute();

// 响应式布局
const { isMobile } = useResponsive();

// 获取当前年份
const currentYear = new Date().getFullYear();

// 年份选项（最近5年 + 全部）
const yearOptions = [
  { label: '全部年份', value: 0 },
  ...Array.from({ length: 5 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i,
  })),
];

// 选中的年份（0表示全部）
const selectedYear = ref<number>(currentYear);

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const modalTitle = ref('新增客户');
const formRef = ref();
const selectedRowKeys = ref<number[]>([]);
const conflictChecking = ref(false);
const conflictCheckResult = ref<null | {
  candidates?: Array<{
    clientId: number;
    clientName: string;
    clientNo: string;
    clientType: string;
    matchScore: number;
    matchType: 'CONTAINS' | 'EXACT' | 'SIMILAR';
    riskLevel: 'HIGH' | 'LOW' | 'MEDIUM';
    riskReason: string;
  }>;
  canProceed: boolean;
  checked: boolean;
  conflictDetail?: string;
  hasConflict: boolean;
  riskLevel?: 'HIGH' | 'LOW' | 'MEDIUM' | 'NONE';
  riskSummary?: string;
}>(null);

// OCR识别相关状态
const ocrLoading = ref(false);
const ocrType = ref<'idcard' | 'license' | null>(null);

const queryParams = ref<ClientQuery>({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  clientType: undefined,
  level: undefined,
  status: undefined,
  // 默认筛选当前年份（按创建时间）
  createdAtFrom: `${currentYear}-01-01T00:00:00`,
  createdAtTo: `${currentYear}-12-31T23:59:59`,
});

const formData = reactive<
  Partial<CreateClientCommand> & { id?: number; opposingParty?: string }
>({
  id: undefined,
  name: '',
  clientType: 'ENTERPRISE',
  creditCode: '',
  idCard: '',
  legalRepresentative: '',
  registeredAddress: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  industry: '',
  source: '',
  level: 'B',
  category: 'NORMAL',
  originatorId: undefined,
  responsibleLawyerId: undefined,
  remark: '',
  opposingParty: '',
});

// ==================== 常量选项 ====================

const clientTypeOptions = [
  { label: '全部', value: undefined },
  { label: '企业客户', value: 'ENTERPRISE' },
  { label: '个人客户', value: 'INDIVIDUAL' },
  { label: '政府机关', value: 'GOVERNMENT' },
  { label: '其他', value: 'OTHER' },
];

const clientTypeFormOptions = [
  { label: '企业客户', value: 'ENTERPRISE' },
  { label: '个人客户', value: 'INDIVIDUAL' },
  { label: '政府机关', value: 'GOVERNMENT' },
  { label: '其他', value: 'OTHER' },
];

const levelOptions = [
  { label: '全部', value: undefined },
  { label: '重要客户', value: 'A' },
  { label: '普通客户', value: 'B' },
  { label: '一般客户', value: 'C' },
];

const levelFormOptions = [
  { label: '重要客户', value: 'A' },
  { label: '普通客户', value: 'B' },
  { label: '一般客户', value: 'C' },
];

const categoryOptions = [
  { label: '重要客户', value: 'VIP' },
  { label: '普通客户', value: 'NORMAL' },
  { label: '潜在客户', value: 'POTENTIAL' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '潜在', value: 'POTENTIAL' },
  { label: '正式', value: 'ACTIVE' },
  { label: '休眠', value: 'INACTIVE' },
  { label: '黑名单', value: 'BLACKLIST' },
];

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { type: 'checkbox' as const, width: 50 },
    { title: '客户编号', field: 'clientNo', width: 120 },
    {
      title: '客户名称',
      field: 'name',
      minWidth: isMobile.value ? 120 : 180,
      mobileShow: true,
    },
    {
      title: '客户类型',
      field: 'clientTypeName',
      width: 100,
      mobileShow: true,
    },
    { title: '联系人', field: 'contactPerson', width: 100 },
    { title: '联系电话', field: 'contactPhone', width: 130 },
    { title: '客户级别', field: 'levelName', width: 100 },
    { title: '客户分类', field: 'categoryName', width: 100 },
    {
      title: '状态',
      field: 'statusName',
      width: 80,
      slots: { default: 'status' },
      mobileShow: true,
    },
    { title: '案源人', field: 'originatorName', width: 100 },
    { title: '负责律师', field: 'responsibleLawyerName', width: 100 },
    { title: '创建时间', field: 'createdAt', width: 160 },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 100 : 180,
      fixed: 'right' as const,
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  // 移动端隐藏部分列
  if (isMobile.value) {
    return baseColumns.filter(
      (col) => col.type === 'checkbox' || col.mobileShow === true,
    );
  }
  return baseColumns;
}

async function loadData({
  page,
}: {
  page: { currentPage: number; pageSize: number };
}) {
  const params = {
    ...queryParams.value,
    pageNum: page.currentPage,
    pageSize: page.pageSize,
  };
  const res = await getClientList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
    checkboxConfig: {
      reserve: true,
    },
  },
  gridEvents: {
    checkboxChange: () => {
      const records = (gridApi.grid?.getCheckboxRecords() as ClientDTO[]) || [];
      selectedRowKeys.value = records.map((r) => r.id);
    },
    checkboxAll: () => {
      const records = (gridApi.grid?.getCheckboxRecords() as ClientDTO[]) || [];
      selectedRowKeys.value = records.map((r) => r.id);
    },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

// 年份变化时更新日期范围
function handleYearChange(value: any) {
  const year = Number(value);
  if (Number.isNaN(year)) return;
  selectedYear.value = year;
  if (year === 0) {
    // 选择"全部年份"时清除日期筛选
    queryParams.value.createdAtFrom = undefined;
    queryParams.value.createdAtTo = undefined;
  } else {
    queryParams.value.createdAtFrom = `${year}-01-01T00:00:00`;
    queryParams.value.createdAtTo = `${year}-12-31T23:59:59`;
  }
  queryParams.value.pageNum = 1;
  gridApi.reload();
}

function handleReset() {
  selectedYear.value = currentYear;
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    clientType: undefined,
    level: undefined,
    status: undefined,
    createdAtFrom: `${currentYear}-01-01T00:00:00`,
    createdAtTo: `${currentYear}-12-31T23:59:59`,
  };
  gridApi.reload();
}

// ==================== CRUD 操作 ====================

function handleAdd() {
  modalTitle.value = '新增客户';
  Object.assign(formData, {
    id: undefined,
    name: '',
    clientType: 'ENTERPRISE',
    creditCode: '',
    idCard: '',
    legalRepresentative: '',
    registeredAddress: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    industry: '',
    source: '',
    level: 'B',
    category: 'NORMAL',
    originatorId: undefined,
    responsibleLawyerId: undefined,
    remark: '',
    opposingParty: '',
  });
  conflictCheckResult.value = null;
  modalVisible.value = true;
}

function handleEdit(row: ClientDTO) {
  modalTitle.value = '编辑客户';
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    clientType: row.clientType,
    creditCode: row.creditCode,
    idCard: row.idCard,
    legalRepresentative: row.legalRepresentative,
    registeredAddress: row.registeredAddress,
    contactPerson: row.contactPerson,
    contactPhone: row.contactPhone,
    contactEmail: row.contactEmail,
    industry: row.industry,
    source: row.source,
    level: row.level,
    category: row.category,
    originatorId: row.originatorId,
    responsibleLawyerId: row.responsibleLawyerId,
    remark: row.remark,
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();

    if (formData.id) {
      const updateData: UpdateClientCommand = { id: formData.id, ...formData };
      await updateClient(updateData);
      message.success('更新成功');
      modalVisible.value = false;
      gridApi.reload();
    } else {
      const createData: CreateClientCommand = {
        ...formData,
      } as CreateClientCommand;
      const newClient = await createClient(createData);
      message.success('创建成功');

      const returnPath = route.query.returnPath as string;
      // 安全校验：仅允许以 / 开头的相对路径，防止开放重定向
      if (
        returnPath &&
        returnPath.startsWith('/') &&
        !returnPath.startsWith('//')
      ) {
        const returnQuery: Record<string, string> = {};
        try {
          const parsedQuery = route.query.returnQuery
            ? JSON.parse(route.query.returnQuery as string)
            : {};
          // 白名单校验：仅保留基本类型的值，防止原型污染
          if (
            parsedQuery &&
            typeof parsedQuery === 'object' &&
            !Array.isArray(parsedQuery)
          ) {
            for (const key of Object.keys(parsedQuery)) {
              const value = parsedQuery[key];
              // 仅保留字符串和数字类型的值，跳过 __proto__ 等危险属性
              if (
                !key.startsWith('__') &&
                (typeof value === 'string' || typeof value === 'number')
              ) {
                returnQuery[key] = String(value);
              }
            }
          }
        } catch {
          // JSON 解析失败，使用空对象
          console.warn('returnQuery 解析失败，使用默认值');
        }
        router.push({
          path: returnPath,
          query: { ...returnQuery, action: 'created', clientId: newClient.id },
        });
      } else {
        modalVisible.value = false;
        gridApi.reload();
      }
    }
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '操作失败');
  }
}

async function handleDelete(row: ClientDTO) {
  try {
    await deleteClient(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的客户');
    return;
  }
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个客户吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await batchDeleteClients(selectedRowKeys.value);
        message.success('批量删除成功');
        selectedRowKeys.value = [];
        gridApi.grid?.clearCheckboxRow();
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '批量删除失败');
      }
    },
  });
}

function handleConvert(row: ClientDTO) {
  Modal.confirm({
    title: '确认转正',
    content: `确定要将 "${row.name}" 转为正式客户吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await convertToFormal(row.id);
        message.success('转正成功');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '转正失败');
      }
    },
  });
}

// ==================== 利冲检索 ====================

async function handleConflictCheck() {
  if (!formData.name) {
    message.warning('请先输入客户名称');
    return;
  }
  if (!formData.opposingParty) {
    message.warning('请先输入对方当事人');
    return;
  }

  conflictChecking.value = true;
  try {
    const result = await quickConflictCheck({
      clientName: formData.name,
      opposingParty: formData.opposingParty,
    });

    conflictCheckResult.value = {
      checked: true,
      hasConflict: result.hasConflict,
      conflictDetail: result.conflictDetail,
      canProceed: !result.hasConflict,
      candidates: result.candidates || [],
      riskLevel: result.riskLevel,
      riskSummary: result.riskSummary,
    };

    // 根据风险级别显示不同提示，与弹窗显示保持一致
    if (result.hasConflict || result.riskLevel === 'HIGH') {
      message.warning('发现可能存在利益冲突，请仔细核对');
    } else if (result.riskLevel === 'MEDIUM') {
      message.warning('发现相似客户，请确认是否为同一人/公司');
    } else if (
      result.riskLevel === 'LOW' ||
      (result.candidates && result.candidates.length > 0)
    ) {
      message.info('发现名称相近的客户，请核对后继续');
    } else {
      message.success('未发现冲突，可以创建客户');
    }
  } catch (error: any) {
    message.error(error.message || '利冲检索失败');
  } finally {
    conflictChecking.value = false;
  }
}

function goToConflictApplication() {
  router.push({
    path: '/crm/conflict',
    query: {
      action: 'apply',
      clientName: formData.name,
      opposingParty: formData.opposingParty,
    },
  });
  modalVisible.value = false;
}

const canSave = computed(() => {
  if (formData.id) return true;
  if (formData.opposingParty) {
    return (
      conflictCheckResult.value?.checked &&
      !conflictCheckResult.value?.hasConflict
    );
  }
  return true;
});

// ==================== OCR识别操作 ====================

async function handleIdCardOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;

  ocrLoading.value = true;
  ocrType.value = 'idcard';

  try {
    const result = await recognizeIdCard(file, true); // 识别正面

    if (result && result.success) {
      // 自动填充表单（处理空字符串的情况）
      // 优先从顶层字段读取，如果没有则从data字段读取
      let name = result.name || result.data?.name || '';
      let idNumber = result.idNumber || result.data?.idNumber || '';

      // 如果字段为空，尝试从原始文本中提取
      const rawText = result.rawText || result.data?.rawText || '';

      // 检查OCR识别质量：如果原始文本太短或看起来不像身份证内容，给出提示
      const isLowQuality =
        rawText.length < 20 || !/[\u4E00-\u9FA5]/.test(rawText);

      if ((!name || !name.trim()) && rawText && !isLowQuality) {
        // 尝试从原始文本中提取姓名（2-4个中文字符）
        const nameMatch = rawText.match(/[\u4E00-\u9FA5]{2,4}/);
        if (nameMatch) {
          name = nameMatch[0];
        }
      }

      if ((!idNumber || !idNumber.trim()) && rawText && !isLowQuality) {
        // 尝试从原始文本中提取身份证号（18位数字，最后一位可能是X）
        const idMatch = rawText.match(/\d{17}[\dX]/i);
        if (idMatch) {
          idNumber = idMatch[0].toUpperCase();
        }
      }

      if (name && name.trim()) {
        formData.name = name.trim();
      }
      if (idNumber && idNumber.trim()) {
        formData.idCard = idNumber.trim();
      }
      // 身份证识别时，如果是个人客户，联系人就是本人
      if (name && name.trim() && !formData.contactPerson) {
        formData.contactPerson = name.trim();
      }
      // 设置客户类型为个人
      formData.clientType = 'INDIVIDUAL';

      // 如果关键字段仍然为空，显示原始文本提示
      if ((!name || !name.trim()) && (!idNumber || !idNumber.trim())) {
        const isLowQuality =
          rawText.length < 20 || !/[\u4E00-\u9FA5]/.test(rawText);
        const qualityTip = isLowQuality
          ? '\n\n⚠️ OCR识别质量较低，可能是图片质量问题或图片不是身份证。请检查图片是否清晰、完整。'
          : '\n\n请手动从原始文本中提取信息并填写表单。';

        Modal.info({
          title: 'OCR识别结果',
          content: `识别成功，但未能自动提取字段。${qualityTip}\n\n原始文本：\n\n${rawText || '无原始文本'}\n\n请手动填写表单。`,
          width: 600,
        });
      } else {
        message.success(
          `身份证识别成功！置信度: ${Math.round((result.confidence || 0) * 100)}%`,
        );
      }
    } else {
      message.error(result?.errorMessage || '身份证识别失败');
    }
  } catch (error: any) {
    message.error(error?.message || '身份证识别失败');
  } finally {
    ocrLoading.value = false;
    ocrType.value = null;
  }
}

async function handleBusinessLicenseOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;

  ocrLoading.value = true;
  ocrType.value = 'license';

  try {
    const result = await recognizeBusinessLicense(file);

    if (result && result.success) {
      // 自动填充表单（处理空字符串的情况）
      // 优先从顶层字段读取，如果没有则从data字段读取
      let companyName = result.companyName || result.data?.companyName || '';
      let creditCode = result.creditCode || result.data?.creditCode || '';
      const legalRepresentative =
        result.legalRepresentative || result.data?.legalRepresentative || '';

      // 如果字段为空，尝试从原始文本中提取
      const rawText = result.rawText || result.data?.rawText || '';

      if ((!companyName || !companyName.trim()) && rawText) {
        // 尝试从原始文本中提取公司名称（通常在"名称"或"公司名称"后面）
        const nameMatch = rawText.match(
          /(?:名称|公司名称|企业名称)[：:]\s*([^\n]+)/,
        );
        if (nameMatch) {
          companyName = nameMatch[1].trim();
        }
      }

      if ((!creditCode || !creditCode.trim()) && rawText) {
        // 尝试从原始文本中提取统一社会信用代码（18位）
        const codeMatch = rawText.match(
          /(?:统一社会信用代码|信用代码|注册号)[：:]\s*([A-Z0-9]{18})/,
        );
        if (codeMatch) {
          creditCode = codeMatch[1];
        }
      }

      if (companyName && companyName.trim()) {
        formData.name = companyName.trim();
      }
      if (creditCode && creditCode.trim()) {
        formData.creditCode = creditCode.trim();
      }
      if (legalRepresentative && legalRepresentative.trim()) {
        formData.legalRepresentative = legalRepresentative.trim();
        // 如果没有联系人，法人代表可以作为联系人
        if (!formData.contactPerson) {
          formData.contactPerson = legalRepresentative.trim();
        }
      }
      // 设置客户类型为企业
      formData.clientType = 'ENTERPRISE';

      // 如果关键字段仍然为空，显示原始文本提示
      if (
        (!companyName || !companyName.trim()) &&
        (!creditCode || !creditCode.trim())
      ) {
        Modal.info({
          title: 'OCR识别结果',
          content: `识别成功，但未能自动提取字段。原始文本：\n\n${rawText || '无原始文本'}\n\n请手动填写表单。`,
          width: 600,
        });
      } else {
        message.success(
          `营业执照识别成功！置信度: ${Math.round((result.confidence || 0) * 100)}%`,
        );
      }
    } else {
      message.error(result?.errorMessage || '营业执照识别失败');
    }
  } catch (error: any) {
    message.error(error?.message || '营业执照识别失败');
  } finally {
    ocrLoading.value = false;
    ocrType.value = null;
  }
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    POTENTIAL: 'orange',
    ACTIVE: 'green',
    INACTIVE: 'gray',
    BLACKLIST: 'red',
  };
  return colorMap[status] || 'default';
}

onMounted(async () => {
  if (route.query.action === 'create') {
    handleAdd();
  }
});
</script>

<template>
  <Page title="客户管理" description="管理客户信息">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="selectedYear"
              placeholder="创建年份"
              style="width: 100%"
              :options="yearOptions"
              @change="handleYearChange"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Input
              v-model:value="queryParams.name"
              placeholder="客户名称"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.clientType"
              placeholder="客户类型"
              allow-clear
              style="width: 100%"
              :options="clientTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.level"
              placeholder="客户级别"
              allow-clear
              style="width: 100%"
              :options="levelOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
        </Row>
        <Row :gutter="16" style="margin-top: 16px">
          <Col :span="24">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button
                v-access:code="'client:create'"
                type="primary"
                @click="handleAdd"
              >
                <Plus class="size-4" />新增客户
              </Button>
              <Button
                v-if="selectedRowKeys.length > 0"
                v-access:code="'client:delete'"
                danger
                @click="handleBatchDelete"
              >
                批量删除 ({{ selectedRowKeys.length }})
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <!-- 状态列 -->
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <Space>
            <a v-access:code="'client:update'" @click="handleEdit(row)">编辑</a>
            <template v-if="row.status === 'POTENTIAL'">
              <a v-access:code="'client:update'" @click="handleConvert(row)"
                >转正式</a
              >
            </template>
            <Popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <a v-access:code="'client:delete'" style="color: red">删除</a>
            </Popconfirm>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="isMobile ? '100%' : '800px'"
      :centered="isMobile"
    >
      <template #footer>
        <Button @click="modalVisible = false">取消</Button>
        <Button type="primary" :disabled="!canSave" @click="handleSave">
          {{ !canSave ? '请先进行利冲检索' : '确定' }}
        </Button>
      </template>
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- OCR智能识别区域 -->
        <div
          v-if="!formData.id && !OCR_DISABLED"
          class="mb-4 rounded-lg border border-blue-200 bg-blue-50 p-4"
        >
          <div class="mb-2 flex items-center">
            <IconifyIcon
              icon="ant-design:scan-outlined"
              class="mr-2 text-blue-500"
            />
            <span class="font-medium text-blue-700">智能识别填充</span>
            <span class="ml-2 text-sm text-gray-500"
              >上传证件照片，自动识别并填充表单</span
            >
          </div>
          <Space>
            <Spin :spinning="ocrLoading && ocrType === 'license'" size="small">
              <Upload
                :show-upload-list="false"
                :before-upload="() => false"
                accept="image/*"
                @change="handleBusinessLicenseOcr"
              >
                <Tooltip title="上传营业执照图片，自动识别企业信息">
                  <Button
                    :loading="ocrLoading && ocrType === 'license'"
                    :disabled="ocrLoading"
                  >
                    <template #icon>
                      <IconifyIcon icon="ant-design:audit-outlined" />
                    </template>
                    营业执照识别
                  </Button>
                </Tooltip>
              </Upload>
            </Spin>
            <Spin :spinning="ocrLoading && ocrType === 'idcard'" size="small">
              <Upload
                :show-upload-list="false"
                :before-upload="() => false"
                accept="image/*"
                @change="handleIdCardOcr"
              >
                <Tooltip title="上传身份证正面图片，自动识别个人信息">
                  <Button
                    :loading="ocrLoading && ocrType === 'idcard'"
                    :disabled="ocrLoading"
                  >
                    <template #icon>
                      <IconifyIcon icon="ant-design:idcard-outlined" />
                    </template>
                    身份证识别
                  </Button>
                </Tooltip>
              </Upload>
            </Spin>
          </Space>
        </div>
        <!-- OCR禁用提示 -->
        <div
          v-else-if="!formData.id && OCR_DISABLED"
          class="mb-4 rounded-lg border border-gray-200 bg-gray-50 p-4"
        >
          <div class="mb-2 flex items-center">
            <IconifyIcon
              icon="ant-design:scan-outlined"
              class="mr-2 text-gray-400"
            />
            <span class="font-medium text-gray-500">智能识别填充</span>
            <Tag color="default" class="ml-2">暂不可用</Tag>
          </div>
          <div class="text-sm text-gray-400">{{ OCR_DISABLED_MESSAGE }}</div>
        </div>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="客户名称"
              name="name"
              :rules="[{ required: true, message: '请输入客户名称' }]"
            >
              <Input
                v-model:value="formData.name"
                placeholder="请输入客户名称"
                :maxlength="100"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="客户类型"
              name="clientType"
              :rules="[{ required: true, message: '请选择客户类型' }]"
            >
              <Select
                v-model:value="formData.clientType"
                :options="clientTypeFormOptions"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              v-if="formData.clientType === 'ENTERPRISE'"
              label="统一社会信用代码"
              name="creditCode"
              :rules="[{ required: true, message: '请输入统一社会信用代码' }]"
            >
              <Input
                v-model:value="formData.creditCode"
                placeholder="请输入统一社会信用代码"
                :maxlength="18"
              />
            </FormItem>
            <FormItem
              v-else-if="formData.clientType === 'INDIVIDUAL'"
              label="身份证号"
              name="idCard"
              :rules="[{ required: true, message: '请输入身份证号' }]"
            >
              <Input
                v-model:value="formData.idCard"
                placeholder="请输入身份证号"
                :maxlength="18"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              v-if="formData.clientType === 'ENTERPRISE'"
              label="法定代表人"
              name="legalRepresentative"
              :rules="[{ required: true, message: '请输入法定代表人' }]"
            >
              <Input
                v-model:value="formData.legalRepresentative"
                placeholder="请输入法定代表人"
                :maxlength="50"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="24">
            <FormItem
              label="联系地址"
              name="registeredAddress"
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 21 }"
              :rules="[{ required: true, message: '请输入联系地址' }]"
            >
              <Input
                v-model:value="formData.registeredAddress"
                placeholder="请输入联系地址（用于合同等法律文书）"
                :maxlength="200"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="联系人"
              name="contactPerson"
              :rules="[{ required: true, message: '请输入联系人' }]"
            >
              <Input
                v-model:value="formData.contactPerson"
                placeholder="请输入联系人"
                :maxlength="50"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem
              label="联系电话"
              name="contactPhone"
              :rules="[
                { required: true, message: '请输入联系电话' },
                {
                  pattern: /^[\d\-+() ]{7,20}$/,
                  message: '请输入有效的电话号码',
                },
              ]"
            >
              <Input
                v-model:value="formData.contactPhone"
                placeholder="请输入联系电话"
                :maxlength="20"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem
              label="联系邮箱"
              name="contactEmail"
              :rules="[
                {
                  type: 'email',
                  message: '请输入有效的邮箱地址',
                },
              ]"
            >
              <Input
                v-model:value="formData.contactEmail"
                placeholder="请输入联系邮箱"
                :maxlength="100"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="所属行业" name="industry">
              <Input
                v-model:value="formData.industry"
                placeholder="请输入所属行业"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="客户级别" name="level">
              <Select
                v-model:value="formData.level"
                :options="levelFormOptions"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="客户分类" name="category">
              <Select
                v-model:value="formData.category"
                :options="categoryOptions"
              />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="案源人" name="originatorId">
              <UserTreeSelect
                v-model:value="formData.originatorId"
                placeholder="选择案源人（按部门筛选）"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="负责律师" name="responsibleLawyerId">
              <UserTreeSelect
                v-model:value="formData.responsibleLawyerId"
                placeholder="选择负责律师（按部门筛选）"
              />
            </FormItem>
          </Col>
        </Row>

        <!-- 利益冲突检索区域（仅新增时显示） -->
        <template v-if="!formData.id">
          <Divider>利益冲突检索</Divider>
          <Row :gutter="16">
            <Col :span="16">
              <FormItem label="对方当事人" name="opposingParty">
                <Input
                  v-model:value="formData.opposingParty"
                  placeholder="请输入对方当事人（如有案件时填写）"
                  @change="conflictCheckResult = null"
                />
              </FormItem>
            </Col>
            <Col :span="8">
              <FormItem :wrapper-col="{ offset: 0 }">
                <Button
                  type="primary"
                  ghost
                  :loading="conflictChecking"
                  :disabled="!formData.name || !formData.opposingParty"
                  @click="handleConflictCheck"
                >
                  🔍 利冲检索
                </Button>
              </FormItem>
            </Col>
          </Row>

          <!-- 利冲检索结果显示 -->
          <Row v-if="conflictCheckResult?.checked" :gutter="16">
            <Col :span="24">
              <!-- 无冲突 -->
              <Alert
                v-if="conflictCheckResult.riskLevel === 'NONE'"
                type="success"
                message="未检测到利益冲突"
                description="对方当事人不是本所现有客户，可以正常创建客户关系。"
                show-icon
              />

              <!-- 有候选匹配项 -->
              <div v-else>
                <!-- 风险摘要 -->
                <Alert
                  :type="
                    conflictCheckResult.riskLevel === 'HIGH'
                      ? 'error'
                      : conflictCheckResult.riskLevel === 'MEDIUM'
                        ? 'warning'
                        : 'info'
                  "
                  :message="
                    conflictCheckResult.riskLevel === 'HIGH'
                      ? '⚠️ 可能存在利益冲突'
                      : conflictCheckResult.riskLevel === 'MEDIUM'
                        ? '发现相似客户，请核对'
                        : '发现名称相近的客户'
                  "
                  :description="conflictCheckResult.riskSummary"
                  show-icon
                  class="mb-3"
                >
                  <template v-if="conflictCheckResult.hasConflict" #action>
                    <Button
                      size="small"
                      type="primary"
                      danger
                      @click="goToConflictApplication"
                    >
                      申请利冲豁免
                    </Button>
                  </template>
                </Alert>

                <!-- 候选列表 -->
                <div
                  v-if="
                    conflictCheckResult.candidates &&
                    conflictCheckResult.candidates.length > 0
                  "
                  class="conflict-candidates"
                >
                  <div class="mb-2 text-sm text-gray-600">匹配的现有客户：</div>
                  <Table
                    :data-source="conflictCheckResult.candidates"
                    :columns="[
                      {
                        title: '客户名称',
                        dataIndex: 'clientName',
                        key: 'clientName',
                      },
                      {
                        title: '客户编号',
                        dataIndex: 'clientNo',
                        key: 'clientNo',
                        width: 140,
                      },
                      {
                        title: '匹配度',
                        dataIndex: 'matchScore',
                        key: 'matchScore',
                        width: 80,
                        align: 'center',
                      },
                      {
                        title: '风险等级',
                        dataIndex: 'riskLevel',
                        key: 'riskLevel',
                        width: 90,
                        align: 'center',
                      },
                      {
                        title: '匹配原因',
                        dataIndex: 'riskReason',
                        key: 'riskReason',
                      },
                    ]"
                    :pagination="false"
                    size="small"
                    row-key="clientId"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'matchScore'">
                        <span
                          :class="
                            record.matchScore >= 90
                              ? 'font-bold text-red-500'
                              : record.matchScore >= 70
                                ? 'text-orange-500'
                                : 'text-gray-500'
                          "
                        >
                          {{ record.matchScore }}%
                        </span>
                      </template>
                      <template v-if="column.key === 'riskLevel'">
                        <Tag
                          :color="
                            record.riskLevel === 'HIGH'
                              ? 'red'
                              : record.riskLevel === 'MEDIUM'
                                ? 'orange'
                                : 'default'
                          "
                        >
                          {{
                            record.riskLevel === 'HIGH'
                              ? '高'
                              : record.riskLevel === 'MEDIUM'
                                ? '中'
                                : '低'
                          }}
                        </Tag>
                      </template>
                    </template>
                  </Table>
                </div>
              </div>
            </Col>
          </Row>

          <Row
            v-if="formData.opposingParty && !conflictCheckResult?.checked"
            :gutter="16"
          >
            <Col :span="24">
              <Alert
                type="warning"
                message="提示"
                description="您已填写对方当事人，请点击【利冲检索】按钮进行冲突检查后再提交。"
                show-icon
              />
            </Col>
          </Row>
        </template>

        <FormItem label="备注" name="remark">
          <Textarea
            v-model:value="formData.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
