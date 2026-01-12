<script setup lang="ts">
import type { EvidenceItem } from './types';

import type {
  CreateEvidenceCommand,
  EvidenceExportItem,
  UpdateEvidenceCommand,
} from '#/api/evidence';

/**
 * 证据登记表 - 表格式编辑器
 * 支持动态增减行和列
 */
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

import { ChevronDown, ChevronUp, Download, Printer } from '@vben/icons';

import {
  Button,
  Checkbox,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  createEvidence,
  deleteEvidence,
  exportEvidenceList,
  getEvidenceGroups,
  getEvidenceListDetail,
  updateEvidence,
  updateEvidenceList,
} from '#/api/evidence';

// 列定义
interface ColumnDef {
  key: string;
  title: string;
  width: number;
  required?: boolean;
  editable?: boolean;
  type?: 'number' | 'select' | 'switch' | 'text' | 'textarea';
  options?: { label: string; value: string }[]; // 下拉选项（用于 select 类型）
  isSystem?: boolean; // 系统列不可删除
  isCustom?: boolean; // 用户自定义列（可以真正删除）
  hidden?: boolean; // 是否隐藏
}

// 表格行数据
interface TableRow {
  id?: number;
  key: string;
  order: number;
  [field: string]: any;
  isNew?: boolean;
  isDirty?: boolean;
}

const props = defineProps<{
  evidences: EvidenceItem[];
  matterId: number;
  matterName?: string; // 案件名称，用于打印显示
  listId?: number; // 关联的证据清单ID
  listName?: string; // 清单名称，用于打印显示
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'export', format: 'pdf' | 'word'): void;
}>();

// 默认列配置（预设列）
const defaultColumns: ColumnDef[] = [
  { key: 'groupName', title: '组别', width: 120, editable: true, type: 'text' }, // 组别列，分组依据，使用文本输入
  { key: 'order', title: '序号', width: 60, editable: false }, // 序号列，按组别分组，每组从1开始
  {
    key: 'name',
    title: '证据名称',
    width: 180,
    required: true,
    editable: true,
    type: 'text',
  },
  { key: 'quantity', title: '数量', width: 80, editable: true, type: 'number' },
  {
    key: 'isOriginal',
    title: '是否原件',
    width: 100,
    editable: true,
    type: 'switch',
  },
  {
    key: 'proofContent',
    title: '证明内容',
    width: 200,
    editable: true,
    type: 'textarea',
  },
  {
    key: 'provePurpose',
    title: '证明目的',
    width: 200,
    editable: true,
    type: 'textarea',
  },
  {
    key: 'pageCount',
    title: '页数',
    width: 80,
    editable: true,
    type: 'number',
  },
  {
    key: 'pageRange',
    title: '页码范围',
    width: 100,
    editable: false, // 自动计算，不可编辑
    type: 'text',
  },
];

// 可选的额外列
const optionalColumns: ColumnDef[] = [
  { key: 'source', title: '来源', width: 120, editable: true, type: 'text' },
  {
    key: 'evidenceType',
    title: '证据类型',
    width: 100,
    editable: true,
    type: 'text',
  },
  { key: 'copies', title: '份数', width: 80, editable: true, type: 'number' },
  {
    key: 'submitTime',
    title: '提交时间',
    width: 120,
    editable: true,
    type: 'text',
  },
  { key: 'remark', title: '备注', width: 150, editable: true, type: 'text' },
];

// 当前列配置
const columns = ref<ColumnDef[]>([...defaultColumns]);
const tableData = ref<TableRow[]>([]);
const saving = ref(false);
const groups = ref<string[]>([]); // 组别列表

// 修改组别弹窗
const groupEditModalVisible = ref(false);
const editingRowKey = ref<string | null>(null);
const editingGroupName = ref('');

// 打开修改组别弹窗
function openGroupEditModal(record: any) {
  editingRowKey.value = record.key;
  editingGroupName.value = record.groupName || '';
  groupEditModalVisible.value = true;
}

// 确认修改组别
function confirmGroupEdit() {
  if (!editingRowKey.value) return;
  
  const row = tableData.value.find(r => r.key === editingRowKey.value);
  if (row) {
    row.groupName = editingGroupName.value;
    row.isDirty = true;
  }
  
  groupEditModalVisible.value = false;
  editingRowKey.value = null;
}

// 排序后的表格数据（按组别排序，同组内保持原顺序）
const sortedTableData = computed(() => {
  // 按组别分组
  const groupMap = new Map<string, TableRow[]>();
  const groupOrder: string[] = []; // 记录组别出现的顺序
  
  tableData.value.forEach((row) => {
    const groupName = row.groupName || '未分组';
    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, []);
      groupOrder.push(groupName);
    }
    groupMap.get(groupName)!.push(row);
  });
  
  // 按组别顺序重组数据，并添加合并信息
  const result: TableRow[] = [];
  groupOrder.forEach((groupName) => {
    const rows = groupMap.get(groupName) || [];
    // 设置组别合并信息（第一行设置 rowSpan 为组内行数，其他行设置为 0）
    rows.forEach((row, index) => {
      row._groupRowSpan = index === 0 ? rows.length : 0;
    });
    result.push(...rows);
  });
  
  // 设置全局序号（按最终顺序递增）
  result.forEach((row, index) => {
    row.order = index + 1;
  });
  
  // 自动计算页码范围（根据页数累计）
  let currentPage = 1;
  result.forEach((row) => {
    const pageCount = typeof row.pageCount === 'number' && row.pageCount > 0 ? row.pageCount : 0;
    if (pageCount > 0) {
      const startPage = currentPage;
      const endPage = currentPage + pageCount - 1;
      row.pageRange = `${startPage}-${endPage}`;
      currentPage = endPage + 1;
    } else {
      row.pageRange = ''; // 没有输入页数则不显示
    }
  });
  
  return result;
});

