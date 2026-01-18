<script setup lang="ts">
import type { MatterDossierItem } from '#/api/document/dossier';
import type { DocumentDTO } from '#/api/document/types';
import type { MatterDTO, MatterSimpleDTO } from '#/api/matter/types';
import type { OcrResultDTO } from '#/api/ocr';

import { computed, h, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import {
  ArrowUp,
  Edit,
  Ellipsis,
  Eye,
  GripVertical,
  Inbox,
  Plus,
  SvgDownloadIcon,
  Trash,
} from '@vben/icons';

import {
  Button,
  Card,
  Col,
  Divider,
  Dropdown,
  Form,
  FormItem,
  Input,
  Menu,
  MenuItem,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Tree,
  Upload,
} from 'ant-design-vue';
import draggable from 'vuedraggable';

import {
  checkDocumentEditSupport,
  createFolder,
  deleteDocument,
  downloadDocument,
  downloadDocumentsAsZip,
  getDocumentList,
  getDocumentPreviewUrl,
  getDocumentVersions,
  moveDocument,
  reorderDocuments,
  shareDocument,
  updateDocument,
  uploadFiles,
} from '#/api/document';
import {
  addDossierItem,
  deleteDossierItem,
  getMatterDossierItems,
  initMatterDossier,
  regeneratePowerOfAttorney,
  reorderDossierItems,
  triggerAutoArchive,
  updateDossierItem,
} from '#/api/document/dossier';
import { getMatterSelectOptions } from '#/api/matter';
import { recognizeGeneral } from '#/api/ocr';
import {
  CASE_CATEGORY_OPTIONS,
  findCauseNameInAll,
  MATTER_TYPE_OPTIONS,
} from '#/composables/useCauseOfAction';

defineOptions({ name: 'DossierManager' });

const router = useRouter();

// 状态管理
const loading = ref(false);
const selectedMatterId = ref<number | undefined>(undefined);
const selectedMatter = ref<MatterDTO | MatterSimpleDTO | null>(null);
const selectedFolder = ref<string>('root');
const currentPath = ref<string[]>(['根目录']);
const documents = ref<DocumentDTO[]>([]);
const matters = ref<MatterSimpleDTO[]>([]);
const fileList = ref<any[]>([]);
const dossierItems = ref<MatterDossierItem[]>([]);

// 批量选择状态
const selectedDocIds = ref<Set<number>>(new Set());
const batchDownloading = ref(false);

// 视图模式状态: 'list' | 'grid'
const viewMode = ref<'grid' | 'list'>('grid');
// 悬停预览相关
const hoverPreview = ref<{
  doc: DocumentDTO | null;
  show: boolean;
  x: number;
  y: number;
}>({
  doc: null,
  show: false,
  x: 0,
  y: 0,
});
let hoverTimeout: ReturnType<typeof setTimeout> | null = null;

// 图片预览缩放
const imageZoom = ref(1);

// 弹窗状态
const uploadModalVisible = ref(false);
const folderModalVisible = ref(false);
const editModalVisible = ref(false);
const previewModalVisible = ref(false);
const shareModalVisible = ref(false);
const versionModalVisible = ref(false);
const ocrModalVisible = ref(false);
const ocrLoading = ref(false);
const ocrResult = ref('');

// 当前操作的文档
const currentDocument = ref<DocumentDTO | null>(null);
const versions = ref<DocumentDTO[]>([]);
const previewUrl = ref('');
const shareUrl = ref('');

// 表单数据
const uploadFormData = reactive({
  matterId: undefined as number | undefined,
  dossierItemId: undefined as number | undefined,
  folder: 'root', // 保留 folder 用于 folderPath
  description: '',
});

const folderFormData = reactive({
  name: '',
  parentFolder: 'root',
});

const editFormData = reactive({
  id: undefined as number | undefined,
  title: '', // 后端使用 title 字段
  description: '',
});

// 从后端获取的卷宗目录项构建树形数据
const folderTreeData = computed(() => {
  if (!selectedMatter.value || dossierItems.value.length === 0) {
    return [
      {
        title: selectedMatter.value ? '卷宗目录（点击初始化）' : '请选择项目',
        key: 'root',
        children: [],
      },
    ];
  }

  // 将扁平数据转换为树形结构
  const items = [...dossierItems.value].sort(
    (a, b) => a.sortOrder - b.sortOrder,
  );
  const rootItems = items.filter((item) => item.parentId === 0);

  const buildTree = (parentItems: MatterDossierItem[]): any[] => {
    return parentItems.map((item) => {
      const children = items.filter((child) => child.parentId === item.id);
      return {
        title: `${item.name}${item.documentCount ? ` (${item.documentCount})` : ''}`,
        key: String(item.id),
        dossierItemId: item.id,
        name: item.name,
        sortOrder: item.sortOrder,
        documentCount: item.documentCount || 0,
        children: children.length > 0 ? buildTree(children) : undefined,
      };
    });
  };

  return [
    {
      title: `${selectedMatter.value.name} 卷宗目录`,
      key: 'root',
      children: buildTree(rootItems),
    },
  ];
});

// 加载项目的卷宗目录
async function loadDossierItems() {
  if (!selectedMatter.value) {
    dossierItems.value = [];
    return;
  }

  try {
    const items = await getMatterDossierItems(selectedMatter.value.id);
    dossierItems.value = items;
  } catch (error) {
    console.error('加载卷宗目录失败:', error);
    dossierItems.value = [];
  }
}

// 初始化卷宗目录
async function handleInitDossier() {
  if (!selectedMatter.value) return;

  try {
    const items = await initMatterDossier(selectedMatter.value.id);
    dossierItems.value = items;
    message.success('卷宗目录初始化成功');
  } catch (error: any) {
    message.error(`初始化失败: ${error.message || '未知错误'}`);
  }
}

// 重新生成授权委托书
const regenerating = ref(false);
async function handleRegeneratePOA() {
  if (!selectedMatter.value) {
    message.warning('请先选择项目');
    return;
  }

  regenerating.value = true;
  try {
    const result = await regeneratePowerOfAttorney(selectedMatter.value.id);
    if (result.templateUsed) {
      message.success(`${result.message}（使用模板：${result.templateName}）`);
    } else {
      message.warning(`${result.message}。${result.hint || ''}`);
    }
    // 刷新文档列表
    loadProjectDocuments();
  } catch (error: any) {
    message.error(error.message || '重新生成失败');
  } finally {
    regenerating.value = false;
  }
}

// 触发自动归档
const archiving = ref(false);
async function handleTriggerArchive() {
  if (!selectedMatter.value) {
    message.warning('请先选择项目');
    return;
  }

  archiving.value = true;
  try {
    const result = await triggerAutoArchive(selectedMatter.value.id);
    message.success(result.message);
    // 刷新文档列表
    loadProjectDocuments();
  } catch (error: any) {
    message.error(error.message || '归档失败');
  } finally {
    archiving.value = false;
  }
}

// 添加目录项
async function handleAddDossierItem() {
  if (!selectedMatter.value) {
    message.warning('请先选择项目');
    return;
  }

  // 使用输入框获取名称
  Modal.confirm({
    title: '新建文件夹',
    content: h('div', [
      h('p', '请输入文件夹名称：'),
      h('input', {
        id: 'newFolderName',
        type: 'text',
        placeholder: '文件夹名称',
        style:
          'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;',
      }),
    ]),
    onOk: async () => {
      const input = document.querySelector(
        '#newFolderName',
      ) as HTMLInputElement;
      const name = input?.value?.trim();
      if (!name) {
        message.error('请输入文件夹名称');
        throw undefined;
      }

      try {
        await addDossierItem(selectedMatter.value!.id, { name, parentId: 0 });
        await loadDossierItems();
        message.success('文件夹创建成功');
      } catch (error: any) {
        message.error(`创建失败: ${error.message || '未知错误'}`);
        return Promise.reject();
      }
    },
  });
}

// 重命名目录项
async function handleRenameDossierItem(item: any) {
  if (!selectedMatter.value) return;

  Modal.confirm({
    title: '重命名文件夹',
    content: h('div', [
      h('p', '请输入新名称：'),
      h('input', {
        id: 'renameFolderName',
        type: 'text',
        value: item.name,
        style:
          'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;',
      }),
    ]),
    onOk: async () => {
      const input = document.querySelector(
        '#renameFolderName',
      ) as HTMLInputElement;
      const name = input?.value?.trim();
      if (!name) {
        message.error('请输入文件夹名称');
        throw undefined;
      }

      try {
        await updateDossierItem(selectedMatter.value!.id, item.dossierItemId, {
          name,
        });
        await loadDossierItems();
        message.success('重命名成功');
      } catch (error: any) {
        message.error(`重命名失败: ${error.message || '未知错误'}`);
        return Promise.reject();
      }
    },
  });
}

// 删除目录项
async function handleDeleteDossierItem(item: any) {
  if (!selectedMatter.value) return;

  if (item.documentCount > 0) {
    message.error('该文件夹下有文件，无法删除');
    return;
  }

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除文件夹 "${item.name}" 吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteDossierItem(selectedMatter.value!.id, item.dossierItemId);
        await loadDossierItems();
        message.success('删除成功');
      } catch (error: any) {
        message.error(`删除失败: ${error.message || '未知错误'}`);
      }
    },
  });
}

// 调整目录项顺序（上移）
async function handleMoveUp(item: any) {
  if (!selectedMatter.value) return;

  const items = dossierItems.value
    .filter((i) => i.parentId === 0)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  const currentIndex = items.findIndex((i) => i.id === item.dossierItemId);
  if (currentIndex <= 0) return;

  // 交换位置
  const newOrder: number[] = items
    .map((i) => i.id)
    .filter((id): id is number => id !== undefined);
  if (newOrder.length < 2) return;
  const temp = newOrder[currentIndex];
  newOrder[currentIndex] = newOrder[currentIndex - 1]!;
  newOrder[currentIndex - 1] = temp!;

  try {
    await reorderDossierItems(selectedMatter.value.id, newOrder);
    await loadDossierItems();
  } catch {
    message.error('调整顺序失败');
  }
}

