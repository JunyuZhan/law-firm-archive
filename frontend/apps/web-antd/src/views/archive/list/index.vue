<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Row,
  Col,
  Tag,
  Textarea,
  Descriptions,
  DescriptionsItem,
  Alert,
  Statistic,
  Checkbox,
  Spin,
  Steps,
  Step,
  Dropdown,
  Menu,
  MenuItem,
  Pagination,
} from 'ant-design-vue';
import { FileOutlined, DownloadOutlined, RotateCw } from '@vben/icons';
import {
  getArchiveList,
  createArchive,
  storeArchive,
  submitStoreApproval,
  approveStore,
  checkArchiveRequirements,
  previewArchiveData,
  getArchiveDataSources,
  downloadArchiveCover,
  regenerateArchiveCover,
} from '#/api/archive';
import { getMatterSelectOptions, changeMatterStatus, getMatterDetail } from '#/api/matter';
import type { ArchiveDTO, ArchiveQuery, CreateArchiveCommand, StoreArchiveCommand, ArchiveCheckResult, ArchiveDataSnapshot, ArchiveDataSource } from '#/api/archive/types';
import type { MatterDTO } from '#/api/matter/types';
import { CASE_CATEGORY_OPTIONS } from '#/constants/causes/utils';

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
const matters = ref<MatterDTO[]>([]);
const matterDetailsMap = ref<Map<number, MatterDTO>>(new Map());

// 归档向导状态
const archiveWizardVisible = ref(false);
const archiveWizardStep = ref(0);
const archiveWizardLoading = ref(false);
const selectedMatterId = ref<number | null>(null);
const checkResult = ref<ArchiveCheckResult | null>(null);
const dataSnapshot = ref<ArchiveDataSnapshot | null>(null);
const dataSources = ref<ArchiveDataSource[]>([]);
const selectedDataSourceIds = ref<number[]>([]);

