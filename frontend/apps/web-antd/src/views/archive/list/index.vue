<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
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
  Collapse,
  CollapsePanel,
  Checkbox,
  Spin,
  Steps,
  Step,
} from 'ant-design-vue';
import {
  getArchiveList,
  createArchive,
  storeArchive,
  submitStoreApproval,
  approveStore,
  checkArchiveRequirements,
  previewArchiveData,
  getArchiveDataSources,
} from '#/api/archive';
import { getMatterList, changeMatterStatus } from '#/api/matter';
import type { ArchiveDTO, ArchiveQuery, CreateArchiveCommand, StoreArchiveCommand, ArchiveCheckResult, ArchiveDataSnapshot, ArchiveDataSource } from '#/api/archive/types';
import type { MatterDTO } from '#/api/matter/types';

defineOptions({ name: 'ArchiveList' });

const route = useRoute();
const router = useRouter();

// 状态
const loading = ref(false);
const dataSource = ref<ArchiveDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const modalTitle = ref('新建档案');
const formRef = ref();
const storeModalVisible = ref(false);
const approveModalVisible = ref(false);
const currentArchive = ref<ArchiveDTO | null>(null);
const matters = ref<MatterDTO[]>([]);

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

// 表单数据
const formData = reactive<Partial<CreateArchiveCommand> & { id?: number }>({
  id: undefined,
  archiveName: '',
  matterId: undefined,
  archiveType: 'LITIGATION',
  retentionPeriod: '10_YEARS',
  remarks: '',
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

// 表格列
const columns = [
  { title: '档案编号', dataIndex: 'archiveNo', key: 'archiveNo', width: 130 },
  { title: '档案名称', dataIndex: 'archiveName', key: 'archiveName', width: 200 },
  { title: '项目编号', dataIndex: 'matterNo', key: 'matterNo', width: 130 },
  { title: '项目名称', dataIndex: 'matterName', key: 'matterName', width: 150 },
  { title: '客户', dataIndex: 'clientName', key: 'clientName', width: 120 },
  { title: '主办律师', dataIndex: 'mainLawyerName', key: 'mainLawyerName', width: 100 },
  { title: '结案日期', dataIndex: 'caseCloseDate', key: 'caseCloseDate', width: 110 },
  { title: '保管期限', dataIndex: 'retentionPeriodName', key: 'retentionPeriodName', width: 100 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 档案类型选项
const archiveTypeOptions = [
  { label: '诉讼', value: 'LITIGATION' },
  { label: '非诉', value: 'NON_LITIGATION' },
  { label: '咨询', value: 'CONSULTATION' },
];

// 保管期限选项
const retentionPeriodOptions = [
  { label: '永久', value: 'PERMANENT' },
  { label: '30年', value: '30_YEARS' },
  { label: '15年', value: '15_YEARS' },
  { label: '10年', value: '10_YEARS' },
  { label: '5年', value: '5_YEARS' },
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
  } catch (error: any) {
    message.error(error.message || '加载档案列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载项目列表
async function loadMatters() {
  try {
    const res = await getMatterList({ pageNum: 1, pageSize: 1000 });
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

// 入库操作
function handleStore(record: ArchiveDTO) {
  currentArchive.value = record;
  storeFormData.archiveId = record.id;
  storeFormData.locationId = 0;
  storeFormData.boxNo = '';
  storeModalVisible.value = true;
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

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => {
            queryParams.pageNum = page;
            queryParams.pageSize = size;
            fetchData();
          },
        }"
        row-key="id"
        :scroll="{ x: 1500 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as ArchiveDTO).status || '')">
              {{ (record as ArchiveDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <!-- 待入库：可提交入库审批 -->
              <template v-if="(record as ArchiveDTO).status === 'PENDING'">
                <a @click="handleSubmitStoreApproval(record as ArchiveDTO)">提交审批</a>
              </template>
              <!-- 待入库审批：可审批 -->
              <template v-if="(record as ArchiveDTO).status === 'PENDING_STORE'">
                <a @click="handleApproveStore(record as ArchiveDTO)">审批</a>
              </template>
              <!-- 审批通过后：可入库 -->
              <template v-if="(record as ArchiveDTO).status === 'PENDING' && false">
                <a @click="handleStore(record as ArchiveDTO)">入库</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
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
              v-model:value="selectedMatterId"
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
          <Select v-model:value="approveFormData.approved" style="width: 100%">
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
