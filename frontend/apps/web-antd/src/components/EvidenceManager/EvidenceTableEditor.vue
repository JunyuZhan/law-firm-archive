<script setup lang="ts">
/**
 * 证据登记表 - 表格式编辑器
 * 支持动态增减行和列
 */
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { Table, Input, InputNumber, Button, Space, Popconfirm, message, Tooltip, Modal, Checkbox, Tag, Select, Switch } from 'ant-design-vue';
import { Printer, Download, ChevronUp, ChevronDown } from '@vben/icons';
import type { EvidenceItem } from './types';
import { createEvidence, updateEvidence, deleteEvidence, exportEvidenceList, getEvidenceGroups, type CreateEvidenceCommand, type UpdateEvidenceCommand, type EvidenceExportItem } from '#/api/evidence';

// 列定义
interface ColumnDef {
  key: string;
  title: string;
  width: number;
  required?: boolean;
  editable?: boolean;
  type?: 'text' | 'number' | 'textarea' | 'select' | 'switch';
  options?: { label: string; value: string }[]; // 下拉选项（用于 select 类型）
  isSystem?: boolean;  // 系统列不可删除
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
  matterId: number;
  evidences: EvidenceItem[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'export', format: 'word' | 'pdf'): void;
}>();

// 默认列配置（预设列）
const defaultColumns: ColumnDef[] = [
  { key: 'groupName', title: '组别', width: 120, editable: true, type: 'text' }, // 组别列，分组依据，使用文本输入
  { key: 'order', title: '序号', width: 60, editable: false }, // 序号列，按组别分组，每组从1开始
  { key: 'name', title: '证据名称', width: 180, required: true, editable: true, type: 'text' },
  { key: 'quantity', title: '数量', width: 80, editable: true, type: 'number' },
  { key: 'isOriginal', title: '是否原件', width: 100, editable: true, type: 'switch' },
  { key: 'proofContent', title: '证明内容', width: 200, editable: true, type: 'textarea' },
  { key: 'provePurpose', title: '证明目的', width: 200, editable: true, type: 'textarea' },
  { key: 'pageStart', title: '页码起始', width: 100, editable: true, type: 'number' },
];

// 可选的额外列
const optionalColumns: ColumnDef[] = [
  { key: 'source', title: '来源', width: 120, editable: true, type: 'text' },
  { key: 'pageEnd', title: '页码结束', width: 100, editable: true, type: 'number' },
  { key: 'pageCount', title: '页数', width: 80, editable: true, type: 'number' },
  { key: 'pageRange', title: '页码范围', width: 100, editable: true, type: 'text' },
  { key: 'evidenceType', title: '证据类型', width: 100, editable: true, type: 'text' },
  { key: 'copies', title: '份数', width: 80, editable: true, type: 'number' },
  { key: 'submitTime', title: '提交时间', width: 120, editable: true, type: 'text' },
  { key: 'remark', title: '备注', width: 150, editable: true, type: 'text' },
];

// 当前列配置
const columns = ref<ColumnDef[]>([...defaultColumns]);
const tableData = ref<TableRow[]>([]);
const saving = ref(false);
const groups = ref<string[]>([]); // 组别列表

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
  const groupOptions = groups.value.map(g => ({ label: g, value: g }));
  // 更新默认列中的组别列选项
  const defaultGroupNameCol = defaultColumns.find(c => c.key === 'groupName');
  if (defaultGroupNameCol) {
    defaultGroupNameCol.options = groupOptions;
  }
  // 更新当前已添加的组别列选项
  columns.value.forEach(col => {
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
  const savedColumns = localStorage.getItem(`evidence-columns-${props.matterId}`);
  if (savedColumns) {
    try {
      const parsedColumns = JSON.parse(savedColumns);
      // 验证是否包含预设列，如果没有则使用预设列
      const hasGroupName = parsedColumns.some((c: ColumnDef) => c.key === 'groupName');
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
    } catch (e) {
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
  localStorage.setItem(`evidence-columns-${props.matterId}`, JSON.stringify(columns.value));
}

// 初始化表格数据（按组别分组，每组内序号从1开始）
watch(() => props.evidences, (evidences) => {
  // 按组别分组
  const groupedEvidences = new Map<string, typeof evidences>();
  evidences.forEach(e => {
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
        pageCount: e.pageStart && e.pageEnd ? e.pageEnd - e.pageStart + 1 : undefined,
        remark: e.description || '',
        evidenceType: e.evidenceTypeName || '',
        copies: (e.originalCount || 0) + (e.copyCount || 0),
        submitTime: e.createdAt ? e.createdAt.split('T')[0] : '',
        pageRange: e.pageStart && e.pageEnd ? `${e.pageStart}-${e.pageEnd}` : '',
        isNew: false,
        isDirty: false,
      });
    });
  });
}, { immediate: true });