// 调整目录项顺序（下移）
async function handleMoveDown(item: any) {
  if (!selectedMatter.value) return;

  const items = dossierItems.value
    .filter((i) => i.parentId === 0)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  const currentIndex = items.findIndex((i) => i.id === item.dossierItemId);
  if (currentIndex === -1 || currentIndex >= items.length - 1) return;

  // 交换位置
  const newOrder: number[] = items
    .map((i) => i.id)
    .filter((id): id is number => id !== undefined);
  if (newOrder.length < 2) return;
  const temp = newOrder[currentIndex];
  newOrder[currentIndex] = newOrder[currentIndex + 1]!;
  newOrder[currentIndex + 1] = temp!;

  try {
    await reorderDossierItems(selectedMatter.value.id, newOrder);
    await loadDossierItems();
  } catch {
    message.error('调整顺序失败');
  }
}

// 当前文件夹的文档列表（可拖拽排序）
const sortableDocuments = ref<DocumentDTO[]>([]);

// 监听文档变化，更新可排序列表
const currentDocuments = computed({
  get: () => sortableDocuments.value,
  set: (val: DocumentDTO[]) => {
    sortableDocuments.value = val;
  },
});

// 更新可排序文档列表
function updateSortableDocuments() {
  if (!selectedMatter.value) {
    sortableDocuments.value = [];
    return;
  }

  const projectDocs = documents.value.filter(
    (doc) => doc.matterId === selectedMatter.value?.id,
  );

  // 如果选中的是根文件夹，显示所有项目文档
  if (selectedFolder.value === 'root') {
    // 按 displayOrder 排序
    sortableDocuments.value = [...projectDocs].sort(
      (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0),
    );
    return;
  }

  // 否则只显示特定 dossierItemId 的文档
  const dossierItemId = Number.parseInt(selectedFolder.value, 10);
  if (!isNaN(dossierItemId)) {
    sortableDocuments.value = projectDocs
      .filter((doc) => doc.dossierItemId === dossierItemId)
      .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    return;
  }

  sortableDocuments.value = [...projectDocs].sort(
    (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0),
  );
}

// 文档统计
const documentStats = computed(() => {
  if (!selectedMatter.value) return { total: 0, size: '0B' };

  const projectDocs = documents.value.filter(
    (doc) => doc.matterId === selectedMatter.value?.id,
  );
  const totalSize = projectDocs.reduce(
    (sum, doc) => sum + (doc.fileSize || 0),
    0,
  );

  return {
    total: projectDocs.length,
    size: formatFileSize(totalSize),
  };
});

// 获取当前年份
const currentYear = new Date().getFullYear();

// 筛选参数
const filterParams = reactive({
  year: currentYear as number | undefined, // 默认当前年份
  matterType: undefined as string | undefined, // 项目大类
  caseType: undefined as string | undefined, // 案件类型
  status: undefined as string | undefined,
  keyword: undefined as string | undefined,
});

// 筛选选项（最近5年 + 全部）
const yearOptions = computed(() => {
  const years = [
    { label: '全部年份', value: 0 },
    ...Array.from({ length: 5 }, (_, i) => ({
      label: `${currentYear - i}年`,
      value: currentYear - i,
    })),
  ];
  return years;
});

// 项目大类（matter_type）- 使用统一常量
const matterTypeOptions = MATTER_TYPE_OPTIONS;

// 案件类型（case_type）- 使用常量确保完整
const caseTypeOptions = CASE_CATEGORY_OPTIONS.map((opt) => ({
  label: opt.label,
  value: opt.value,
}));

// 根据案件类型获取显示名称
function getCaseTypeName(type: string | undefined): string {
  if (!type) return '-';
  const option = caseTypeOptions.find((opt) => opt.value === type);
  return option?.label || type;
}

// 根据状态值获取显示名称
function getStatusName(status: string | undefined): string {
  if (!status) return '-';
  const option = statusOptions.find((opt) => opt.value === status);
  return option?.label || status;
}

// 根据项目类型获取封面主题颜色
function getMatterCoverTheme(matter: MatterDTO | MatterSimpleDTO) {
  // 根据案件类型设置主题色
  const caseType = matter.caseType;
  const matterType = matter.matterType;

  // 诉讼类案件
  if (matterType === 'LITIGATION') {
    switch (caseType) {
      case 'ADMINISTRATIVE': {
        return {
          primaryColor: '#388e3c', // 绿色 - 行政
          bgColor: '#f1f8e9',
          borderColor: '#c5e1a5',
          label: '行政诉讼类',
        };
      }
      case 'ARBITRATION': {
        return {
          primaryColor: '#0288d1', // 青色 - 仲裁
          bgColor: '#e0f7fa',
          borderColor: '#b2ebf2',
          label: '仲裁案件类',
        };
      }
      case 'BANKRUPTCY': {
        return {
          primaryColor: '#f57c00', // 橙色 - 破产
          bgColor: '#fff3e0',
          borderColor: '#ffe0b2',
          label: '破产案件类',
        };
      }
      case 'CIVIL': {
        return {
          primaryColor: '#1976d2', // 蓝色 - 民事
          bgColor: '#e3f2fd',
          borderColor: '#bbdefb',
          label: '民事诉讼类',
        };
      }
      case 'CRIMINAL': {
        return {
          primaryColor: '#d32f2f', // 红色 - 刑事
          bgColor: '#fff5f5',
          borderColor: '#ffcdd2',
          label: '刑事诉讼类',
        };
      }
      case 'ENFORCEMENT': {
        return {
          primaryColor: '#5d4037', // 棕色 - 执行
          bgColor: '#efebe9',
          borderColor: '#d7ccc8',
          label: '执行案件类',
        };
      }
      case 'IP': {
        return {
          primaryColor: '#7b1fa2', // 紫色 - 知识产权
          bgColor: '#f3e5f5',
          borderColor: '#ce93d8',
          label: '知识产权类',
        };
      }
      default: {
        return {
          primaryColor: '#616161', // 灰色 - 其他诉讼
          bgColor: '#fafafa',
          borderColor: '#e0e0e0',
          label: '诉讼案件类',
        };
      }
    }
  }

  // 非诉项目
  if (matterType === 'NON_LITIGATION') {
    switch (caseType) {
      case 'LEGAL_COUNSEL': {
        return {
          primaryColor: '#00796b', // 深青色 - 法律顾问
          bgColor: '#e0f2f1',
          borderColor: '#b2dfdb',
          label: '法律顾问类',
        };
      }
      case 'SPECIAL_SERVICE': {
        return {
          primaryColor: '#e64a19', // 深橙色 - 专项服务
          bgColor: '#fbe9e7',
          borderColor: '#ffccbc',
          label: '专项服务类',
        };
      }
      default: {
        return {
          primaryColor: '#455a64', // 蓝灰色 - 其他非诉
          bgColor: '#eceff1',
          borderColor: '#cfd8dc',
          label: '非诉项目类',
        };
      }
    }
  }

  // 默认主题
  return {
    primaryColor: '#757575',
    bgColor: '#fafafa',
    borderColor: '#e0e0e0',
    label: '业务档案卷宗',
  };
}

// 格式化日期（只显示年月日）
function formatDate(dateStr: string | undefined): string {
  if (!dateStr) return '-';
  return dateStr.slice(0, 10);
}

// 获取案由名称（将代码转换为名称）
function getCauseOfActionName(matter: MatterDTO | MatterSimpleDTO): string {
  // 优先使用后端返回的名称
  if (matter.causeOfActionName) {
    return matter.causeOfActionName;
  }
  // 如果有代码，尝试从前端案由数据中查找名称
  if (matter.causeOfAction) {
    const name = findCauseNameInAll(matter.causeOfAction);
    if (name) return name;
    // 如果找不到，返回代码（可能是自定义案由）
    return matter.causeOfAction;
  }
  return '-';
}

// 选择项目（从项目列表卡片点击）
async function handleSelectMatter(matter: MatterDTO | MatterSimpleDTO) {
  selectedMatterId.value = matter.id;
  selectedMatter.value = matter;
  selectedFolder.value = 'root';
  currentPath.value = [`${matter.name} 卷宗目录`];

  // 加载卷宗目录和文档
  await loadDossierItems();
  loadProjectDocuments();
}

// 返回项目列表
function handleBackToList() {
  selectedMatterId.value = undefined;
  selectedMatter.value = null;
  selectedFolder.value = 'root';
  currentPath.value = ['根目录'];
  documents.value = [];
  dossierItems.value = [];
}

// 跳转到项目详情页
function handleGoToMatterDetail() {
  if (selectedMatterId.value) {
    router.push(`/matter/detail/${selectedMatterId.value}`);
  }
}

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审批', value: 'PENDING' },
  { label: '进行中', value: 'ACTIVE' },
  { label: '已暂停', value: 'SUSPENDED' },
  { label: '待审批结案', value: 'PENDING_CLOSE' },
  { label: '已结案', value: 'CLOSED' },
  { label: '已归档', value: 'ARCHIVED' },
];

// 过滤后的项目列表（年份筛选已在后端完成，这里只做其他筛选）
const filteredMatters = computed(() => {
  let filtered = matters.value;

  // 按项目大类筛选
  if (filterParams.matterType) {
    filtered = filtered.filter(
      (matter) => matter.matterType === filterParams.matterType,
    );
  }

  // 按案件类型筛选
  if (filterParams.caseType) {
    filtered = filtered.filter(
      (matter) => matter.caseType === filterParams.caseType,
    );
  }

  // 按状态筛选
  if (filterParams.status) {
    filtered = filtered.filter(
      (matter) => matter.status === filterParams.status,
    );
  }

  // 按关键词筛选
  if (filterParams.keyword) {
    const keyword = filterParams.keyword.toLowerCase();
    filtered = filtered.filter(
      (matter) =>
        matter.name.toLowerCase().includes(keyword) ||
        matter.clientName?.toLowerCase().includes(keyword),
    );
  }

  return filtered;
});

// 注意：handleMatterChange 和 filterMatterOption 已被 handleSelectMatter 替代

// 筛选
function handleFilter() {
  // 如果在卷宗目录视图，返回项目列表
  if (selectedMatter.value) {
    handleBackToList();
  }

  const count = filteredMatters.value.length;
  if (count === 0) {
    message.warning('没有找到符合条件的项目');
  } else {
    message.info(`找到 ${count} 个符合条件的项目`);
  }
}

// 重置筛选
function handleResetFilter() {
  filterParams.year = currentYear; // 重置为当前年份
  filterParams.matterType = undefined;
  filterParams.caseType = undefined;
  filterParams.status = undefined;
  filterParams.keyword = undefined;
  handleBackToList();
  loadMatters(); // 重新加载项目列表
}

// 年份变化时重新加载项目列表
function handleYearChange() {
  handleBackToList();
  loadMatters();
}

