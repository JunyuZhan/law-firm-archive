<script setup lang="ts">
import type { MatterDossierItem } from '#/api/document/dossier';
import type { DocumentDTO } from '#/api/document/types';
import type { MatterDTO, MatterSimpleDTO } from '#/api/matter/types';
import type { OcrResultDTO } from '#/api/ocr';

import { computed, h, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

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

const route = useRoute();
const router = useRouter();

// 状态管理
const loading = ref(false);
// 跟踪文件输入元素，用于清理（防止内存泄漏）
const activeFileInputs = new Set<{
  handler: (e: Event) => void;
  input: HTMLInputElement;
}>();
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

// 标记是否正在从路由恢复状态，避免 watch 触发循环
const isRestoringFromRoute = ref(false);

// 视图模式状态: 'list' | 'grid'
const viewMode = ref<'grid' | 'list'>('grid');

// 图片预览缩放
const imageZoom = ref(1);

// 弹窗状态
const uploadModalVisible = ref(false);
const uploadSubmitting = ref(false);
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
  const items = [...dossierItems.value].toSorted(
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
    dossierItems.value = items || [];
  } catch (error) {
    console.error('加载卷宗目录失败:', error);
    dossierItems.value = [];
    message.error('加载卷宗目录失败');
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
        throw new Error('操作失败');
      }

      try {
        await addDossierItem(selectedMatter.value!.id, { name, parentId: 0 });
        await loadDossierItems();
        message.success('文件夹创建成功');
      } catch (error: any) {
        message.error(`创建失败: ${error.message || '未知错误'}`);
        throw error;
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
        throw new Error('操作失败');
      }

      try {
        await updateDossierItem(selectedMatter.value!.id, item.dossierItemId, {
          name,
        });
        await loadDossierItems();
        message.success('重命名成功');
      } catch (error: any) {
        message.error(`重命名失败: ${error.message || '未知错误'}`);
        throw error;
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
    .toSorted((a, b) => a.sortOrder - b.sortOrder);
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
    .toSorted((a, b) => a.sortOrder - b.sortOrder);
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
    sortableDocuments.value = [...projectDocs].toSorted(
      (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0),
    );
    return;
  }

  // 否则只显示特定 dossierItemId 的文档
  const dossierItemId = Number.parseInt(selectedFolder.value, 10);
  if (!Number.isNaN(dossierItemId)) {
    sortableDocuments.value = projectDocs
      .filter((doc) => doc.dossierItemId === dossierItemId)
      .toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    return;
  }

  sortableDocuments.value = [...projectDocs].toSorted(
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
  // 注意：路由参数更新由 watch 监听器自动处理，避免在这里更新导致循环
}

// 返回项目列表
function handleBackToList() {
  // 设置标记，避免 watch 监听器触发路由更新导致页面刷新
  isRestoringFromRoute.value = true;

  try {
    // 清除选中状态
    selectedMatterId.value = undefined;
    selectedMatter.value = null;
    selectedFolder.value = 'root';
    currentPath.value = ['根目录'];
    documents.value = [];
    dossierItems.value = [];

    // 手动清除路由参数（避免 watch 触发导致的路由更新）
    const newQuery = { ...route.query };
    delete newQuery.matterId;
    delete newQuery.folderId;
    router.replace({ query: newQuery });
  } finally {
    // 重置标记
    isRestoringFromRoute.value = false;
  }
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
  pdf: { icon: '📕', color: '#e74c3c', label: 'PDF' },
  doc: { icon: '📘', color: '#2b579a', label: 'Word' },
  docx: { icon: '📘', color: '#2b579a', label: 'Word' },
  xls: { icon: '📗', color: '#217346', label: 'Excel' },
  xlsx: { icon: '📗', color: '#217346', label: 'Excel' },
  csv: { icon: '📗', color: '#217346', label: 'CSV' },
  ods: { icon: '📗', color: '#217346', label: 'ODS' },
  ppt: { icon: '📙', color: '#d24726', label: 'PPT' },
  pptx: { icon: '📙', color: '#d24726', label: 'PPT' },
  odp: { icon: '📙', color: '#d24726', label: 'ODP' },
  txt: { icon: '📄', color: '#666666', label: '文本' },
  rtf: { icon: '📄', color: '#666666', label: 'RTF' },
  odt: { icon: '📘', color: '#0066cc', label: 'ODT' },
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
  // 校验日期是否有效
  if (Number.isNaN(date.getTime())) return '-';
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

    matters.value = res && res.list ? res.list : [];
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
    // 安全访问 API 响应
    documents.value = res?.list ?? [];
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
        selectedMatter.value?.name
          ? `${selectedMatter.value.name} 卷宗目录`
          : '卷宗目录',
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
    // 注意：路由参数更新由 watch 监听器自动处理，避免在这里更新导致循环
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
      .toSorted((a, b) => a.sortOrder - b.sortOrder)
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
    message.error('获取卷宗目录失败');
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
  // 移除点号并转为小写
  const ext = fileType.toLowerCase().replace('.', '').trim();
  // PDF 不在 Office 文件列表中，使用浏览器直接预览
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
  // 获取文件扩展名（优先从 fileType，如果没有则从 fileName 提取）
  let fileExt = '';

  // 方法1: 从 fileType 字段提取
  if (record.fileType) {
    fileExt = record.fileType.toLowerCase().replace(/^\.+/, '').trim();
  }

  // 方法2: 如果 fileType 为空或无效，从 fileName 提取
  if (!fileExt && record.fileName) {
    const match = record.fileName.match(/\.([^.]+)$/);
    if (match && match[1]) {
      fileExt = match[1].toLowerCase();
    }
  }

  // 方法3: 如果还是为空，从 filePath 提取
  if (!fileExt && record.filePath) {
    const match = record.filePath.match(/\.([^.]+)(?:\?|$)/);
    if (match && match[1]) {
      fileExt = match[1].toLowerCase();
    }
  }

  // PDF 类型 - 优先处理，使用弹窗预览（像图片一样在浏览器中预览）
  if (fileExt === 'pdf') {
    try {
      const { previewUrl: previewUrlResult } = await getDocumentPreviewUrl(
        record.id,
      );
      const fileUrl = previewUrlResult;
      currentDocument.value = { ...record, filePath: fileUrl };
      previewUrl.value = fileUrl;
      previewModalVisible.value = true;
      return;
    } catch (error: any) {
      message.error(`获取预览链接失败: ${error.message || '未知错误'}`);
      return;
    }
  }

  // Office 文档类型 - 在弹窗中使用 iframe 嵌入 OnlyOffice 预览（确保不是PDF）
  if (fileExt && fileExt !== 'pdf' && isOfficeFile(fileExt)) {
    // 构建 OnlyOffice 预览页面的 URL（嵌入模式，隐藏工具栏）
    // 添加时间戳参数防止 iframe 缓存
    const timestamp = Date.now();
    const resolved = router.resolve({
      path: '/office-preview',
      query: {
        documentId: String(record.id),
        mode: 'view',
        embed: 'true',
        _t: String(timestamp), // 添加时间戳防止缓存
      },
    });
    // 在弹窗中使用 iframe 预览，而不是新窗口打开
    currentDocument.value = { ...record, filePath: resolved.href };
    previewUrl.value = resolved.href;
    previewModalVisible.value = true;
    return;
  }

  // 对于非 Office 文件，先获取预签名 URL
  try {
    const { previewUrl } = await getDocumentPreviewUrl(record.id);
    const fileUrl = previewUrl;

    // 图片类型 - 使用专用图片预览弹窗（支持缩放）
    if (isImageFile(fileExt)) {
      currentDocument.value = { ...record, filePath: fileUrl };
      imageZoom.value = 1; // 重置缩放
      previewModalVisible.value = true;
      return;
    }

    // 视频类型 - 使用 HTML5 Video 播放
    if (isVideoFile(fileExt)) {
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
    if (isAudioFile(fileExt)) {
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

    // 其他类型 - 尝试直接下载或打开
    message.info('该文件类型不支持在线预览，将尝试下载');
    window.open(fileUrl, '_blank');
  } catch (error: any) {
    message.error(`获取预览链接失败: ${error.message || '未知错误'}`);
  }
}

// 关闭预览弹窗时的处理（清空缓存）
function handlePreviewModalClose() {
  previewModalVisible.value = false;
  // 清空预览 URL 和当前文档，防止 iframe 缓存
  previewUrl.value = '';
  currentDocument.value = null;
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

// 从预览弹框打开编辑器（关闭弹框后在新窗口打开编辑页面）
async function handleOpenInEditor(record: DocumentDTO) {
  // 先关闭预览弹框
  previewModalVisible.value = false;

  // 然后调用在线编辑
  await handleOnlineEdit(record);
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

      if (!targetId || Number.isNaN(targetId)) {
        message.error('请选择目标目录');
        throw new Error('操作失败');
      }

      try {
        await moveDocument(record.id, targetId);
        message.success('移动成功');
        loadProjectDocuments();
        await loadDossierItems(); // 刷新目录计数
      } catch (error: any) {
        message.error(`移动失败: ${error.message || '未知错误'}`);
        throw error;
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

  // 用于跟踪和清理的引用
  let inputRef: null | {
    handler: (e: Event) => void;
    input: HTMLInputElement;
  } = null;

  // 定义处理函数，便于移除监听器
  const handleChange = async (e: Event) => {
    // 移除监听器，防止内存泄漏
    input.removeEventListener('change', handleChange);
    // 从跟踪集合中移除
    if (inputRef) {
      activeFileInputs.delete(inputRef);
      inputRef = null;
    }

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

  // 跟踪输入元素和处理函数，以便在组件卸载时清理
  inputRef = { input, handler: handleChange };
  activeFileInputs.add(inputRef);

  input.addEventListener('change', handleChange);
  input.click();
}

// 保存上传
async function handleSaveUpload() {
  if (fileList.value.length === 0) {
    message.error('请选择要上传的文件');
    return;
  }

  uploadSubmitting.value = true;
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
  } finally {
    uploadSubmitting.value = false;
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

// 从路由参数恢复状态
async function restoreStateFromRoute(skipLoadMatters = false) {
  const matterId = route.query.matterId as string | undefined;
  const folderId = route.query.folderId as string | undefined;

  if (matterId) {
    const matterIdNum = Number.parseInt(matterId, 10);
    if (!Number.isNaN(matterIdNum)) {
      // 如果项目列表还未加载，先加载
      if (!skipLoadMatters && matters.value.length === 0) {
        await loadMatters();
      }

      // 查找对应的项目
      const matter = matters.value.find((m) => m.id === matterIdNum);
      if (matter) {
        // 设置标记，避免 watch 触发循环
        isRestoringFromRoute.value = true;

        try {
          // 如果当前选中的项目不同，才切换
          if (selectedMatterId.value === matterIdNum) {
            // 项目已选中，但需要确保文档列表已加载（刷新后可能为空）
            if (documents.value.length === 0) {
              await loadProjectDocuments();
            }
            // 确保目录已加载
            if (dossierItems.value.length === 0) {
              await loadDossierItems();
            }
          } else {
            // 直接设置状态，不调用 handleSelectMatter（避免触发 watch）
            selectedMatterId.value = matter.id;
            selectedMatter.value = matter;
            selectedFolder.value = 'root';
            currentPath.value = [`${matter.name} 卷宗目录`];

            // 加载卷宗目录和文档
            await loadDossierItems();
            await loadProjectDocuments();
          }

          // 如果指定了文件夹，选择对应的文件夹
          if (
            folderId &&
            folderId !== 'root' &&
            selectedFolder.value !== folderId
          ) {
            const folderIdNum = Number.parseInt(folderId, 10);
            if (!Number.isNaN(folderIdNum)) {
              // 等待目录加载完成
              if (dossierItems.value.length === 0) {
                await loadDossierItems();
              }

              // 检查文件夹是否存在
              const folderExists = dossierItems.value.some(
                (item) => item.id === folderIdNum,
              );
              if (folderExists) {
                selectedFolder.value = folderId;
                // 更新面包屑路径
                const item = dossierItems.value.find(
                  (i) => i.id === folderIdNum,
                );
                if (item) {
                  currentPath.value = [
                    selectedMatter.value?.name || '卷宗',
                    item.name,
                  ];
                }
                // 更新可排序文档列表
                updateSortableDocuments();
              }
            }
          }
        } finally {
          // 恢复标记
          isRestoringFromRoute.value = false;
        }
      }
    }
  }
}

onMounted(async () => {
  await loadMatters();
  // 尝试从路由参数恢复状态（跳过重复加载项目列表）
  await restoreStateFromRoute(true);
});

// 清理未完成的文件输入监听器（防止内存泄漏）
onUnmounted(() => {
  activeFileInputs.forEach(({ input, handler }) => {
    input.removeEventListener('change', handler);
  });
  activeFileInputs.clear();
});

// 当选择项目时，更新路由参数（避免无限循环）
watch(
  () => selectedMatterId.value,
  (newMatterId, oldMatterId) => {
    // 如果正在从路由恢复状态，跳过更新
    if (isRestoringFromRoute.value) return;
    // 避免初始化时的触发
    if (newMatterId === oldMatterId) return;

    if (newMatterId) {
      const currentMatterId = route.query.matterId as string | undefined;
      // 只有当路由参数不同时才更新
      if (currentMatterId !== String(newMatterId)) {
        router.replace({
          query: {
            ...route.query,
            matterId: String(newMatterId),
            folderId: selectedFolder.value,
          },
        });
      }
    } else {
      // 清除路由参数
      const newQuery = { ...route.query };
      delete newQuery.matterId;
      delete newQuery.folderId;
      router.replace({ query: newQuery });
    }
  },
);

// 当选择文件夹时，更新路由参数（使用 history.replaceState 避免重新渲染）
watch(
  () => selectedFolder.value,
  (newFolder, oldFolder) => {
    // 如果正在从路由恢复状态，跳过更新
    if (isRestoringFromRoute.value) return;
    // 避免初始化时的触发
    if (newFolder === oldFolder) return;

    if (selectedMatterId.value) {
      const currentFolderId = route.query.folderId as string | undefined;
      // 只有当路由参数不同时才更新
      if (currentFolderId !== newFolder) {
        // 使用 history.replaceState 直接更新 URL，不触发 Vue Router 导航
        // 这样可以保持 URL 与状态同步（刷新页面后能恢复状态），同时避免组件重新渲染
        const newQuery: Record<string, string> = {};
        // 保留现有的查询参数
        Object.keys(route.query).forEach((key) => {
          const value = route.query[key];
          if (value !== undefined && value !== null) {
            const stringValue: null | string | undefined = Array.isArray(value)
              ? (value[0] ?? null)
              : value;
            if (stringValue !== undefined && stringValue !== null) {
              newQuery[key] = String(stringValue);
            }
          }
        });
        // 更新 matterId 和 folderId
        newQuery.matterId = String(selectedMatterId.value);
        newQuery.folderId = newFolder;

        // 构建新的 URL
        const queryString = new URLSearchParams(newQuery).toString();
        const newUrl = `${route.path}${queryString ? `?${queryString}` : ''}`;

        // 使用 history.replaceState 更新 URL，不触发 Vue Router 导航
        history.replaceState({ ...history.state }, '', newUrl);

        // 手动更新 route.query，确保刷新页面后能正确恢复状态
        // 注意：这不会触发 Vue Router 的导航钩子，因此不会导致组件重新渲染
        Object.keys(route.query).forEach((key) => {
          if (!(key in newQuery)) {
            delete (route.query as Record<string, any>)[key];
          }
        });
        Object.assign(route.query, newQuery);
      }
    }
  },
);
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
            <div class="matter-card-content">
              <div class="matter-card-item">
                <span class="matter-card-label">项目编号：</span>
                <span class="matter-card-value matter-card-value-primary">{{
                  matter.matterNo || '-'
                }}</span>
              </div>
              <div class="matter-card-item">
                <span class="matter-card-label">客户：</span
                >{{ matter.clientName || '-' }}
              </div>
              <div
                class="matter-card-item"
                v-if="matter.causeOfActionName || matter.causeOfAction"
              >
                <span class="matter-card-label">案由：</span>
                <span class="matter-card-value">{{
                  getCauseOfActionName(matter)
                }}</span>
              </div>
              <div class="matter-card-item" v-if="matter.leadLawyerName">
                <span class="matter-card-label">承办律师：</span
                >{{ matter.leadLawyerName }}
              </div>
              <div class="matter-card-item" v-if="matter.opposingParty">
                <span class="matter-card-label">对方：</span>
                <span class="matter-card-value">{{
                  matter.opposingParty
                }}</span>
              </div>
              <div class="matter-card-footer">
                <div>
                  <span class="matter-card-label">类型：</span>
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
                <div class="matter-card-date">
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
                <div class="col-checkbox">
                  <input
                    type="checkbox"
                    :checked="isAllSelected()"
                    @change="toggleSelectAll"
                    style="width: 16px; height: 16px; cursor: pointer"
                    title="全选/取消全选"
                  />
                </div>
                <div class="col-drag"></div>
                <div class="col-name">文档名称</div>
                <div class="col-type">类型</div>
                <div class="col-source">来源</div>
                <div class="col-size">大小</div>
                <div class="col-time">修改时间</div>
                <div class="col-action">操作</div>
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
                          <Button
                            type="text"
                            size="small"
                            class="action-btn"
                            aria-label="更多操作"
                          >
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
                      margin-right: 8px;
                      cursor: pointer;
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
                      style="padding: 0 4px; margin: 0; font-size: 10px"
                    >
                      {{ getFileTypeConfig(doc.fileType).label }}
                    </Tag>
                    <span class="grid-item-size">{{
                      formatFileSize(doc.fileSize)
                    }}</span>
                  </div>

                  <!-- 悬停时底部操作栏 (vben-admin 风格) -->
                  <div class="grid-item-toolbar" @click.stop>
                    <Space :size="4">
                      <Tooltip title="预览">
                        <Button
                          type="text"
                          size="small"
                          @click="handlePreview(doc)"
                        >
                          <Eye :size="14" />
                        </Button>
                      </Tooltip>
                      <Tooltip v-if="isEditableFile(doc.fileType)" title="编辑">
                        <Button
                          type="text"
                          size="small"
                          @click="handleOnlineEdit(doc)"
                        >
                          <Edit :size="14" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="下载">
                        <Button
                          type="text"
                          size="small"
                          @click="handleDownload(doc)"
                        >
                          <SvgDownloadIcon :size="14" />
                        </Button>
                      </Tooltip>
                      <Dropdown placement="topRight" trigger="click">
                        <template #overlay>
                          <Menu class="grid-dropdown-menu">
                            <MenuItem key="rename" @click="handleEdit(doc)">
                              <span class="menu-icon">✏️</span>
                              <span>重命名</span>
                            </MenuItem>
                            <MenuItem key="share" @click="handleShare(doc)">
                              <span class="menu-icon">🔗</span>
                              <span>分享链接</span>
                            </MenuItem>
                            <MenuItem
                              key="move"
                              @click="handleMoveDocument(doc)"
                            >
                              <span class="menu-icon">📁</span>
                              <span>移动到...</span>
                            </MenuItem>
                            <MenuItem
                              key="versions"
                              @click="handleViewVersions(doc)"
                            >
                              <span class="menu-icon">📋</span>
                              <span>版本历史</span>
                            </MenuItem>
                            <MenuItem
                              v-if="isImageFile(doc.fileType)"
                              key="ocr"
                              @click="handleOcrExtract(doc)"
                            >
                              <span class="menu-icon">🔍</span>
                              <span>提取文字(OCR)</span>
                            </MenuItem>
                            <Divider style="margin: 4px 0" />
                            <MenuItem
                              key="delete"
                              @click="handleDelete(doc)"
                              class="menu-item-danger"
                            >
                              <span class="menu-icon">🗑️</span>
                              <span>删除文件</span>
                            </MenuItem>
                          </Menu>
                        </template>
                        <Button type="text" size="small">
                          <Ellipsis :size="14" />
                        </Button>
                      </Dropdown>
                    </Space>
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
      :confirm-loading="uploadSubmitting"
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
      :width="
        isImageFile(currentDocument?.fileType) ||
        currentDocument?.fileType?.toLowerCase() === 'pdf' ||
        (currentDocument?.fileType && isOfficeFile(currentDocument.fileType))
          ? '95%'
          : '80%'
      "
      :footer="null"
      style="top: 20px"
      :class="{ 'image-preview-modal': isImageFile(currentDocument?.fileType) }"
      @cancel="handlePreviewModalClose"
    >
      <template #title>
        <div
          style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding-right: 32px;
          "
        >
          <span
            >预览 -
            {{
              currentDocument?.title ||
              currentDocument?.fileName ||
              currentDocument?.name
            }}</span
          >
          <Space
            v-if="currentDocument && isEditableFile(currentDocument.fileType)"
          >
            <Button
              type="primary"
              size="small"
              @click="handleOpenInEditor(currentDocument)"
            >
              <Edit :size="14" style="margin-right: 4px" />
              在线编辑
            </Button>
          </Space>
        </div>
      </template>
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
      <!-- 其他类型文件预览（PDF、Office文档等） -->
      <div v-else style="height: 80vh">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          style="width: 100%; height: 100%; border: none"
          frameborder="0"
          allowfullscreen
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
  </Page>
</template>

<style scoped>
/* 文档列表头部 */
.doc-list-header {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  overflow: hidden;
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

.col-name {
  flex: 1;
  min-width: 150px;
  overflow: hidden;
}

.col-type {
  flex-shrink: 0;
  width: 80px;
  text-align: center;
}

.col-source {
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
  width: 180px;
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
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 移除 Card 的默认左边框，使用伪元素替代 */
.matter-card :deep(.ant-card) {
  border-left: none;
}

/* 使用伪元素创建左侧彩色包边 */
.matter-card::before {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 2;
  width: 4px;
  height: 100%;
  content: '';
  border-radius: 4px 0 0 4px;
}

/* 确保 Card 内容不被伪元素遮挡 */
.matter-card :deep(.ant-card-body) {
  position: relative;
  z-index: 1;
}

.matter-card:hover {
  box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
  transform: translateY(-2px);
}

/* 根据案件类型设置不同的左侧边框颜色和背景色 */
.matter-card-CRIMINAL {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(211 47 47 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-CRIMINAL::before {
  background: #d32f2f;
}

.matter-card-CIVIL {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(25 118 210 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-CIVIL::before {
  background: #1976d2;
}

.matter-card-ADMINISTRATIVE {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(56 142 60 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-ADMINISTRATIVE::before {
  background: #388e3c;
}

.matter-card-BANKRUPTCY {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(245 124 0 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-BANKRUPTCY::before {
  background: #f57c00;
}

.matter-card-IP {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(123 31 162 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-IP::before {
  background: #7b1fa2;
}

.matter-card-ARBITRATION {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(2 136 209 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-ARBITRATION::before {
  background: #0288d1;
}

.matter-card-ENFORCEMENT {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(93 64 55 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

.matter-card-ENFORCEMENT::before {
  background: #5d4037;
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.LEGAL_COUNSEL 一致 */
/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.LEGAL_COUNSEL 一致 */
.matter-card-LEGAL_COUNSEL {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(0 121 107 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.LEGAL_COUNSEL 一致 */
.matter-card-LEGAL_COUNSEL::before {
  background: #00796b;
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.SPECIAL_SERVICE 一致 */
.matter-card-SPECIAL_SERVICE {
  color: var(--ant-color-text, #333);
  background: linear-gradient(
    to right,
    rgb(230 74 25 / 2%),
    var(--ant-color-bg-container, #fff)
  );
}

/* stylelint-disable-next-line selector-class-pattern -- 类名与后端枚举 CaseType.SPECIAL_SERVICE 一致 */
.matter-card-SPECIAL_SERVICE::before {
  background: #e64a19;
}

.matter-card-default {
  color: var(--ant-color-text, #333);
  background: var(--ant-color-bg-container, #fff);
}

.matter-card-default::before {
  background: #757575;
}

/* 深色模式适配 */
[data-theme='dark'] .matter-card-CRIMINAL,
.dark .matter-card-CRIMINAL {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(211 47 47 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-CIVIL,
.dark .matter-card-CIVIL {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(25 118 210 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-ADMINISTRATIVE,
.dark .matter-card-ADMINISTRATIVE {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(56 142 60 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-BANKRUPTCY,
.dark .matter-card-BANKRUPTCY {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(245 124 0 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-IP,
.dark .matter-card-IP {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(123 31 162 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-ARBITRATION,
.dark .matter-card-ARBITRATION {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(2 136 209 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-ENFORCEMENT,
.dark .matter-card-ENFORCEMENT {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(93 64 55 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

/* stylelint-disable selector-class-pattern -- 类名与后端枚举一致 */
[data-theme='dark'] .matter-card-LEGAL_COUNSEL,
.dark .matter-card-LEGAL_COUNSEL {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(0 121 107 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}

[data-theme='dark'] .matter-card-SPECIAL_SERVICE,
.dark .matter-card-SPECIAL_SERVICE {
  color: var(--ant-color-text, #fff);
  background: linear-gradient(
    to right,
    rgb(230 74 25 / 15%),
    var(--ant-color-bg-container, #1f1f1f)
  );
}
/* stylelint-enable selector-class-pattern */

[data-theme='dark'] .matter-card-default,
.dark .matter-card-default {
  color: var(--ant-color-text, #fff);
  background: var(--ant-color-bg-container, #1f1f1f);
}

/* 卡片内容样式 */
.matter-card-content {
  font-size: 12px;
  color: var(--ant-color-text-secondary, #666);
}

.matter-card-item {
  margin-bottom: 6px;
}

.matter-card-label {
  color: var(--ant-color-text-tertiary, #999);
}

.matter-card-value {
  color: var(--ant-color-text, #333);
}

.matter-card-value-primary {
  font-weight: 500;
  color: #1890ff;
}

.matter-card-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 8px;
  margin-top: 8px;
  border-top: 1px solid var(--ant-color-border-secondary, #f0f0f0);
}

.matter-card-date {
  font-size: 11px;
  color: var(--ant-color-text-tertiary, #999);
}

/* 深色模式下的卡片内容 */
[data-theme='dark'] .matter-card-content,
.dark .matter-card-content {
  color: var(--ant-color-text-secondary, rgb(255 255 255 / 65%));
}

[data-theme='dark'] .matter-card-value,
.dark .matter-card-value {
  color: var(--ant-color-text, rgb(255 255 255 / 85%));
}

[data-theme='dark'] .matter-card-footer,
.dark .matter-card-footer {
  border-top-color: var(--ant-color-border-secondary, rgb(255 255 255 / 10%));
}

/* 深色模式下的网格视图 */
[data-theme='dark'] .grid-item,
.dark .grid-item {
  background: var(--ant-color-bg-container, #1f1f1f);
  border-color: var(--ant-color-border-secondary, rgb(255 255 255 / 10%));
}

[data-theme='dark'] .grid-item:hover,
.dark .grid-item:hover {
  background: var(--ant-color-bg-container-hover, rgb(255 255 255 / 8%));
}

[data-theme='dark'] .grid-item-selected,
.dark .grid-item-selected {
  background: var(--ant-color-primary-bg, rgb(24 144 255 / 20%)) !important;
}

[data-theme='dark'] .select-all-label,
.dark .select-all-label {
  color: var(--ant-color-text-secondary, rgb(255 255 255 / 65%));
  background: var(--ant-color-fill-tertiary, rgb(255 255 255 / 8%));
}

[data-theme='dark'] .select-all-label:hover,
.dark .select-all-label:hover {
  background: var(--ant-color-fill-secondary, rgb(255 255 255 / 12%));
}

[data-theme='dark'] .grid-thumbnail,
.dark .grid-thumbnail {
  background: linear-gradient(
    145deg,
    var(--ant-color-fill-tertiary, rgb(255 255 255 / 8%)) 0%,
    var(--ant-color-fill-secondary, rgb(255 255 255 / 12%)) 100%
  );
}

[data-theme='dark'] .grid-thumbnail-icon .file-ext,
.dark .grid-thumbnail-icon .file-ext {
  color: var(--ant-color-text-secondary, rgb(255 255 255 / 65%));
  background: var(--ant-color-fill-quaternary, rgb(255 255 255 / 12%));
}

[data-theme='dark'] .grid-item-name,
.dark .grid-item-name {
  color: var(--ant-color-text, rgb(255 255 255 / 85%));
}

[data-theme='dark'] .grid-item-size,
.dark .grid-item-size {
  color: var(--ant-color-text-tertiary, rgb(255 255 255 / 45%));
}

[data-theme='dark'] .grid-item-toolbar,
.dark .grid-item-toolbar {
  background: linear-gradient(
    to top,
    var(--ant-color-bg-container, rgb(31 31 31 / 95%)) 0%,
    var(--ant-color-bg-container-secondary, rgb(31 31 31 / 85%)) 100%
  );
  border-top-color: var(--ant-color-border-secondary, rgb(255 255 255 / 10%));
}

[data-theme='dark'] .grid-item-toolbar :deep(.ant-btn-text),
.dark .grid-item-toolbar :deep(.ant-btn-text) {
  color: var(--ant-color-text-secondary, rgb(255 255 255 / 65%));
}

[data-theme='dark'] .grid-item-toolbar :deep(.ant-btn-text:hover),
.dark .grid-item-toolbar :deep(.ant-btn-text:hover) {
  background: var(--ant-color-primary-bg, rgb(24 144 255 / 20%));
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
  padding: 0 8px;
  margin-bottom: 16px;
}

.select-all-label {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  color: var(--ant-color-text-secondary, #666);
  cursor: pointer;
  background: var(--ant-color-fill-tertiary, #fafafa);
  border-radius: 6px;
  transition: all 0.2s;
}

.select-all-label:hover {
  background: var(--ant-color-fill-secondary, #f0f0f0);
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
  background: var(--ant-color-bg-container, #fff);
  border: 1px solid var(--ant-color-border-secondary, #e8e8e8);
  border-radius: 12px;
  transition: all 0.25s ease;
}

.grid-item:hover {
  background: var(--ant-color-bg-container-hover, #f8fafc);
  border-color: #1890ff;
  box-shadow: 0 6px 20px rgb(24 144 255 / 12%);
  transform: translateY(-4px);
}

.grid-item-selected {
  background: var(--ant-color-primary-bg, #e6f7ff) !important;
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
  background: linear-gradient(
    145deg,
    var(--ant-color-fill-tertiary, #f8f9fa) 0%,
    var(--ant-color-fill-secondary, #e9ecef) 100%
  );
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
  color: var(--ant-color-text-secondary, #666);
  background: var(--ant-color-fill-quaternary, rgb(0 0 0 / 8%));
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
  text-overflow: ellipsis;

  /* stylelint-disable-next-line property-no-vendor-prefix */
  -webkit-line-clamp: 2;
  line-clamp: 2;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  color: var(--ant-color-text, #333);
  word-break: break-all;
  -webkit-box-orient: vertical;
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
  color: var(--ant-color-text-tertiary, #999);
}

/* 网格项底部操作栏 (vben-admin 风格) */
.grid-item-toolbar {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  background: linear-gradient(
    to top,
    var(--ant-color-bg-container, rgb(255 255 255 / 95%)) 0%,
    var(--ant-color-bg-container-secondary, rgb(255 255 255 / 85%)) 100%
  );
  border-top: 1px solid var(--ant-color-border-secondary, #f0f0f0);
  border-radius: 0 0 12px 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.grid-item:hover .grid-item-toolbar {
  opacity: 1;
}

.grid-item-toolbar :deep(.ant-btn-text) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  color: var(--ant-color-text-secondary, #595959);
  border-radius: 4px;
}

.grid-item-toolbar :deep(.ant-btn-text:hover) {
  color: #1890ff;
  background: var(--ant-color-primary-bg, #e6f7ff);
}

/* 网格视图下拉菜单样式 */
.grid-dropdown-menu {
  min-width: 140px;
  padding: 4px 0;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgb(0 0 0 / 12%);
}

.grid-dropdown-menu :deep(.ant-dropdown-menu-item) {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 12px;
  margin: 2px 4px;
  font-size: 13px;
  border-radius: 4px;
  transition: all 0.2s;
}

.grid-dropdown-menu :deep(.ant-dropdown-menu-item:hover) {
  background: #f5f5f5;
}

.grid-dropdown-menu .menu-icon {
  width: 16px;
  font-size: 14px;
  text-align: center;
}

.grid-dropdown-menu .menu-item-danger {
  color: #ff4d4f;
}

.grid-dropdown-menu .menu-item-danger:hover {
  color: #ff4d4f;
  background: #fff1f0;
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
  transform-origin: center center;
  transition: transform 0.15s ease;
}

/* 图片预览模态框特殊样式 */
:deep(.image-preview-modal .ant-modal-body) {
  padding: 0;
  overflow: hidden;
  border-radius: 8px;
}
</style>
