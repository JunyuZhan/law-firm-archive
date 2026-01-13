<script setup lang="ts">
import type {
  ArchiveCheckResult,
  ArchiveDataSnapshot,
  ArchiveDataSource,
  ArchiveDTO,
  ArchiveQuery,
  CreateArchiveCommand,
  StoreArchiveCommand,
} from '#/api/archive/types';
import type { MatterDTO, MatterSimpleDTO } from '#/api/matter/types';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { FileOutlined, Printer, RotateCw } from '@vben/icons';

import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  DescriptionsItem,
  Dropdown,
  Form,
  FormItem,
  Input,
  Menu,
  MenuItem,
  message,
  Modal,
  Pagination,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Step,
  Steps,
  Tabs,
  TabPane,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  approveStore,
  checkArchiveRequirements,
  createArchive,
  getArchiveDataSources,
  getArchiveList,
  previewArchiveData,
  regenerateArchiveCover,
  storeArchive,
  submitStoreApproval,
} from '#/api/archive';
import {
  changeMatterStatus,
  getMatterDetail,
  getMatterSelectOptions,
} from '#/api/matter';
import { getConfigValue } from '#/api/system';
import {
  CASE_CATEGORY_OPTIONS,
  getCaseCategoryByMatterType,
  MATTER_TYPE_OPTIONS,
} from '#/constants/causes/utils';
import ArchiveCoverPreview from '../components/ArchiveCoverPreview.vue';

defineOptions({ name: 'ArchiveList' });

const route = useRoute();
const router = useRouter();

// 状态
const loading = ref(false);
const dataSource = ref<ArchiveDTO[]>([]);
const total = ref(0);
const storeModalVisible = ref(false);
const approveModalVisible = ref(false);
const currentArchive = ref<ArchiveDTO | null>(null);
const matters = ref<MatterSimpleDTO[]>([]);
const matterDetailsMap = ref<Map<number, MatterDTO>>(new Map());
const systemFirmName = ref('律师事务所'); // 系统律所名称

// 归档向导状态
const archiveWizardVisible = ref(false);
const archiveWizardStep = ref(0);
const archiveWizardLoading = ref(false);
const selectedMatterId = ref<null | number>(null);
const checkResult = ref<ArchiveCheckResult | null>(null);
const dataSnapshot = ref<ArchiveDataSnapshot | null>(null);
const dataSources = ref<ArchiveDataSource[]>([]);
const selectedDataSourceIds = ref<number[]>([]);

// Tab 当前选中
const activeTab = ref<string>('all');

// 扩展查询参数（前端过滤用）
const extendedQuery = reactive({
  year: undefined as number | undefined,
  matterType: undefined as string | undefined,
  caseType: undefined as string | undefined,
});

// 查询参数
const queryParams = reactive<ArchiveQuery>({
  pageNum: 1,
  pageSize: 24, // 优化：减少单页数据量，使用分页加载
  archiveNo: undefined,
  archiveName: undefined,
  matterId: undefined,
  archiveType: undefined,
  status: undefined,
});

// 年度选项（最近10年）
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear();
  const options = [];
  for (let y = currentYear; y >= currentYear - 10; y--) {
    options.push({ label: `${y}年`, value: y });
  }
  return options;
});

// 案件类型选项（根据项目类型动态过滤）
const caseTypeOptions = computed(() => {
  if (extendedQuery.matterType) {
    return getCaseCategoryByMatterType(extendedQuery.matterType);
  }
  return CASE_CATEGORY_OPTIONS;
});

// 入库表单数据
const storeFormData = reactive<StoreArchiveCommand & { boxNo?: string }>({
  archiveId: 0,
  locationId: 0,
  boxNo: '',
});

// 审批表单数据
const approveFormData = reactive({
  approved: true,
  comment: '',
});

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待入库', value: 'PENDING' },
  { label: '待入库审批', value: 'PENDING_STORE' },
  { label: '已入库', value: 'STORED' },
  { label: '借出', value: 'BORROWED' },
  { label: '待迁移审批', value: 'PENDING_MIGRATE' },
  { label: '已迁移', value: 'MIGRATED' },
];

// 计算属性：可归档的项目（已结案或已归档）
const archivableMatters = computed(() => {
  return matters.value.filter(
    (m) => m.status === 'CLOSED' || m.status === 'ARCHIVED',
  );
});

// 获取档案对应的项目详情
function getMatterInfo(archive: ArchiveDTO): MatterDTO | undefined {
  if (archive.matterId) {
    return matterDetailsMap.value.get(archive.matterId);
  }
  return undefined;
}