// 筛选条件变化时返回项目列表
function handleFilterChange() {
  // 如果在卷宗目录视图，返回项目列表
  if (selectedMatter.value) {
    handleBackToList();
  }
}

// 文件类型图标和颜色配置
const fileTypeConfig: Record<
  string,
  { color: string; icon: string; label: string }
> = {
  // 文档类
  pdf: { icon: '📄', color: '#e74c3c', label: 'PDF' },
  doc: { icon: '📝', color: '#2b579a', label: 'Word' },
  docx: { icon: '📝', color: '#2b579a', label: 'Word' },
  xls: { icon: '📊', color: '#217346', label: 'Excel' },
  xlsx: { icon: '📊', color: '#217346', label: 'Excel' },
  ppt: { icon: '📽️', color: '#d24726', label: 'PPT' },
  pptx: { icon: '📽️', color: '#d24726', label: 'PPT' },
  txt: { icon: '📃', color: '#666666', label: '文本' },
  rtf: { icon: '📃', color: '#666666', label: 'RTF' },
  odt: { icon: '📝', color: '#0066cc', label: 'ODT' },
  // 图片类
  jpg: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  jpeg: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  png: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  gif: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  bmp: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  svg: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  webp: { icon: '🖼️', color: '#9b59b6', label: '图片' },
  // 压缩包
  zip: { icon: '📦', color: '#f39c12', label: '压缩包' },
  rar: { icon: '📦', color: '#f39c12', label: '压缩包' },
  '7z': { icon: '📦', color: '#f39c12', label: '压缩包' },
  tar: { icon: '📦', color: '#f39c12', label: '压缩包' },
  gz: { icon: '📦', color: '#f39c12', label: '压缩包' },
  // 视频
  mp4: { icon: '🎬', color: '#e67e22', label: '视频' },
  avi: { icon: '🎬', color: '#e67e22', label: '视频' },
  mov: { icon: '🎬', color: '#e67e22', label: '视频' },
  mkv: { icon: '🎬', color: '#e67e22', label: '视频' },
  // 音频
  mp3: { icon: '🎵', color: '#1abc9c', label: '音频' },
  wav: { icon: '🎵', color: '#1abc9c', label: '音频' },
  m4a: { icon: '🎵', color: '#1abc9c', label: '音频' },
  // 代码
  json: { icon: '📋', color: '#27ae60', label: 'JSON' },
  xml: { icon: '📋', color: '#27ae60', label: 'XML' },
  html: { icon: '🌐', color: '#e44d26', label: 'HTML' },
  css: { icon: '🎨', color: '#264de4', label: 'CSS' },
  js: { icon: '⚡', color: '#f7df1e', label: 'JS' },
  // 其他
  default: { icon: '📁', color: '#95a5a6', label: '文件' },
};

// 获取文件类型配置
function getFileTypeConfig(fileType: string | undefined): {
  color: string;
  icon: string;
  label: string;
} {
  if (!fileType) return fileTypeConfig.default!;
  const ext = fileType.toLowerCase().replace('.', '');
  return fileTypeConfig[ext] || fileTypeConfig.default!;
}

// 获取文档来源类型配置
const sourceTypeConfig: Record<string, { color: string; label: string }> = {
  SYSTEM_GENERATED: { color: 'blue', label: '系统生成' },
  SYSTEM_LINKED: { color: 'cyan', label: '系统关联' },
  USER_UPLOADED: { color: 'default', label: '用户上传' },
  SIGNED_VERSION: { color: 'green', label: '签字版' },
};

function getSourceTypeConfig(sourceType: string | undefined): {
  color: string;
  label: string;
} {
  if (!sourceType) return { color: 'default', label: '用户上传' };
  return (
    sourceTypeConfig[sourceType] || { color: 'default', label: sourceType }
  );
}

// 格式化日期时间
function formatDateTime(dateStr: string | undefined) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// 加载项目列表
async function loadMatters() {
  loading.value = true;
  try {
    // 处理年份筛选参数
    let createdAtFrom: string | undefined;
    let createdAtTo: string | undefined;
    if (filterParams.year && filterParams.year !== 0) {
      createdAtFrom = `${filterParams.year}-01-01T00:00:00`;
      createdAtTo = `${filterParams.year}-12-31T23:59:59`;
    }

    const res = await getMatterSelectOptions({
      pageNum: 1,
      pageSize: 1000,
      createdAtFrom,
      createdAtTo,
    });

    if (res && res.list) {
      matters.value = res.list;
    } else {
      matters.value = [];
    }
  } catch (error: any) {
    message.error(`加载项目列表失败: ${error.message || '未知错误'}`);
    matters.value = [];
  } finally {
    loading.value = false;
  }
}

// 加载项目文档
async function loadProjectDocuments() {
  if (!selectedMatter.value) return;

  loading.value = true;
  try {
    const res = await getDocumentList({
      matterId: selectedMatter.value.id,
      pageNum: 1,
      pageSize: 1000,
    });
    documents.value = res.list;
    // 更新可排序文档列表
    updateSortableDocuments();
  } catch {
    message.error('加载文档失败');
  } finally {
    loading.value = false;
  }
}

// 选择文件夹
function handleFolderSelect(selectedKeys: any[], _info: any) {
  if (selectedKeys.length > 0) {
    const key = String(selectedKeys[0]);
    selectedFolder.value = key;

    // 更新面包屑路径
    if (key === 'root') {
      currentPath.value = [
        `${selectedMatter.value?.name} 卷宗目录` || '卷宗目录',
      ];
    } else {
      const dossierItemId = Number.parseInt(key, 10);
      const item = dossierItems.value.find((i) => i.id === dossierItemId);
      if (item) {
        currentPath.value = [selectedMatter.value?.name || '卷宗', item.name];
      }
    }

    // 更新可排序文档列表
    updateSortableDocuments();
  }
}

// 获取文件夹选项（用于上传时选择存储位置）
// 优先使用从后端获取的真实卷宗目录项
const folderOptions = computed(() => {
  // 如果有从后端获取的卷宗目录，使用真实数据
  if (dossierItems.value.length > 0) {
    // 包含所有顶级目录项（不过滤 itemType，因为 FILE 类型也可以作为上传目标）
    return dossierItems.value
      .filter((item) => item.parentId === 0) // 只显示顶级目录
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map((item) => ({
        label: item.name,
        value: item.id, // 使用实际的 dossierItemId
        dossierItemId: item.id,
        folderPath: item.name,
      }));
  }

  // 如果没有卷宗目录，返回空（用户需要先初始化卷宗目录）
  if (!selectedMatter.value) return [];

  // 备用静态选项（仅在后端没有数据时使用）
  return [
    { label: '根目录', value: 0, dossierItemId: undefined, folderPath: 'root' },
  ];
});

// 上传文档
async function handleUpload() {
  if (!selectedMatter.value) {
    message.warning('请先选择项目');
    return;
  }
  uploadFormData.matterId = selectedMatter.value.id;

  // 从后端获取项目的卷宗目录
  try {
    const items = await getMatterDossierItems(selectedMatter.value.id);
    dossierItems.value = items;

    if (items.length === 0) {
      message.warning('该项目尚未初始化卷宗目录，请先在左侧目录区初始化');
    }
  } catch (error) {
    console.error('获取卷宗目录失败:', error);
    dossierItems.value = [];
  }

  // 设置默认的存储位置为第一个可用的文件夹
  const options = folderOptions.value;
  if (options.length > 0 && options[0]) {
    uploadFormData.dossierItemId = options[0].dossierItemId ?? undefined;
    uploadFormData.folder = options[0].folderPath ?? 'root';
  } else {
    uploadFormData.dossierItemId = undefined;
    uploadFormData.folder = 'root';
  }

  uploadFormData.description = '';
  fileList.value = [];
  uploadModalVisible.value = true;
}

// 处理上传时的文件夹选择
function handleUploadFolderSelect(value: any) {
  const option = folderOptions.value.find((opt) => opt.value === value);
  if (option) {
    uploadFormData.dossierItemId = option.dossierItemId ?? undefined;
    uploadFormData.folder = option.folderPath ?? 'root';
  }
}

// 创建文件夹
function handleCreateFolder() {
  // 调用新的目录项添加功能
  handleAddDossierItem();
}

// 判断文件是否为图片类型
function isImageFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['bmp', 'gif', 'ico', 'jpeg', 'jpg', 'png', 'svg', 'webp'].includes(
    ext,
  );
}

// 判断文件是否为视频类型
function isVideoFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['avi', 'mkv', 'mov', 'mp4', 'ogg', 'webm'].includes(ext);
}

// 判断文件是否为音频类型
function isAudioFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['aac', 'flac', 'm4a', 'mp3', 'ogg', 'wav'].includes(ext);
}

// 判断文件是否支持 OnlyOffice 预览
function isOfficeFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return [
    'csv',
    'doc',
    'docx',
    'odp',
    'ods',
    'odt',
    'pdf',
    'ppt',
    'pptx',
    'rtf',
    'txt',
    'xls',
    'xlsx',
  ].includes(ext);
}

// 判断文件是否支持在线编辑（不包括 PDF，PDF 只能预览）
function isEditableFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  // 支持编辑的文件类型：Word、Excel、PowerPoint 及其开放格式
  return [
    'csv',
    'doc',
    'docx',
    'odp',
    'ods',
    'odt',
    'ppt',
    'pptx',
    'rtf',
    'txt',
    'xls',
    'xlsx',
  ].includes(ext);
}