// 列管理弹窗
const columnModalVisible = ref(false);
const newColumnTitle = ref('');
const selectedOptionalColumns = ref<string[]>([]);

// 加载组别列表
async function loadGroups() {
  if (!props.matterId) return;
  try {
    const groupList = await getEvidenceGroups(props.matterId);
    groups.value = groupList || [];
    // 更新组别列的选项
    updateGroupNameColumnOptions();
  } catch (error: any) {
    console.error('加载组别列表失败:', error);
  }
}

// 更新组别列的选项
function updateGroupNameColumnOptions() {
  const groupOptions = groups.value.map((g) => ({ label: g, value: g }));
  // 更新默认列中的组别列选项
  const defaultGroupNameCol = defaultColumns.find((c) => c.key === 'groupName');
  if (defaultGroupNameCol) {
    defaultGroupNameCol.options = groupOptions;
  }
  // 更新当前已添加的组别列选项
  columns.value.forEach((col) => {
    if (col.key === 'groupName') {
      col.options = groupOptions;
    }
  });
}

// 处理组别搜索（支持输入新组别）
function handleGroupSearch(value: string) {
  if (value && value.trim() && !groups.value.includes(value.trim())) {
    groups.value.push(value.trim());
    updateGroupNameColumnOptions();
  }
}

// 从本地存储加载列配置
onMounted(async () => {
  // 加载组别列表
  await loadGroups();

  // 加载列配置
  const savedColumns = localStorage.getItem(
    `evidence-columns-${props.matterId}`,
  );
  if (savedColumns) {
    try {
      const parsedColumns = JSON.parse(savedColumns);
      // 验证是否包含预设列，如果没有则使用预设列
      const hasGroupName = parsedColumns.some(
        (c: ColumnDef) => c.key === 'groupName',
      );
      const hasOrder = parsedColumns.some((c: ColumnDef) => c.key === 'order');
      const hasName = parsedColumns.some((c: ColumnDef) => c.key === 'name');

      if (hasGroupName && hasOrder && hasName) {
        columns.value = parsedColumns;
      } else {
        // 如果保存的配置不完整，使用预设列
        columns.value = [...defaultColumns];
      }
      // 更新组别列选项
      updateGroupNameColumnOptions();
    } catch {
      // 解析失败，使用预设列
      columns.value = [...defaultColumns];
      updateGroupNameColumnOptions();
    }
  } else {
    // 没有保存的配置，使用预设列
    columns.value = [...defaultColumns];
    updateGroupNameColumnOptions();
  }
});

// 保存列配置到本地存储
function saveColumnConfig() {
  localStorage.setItem(
    `evidence-columns-${props.matterId}`,
    JSON.stringify(columns.value),
  );
}

// 初始化表格数据（按组别分组，每组内序号从1开始）
watch(
  () => props.evidences,
  (evidences) => {
    // 按组别分组
    const groupedEvidences = new Map<string, typeof evidences>();
    evidences.forEach((e) => {
      const groupName = e.groupName || '未分组';
      if (!groupedEvidences.has(groupName)) {
        groupedEvidences.set(groupName, []);
      }
      groupedEvidences.get(groupName)!.push(e);
    });

    // 转换为表格数据，每组内的序号从1开始
    tableData.value = [];
    groupedEvidences.forEach((groupEvidences, groupName) => {
      groupEvidences.forEach((e, groupIndex) => {
        tableData.value.push({
          id: e.id,
          key: `evidence-${e.id}`,
          order: groupIndex + 1, // 组内序号，从1开始
          groupName: groupName === '未分组' ? '' : groupName, // 组别
          name: e.name || '',
          quantity: (e.originalCount || 0) + (e.copyCount || 0), // 数量
          isOriginal: e.isOriginal ? '是' : '否', // 是否原件
          proofContent: e.description || '', // 证明内容
          provePurpose: e.provePurpose || '', // 证明目的
          pageStart: e.pageStart || undefined, // 页码起始
          // 其他可选字段
          source: e.source || '',
          pageEnd: e.pageEnd || undefined,
          pageCount:
            e.pageStart && e.pageEnd ? e.pageEnd - e.pageStart + 1 : undefined,
          remark: e.description || '',
          evidenceType: e.evidenceTypeName || '',
          copies: (e.originalCount || 0) + (e.copyCount || 0),
          submitTime: e.createdAt ? e.createdAt.split('T')[0] : '',
          pageRange:
            e.pageStart && e.pageEnd ? `${e.pageStart}-${e.pageEnd}` : '',
          isNew: false,
          isDirty: false,
        });
      });
    });
  },
  { immediate: true },
);

// 需要按组别合并的列
const mergeColumns = ['groupName', 'proofContent', 'provePurpose'];

// 需要首行缩进、两端对齐的列
const textColumns = ['proofContent', 'provePurpose'];

// 计算表格列（过滤掉隐藏的列）
const tableColumns = computed(() => {
  const visibleColumns = columns.value.filter((col) => !col.hidden);
  const cols = visibleColumns.map((col) => ({
    title: col.title,
    dataIndex: col.key,
    key: col.key,
    width: col.width,
    align: 'center' as const, // 表头居中对齐
    onHeaderCell: () => ({
      'data-column-key': col.key,
    }),
    // 组别、证明内容、证明目的列添加单元格合并和特殊样式
    customCell: (record: TableRow) => {
      const cellProps: Record<string, any> = {};
      
      // 合并单元格
      if (mergeColumns.includes(col.key)) {
        cellProps.rowSpan = record._groupRowSpan;
      }
      
      // 证明内容和证明目的列：首行缩进、两端对齐、顶部对齐
      if (textColumns.includes(col.key)) {
        cellProps.class = 'text-column-cell';
      }
      
      return cellProps;
    },
  }));

  // 不再添加操作列，删除功能通过行内按钮实现

  return cols;
});

// === 行操作 ===

