<script setup lang="ts">
import type {
  CreateQualityCheckStandardCommand,
  CreateQualityIssueCommand,
  CreateRiskWarningCommand,
  QualityCheckDTO,
  QualityCheckStandardDTO,
  QualityIssueDTO,
  RiskWarningDTO,
} from '#/api/knowledge/quality';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Badge,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Table,
  Tabs,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  acknowledgeWarning,
  CHECK_RESULT_OPTIONS,
  closeWarning,
  createIssue,
  createStandard,
  createWarning,
  deleteStandard,
  getActiveWarnings,
  getEnabledStandards,
  getHighRiskWarnings,
  getInProgressChecks,
  getIssueById,
  getPendingIssues,
  getWarningById,
  ISSUE_SEVERITY_OPTIONS,
  ISSUE_STATUS_OPTIONS,
  ISSUE_TYPE_OPTIONS,
  resolveWarning,
  RISK_LEVEL_OPTIONS,
  RISK_TYPE_OPTIONS,
  STANDARD_CATEGORY_OPTIONS,
  updateIssueStatus,
  updateStandard,
  WARNING_STATUS_OPTIONS,
} from '#/api/knowledge/quality';

defineOptions({ name: 'QualityManagement' });

// 响应式布局
const { isMobile } = useResponsive();

// 当前Tab
const activeTab = ref('overview');

// 加载状态
const loading = ref(false);

// 概览数据
const overviewData = reactive({
  inProgressChecks: 0,
  pendingIssues: 0,
  activeWarnings: 0,
  highRiskWarnings: 0,
});

// 检查标准列表
const standardList = ref<QualityCheckStandardDTO[]>([]);

// 问题列表
const issueList = ref<QualityIssueDTO[]>([]);

// 预警列表
const warningList = ref<RiskWarningDTO[]>([]);

// 进行中的检查
const checkList = ref<QualityCheckDTO[]>([]);

// ==================== 标准管理 ====================
const standardModalVisible = ref(false);
const standardForm = reactive<
  CreateQualityCheckStandardCommand & { id?: number }
>({
  standardName: '',
  category: '',
  description: '',
  checkPoints: '',
  maxScore: 100,
  weight: 1,
});
const isEditStandard = ref(false);
const standardLoading = ref(false);

// ==================== 问题管理 ====================
const issueDetailVisible = ref(false);
const issueDetailData = ref<QualityIssueDTO | null>(null);
const issueDetailLoading = ref(false);

// ==================== 预警管理 ====================
const warningDetailVisible = ref(false);
const warningDetailData = ref<RiskWarningDTO | null>(null);
const warningDetailLoading = ref(false);

// 标准表格列
const standardColumns = [
  {
    title: '标准名称',
    dataIndex: 'standardName',
    key: 'standardName',
    ellipsis: true,
  },
  { title: '分类', dataIndex: 'category', key: 'category', width: 120 },
  { title: '最高分', dataIndex: 'maxScore', key: 'maxScore', width: 80 },
  { title: '权重', dataIndex: 'weight', key: 'weight', width: 80 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 150 },
];

