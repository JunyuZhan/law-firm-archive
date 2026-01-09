<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue';
import { message, Modal } from 'ant-design-vue';
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
  Popconfirm,
  Upload,
  Tree,
  Divider,
  Tooltip,
  Dropdown,
  Menu,
  MenuItem,
  Tag,
  Progress,
} from 'ant-design-vue';
import {
  Eye,
  Plus,
  Inbox,
  ArrowUp,
  Edit,
  Ellipsis,
  GripVertical,
  Trash,
} from '@vben/icons';
import { SvgDownloadIcon } from '@vben/icons';
import draggable from 'vuedraggable';
import {
  getDocumentList,
  updateDocument,
  deleteDocument,
  downloadDocument,
  getDocumentVersions,
  shareDocument,
  createFolder,
  uploadFiles,
  moveDocument,
  checkDocumentEditSupport,
  getDocumentPreviewUrl,
  reorderDocuments,
} from '#/api/document';
import { 
  getMatterDossierItems, 
  initMatterDossier, 
  addDossierItem, 
  updateDossierItem, 
  deleteDossierItem,
  reorderDossierItems,
  type MatterDossierItem 
} from '#/api/document/dossier';
import { getMatterList } from '#/api/matter';
import { recognizeGeneral, type OcrResultDTO } from '#/api/ocr';
import type { DocumentDTO, DocumentQuery, CreateDocumentCommand, UpdateDocumentCommand } from '#/api/document/types';
import type { MatterDTO } from '#/api/matter/types';

defineOptions({ name: 'DossierManager' });

// 状态管理
const loading = ref(false);
const selectedMatterId = ref<number | undefined>(undefined);
const selectedMatter = ref<MatterDTO | null>(null);
const selectedFolder = ref<string>('root');
const currentPath = ref<string[]>(['根目录']);
const documents = ref<DocumentDTO[]>([]);
const matters = ref<MatterDTO[]>([]);
const fileList = ref<any[]>([]);
const dossierItems = ref<MatterDossierItem[]>([]);

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
  folder: 'root',  // 保留 folder 用于 folderPath
  description: '',
});

const folderFormData = reactive({
  name: '',
  parentFolder: 'root',
});

const editFormData = reactive({
  id: undefined as number | undefined,
  title: '',  // 后端使用 title 字段
  description: '',
});

// 从后端获取的卷宗目录项构建树形数据
const folderTreeData = computed(() => {
  if (!selectedMatter.value || dossierItems.value.length === 0) {
    return [{
      title: selectedMatter.value ? '卷宗目录（点击初始化）' : '请选择项目',
      key: 'root',
      children: []
    }];
  }
  
  // 将扁平数据转换为树形结构
  const items = [...dossierItems.value].sort((a, b) => a.sortOrder - b.sortOrder);
  const rootItems = items.filter(item => item.parentId === 0);
  
  const buildTree = (parentItems: MatterDossierItem[]): any[] => {
    return parentItems.map(item => {
      const children = items.filter(child => child.parentId === item.id);
      return {
        title: `${item.name}${item.documentCount ? ` (${item.documentCount})` : ''}`,
        key: String(item.id),
        dossierItemId: item.id,
        name: item.name,
        sortOrder: item.sortOrder,
        documentCount: item.documentCount || 0,
        children: children.length > 0 ? buildTree(children) : undefined
      };
    });
  };
  
  return [{
    title: selectedMatter.value.name + ' 卷宗目录',
    key: 'root',
    children: buildTree(rootItems)
  }];
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
    message.error('初始化失败: ' + (error.message || '未知错误'));
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
        style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
      })
    ]),
    onOk: async () => {
      const input = document.getElementById('newFolderName') as HTMLInputElement;
      const name = input?.value?.trim();
      if (!name) {
        message.error('请输入文件夹名称');
        return Promise.reject();
      }
      
      try {
        await addDossierItem(selectedMatter.value!.id, { name, parentId: 0 });
        await loadDossierItems();
        message.success('文件夹创建成功');
      } catch (error: any) {
        message.error('创建失败: ' + (error.message || '未知错误'));
        return Promise.reject();
      }
    }
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
        style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px;'
      })
    ]),
    onOk: async () => {
      const input = document.getElementById('renameFolderName') as HTMLInputElement;
      const name = input?.value?.trim();
      if (!name) {
        message.error('请输入文件夹名称');
        return Promise.reject();
      }
      
      try {
        await updateDossierItem(selectedMatter.value!.id, item.dossierItemId, { name });
        await loadDossierItems();
        message.success('重命名成功');
      } catch (error: any) {
        message.error('重命名失败: ' + (error.message || '未知错误'));
        return Promise.reject();
      }
    }
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
        message.error('删除失败: ' + (error.message || '未知错误'));
      }
    }
  });
}

