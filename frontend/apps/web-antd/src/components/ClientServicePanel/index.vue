<script setup lang="ts">
/**
 * 客户服务面板组件
 * 用于项目详情页的"客户服务"Tab，管理数据推送到客户服务系统
 */
import type {
  PushRecordDTO,
  PushRequest,
  ScopeOption,
} from '#/api/system/openapi';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  DescriptionsItem,
  Empty,
  Form,
  FormItem,
  InputNumber,
  message,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Switch,
  Table,
  Tag,
  Timeline,
  TimelineItem,
  Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getPushConfig,
  getPushRecords,
  getPushStatistics,
  pushMatterData,
  updatePushConfig,
} from '#/api/system/openapi';
import { getDocumentsByMatter } from '#/api/document';

const props = defineProps<{
  /** 客户ID */
  clientId: number;
  /** 客户名称 */
  clientName: string;
  /** 项目ID */
  matterId: number;
  /** 项目名称 */
  matterName: string;
  /** 是否只读 */
  readonly?: boolean;
}>();

// 授权范围选项
const SCOPE_OPTIONS: ScopeOption[] = [
  { value: 'MATTER_INFO', label: '项目基本信息', description: '项目名称、编号、类型、状态等' },
  { value: 'MATTER_PROGRESS', label: '项目进度', description: '当前阶段、整体进度、最近更新时间' },
  { value: 'LAWYER_INFO', label: '承办律师', description: '团队成员姓名、角色、联系方式（脱敏）' },
  { value: 'DEADLINE_INFO', label: '关键期限', description: '诉讼时效、举证期限、开庭时间等' },
  { value: 'TASK_LIST', label: '办理事项', description: '待办事项标题、状态、进度' },
  { value: 'DOCUMENT_LIST', label: '文书目录', description: '文档名称列表（仅标题，不含文件）' },
  { value: 'DOCUMENT_FILES', label: '文书文件', description: '推送选定的文档文件（如判决书PDF），客户可下载' },
  { value: 'FEE_INFO', label: '费用信息', description: '合同金额、已收款、待收款' },
];
const scopeOptions = ref<ScopeOption[]>(SCOPE_OPTIONS);

// 文档列表（用于选择要推送的文档）
interface DocumentItem {
  id: number;
  name: string;
  type: string;
  fileType: string;
  fileSize: number;
  uploadTime: string;
}
const documentList = ref<DocumentItem[]>([]);
const selectedDocumentIds = ref<number[]>([]);
const documentLoading = ref(false);

// 推送记录
const pushRecords = ref<PushRecordDTO[]>([]);
const loading = ref(false);

// 统计
const statistics = ref<{
  totalPushCount: number;
  lastPushTime?: string;
  lastPushStatus?: string;
}>({ totalPushCount: 0 });

// 推送表单
const pushModalVisible = ref(false);
const pushLoading = ref(false);
const pushForm = reactive<PushRequest & { documentIds?: number[] }>({
  matterId: 0,
  clientId: undefined,
  scopes: ['MATTER_INFO', 'MATTER_PROGRESS', 'LAWYER_INFO', 'DEADLINE_INFO'],
  validDays: 30,
  documentIds: [],
});

// 是否选择了文档文件推送
const hasDocumentFilesScope = computed(() => pushForm.scopes.includes('DOCUMENT_FILES'));

// 配置
const configLoading = ref(false);
const config = reactive({
  enabled: false,
  scopes: ['MATTER_INFO', 'MATTER_PROGRESS', 'LAWYER_INFO', 'DEADLINE_INFO'],
  autoPushOnUpdate: false,
  validDays: 30,
});

// 状态映射（常量使用 UPPER_SNAKE_CASE）
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待推送', color: 'processing' },
  SUCCESS: { text: '已推送', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
};

// 推送类型映射
const PUSH_TYPE_MAP: Record<string, string> = {
  MANUAL: '手动推送',
  AUTO: '自动推送',
  UPDATE: '数据更新',
};