// 创建新行（通用方法）
function createNewRow(groupName?: string): TableRow {
  const newRow: TableRow = {
    key: `new-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    order: 1, // 默认序号为1，会根据组别调整
    isNew: true,
    isDirty: true,
    groupName: groupName || '', // 继承组别
  };

  // 初始化所有列的值
  columns.value.forEach((col) => {
    if (col.key !== 'order' && col.key !== 'groupName') {
      newRow[col.key] =
        col.key === 'quantity' ||
        col.key === 'pageStart' ||
        col.key === 'pageEnd' ||
        col.key === 'pageCount'
          ? undefined
          : '';
    }
  });

  return newRow;
}

// 添加新行（末尾）
function handleAddRow() {
  const newRow = createNewRow();
  tableData.value.push(newRow);
  // 更新行序号（按组别分组）
  updateRowOrder();
}

// 在指定行上方插入新行
function handleInsertRowAbove(rowKey: string) {
  const sortedData = sortedTableData.value;
  const sortedIndex = sortedData.findIndex(r => r.key === rowKey);
  if (sortedIndex === -1) return;
  
  const currentRow = sortedData[sortedIndex];
  const newRow = createNewRow(currentRow?.groupName); // 继承当前行的组别
  
  // 在原始数据中找到位置并插入
  // 由于 sortedTableData 是按组别排序的，我们需要在 tableData 中正确的位置插入
  const originalIndex = tableData.value.findIndex(r => r.key === rowKey);
  if (originalIndex !== -1) {
    tableData.value.splice(originalIndex, 0, newRow);
  } else {
    tableData.value.push(newRow);
  }
  
  updateRowOrder();
  message.success('已在上方插入新行');
}

// 在指定行下方插入新行
function handleInsertRowBelow(rowKey: string) {
  const sortedData = sortedTableData.value;
  const sortedIndex = sortedData.findIndex(r => r.key === rowKey);
  if (sortedIndex === -1) return;
  
  const currentRow = sortedData[sortedIndex];
  const newRow = createNewRow(currentRow?.groupName); // 继承当前行的组别
  
  // 在原始数据中找到位置并插入
  const originalIndex = tableData.value.findIndex(r => r.key === rowKey);
  if (originalIndex !== -1) {
    tableData.value.splice(originalIndex + 1, 0, newRow);
  } else {
    tableData.value.push(newRow);
  }
  
  updateRowOrder();
  message.success('已在下方插入新行');
}

// 删除行（通过 key 查找实际位置）
async function handleDeleteRow(rowKey: string) {
  const index = tableData.value.findIndex(r => r.key === rowKey);
  if (index === -1) return;
  
  const row = tableData.value[index];
  if (row?.id) {
    try {
      await deleteEvidence(row.id);
      message.success('删除成功');
    } catch (error: any) {
      message.error(error.message || '删除失败');
      return;
    }
  }
  tableData.value.splice(index, 1);
  updateRowOrder();
}

// 更新行序号（按组别分组，每组内从1开始）
function updateRowOrder() {
  // 按组别分组
  const groupMap = new Map<string, TableRow[]>();
  tableData.value.forEach((row) => {
    const groupName = row.groupName || '未分组';
    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, []);
    }
    groupMap.get(groupName)!.push(row);
  });

  // 为每组内的行分配序号
  groupMap.forEach((rows) => {
    rows.forEach((row, index) => {
      row.order = index + 1;
    });
  });
}

// 标记为已修改（通过 key 查找）
function markDirty(rowKey: string) {
  const row = tableData.value.find(r => r.key === rowKey);
  if (row) {
    row.isDirty = true;
  }
}

// === 列操作 ===

// 显示列管理弹窗
function showColumnModal() {
  // 检查哪些可选列已添加
  selectedOptionalColumns.value = optionalColumns
    .filter((opt) => columns.value.some((col) => col.key === opt.key))
    .map((opt) => opt.key);
  newColumnTitle.value = '';
  columnModalVisible.value = true;
}

// 添加可选列（添加到末尾）
function handleToggleOptionalColumn(key: string, checked: boolean) {
  nextTick(() => {
    if (checked) {
      const optCol = optionalColumns.find((c) => c.key === key);
      if (optCol && !columns.value.some((c) => c.key === key)) {
        // 添加到末尾
        columns.value.push({ ...optCol });
        saveColumnConfig();
        message.success(`已添加列"${optCol.title}"，可在下方调整位置`);
      }
    } else {
      const index = columns.value.findIndex((c) => c.key === key);
      if (index !== -1) {
        columns.value.splice(index, 1);
        saveColumnConfig();
        message.success('列已移除');
      }
    }
  });
}

// 添加自定义列
function handleAddCustomColumn() {
  if (!newColumnTitle.value.trim()) {
    message.warning('请输入列标题');
    return;
  }

  const key = `custom_${Date.now()}`;
  const title = newColumnTitle.value.trim();

  nextTick(() => {
    columns.value.push({
      key,
      title,
      width: 150,
      editable: true,
      type: 'text',
      isCustom: true, // 标记为自定义列，可以真正删除
    });

    // 为现有行初始化该列的值
    tableData.value.forEach((row) => {
      row[key] = '';
    });

    newColumnTitle.value = '';
    saveColumnConfig();
    message.success(`已添加列"${title}"，可在下方调整位置`);
  });
}

// 删除/隐藏列
function handleDeleteColumn(key: string) {
  nextTick(() => {
    const col = columns.value.find((c) => c.key === key);
    if (!col) return;
    
    // 自定义列可以真正删除
    if (col.isCustom) {
      const index = columns.value.findIndex((c) => c.key === key);
      if (index !== -1) {
        columns.value.splice(index, 1);
        saveColumnConfig();
        message.success('列已删除');
      }
    } else {
      // 预设列只能隐藏
      col.hidden = true;
      saveColumnConfig();
      message.success('列已隐藏，可在"管理列"中重新显示');
    }
  });
}

// 显示/隐藏列
function toggleColumnVisibility(key: string, visible: boolean) {
  const col = columns.value.find((c) => c.key === key);
  if (col) {
    col.hidden = !visible;
    saveColumnConfig();
  }
}

// 列上移
function handleMoveColumnUp(index: number) {
  if (index <= 0) {
    return;
  }

  nextTick(() => {
    const currentCol = columns.value[index];
    if (!currentCol) return;

    // 交换位置
    [columns.value[index], columns.value[index - 1]] = [
      columns.value[index - 1]!,
      columns.value[index]!,
    ];
    saveColumnConfig();
    message.success(`已将"${currentCol.title}"上移`);
  });
}

// 列下移
function handleMoveColumnDown(index: number) {
  if (index >= columns.value.length - 1) {
    return;
  }

  nextTick(() => {
    const currentCol = columns.value[index];
    if (!currentCol) return;

    // 交换位置
    [columns.value[index], columns.value[index + 1]] = [
      columns.value[index + 1]!,
      columns.value[index]!,
    ];
    saveColumnConfig();
    message.success(`已将"${currentCol.title}"下移`);
  });
}

// 更新列宽
function handleUpdateColumnWidth(key: string, width: null | number) {
  const col = columns.value.find((c) => c.key === key);
  if (col && width) {
    col.width = Math.max(50, Math.min(1000, width)); // 限制在 50-1000 之间
    saveColumnConfig();
  }
}

// 确认列配置
function handleColumnModalOk() {
  nextTick(() => {
    columnModalVisible.value = false;
  });
}

// 列宽拖动调整功能
let isResizing = false;
let currentCol: HTMLElement | null = null;
let startX = 0;
let startWidth = 0;
let currentColKey = '';

function handleMouseDown(e: Event) {
  const mouseEvent = e as MouseEvent;
  const target = mouseEvent.target as HTMLElement;

  // 如果点击的是按钮或其他交互元素，不处理
  if (target.tagName === 'BUTTON' || target.closest('button')) return;

  const th = target.closest('th') as HTMLElement;
  if (
    !th ||
    th.classList.contains('fixed-left') ||
    th.classList.contains('fixed-right')
  )
    return;

  const rect = th.getBoundingClientRect();
  const rightEdge = rect.right;

  // 检查是否点击在列的右边缘（4px范围内）
  if (
    mouseEvent.clientX >= rightEdge - 4 &&
    mouseEvent.clientX <= rightEdge + 4
  ) {
    isResizing = true;
    currentCol = th;
    startX = mouseEvent.clientX;
    startWidth = th.offsetWidth;
    // 从 data-column-key 属性获取 key
    currentColKey = th.dataset.columnKey || '';
    th.classList.add('resizing');
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    mouseEvent.preventDefault();
    mouseEvent.stopPropagation();
  }
}

function handleMouseMove(e: Event) {
  if (!isResizing || !currentCol) return;

  const mouseEvent = e as MouseEvent;
  const diff = mouseEvent.clientX - startX;
  const newWidth = Math.max(50, startWidth + diff);

  // 更新列宽
  if (currentColKey) {
    const col = columns.value.find((c) => c.key === currentColKey);
    if (col) {
      col.width = newWidth;
    }
  }

  mouseEvent.preventDefault();
}

function handleMouseUp() {
  if (isResizing && currentCol) {
    currentCol.classList.remove('resizing');
    saveColumnConfig();
  }
  isResizing = false;
  currentCol = null;
  currentColKey = '';
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

// 初始化拖动功能
onMounted(() => {
  nextTick(() => {
    // 绑定到整个文档，这样可以在任何地方拖动
    document.addEventListener('mousedown', handleMouseDown as EventListener);
    document.addEventListener('mousemove', handleMouseMove as EventListener);
    document.addEventListener('mouseup', handleMouseUp as EventListener);
  });
});

onUnmounted(() => {
  document.removeEventListener('mousedown', handleMouseDown as EventListener);
  document.removeEventListener('mousemove', handleMouseMove as EventListener);
  document.removeEventListener('mouseup', handleMouseUp as EventListener);
});

// === 保存和导出 ===

// 处理输入变化（通过 key 查找）
function handleInputChange(rowKey: string) {
  markDirty(rowKey);
  // sortedTableData 计算属性会自动处理排序和序号更新
}

// 从页码范围字符串解析起始和结束页码
function parsePageRange(pageRange: string): { pageStart?: number; pageEnd?: number } {
  if (!pageRange || !pageRange.includes('-')) {
    return {};
  }
  const parts = pageRange.split('-');
  const pageStart = parseInt(parts[0] || '', 10);
  const pageEnd = parseInt(parts[1] || '', 10);
  return {
    pageStart: isNaN(pageStart) ? undefined : pageStart,
    pageEnd: isNaN(pageEnd) ? undefined : pageEnd,
  };
}

// 保存所有修改
async function handleSaveAll() {
  const dirtyRows = tableData.value.filter((row) => row.isDirty);
  if (dirtyRows.length === 0) {
    message.info('没有需要保存的修改');
    return;
  }

  saving.value = true;
  try {
    const newEvidenceIds: number[] = []; // 收集新创建的证据ID
    
    // 使用 sortedTableData 获取计算后的页码范围
    const sortedData = sortedTableData.value;
    
    for (const row of dirtyRows) {
      // 从排序后的数据中找到对应行，获取计算后的页码范围
      const sortedRow = sortedData.find(r => r.key === row.key);
      const { pageStart, pageEnd } = parsePageRange(sortedRow?.pageRange || '');
      
      if (row.isNew) {
        const command: CreateEvidenceCommand = {
          matterId: props.matterId,
          name: row.name || '未命名证据',
          evidenceType: 'DOCUMENTARY',
          source: row.source,
          groupName: row.groupName || undefined, // 保存组别
          provePurpose: row.provePurpose || row.proofContent, // 证明目的
          description: row.proofContent || row.remark, // 证明内容/描述
          isOriginal: row.isOriginal === '是' || row.isOriginal === true,
          pageStart, // 使用计算后的页码起始
          pageEnd, // 使用计算后的页码结束
          originalCount:
            typeof row.quantity === 'number' ? row.quantity : undefined,
        };
        const newEvidence = await createEvidence(command);
        if (newEvidence?.id) {
          newEvidenceIds.push(newEvidence.id);
        }
      } else if (row.id) {
        const command: UpdateEvidenceCommand = {
          name: row.name,
          source: row.source,
          groupName: row.groupName || undefined, // 保存组别
          provePurpose: row.provePurpose || row.proofContent, // 证明目的
          description: row.proofContent || row.remark, // 证明内容/描述
          isOriginal: row.isOriginal === '是' || row.isOriginal === true,
          pageStart, // 使用计算后的页码起始
          pageEnd, // 使用计算后的页码结束
          originalCount:
            typeof row.quantity === 'number' ? row.quantity : undefined,
        };
        await updateEvidence(row.id, command);
      }
    }
    
    // 如果有新创建的证据且有关联的清单，需要将新证据添加到清单中
    if (newEvidenceIds.length > 0 && props.listId) {
      try {
        // 获取当前清单的证据ID列表
        const listDetail = await getEvidenceListDetail(props.listId);
        const currentEvidenceIds = listDetail.evidenceIdList || [];
        // 合并新证据ID到清单
        const updatedEvidenceIds = [...currentEvidenceIds, ...newEvidenceIds];
        await updateEvidenceList(props.listId, {}, updatedEvidenceIds);
      } catch (error: any) {
        console.error('更新清单证据列表失败:', error);
        // 不阻断主流程，只是提示
        message.warning('证据已保存，但添加到清单失败，请手动刷新');
      }
    }
    
    message.success(`保存成功，共更新 ${dirtyRows.length} 条记录`);
    // 刷新后重新加载组别列表
    await loadGroups();
    emit('refresh');
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

// 导出
async function handleExport(format: 'pdf' | 'word') {
  if (tableData.value.length === 0) {
    message.warning('暂无证据可导出');
    return;
  }

  try {
    message.loading('正在导出...', 0);

    // 转换为导出格式
    const exportItems: EvidenceExportItem[] = tableData.value
      .filter((row) => row.name && row.name.trim())
      .map((row, index) => ({
        id: row.id,
        name: row.name || '',
        source: row.source || '',
        provePurpose: row.proofContent || '',
        isOriginal: row.isOriginal === true || row.isOriginal === '是',
        pageStart:
          typeof row.pageStart === 'number' ? row.pageStart : undefined,
        pageEnd: typeof row.pageEnd === 'number' ? row.pageEnd : undefined,
        listOrder: index + 1,
      }));

    await exportEvidenceList(props.matterId, exportItems, format);
    message.destroy();
    message.success('导出成功');
  } catch (error: any) {
    message.destroy();
    message.error(error.message || '导出失败');
  }
}

// 打印
function handlePrint() {
  if (tableData.value.length === 0) {
    message.warning('暂无证据可打印');
    return;
  }

  // 创建打印窗口
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器弹窗设置');
    return;
  }

  // 获取可见列（过滤掉隐藏的列）
  const visibleColumns = columns.value.filter((col) => !col.hidden);
  
  // 计算总宽度用于百分比（只计算可见列）
  const totalWidth = visibleColumns.reduce((sum, col) => sum + (col.width || 100), 0);

  // 生成打印内容
  let printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>证据登记表</title>
      <style>
        @media print {
          @page {
            size: A4 landscape;
            margin: 1.5cm;
          }
        }
        body {
          font-family: "Microsoft YaHei", "SimSun", serif;
          font-size: 12px;
          line-height: 1.6;
          color: #000;
          padding: 10px;
        }
        .print-header {
          text-align: center;
          margin-bottom: 20px;
        }
        .print-header h1 {
          font-size: 20px;
          font-weight: bold;
          margin: 0 0 10px 0;
        }
        .print-table {
          width: 100%;
          border-collapse: collapse;
          margin-bottom: 20px;
          table-layout: fixed; /* 固定列宽，不根据内容自动调整 */
        }
        .print-table th,
        .print-table td {
          border: 1px solid #000;
          padding: 6px 8px;
          font-size: 11px;
          word-wrap: break-word; /* 允许文字换行 */
          overflow-wrap: break-word;
          vertical-align: middle; /* 所有列垂直居中 */
          text-align: center;
        }
        .print-table th {
          background-color: #f0f0f0;
          font-weight: bold;
        }
        /* 证明内容和证明目的列 - 首行缩进、两端对齐 */
        .print-table td.col-proofContent,
        .print-table td.col-provePurpose {
          text-indent: 2em;
          text-align: justify;
        }
        .print-case-info {
          text-align: center;
          margin-bottom: 15px;
          font-size: 14px;
          color: #333;
        }
      </style>
    </head>
    <body>
      <div class="print-header">
        <h1>证据登记表</h1>
        ${props.matterName ? `<div class="print-case-info">${props.matterName}</div>` : ''}
      </div>
      <table class="print-table">
        <thead>
          <tr>
  `;

  // 添加表头（使用配置的列宽，只显示可见列）
  visibleColumns.forEach((col) => {
    if (col.key !== 'key') {
      // 将像素宽度转换为百分比
      const widthPercent = ((col.width || 100) / totalWidth * 100).toFixed(1);
      printContent += `<th class="col-${col.key}" style="width: ${widthPercent}%">${col.title}</th>`;
    }
  });

  printContent += `
          </tr>
        </thead>
        <tbody>
  `;

  // 使用 sortedTableData 来保持与前端显示一致的顺序和合并信息
  // 添加数据行（带单元格合并，只显示可见列）
  sortedTableData.value.forEach((row) => {
    if (row.name && row.name.trim()) {
      printContent += '<tr>';
      visibleColumns.forEach((col) => {
        if (col.key !== 'key') {
          // 需要合并的列（组别、证明内容、证明目的）
          const isMergeColumn = mergeColumns.includes(col.key);
          
          // 如果是合并列且 rowSpan 为 0，跳过此单元格（已被上方单元格合并）
          if (isMergeColumn && row._groupRowSpan === 0) {
            return;
          }
          
          // 构建单元格属性
          const rowSpanAttr = isMergeColumn && row._groupRowSpan > 1 
            ? ` rowspan="${row._groupRowSpan}"` 
            : '';
          
          // 序号列显示动态生成的序号
          if (col.key === 'order') {
            printContent += `<td class="col-${col.key}">${row.order}</td>`;
          } else {
            const value = row[col.key] || '';
            const displayValue =
              value === true ? '是' : value === false ? '否' : String(value);
            printContent += `<td class="col-${col.key}"${rowSpanAttr}>${displayValue}</td>`;
          }
        }
      });
      printContent += '</tr>';
    }
  });

  printContent += `
          </tbody>
        </table>
      </body>
    </html>
  `;

  printWindow.document.write(printContent);
  printWindow.document.close();

  // 等待内容加载后打印
  printWindow.addEventListener('load', () => {
    setTimeout(() => {
      printWindow.print();
    }, 250);
  });
}

// 计算是否有未保存的修改
const hasDirtyRows = computed(() => tableData.value.some((row) => row.isDirty));

// 计算最后一列的 key（用于显示删除按钮，使用可见列）
const lastColumnKey = computed(() => {
  const visibleColumns = columns.value.filter((col) => !col.hidden);
  return visibleColumns.length > 0
    ? visibleColumns[visibleColumns.length - 1]?.key
    : null;
});
</script>

<template>
  <div class="evidence-table-editor">
    <!-- 工具栏 -->
    <div
      class="toolbar"
      style="
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 16px;
      "
    >
      <Space>
        <Button v-if="!readonly" type="primary" @click="handleAddRow">
          ➕ 添加行
        </Button>
        <Button v-if="!readonly" @click="showColumnModal"> 📊 管理列 </Button>
        <Button
          v-if="!readonly"
          type="primary"
          :loading="saving"
          :disabled="!hasDirtyRows"
          @click="handleSaveAll"
        >
          💾 保存{{ hasDirtyRows ? ` (${tableData.filter(r => r.isDirty).length})` : '' }}
        </Button>
      </Space>
      <Space>
        <Button @click="handlePrint">
          <Printer class="h-4 w-4" />
          打印
        </Button>
        <Button @click="handleExport('word')">
          <Download class="h-4 w-4" />
          导出 Word
        </Button>
        <Button @click="handleExport('pdf')">
          <Download class="h-4 w-4" />
          导出 PDF
        </Button>
      </Space>
    </div>

    <!-- 表格标题 -->
    <div
      class="table-title"
      style="
        margin-bottom: 8px;
        font-size: 18px;
        font-weight: bold;
        text-align: center;
      "
    >
      证据登记表
    </div>

    <!-- 使用说明 -->
    <div class="usage-tips">
      <span class="tip-icon">💡</span>
      <span class="tip-text">
        <strong>组别说明：</strong>
        输入相同组别名称的证据会自动归为一组，组别、证明内容、证明目的列会合并显示。
        <strong>如需修改组别：</strong>首行直接编辑；其他行点击右侧 📝 按钮修改。
      </span>
    </div>

    <!-- 表格 -->
    <Table
      :columns="tableColumns"
      :data-source="sortedTableData"
      :pagination="false"
      :scroll="{ x: 1200 }"
      bordered
      size="middle"
      row-key="key"
    >
      <template #bodyCell="{ column, record }">
        <!-- 合并列（组别、证明内容、证明目的）- 被合并的单元格不渲染内容 -->
        <template v-if="['groupName', 'proofContent', 'provePurpose'].includes(column.key as string) && record._groupRowSpan === 0">
          <!-- 空，被合并的行不显示 -->
        </template>
        
        <!-- 序号列 - 动态生成，不依赖 order 字段 -->
        <template v-else-if="column.key === 'order'">
          <span>{{ record.order || 1 }}</span>
        </template>

        <!-- 可编辑列 -->
        <template v-else>
          <div class="cell-content-wrapper">
            <div class="cell-content">
              <!-- 不可编辑列（如页码范围）显示为只读文本 -->
              <span v-if="column.key && columns.find((c) => c.key === column.key)?.editable === false">
                {{ record[column.key] || '' }}
              </span>
              <template v-else-if="!readonly">
                <!-- 下拉选择（组别列） -->
                <Select
                  v-if="
                    column.key &&
                    columns.find((c) => c.key === column.key)?.type === 'select'
                  "
                  v-model:value="record[column.key!]"
                  :options="
                    columns.find((c) => c.key === column.key)?.options || []
                  "
                  placeholder="选择或输入组别"
                  allow-clear
                  show-search
                  :filter-option="
                    (input: string, option: any) =>
                      (option?.label ?? '')
                        .toLowerCase()
                        .includes(input.toLowerCase())
                  "
                  style="width: 100%"
                  @change="handleInputChange(record.key)"
                  @search="handleGroupSearch"
                />
                <!-- Switch开关（是否原件） -->
                <Switch
                  v-else-if="
                    column.key &&
                    columns.find((c) => c.key === column.key)?.type === 'switch'
                  "
                  :checked="
                    record[column.key!] === true || record[column.key!] === '是'
                  "
                  @change="
                    (checked: any) => {
                      record[column.key!] = checked ? '是' : '否';
                      handleInputChange(record.key);
                    }
                  "
                />
                <!-- 数字输入 -->
                <InputNumber
                  v-else-if="
                    column.key &&
                    columns.find((c) => c.key === column.key)?.type === 'number'
                  "
                  v-model:value="record[column.key!]"
                  :controls="false"
                  style="width: 100%"
                  @change="handleInputChange(record.key)"
                />
                <!-- 多行文本 -->
                <Input.TextArea
                  v-else-if="
                    column.key &&
                    columns.find((c) => c.key === column.key)?.type ===
                      'textarea'
                  "
                  v-model:value="record[column.key!]"
                  :auto-size="{ minRows: 2 }"
                  @change="handleInputChange(record.key)"
                />
                <!-- 单行文本 -->
                <Input
                  v-else-if="column.key"
                  v-model:value="record[column.key]"
                  @change="handleInputChange(record.key)"
                />
              </template>
              <span v-else>{{ column.key ? record[column.key] : '' }}</span>
            </div>
            <!-- 最后一列显示操作按钮 -->
            <template v-if="!readonly && column.key === lastColumnKey">
              <!-- 修改组别按钮（被合并行显示） -->
              <Tooltip v-if="record._groupRowSpan === 0" title="修改组别">
                <Button
                  type="text"
                  size="small"
                  style="flex-shrink: 0; padding: 0 4px"
                  @click="openGroupEditModal(record)"
                >
                  📝
                </Button>
              </Tooltip>
              <!-- 在上方插入行 -->
              <Tooltip title="在上方插入行">
                <Button
                  type="text"
                  size="small"
                  style="flex-shrink: 0; padding: 0 4px"
                  @click="handleInsertRowAbove(record.key)"
                >
                  ⬆️
                </Button>
              </Tooltip>
              <!-- 在下方插入行 -->
              <Tooltip title="在下方插入行">
                <Button
                  type="text"
                  size="small"
                  style="flex-shrink: 0; padding: 0 4px"
                  @click="handleInsertRowBelow(record.key)"
                >
                  ⬇️
                </Button>
              </Tooltip>
              <!-- 删除按钮 -->
              <Popconfirm
                title="确定删除此行吗？"
                @confirm="handleDeleteRow(record.key)"
              >
                <Tooltip title="删除">
                  <Button
                    type="text"
                    size="small"
                    danger
                    style="flex-shrink: 0; padding: 0 4px"
                  >
                    🗑️
                  </Button>
                </Tooltip>
              </Popconfirm>
            </template>
          </div>
        </template>
      </template>

      <!-- 表头自定义（显示删除按钮） -->
      <template #headerCell="{ column }">
        <div class="header-cell-wrapper">
          <span class="header-title">{{ column.title }}</span>
          <Tooltip v-if="!readonly && column.key" title="删除此列">
            <Button
              type="text"
              size="small"
              class="header-delete-btn"
              @click.stop="handleDeleteColumn(String(column.key))"
            >
              ✕
            </Button>
          </Tooltip>
        </div>
      </template>
    </Table>

    <!-- 添加行按钮（表格底部，类似清单式） -->
    <div v-if="!readonly" style="margin-top: 8px">
      <Button type="dashed" block @click="handleAddRow"> ➕ 添加行 </Button>
    </div>

    <!-- 底部信息 -->
    <div
      class="footer-info"
      style="
        display: flex;
        justify-content: space-between;
        margin-top: 16px;
        color: #666;
      "
    >
      <span>提交人：_______________</span>
      <span>第 ___ 页 共 ___ 页</span>
    </div>

    <!-- 列管理弹窗 -->
    <Modal
      v-model:open="columnModalVisible"
      title="管理表格列"
      @ok="handleColumnModalOk"
    >
      <div style="display: flex; flex-direction: column; gap: 16px">
        <!-- 可选列 -->
        <div>
          <div style="margin-bottom: 8px; font-weight: 500">可选列：</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px">
            <Checkbox
              v-for="opt in optionalColumns"
              :key="opt.key"
              :checked="columns.some((c) => c.key === opt.key)"
              @change="
                (e: any) =>
                  handleToggleOptionalColumn(opt.key, e.target.checked)
              "
            >
              {{ opt.title }}
            </Checkbox>
          </div>
        </div>

        <!-- 添加自定义列 -->
        <div>
          <div style="margin-bottom: 8px; font-weight: 500">添加自定义列：</div>
          <div style="display: flex; gap: 8px">
            <Input
              v-model:value="newColumnTitle"
              placeholder="输入列标题"
              style="flex: 1"
            />
            <Button type="primary" @click="handleAddCustomColumn">添加</Button>
          </div>
        </div>

        <!-- 当前列列表（可调整顺序和显示/隐藏） -->
        <div>
          <div style="margin-bottom: 8px; font-weight: 500">当前列顺序：</div>
          <div style="display: flex; flex-direction: column; gap: 8px">
            <div
              v-for="(col, index) in columns"
              :key="col.key"
              :style="{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '8px',
                background: col.hidden ? '#f5f5f5' : '#fafafa',
                border: '1px solid #e8e8e8',
                borderRadius: '4px',
                opacity: col.hidden ? 0.6 : 1,
              }"
            >
              <div
                style="display: flex; flex: 1; gap: 8px; align-items: center"
              >
                <!-- 显示/隐藏复选框 -->
                <Checkbox
                  :checked="!col.hidden"
                  @change="(e: any) => toggleColumnVisibility(col.key, e.target.checked)"
                />
                <span style="min-width: 30px; font-size: 12px; color: #999">{{
                  index + 1
                }}</span>
                <span :style="{ minWidth: '100px', fontWeight: 500, textDecoration: col.hidden ? 'line-through' : 'none' }">{{
                  col.title
                }}</span>
                <Tag v-if="col.isSystem" color="blue" size="small">系统</Tag>
                <Tag v-if="col.required" color="red" size="small">必填</Tag>
                <Tag v-if="col.isCustom" color="purple" size="small">自定义</Tag>
                <Tag v-if="col.hidden" color="default" size="small">已隐藏</Tag>
                <span style="margin-left: 8px; font-size: 12px; color: #999"
                  >列宽：</span
                >
                <InputNumber
                  :value="col.width"
                  :min="50"
                  :max="1000"
                  :step="10"
                  size="small"
                  style="width: 80px"
                  @change="
                    (val) =>
                      handleUpdateColumnWidth(
                        col.key,
                        typeof val === 'number' ? val : 150,
                      )
                  "
                />
                <span style="font-size: 12px; color: #999">px</span>
              </div>
              <Space>
                <Tooltip :title="index === 0 ? '已是第一列' : '上移'">
                  <Button
                    type="text"
                    size="small"
                    :disabled="index === 0"
                    @click="handleMoveColumnUp(index)"
                  >
                    <ChevronUp class="h-4 w-4" />
                  </Button>
                </Tooltip>
                <Tooltip
                  :title="
                    index === columns.length - 1 ? '已是最后一列' : '下移'
                  "
                >
                  <Button
                    type="text"
                    size="small"
                    :disabled="index === columns.length - 1"
                    @click="handleMoveColumnDown(index)"
                  >
                    <ChevronDown class="h-4 w-4" />
                  </Button>
                </Tooltip>
                <!-- 只有自定义列可以真正删除 -->
                <Button
                  v-if="col.isCustom"
                  type="text"
                  size="small"
                  danger
                  @click="handleDeleteColumn(col.key)"
                >
                  删除
                </Button>
              </Space>
            </div>
          </div>
        </div>
      </div>
    </Modal>

    <!-- 修改组别弹窗 -->
    <Modal
      v-model:open="groupEditModalVisible"
      title="修改组别"
      :width="360"
      @ok="confirmGroupEdit"
    >
      <div style="padding: 16px 0">
        <div style="margin-bottom: 8px; color: #666">
          修改此证据的组别，将从当前组移出：
        </div>
        <Input
          v-model:value="editingGroupName"
          placeholder="请输入新的组别名称"
          allow-clear
        />
        <div style="margin-top: 12px; font-size: 12px; color: #999">
          💡 提示：输入与其他证据相同的组别名称，将自动归入该组
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.usage-tips {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fffbe6 0%, #fff7e6 100%);
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
}