// 调整目录项顺序（上移）
async function handleMoveUp(item: any) {
  if (!selectedMatter.value) return;
  
  const items = dossierItems.value.filter(i => i.parentId === 0).sort((a, b) => a.sortOrder - b.sortOrder);
  const currentIndex = items.findIndex(i => i.id === item.dossierItemId);
  if (currentIndex <= 0) return;
  
  // 交换位置
  const newOrder: number[] = items.map(i => i.id).filter((id): id is number => id !== undefined);
  if (newOrder.length < 2) return;
  const temp = newOrder[currentIndex];
  newOrder[currentIndex] = newOrder[currentIndex - 1]!;
  newOrder[currentIndex - 1] = temp!;
  
  try {
    await reorderDossierItems(selectedMatter.value.id, newOrder);
    await loadDossierItems();
  } catch (error: any) {
    message.error('调整顺序失败');
  }
}

// 调整目录项顺序（下移）
async function handleMoveDown(item: any) {
  if (!selectedMatter.value) return;
  
  const items = dossierItems.value.filter(i => i.parentId === 0).sort((a, b) => a.sortOrder - b.sortOrder);
  const currentIndex = items.findIndex(i => i.id === item.dossierItemId);
  if (currentIndex < 0 || currentIndex >= items.length - 1) return;
  
  // 交换位置
  const newOrder: number[] = items.map(i => i.id).filter((id): id is number => id !== undefined);
  if (newOrder.length < 2) return;
  const temp = newOrder[currentIndex];
  newOrder[currentIndex] = newOrder[currentIndex + 1]!;
  newOrder[currentIndex + 1] = temp!;
  
  try {
    await reorderDossierItems(selectedMatter.value.id, newOrder);
    await loadDossierItems();
  } catch (error: any) {
    message.error('调整顺序失败');
  }
}

// 当前右键选中的目录项
const contextMenuItem = ref<any>(null);

// 当前文件夹的文档列表（可拖拽排序）
const sortableDocuments = ref<DocumentDTO[]>([]);

// 监听文档变化，更新可排序列表
const currentDocuments = computed({
  get: () => sortableDocuments.value,
  set: (val: DocumentDTO[]) => {
    sortableDocuments.value = val;
  }
});

// 更新可排序文档列表
function updateSortableDocuments() {
  if (!selectedMatter.value) {
    sortableDocuments.value = [];
    return;
  }
  
  const projectDocs = documents.value.filter(doc => doc.matterId === selectedMatter.value?.id);
  
  // 如果选中的是根文件夹，显示所有项目文档
  if (selectedFolder.value === 'root') {
    // 按 displayOrder 排序
    sortableDocuments.value = [...projectDocs].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    return;
  }
  
  // 否则只显示特定 dossierItemId 的文档
  const dossierItemId = parseInt(selectedFolder.value, 10);
  if (!isNaN(dossierItemId)) {
    sortableDocuments.value = [...projectDocs.filter(doc => doc.dossierItemId === dossierItemId)]
      .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
    return;
  }
  
  sortableDocuments.value = [...projectDocs].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
}