// 预览文档（根据文件类型选择不同的预览方式）
async function handlePreview(record: DocumentDTO) {
  const fileType = record.fileType?.toLowerCase() || '';

  // Office 文档类型 - 使用 OnlyOffice 预览
  if (isOfficeFile(fileType)) {
    const resolved = router.resolve({
      path: '/office-preview',
      query: { documentId: String(record.id), mode: 'view' },
    });
    window.open(resolved.href, '_blank');
    return;
  }

  // 对于非 Office 文件，先获取预签名 URL
  try {
    const { previewUrl } = await getDocumentPreviewUrl(record.id);
    const fileUrl = previewUrl;

    // 图片类型 - 使用专用图片预览弹窗（支持缩放）
    if (isImageFile(fileType)) {
      currentDocument.value = { ...record, filePath: fileUrl };
      imageZoom.value = 1; // 重置缩放
      previewModalVisible.value = true;
      return;
    }

    // 视频类型 - 使用 HTML5 Video 播放
    if (isVideoFile(fileType)) {
      Modal.info({
        title: record.fileName || record.name,
        icon: null,
        width: '80%',
        centered: true,
        content: h('div', { style: 'text-align: center;' }, [
          h('video', {
            src: fileUrl,
            controls: true,
            style: 'max-width: 100%; max-height: 70vh;',
          }),
        ]),
        okText: '关闭',
      });
      return;
    }

    // 音频类型 - 使用 HTML5 Audio 播放
    if (isAudioFile(fileType)) {
      Modal.info({
        title: record.fileName || record.name,
        icon: null,
        width: 500,
        centered: true,
        content: h('div', { style: 'text-align: center; padding: 20px 0;' }, [
          h('audio', {
            src: fileUrl,
            controls: true,
            style: 'width: 100%;',
          }),
        ]),
        okText: '关闭',
      });
      return;
    }

    // PDF 类型 - 直接在新窗口打开
    if (fileType === 'pdf') {
      window.open(fileUrl, '_blank');
      return;
    }

    // 其他类型 - 尝试直接下载或打开
    message.info('该文件类型不支持在线预览，将尝试下载');
    window.open(fileUrl, '_blank');
  } catch (error: any) {
    message.error(`获取预览链接失败: ${error.message || '未知错误'}`);
  }
}

// 在线编辑文档（跳转到 OnlyOffice 编辑页面）
async function handleOnlineEdit(record: DocumentDTO) {
  try {
    // 先检查是否支持在线编辑
    const support = await checkDocumentEditSupport(record.id);
    if (!support.canEdit) {
      message.warning('该文件类型不支持在线编辑，请下载后使用本地软件编辑');
      return;
    }
    // 跳转到编辑页面
    const resolved = router.resolve({
      path: '/office-preview',
      query: { documentId: String(record.id), mode: 'edit' },
    });
    window.open(resolved.href, '_blank');
  } catch {
    message.error('检查编辑支持失败');
  }
}

// 下载文档
async function handleDownload(record: DocumentDTO) {
  try {
    await downloadDocument(record.id);
    message.success('下载成功');
  } catch {
    message.error('下载失败');
  }
}

// 批量选择相关方法
function toggleDocSelection(docId: number) {
  if (selectedDocIds.value.has(docId)) {
    selectedDocIds.value.delete(docId);
  } else {
    selectedDocIds.value.add(docId);
  }
  // 触发响应式更新
  selectedDocIds.value = new Set(selectedDocIds.value);
}

function isDocSelected(docId: number) {
  return selectedDocIds.value.has(docId);
}

function isAllSelected() {
  return (
    currentDocuments.value.length > 0 &&
    currentDocuments.value.every((doc) => selectedDocIds.value.has(doc.id))
  );
}

function toggleSelectAll() {
  if (isAllSelected()) {
    // 取消全选
    currentDocuments.value.forEach((doc) =>
      selectedDocIds.value.delete(doc.id),
    );
  } else {
    // 全选当前文件夹
    currentDocuments.value.forEach((doc) => selectedDocIds.value.add(doc.id));
  }
  selectedDocIds.value = new Set(selectedDocIds.value);
}

function clearSelection() {
  selectedDocIds.value = new Set();
}

// 批量下载
async function handleBatchDownload() {
  const ids = [...selectedDocIds.value];
  if (ids.length === 0) {
    message.warning('请先选择要下载的文档');
    return;
  }

  if (ids.length > 100) {
    message.warning('单次最多下载100个文档');
    return;
  }

  batchDownloading.value = true;
  try {
    const matterName = selectedMatter.value?.name || '文档';
    const fileName = `${matterName}_文档_${new Date().toISOString().slice(0, 10)}.zip`;
    await downloadDocumentsAsZip(ids, fileName);
    message.success(`成功下载 ${ids.length} 个文档`);
    clearSelection();
  } catch (error: any) {
    message.error(`批量下载失败：${error.message || '未知错误'}`);
  } finally {
    batchDownloading.value = false;
  }
}

// OCR提取文字（仅用于图片文件）
async function handleOcrExtract(record: DocumentDTO) {
  if (!isImageFile(record.fileType || '')) {
    message.warning('仅支持对图片文件进行OCR识别');
    return;
  }

  ocrLoading.value = true;
  ocrModalVisible.value = true;
  ocrResult.value = '';
  currentDocument.value = record;

  try {
    // 先获取图片的预签名URL
    const { previewUrl } = await getDocumentPreviewUrl(record.id);

    // 下载图片并转换为File对象
    const response = await fetch(previewUrl);
    const blob = await response.blob();
    const file = new File([blob], record.fileName || 'image.jpg', {
      type: blob.type,
    });

    // 调用OCR识别
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success) {
      ocrResult.value = result.rawText || '未识别到文字内容';
      message.success('OCR识别完成');
    } else {
      ocrResult.value = result.errorMessage || 'OCR识别失败';
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (error: any) {
    ocrResult.value = error?.message || 'OCR识别失败';
    message.error(error?.message || 'OCR识别失败');
  } finally {
    ocrLoading.value = false;
  }
}

// 复制OCR结果到剪贴板
function handleCopyOcrResult() {
  if (ocrResult.value) {
    navigator.clipboard.writeText(ocrResult.value);
    message.success('已复制到剪贴板');
  }
}

// 编辑文档（重命名）
function handleEdit(record: DocumentDTO) {
  editFormData.id = record.id;
  editFormData.title = record.title || record.fileName || record.name;
  editFormData.description = record.description || '';
  editModalVisible.value = true;
}

// 分享文档
async function handleShare(record: DocumentDTO) {
  try {
    currentDocument.value = record;
    const url = await shareDocument(record.id);
    shareUrl.value = url;
    shareModalVisible.value = true;
  } catch {
    message.error('生成分享链接失败');
  }
}

// 查看版本
async function handleViewVersions(record: DocumentDTO) {
  try {
    currentDocument.value = record;
    const data = await getDocumentVersions(record.id);
    versions.value = data;
    versionModalVisible.value = true;
  } catch {
    message.error('加载版本列表失败');
  }
}

// 删除文档
function handleDelete(record: DocumentDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除文档 "${record.title || record.fileName || record.name}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDocument(record.id);
        message.success('删除成功');
        await loadProjectDocuments(); // 刷新文档列表
        await loadDossierItems(); // 刷新目录计数
      } catch {
        message.error('删除失败');
      }
    },
  });
}

// 拖拽排序结束
async function handleDragEnd() {
  // 获取排序后的文档ID列表
  const documentIds = currentDocuments.value.map((doc: DocumentDTO) => doc.id);

  try {
    // 保存排序到后端
    await reorderDocuments(documentIds);
    message.success('排序已保存');
  } catch (error: any) {
    message.error(`排序保存失败: ${error.message || '未知错误'}`);
    // 重新加载恢复原顺序
    await loadProjectDocuments();
  }
}

// 移动文件到指定目录
function handleMoveDocument(record: DocumentDTO) {
  if (dossierItems.value.length === 0) {
    message.warning('请先初始化卷宗目录');
    return;
  }

  // 构建目录选项
  const options = dossierItems.value
    .filter((item) => item.itemType === 'FOLDER')
    .map((item) => ({
      label: item.name,
      value: item.id,
    }));

  Modal.confirm({
    title: '移动文件',
    content: h('div', { style: 'padding: 10px 0' }, [
      h('p', `将 "${record.title || record.fileName}" 移动到：`),
      h(
        'select',
        {
          id: 'moveTargetSelect',
          style:
            'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;',
        },
        options.map((opt) => h('option', { value: opt.value }, opt.label)),
      ),
    ]),
    onOk: async () => {
      const select = document.querySelector(
        '#moveTargetSelect',
      ) as HTMLSelectElement;
      const targetId = Number.parseInt(select?.value, 10);

      if (!targetId || isNaN(targetId)) {
        message.error('请选择目标目录');
        throw undefined;
      }

      try {
        await moveDocument(record.id, targetId);
        message.success('移动成功');
        loadProjectDocuments();
        await loadDossierItems(); // 刷新目录计数
      } catch (error: any) {
        message.error(`移动失败: ${error.message || '未知错误'}`);
        return Promise.reject();
      }
    },
  });
}

// 上传签字版本
function handleUploadSignedVersion(record: DocumentDTO) {
  if (!selectedMatter.value) {
    message.warning('请先选择项目');
    return;
  }

  // 创建文件输入元素
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.pdf,.jpg,.jpeg,.png';
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (!file) return;

    try {
      loading.value = true;
      // 上传签字版本，传递 sourceType 参数
      await uploadFiles([file], {
        matterId: selectedMatter.value!.id,
        dossierItemId: record.dossierItemId,
        description: `${record.title || record.fileName} 的签字版本`,
        sourceType: 'SIGNED_VERSION', // 标记为签字版本
      });
      message.success('签字版本上传成功');
      await loadProjectDocuments();
      await loadDossierItems();
    } catch (error: any) {
      message.error(`上传失败: ${error.message || '未知错误'}`);
    } finally {
      loading.value = false;
    }
  };
  input.click();
}

// 保存上传
async function handleSaveUpload() {
  if (fileList.value.length === 0) {
    message.error('请选择要上传的文件');
    return;
  }

  try {
    // 提取文件对象
    const files: File[] = fileList.value.map((f) => f.originFileObj || f);

    // 调用批量上传 API，传递 dossierItemId 以关联到正确的卷宗目录
    await uploadFiles(files, {
      matterId: uploadFormData.matterId,
      folder: uploadFormData.folder,
      description: uploadFormData.description,
      dossierItemId: uploadFormData.dossierItemId,
    });

    message.success(`成功上传 ${files.length} 个文件`);
    uploadModalVisible.value = false;
    fileList.value = [];
    loadProjectDocuments(); // 刷新文档列表
    await loadDossierItems(); // 刷新目录计数
  } catch (error: any) {
    console.error('上传失败:', error);
    message.error(`上传失败: ${error.message || '未知错误'}`);
  }
}

// 保存文件夹
async function handleSaveFolder() {
  if (!folderFormData.name.trim()) {
    message.error('请输入文件夹名称');
    return;
  }

  try {
    await createFolder({
      name: folderFormData.name,
      parentFolder: folderFormData.parentFolder,
      matterId: selectedMatter.value!.id,
    });
    message.success('创建成功');
    folderModalVisible.value = false;
    // 刷新文件夹树
  } catch {
    message.error('创建失败');
  }
}

