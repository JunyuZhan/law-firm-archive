<script setup lang="ts">
/**
 * 客户服务面板组件
 * 用于项目详情页的"客户服务"Tab，管理数据推送到客户服务系统
 */
import type {
  ClientFileDTO,
  ClientFileSyncRequest,
  PushRecordDTO,
  PushRequest,
  ScopeOption,
} from '#/api/matter/client-service';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import {
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  DescriptionsItem,
  Divider,
  Empty,
  Form,
  FormItem,
  InputNumber,
  List,
  ListItem,
  ListItemMeta,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Spin,
  Statistic,
  Switch,
  Tag,
  Tooltip,
  TreeSelect,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  countPendingClientFiles,
  getPendingClientFiles,
  getPushConfig,
  getPushRecords,
  getPushStatistics,
  ignoreClientFile,
  pushMatterData,
  syncClientFile,
  updatePushConfig,
} from '#/api/matter/client-service';
import { getDocumentsByMatter } from '#/api/document';
import { getMatterDossierItems } from '#/api/document/dossier';

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
  {
    value: 'MATTER_INFO',
    label: '项目基本信息',
    description: '项目名称、编号、类型、状态等',
  },
  {
    value: 'MATTER_PROGRESS',
    label: '项目进度',
    description: '当前阶段、整体进度、最近更新时间',
  },
  {
    value: 'LAWYER_INFO',
    label: '承办律师',
    description: '团队成员姓名、角色、联系方式（脱敏）',
  },
  {
    value: 'DEADLINE_INFO',
    label: '关键期限',
    description: '诉讼时效、举证期限、开庭时间等',
  },
  {
    value: 'TASK_LIST',
    label: '办理事项',
    description: '待办事项标题、状态、进度',
  },
  {
    value: 'DOCUMENT_LIST',
    label: '文书目录',
    description: '文档名称列表（仅标题，不含文件）',
  },
  {
    value: 'DOCUMENT_FILES',
    label: '文书文件',
    description: '推送选定的文档文件（如判决书PDF），客户可下载',
  },
  {
    value: 'FEE_INFO',
    label: '费用信息',
    description: '合同金额、已收款、待收款',
  },
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

// ========== 客户上传文件 ==========
const clientFiles = ref<ClientFileDTO[]>([]);
const clientFilesLoading = ref(false);
const pendingFileCount = ref(0);

// 同步弹窗
const syncModalVisible = ref(false);
const syncLoading = ref(false);
const syncForm = reactive<ClientFileSyncRequest>({
  fileId: 0,
  targetDossierId: 0,
  targetFileName: '',
  documentCategory: '',
});
const currentSyncFile = ref<ClientFileDTO | null>(null);

// 卷宗目录树
const dossierTree = ref<any[]>([]);
const dossierLoading = ref(false);

// 文件类别映射
const FILE_CATEGORY_MAP: Record<string, { text: string; color: string }> = {
  EVIDENCE: { text: '证据材料', color: 'blue' },
  CONTRACT: { text: '合同文件', color: 'green' },
  ID_CARD: { text: '身份证件', color: 'orange' },
  OTHER: { text: '其他', color: 'default' },
};

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
const hasDocumentFilesScope = computed(() =>
  pushForm.scopes.includes('DOCUMENT_FILES'),
);

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

// ========== 客户文件相关函数 ==========

// 加载客户上传的文件
async function loadClientFiles() {
  if (!props.matterId) return;

  clientFilesLoading.value = true;
  try {
    const [files, countRes] = await Promise.all([
      getPendingClientFiles(props.matterId),
      countPendingClientFiles(props.matterId),
    ]);
    clientFiles.value = files || [];
    pendingFileCount.value = countRes?.count || 0;
  } catch (error) {
    console.error('加载客户文件失败', error);
  } finally {
    clientFilesLoading.value = false;
  }
}

// 加载卷宗目录树
async function loadDossierTree() {
  if (!props.matterId) return;

  dossierLoading.value = true;
  try {
    const items = await getMatterDossierItems(props.matterId);
    // 将扁平数据转换为树形结构
    dossierTree.value = buildTreeData(items || []);
  } catch (error) {
    console.error('加载卷宗目录失败', error);
  } finally {
    dossierLoading.value = false;
  }
}