// 文档统计
const documentStats = computed(() => {
  if (!selectedMatter.value) return { total: 0, size: '0B' };
  
  const projectDocs = documents.value.filter(doc => doc.matterId === selectedMatter.value?.id);
  const totalSize = projectDocs.reduce((sum, doc) => sum + (doc.fileSize || 0), 0);
  
  return {
    total: projectDocs.length,
    size: formatFileSize(totalSize)
  };
});

// 获取当前年份
const currentYear = new Date().getFullYear();

// 筛选参数
const filterParams = reactive({
  year: currentYear as number | undefined, // 默认当前年份
  matterType: undefined as string | undefined, // 项目大类
  caseType: undefined as string | undefined,   // 案件类型
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

// 项目大类（matter_type）
const matterTypeOptions = [
  { label: '诉讼案件', value: 'LITIGATION' },
  { label: '非诉项目', value: 'NON_LITIGATION' },
];

// 案件类型（case_type）- 更细的分类
const caseTypeOptions = [
  { label: '民事', value: 'CIVIL' },
  { label: '刑事', value: 'CRIMINAL' },
  { label: '行政', value: 'ADMINISTRATIVE' },
  { label: '破产', value: 'BANKRUPTCY' },
  { label: '知识产权', value: 'IP' },
  { label: '仲裁', value: 'ARBITRATION' },
  { label: '执行', value: 'ENFORCEMENT' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '专项服务', value: 'SPECIAL_SERVICE' },
];

// 根据项目大类获取显示名称
function getMatterTypeName(type: string | undefined): string {
  if (!type) return '-';
  const option = matterTypeOptions.find(opt => opt.value === type);
  return option?.label || type;
}

// 根据案件类型获取显示名称
function getCaseTypeName(type: string | undefined): string {
  if (!type) return '-';
  const option = caseTypeOptions.find(opt => opt.value === type);
  return option?.label || type;
}

// 根据状态值获取显示名称
function getStatusName(status: string | undefined): string {
  if (!status) return '-';
  const option = statusOptions.find(opt => opt.value === status);
  return option?.label || status;
}

// 选择项目（从项目列表卡片点击）
async function handleSelectMatter(matter: MatterDTO) {
  selectedMatterId.value = matter.id;
  selectedMatter.value = matter;
  selectedFolder.value = 'root';
  currentPath.value = [matter.name + ' 卷宗目录'];
  
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

const statusOptions = [
  { label: '进行中', value: 'ACTIVE' },
  { label: '已结案', value: 'CLOSED' },
  { label: '已暂停', value: 'SUSPENDED' },
  { label: '已取消', value: 'CANCELLED' },
];

// 过滤后的项目列表（年份筛选已在后端完成，这里只做其他筛选）
const filteredMatters = computed(() => {
  let filtered = matters.value;

  // 按项目大类筛选
  if (filterParams.matterType) {
    filtered = filtered.filter(matter => matter.matterType === filterParams.matterType);
  }

  // 按案件类型筛选
  if (filterParams.caseType) {
    filtered = filtered.filter(matter => matter.caseType === filterParams.caseType);
  }

  // 按状态筛选
  if (filterParams.status) {
    filtered = filtered.filter(matter => matter.status === filterParams.status);
  }

  // 按关键词筛选
  if (filterParams.keyword) {
    const keyword = filterParams.keyword.toLowerCase();
    filtered = filtered.filter(matter => 
      matter.name.toLowerCase().includes(keyword) ||
      matter.clientName?.toLowerCase().includes(keyword)
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
const fileTypeConfig: Record<string, { icon: string; color: string; label: string }> = {
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
function getFileTypeConfig(fileType: string | undefined): { icon: string; color: string; label: string } {
  if (!fileType) return fileTypeConfig.default!;
  const ext = fileType.toLowerCase().replace('.', '');
  return fileTypeConfig[ext] || fileTypeConfig.default!;
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
    minute: '2-digit'
  });
}

// 表格列定义
const columns = [
  { 
    title: '文档名称', 
    dataIndex: 'name', 
    key: 'name', 
    width: 350,
  },
  { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80 },
  { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '修改时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 160 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 60 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
];

// 加载项目列表
async function loadMatters() {
  loading.value = true;
  try {
    console.log('开始加载项目列表...');
    // 处理年份筛选参数
    let createdAtFrom: string | undefined;
    let createdAtTo: string | undefined;
    if (filterParams.year && filterParams.year !== 0) {
      createdAtFrom = `${filterParams.year}-01-01T00:00:00`;
      createdAtTo = `${filterParams.year}-12-31T23:59:59`;
    }
    
    const res = await getMatterList({ 
      pageNum: 1, 
      pageSize: 1000,
      createdAtFrom,
      createdAtTo,
    });
    console.log('项目列表响应:', res);
    
    if (res && res.list) {
      matters.value = res.list;
      console.log('成功加载项目数量:', matters.value.length);
    } else {
      console.warn('项目列表响应格式异常:', res);
      matters.value = [];
    }
  } catch (error: any) {
    console.error('加载项目列表失败:', error);
    message.error(`加载项目列表失败: ${error.message || '未知错误'}`);
    
    // 如果 API 失败，使用测试数据
    console.log('使用测试数据...');
    matters.value = [
      {
        id: 1,
        matterNo: 'M2024001',
        name: '张三诉李四合同纠纷案',
        matterType: 'CIVIL',
        matterTypeName: '民事案件',
        clientId: 1,
        clientName: '张三',
        status: 'ACTIVE',
        statusName: '进行中',
        createdAt: '2024-01-01T00:00:00Z'
      },
      {
        id: 2,
        matterNo: 'M2024002',
        name: '王五刑事辩护案',
        matterType: 'CRIMINAL',
        matterTypeName: '刑事案件',
        clientId: 2,
        clientName: '王五',
        status: 'ACTIVE',
        statusName: '进行中',
        createdAt: '2024-01-02T00:00:00Z'
      },
      {
        id: 3,
        matterNo: 'M2024003',
        name: 'ABC公司法律顾问',
        matterType: 'LEGAL_COUNSEL',
        matterTypeName: '法律顾问',
        clientId: 3,
        clientName: 'ABC公司',
        status: 'ACTIVE',
        statusName: '进行中',
        createdAt: '2024-01-03T00:00:00Z'
      }
    ];
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
      pageSize: 1000
    });
    documents.value = res.list;
    // 更新可排序文档列表
    updateSortableDocuments();
  } catch (error: any) {
    message.error('加载文档失败');
  } finally {
    loading.value = false;
  }
}

// 选择文件夹
function handleFolderSelect(selectedKeys: any[], info: any) {
  if (selectedKeys.length > 0) {
    const key = String(selectedKeys[0]);
    selectedFolder.value = key;
    
    // 更新面包屑路径
    if (key === 'root') {
      currentPath.value = [selectedMatter.value?.name + ' 卷宗目录' || '卷宗目录'];
    } else {
      const dossierItemId = parseInt(key, 10);
      const item = dossierItems.value.find(i => i.id === dossierItemId);
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
      .filter(item => item.parentId === 0) // 只显示顶级目录
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(item => ({
        label: item.name,
        value: item.id,  // 使用实际的 dossierItemId
        dossierItemId: item.id,
        folderPath: item.name
      }));
  }
  
  // 如果没有卷宗目录，返回空（用户需要先初始化卷宗目录）
  if (!selectedMatter.value) return [];
  
  // 备用静态选项（仅在后端没有数据时使用）
  return [{ label: '根目录', value: 0, dossierItemId: undefined, folderPath: 'root' }];
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
  const option = folderOptions.value.find(opt => opt.value === value);
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
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp', 'ico'].includes(ext);
}

// 判断文件是否为视频类型
function isVideoFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['mp4', 'webm', 'ogg', 'avi', 'mov', 'mkv'].includes(ext);
}

// 判断文件是否为音频类型
function isAudioFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac'].includes(ext);
}

// 判断文件是否支持 OnlyOffice 预览
function isOfficeFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'odt', 'ods', 'odp', 'rtf', 'txt', 'csv', 'pdf'].includes(ext);
}

// 判断文件是否支持在线编辑（不包括 PDF，PDF 只能预览）
function isEditableFile(fileType: string | undefined): boolean {
  if (!fileType) return false;
  const ext = fileType.toLowerCase();
  // 支持编辑的文件类型：Word、Excel、PowerPoint 及其开放格式
  return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'odt', 'ods', 'odp', 'rtf', 'txt', 'csv'].includes(ext);
}

// 预览文档（根据文件类型选择不同的预览方式）
async function handlePreview(record: DocumentDTO) {
  const fileType = record.fileType?.toLowerCase() || '';
  
  // Office 文档类型 - 使用 OnlyOffice 预览
  if (isOfficeFile(fileType)) {
    const url = `/office-preview?documentId=${record.id}&mode=view`;
    window.open(url, '_blank');
    return;
  }
  
  // 对于非 Office 文件，先获取预签名 URL
  try {
    const { previewUrl } = await getDocumentPreviewUrl(record.id);
    const fileUrl = previewUrl;
    
    // 图片类型 - 使用模态框显示
    if (isImageFile(fileType)) {
      Modal.info({
        title: record.fileName || record.name,
        icon: null,
        width: '80%',
        centered: true,
        content: h('div', { style: 'text-align: center; max-height: 70vh; overflow: auto;' }, [
          h('img', { 
            src: fileUrl, 
            style: 'max-width: 100%; max-height: 65vh; object-fit: contain;',
            alt: record.fileName
          })
        ]),
        okText: '关闭',
      });
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
          })
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
          })
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
    message.error('获取预览链接失败: ' + (error.message || '未知错误'));
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
    const url = `/office-preview?documentId=${record.id}&mode=edit`;
    window.open(url, '_blank');
  } catch (error: any) {
    message.error('检查编辑支持失败');
  }
}