// 保存编辑
async function handleSaveEdit() {
  if (!editFormData.title.trim()) {
    message.error('请输入文档名称');
    return;
  }

  try {
    await updateDocument(editFormData.id!, {
      id: editFormData.id!,
      title: editFormData.title,
      description: editFormData.description,
    });
    message.success('更新成功');
    editModalVisible.value = false;
    await loadProjectDocuments();
  } catch (error: any) {
    message.error(`更新失败: ${error.message || '未知错误'}`);
  }
}

// 格式化文件大小
function formatFileSize(bytes?: number) {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

// 切换视图模式
function toggleViewMode(mode: 'grid' | 'list') {
  viewMode.value = mode;
}

// 处理悬停预览 - 鼠标进入
function handleDocHover(doc: DocumentDTO, event: MouseEvent) {
  // 只对图片和 PDF 类型显示预览
  if (!isPreviewableFile(doc.fileType)) return;

  if (hoverTimeout) {
    clearTimeout(hoverTimeout);
  }

  hoverTimeout = setTimeout(() => {
    const rect = (event.target as HTMLElement).getBoundingClientRect();
    hoverPreview.value = {
      doc,
      show: true,
      x: rect.right + 10,
      y: rect.top,
    };
  }, 300); // 300ms 延迟显示
}

// 处理悬停预览 - 鼠标离开
function handleDocHoverLeave() {
  if (hoverTimeout) {
    clearTimeout(hoverTimeout);
    hoverTimeout = null;
  }
  hoverPreview.value.show = false;
}

// 判断文件是否可预览（图片、PDF）
function isPreviewableFile(fileType?: string): boolean {
  if (!fileType) return false;
  const type = fileType.toLowerCase();
  return ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'pdf'].includes(
    type,
  );
}

// 获取预览图片URL（用于悬停预览）
function getPreviewImageUrl(doc: DocumentDTO): string {
  // 如果有缩略图，优先使用缩略图
  if (doc.thumbnailUrl) {
    return doc.thumbnailUrl;
  }
  // 如果是图片文件，直接使用原图
  if (isImageFile(doc.fileType)) {
    return doc.filePath || '';
  }
  return '';
}

// 图片预览滚轮缩放
function handleImageWheel(e: WheelEvent) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1;
  const newZoom = imageZoom.value + delta;
  if (newZoom >= 0.2 && newZoom <= 3) {
    imageZoom.value = newZoom;
  }
}

// 文件上传前处理
function beforeUpload(file: any) {
  const isLt100M = file.size / 1024 / 1024 < 100;
  if (!isLt100M) {
    message.error('文件大小不能超过 100MB');
    return false;
  }
  return false; // 阻止自动上传
}

// 文件列表变化
function handleFileChange(info: any) {
  fileList.value = info.fileList;
}

// 复制分享链接
function copyShareUrl() {
  navigator.clipboard.writeText(shareUrl.value);
  message.success('链接已复制到剪贴板');
}

onMounted(() => {
  loadMatters();
});
</script>