// 将扁平数据构建为 TreeSelect 树形结构
function buildTreeData(items: any[]): any[] {
  const map = new Map<number, any>();
  const roots: any[] = [];

  // 只包含文件夹类型的目录项
  const folders = items.filter((item) => item.itemType === 'FOLDER');

  // 创建节点映射
  folders.forEach((item) => {
    map.set(item.id, {
      value: item.id,
      title: item.name,
      children: [],
    });
  });

  // 构建树
  folders.forEach((item) => {
    const node = map.get(item.id);
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId).children.push(node);
    } else {
      roots.push(node);
    }
  });

  // 移除空的 children 数组
  function cleanEmpty(nodes: any[]) {
    nodes.forEach((node) => {
      if (node.children && node.children.length === 0) {
        delete node.children;
      } else if (node.children) {
        cleanEmpty(node.children);
      }
    });
  }
  cleanEmpty(roots);

  return roots;
}

// 打开同步弹窗
async function openSyncModal(file: ClientFileDTO) {
  currentSyncFile.value = file;
  syncForm.fileId = file.id;
  syncForm.targetDossierId = 0;
  syncForm.targetFileName = file.originalFileName || file.fileName;
  syncForm.documentCategory = file.fileCategory || '';

  // 加载卷宗目录
  if (dossierTree.value.length === 0) {
    await loadDossierTree();
  }

  syncModalVisible.value = true;
}

// 执行同步
async function handleSync() {
  if (!syncForm.targetDossierId) {
    message.warning('请选择目标卷宗目录');
    return;
  }

  syncLoading.value = true;
  try {
    await syncClientFile(syncForm);
    message.success('文件已同步到卷宗');
    syncModalVisible.value = false;
    loadClientFiles();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '同步失败');
  } finally {
    syncLoading.value = false;
  }
}