// 下载文档
async function handleDownload(record: DocumentDTO) {
  try {
    await downloadDocument(record.id);
    message.success('下载成功');
  } catch (error: any) {
    message.error('下载失败');
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
    const file = new File([blob], record.fileName || 'image.jpg', { type: blob.type });
    
    // 调用OCR识别
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success) {
      ocrResult.value = result.rawText || result.recognizedText || '未识别到文字内容';
      message.success('OCR识别完成');
    } else {
      ocrResult.value = result.errorMessage || 'OCR识别失败';
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (e: any) {
    ocrResult.value = e?.message || 'OCR识别失败';
    message.error(e?.message || 'OCR识别失败');
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
  } catch (error: any) {
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
  } catch (error: any) {
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
      } catch (error: any) {
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
    message.error('排序保存失败: ' + (error.message || '未知错误'));
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
    .filter(item => item.itemType === 'FOLDER')
    .map(item => ({
      label: item.name,
      value: item.id
    }));
  
  let selectedTargetId: number | undefined;
  
  Modal.confirm({
    title: '移动文件',
    content: h('div', { style: 'padding: 10px 0' }, [
      h('p', `将 "${record.title || record.fileName}" 移动到：`),
      h('select', {
        id: 'moveTargetSelect',
        style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; margin-top: 8px;',
        onChange: (e: Event) => {
          selectedTargetId = parseInt((e.target as HTMLSelectElement).value, 10);
        }
      }, options.map(opt => 
        h('option', { value: opt.value }, opt.label)
      ))
    ]),
    onOk: async () => {
      const select = document.getElementById('moveTargetSelect') as HTMLSelectElement;
      const targetId = parseInt(select?.value, 10);
      
      if (!targetId || isNaN(targetId)) {
        message.error('请选择目标目录');
        return Promise.reject();
      }
      
      try {
        await moveDocument(record.id, targetId);
        message.success('移动成功');
        loadProjectDocuments();
        await loadDossierItems(); // 刷新目录计数
      } catch (error: any) {
        message.error('移动失败: ' + (error.message || '未知错误'));
        return Promise.reject();
      }
    }
  });
}

// 保存上传
async function handleSaveUpload() {
  if (fileList.value.length === 0) {
    message.error('请选择要上传的文件');
    return;
  }
  
  try {
    // 提取文件对象
    const files: File[] = fileList.value.map(f => f.originFileObj || f);
    
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
    message.error('上传失败: ' + (error.message || '未知错误'));
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
      matterId: selectedMatter.value!.id
    });
    message.success('创建成功');
    folderModalVisible.value = false;
    // 刷新文件夹树
  } catch (error: any) {
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
      description: editFormData.description
    });
    message.success('更新成功');
    editModalVisible.value = false;
    await loadProjectDocuments();
  } catch (error: any) {
    message.error('更新失败: ' + (error.message || '未知错误'));
  }
}