// 计算表格列
const tableColumns = computed(() => {
  const cols = columns.value.map(col => ({
    title: col.title,
    dataIndex: col.key,
    key: col.key,
    width: col.width,
    align: col.key === 'order' || col.type === 'number' ? 'center' as const : 'left' as const,
    onHeaderCell: () => ({
      'data-column-key': col.key,
    }),
  }));

  // 不再添加操作列，删除功能通过行内按钮实现

  return cols;
});

// === 行操作 ===

// 添加新行
function handleAddRow() {
  const newRow: TableRow = {
    key: `new-${Date.now()}`,
    order: 1, // 默认序号为1，会根据组别调整
    isNew: true,
    isDirty: true,
  };
  
  // 初始化所有列的值
  columns.value.forEach(col => {
    if (col.key !== 'order') {
      if (col.key === 'quantity' || col.key === 'pageStart' || col.key === 'pageEnd') {
        newRow[col.key] = undefined;
      } else {
        newRow[col.key] = '';
      }
    }
  });

  tableData.value.push(newRow);
  // 更新行序号（按组别分组）
  updateRowOrder();
}

// 删除行
async function handleDeleteRow(index: number) {
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
  tableData.value.forEach(row => {
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

// 标记为已修改
function markDirty(index: number) {
  const row = tableData.value[index];
  if (row) {
    row.isDirty = true;
  }
}

// === 列操作 ===

// 显示列管理弹窗
function showColumnModal() {
  // 检查哪些可选列已添加
  selectedOptionalColumns.value = optionalColumns
    .filter(opt => columns.value.some(col => col.key === opt.key))
    .map(opt => opt.key);
  newColumnTitle.value = '';
  columnModalVisible.value = true;
}

// 添加可选列（添加到末尾）
function handleToggleOptionalColumn(key: string, checked: boolean) {
  nextTick(() => {
    if (checked) {
      const optCol = optionalColumns.find(c => c.key === key);
      if (optCol && !columns.value.some(c => c.key === key)) {
        // 添加到末尾
        columns.value.push({ ...optCol });
        saveColumnConfig();
        message.success(`已添加列"${optCol.title}"，可在下方调整位置`);
      }
    } else {
      const index = columns.value.findIndex(c => c.key === key);
      if (index > -1) {
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
    });

    // 为现有行初始化该列的值
    tableData.value.forEach(row => {
      row[key] = '';
    });

    newColumnTitle.value = '';
    saveColumnConfig();
    message.success(`已添加列"${title}"，可在下方调整位置`);
  });
}

// 删除列
function handleDeleteColumn(key: string) {
  nextTick(() => {
    const index = columns.value.findIndex(c => c.key === key);
    if (index > -1) {
      columns.value.splice(index, 1);
      saveColumnConfig();
      message.success('列已删除');
    }
  });
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
    [columns.value[index], columns.value[index - 1]] = [columns.value[index - 1]!, columns.value[index]!];
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
    [columns.value[index], columns.value[index + 1]] = [columns.value[index + 1]!, columns.value[index]!];
    saveColumnConfig();
    message.success(`已将"${currentCol.title}"下移`);
  });
}

// 更新列宽
function handleUpdateColumnWidth(key: string, width: number | null) {
  const col = columns.value.find(c => c.key === key);
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
  if (!th || th.classList.contains('fixed-left') || th.classList.contains('fixed-right')) return;
  
  const rect = th.getBoundingClientRect();
  const rightEdge = rect.right;
  
  // 检查是否点击在列的右边缘（4px范围内）
  if (mouseEvent.clientX >= rightEdge - 4 && mouseEvent.clientX <= rightEdge + 4) {
    isResizing = true;
    currentCol = th;
    startX = mouseEvent.clientX;
    startWidth = th.offsetWidth;
    // 从 data-column-key 属性获取 key
    currentColKey = th.getAttribute('data-column-key') || '';
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
    const col = columns.value.find(c => c.key === currentColKey);
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

// 处理输入变化
function handleInputChange(index: number) {
  markDirty(index);
  const row = tableData.value[index];
  if (row) {
    // 延迟更新，确保组别值已经更新
    nextTick(() => {
      // 如果组别改变，需要重新排序数据并更新序号
      // 按组别排序，同一组内的数据保持原有顺序
      const rowIndices = new Map<string, number>();
      tableData.value.forEach((r, idx) => {
        rowIndices.set(r.key, idx);
      });
      
      tableData.value.sort((a, b) => {
        const groupA = a.groupName || '';
        const groupB = b.groupName || '';
        if (groupA !== groupB) {
          return groupA.localeCompare(groupB);
        }
        // 同一组内保持原有顺序
        const indexA = rowIndices.get(a.key) || 0;
        const indexB = rowIndices.get(b.key) || 0;
        return indexA - indexB;
      });
      
      // 更新序号
      updateRowOrder();
    });
  }
}

// 保存所有修改
async function handleSaveAll() {
  const dirtyRows = tableData.value.filter(row => row.isDirty);
  if (dirtyRows.length === 0) {
    message.info('没有需要保存的修改');
    return;
  }

  saving.value = true;
  try {
    for (const row of dirtyRows) {
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
          pageStart: typeof row.pageStart === 'number' ? row.pageStart : undefined,
          pageEnd: typeof row.pageEnd === 'number' ? row.pageEnd : undefined,
          originalCount: typeof row.quantity === 'number' ? row.quantity : undefined,
        };
        await createEvidence(command);
      } else if (row.id) {
        const command: UpdateEvidenceCommand = {
          name: row.name,
          source: row.source,
          groupName: row.groupName || undefined, // 保存组别
          provePurpose: row.provePurpose || row.proofContent, // 证明目的
          description: row.proofContent || row.remark, // 证明内容/描述
          isOriginal: row.isOriginal === '是' || row.isOriginal === true,
          pageStart: typeof row.pageStart === 'number' ? row.pageStart : undefined,
          pageEnd: typeof row.pageEnd === 'number' ? row.pageEnd : undefined,
          originalCount: typeof row.quantity === 'number' ? row.quantity : undefined,
        };
        await updateEvidence(row.id, command);
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
async function handleExport(format: 'word' | 'pdf') {
  if (tableData.value.length === 0) {
    message.warning('暂无证据可导出');
    return;
  }

  try {
    message.loading('正在导出...', 0);
    
    // 转换为导出格式
    const exportItems: EvidenceExportItem[] = tableData.value
      .filter(row => row.name && row.name.trim())
      .map((row, index) => ({
        id: row.id,
        name: row.name || '',
        source: row.source || '',
        provePurpose: row.proofContent || '',
        isOriginal: row.isOriginal === true || row.isOriginal === '是',
        pageStart: typeof row.pageStart === 'number' ? row.pageStart : undefined,
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
          line-height: 1.5;
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
        }
        .print-table th,
        .print-table td {
          border: 1px solid #000;
          padding: 6px;
          text-align: left;
          font-size: 11px;
        }
        .print-table th {
          background-color: #f0f0f0;
          font-weight: bold;
          text-align: center;
        }
        .print-table .col-order {
          width: 40px;
          text-align: center;
        }
        .print-table .col-name {
          width: 150px;
        }
        .print-table .col-source {
          width: 100px;
        }
        .print-table .col-page {
          width: 60px;
          text-align: center;
        }
        .print-table .col-proof {
          width: 200px;
        }
        .print-table .col-remark {
          width: 120px;
        }
      </style>
    </head>
    <body>
      <div class="print-header">
        <h1>证据登记表</h1>
      </div>
      <table class="print-table">
        <thead>
          <tr>
  `;

  // 添加表头
  columns.value.forEach(col => {
    if (col.key !== 'key') {
      printContent += `<th class="col-${col.key}">${col.title}</th>`;
    }
  });

  printContent += `
          </tr>
        </thead>
        <tbody>
  `;

  // 添加数据行
  let rowIndex = 0;
  tableData.value.forEach((row) => {
    if (row.name && row.name.trim()) {
      rowIndex++;
      printContent += '<tr>';
      columns.value.forEach(col => {
        if (col.key !== 'key') {
          // 序号列显示动态生成的序号
          if (col.key === 'order') {
            printContent += `<td class="col-${col.key}">${rowIndex}</td>`;
          } else {
            const value = row[col.key] || '';
            const displayValue = value === true ? '是' : value === false ? '否' : String(value);
            printContent += `<td class="col-${col.key}">${displayValue}</td>`;
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
  printWindow.onload = () => {
    setTimeout(() => {
      printWindow.print();
    }, 250);
  };
}

// 计算是否有未保存的修改
const hasDirtyRows = computed(() => tableData.value.some(row => row.isDirty));

// 计算最后一列的 key（用于显示删除按钮）
const lastColumnKey = computed(() => {
  return columns.value.length > 0 ? columns.value[columns.value.length - 1]?.key : null;
});
</script>

<template>
  <div class="evidence-table-editor">
    <!-- 工具栏 -->
    <div class="toolbar" style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
      <Space>
        <Button v-if="!readonly" type="primary" @click="handleAddRow">
          ➕ 添加行
        </Button>
        <Button v-if="!readonly" @click="showColumnModal">
          📊 管理列
        </Button>
        <Button v-if="!readonly && hasDirtyRows" type="primary" :loading="saving" @click="handleSaveAll">
          💾 保存修改
        </Button>
      </Space>
      <Space>
        <Button @click="handlePrint">
          <Printer class="w-4 h-4" />
          打印
        </Button>
        <Button @click="handleExport('word')">
          <Download class="w-4 h-4" />
          导出 Word
        </Button>
        <Button @click="handleExport('pdf')">
          <Download class="w-4 h-4" />
          导出 PDF
        </Button>
      </Space>
    </div>

    <!-- 表格标题 -->
    <div class="table-title" style="text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 16px;">
      证据登记表
    </div>

    <!-- 表格 -->
    <Table
      :columns="tableColumns"
      :data-source="tableData"
      :pagination="false"
      :scroll="{ x: 1200 }"
      bordered
      size="middle"
      row-key="key"
    >
      <template #bodyCell="{ column, record, index }">
        <!-- 序号列 - 动态生成，不依赖 order 字段 -->
        <template v-if="column.key === 'order'">
          <span>{{ record.order || 1 }}</span>
        </template>

        <!-- 可编辑列 -->
        <template v-else>
          <div style="display: flex; align-items: center; gap: 4px;">
            <div style="flex: 1;">
              <template v-if="!readonly">
                <!-- 下拉选择（组别列） -->
                <Select
                  v-if="column.key && columns.find(c => c.key === column.key)?.type === 'select'"
                  v-model:value="record[column.key!]"
                  :options="columns.find(c => c.key === column.key)?.options || []"
                  placeholder="选择或输入组别"
                  allow-clear
                  show-search
                  :filterOption="(input: string, option: any) => (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
                  style="width: 100%;"
                  @change="handleInputChange(index)"
                  @search="handleGroupSearch"
                />
                <!-- Switch开关（是否原件） -->
                <Switch
                  v-else-if="column.key && columns.find(c => c.key === column.key)?.type === 'switch'"
                  :checked="record[column.key!] === true || record[column.key!] === '是'"
                  @change="(checked: any) => { record[column.key!] = checked ? '是' : '否'; handleInputChange(index); }"
                />
                <!-- 数字输入 -->
                <InputNumber
                  v-else-if="column.key && columns.find(c => c.key === column.key)?.type === 'number'"
                  v-model:value="record[column.key!]"
                  :controls="false"
                  style="width: 100%;"
                  @change="handleInputChange(index)"
                />
                <!-- 多行文本 -->
                <Input.TextArea
                  v-else-if="column.key && columns.find(c => c.key === column.key)?.type === 'textarea'"
                  v-model:value="record[column.key!]"
                  :auto-size="{ minRows: 1, maxRows: 3 }"
                  @change="handleInputChange(index)"
                />
                <!-- 单行文本 -->
                <Input
                  v-else-if="column.key"
                  v-model:value="record[column.key]"
                  @change="handleInputChange(index)"
                />
              </template>
              <span v-else>{{ column.key ? record[column.key] : '' }}</span>
            </div>
            <!-- 最后一列显示删除按钮（类似清单式） -->
            <Popconfirm 
              v-if="!readonly && column.key === lastColumnKey" 
              title="确定删除此行吗？" 
              @confirm="handleDeleteRow(index)"
            >
              <Tooltip title="删除">
                <Button type="text" size="small" danger style="padding: 0 4px; flex-shrink: 0;">
                  🗑️
                </Button>
              </Tooltip>
            </Popconfirm>
          </div>
        </template>
      </template>

      <!-- 表头自定义（显示删除按钮） -->
      <template #headerCell="{ column }">
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span>{{ column.title }}</span>
          <Tooltip v-if="!readonly && column.key" title="删除此列">
            <Button type="text" size="small" style="padding: 0 4px; opacity: 0.5;" @click.stop="handleDeleteColumn(String(column.key))">✕</Button>
          </Tooltip>
        </div>
      </template>
    </Table>

    <!-- 添加行按钮（表格底部，类似清单式） -->
    <div v-if="!readonly" style="margin-top: 8px;">
      <Button type="dashed" block @click="handleAddRow">
        ➕ 添加行
      </Button>
    </div>

    <!-- 底部信息 -->
    <div class="footer-info" style="margin-top: 16px; display: flex; justify-content: space-between; color: #666;">
      <span>提交人：_______________</span>
      <span>第 ___ 页 共 ___ 页</span>
    </div>

    <!-- 列管理弹窗 -->
    <Modal
      v-model:open="columnModalVisible"
      title="管理表格列"
      @ok="handleColumnModalOk"
    >
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <!-- 可选列 -->
        <div>
          <div style="font-weight: 500; margin-bottom: 8px;">可选列：</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px;">
            <Checkbox
              v-for="opt in optionalColumns"
              :key="opt.key"
              :checked="columns.some(c => c.key === opt.key)"
              @change="(e: any) => handleToggleOptionalColumn(opt.key, e.target.checked)"
            >
              {{ opt.title }}
            </Checkbox>
          </div>
        </div>

        <!-- 添加自定义列 -->
        <div>
          <div style="font-weight: 500; margin-bottom: 8px;">添加自定义列：</div>
          <div style="display: flex; gap: 8px;">
            <Input v-model:value="newColumnTitle" placeholder="输入列标题" style="flex: 1;" />
            <Button type="primary" @click="handleAddCustomColumn">添加</Button>
          </div>
        </div>

        <!-- 当前列列表（可调整顺序） -->
        <div>
          <div style="font-weight: 500; margin-bottom: 8px;">当前列顺序：</div>
          <div style="display: flex; flex-direction: column; gap: 8px;">
            <div 
              v-for="(col, index) in columns" 
              :key="col.key"
              style="padding: 8px; background: #fafafa; border: 1px solid #e8e8e8; border-radius: 4px; display: flex; align-items: center; justify-content: space-between;"
            >
              <div style="display: flex; align-items: center; gap: 8px; flex: 1;">
                <span style="color: #999; font-size: 12px; min-width: 30px;">{{ index + 1 }}</span>
                <span style="font-weight: 500; min-width: 100px;">{{ col.title }}</span>
                <Tag v-if="col.isSystem" color="blue" size="small">系统</Tag>
                <Tag v-if="col.required" color="red" size="small">必填</Tag>
                <span style="color: #999; font-size: 12px; margin-left: 8px;">列宽：</span>
                <InputNumber 
                  :value="col.width" 
                  :min="50" 
                  :max="1000" 
                  :step="10"
                  size="small"
                  style="width: 80px;"
                  @change="(val) => handleUpdateColumnWidth(col.key, typeof val === 'number' ? val : 150)"
                />
                <span style="color: #999; font-size: 12px;">px</span>
              </div>
              <Space>
                <Tooltip :title="index === 0 ? '已是第一列' : '上移'">
                  <Button 
                    type="text" 
                    size="small" 
                    :disabled="index === 0"
                    @click="handleMoveColumnUp(index)"
                  >
                    <ChevronUp class="w-4 h-4" />
                  </Button>
                </Tooltip>
                <Tooltip :title="index === columns.length - 1 ? '已是最后一列' : '下移'">
                  <Button 
                    type="text" 
                    size="small" 
                    :disabled="index === columns.length - 1"
                    @click="handleMoveColumnDown(index)"
                  >
                    <ChevronDown class="w-4 h-4" />
                  </Button>
                </Tooltip>
                <Button 
                  v-if="col.key !== 'action'" 
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
  </div>
</template>

<style scoped>
.evidence-table-editor :deep(.ant-table-cell) {
  padding: 8px !important;
}

.evidence-table-editor :deep(.ant-input),
.evidence-table-editor :deep(.ant-input-textarea),
.evidence-table-editor :deep(.ant-input-number) {
  border: none;
  background: transparent;
}

.evidence-table-editor :deep(.ant-input:focus),
.evidence-table-editor :deep(.ant-input-textarea:focus),
.evidence-table-editor :deep(.ant-input-number:focus) {
  border: 1px solid #1890ff;
  background: #fff;
}

.evidence-table-editor :deep(.ant-input:hover),
.evidence-table-editor :deep(.ant-input-textarea:hover),
.evidence-table-editor :deep(.ant-input-number:hover) {
  background: #f5f5f5;
}

.evidence-table-editor :deep(.ant-table-thead th) {
  background: #fafafa;
}

.evidence-table-editor :deep(.ant-table-thead th:hover .ant-btn) {
  opacity: 1 !important;
}

/* 列宽拖动调整 */
.resizable-table :deep(.ant-table-thead > tr > th) {
  position: relative;
  user-select: none;
}

.resizable-table :deep(.ant-table-thead > tr > th:not(.fixed-left):not(.fixed-right))::after {
  content: '';
  position: absolute;
  right: -2px;
  top: 0;
  bottom: 0;
  width: 4px;
  cursor: col-resize;
  background: transparent;
  z-index: 10;
}

.resizable-table :deep(.ant-table-thead > tr > th:not(.fixed-left):not(.fixed-right)):hover::after {
  background: #1890ff;
}

.resizable-table :deep(.ant-table-thead > tr > th.resizing)::after {
  background: #1890ff;
  width: 2px;
}

.resizable-table :deep(.ant-table-thead > tr > th.fixed-left)::after,
.resizable-table :deep(.ant-table-thead > tr > th.fixed-right)::after {
  display: none;
}
</style>