// 问题表格列
const issueColumns = [
  { title: '问题编号', dataIndex: 'issueNo', key: 'issueNo', width: 130 },
  { title: '项目', dataIndex: 'matterName', key: 'matterName', ellipsis: true },
  { title: '类型', dataIndex: 'issueType', key: 'issueType', width: 100 },
  { title: '严重程度', dataIndex: 'severity', key: 'severity', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline', width: 120 },
  { title: '操作', key: 'action', width: 150 },
];

// 预警表格列
const warningColumns = [
  { title: '预警编号', dataIndex: 'warningNo', key: 'warningNo', width: 130 },
  { title: '项目', dataIndex: 'matterName', key: 'matterName', ellipsis: true },
  { title: '风险类型', dataIndex: 'riskType', key: 'riskType', width: 100 },
  { title: '风险等级', dataIndex: 'riskLevel', key: 'riskLevel', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 180 },
];

// 加载概览数据
async function loadOverview() {
  try {
    const [checks, issues, warnings, highRisk] = await Promise.all([
      getInProgressChecks(),
      getPendingIssues(),
      getActiveWarnings(),
      getHighRiskWarnings(),
    ]);
    overviewData.inProgressChecks = checks?.length || 0;
    overviewData.pendingIssues = issues?.length || 0;
    overviewData.activeWarnings = warnings?.length || 0;
    overviewData.highRiskWarnings = highRisk?.length || 0;

    checkList.value = checks || [];
    issueList.value = issues || [];
    warningList.value = warnings || [];
  } catch (error: any) {
    console.error('加载概览数据失败', error);
  }
}

// 加载标准列表
async function loadStandards() {
  loading.value = true;
  try {
    const res = await getEnabledStandards();
    standardList.value = res || [];
  } catch (error: any) {
    message.error(`加载标准失败：${error.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
}

// 打开新建标准弹窗
function handleCreateStandard() {
  isEditStandard.value = false;
  Object.assign(standardForm, {
    id: undefined,
    standardName: '',
    category: '',
    description: '',
    checkPoints: '',
    maxScore: 100,
    weight: 1,
  });
  standardModalVisible.value = true;
}

// 打开编辑标准弹窗
function handleEditStandard(record: QualityCheckStandardDTO) {
  isEditStandard.value = true;
  Object.assign(standardForm, {
    id: record.id,
    standardName: record.standardName,
    category: record.category,
    description: record.description,
    checkPoints: record.checkPoints,
    maxScore: record.maxScore,
    weight: record.weight,
  });
  standardModalVisible.value = true;
}

// 提交标准
async function handleStandardSubmit() {
  if (!standardForm.standardName?.trim()) {
    message.warning('请输入标准名称');
    return;
  }
  if (!standardForm.category) {
    message.warning('请选择分类');
    return;
  }

  standardLoading.value = true;
  try {
    const data: CreateQualityCheckStandardCommand = {
      standardName: standardForm.standardName,
      category: standardForm.category,
      description: standardForm.description,
      checkPoints: standardForm.checkPoints,
      maxScore: standardForm.maxScore,
      weight: standardForm.weight,
    };

    if (isEditStandard.value && standardForm.id) {
      await updateStandard(standardForm.id, data);
      message.success('更新成功');
    } else {
      await createStandard(data);
      message.success('创建成功');
    }
    standardModalVisible.value = false;
    loadStandards();
  } catch (error: any) {
    message.error(`操作失败：${error.message || '未知错误'}`);
  } finally {
    standardLoading.value = false;
  }
}

// 删除标准
async function handleDeleteStandard(record: QualityCheckStandardDTO) {
  try {
    await deleteStandard(record.id);
    message.success('删除成功');
    loadStandards();
  } catch (error: any) {
    message.error(`删除失败：${error.message || '未知错误'}`);
  }
}

// 查看问题详情
async function handleViewIssue(record: QualityIssueDTO) {
  issueDetailLoading.value = true;
  issueDetailVisible.value = true;
  try {
    const res = await getIssueById(record.id);
    issueDetailData.value = res;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  } finally {
    issueDetailLoading.value = false;
  }
}

// 处理问题
async function handleResolveIssue(record: QualityIssueDTO) {
  Modal.confirm({
    title: '确认解决问题',
    content: '确定将此问题标记为已解决？',
    onOk: async () => {
      try {
        await updateIssueStatus(record.id, 'RESOLVED', '已处理');
        message.success('操作成功');
        loadOverview();
      } catch (error: any) {
        message.error(`操作失败：${error.message || '未知错误'}`);
      }
    },
  });
}

// 查看预警详情
async function handleViewWarning(record: RiskWarningDTO) {
  warningDetailLoading.value = true;
  warningDetailVisible.value = true;
  try {
    const res = await getWarningById(record.id);
    warningDetailData.value = res;
  } catch (error: any) {
    message.error(`加载详情失败：${error.message || '未知错误'}`);
  } finally {
    warningDetailLoading.value = false;
  }
}

// 确认预警
async function handleAcknowledgeWarning(record: RiskWarningDTO) {
  try {
    await acknowledgeWarning(record.id);
    message.success('确认成功');
    loadOverview();
  } catch (error: any) {
    message.error(`操作失败：${error.message || '未知错误'}`);
  }
}

// 解决预警
async function handleResolveWarning(record: RiskWarningDTO) {
  try {
    await resolveWarning(record.id);
    message.success('解决成功');
    loadOverview();
  } catch (error: any) {
    message.error(`操作失败：${error.message || '未知错误'}`);
  }
}

// 关闭预警
async function handleCloseWarning(record: RiskWarningDTO) {
  try {
    await closeWarning(record.id);
    message.success('关闭成功');
    loadOverview();
  } catch (error: any) {
    message.error(`操作失败：${error.message || '未知错误'}`);
  }
}

// 获取分类名称
function getCategoryName(category: string) {
  const opt = STANDARD_CATEGORY_OPTIONS.find((o) => o.value === category);
  return opt?.label || category;
}

// 获取问题类型名称
function getIssueTypeName(type: string) {
  const opt = ISSUE_TYPE_OPTIONS.find((o) => o.value === type);
  return opt?.label || type;
}

// 获取严重程度配置
function getSeverityConfig(severity: string) {
  const opt = ISSUE_SEVERITY_OPTIONS.find((o) => o.value === severity);
  return opt || { label: severity, color: 'default' };
}

// 获取问题状态配置
function getIssueStatusConfig(status: string) {
  const opt = ISSUE_STATUS_OPTIONS.find((o) => o.value === status);
  return opt || { label: status, color: 'default' };
}

// 获取风险类型名称
function getRiskTypeName(type: string) {
  const opt = RISK_TYPE_OPTIONS.find((o) => o.value === type);
  return opt?.label || type;
}

// 获取风险等级配置
function getRiskLevelConfig(level: string) {
  const opt = RISK_LEVEL_OPTIONS.find((o) => o.value === level);
  return opt || { label: level, color: 'default' };
}

// 获取预警状态配置
function getWarningStatusConfig(status: string) {
  const opt = WARNING_STATUS_OPTIONS.find((o) => o.value === status);
  return opt || { label: status, color: 'default' };
}

// 格式化日期
function formatDate(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD');
}

// 格式化时间
function formatDateTime(date: string | null | undefined) {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
}

// Tab切换
function handleTabChange(key: string) {
  activeTab.value = key;
  if (key === 'standards') {
    loadStandards();
  }
}

// 初始化
onMounted(() => {
  loadOverview();
});
</script>

<template>
  <Page title="质量管理" description="项目质量检查、问题整改和风险预警管理">
    <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <!-- 概览 -->
      <Tabs.TabPane key="overview" tab="概览">
        <Row :gutter="[16, 16]" style="margin-bottom: 24px">
          <Col :xs="12" :sm="12" :md="6" :lg="6">
            <Card>
              <Statistic
                title="进行中的检查"
                :value="overviewData.inProgressChecks"
                :value-style="{ color: '#1890ff' }"
              />
            </Card>
          </Col>
          <Col :xs="12" :sm="12" :md="6" :lg="6">
            <Card>
              <Statistic
                title="待处理问题"
                :value="overviewData.pendingIssues"
                :value-style="{ color: '#faad14' }"
              />
            </Card>
          </Col>
          <Col :xs="12" :sm="12" :md="6" :lg="6">
            <Card>
              <Statistic
                title="活跃预警"
                :value="overviewData.activeWarnings"
                :value-style="{ color: '#ff4d4f' }"
              />
            </Card>
          </Col>
          <Col :xs="12" :sm="12" :md="6" :lg="6">
            <Card>
              <Statistic
                title="高风险预警"
                :value="overviewData.highRiskWarnings"
                :value-style="{ color: '#cf1322' }"
              />
            </Card>
          </Col>
        </Row>

        <!-- 待处理问题 -->
        <Card title="待处理问题" style="margin-bottom: 16px">
          <Table
            :columns="issueColumns"
            :data-source="issueList"
            :pagination="false"
            row-key="id"
            size="small"
            :scroll="{ x: isMobile ? 700 : undefined }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'issueType'">
                {{ record.issueTypeName || getIssueTypeName(record.issueType) }}
              </template>
              <template v-else-if="column.key === 'severity'">
                <Tag :color="getSeverityConfig(record.severity).color">
                  {{
                    record.severityName ||
                    getSeverityConfig(record.severity).label
                  }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="getIssueStatusConfig(record.status).color">
                  {{
                    record.statusName ||
                    getIssueStatusConfig(record.status).label
                  }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'deadline'">
                {{ formatDate(record.deadline) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button
                    type="link"
                    size="small"
                    @click="handleViewIssue(record)"
                    >详情</Button
                  >
                  <Button
                    v-if="record.status !== 'RESOLVED'"
                    type="link"
                    size="small"
                    @click="handleResolveIssue(record)"
                  >
                    解决
                  </Button>
                </Space>
              </template>
            </template>
          </Table>
        </Card>

        <!-- 活跃预警 -->
        <Card title="活跃预警">
          <Table
            :columns="warningColumns"
            :data-source="warningList"
            :pagination="false"
            row-key="id"
            size="small"
            :scroll="{ x: isMobile ? 800 : undefined }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'riskType'">
                {{ record.riskTypeName || getRiskTypeName(record.riskType) }}
              </template>
              <template v-else-if="column.key === 'riskLevel'">
                <Tag :color="getRiskLevelConfig(record.riskLevel).color">
                  {{
                    record.riskLevelName ||
                    getRiskLevelConfig(record.riskLevel).label
                  }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="getWarningStatusConfig(record.status).color">
                  {{
                    record.statusName ||
                    getWarningStatusConfig(record.status).label
                  }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'createdAt'">
                {{ formatDateTime(record.createdAt) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button
                    type="link"
                    size="small"
                    @click="handleViewWarning(record)"
                    >详情</Button
                  >
                  <Button
                    v-if="record.status === 'PENDING'"
                    type="link"
                    size="small"
                    @click="handleAcknowledgeWarning(record)"
                  >
                    确认
                  </Button>
                  <Button
                    v-if="record.status === 'ACKNOWLEDGED'"
                    type="link"
                    size="small"
                    @click="handleResolveWarning(record)"
                  >
                    解决
                  </Button>
                  <Button
                    v-if="record.status !== 'CLOSED'"
                    type="link"
                    size="small"
                    danger
                    @click="handleCloseWarning(record)"
                  >
                    关闭
                  </Button>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </Tabs.TabPane>

      <!-- 检查标准 -->
      <Tabs.TabPane key="standards" tab="检查标准">
        <Card>
          <template #extra>
            <Button type="primary" @click="handleCreateStandard"
              >新建标准</Button
            >
          </template>

          <Spin :spinning="loading">
            <Table
              :columns="standardColumns"
              :data-source="standardList"
              :pagination="false"
              row-key="id"
              :scroll="{ x: isMobile ? 600 : undefined }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'category'">
                  {{ record.categoryName || getCategoryName(record.category) }}
                </template>
                <template v-else-if="column.key === 'enabled'">
                  <Badge
                    :status="record.enabled ? 'success' : 'default'"
                    :text="record.enabled ? '启用' : '停用'"
                  />
                </template>
                <template v-else-if="column.key === 'action'">
                  <Space>
                    <Button
                      type="link"
                      size="small"
                      @click="handleEditStandard(record)"
                      >编辑</Button
                    >
                    <Popconfirm
                      title="确定删除此标准？"
                      @confirm="handleDeleteStandard(record)"
                    >
                      <Button type="link" size="small" danger>删除</Button>
                    </Popconfirm>
                  </Space>
                </template>
              </template>
            </Table>
          </Spin>
        </Card>
      </Tabs.TabPane>
    </Tabs>

    <!-- 标准编辑弹窗 -->
    <Modal
      v-model:open="standardModalVisible"
      :title="isEditStandard ? '编辑检查标准' : '新建检查标准'"
      :confirm-loading="standardLoading"
      :width="isMobile ? '100%' : '600px'"
      :centered="isMobile"
      @ok="handleStandardSubmit"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <Form.Item label="标准名称" required>
          <Input
            v-model:value="standardForm.standardName"
            placeholder="请输入标准名称"
          />
        </Form.Item>
        <Form.Item label="分类" required>
          <Select
            v-model:value="standardForm.category"
            placeholder="请选择分类"
            :options="STANDARD_CATEGORY_OPTIONS"
          />
        </Form.Item>
        <Form.Item label="最高分">
          <InputNumber
            v-model:value="standardForm.maxScore"
            :min="1"
            :max="100"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="权重">
          <InputNumber
            v-model:value="standardForm.weight"
            :min="0.1"
            :max="10"
            :step="0.1"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="检查要点">
          <Input.TextArea
            v-model:value="standardForm.checkPoints"
            :rows="3"
            placeholder="请输入检查要点"
          />
        </Form.Item>
        <Form.Item label="说明">
          <Input.TextArea
            v-model:value="standardForm.description"
            :rows="2"
            placeholder="请输入说明"
          />
        </Form.Item>
      </Form>
    </Modal>

    <!-- 问题详情弹窗 -->
    <Modal
      v-model:open="issueDetailVisible"
      title="问题详情"
      :footer="null"
      :width="isMobile ? '100%' : '650px'"
      :centered="isMobile"
    >
      <Spin :spinning="issueDetailLoading">
        <template v-if="issueDetailData">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="问题编号">{{
              issueDetailData.issueNo
            }}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag :color="getIssueStatusConfig(issueDetailData.status).color">
                {{
                  issueDetailData.statusName ||
                  getIssueStatusConfig(issueDetailData.status).label
                }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="项目">{{
              issueDetailData.matterName
            }}</Descriptions.Item>
            <Descriptions.Item label="检查编号">{{
              issueDetailData.checkNo || '-'
            }}</Descriptions.Item>
            <Descriptions.Item label="问题类型">
              {{
                issueDetailData.issueTypeName ||
                getIssueTypeName(issueDetailData.issueType)
              }}
            </Descriptions.Item>
            <Descriptions.Item label="严重程度">
              <Tag :color="getSeverityConfig(issueDetailData.severity).color">
                {{
                  issueDetailData.severityName ||
                  getSeverityConfig(issueDetailData.severity).label
                }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="责任人">{{
              issueDetailData.responsible || '-'
            }}</Descriptions.Item>
            <Descriptions.Item label="截止日期">{{
              formatDate(issueDetailData.deadline)
            }}</Descriptions.Item>
            <Descriptions.Item label="问题描述" :span="2">{{
              issueDetailData.description
            }}</Descriptions.Item>
            <Descriptions.Item label="处理结果" :span="2">{{
              issueDetailData.resolution || '-'
            }}</Descriptions.Item>
          </Descriptions>
        </template>
      </Spin>
    </Modal>

    <!-- 预警详情弹窗 -->
    <Modal
      v-model:open="warningDetailVisible"
      title="预警详情"
      :footer="null"
      :width="isMobile ? '100%' : '650px'"
      :centered="isMobile"
    >
      <Spin :spinning="warningDetailLoading">
        <template v-if="warningDetailData">
          <Descriptions :column="2" bordered size="small">
            <Descriptions.Item label="预警编号">{{
              warningDetailData.warningNo
            }}</Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag
                :color="getWarningStatusConfig(warningDetailData.status).color"
              >
                {{
                  warningDetailData.statusName ||
                  getWarningStatusConfig(warningDetailData.status).label
                }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="项目">{{
              warningDetailData.matterName
            }}</Descriptions.Item>
            <Descriptions.Item label="风险类型">
              {{
                warningDetailData.riskTypeName ||
                getRiskTypeName(warningDetailData.riskType)
              }}
            </Descriptions.Item>
            <Descriptions.Item label="风险等级" :span="2">
              <Tag
                :color="getRiskLevelConfig(warningDetailData.riskLevel).color"
              >
                {{
                  warningDetailData.riskLevelName ||
                  getRiskLevelConfig(warningDetailData.riskLevel).label
                }}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="风险描述" :span="2">{{
              warningDetailData.description
            }}</Descriptions.Item>
            <Descriptions.Item label="建议措施" :span="2">{{
              warningDetailData.suggestedAction || '-'
            }}</Descriptions.Item>
            <Descriptions.Item label="确认人">{{
              warningDetailData.acknowledgedByName || '-'
            }}</Descriptions.Item>
            <Descriptions.Item label="确认时间">{{
              formatDateTime(warningDetailData.acknowledgedAt)
            }}</Descriptions.Item>
            <Descriptions.Item label="解决人">{{
              warningDetailData.resolvedByName || '-'
            }}</Descriptions.Item>
            <Descriptions.Item label="解决时间">{{
              formatDateTime(warningDetailData.resolvedAt)
            }}</Descriptions.Item>
            <Descriptions.Item label="解决方案" :span="2">{{
              warningDetailData.resolution || '-'
            }}</Descriptions.Item>
          </Descriptions>
        </template>
      </Spin>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-descriptions-item-label) {
  width: 100px;
  background-color: #fafafa;
}
</style>