// 格式化文件大小
function formatFileSize(bytes?: number) {
  if (!bytes) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
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
  <Page title="卷宗管理" description="按项目管理卷宗文件，支持预览、编辑、归档等功能">
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
            allowClear
            style="width: 100%"
            :options="matterTypeOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="3" :xl="3">
          <Select
            v-model:value="filterParams.caseType"
            placeholder="案件类型"
            allowClear
            style="width: 100%"
            :options="caseTypeOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="3" :xl="3">
          <Select
            v-model:value="filterParams.status"
            placeholder="项目状态"
            allowClear
            style="width: 100%"
            :options="statusOptions"
            @change="handleFilterChange"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Input
            v-model:value="filterParams.keyword"
            placeholder="搜索项目名称或客户"
            allowClear
            @pressEnter="handleFilter"
          />
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <Space>
            <Button type="primary" @click="handleFilter">筛选</Button>
            <Button @click="handleResetFilter">重置</Button>
            <Button v-if="selectedMatter" @click="handleBackToList">返回列表</Button>
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
      
      <div v-if="loading" style="text-align: center; padding: 40px">
        加载中...
      </div>
      
      <div v-else-if="filteredMatters.length === 0" style="text-align: center; padding: 40px; color: #999">
        暂无符合条件的项目
      </div>
      
      <Row v-else :gutter="[16, 16]">
        <Col v-for="matter in filteredMatters" :key="matter.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
          <Card 
            hoverable 
            size="small" 
            @click="handleSelectMatter(matter)"
            style="cursor: pointer"
          >
            <template #title>
              <div style="font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                {{ matter.name }}
              </div>
            </template>
            <template #extra>
              <Tag :color="matter.status === 'ACTIVE' ? 'green' : matter.status === 'CLOSED' ? 'default' : 'orange'" style="margin: 0">
                {{ matter.statusName || getStatusName(matter.status) }}
              </Tag>
            </template>
            <div style="font-size: 12px; color: #666">
              <div style="margin-bottom: 4px">
                <span style="color: #999">客户：</span>{{ matter.clientName || '-' }}
              </div>
              <div style="margin-bottom: 4px">
                <span style="color: #999">类型：</span>{{ matter.caseTypeName || getCaseTypeName(matter.caseType) }}
              </div>
              <div>
                <span style="color: #999">创建：</span>{{ matter.createdAt?.substring(0, 10) || '-' }}
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
          <Tag color="orange">{{ selectedMatter.caseTypeName || getCaseTypeName(selectedMatter.caseType) }}</Tag>
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
                <Button size="small" @click="handleAddDossierItem" title="添加文件夹">
                  <Plus :size="14" />
                </Button>
              </Space>
            </template>
            
            <Tree
              :tree-data="folderTreeData"
              :selected-keys="[selectedFolder]"
              @select="handleFolderSelect"
              :show-icon="true"
              default-expand-all
            >
              <template #title="{ title, key, dossierItemId, name, documentCount }">
                <Dropdown :trigger="['contextmenu']" v-if="key !== 'root'">
                  <span>{{ title }}</span>
                  <template #overlay>
                    <Menu>
                      <MenuItem @click="handleRenameDossierItem({ dossierItemId, name })">
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
                        @click="handleDeleteDossierItem({ dossierItemId, name, documentCount })" 
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
              </Space>
            </template>

            <!-- 文档列表头部 -->
            <div class="doc-list-header">
              <div class="col-drag" style="width: 30px;"></div>
              <div class="col-name" style="flex: 1;">文档名称</div>
              <div class="col-type" style="width: 80px;">类型</div>
              <div class="col-size" style="width: 80px;">大小</div>
              <div class="col-time" style="width: 140px;">修改时间</div>
              <div class="col-action" style="width: 160px;">操作</div>
            </div>
            
            <!-- 可拖拽文档列表 -->
            <div v-if="loading" style="text-align: center; padding: 40px;">加载中...</div>
            <div v-else-if="currentDocuments.length === 0" style="text-align: center; padding: 40px; color: #999;">
              暂无文档
            </div>
            <draggable 
              v-else
              v-model="currentDocuments" 
              item-key="id"
              handle=".drag-handle"
              @end="handleDragEnd"
              class="doc-list"
            >
              <template #item="{ element: record }">
                <div class="doc-item">
                  <!-- 拖拽手柄 -->
                  <div class="col-drag drag-handle">
                    <GripVertical :size="16" style="color: #bbb; cursor: grab;" />
                  </div>
                  
                  <!-- 文档名称 -->
                  <div class="col-name" style="flex: 1; display: flex; align-items: center; gap: 10px; min-width: 0;">
                    <span style="font-size: 26px; line-height: 1; flex-shrink: 0;">{{ getFileTypeConfig(record.fileType).icon }}</span>
                    <div style="display: flex; flex-direction: column; gap: 2px; min-width: 0; overflow: hidden;">
                      <a 
                        @click="handlePreview(record)" 
                        class="doc-name-link"
                        :title="record.title || record.fileName || record.name"
                      >
                        {{ record.title || record.fileName || record.name }}
                      </a>
                      <span class="doc-desc" v-if="record.description">{{ record.description }}</span>
                    </div>
                  </div>
                  
                  <!-- 文件类型 -->
                  <div class="col-type">
                    <Tag 
                      :color="getFileTypeConfig(record.fileType).color"
                      style="margin: 0; font-size: 11px; padding: 1px 6px;"
                    >
                      {{ getFileTypeConfig(record.fileType).label }}
                    </Tag>
                  </div>
                  
                  <!-- 文件大小 -->
                  <div class="col-size">{{ formatFileSize(record.fileSize) }}</div>
                  
                  <!-- 修改时间 -->
                  <div class="col-time">{{ formatDateTime(record.updatedAt) }}</div>
                  
                  <!-- 操作按钮 -->
                  <div class="col-action">
                    <Space :size="2">
                      <Tooltip title="预览">
                        <Button type="text" size="small" @click="handlePreview(record)" class="action-btn">
                          <Eye :size="15" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="在线编辑" v-if="isEditableFile(record.fileType)">
                        <Button type="text" size="small" @click="handleOnlineEdit(record)" class="action-btn">
                          <Edit :size="15" />
                        </Button>
                      </Tooltip>
                      <Tooltip title="下载">
                        <Button type="text" size="small" @click="handleDownload(record)" class="action-btn">
                          <SvgDownloadIcon :size="15" />
                        </Button>
                      </Tooltip>
                      <Dropdown placement="bottomRight">
                        <template #overlay>
                          <Menu class="action-menu">
                            <MenuItem key="rename" @click="handleEdit(record)">
                              <Edit :size="14" style="margin-right: 8px;" />
                              重命名
                            </MenuItem>
                            <MenuItem key="share" @click="handleShare(record)">
                              <span style="margin-right: 8px;">🔗</span>
                              分享
                            </MenuItem>
                            <MenuItem key="versions" @click="handleViewVersions(record)">
                              <span style="margin-right: 8px;">📋</span>
                              版本历史
                            </MenuItem>
                            <MenuItem key="move" @click="handleMoveDocument(record)">
                              <span style="margin-right: 8px;">📁</span>
                              移动
                            </MenuItem>
                            <MenuItem v-if="isImageFile(record.fileType)" key="ocr" @click="handleOcrExtract(record)">
                              <span style="margin-right: 8px;">🔍</span>
                              提取文字(OCR)
                            </MenuItem>
                            <Divider style="margin: 6px 0" />
                            <MenuItem key="delete" @click="handleDelete(record)" style="color: #ff4d4f">
                              <Trash :size="14" style="margin-right: 8px;" />
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
          <div style="margin-top: 8px; color: #666; font-size: 12px">
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
      :title="`预览 - ${currentDocument?.name}`"
      width="80%"
      :footer="null"
      style="top: 20px"
    >
      <div style="height: 70vh">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          style="width: 100%; height: 100%; border: none"
        />
        <div v-else style="text-align: center; padding: 50px">
          <div>暂不支持预览此类型文件</div>
          <Button type="primary" @click="handleDownload(currentDocument!)" style="margin-top: 16px">
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
        <div style="margin-bottom: 16px">
          分享链接（7天内有效）：
        </div>
        <Input.Group compact>
          <Input
            :value="shareUrl"
            readonly
            style="width: calc(100% - 80px)"
          />
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
          { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
          { title: '修改时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 160 },
          { title: '修改人', dataIndex: 'uploaderName', key: 'uploaderName', width: 100 },
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
      <div v-if="ocrLoading" style="text-align: center; padding: 40px;">
        <div style="font-size: 16px; color: #1890ff;">正在识别中...</div>
        <div style="color: #999; margin-top: 8px;">请稍候，OCR正在分析图片内容</div>
      </div>
      <div v-else>
        <div style="background: #f9f9f9; padding: 16px; border-radius: 8px; min-height: 200px; max-height: 400px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; font-family: monospace; line-height: 1.8;">
          {{ ocrResult || '未识别到文字内容' }}
        </div>
        <div style="margin-top: 12px; color: #999; font-size: 12px;">
          提示：识别结果仅供参考，可能存在误差。如需精确内容，请人工校对。
        </div>
      </div>
      <template #footer>
        <Space>
          <Button @click="ocrModalVisible = false">关闭</Button>
          <Button type="primary" :disabled="!ocrResult" @click="handleCopyOcrResult">
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
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
  font-size: 13px;
  color: #666;
  gap: 12px;
}

/* 文档列表 */
.doc-list {
  min-height: 100px;
}

/* 单个文档项 */
.doc-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
  transition: background-color 0.2s;
}

.doc-item:hover {
  background-color: #f8fafc;
}

.doc-item:last-child {
  border-bottom: none;
}

/* 列宽度 */
.col-drag { width: 30px; flex-shrink: 0; }
.col-type { width: 80px; flex-shrink: 0; text-align: center; }
.col-size { width: 80px; flex-shrink: 0; text-align: right; font-size: 12px; color: #888; }
.col-time { width: 140px; flex-shrink: 0; text-align: right; font-size: 12px; color: #888; }
.col-action { width: 160px; flex-shrink: 0; text-align: right; }

/* 拖拽手柄 */
.drag-handle {
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drag-handle:active {
  cursor: grabbing;
}

/* 文档名称链接 */
.doc-name-link {
  font-weight: 500;
  color: #1890ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.doc-name-link:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 文档描述 */
.doc-desc {
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作按钮 */
.action-btn {
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
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
  opacity: 0.4;
  background: #e6f7ff;
}

.sortable-drag {
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