<template>
  <Page
    title="卷宗管理"
    description="按项目管理卷宗文件，支持预览、编辑、归档等功能"
  >
    <!-- 筛选条件 -->
    <Card class="mb-4">
      <Row :gutter="[16, 16]" align="middle">
        <Col :xs="24" :sm="12" :md="6" :lg="4" :xl="4">
          <Select
            v-model:value="filterParams.year"
            placeholder="创建年份"
            style="width: 100%"
            :options="yearOptions"
            @change="handleYearChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="3" :xl="3">
          <Select
            v-model:value="filterParams.matterType"
            placeholder="项目大类"
            allow-clear
            style="width: 100%"
            :options="matterTypeOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="3" :xl="3">
          <Select
            v-model:value="filterParams.caseType"
            placeholder="案件类型"
            allow-clear
            style="width: 100%"
            :options="caseTypeOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="3" :xl="3">
          <Select
            v-model:value="filterParams.status"
            placeholder="项目状态"
            allow-clear
            style="width: 100%"
            :options="statusOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Input
            v-model:value="filterParams.keyword"
            placeholder="搜索项目名称或客户"
            allow-clear
            @press-enter="handleFilter"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Space>
            <Button type="primary" @click="handleFilter">筛选</Button>
            <Button @click="handleResetFilter">重置</Button>
            <Button v-if="selectedMatter" @click="handleBackToList">
              返回列表
            </Button>
            <Button
              v-if="selectedMatter"
              type="link"
              @click="handleGoToMatterDetail"
            >
              返回项目详情
            </Button>
          </Space>
        </Col>
      </Row>
    </Card>

    <!-- 项目列表（第一层：选择项目） -->
    <Card v-if="!selectedMatter" class="mt-4">
      <template #title>
        <Space>
          <Inbox />
          项目列表
          <Tag color="blue">{{ filteredMatters.length }} 个项目</Tag>
        </Space>
      </template>

      <div v-if="loading" style="padding: 40px; text-align: center">
        加载中...
      </div>

      <div
        v-else-if="filteredMatters.length === 0"
        style="padding: 40px; color: #999; text-align: center"
      >
        暂无符合条件的项目
      </div>

      <Row v-else :gutter="[16, 16]">
        <Col
          v-for="matter in filteredMatters"
          :key="matter.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          :xl="6"
        >
          <Card
            hoverable
            size="small"
            @click="handleSelectMatter(matter)"
            class="matter-card"
            :class="`matter-card-${matter.caseType || 'default'}`"
          >
            <template #title>
              <div
                style="
                  overflow: hidden;
                  text-overflow: ellipsis;
                  font-size: 14px;
                  font-weight: 500;
                  white-space: nowrap;
                "
              >
                {{ matter.name }}
              </div>
            </template>
            <template #extra>
              <Tag
                :color="
                  matter.status === 'ACTIVE'
                    ? 'green'
                    : matter.status === 'CLOSED'
                      ? 'default'
                      : 'orange'
                "
                style="margin: 0"
              >
                {{ matter.statusName || getStatusName(matter.status) }}
              </Tag>
            </template>
            <div style="font-size: 12px; color: #666">
              <div style="margin-bottom: 6px">
                <span style="color: #999">项目编号：</span>
                <span style="font-weight: 500; color: #1890ff">{{
                  matter.matterNo || '-'
                }}</span>
              </div>
              <div style="margin-bottom: 6px">
                <span style="color: #999">客户：</span
                >{{ matter.clientName || '-' }}
              </div>
              <div
                style="margin-bottom: 6px"
                v-if="matter.causeOfActionName || matter.causeOfAction"
              >
                <span style="color: #999">案由：</span>
                <span style="color: #333">{{
                  getCauseOfActionName(matter)
                }}</span>
              </div>
              <div style="margin-bottom: 6px" v-if="matter.leadLawyerName">
                <span style="color: #999">承办律师：</span
                >{{ matter.leadLawyerName }}
              </div>
              <div style="margin-bottom: 6px" v-if="matter.opposingParty">
                <span style="color: #999">对方：</span>
                <span style="color: #666">{{ matter.opposingParty }}</span>
              </div>
              <div
                style="
                  display: flex;
                  justify-content: space-between;
                  padding-top: 8px;
                  margin-top: 8px;
                  border-top: 1px solid #f0f0f0;
                "
              >
                <div>
                  <span style="color: #999">类型：</span>
                  <Tag
                    :color="getMatterCoverTheme(matter).primaryColor"
                    style="
                      padding: 0 4px;
                      margin: 0;
                      font-size: 10px;
                      border: none;
                    "
                  >
                    {{
                      matter.caseTypeName || getCaseTypeName(matter.caseType)
                    }}
                  </Tag>
                </div>
                <div style="font-size: 11px; color: #999">
                  {{
                    formatDate(matter.filingDate) ||
                    formatDate(matter.createdAt) ||
                    '-'
                  }}
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </Card>

    <!-- 卷宗目录（第二层：查看项目卷宗） -->
    <Card v-if="selectedMatter" class="mt-4">
      <template #title>
        <Space>
          <Inbox />
          {{ selectedMatter.name }} - 文档管理
          <Tag color="orange">
            {{
              selectedMatter.caseTypeName ||
              getCaseTypeName(selectedMatter.caseType)
            }}
          </Tag>
          <Tag color="blue">{{ documentStats.total }} 个文档</Tag>
          <Tag color="green">{{ documentStats.size }}</Tag>
        </Space>
      </template>

      <template #extra>
        <Space>
          <Button type="primary" @click="handleUpload">
            <ArrowUp />
            上传文档
          </Button>
          <Button @click="handleCreateFolder">
            <Plus />
            新建文件夹
          </Button>
        </Space>
      </template>

      <Row :gutter="16">
        <!-- 左侧文件夹树 -->
        <Col :span="6">
          <Card size="small">
            <template #title>
              <Space>
                <span>卷宗目录</span>
                <Button
                  v-if="dossierItems.length === 0"
                  type="link"
                  size="small"
                  @click="handleInitDossier"
                  style="padding: 0"
                >
                  初始化
                </Button>
              </Space>
            </template>
            <template #extra>
              <Space size="small">
                <Button
                  size="small"
                  @click="handleAddDossierItem"
                  title="添加文件夹"
                >
                  <Plus :size="14" />
                </Button>
                <!-- 更多操作下拉菜单 -->
                <Dropdown v-if="dossierItems.length > 0">
                  <Button size="small" :loading="regenerating || archiving">
                    ⚙️
                  </Button>
                  <template #overlay>
                    <Menu>
                      <MenuItem
                        key="regenerate-poa"
                        @click="handleRegeneratePOA"
                      >
                        🔄 重新生成授权委托书
                      </MenuItem>
                      <MenuItem
                        key="trigger-archive"
                        @click="handleTriggerArchive"
                      >
                        📦 触发自动归档
                      </MenuItem>
                    </Menu>
                  </template>
                </Dropdown>
              </Space>
            </template>

            <Tree
              :tree-data="folderTreeData"
              :selected-keys="[selectedFolder]"
              @select="handleFolderSelect"
              :show-icon="true"
              default-expand-all
            >
              <template
                #title="{ title, key, dossierItemId, name, documentCount }"
              >
                <Dropdown :trigger="['contextmenu']" v-if="key !== 'root'">
                  <span>{{ title }}</span>
                  <template #overlay>
                    <Menu>
                      <MenuItem
                        @click="
                          handleRenameDossierItem({ dossierItemId, name })
                        "
                      >
                        重命名
                      </MenuItem>
                      <MenuItem @click="handleMoveUp({ dossierItemId })">
                        上移
                      </MenuItem>
                      <MenuItem @click="handleMoveDown({ dossierItemId })">
                        下移
                      </MenuItem>
                      <Divider style="margin: 4px 0" />
                      <MenuItem
                        @click="
                          handleDeleteDossierItem({
                            dossierItemId,
                            name,
                            documentCount,
                          })
                        "
                        style="color: red"
                        :disabled="documentCount > 0"
                      >
                        删除
                      </MenuItem>
                    </Menu>
                  </template>
                </Dropdown>
                <span v-else>{{ title }}</span>
              </template>
            </Tree>
          </Card>
        </Col>

        <!-- 右侧文档列表 -->
        <Col :span="18">
          <Card size="small">
            <template #title>
              <Space>
                <span>{{ currentPath.join(' / ') }}</span>
                <Tag color="blue">{{ currentDocuments.length }} 个文档</Tag>
                <Tag v-if="selectedDocIds.size > 0" color="green">
                  已选 {{ selectedDocIds.size }} 个
                </Tag>
              </Space>
            </template>
            <template #extra>
              <Space>
                <!-- 批量操作按钮 -->
                <template v-if="selectedDocIds.size > 0">
                  <Button
                    type="primary"
                    size="small"
                    :loading="batchDownloading"
                    @click="handleBatchDownload"
                  >
                    <SvgDownloadIcon :size="14" style="margin-right: 4px" />
                    批量下载 ({{ selectedDocIds.size }})
                  </Button>
                  <Button size="small" @click="clearSelection">取消选择</Button>
                </template>
                <!-- 视图模式切换 -->
                <div class="view-mode-switch">
                  <Tooltip title="网格视图">
                    <Button
                      :type="viewMode === 'grid' ? 'primary' : 'default'"
                      size="small"
                      @click="toggleViewMode('grid')"
                    >
                      <span style="font-size: 14px">▦</span>
                    </Button>
                  </Tooltip>
                  <Tooltip title="列表视图">
                    <Button
                      :type="viewMode === 'list' ? 'primary' : 'default'"
                      size="small"
                      @click="toggleViewMode('list')"
                    >
                      <span style="font-size: 14px">☰</span>
                    </Button>
                  </Tooltip>
                </div>
              </Space>
            </template>

            <!-- 加载中/空状态 -->
            <div v-if="loading" style="padding: 40px; text-align: center">
              加载中...
            </div>
            <div
              v-else-if="currentDocuments.length === 0"
              style="padding: 40px; color: #999; text-align: center"
            >
              暂无文档
            </div>

            <!-- ==================== 列表视图 ==================== -->
            <template v-else-if="viewMode === 'list'">
              <!-- 文档列表头部 -->
              <div class="doc-list-header">
                <div class="col-checkbox" style="width: 32px">
                  <input
                    type="checkbox"
                    :checked="isAllSelected()"
                    @change="toggleSelectAll"
                    style="width: 16px; height: 16px; cursor: pointer"
                    title="全选/取消全选"
                  />
                </div>
                <div class="col-drag" style="width: 30px"></div>
                <div class="col-name" style="flex: 1">文档名称</div>
                <div class="col-type" style="width: 80px">类型</div>
                <div class="col-source" style="width: 80px">来源</div>
                <div class="col-size" style="width: 80px">大小</div>
                <div class="col-time" style="width: 140px">修改时间</div>
                <div class="col-action" style="width: 180px">操作</div>
              </div>
              <!-- 可拖拽文档列表 -->
              <draggable
                v-model="currentDocuments"
                item-key="id"
                handle=".drag-handle"
                @end="handleDragEnd"
                class="doc-list"
              >
                <template #item="{ element: record }">
                  <div
                    class="doc-item"
                    :class="{ 'doc-item-selected': isDocSelected(record.id) }"
                  >
                    <!-- 复选框 -->
                    <div class="col-checkbox" style="width: 32px">
                      <input
                        type="checkbox"
                        :checked="isDocSelected(record.id)"
                        @change="toggleDocSelection(record.id)"
                        @click.stop
                        style="width: 16px; height: 16px; cursor: pointer"
                      />
                    </div>

                    <!-- 拖拽手柄 -->
                    <div class="col-drag drag-handle">
                      <GripVertical
                        :size="16"
                        style="color: #bbb; cursor: grab"
                      />
                    </div>

                    <!-- 文档名称 -->
                    <div
                      class="col-name"
                      style="
                        display: flex;
                        flex: 1;
                        gap: 10px;
                        align-items: center;
                        min-width: 0;
                      "
                    >
                      <!-- 缩略图或文件类型图标 -->
                      <div class="doc-thumbnail" style="flex-shrink: 0">
                        <img
                          v-if="record.thumbnailUrl"
                          :src="record.thumbnailUrl"
                          :alt="record.fileName"
                          class="thumbnail-img"
                          @error="
                            (e: Event) =>
                              ((e.target as HTMLImageElement).style.display =
                                'none')
                          "
                        />
                        <span v-else style="font-size: 26px; line-height: 1">{{
                          getFileTypeConfig(record.fileType).icon
                        }}</span>
                      </div>
                      <div
                        style="
                          display: flex;
                          flex-direction: column;
                          gap: 2px;
                          min-width: 0;
                          overflow: hidden;
                        "
                      >
                        <a
                          @click="handlePreview(record)"
                          class="doc-name-link"
                          :title="
                            record.title || record.fileName || record.name
                          "
                        >
                          {{ record.title || record.fileName || record.name }}
                        </a>
                        <span class="doc-desc" v-if="record.description">{{
                          record.description
                        }}</span>
                      </div>
                    </div>

                    <!-- 文件类型 -->
                    <div class="col-type">
                      <Tag
                        :color="getFileTypeConfig(record.fileType).color"
                        style="padding: 1px 6px; margin: 0; font-size: 11px"
                      >
                        {{ getFileTypeConfig(record.fileType).label }}
                      </Tag>
                    </div>

                    <!-- 文档来源 -->
                    <div class="col-source">
                      <Tag
                        :color="getSourceTypeConfig(record.sourceType).color"
                        style="padding: 1px 6px; margin: 0; font-size: 11px"
                      >
                        {{ getSourceTypeConfig(record.sourceType).label }}
                      </Tag>
                    </div>

                    <!-- 文件大小 -->
                    <div class="col-size">
                      {{ formatFileSize(record.fileSize) }}
                    </div>

                    <!-- 修改时间 -->
                    <div class="col-time">
                      {{ formatDateTime(record.updatedAt) }}
                    </div>

                    <!-- 操作按钮 -->
                    <div class="col-action">
                      <Space :size="2">
                        <Tooltip title="预览">
                          <Button
                            type="text"
                            size="small"
                            @click="handlePreview(record)"
                            class="action-btn"
                          >
                            <Eye :size="15" />
                          </Button>
                        </Tooltip>
                        <Tooltip
                          title="在线编辑"
                          v-if="isEditableFile(record.fileType)"
                        >
                          <Button
                            type="text"
                            size="small"
                            @click="handleOnlineEdit(record)"
                            class="action-btn"
                          >
                            <Edit :size="15" />
                          </Button>
                        </Tooltip>
                        <Tooltip title="下载">
                          <Button
                            type="text"
                            size="small"
                            @click="handleDownload(record)"
                            class="action-btn"
                          >
                            <SvgDownloadIcon :size="15" />
                          </Button>
                        </Tooltip>
                        <Dropdown placement="bottomRight">
                          <template #overlay>
                            <Menu class="action-menu">
                              <MenuItem
                                key="rename"
                                @click="handleEdit(record)"
                              >
                                <Edit :size="14" style="margin-right: 8px" />
                                重命名
                              </MenuItem>
                              <MenuItem
                                key="share"
                                @click="handleShare(record)"
                              >
                                <span style="margin-right: 8px">🔗</span>
                                分享
                              </MenuItem>
                              <MenuItem
                                key="versions"
                                @click="handleViewVersions(record)"
                              >
                                <span style="margin-right: 8px">📋</span>
                                版本历史
                              </MenuItem>
                              <MenuItem
                                key="move"
                                @click="handleMoveDocument(record)"
                              >
                                <span style="margin-right: 8px">📁</span>
                                移动
                              </MenuItem>
                              <MenuItem
                                v-if="record.sourceType === 'SYSTEM_GENERATED'"
                                key="upload-signed"
                                @click="handleUploadSignedVersion(record)"
                              >
                                <span style="margin-right: 8px">✍️</span>
                                上传签字版本
                              </MenuItem>
                              <MenuItem
                                v-if="isImageFile(record.fileType)"
                                key="ocr"
                                @click="handleOcrExtract(record)"
                              >
                                <span style="margin-right: 8px">🔍</span>
                                提取文字(OCR)
                              </MenuItem>
                              <Divider style="margin: 6px 0" />
                              <MenuItem
                                key="delete"
                                @click="handleDelete(record)"
                                style="color: #ff4d4f"
                              >
                                <Trash :size="14" style="margin-right: 8px" />
                                删除
                              </MenuItem>
                            </Menu>
                          </template>
                          <Button type="text" size="small" class="action-btn">
                            <Ellipsis :size="15" />
                          </Button>
                        </Dropdown>
                      </Space>
                    </div>
                  </div>
                </template>
              </draggable>
            </template>

            <!-- ==================== 网格视图 ==================== -->
            <div v-else class="grid-view">
              <!-- 全选复选框 -->
              <div class="grid-select-all">
                <label class="select-all-label">
                  <input
                    type="checkbox"
                    :checked="isAllSelected()"
                    @change="toggleSelectAll"
                    style="
                      width: 16px;
                      height: 16px;
                      cursor: pointer;
                      margin-right: 8px;
                    "
                  />
                  全选
                </label>
              </div>

              <!-- 文档网格 -->
              <div class="grid-container">
                <div
                  v-for="doc in currentDocuments"
                  :key="doc.id"
                  class="grid-item"
                  :class="{ 'grid-item-selected': isDocSelected(doc.id) }"
                  @mouseenter="handleDocHover(doc, $event)"
                  @mouseleave="handleDocHoverLeave"
                  @click="handlePreview(doc)"
                >
                  <!-- 选择复选框 -->
                  <div class="grid-item-checkbox" @click.stop>
                    <input
                      type="checkbox"
                      :checked="isDocSelected(doc.id)"
                      @change="toggleDocSelection(doc.id)"
                      style="width: 16px; height: 16px; cursor: pointer"
                    />
                  </div>

                  <!-- 缩略图区域 -->
                  <div class="grid-thumbnail">
                    <!-- 图片文件 - 显示实际图片 -->
                    <img
                      v-if="
                        isImageFile(doc.fileType) &&
                        (doc.thumbnailUrl || doc.filePath)
                      "
                      :src="doc.thumbnailUrl || doc.filePath"
                      :alt="doc.fileName"
                      class="grid-thumbnail-img"
                      @error="
                        (e: Event) =>
                          ((e.target as HTMLImageElement).style.display =
                            'none')
                      "
                    />
                    <!-- PDF 文件 - 显示缩略图或 PDF 图标 -->
                    <template v-else-if="doc.fileType?.toLowerCase() === 'pdf'">
                      <img
                        v-if="doc.thumbnailUrl"
                        :src="doc.thumbnailUrl"
                        :alt="doc.fileName"
                        class="grid-thumbnail-img"
                        @error="
                          (e: Event) =>
                            ((e.target as HTMLImageElement).style.display =
                              'none')
                        "
                      />
                      <div v-else class="grid-thumbnail-icon pdf-icon">
                        <span class="file-icon">📄</span>
                        <span class="file-ext">PDF</span>
                      </div>
                    </template>
                    <!-- 其他文件 - 显示文件类型图标 -->
                    <div v-else class="grid-thumbnail-icon">
                      <span class="file-icon">{{
                        getFileTypeConfig(doc.fileType).icon
                      }}</span>
                      <span class="file-ext">{{
                        doc.fileType?.toUpperCase() || '?'
                      }}</span>
                    </div>
                  </div>

                  <!-- 文件名 -->
                  <div
                    class="grid-item-name"
                    :title="doc.title || doc.fileName || doc.name"
                  >
                    {{ doc.title || doc.fileName || doc.name }}
                  </div>

                  <!-- 文件信息 -->
                  <div class="grid-item-info">
                    <Tag
                      :color="getFileTypeConfig(doc.fileType).color"
                      size="small"
                      style="font-size: 10px; padding: 0 4px; margin: 0"
                    >
                      {{ getFileTypeConfig(doc.fileType).label }}
                    </Tag>
                    <span class="grid-item-size">{{
                      formatFileSize(doc.fileSize)
                    }}</span>
                  </div>

                  <!-- 快捷操作按钮 -->
                  <div class="grid-item-actions" @click.stop>
                    <Tooltip title="预览">
                      <Button
                        type="text"
                        size="small"
                        @click="handlePreview(doc)"
                        class="grid-action-btn"
                      >
                        <Eye :size="14" />
                      </Button>
                    </Tooltip>
                    <Tooltip title="下载">
                      <Button
                        type="text"
                        size="small"
                        @click="handleDownload(doc)"
                        class="grid-action-btn"
                      >
                        <SvgDownloadIcon :size="14" />
                      </Button>
                    </Tooltip>
                    <Dropdown placement="bottomRight">
                      <template #overlay>
                        <Menu class="action-menu">
                          <MenuItem key="rename" @click="handleEdit(doc)">
                            <Edit :size="14" style="margin-right: 8px" />
                            重命名
                          </MenuItem>
                          <MenuItem
                            v-if="isEditableFile(doc.fileType)"
                            key="online-edit"
                            @click="handleOnlineEdit(doc)"
                          >
                            <Edit :size="14" style="margin-right: 8px" />
                            在线编辑
                          </MenuItem>
                          <MenuItem key="share" @click="handleShare(doc)">
                            <span style="margin-right: 8px">🔗</span>
                            分享
                          </MenuItem>
                          <Divider style="margin: 6px 0" />
                          <MenuItem
                            key="delete"
                            @click="handleDelete(doc)"
                            style="color: #ff4d4f"
                          >
                            <Trash :size="14" style="margin-right: 8px" />
                            删除
                          </MenuItem>
                        </Menu>
                      </template>
                      <Button type="text" size="small" class="grid-action-btn">
                        <Ellipsis :size="14" />
                      </Button>
                    </Dropdown>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </Card>

    <!-- 上传文档弹窗 -->
    <Modal
      v-model:open="uploadModalVisible"
      title="上传文档"
      width="600px"
      @ok="handleSaveUpload"
    >
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <FormItem label="选择文件" required>
          <Upload
            :file-list="fileList"
            :before-upload="beforeUpload"
            @change="handleFileChange"
            multiple
          >
            <Button>
              <ArrowUp />
              选择文件
            </Button>
          </Upload>
          <div style="margin-top: 8px; font-size: 12px; color: #666">
            支持多文件上传，单个文件不超过100MB
          </div>
        </FormItem>
        <FormItem label="存储位置">
          <Select
            v-model:value="uploadFormData.dossierItemId"
            style="width: 100%"
            :options="folderOptions"
            :field-names="{ label: 'label', value: 'value' }"
            @change="handleUploadFolderSelect"
            placeholder="请选择存储位置"
          />
        </FormItem>
        <FormItem label="描述">
          <Input.TextArea
            v-model:value="uploadFormData.description"
            placeholder="请输入文档描述"
            :rows="3"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 新建文件夹弹窗 -->
    <Modal
      v-model:open="folderModalVisible"
      title="新建文件夹"
      width="400px"
      @ok="handleSaveFolder"
    >
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <FormItem label="文件夹名称" required>
          <Input
            v-model:value="folderFormData.name"
            placeholder="请输入文件夹名称"
          />
        </FormItem>
        <FormItem label="父文件夹">
          <Select
            v-model:value="folderFormData.parentFolder"
            style="width: 100%"
            :options="folderOptions"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 编辑文档弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      title="编辑文档"
      width="500px"
      @ok="handleSaveEdit"
    >
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <FormItem label="文档名称" required>
          <Input
            v-model:value="editFormData.title"
            placeholder="请输入文档名称"
          />
        </FormItem>
        <FormItem label="描述">
          <Input.TextArea
            v-model:value="editFormData.description"
            placeholder="请输入文档描述"
            :rows="3"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 文档预览弹窗 -->
    <Modal
      v-model:open="previewModalVisible"
      :title="`预览 - ${currentDocument?.title || currentDocument?.fileName || currentDocument?.name}`"
      :width="isImageFile(currentDocument?.fileType) ? '90%' : '80%'"
      :footer="null"
      style="top: 20px"
      :class="{ 'image-preview-modal': isImageFile(currentDocument?.fileType) }"
    >
      <!-- 图片专属预览（支持放大缩小） -->
      <div
        v-if="isImageFile(currentDocument?.fileType)"
        class="image-preview-container"
      >
        <div class="image-preview-toolbar">
          <Space>
            <Button
              size="small"
              @click="imageZoom -= 0.2"
              :disabled="imageZoom <= 0.2"
            >
              <span style="font-size: 16px">−</span>
            </Button>
            <span class="zoom-label">{{ Math.round(imageZoom * 100) }}%</span>
            <Button
              size="small"
              @click="imageZoom += 0.2"
              :disabled="imageZoom >= 3"
            >
              <span style="font-size: 16px">+</span>
            </Button>
            <Button size="small" @click="imageZoom = 1"> 重置 </Button>
            <Button size="small" @click="handleDownload(currentDocument!)">
              <SvgDownloadIcon :size="14" />
              下载
            </Button>
          </Space>
        </div>
        <div class="image-preview-scroll">
          <img
            :src="currentDocument?.filePath"
            :alt="currentDocument?.fileName"
            class="preview-image"
            :style="{ transform: `scale(${imageZoom})` }"
            @wheel.prevent="handleImageWheel"
          />
        </div>
      </div>
      <!-- 其他类型文件预览 -->
      <div v-else style="height: 70vh">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          style="width: 100%; height: 100%; border: none"
        ></iframe>
        <div v-else style="padding: 50px; text-align: center">
          <div>暂不支持预览此类型文件</div>
          <Button
            type="primary"
            @click="handleDownload(currentDocument!)"
            style="margin-top: 16px"
          >
            <SvgDownloadIcon />
            下载文件
          </Button>
        </div>
      </div>
    </Modal>

    <!-- 分享链接弹窗 -->
    <Modal
      v-model:open="shareModalVisible"
      title="分享文档"
      width="500px"
      :footer="null"
    >
      <div>
        <div style="margin-bottom: 16px">
          <strong>{{ currentDocument?.name }}</strong>
        </div>
        <div style="margin-bottom: 16px">分享链接（7天内有效）：</div>
        <Input.Group compact>
          <Input :value="shareUrl" readonly style="width: calc(100% - 80px)" />
          <Button type="primary" @click="copyShareUrl">复制</Button>
        </Input.Group>
      </div>
    </Modal>

    <!-- 版本历史弹窗 -->
    <Modal
      v-model:open="versionModalVisible"
      :title="`版本历史 - ${currentDocument?.name}`"
      width="700px"
      :footer="null"
    >
      <Table
        :columns="[
          { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
          {
            title: '文件大小',
            dataIndex: 'fileSize',
            key: 'fileSize',
            width: 100,
          },
          {
            title: '修改时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            width: 160,
          },
          {
            title: '修改人',
            dataIndex: 'uploaderName',
            key: 'uploaderName',
            width: 100,
          },
          { title: '操作', key: 'action', width: 120 },
        ]"
        :data-source="versions"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }: any">
          <template v-if="column.key === 'fileSize'">
            {{ formatFileSize(record.fileSize) }}
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handlePreview(record)">预览</a>
              <a @click="handleDownload(record)">下载</a>
            </Space>
          </template>
        </template>
      </Table>
    </Modal>

    <!-- OCR识别结果弹窗 -->
    <Modal
      v-model:open="ocrModalVisible"
      :title="`OCR识别结果 - ${currentDocument?.fileName || currentDocument?.name || ''}`"
      width="700px"
    >
      <div v-if="ocrLoading" style="padding: 40px; text-align: center">
        <div style="font-size: 16px; color: #1890ff">正在识别中...</div>
        <div style="margin-top: 8px; color: #999">
          请稍候，OCR正在分析图片内容
        </div>
      </div>
      <div v-else>
        <div
          style="
            min-height: 200px;
            max-height: 400px;
            padding: 16px;
            overflow-y: auto;
            font-family: monospace;
            line-height: 1.8;
            word-break: break-all;
            white-space: pre-wrap;
            background: #f9f9f9;
            border-radius: 8px;
          "
        >
          {{ ocrResult || '未识别到文字内容' }}
        </div>
        <div style="margin-top: 12px; font-size: 12px; color: #999">
          提示：识别结果仅供参考，可能存在误差。如需精确内容，请人工校对。
        </div>
      </div>
      <template #footer>
        <Space>
          <Button @click="ocrModalVisible = false">关闭</Button>
          <Button
            type="primary"
            :disabled="!ocrResult"
            @click="handleCopyOcrResult"
          >
            复制文字
          </Button>
        </Space>
      </template>
    </Modal>

    <!-- 悬停预览弹层 -->
    <Teleport to="body">
      <div
        v-if="hoverPreview.show && hoverPreview.doc"
        class="hover-preview"
        :style="{
          left: `${hoverPreview.x}px`,
          top: `${hoverPreview.y}px`,
        }"
        @mouseenter="hoverPreview.show = true"
        @mouseleave="handleDocHoverLeave"
      >
        <div class="hover-preview-content">
          <!-- 图片预览 -->
          <img
            v-if="getPreviewImageUrl(hoverPreview.doc)"
            :src="getPreviewImageUrl(hoverPreview.doc)"
            :alt="hoverPreview.doc.fileName"
            class="hover-preview-img"
          />
          <!-- PDF 预览提示 -->
          <div
            v-else-if="hoverPreview.doc.fileType?.toLowerCase() === 'pdf'"
            class="hover-preview-placeholder"
          >
            <span style="font-size: 48px">📄</span>
            <div>PDF 文档</div>
            <div class="preview-hint">点击预览完整内容</div>
          </div>
        </div>
        <div class="hover-preview-info">
          <div class="preview-filename">
            {{ hoverPreview.doc.title || hoverPreview.doc.fileName }}
          </div>
          <div class="preview-meta">
            <Tag
              :color="getFileTypeConfig(hoverPreview.doc.fileType).color"
              size="small"
            >
              {{ getFileTypeConfig(hoverPreview.doc.fileType).label }}
            </Tag>
            <span>{{ formatFileSize(hoverPreview.doc.fileSize) }}</span>
          </div>
        </div>
      </div>
    </Teleport>
  </Page>