.usage-tips .tip-icon {
  flex-shrink: 0;
  font-size: 16px;
}

.usage-tips .tip-text {
  line-height: 1.6;
}

.usage-tips .tip-text strong {
  color: #d48806;
}

.evidence-table-editor :deep(.ant-table-cell) {
  padding: 8px !important;
  vertical-align: middle !important; /* 内容垂直居中 */
}

/* 表格行自动适应内容高度 */
.evidence-table-editor :deep(.ant-table-tbody > tr) {
  height: auto !important;
}

.evidence-table-editor :deep(.ant-table-tbody > tr > td) {
  height: auto !important;
  white-space: normal !important; /* 允许换行 */
  word-wrap: break-word;
  vertical-align: middle !important;
}

/* 单元格内容包装器 - 垂直居中 */
.cell-content-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-height: 32px;
}

.cell-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.evidence-table-editor :deep(.ant-input),
.evidence-table-editor :deep(.ant-input-textarea),
.evidence-table-editor :deep(.ant-input-number) {
  background: transparent;
  border: none;
}

.evidence-table-editor :deep(.ant-input:focus),
.evidence-table-editor :deep(.ant-input-textarea:focus),
.evidence-table-editor :deep(.ant-input-number:focus) {
  background: #fff;
  border: 1px solid #1890ff;
}