// 计算属性：按扩展条件过滤后的数据
const extendedFilteredData = computed(() => {
  let result = dataSource.value;

  // 年度过滤
  if (extendedQuery.year) {
    result = result.filter((a) => {
      const dateStr = a.archiveDate || a.createdAt;
      if (!dateStr) return false;
      const year = new Date(dateStr).getFullYear();
      return year === extendedQuery.year;
    });
  }

  // 项目类型过滤（需要从matter详情获取）
  if (extendedQuery.matterType) {
    result = result.filter((a) => {
      const matter = getMatterInfo(a);
      return matter?.matterType === extendedQuery.matterType;
    });
  }

  // 案件类型过滤
  if (extendedQuery.caseType) {
    result = result.filter((a) => {
      const matter = getMatterInfo(a);
      return matter?.caseType === extendedQuery.caseType;
    });
  }

  return result;
});

// 计算属性：按Tab过滤后的数据
const filteredDataSource = computed(() => {
  let result = extendedFilteredData.value;

  if (activeTab.value === 'pending') {
    result = result.filter(
      (a) => a.status === 'PENDING' || a.status === 'PENDING_STORE',
    );
  } else if (activeTab.value === 'stored') {
    result = result.filter((a) => a.status === 'STORED');
  } else if (activeTab.value === 'borrowed') {
    result = result.filter((a) => a.status === 'BORROWED');
  }

  return result;
});

// 计算属性：各状态数量统计（基于扩展过滤后的数据）
const statusCounts = computed(() => {
  const data = extendedFilteredData.value;
  const counts = {
    all: data.length,
    pending: 0,
    stored: 0,
    borrowed: 0,
  };
  data.forEach((a) => {
    if (a.status === 'PENDING' || a.status === 'PENDING_STORE') {
      counts.pending++;
    } else if (a.status === 'STORED') {
      counts.stored++;
    } else if (a.status === 'BORROWED') {
      counts.borrowed++;
    }
  });
  return counts;
});