</template>

<style scoped>
/* 文档列表头部 */
.doc-list-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

/* 文档列表 */
.doc-list {
  min-height: 100px;
}

/* 单个文档项 */
.doc-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s;
}

.doc-item:hover {
  background-color: #f8fafc;
}

.doc-item-selected {
  background-color: #e6f7ff !important;
}

.doc-item:last-child {
  border-bottom: none;
}

/* 列宽度 */
.col-checkbox {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 32px;
}

.col-drag {
  flex-shrink: 0;
  width: 30px;
}

.col-type {
  flex-shrink: 0;
  width: 80px;
  text-align: center;
}

.col-size {
  flex-shrink: 0;
  width: 80px;
  font-size: 12px;
  color: #888;
  text-align: right;
}

.col-time {
  flex-shrink: 0;
  width: 140px;
  font-size: 12px;
  color: #888;
  text-align: right;
}

.col-action {
  flex-shrink: 0;
  width: 160px;
  text-align: right;
}

/* 拖拽手柄 */
.drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
}

.drag-handle:active {
  cursor: grabbing;
}

/* 文档名称链接 */
.doc-name-link {
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
  color: #1890ff;
  white-space: nowrap;
  cursor: pointer;
}

.doc-name-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 文档描述 */
.doc-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 11px;
  color: #999;
  white-space: nowrap;
}