.evidence-table-editor :deep(.ant-input:hover),
.evidence-table-editor :deep(.ant-input-textarea:hover),
.evidence-table-editor :deep(.ant-input-number:hover) {
  background: #f5f5f5;
}

.evidence-table-editor :deep(.ant-table-thead th) {
  background: #fafafa;
}

/* 表头单元格样式 - 标题居中，删除按钮右侧 */
.header-cell-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.header-cell-wrapper .header-title {
  text-align: center;
}

.header-cell-wrapper .header-delete-btn {
  position: absolute;
  right: -8px;
  padding: 0 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.evidence-table-editor :deep(.ant-table-thead th:hover .header-delete-btn) {
  opacity: 0.5;
}

.evidence-table-editor :deep(.ant-table-thead th:hover .header-delete-btn:hover) {
  opacity: 1;
}

/* 证明内容和证明目的列 - 首行缩进、两端对齐、垂直居中 */
.evidence-table-editor :deep(.text-column-cell) {
  text-align: justify !important;
  vertical-align: middle !important;
}

.evidence-table-editor :deep(.text-column-cell .ant-input-textarea textarea) {
  text-indent: 2em;
  text-align: justify;
  min-height: 60px; /* 最小高度 */
}

.evidence-table-editor :deep(.text-column-cell span) {
  display: block;
  text-indent: 2em;
  text-align: justify;
}

/* 列宽拖动调整 */
.resizable-table :deep(.ant-table-thead > tr > th) {
  position: relative;
  user-select: none;
}

.resizable-table
  :deep(.ant-table-thead > tr > th:not(.fixed-left):not(.fixed-right))::after {
  position: absolute;
  top: 0;
  right: -2px;
  bottom: 0;
  z-index: 10;
  width: 4px;
  cursor: col-resize;
  content: '';
  background: transparent;
}

.resizable-table
  :deep(
    .ant-table-thead > tr > th:not(.fixed-left):not(.fixed-right)
  ):hover::after {
  background: #1890ff;
}

.resizable-table :deep(.ant-table-thead > tr > th.resizing)::after {
  width: 2px;
  background: #1890ff;
}

.resizable-table :deep(.ant-table-thead > tr > th.fixed-left)::after,
.resizable-table :deep(.ant-table-thead > tr > th.fixed-right)::after {
  display: none;
}
</style>