// 表格列
const columns = [
  { title: '推送时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '推送内容', dataIndex: 'scopes', key: 'scopes' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '有效期至', dataIndex: 'expiresAt', key: 'expiresAt', width: 120 },
];

// 加载数据
async function loadData() {
  if (!props.matterId || !props.clientId) return;
  
  loading.value = true;
  try {
    // 并行加载
    const [recordsRes, statsRes, configRes] = await Promise.all([
      getPushRecords({ matterId: props.matterId, pageNum: 1, pageSize: 50 }),
      getPushStatistics(props.matterId),
      getPushConfig(props.matterId, props.clientId),
    ]);
    
    pushRecords.value = recordsRes.list || [];
    statistics.value = statsRes;
    
    if (configRes) {
      config.enabled = configRes.enabled || false;
      config.scopes = configRes.scopes || [];
      config.autoPushOnUpdate = configRes.autoPushOnUpdate || false;
      config.validDays = configRes.validDays || 30;
    }
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
}

// 客户服务系统是否已对接（从后端获取，暂时模拟为未对接）
const clientServiceConnected = ref(false);

// 加载项目文档列表
async function loadDocuments() {
  if (!props.matterId) return;
  documentLoading.value = true;
  try {
    const docs = await getDocumentsByMatter(props.matterId);
    documentList.value = (docs || []).map(d => ({
      id: d.id,
      name: d.name || d.title || '未命名文档',
      type: d.categoryName || d.fileCategory || '其他',
      fileType: d.fileType || '',
      fileSize: d.fileSize || 0,
      uploadTime: d.createdAt || d.uploadTime || '',
    }));
  } catch (error) {
    console.error('加载文档列表失败', error);
  } finally {
    documentLoading.value = false;
  }
}

// 打开推送弹窗
async function openPushModal() {
  // 检查客户服务系统是否已对接
  if (!clientServiceConnected.value) {
    Modal.info({
      title: '客户服务系统尚未对接',
      content: '客户服务系统正在开发中，暂时无法推送数据。对接完成后，您可以在此将项目信息推送给客户，系统将自动通过短信、公众号等方式通知客户查看。',
      okText: '我知道了',
    });
    return;
  }
  
  pushForm.matterId = props.matterId;
  pushForm.clientId = props.clientId;
  pushForm.scopes = [...config.scopes];
  pushForm.validDays = config.validDays;
  pushForm.documentIds = [];
  selectedDocumentIds.value = [];
  
  // 加载文档列表
  await loadDocuments();
  
  pushModalVisible.value = true;
}

// 执行推送
async function handlePush() {
  if (pushForm.scopes.length === 0) {
    message.error('请至少选择一项推送内容');
    return;
  }
  
  pushLoading.value = true;
  try {
    await pushMatterData(pushForm);
    message.success('数据已推送到客户服务系统，系统将自动通知客户');
    pushModalVisible.value = false;
    loadData();
  } catch (error: unknown) {
    const err = error as { message?: string; code?: string };
    // 检查是否是客户服务系统未配置的错误
    if (err.message?.includes('未配置') || err.message?.includes('未对接')) {
      Modal.warning({
        title: '客户服务系统尚未对接',
        content: '请先在【系统管理 → 外部系统集成 → 客户服务系统】中配置并启用客户服务系统。',
        okText: '我知道了',
      });
    } else {
      message.error(err.message || '推送失败');
    }
  } finally {
    pushLoading.value = false;
  }
}

// 保存配置
async function saveConfig() {
  configLoading.value = true;
  try {
    await updatePushConfig(props.matterId, {
      enabled: config.enabled,
      scopes: config.scopes,
      autoPushOnUpdate: config.autoPushOnUpdate,
      validDays: config.validDays,
    });
    message.success('配置已保存');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  } finally {
    configLoading.value = false;
  }
}

// 格式化时间
function formatTime(time: string | undefined) {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

function formatDate(time: string | undefined) {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD');
}

// 格式化文件大小
function formatFileSize(bytes: number): string {
  if (!bytes || bytes === 0) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let size = bytes;
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024;
    i++;
  }
  return `${size.toFixed(1)} ${units[i]}`;
}

// 获取文件图标
function getFileIcon(fileType: string | undefined): string {
  const type = (fileType || '').toLowerCase();
  const icons: Record<string, string> = {
    pdf: '📕',
    doc: '📘',
    docx: '📘',
    xls: '📗',
    xlsx: '📗',
    ppt: '📙',
    pptx: '📙',
    jpg: '🖼️',
    jpeg: '🖼️',
    png: '🖼️',
    gif: '🖼️',
    zip: '📦',
    rar: '📦',
    txt: '📄',
  };
  return icons[type] || '📄';
}

// 全选/取消全选文档（弹窗中）
function handleSelectAllDocs(e: { target: { checked: boolean } }) {
  if (e.target.checked) {
    pushForm.documentIds = documentList.value.map(d => d.id);
  } else {
    pushForm.documentIds = [];
  }
}

// 全选/取消全选文档（配置区域）
function handleSelectAllDocsConfig(e: { target: { checked: boolean } }) {
  if (e.target.checked) {
    selectedDocumentIds.value = documentList.value.map(d => d.id);
  } else {
    selectedDocumentIds.value = [];
  }
}

// 监听 config.scopes 变化，当选择 DOCUMENT_FILES 时加载文档列表
watch(() => config.scopes, (newScopes) => {
  if (newScopes.includes('DOCUMENT_FILES') && documentList.value.length === 0) {
    loadDocuments();
  }
}, { deep: true });

// 初始化
onMounted(() => {
  if (props.clientId && props.matterId) {
    loadData();
  }
});

// 监听 props 变化
watch(() => [props.clientId, props.matterId], () => {
  if (props.clientId && props.matterId) {
    loadData();
  }
});
</script>

<template>
  <div class="client-service-panel">
    <!-- 未关联客户提示 -->
    <template v-if="!clientId">
      <Empty description="该项目未关联客户，无法使用客户服务功能">
        <template #image>
          <span style="font-size: 48px">👥</span>
        </template>
      </Empty>
    </template>

    <template v-else>
      <Spin :spinning="loading">
        <!-- 功能说明 -->
        <Alert
          type="info"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #message>
            <span>
              将项目信息推送到<b>客户服务系统</b>后，系统会自动通过短信、公众号等方式通知客户 <b>{{ clientName }}</b> 查看。
            </span>
          </template>
        </Alert>

        <Row :gutter="16">
          <!-- 左侧：推送操作和配置 -->
          <Col :span="14">
            <!-- 推送配置 -->
            <Card title="推送设置" size="small" style="margin-bottom: 16px">
              <Form layout="vertical">
                <FormItem label="默认推送内容">
                  <Checkbox.Group v-model:value="config.scopes" :disabled="readonly">
                    <Row :gutter="[0, 8]">
                      <Col v-for="opt in scopeOptions" :key="opt.value" :span="12">
                        <Tooltip :title="opt.description">
                          <Checkbox :value="opt.value">{{ opt.label }}</Checkbox>
                        </Tooltip>
                      </Col>
                    </Row>
                  </Checkbox.Group>
                </FormItem>

                <!-- 文档选择（当配置中选择了 DOCUMENT_FILES 时显示） -->
                <FormItem v-if="config.scopes.includes('DOCUMENT_FILES')" label="选择要推送的文档">
                  <Spin :spinning="documentLoading">
                    <div v-if="documentList.length === 0" style="color: #999; padding: 16px; text-align: center; background: #fafafa; border-radius: 4px">
                      <div style="font-size: 32px; margin-bottom: 8px">📁</div>
                      <div>该项目暂无可推送的文档</div>
                      <div style="font-size: 12px; margin-top: 4px">请先在项目中上传文档</div>
                      <Button size="small" style="margin-top: 8px" @click="loadDocuments">刷新</Button>
                    </div>
                    <div v-else>
                      <!-- 全选操作 -->
                      <div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0">
                        <Checkbox 
                          :checked="(selectedDocumentIds.length || 0) === documentList.length"
                          :indeterminate="selectedDocumentIds.length > 0 && selectedDocumentIds.length < documentList.length"
                          :disabled="readonly"
                          @change="handleSelectAllDocsConfig"
                        >
                          全选 ({{ documentList.length }} 个文档)
                        </Checkbox>
                      </div>
                      <!-- 文档列表 -->
                      <Checkbox.Group v-model:value="selectedDocumentIds" :disabled="readonly" style="width: 100%">
                        <div style="max-height: 200px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 4px">
                          <div 
                            v-for="doc in documentList" 
                            :key="doc.id"
                            class="doc-item"
                          >
                            <Checkbox :value="doc.id" style="width: 100%">
                              <div class="doc-item-content">
                                <span class="doc-icon">{{ getFileIcon(doc.fileType) }}</span>
                                <div class="doc-info">
                                  <div class="doc-name">{{ doc.name }}</div>
                                  <div class="doc-meta">
                                    <Tag size="small" color="blue">{{ doc.fileType?.toUpperCase() || '文件' }}</Tag>
                                    <span v-if="doc.fileSize">{{ formatFileSize(doc.fileSize) }}</span>
                                  </div>
                                </div>
                              </div>
                            </Checkbox>
                          </div>
                        </div>
                      </Checkbox.Group>
                      <div style="margin-top: 8px; color: #1890ff; font-size: 13px">
                        ✓ 已选择 <b>{{ selectedDocumentIds.length }}</b> 个文档
                      </div>
                    </div>
                  </Spin>
                </FormItem>
                
                <Row :gutter="16">
                  <Col :span="12">
                    <FormItem label="数据有效期">
                      <Space>
                        <InputNumber
                          v-model:value="config.validDays"
                          :min="1"
                          :max="365"
                          :disabled="readonly"
                          style="width: 100px"
                        />
                        <span>天</span>
                      </Space>
                    </FormItem>
                  </Col>
                  <Col :span="12">
                    <FormItem label="自动推送">
                      <Switch
                        v-model:checked="config.autoPushOnUpdate"
                        :disabled="readonly"
                        checked-children="开"
                        un-checked-children="关"
                      />
                      <span style="margin-left: 8px; color: #999; font-size: 12px">
                        项目更新时自动推送
                      </span>
                    </FormItem>
                  </Col>
                </Row>
                
                <FormItem v-if="!readonly">
                  <Space>
                    <Button type="primary" @click="openPushModal">
                      📤 推送到客户服务系统
                    </Button>
                    <Button :loading="configLoading" @click="saveConfig">
                      保存配置
                    </Button>
                  </Space>
                </FormItem>
              </Form>
            </Card>

            <!-- 推送历史 -->
            <Card title="推送记录" size="small">
              <template #extra>
                <Button size="small" @click="loadData">刷新</Button>
              </template>
              
              <Table
                v-if="pushRecords.length > 0"
                :columns="columns"
                :data-source="pushRecords"
                :pagination="{ pageSize: 5, size: 'small' }"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record: rawRecord }">
                  <template v-if="column.key === 'createdAt'">
                    <div>{{ formatTime((rawRecord as PushRecordDTO).createdAt) }}</div>
                    <div style="font-size: 12px; color: #999">
                      {{ PUSH_TYPE_MAP[(rawRecord as PushRecordDTO).pushType] || (rawRecord as PushRecordDTO).pushType }}
                    </div>
                  </template>
                  <template v-else-if="column.key === 'scopes'">
                    <Space size="small" wrap>
                      <Tag
                        v-for="scope in (rawRecord as PushRecordDTO).scopes.slice(0, 3)"
                        :key="scope"
                        size="small"
                      >
                        {{ scopeOptions.find(o => o.value === scope)?.label || scope }}
                      </Tag>
                      <Tag v-if="(rawRecord as PushRecordDTO).scopes.length > 3" size="small">
                        +{{ (rawRecord as PushRecordDTO).scopes.length - 3 }}
                      </Tag>
                    </Space>
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <Tag :color="STATUS_MAP[(rawRecord as PushRecordDTO).status]?.color">
                      {{ STATUS_MAP[(rawRecord as PushRecordDTO).status]?.text }}
                    </Tag>
                    <Tooltip v-if="(rawRecord as PushRecordDTO).errorMessage" :title="(rawRecord as PushRecordDTO).errorMessage">
                      <span style="color: #ff4d4f; cursor: help">❓</span>
                    </Tooltip>
                  </template>
                  <template v-else-if="column.key === 'expiresAt'">
                    {{ formatDate((rawRecord as PushRecordDTO).expiresAt) }}
                  </template>
                </template>
              </Table>
              
              <Empty v-else description="暂无推送记录" />
            </Card>
          </Col>

          <!-- 右侧：统计和状态 -->
          <Col :span="10">
            <Card title="推送统计" size="small" style="margin-bottom: 16px">
              <Row :gutter="16">
                <Col :span="12">
                  <Statistic
                    title="累计推送"
                    :value="statistics.totalPushCount"
                    suffix="次"
                  />
                </Col>
                <Col :span="12">
                  <div class="stat-item">
                    <div class="stat-title">最近推送</div>
                    <div class="stat-value">
                      {{ statistics.lastPushTime ? formatTime(statistics.lastPushTime) : '暂无' }}
                    </div>
                  </div>
                </Col>
              </Row>
            </Card>

            <!-- 说明 -->
            <Card title="功能说明" size="small">
              <Timeline>
                <TimelineItem color="blue">
                  <b>1. 选择推送内容</b>
                  <p style="color: #666; margin: 4px 0 0">选择要同步给客户的项目信息</p>
                </TimelineItem>
                <TimelineItem color="blue">
                  <b>2. 点击推送</b>
                  <p style="color: #666; margin: 4px 0 0">数据将发送到客户服务系统</p>
                </TimelineItem>
                <TimelineItem color="green">
                  <b>3. 客户收到通知</b>
                  <p style="color: #666; margin: 4px 0 0">系统自动通过短信/公众号通知客户</p>
                </TimelineItem>
                <TimelineItem color="green">
                  <b>4. 客户查看</b>
                  <p style="color: #666; margin: 4px 0 0">客户点击链接查看项目信息</p>
                </TimelineItem>
              </Timeline>
            </Card>
          </Col>
        </Row>
      </Spin>

      <!-- 推送确认弹窗 -->
      <Modal
        v-model:open="pushModalVisible"
        title="确认推送"
        :width="hasDocumentFilesScope ? 720 : 560"
        @ok="handlePush"
        :confirm-loading="pushLoading"
        ok-text="确认推送"
      >
        <Alert
          type="warning"
          show-icon
          style="margin-bottom: 16px"
          message="推送后，客户服务系统将自动通知客户"
          description="请确认推送内容，客户将通过短信、公众号等方式收到通知并查看项目信息。"
        />
        
        <Descriptions :column="1" bordered size="small">
          <DescriptionsItem label="客户">{{ clientName }}</DescriptionsItem>
          <DescriptionsItem label="项目">{{ matterName }}</DescriptionsItem>
          <DescriptionsItem label="推送内容">
            <Checkbox.Group v-model:value="pushForm.scopes">
              <Row :gutter="[0, 8]">
                <Col v-for="opt in SCOPE_OPTIONS" :key="opt.value" :span="12">
                  <Tooltip :title="opt.description">
                    <Checkbox :value="opt.value">{{ opt.label }}</Checkbox>
                  </Tooltip>
                </Col>
              </Row>
            </Checkbox.Group>
          </DescriptionsItem>
          
          <!-- 文档选择（当选择了 DOCUMENT_FILES 时显示） -->
          <DescriptionsItem v-if="hasDocumentFilesScope" label="选择文档">
            <Spin :spinning="documentLoading">
              <div v-if="documentList.length === 0" style="color: #999; padding: 16px; text-align: center; background: #fafafa; border-radius: 4px">
                <div style="font-size: 32px; margin-bottom: 8px">📁</div>
                <div>该项目暂无可推送的文档</div>
                <div style="font-size: 12px; margin-top: 4px">请先在项目中上传文档</div>
              </div>
              <div v-else>
                <!-- 全选操作 -->
                <div style="margin-bottom: 8px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0">
                  <Checkbox 
                    :checked="(pushForm.documentIds?.length || 0) === documentList.length"
                    :indeterminate="(pushForm.documentIds?.length || 0) > 0 && (pushForm.documentIds?.length || 0) < documentList.length"
                    @change="handleSelectAllDocs"
                  >
                    全选 ({{ documentList.length }} 个文档)
                  </Checkbox>
                </div>
                <!-- 文档列表 -->
                <Checkbox.Group v-model:value="pushForm.documentIds" style="width: 100%">
                  <div style="max-height: 240px; overflow-y: auto; border: 1px solid #f0f0f0; border-radius: 4px">
                    <div 
                      v-for="doc in documentList" 
                      :key="doc.id"
                      class="doc-item"
                    >
                      <Checkbox :value="doc.id" style="width: 100%">
                        <div class="doc-item-content">
                          <span class="doc-icon">{{ getFileIcon(doc.fileType) }}</span>
                          <div class="doc-info">
                            <div class="doc-name">{{ doc.name }}</div>
                            <div class="doc-meta">
                              <Tag size="small" color="blue">{{ doc.fileType?.toUpperCase() || '文件' }}</Tag>
                              <span v-if="doc.fileSize">{{ formatFileSize(doc.fileSize) }}</span>
                            </div>
                          </div>
                        </div>
                      </Checkbox>
                    </div>
                  </div>
                </Checkbox.Group>
                <div style="margin-top: 8px; color: #1890ff; font-size: 13px">
                  ✓ 已选择 <b>{{ pushForm.documentIds?.length || 0 }}</b> 个文档，客户可在有效期内下载
                </div>
              </div>
            </Spin>
          </DescriptionsItem>
          
          <DescriptionsItem label="有效期">
            <Space>
              <InputNumber
                v-model:value="pushForm.validDays"
                :min="1"
                :max="365"
                style="width: 100px"
              />
              <span>天</span>
              <span style="color: #999; font-size: 12px">（超过有效期，客户服务系统自动删除数据）</span>
            </Space>
          </DescriptionsItem>
        </Descriptions>
      </Modal>
    </template>
  </div>
</template>

<style scoped>
.client-service-panel {
  padding: 0;
}

.stat-item {
  text-align: center;
}

.stat-title {
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  color: rgba(0, 0, 0, 0.85);
  font-size: 14px;
}

:deep(.ant-statistic-title) {
  font-size: 14px;
}

:deep(.ant-statistic-content) {
  font-size: 24px;
}

:deep(.ant-timeline-item-content) {
  padding-bottom: 12px;
}

:deep(.ant-timeline-item-content p) {
  font-size: 12px;
}

/* 文档选择器样式 */
.doc-item {
  padding: 10px 12px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.doc-item:last-child {
  border-bottom: none;
}

.doc-item:hover {
  background-color: #f5f7fa;
}

.doc-item-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doc-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

:deep(.doc-item .ant-checkbox-wrapper) {
  width: 100%;
}
</style>