/* 文档缩略图 */
.doc-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  background: #f5f5f5;
  border-radius: 4px;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

/* 操作按钮 */
.action-btn {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
  color: #666;
  border-radius: 6px;
}

.action-btn:hover {
  color: #1890ff !important;
  background-color: #e6f7ff !important;
}

/* 更多操作菜单 */
.action-menu {
  min-width: 140px;
}

.action-menu :deep(.ant-dropdown-menu-item) {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  font-size: 13px;
}

/* 拖拽中状态 */
.sortable-ghost {
  background: #e6f7ff;
  opacity: 0.4;
}

.sortable-drag {
  background: white;
  box-shadow: 0 4px 12px rgb(0 0 0 / 15%);
}

/* 项目卡片样式增强 */
.matter-card {
  border-left-width: 4px !important;
  transition: all 0.3s ease;
}

.matter-card:hover {
  box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
  transform: translateY(-2px);
}

/* 根据案件类型设置不同的左侧边框颜色和背景色 */
.matter-card-CRIMINAL {
  background: linear-gradient(to right, rgb(211 47 47 / 2%), #fff);
  border-left-color: #d32f2f !important;
}

.matter-card-CIVIL {
  background: linear-gradient(to right, rgb(25 118 210 / 2%), #fff);
  border-left-color: #1976d2 !important;
}

.matter-card-ADMINISTRATIVE {
  background: linear-gradient(to right, rgb(56 142 60 / 2%), #fff);
  border-left-color: #388e3c !important;
}

.matter-card-BANKRUPTCY {
  background: linear-gradient(to right, rgb(245 124 0 / 2%), #fff);
  border-left-color: #f57c00 !important;
}

.matter-card-IP {
  background: linear-gradient(to right, rgb(123 31 162 / 2%), #fff);
  border-left-color: #7b1fa2 !important;
}

.matter-card-ARBITRATION {
  background: linear-gradient(to right, rgb(2 136 209 / 2%), #fff);
  border-left-color: #0288d1 !important;
}

.matter-card-ENFORCEMENT {
  background: linear-gradient(to right, rgb(93 64 55 / 2%), #fff);
  border-left-color: #5d4037 !important;
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.LEGAL_COUNSEL 一致 */
.matter-card-LEGAL_COUNSEL {
  background: linear-gradient(to right, rgb(0 121 107 / 2%), #fff);
  border-left-color: #00796b !important;
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.SPECIAL_SERVICE 一致 */
.matter-card-SPECIAL_SERVICE {
  background: linear-gradient(to right, rgb(230 74 25 / 2%), #fff);
  border-left-color: #e64a19 !important;
}

.matter-card-default {
  background: #fff;
  border-left-color: #757575 !important;
}

/* ==================== 视图切换按钮 ==================== */
.view-mode-switch {
  display: flex;
  gap: 2px;
  padding: 2px;
  background: #f0f0f0;
  border-radius: 6px;
}

/* ==================== 网格视图样式 ==================== */
.grid-view {
  padding: 16px 0;
}

.grid-select-all {
  margin-bottom: 16px;
  padding: 0 8px;
}

.select-all-label {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  background: #fafafa;
  border-radius: 6px;
  transition: all 0.2s;
}

.select-all-label:hover {
  background: #f0f0f0;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 0 8px;
}

.grid-item {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 12px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  transition: all 0.25s ease;
}

.grid-item:hover {
  background: #f8fafc;
  border-color: #1890ff;
  box-shadow: 0 6px 20px rgb(24 144 255 / 12%);
  transform: translateY(-4px);
}

.grid-item-selected {
  background: #e6f7ff !important;
  border-color: #1890ff !important;
}

.grid-item-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.grid-item:hover .grid-item-checkbox,
.grid-item-selected .grid-item-checkbox {
  opacity: 1;
}

/* 网格缩略图区域 */
.grid-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 140px;
  margin-bottom: 12px;
  overflow: hidden;
  background: linear-gradient(145deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
}

.grid-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  transition: transform 0.3s ease;
}

.grid-item:hover .grid-thumbnail-img {
  transform: scale(1.05);
}

.grid-thumbnail-icon {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.grid-thumbnail-icon .file-icon {
  font-size: 48px;
  line-height: 1;
}

.grid-thumbnail-icon .file-ext {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  color: #666;
  background: rgb(0 0 0 / 8%);
  border-radius: 4px;
}

.grid-thumbnail-icon.pdf-icon .file-ext {
  color: #ff4d4f;
  background: rgb(255 77 79 / 10%);
}

/* 网格项文件名 */
.grid-item-name {
  display: -webkit-box;
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  color: #333;
  text-overflow: ellipsis;
  word-break: break-all;
  -webkit-box-orient: vertical;

  /* stylelint-disable-next-line property-no-vendor-prefix */
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

/* 网格项信息 */
.grid-item-info {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}

.grid-item-size {
  font-size: 11px;
  color: #999;
}

/* 网格项操作按钮 */
.grid-item-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 2px;
  padding: 4px;
  background: rgb(255 255 255 / 95%);
  border-radius: 6px;
  opacity: 0;
  box-shadow: 0 2px 8px rgb(0 0 0 / 10%);
  transition: opacity 0.2s;
}

.grid-item:hover .grid-item-actions {
  opacity: 1;
}

.grid-action-btn {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
  color: #666;
  border-radius: 4px;
}

.grid-action-btn:hover {
  color: #1890ff !important;
  background-color: #e6f7ff !important;
}

/* ==================== 悬停预览样式 ==================== */
.hover-preview {
  position: fixed;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  width: 320px;
  max-height: 400px;
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgb(0 0 0 / 20%);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hover-preview-content {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  background: linear-gradient(145deg, #f8f9fa 0%, #e9ecef 100%);
}

.hover-preview-img {
  width: 100%;
  height: auto;
  max-height: 280px;
  object-fit: contain;
}

.hover-preview-placeholder {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: #666;
  text-align: center;
}

.preview-hint {
  font-size: 12px;
  color: #999;
}

.hover-preview-info {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.preview-filename {
  margin-bottom: 8px;
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: #999;
}

/* ==================== 图片预览弹窗样式 ==================== */
.image-preview-container {
  display: flex;
  flex-direction: column;
  height: 80vh;
}

.image-preview-toolbar {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
}

.zoom-label {
  min-width: 50px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  font-size: 13px;
  color: #666;
  text-align: center;
}

.image-preview-scroll {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  overflow: auto;
  background:
    linear-gradient(
      45deg,
      #f0f0f0 25%,
      transparent 25%,
      transparent 75%,
      #f0f0f0 75%
    ),
    linear-gradient(
      45deg,
      #f0f0f0 25%,
      transparent 25%,
      transparent 75%,
      #f0f0f0 75%
    );
  background-color: #fafafa;
  background-position:
    0 0,
    10px 10px;
  background-size: 20px 20px;
  border-radius: 0 0 8px 8px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.15s ease;
  transform-origin: center center;
}

/* 图片预览模态框特殊样式 */
:deep(.image-preview-modal .ant-modal-body) {
  padding: 0;
  overflow: hidden;
  border-radius: 8px;
}
</style>