// Tab切换处理
function handleTabChange(key: string | number) {
  activeTab.value = String(key);
}

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getArchiveList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
    // 加载项目详情（用于显示封面信息）
    await loadMatterDetails(res.list);
  } catch (error: any) {
    message.error(error.message || '加载档案列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载项目详情
async function loadMatterDetails(archives: ArchiveDTO[]) {
  const matterIds = archives
    .filter((a) => a.matterId)
    .map((a) => a.matterId!)
    .filter((id, index, arr) => arr.indexOf(id) === index); // 去重

  // 创建新的 Map 以确保响应式更新
  const newMap = new Map(matterDetailsMap.value);
  let hasNewData = false;

  for (const matterId of matterIds) {
    if (!newMap.has(matterId)) {
      try {
        const matter = await getMatterDetail(matterId);
        newMap.set(matterId, matter);
        hasNewData = true;
      } catch (error: any) {
        console.error(`加载项目详情失败: matterId=${matterId}`, error);
      }
    }
  }

  // 触发响应式更新
  if (hasNewData) {
    matterDetailsMap.value = newMap;
  }
}

// 加载项目列表
async function loadMatters() {
  try {
    const res = await getMatterSelectOptions({ pageNum: 1, pageSize: 1000 });
    matters.value = res.list;
  } catch (error: any) {
    console.error('加载项目列表失败:', error);
  }
}

// 加载系统配置（律所名称）
async function loadSystemConfig() {
  try {
    const config = await getConfigValue('firm.name');
    if (config && config.configValue) {
      systemFirmName.value = config.configValue;
    }
  } catch (error: any) {
    console.error('加载系统配置失败:', error);
  }
}

// 加载数据源配置
async function loadDataSources() {
  try {
    dataSources.value = await getArchiveDataSources();
    // 默认选中所有启用的数据源
    selectedDataSourceIds.value = dataSources.value
      .filter((ds) => ds.is_enabled)
      .map((ds) => ds.id);
  } catch (error: any) {
    console.error('加载数据源配置失败:', error);
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.archiveNo = undefined;
  queryParams.archiveName = undefined;
  queryParams.matterId = undefined;
  queryParams.archiveType = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  // 重置扩展筛选条件
  extendedQuery.year = undefined;
  extendedQuery.matterType = undefined;
  extendedQuery.caseType = undefined;
  fetchData();
}

// 项目类型变化时清空案件类型
function handleMatterTypeChange() {
  extendedQuery.caseType = undefined;
}

// 打开归档向导
function handleOpenArchiveWizard() {
  archiveWizardVisible.value = true;
  archiveWizardStep.value = 0;
  selectedMatterId.value = null;
  checkResult.value = null;
  dataSnapshot.value = null;
  loadDataSources();
}

// 归档向导：选择项目后检查
async function handleSelectMatter() {
  if (!selectedMatterId.value) {
    message.warning('请选择要归档的项目');
    return;
  }

  archiveWizardLoading.value = true;
  try {
    // 检查归档条件
    checkResult.value = await checkArchiveRequirements(selectedMatterId.value);
    archiveWizardStep.value = 1;
  } catch (error: any) {
    message.error(error.message || '检查归档条件失败');
  } finally {
    archiveWizardLoading.value = false;
  }
}

// 归档向导：预览数据
async function handlePreviewData() {
  if (!selectedMatterId.value) return;

  archiveWizardLoading.value = true;
  try {
    dataSnapshot.value = await previewArchiveData(selectedMatterId.value);
    archiveWizardStep.value = 2;
  } catch (error: any) {
    message.error(error.message || '获取归档数据失败');
  } finally {
    archiveWizardLoading.value = false;
  }
}

// 归档向导：创建档案
async function handleCreateArchive() {
  if (!selectedMatterId.value || !dataSnapshot.value) return;

  archiveWizardLoading.value = true;
  try {
    const command: CreateArchiveCommand = {
      matterId: selectedMatterId.value,
      archiveName: `${dataSnapshot.value.matterName} - 档案`,
      archiveType: 'LITIGATION',
      retentionPeriod: '10_YEARS',
      selectedDataSourceIds: selectedDataSourceIds.value,
    };

    await createArchive(command);

    // 更新项目状态为已归档
    try {
      await changeMatterStatus(selectedMatterId.value, 'ARCHIVED');
    } catch (error) {
      console.warn('更新项目状态失败，项目可能已是归档状态', error);
    }

    message.success('档案创建成功，项目已归档');
    archiveWizardVisible.value = false;
    fetchData();
    // 重新加载项目列表以更新状态
    loadMatters();
  } catch (error: any) {
    message.error(error.message || '创建档案失败');
  } finally {
    archiveWizardLoading.value = false;
  }
}

// 提交入库审批
async function handleSubmitStoreApproval(record: ArchiveDTO) {
  Modal.confirm({
    title: '提交入库审批',
    content: `确定要提交档案 "${record.archiveName}" 的入库审批吗？`,
    onOk: async () => {
      try {
        await submitStoreApproval(record.id);
        message.success('已提交入库审批');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '提交失败');
      }
    },
  });
}

// 入库审批
function handleApproveStore(record: ArchiveDTO) {
  currentArchive.value = record;
  approveFormData.approved = true;
  approveFormData.comment = '';
  approveModalVisible.value = true;
}

// 保存入库审批
async function handleApproveStoreSave() {
  try {
    if (currentArchive.value) {
      await approveStore(
        currentArchive.value.id,
        approveFormData.approved,
        approveFormData.comment,
      );
      message.success(approveFormData.approved ? '审批通过' : '已拒绝');
      approveModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 打印卷宗封面
function handlePrintCover(record: ArchiveDTO) {
  const matter = getMatterInfo(record);
  
  // 创建打印窗口
  const printWindow = window.open('', '_blank', 'width=800,height=1000');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器是否阻止了弹窗');
    return;
  }

  // 判断案件类型
  const isCriminal = matter?.caseType === 'CRIMINAL';
  const isArbitration =
    matter?.litigationStage === 'ARBITRATION' ||
    matter?.caseType === 'LABOR_ARBITRATION' ||
    matter?.caseType === 'COMMERCIAL_ARBITRATION';

  // 诉讼阶段名称
  const stageMap: Record<string, string> = {
    FIRST_INSTANCE: '一审',
    SECOND_INSTANCE: '二审',
    RETRIAL: '再审',
    EXECUTION: '执行',
    ARBITRATION: '仲裁',
    CONSULTATION: '咨询',
    ALL_STAGES: '全程',
  };
  const stageName = matter?.litigationStage ? stageMap[matter.litigationStage] || '' : '';

  // 结果标签
  const resultLabel = isArbitration
    ? '仲裁裁决'
    : matter?.litigationStage === 'FIRST_INSTANCE'
      ? '一审结果'
      : matter?.litigationStage === 'SECOND_INSTANCE'
        ? '二审结果'
        : matter?.litigationStage === 'RETRIAL'
          ? '再审结果'
          : matter?.litigationStage === 'EXECUTION'
            ? '执行结果'
            : '案件结果';

  // 保管期限
  const periodMap: Record<string, string> = {
    PERMANENT: '永久',
    '30_YEARS': '30年',
    '20_YEARS': '20年',
    '10_YEARS': '10年',
    '5_YEARS': '5年',
    '3_YEARS': '3年',
    '1_YEAR': '1年',
  };
  const retentionPeriod = record.retentionPeriod ? periodMap[record.retentionPeriod] || '' : '';

  // 格式化日期
  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日`;
  };

  // 年份转中文
  const toChineseYear = (year: number) => {
    const cnNumbers = ['〇', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
    return String(year)
      .split('')
      .map((c) => cnNumbers[Number.parseInt(c)])
      .join('');
  };

  const year = matter?.createdAt ? new Date(matter.createdAt).getFullYear() : new Date().getFullYear();
  const yearCn = toChineseYear(year);
  const archiveNo = record.archiveNo || '    ';
  const matterNo = record.matterNo || matter?.matterNo || '';
  const archiveDate = formatDate(record.archiveDate || record.createdAt);

  // 生成HTML内容
  const html = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>卷宗封面 - ${record.archiveName || record.matterName || '档案'}</title>
  <style>
    @page {
      size: A4;
      margin: 15mm;
    }
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'SimSun', 'STSong', 'Songti SC', serif;
      color: #3d2914;
      background: #d2b48c;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .cover {
      width: 180mm;
      height: 257mm;
      margin: 0 auto;
      padding: 10mm;
      background: #d2b48c;
      border: 2px solid #5a3e1b;
    }
    .cover-inner {
      width: 100%;
      height: 100%;
      border: 1px solid #5a3e1b;
      padding: 8mm;
      display: flex;
      flex-direction: column;
    }
    .firm-name {
      text-align: center;
      font-size: 16pt;
      margin-bottom: 5mm;
      letter-spacing: 2px;
    }
    .main-title {
      text-align: center;
      font-size: 26pt;
      font-weight: bold;
      margin-bottom: 3mm;
      letter-spacing: 4px;
    }
    .sub-title {
      text-align: center;
      font-size: 14pt;
      margin-bottom: 3mm;
    }
    .year-no {
      text-align: center;
      font-size: 12pt;
      margin-bottom: 8mm;
      letter-spacing: 1px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      flex: 1;
      table-layout: fixed;
    }
    /* 四列布局：标签18% + 值32% + 标签18% + 值32% */
    col.col-label { width: 18%; }
    col.col-value { width: 32%; }
    col.col-label-small { width: 12%; }
    col.col-value-wide { width: 38%; }
    col.col-vertical { width: 8%; }
    td {
      border: 1px solid #5a3e1b;
      padding: 2mm 3mm;
      vertical-align: middle;
      line-height: 1.4;
      font-size: 11pt;
      word-break: break-all;
    }
    .label {
      text-align: center;
      background: rgba(210,180,140,0.6);
    }
    .label-small {
      text-align: center;
      background: rgba(210,180,140,0.6);
      font-size: 10pt;
    }
    .label-vertical {
      writing-mode: vertical-lr;
      text-orientation: upright;
      letter-spacing: 2px;
      font-size: 10pt;
      text-align: center;
      background: rgba(210,180,140,0.6);
    }
    .value {
      text-align: left;
      padding-left: 3mm;
      background: rgba(222,197,160,0.3);
    }
    @media print {
      body { background: white; }
      .cover { 
        background: #d2b48c !important; 
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
    }
  </style>
</head>
<body>
  <div class="cover">
    <div class="cover-inner">
      <div class="firm-name">${systemFirmName.value}</div>
      <div class="main-title">业 务 档 案 卷 宗</div>
      ${isCriminal ? '<div class="sub-title">（刑事诉讼类）</div>' : isArbitration ? '<div class="sub-title">（仲裁类）</div>' : ''}
      <div class="year-no">${yearCn}年度        字第${archiveNo}号</div>
      
      ${
        isCriminal
          ? `
      <table>
        <colgroup>
          <col style="width: 18%">
          <col style="width: 32%">
          <col style="width: 18%">
          <col style="width: 32%">
        </colgroup>
        <tr><td class="label">项目编号</td><td class="value" colspan="3">${matterNo}</td></tr>
        <tr><td class="label">被 告 人</td><td class="value" colspan="3">${matter?.opposingParty || ''}</td></tr>
        <tr><td class="label">罪    名</td><td class="value" colspan="3">${matter?.causeOfAction || ''}</td></tr>
        <tr><td class="label">委 托 人</td><td class="value">${record.clientName || ''}</td><td class="label">承办律师</td><td class="value">${record.mainLawyerName || ''}</td></tr>
        <tr><td class="label">审理法院</td><td class="value" colspan="3"></td></tr>
        <tr><td class="label">收案日期</td><td class="value">${formatDate(matter?.filingDate)}</td><td class="label">结案日期</td><td class="value">${formatDate(record.caseCloseDate)}</td></tr>
        <tr><td class="label">${resultLabel}</td><td class="value">${matter?.outcome || ''}</td><td class="label">代理阶段</td><td class="value">${stageName}</td></tr>
        <tr><td class="label">归档日期</td><td class="value">${archiveDate}</td><td class="label">保存期限</td><td class="value">${retentionPeriod}</td></tr>
        <tr><td class="label">立 卷 人</td><td class="value">${record.mainLawyerName || ''}</td><td class="label">备    注</td><td class="value">${record.remarks || ''}</td></tr>
      </table>`
          : `
      <table>
        <colgroup>
          <col style="width: 18%">
          <col style="width: 32%">
          <col style="width: 18%">
          <col style="width: 32%">
        </colgroup>
        <tr><td class="label">项目编号</td><td class="value" colspan="3">${matterNo}</td></tr>
        <tr><td class="label">案    由</td><td class="value" colspan="3">${matter?.causeOfAction || ''}</td></tr>
        <tr><td class="label">委 托 人</td><td class="value">${record.clientName || ''}</td><td class="label">承 办 人</td><td class="value">${record.mainLawyerName || ''}</td></tr>
        <tr>
          <td class="label-vertical" rowspan="3">当<br/>事<br/>人</td>
          <td class="label-small">${isArbitration ? '申请人' : '原告'}</td>
          <td class="value" colspan="2">${record.clientName || ''}</td>
        </tr>
        <tr><td class="label-small">${isArbitration ? '被申请人' : '被告'}</td><td class="value" colspan="2">${matter?.opposingParty || ''}</td></tr>
        <tr><td class="label-small">第三人</td><td class="value" colspan="2"></td></tr>
        <tr><td class="label">${isArbitration ? '仲裁机构' : '审理法院'}</td><td class="value" colspan="3"></td></tr>
        <tr><td class="label">收案日期</td><td class="value">${formatDate(matter?.filingDate)}</td><td class="label">结案日期</td><td class="value">${formatDate(record.caseCloseDate)}</td></tr>
        <tr><td class="label">${resultLabel}</td><td class="value">${matter?.outcome || ''}</td><td class="label">代理阶段</td><td class="value">${stageName}</td></tr>
        <tr><td class="label">归档日期</td><td class="value">${archiveDate}</td><td class="label">保存期限</td><td class="value">${retentionPeriod}</td></tr>
        <tr><td class="label">立 卷 人</td><td class="value">${record.mainLawyerName || ''}</td><td class="label">备    注</td><td class="value">${record.remarks || ''}</td></tr>
      </table>`
      }
    </div>
  </div>
  <scr` + `ipt>
    window.onload = function() {
      setTimeout(function() {
        window.print();
      }, 300);
    };
  </scr` + `ipt>
</bo` + `dy>
</ht` + `ml>`;

  printWindow.document.write(html);
  printWindow.document.close();
}

// 重新生成卷宗封面
async function handleRegenerateCover(record: ArchiveDTO) {
  Modal.confirm({
    title: '重新生成卷宗封面',
    content: `确定要重新生成档案 "${record.archiveName}" 的卷宗封面吗？`,
    onOk: async () => {
      try {
        await regenerateArchiveCover(record.id);
        message.success('封面重新生成成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '重新生成封面失败');
      }
    },
  });
}

// 保存入库
async function handleStoreSave() {
  try {
    if (!storeFormData.locationId) {
      message.error('请选择存放位置');
      return;
    }
    await storeArchive(storeFormData);
    message.success('入库成功');
    storeModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error.message || '入库失败');
  }
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'blue',
    PENDING_STORE: 'orange',
    STORED: 'green',
    BORROWED: 'gold',
    PENDING_MIGRATE: 'purple',
    MIGRATED: 'default',
    DESTROYED: 'red',
  };
  return colorMap[status] || 'default';
}

// 获取统计标签
function getStatLabel(key: string): string {
  const labelMap: Record<string, string> = {
    客户数: '客户',
    团队成员数: '团队成员',
    合同数: '合同',
    收费记录数: '收费记录',
    工时记录数: '工时记录',
    文档数: '文档',
    证据数: '证据',
    审批记录数: '审批',
    任务数: '任务',
  };
  return labelMap[key] || key;
}

// 从项目列表页面跳转过来时，自动打开归档向导
async function handleRouteQuery() {
  const matterId = route.query.matterId;
  if (matterId) {
    const id = Number(matterId);
    if (!isNaN(id)) {
      // 清除查询参数
      router.replace({ path: '/archive/list', query: {} });

      // 等待数据加载完成
      await loadMatters();
      await loadDataSources();

      // 自动选中项目并打开向导
      selectedMatterId.value = id;
      archiveWizardVisible.value = true;

      // 自动开始检查
      archiveWizardLoading.value = true;
      try {
        checkResult.value = await checkArchiveRequirements(id);
        archiveWizardStep.value = 1;
      } catch (error: any) {
        message.error(error.message || '检查归档条件失败');
        archiveWizardStep.value = 0;
      } finally {
        archiveWizardLoading.value = false;
      }
    }
  }
}

onMounted(async () => {
  loadSystemConfig(); // 加载系统配置
  fetchData();
  await loadMatters();
  // 处理路由参数
  handleRouteQuery();
});
</script>

<template>
  <Page title="档案列表" description="管理项目档案">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[12, 12]">
          <!-- 第一行：基础筛选 -->
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="extendedQuery.year"
              placeholder="年度"
              allow-clear
              style="width: 100%"
              :options="yearOptions"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="extendedQuery.matterType"
              placeholder="项目类型"
              allow-clear
              style="width: 100%"
              :options="MATTER_TYPE_OPTIONS"
              @change="handleMatterTypeChange"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="extendedQuery.caseType"
              placeholder="案件类型"
              allow-clear
              style="width: 100%"
              :options="caseTypeOptions"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Input
              v-model:value="queryParams.archiveName"
              placeholder="档案/项目名称"
              allow-clear
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Input
              v-model:value="queryParams.matterNo"
              placeholder="项目编号"
              allow-clear
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="24" :lg="6">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleOpenArchiveWizard">
                创建档案
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- Tab标签页 -->
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <TabPane key="all">
          <template #tab>
            <span>全部</span>
            <Badge
              :count="statusCounts.all"
              :number-style="{ backgroundColor: '#999' }"
              style="margin-left: 6px"
            />
          </template>
        </TabPane>
        <TabPane key="pending">
          <template #tab>
            <span>待入库</span>
            <Badge
              :count="statusCounts.pending"
              :number-style="{ backgroundColor: '#faad14' }"
              style="margin-left: 6px"
            />
          </template>
        </TabPane>
        <TabPane key="stored">
          <template #tab>
            <span>已入库</span>
            <Badge
              :count="statusCounts.stored"
              :number-style="{ backgroundColor: '#52c41a' }"
              style="margin-left: 6px"
            />
          </template>
        </TabPane>
        <TabPane key="borrowed">
          <template #tab>
            <span>借出中</span>
            <Badge
              :count="statusCounts.borrowed"
              :number-style="{ backgroundColor: '#1890ff' }"
              style="margin-left: 6px"
            />
          </template>
        </TabPane>
      </Tabs>

      <!-- 卡片列表 -->
      <Spin :spinning="loading">
        <div
          v-if="filteredDataSource.length === 0"
          style="padding: 60px; color: #999; text-align: center"
        >
          {{ activeTab === 'all' ? '暂无档案数据' : '暂无该状态的档案' }}
        </div>
        <Row v-else :gutter="[16, 16]">
          <Col
            v-for="archive in filteredDataSource"
            :key="archive.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            :xl="6"
          >
            <div class="archive-cover-card" @click.stop>
              <!-- 标准封面预览 -->
              <ArchiveCoverPreview
                :archive="archive"
                :matter="getMatterInfo(archive)"
                :firm-name="systemFirmName"
                :scale="0.85"
              />

              <!-- 底部操作栏（状态+操作按钮） -->
              <div class="cover-footer">
                <Tag
                  :color="getStatusColor(archive.status || '')"
                  size="small"
                >
                  {{ archive.statusName || '-' }}
                </Tag>
                <div class="cover-actions">
                  <Dropdown>
                    <template #overlay>
                      <Menu>
                        <MenuItem
                          key="print"
                          @click="handlePrintCover(archive)"
                        >
                          <Printer :size="14" style="margin-right: 8px" />
                          打印封面
                        </MenuItem>
                        <MenuItem
                          key="regenerate"
                          @click="handleRegenerateCover(archive)"
                        >
                          <RotateCw :size="14" style="margin-right: 8px" />
                          重新生成
                        </MenuItem>
                      </Menu>
                    </template>
                    <Button type="text" size="small">
                      <FileOutlined />
                    </Button>
                  </Dropdown>
                  <Button
                    v-if="archive.status === 'PENDING'"
                    type="link"
                    size="small"
                    @click="handleSubmitStoreApproval(archive)"
                  >
                    提交审批
                  </Button>
                  <Button
                    v-if="archive.status === 'PENDING_STORE'"
                    type="link"
                    size="small"
                    @click="handleApproveStore(archive)"
                  >
                    审批
                  </Button>
                </div>
              </div>
            </div>
          </Col>
        </Row>

        <!-- 分页 -->
        <div style="margin-top: 24px; text-align: right">
          <Pagination
            v-model:current="queryParams.pageNum"
            v-model:page-size="queryParams.pageSize"
            :total="total"
            :show-size-changer="true"
            :show-total="(total) => `共 ${total} 条`"
            @change="
              (page, size) => {
                queryParams.pageNum = page;
                queryParams.pageSize = size;
                fetchData();
              }
            "
          />
        </div>
      </Spin>
    </Card>

    <!-- 归档向导弹窗 -->
    <Modal
      v-model:open="archiveWizardVisible"
      title="创建档案"
      width="900px"
      :footer="null"
    >
      <Spin :spinning="archiveWizardLoading">
        <Steps :current="archiveWizardStep" style="margin-bottom: 24px">
          <Step title="选择项目" />
          <Step title="检查条件" />
          <Step title="预览数据" />
          <Step title="确认创建" />
        </Steps>

        <!-- 步骤1：选择项目 -->
        <div v-if="archiveWizardStep === 0">
          <Alert
            message="选择要归档的项目"
            description="只有已结案或已归档状态的项目才能创建档案"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <FormItem label="选择项目">
            <Select
              v-model:value="selectedMatterId as any"
              placeholder="请选择已结案的项目"
              show-search
              style="width: 100%"
              :virtual="archivableMatters.length > 50"
              :list-height="256"
              :filter-option="
                (input, option) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              :options="
                archivableMatters.map((m) => ({
                  label: `[${m.matterNo}] ${m.name}`,
                  value: m.id,
                }))
              "
            />
          </FormItem>
          <div style="margin-top: 24px; text-align: right">
            <Space>
              <Button @click="archiveWizardVisible = false">取消</Button>
              <Button
                type="primary"
                @click="handleSelectMatter"
                :disabled="!selectedMatterId"
              >
                下一步
              </Button>
            </Space>
          </div>
        </div>

        <!-- 步骤2：检查条件 -->
        <div v-if="archiveWizardStep === 1 && checkResult">
          <Alert
            v-if="checkResult.passed"
            message="检查通过"
            description="项目满足归档条件，可以继续"
            type="success"
            show-icon
            style="margin-bottom: 16px"
          />
          <Alert
            v-else
            message="检查未通过"
            description="项目不满足归档条件，请先处理以下问题"
            type="error"
            show-icon
            style="margin-bottom: 16px"
          />

          <div
            v-if="checkResult.missingItems.length > 0"
            style="margin-bottom: 16px"
          >
            <h4>缺失项：</h4>
            <ul>
              <li
                v-for="item in checkResult.missingItems"
                :key="item"
                style="color: red"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <div
            v-if="checkResult.warnings.length > 0"
            style="margin-bottom: 16px"
          >
            <h4>警告：</h4>
            <ul>
              <li
                v-for="item in checkResult.warnings"
                :key="item"
                style="color: orange"
              >
                {{ item }}
              </li>
            </ul>
          </div>

          <div style="margin-top: 24px; text-align: right">
            <Space>
              <Button @click="archiveWizardStep = 0">上一步</Button>
              <Button type="primary" @click="handlePreviewData">
                继续预览数据
              </Button>
            </Space>
          </div>
        </div>

        <!-- 步骤3：预览数据 -->
        <div v-if="archiveWizardStep === 2 && dataSnapshot">
          <Descriptions
            title="项目信息"
            :column="2"
            bordered
            size="small"
            style="margin-bottom: 16px"
          >
            <DescriptionsItem label="项目编号">
              {{ dataSnapshot.matterNo }}
            </DescriptionsItem>
            <DescriptionsItem label="项目名称">
              {{ dataSnapshot.matterName }}
            </DescriptionsItem>
          </Descriptions>

          <h4 style="margin-bottom: 12px">数据统计</h4>
          <Row :gutter="16" style="margin-bottom: 16px">
            <Col
              :span="4"
              v-for="(value, key) in dataSnapshot.statistics"
              :key="key"
            >
              <Card size="small">
                <Statistic
                  :title="getStatLabel(key as string)"
                  :value="value"
                />
              </Card>
            </Col>
          </Row>

          <h4 style="margin-bottom: 12px">选择要包含的数据</h4>
          <Checkbox.Group
            v-model:value="selectedDataSourceIds"
            style="width: 100%"
          >
            <Row>
              <Col :span="8" v-for="ds in dataSources" :key="ds.id">
                <Checkbox :value="ds.id" :disabled="ds.is_required">
                  {{ ds.source_name }}
                  <Tag v-if="ds.is_required" color="red" size="small">必填</Tag>
                </Checkbox>
              </Col>
            </Row>
          </Checkbox.Group>

          <div style="margin-top: 24px; text-align: right">
            <Space>
              <Button @click="archiveWizardStep = 1">上一步</Button>
              <Button type="primary" @click="archiveWizardStep = 3">
                下一步
              </Button>
            </Space>
          </div>
        </div>

        <!-- 步骤4：确认创建 -->
        <div v-if="archiveWizardStep === 3 && dataSnapshot">
          <Alert
            message="确认创建档案"
            description="系统将收集选中的项目数据并创建档案记录"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />

          <Descriptions title="档案信息" :column="2" bordered size="small">
            <DescriptionsItem label="档案名称">
              {{ dataSnapshot.matterName }} - 档案
            </DescriptionsItem>
            <DescriptionsItem label="项目编号">
              {{ dataSnapshot.matterNo }}
            </DescriptionsItem>
            <DescriptionsItem label="包含数据源">
              {{ selectedDataSourceIds.length }} 个
            </DescriptionsItem>
            <DescriptionsItem label="初始状态">待入库</DescriptionsItem>
          </Descriptions>

          <div style="margin-top: 24px; text-align: right">
            <Space>
              <Button @click="archiveWizardStep = 2">上一步</Button>
              <Button type="primary" @click="handleCreateArchive">
                确认创建
              </Button>
            </Space>
          </div>
        </div>
      </Spin>
    </Modal>

    <!-- 入库弹窗 -->
    <Modal
      v-model:open="storeModalVisible"
      title="档案入库"
      width="500px"
      @ok="handleStoreSave"
    >
      <Form
        :model="storeFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="档案名称">
          <Input :value="currentArchive?.archiveName" disabled />
        </FormItem>
        <FormItem
          label="存放位置"
          :rules="[{ required: true, message: '请选择存放位置' }]"
        >
          <Select
            v-model:value="storeFormData.locationId"
            placeholder="请选择库位"
          >
            <Select.Option :value="1">A区-01-001</Select.Option>
            <Select.Option :value="2">A区-01-002</Select.Option>
            <Select.Option :value="3">B区-01-001</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="档案盒编号">
          <Input
            v-model:value="storeFormData.boxNo"
            placeholder="请输入档案盒编号"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 入库审批弹窗 -->
    <Modal
      v-model:open="approveModalVisible"
      title="入库审批"
      width="500px"
      @ok="handleApproveStoreSave"
    >
      <Form
        :model="approveFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="档案名称">
          <Input :value="currentArchive?.archiveName" disabled />
        </FormItem>
        <FormItem label="项目名称">
          <Input :value="currentArchive?.matterName" disabled />
        </FormItem>
        <FormItem label="审批结果">
          <Select
            v-model:value="approveFormData.approved as any"
            style="width: 100%"
          >
            <Select.Option :value="true">通过</Select.Option>
            <Select.Option :value="false">拒绝</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="审批意见">
          <Textarea
            v-model:value="approveFormData.comment"
            :rows="3"
            placeholder="请输入审批意见"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
/* Tab样式 */
:deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

:deep(.ant-tabs-tab) {
  padding: 8px 16px;
}

:deep(.ant-badge-count) {
  box-shadow: none;
}

/* 档案封面卡片容器 */
.archive-cover-card {
  margin-bottom: 16px;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  /* 性能优化：延迟渲染不在视口中的卡片 */
  content-visibility: auto;
  contain-intrinsic-size: 0 380px;
}

.archive-cover-card:hover {
  transform: translateY(-4px);
}

/* 底部操作栏 */
.cover-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  background-color: rgb(210 180 140 / 40%);
  border-radius: 0 0 4px 4px;
}

/* 操作按钮组 */
.cover-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.cover-actions :deep(.ant-btn-text) {
  padding: 2px 6px;
  color: #5a3e1b;
}

.cover-actions :deep(.ant-btn-link) {
  padding: 0 4px;
  font-size: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .archive-cover-card {
    margin-bottom: 12px;
  }

  .cover-footer {
    flex-wrap: wrap;
    gap: 4px;
  }
}
</style>