// 查询参数
const queryParams = reactive<ArchiveQuery>({
  pageNum: 1,
  pageSize: 10,
  archiveNo: undefined,
  archiveName: undefined,
  matterId: undefined,
  archiveType: undefined,
  status: undefined,
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

// 档案类型选项
const archiveTypeOptions = [
  { label: '诉讼', value: 'LITIGATION' },
  { label: '非诉', value: 'NON_LITIGATION' },
  { label: '咨询', value: 'CONSULTATION' },
];

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
  return matters.value.filter(m => m.status === 'CLOSED' || m.status === 'ARCHIVED');
});

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
    .filter(a => a.matterId)
    .map(a => a.matterId!)
    .filter((id, index, arr) => arr.indexOf(id) === index); // 去重
  
  for (const matterId of matterIds) {
    if (!matterDetailsMap.value.has(matterId)) {
      try {
        const matter = await getMatterDetail(matterId);
        matterDetailsMap.value.set(matterId, matter);
      } catch (error: any) {
        console.error(`加载项目详情失败: matterId=${matterId}`, error);
      }
    }
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

// 加载数据源配置
async function loadDataSources() {
  try {
    dataSources.value = await getArchiveDataSources();
    // 默认选中所有启用的数据源
    selectedDataSourceIds.value = dataSources.value
      .filter(ds => ds.is_enabled)
      .map(ds => ds.id);
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
  fetchData();
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
      archiveName: dataSnapshot.value.matterName + ' - 档案',
      archiveType: 'LITIGATION',
      retentionPeriod: '10_YEARS',
      selectedDataSourceIds: selectedDataSourceIds.value,
    };
    
    await createArchive(command);
    
    // 更新项目状态为已归档
    try {
      await changeMatterStatus(selectedMatterId.value, 'ARCHIVED');
    } catch (e) {
      console.warn('更新项目状态失败，项目可能已是归档状态', e);
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
      await approveStore(currentArchive.value.id, approveFormData.approved, approveFormData.comment);
      message.success(approveFormData.approved ? '审批通过' : '已拒绝');
      approveModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 下载卷宗封面
async function handleDownloadCover(record: ArchiveDTO) {
  try {
    const response = await downloadArchiveCover(record.id);
    // 创建下载链接
    const blob = new Blob([response as any], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const fileName = (record.archiveName || record.matterName || '档案') + '_卷宗封面.pdf';
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    message.success('封面下载成功');
  } catch (error: any) {
    message.error(error.message || '下载封面失败');
  }
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

// 获取项目封面主题（根据项目类型）
function getArchiveCoverTheme(archive: ArchiveDTO) {
  const matter = archive.matterId ? matterDetailsMap.value.get(archive.matterId) : null;
  const matterType = matter?.matterType || archive.archiveType || 'LITIGATION';
  const caseType = matter?.caseType || '';
  
  // 根据项目大类设置主题
  if (matterType === 'LITIGATION') {
    const caseTypeOption = CASE_CATEGORY_OPTIONS.find(opt => opt.value === caseType);
    const label = caseTypeOption?.label || '诉讼案件';
    
    switch (caseType) {
      case 'CRIMINAL':
        return { label, color: '#d32f2f' };
      case 'CIVIL':
        return { label, color: '#1976d2' };
      case 'ADMINISTRATIVE':
        return { label, color: '#388e3c' };
      case 'BANKRUPTCY':
        return { label, color: '#f57c00' };
      case 'IP':
        return { label, color: '#7b1fa2' };
      case 'ARBITRATION':
      case 'COMMERCIAL_ARBITRATION':
      case 'LABOR_ARBITRATION':
        return { label, color: '#0288d1' };
      case 'ENFORCEMENT':
        return { label, color: '#5d4037' };
      default:
        return { label, color: '#616161' };
    }
  } else if (matterType === 'NON_LITIGATION') {
    const caseTypeOption = CASE_CATEGORY_OPTIONS.find(opt => opt.value === caseType);
    const label = caseTypeOption?.label || '非诉项目';
    
    switch (caseType) {
      case 'LEGAL_COUNSEL':
        return { label, color: '#00796b' };
      case 'SPECIAL_SERVICE':
        return { label, color: '#e64a19' };
      default:
        return { label, color: '#455a64' };
    }
  }
  
  return { label: '业务档案卷宗', color: '#757575' };
}

// 格式化日期
function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return '-';
  return dateStr.substring(0, 10);
}

// 获取项目信息
function getMatterInfo(archive: ArchiveDTO) {
  return archive.matterId ? matterDetailsMap.value.get(archive.matterId) : null;
}

// 获取统计标签
function getStatLabel(key: string): string {
  const labelMap: Record<string, string> = {
    '客户数': '客户',
    '团队成员数': '团队成员',
    '合同数': '合同',
    '收费记录数': '收费记录',
    '工时记录数': '工时记录',
    '文档数': '文档',
    '证据数': '证据',
    '审批记录数': '审批',
    '任务数': '任务',
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
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Input
              v-model:value="queryParams.archiveName"
              placeholder="档案名称"
              allowClear
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Input
              v-model:value="queryParams.matterNo"
              placeholder="项目编号"
              allowClear
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <Select
              v-model:value="queryParams.archiveType"
              placeholder="档案类型"
              allowClear
              style="width: 100%"
              :options="archiveTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="4">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="24" :lg="6">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleOpenArchiveWizard">创建档案</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 卡片列表 -->
      <Spin :spinning="loading">
        <div v-if="dataSource.length === 0" style="text-align: center; padding: 60px; color: #999">
          暂无档案数据
        </div>
        <Row v-else :gutter="[16, 16]">
          <Col
            v-for="archive in dataSource"
            :key="archive.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            :xl="4"
          >
            <div class="archive-cover-card" @click.stop>
              <!-- 牛皮纸封面 -->
              <div 
                class="cover-paper"
                :style="{
                  borderColor: getArchiveCoverTheme(archive).color,
                  borderWidth: '3px',
                }"
              >
                <!-- 颜色标识条 -->
                <div 
                  class="cover-color-bar"
                  :style="{ backgroundColor: getArchiveCoverTheme(archive).color }"
                ></div>
                
                <!-- 封面标题区域 -->
                <div class="cover-header">
                  <div class="cover-title-main">业务档案卷宗</div>
                  <div class="cover-title-sub">{{ getArchiveCoverTheme(archive).label }}</div>
                </div>
                
                <!-- 封面信息区域 -->
                <div class="cover-info">
                  <div class="cover-info-row">
                    <span class="cover-info-label">项目名称</span>
                    <span class="cover-info-value">{{ archive.matterName || archive.archiveName || '-' }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">项目编号</span>
                    <span class="cover-info-value">{{ archive.matterNo || '-' }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">档案编号</span>
                    <span class="cover-info-value">{{ archive.archiveNo || '-' }}</span>
                  </div>
                  <div class="cover-info-row" v-if="getMatterInfo(archive)">
                    <span class="cover-info-label">案件类型</span>
                    <span class="cover-info-value">{{ getMatterInfo(archive)?.caseTypeName || '-' }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">客户</span>
                    <span class="cover-info-value">{{ archive.clientName || '-' }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">主办律师</span>
                    <span class="cover-info-value">{{ archive.mainLawyerName || '-' }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">结案日期</span>
                    <span class="cover-info-value">{{ formatDate(archive.caseCloseDate) }}</span>
                  </div>
                  <div class="cover-info-row">
                    <span class="cover-info-label">归档日期</span>
                    <span class="cover-info-value">{{ formatDate(archive.archiveDate) }}</span>
                  </div>
                </div>
                
                <!-- 封面底部 -->
                <div class="cover-footer">
                  <Tag :color="getStatusColor(archive.status || '')" style="margin: 0">
                    {{ archive.statusName || '-' }}
                  </Tag>
                </div>
              </div>
              
              <!-- 卡片操作栏 -->
              <div class="cover-actions">
                <Space>
                  <Dropdown>
                    <template #overlay>
                      <Menu>
                        <MenuItem key="download" @click="handleDownloadCover(archive)">
                      <DownloadOutlined style="margin-right: 8px;" />
                      下载封面
                    </MenuItem>
                        <MenuItem key="regenerate" @click="handleRegenerateCover(archive)">
                      <RotateCw :size="14" style="margin-right: 8px;" />
                      重新生成
                    </MenuItem>
                      </Menu>
                    </template>
                    <Button type="link" size="small">
                      <FileOutlined style="margin-right: 4px;" />
                      封面
                    </Button>
                  </Dropdown>
                  <template v-if="archive.status === 'PENDING'">
                    <Button type="link" size="small" @click="handleSubmitStoreApproval(archive)">
                      提交审批
                    </Button>
                  </template>
                  <template v-if="archive.status === 'PENDING_STORE'">
                    <Button type="link" size="small" @click="handleApproveStore(archive)">
                      审批
                    </Button>
                  </template>
                </Space>
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
            @change="(page, size) => {
              queryParams.pageNum = page;
              queryParams.pageSize = size;
              fetchData();
            }"
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
            showSearch
            style="width: 100%"
            :filterOption="(input, option) => (option?.label || '').toLowerCase().includes(input.toLowerCase())"
              :options="archivableMatters.map(m => ({ label: `[${m.matterNo}] ${m.name}`, value: m.id }))"
          />
        </FormItem>
          <div style="text-align: right; margin-top: 24px">
            <Space>
              <Button @click="archiveWizardVisible = false">取消</Button>
              <Button type="primary" @click="handleSelectMatter" :disabled="!selectedMatterId">
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

          <div v-if="checkResult.missingItems.length > 0" style="margin-bottom: 16px">
            <h4>缺失项：</h4>
            <ul>
              <li v-for="item in checkResult.missingItems" :key="item" style="color: red">
                {{ item }}
              </li>
            </ul>
          </div>

          <div v-if="checkResult.warnings.length > 0" style="margin-bottom: 16px">
            <h4>警告：</h4>
            <ul>
              <li v-for="item in checkResult.warnings" :key="item" style="color: orange">
                {{ item }}
              </li>
            </ul>
          </div>

          <div style="text-align: right; margin-top: 24px">
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
          <Descriptions title="项目信息" :column="2" bordered size="small" style="margin-bottom: 16px">
            <DescriptionsItem label="项目编号">{{ dataSnapshot.matterNo }}</DescriptionsItem>
            <DescriptionsItem label="项目名称">{{ dataSnapshot.matterName }}</DescriptionsItem>
          </Descriptions>

          <h4 style="margin-bottom: 12px">数据统计</h4>
          <Row :gutter="16" style="margin-bottom: 16px">
            <Col :span="4" v-for="(value, key) in dataSnapshot.statistics" :key="key">
              <Card size="small">
                <Statistic :title="getStatLabel(key as string)" :value="value" />
              </Card>
            </Col>
          </Row>

          <h4 style="margin-bottom: 12px">选择要包含的数据</h4>
          <Checkbox.Group v-model:value="selectedDataSourceIds" style="width: 100%">
            <Row>
              <Col :span="8" v-for="ds in dataSources" :key="ds.id">
                <Checkbox :value="ds.id" :disabled="ds.is_required">
                  {{ ds.source_name }}
                  <Tag v-if="ds.is_required" color="red" size="small">必填</Tag>
                </Checkbox>
              </Col>
            </Row>
          </Checkbox.Group>

          <div style="text-align: right; margin-top: 24px">
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
            <DescriptionsItem label="档案名称">{{ dataSnapshot.matterName }} - 档案</DescriptionsItem>
            <DescriptionsItem label="项目编号">{{ dataSnapshot.matterNo }}</DescriptionsItem>
            <DescriptionsItem label="包含数据源">{{ selectedDataSourceIds.length }} 个</DescriptionsItem>
            <DescriptionsItem label="初始状态">待入库</DescriptionsItem>
          </Descriptions>

          <div style="text-align: right; margin-top: 24px">
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
        <FormItem label="存放位置" :rules="[{ required: true, message: '请选择存放位置' }]">
          <Select v-model:value="storeFormData.locationId" placeholder="请选择库位">
            <Select.Option :value="1">A区-01-001</Select.Option>
            <Select.Option :value="2">A区-01-002</Select.Option>
            <Select.Option :value="3">B区-01-001</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="档案盒编号">
          <Input v-model:value="storeFormData.boxNo" placeholder="请输入档案盒编号" />
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
          <Select v-model:value="approveFormData.approved as any" style="width: 100%">
            <Select.Option :value="true">通过</Select.Option>
            <Select.Option :value="false">拒绝</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="审批意见">
          <Textarea v-model:value="approveFormData.comment" :rows="3" placeholder="请输入审批意见" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
/* 档案封面卡片容器 */
.archive-cover-card {
  margin-bottom: 16px;
}

/* 牛皮纸封面 */
.cover-paper {
  /* A4比例：宽高比约1:1.414 */
  aspect-ratio: 1 / 1.414;
  width: 100%;
  min-height: 400px;
  background-color: #deb887; /* 牛皮纸颜色 RGB(222, 184, 135) */
  border: 3px solid; /* 边框颜色由内联样式动态设置 */
  border-radius: 4px;
  padding: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  overflow: hidden;
}

/* 颜色标识条 */
.cover-color-bar {
  height: 6px;
  width: 100%;
  flex-shrink: 0;
}

.cover-paper:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* 封面标题区域 */
.cover-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px 20px 20px 20px;
  padding-bottom: 20px;
  border-bottom: 2px solid #8b5a2b;
}

.cover-title-main {
  font-size: 28px;
  font-weight: bold;
  color: #654321; /* 深棕色文字 RGB(101, 67, 33) */
  margin-bottom: 10px;
  letter-spacing: 2px;
}

.cover-title-sub {
  font-size: 18px;
  color: #654321; /* 深棕色文字 RGB(101, 67, 33) */
  letter-spacing: 1px;
}

/* 封面信息区域 */
.cover-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0 20px;
}

.cover-info-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid rgba(139, 90, 43, 0.2);
}

.cover-info-label {
  flex: 0 0 80px;
  font-size: 13px;
  font-weight: bold;
  color: #654321;
  text-align: right;
  padding-right: 12px;
}

.cover-info-value {
  flex: 1;
  font-size: 13px;
  color: #654321;
  word-break: break-all;
}

/* 封面底部 */
.cover-footer {
  margin-top: 20px;
  padding: 15px 20px 20px 20px;
  border-top: 2px solid #8b5a2b;
  text-align: center;
}

/* 卡片操作栏 */
.cover-actions {
  margin-top: 12px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
  text-align: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .cover-paper {
    min-height: 300px;
    padding: 15px;
  }
  
  .cover-title-main {
    font-size: 22px;
  }
  
  .cover-title-sub {
    font-size: 16px;
  }
  
  .cover-info-label {
    flex: 0 0 70px;
    font-size: 12px;
  }
  
  .cover-info-value {
    font-size: 12px;
  }
}
</style>