// 忽略文件
async function handleIgnoreFile(file: ClientFileDTO) {
  try {
    await ignoreClientFile(file.id);
    message.success('已忽略该文件');
    loadClientFiles();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

// 预览/下载文件
function handlePreviewFile(file: ClientFileDTO) {
  if (file.externalFileUrl) {
    window.open(file.externalFileUrl, '_blank');
  } else {
    message.warning('文件链接不可用');
  }
}

// 加载项目文档列表
async function loadDocuments() {
  if (!props.matterId) return;
  documentLoading.value = true;
  try {
    const docs = await getDocumentsByMatter(props.matterId);
    documentList.value = (docs || []).map((d) => ({
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
      content:
        '客户服务系统正在开发中，暂时无法推送数据。对接完成后，您可以在此将项目信息推送给客户，系统将自动通过短信、公众号等方式通知客户查看。',
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
        content:
          '请先在【系统管理 → 外部系统集成 → 客户服务系统】中配置并启用客户服务系统。',
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
    pushForm.documentIds = documentList.value.map((d) => d.id);
  } else {
    pushForm.documentIds = [];
  }
}

// 监听 config.scopes 变化，当选择 DOCUMENT_FILES 时加载文档列表
watch(
  () => config.scopes,
  (newScopes) => {
    if (
      newScopes.includes('DOCUMENT_FILES') &&
      documentList.value.length === 0
    ) {
      loadDocuments();
    }
  },
  { deep: true },
);

// 初始化
onMounted(() => {
  if (props.clientId && props.matterId) {
    loadData();
    loadClientFiles();
  }
});

// 监听 props 变化
watch(
  () => [props.clientId, props.matterId],
  () => {
    if (props.clientId && props.matterId) {
      loadData();
      loadClientFiles();
    }
  },
);
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
        <Alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            <span>
              将项目信息推送到<b>客户服务系统</b>后，系统会自动通过短信、公众号等方式通知客户
              <b>{{ clientName }}</b> 查看。
            </span>
          </template>
        </Alert>

        <Row :gutter="16">
          <!-- 左侧：客户上传文件（主要功能区） -->
          <Col :span="14">
            <!-- 客户上传的文件 - 突出显示 -->
            <Card size="small" class="client-files-card">
              <template #title>
                <Space>
                  <span style="font-size: 16px; font-weight: 600"
                    >📥 客户上传的文件</span
                  >
                  <Badge
                    v-if="pendingFileCount > 0"
                    :count="pendingFileCount"
                    :overflow-count="99"
                  />
                </Space>
              </template>
              <template #extra>
                <Button size="small" @click="loadClientFiles">刷新</Button>
              </template>

              <Spin :spinning="clientFilesLoading">
                <div
                  v-if="clientFiles.length === 0"
                  class="empty-files-placeholder"
                >
                  <div style="margin-bottom: 16px; font-size: 64px">📥</div>
                  <div style="font-size: 16px; font-weight: 500; color: #666">
                    暂无客户上传的文件
                  </div>
                  <div style="margin-top: 8px; font-size: 13px; color: #999">
                    客户通过客服系统上传的证据材料、合同文件等会显示在这里
                  </div>
                  <div
                    style="
                      padding: 12px;
                      margin-top: 16px;
                      text-align: left;
                      background: #f0f7ff;
                      border-radius: 6px;
                    "
                  >
                    <div
                      style="
                        margin-bottom: 8px;
                        font-size: 13px;
                        font-weight: 500;
                        color: #1890ff;
                      "
                    >
                      💡 文件接收流程
                    </div>
                    <ol
                      style="
                        padding-left: 20px;
                        margin: 0;
                        font-size: 12px;
                        line-height: 1.8;
                        color: #666;
                      "
                    >
                      <li>客户通过客服小程序/公众号上传文件</li>
                      <li>系统自动将文件推送到此处</li>
                      <li>您可以预览、同步到卷宗或忽略</li>
                    </ol>
                  </div>
                </div>

                <List v-else :data-source="clientFiles" size="small">
                  <template #renderItem="{ item }">
                    <ListItem class="file-list-item">
                      <ListItemMeta>
                        <template #avatar>
                          <div class="file-avatar">
                            <span style="font-size: 32px">{{
                              getFileIcon(item.fileType)
                            }}</span>
                          </div>
                        </template>
                        <template #title>
                          <Space>
                            <span style="font-weight: 500">{{
                              item.fileName
                            }}</span>
                            <Tag
                              :color="
                                FILE_CATEGORY_MAP[item.fileCategory]?.color ||
                                'default'
                              "
                              size="small"
                            >
                              {{
                                FILE_CATEGORY_MAP[item.fileCategory]?.text ||
                                '其他'
                              }}
                            </Tag>
                          </Space>
                        </template>
                        <template #description>
                          <Space size="small" wrap>
                            <span
                              >上传人:
                              {{
                                item.uploadedBy || item.clientName || '-'
                              }}</span
                            >
                            <Divider type="vertical" />
                            <span>{{ formatTime(item.uploadedAt) }}</span>
                            <span v-if="item.fileSize"
                              >· {{ formatFileSize(item.fileSize) }}</span
                            >
                          </Space>
                        </template>
                      </ListItemMeta>
                      <template #actions>
                        <Button
                          type="link"
                          size="small"
                          @click="handlePreviewFile(item)"
                        >
                          预览
                        </Button>
                        <Button
                          v-if="item.status === 'PENDING' && !readonly"
                          type="primary"
                          size="small"
                          @click="openSyncModal(item)"
                        >
                          同步到卷宗
                        </Button>
                        <Popconfirm
                          v-if="item.status === 'PENDING' && !readonly"
                          title="确定忽略此文件？忽略后客服系统将删除该文件。"
                          @confirm="handleIgnoreFile(item)"
                        >
                          <Button type="link" size="small" danger>忽略</Button>
                        </Popconfirm>
                        <Tag
                          v-if="item.status === 'SYNCED'"
                          color="success"
                          size="small"
                          >已同步</Tag
                        >
                      </template>
                    </ListItem>
                  </template>
                </List>
              </Spin>
            </Card>
          </Col>

          <!-- 右侧：推送设置和统计 -->
          <Col :span="10">
            <!-- 推送统计 - 紧凑显示 -->
            <Card size="small" style="margin-bottom: 12px">
              <Row :gutter="16" align="middle">
                <Col :span="8">
                  <Statistic
                    title="累计推送"
                    :value="statistics.totalPushCount"
                    suffix="次"
                    :value-style="{ fontSize: '20px' }"
                  />
                </Col>
                <Col :span="8">
                  <div class="stat-item">
                    <div class="stat-title">最近推送</div>
                    <div class="stat-value" style="font-size: 13px">
                      {{
                        statistics.lastPushTime
                          ? formatTime(statistics.lastPushTime)
                          : '暂无'
                      }}
                    </div>
                  </div>
                </Col>
                <Col :span="8" style="text-align: right">
                  <Button
                    type="primary"
                    size="small"
                    @click="openPushModal"
                    :disabled="readonly"
                  >
                    📤 推送信息
                  </Button>
                </Col>
              </Row>
            </Card>

            <!-- 推送设置 - 精简折叠 -->
            <Card size="small" style="margin-bottom: 12px">
              <template #title>
                <span style="font-size: 13px">推送设置</span>
              </template>
              <template #extra>
                <Button
                  type="link"
                  size="small"
                  :loading="configLoading"
                  @click="saveConfig"
                  >保存</Button
                >
              </template>

              <Form layout="vertical" size="small">
                <FormItem label="推送内容" style="margin-bottom: 8px">
                  <Checkbox.Group
                    v-model:value="config.scopes"
                    :disabled="readonly"
                  >
                    <Row :gutter="[0, 4]">
                      <Col
                        v-for="opt in scopeOptions"
                        :key="opt.value"
                        :span="12"
                      >
                        <Tooltip :title="opt.description">
                          <Checkbox
                            :value="opt.value"
                            style="font-size: 12px"
                            >{{ opt.label }}</Checkbox
                          >
                        </Tooltip>
                      </Col>
                    </Row>
                  </Checkbox.Group>
                </FormItem>

                <Row :gutter="8">
                  <Col :span="12">
                    <FormItem label="有效期" style="margin-bottom: 0">
                      <Space size="small">
                        <InputNumber
                          v-model:value="config.validDays"
                          :min="1"
                          :max="365"
                          :disabled="readonly"
                          size="small"
                          style="width: 60px"
                        />
                        <span style="font-size: 12px">天</span>
                      </Space>
                    </FormItem>
                  </Col>
                  <Col :span="12">
                    <FormItem label="自动推送" style="margin-bottom: 0">
                      <Switch
                        v-model:checked="config.autoPushOnUpdate"
                        :disabled="readonly"
                        size="small"
                      />
                    </FormItem>
                  </Col>
                </Row>
              </Form>
            </Card>

            <!-- 推送记录 - 精简显示 -->
            <Card size="small">
              <template #title>
                <span style="font-size: 13px">推送记录</span>
              </template>
              <template #extra>
                <Button type="link" size="small" @click="loadData">刷新</Button>
              </template>

              <div
                v-if="pushRecords.length > 0"
                style="max-height: 200px; overflow-y: auto"
              >
                <div
                  v-for="record in pushRecords.slice(0, 5)"
                  :key="record.id"
                  class="push-record-item"
                >
                  <div
                    style="
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                    "
                  >
                    <span style="font-size: 12px; color: #666">{{
                      formatTime(record.createdAt)
                    }}</span>
                    <Tag :color="STATUS_MAP[record.status]?.color" size="small">
                      {{ STATUS_MAP[record.status]?.text }}
                    </Tag>
                  </div>
                  <div style="margin-top: 4px">
                    <Tag
                      v-for="scope in record.scopes.slice(0, 2)"
                      :key="scope"
                      size="small"
                      style="font-size: 11px"
                    >
                      {{
                        scopeOptions.find((o) => o.value === scope)?.label ||
                        scope
                      }}
                    </Tag>
                    <span
                      v-if="record.scopes.length > 2"
                      style="font-size: 11px; color: #999"
                    >
                      +{{ record.scopes.length - 2 }}
                    </span>
                  </div>
                </div>
              </div>

              <Empty
                v-else
                description="暂无推送记录"
                :image="Empty.PRESENTED_IMAGE_SIMPLE"
              />
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
              <div
                v-if="documentList.length === 0"
                style="
                  padding: 16px;
                  color: #999;
                  text-align: center;
                  background: #fafafa;
                  border-radius: 4px;
                "
              >
                <div style="margin-bottom: 8px; font-size: 32px">📁</div>
                <div>该项目暂无可推送的文档</div>
                <div style="margin-top: 4px; font-size: 12px">
                  请先在项目中上传文档
                </div>
              </div>
              <div v-else>
                <!-- 全选操作 -->
                <div
                  style="
                    padding-bottom: 8px;
                    margin-bottom: 8px;
                    border-bottom: 1px solid #f0f0f0;
                  "
                >
                  <Checkbox
                    :checked="
                      (pushForm.documentIds?.length || 0) ===
                      documentList.length
                    "
                    :indeterminate="
                      (pushForm.documentIds?.length || 0) > 0 &&
                      (pushForm.documentIds?.length || 0) < documentList.length
                    "
                    @change="handleSelectAllDocs"
                  >
                    全选 ({{ documentList.length }} 个文档)
                  </Checkbox>
                </div>
                <!-- 文档列表 -->
                <Checkbox.Group
                  v-model:value="pushForm.documentIds"
                  style="width: 100%"
                >
                  <div
                    style="
                      max-height: 240px;
                      overflow-y: auto;
                      border: 1px solid #f0f0f0;
                      border-radius: 4px;
                    "
                  >
                    <div
                      v-for="doc in documentList"
                      :key="doc.id"
                      class="doc-item"
                    >
                      <Checkbox :value="doc.id" style="width: 100%">
                        <div class="doc-item-content">
                          <span class="doc-icon">{{
                            getFileIcon(doc.fileType)
                          }}</span>
                          <div class="doc-info">
                            <div class="doc-name">{{ doc.name }}</div>
                            <div class="doc-meta">
                              <Tag size="small" color="blue">{{
                                doc.fileType?.toUpperCase() || '文件'
                              }}</Tag>
                              <span v-if="doc.fileSize">{{
                                formatFileSize(doc.fileSize)
                              }}</span>
                            </div>
                          </div>
                        </div>
                      </Checkbox>
                    </div>
                  </div>
                </Checkbox.Group>
                <div style="margin-top: 8px; font-size: 13px; color: #1890ff">
                  ✓ 已选择
                  <b>{{ pushForm.documentIds?.length || 0 }}</b>
                  个文档，客户可在有效期内下载
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
              <span style="font-size: 12px; color: #999"
                >（超过有效期，客户服务系统自动删除数据）</span
              >
            </Space>
          </DescriptionsItem>
        </Descriptions>
      </Modal>

      <!-- 同步到卷宗弹窗 -->
      <Modal
        v-model:open="syncModalVisible"
        title="同步文件到卷宗"
        :width="500"
        :confirm-loading="syncLoading"
        ok-text="确认同步"
        @ok="handleSync"
      >
        <Alert
          type="info"
          show-icon
          style="margin-bottom: 16px"
          message="同步后文件将保存到项目卷宗中，客服系统中的文件将被删除以节省空间"
        />

        <Descriptions v-if="currentSyncFile" :column="1" bordered size="small">
          <DescriptionsItem label="文件名">
            {{ currentSyncFile.fileName }}
          </DescriptionsItem>
          <DescriptionsItem label="文件类型">
            <Tag
              :color="
                FILE_CATEGORY_MAP[currentSyncFile.fileCategory || 'OTHER']
                  ?.color || 'default'
              "
            >
              {{
                FILE_CATEGORY_MAP[currentSyncFile.fileCategory || 'OTHER']
                  ?.text || '其他'
              }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="上传人">
            {{
              currentSyncFile.uploadedBy || currentSyncFile.clientName || '-'
            }}
          </DescriptionsItem>
          <DescriptionsItem label="目标卷宗">
            <TreeSelect
              v-model:value="syncForm.targetDossierId"
              :tree-data="dossierTree"
              :loading="dossierLoading"
              placeholder="请选择目标卷宗目录"
              tree-default-expand-all
              style="width: 100%"
              :dropdown-style="{ maxHeight: '300px', overflow: 'auto' }"
            />
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

/* 客户文件卡片 - 突出显示 */
.client-files-card {
  min-height: 400px;
}

.client-files-card :deep(.ant-card-head-title) {
  padding: 12px 0;
}

/* 空文件占位符 */
.empty-files-placeholder {
  padding: 48px 24px;
  text-align: center;
}

/* 文件列表项 */
.file-list-item {
  padding: 12px 0 !important;
  border-bottom: 1px solid #f0f0f0;
}

.file-list-item:last-child {
  border-bottom: none;
}

.file-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #f5f7fa;
  border-radius: 8px;
}

/* 推送记录项 */
.push-record-item {
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}

.push-record-item:last-child {
  border-bottom: none;
}

/* 统计样式 */
.stat-item {
  text-align: center;
}

.stat-title {
  margin-bottom: 4px;
  font-size: 12px;
  color: rgb(0 0 0 / 45%);
}

.stat-value {
  font-size: 14px;
  color: rgb(0 0 0 / 85%);
}

:deep(.ant-statistic-title) {
  font-size: 12px;
}

:deep(.ant-statistic-content) {
  font-size: 20px;
}

/* 紧凑表单 */
:deep(.ant-form-item-label) {
  padding-bottom: 4px !important;
}

:deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: #666;
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
  gap: 10px;
  align-items: center;
}

.doc-icon {
  flex-shrink: 0;
  font-size: 24px;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-name {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: #333;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}

:deep(.doc-item .ant-checkbox-wrapper) {
  width: 100%;
}
</style>
