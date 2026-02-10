<script setup lang="ts">
import type { EvidenceItem } from './types';

import type { MatterDossierItem } from '#/api/document/dossier';
import type {
  CreateEvidenceCommand,
  EvidenceExportItem,
  UpdateEvidenceCommand,
} from '#/api/evidence';

/**
 * 证据整理 - 清单式编辑器
 * 支持分组管理，每组可包含多个证据
 */
import { ref, watch } from 'vue';

import {
  ChevronDown,
  ChevronUp,
  Download,
  Plus,
  Printer,
  Save,
  Trash,
} from '@vben/icons';

import {
  Button,
  Divider,
  Empty,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { getMatterDossierItems } from '#/api/document/dossier';
import {
  createEvidence,
  deleteEvidence,
  downloadEvidenceListDirect,
  getAccessToken,
  saveEvidenceListToDossier,
  updateEvidence,
} from '#/api/evidence';
import { escapeHtml } from '#/utils/sanitize';

interface EvidenceRow {
  id?: number;
  key: string;
  name: string;
  source: string;
  isOriginal: boolean;
  pageStart?: number;
  pageEnd?: number;
  isNew?: boolean;
  isDirty?: boolean;
  isSaving?: boolean;
}

interface GroupData {
  key: string;
  groupName: string;
  order: number;
  proofContent: string; // 证明内容（组级别）
  proofPurpose: string; // 证明目的（组级别）
  evidences: EvidenceRow[];
  isNew?: boolean;
  isDirty?: boolean;
}

const props = defineProps<{
  evidences: EvidenceItem[];
  listId?: number; // 关联的证据清单ID
  listName?: string; // 清单名称，用于打印显示
  matterId: number;
  matterName?: string; // 案件名称，用于打印显示
  readonly?: boolean;
  submitDate?: string;
  submitter?: string;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'export', format: 'pdf' | 'word'): void;
}>();

// 分组数据
const groupList = ref<GroupData[]>([]);
const saving = ref(false);

// 提交人和日期
const submitterName = ref(props.submitter || '');
const submitDateStr = ref(props.submitDate || '');

// 初始化分组数据 - 按 groupName 或 sortOrder 分组
watch(
  () => props.evidences,
  (evidences) => {
    // 按 groupName 分组，没有 groupName 的按 sortOrder 单独成组
    const groupMap = new Map<string, EvidenceItem[]>();

    evidences.forEach((e) => {
      const groupKey = e.groupName || `group-${e.sortOrder || e.id}`;
      if (!groupMap.has(groupKey)) {
        groupMap.set(groupKey, []);
      }
      groupMap.get(groupKey)!.push(e);
    });

    // 转换为分组数据
    let order = 1;
    groupList.value = [...groupMap.entries()].map(([groupName, items]) => {
      const firstItem = items[0];
      return {
        key: `group-${order}`,
        groupName: groupName.startsWith('group-')
          ? `第 ${order} 组`
          : groupName,
        order: order++,
        proofContent: firstItem?.description || '',
        proofPurpose: firstItem?.provePurpose || '',
        evidences: items.map((e) => ({
          id: e.id,
          key: `evidence-${e.id}`,
          name: e.name || '',
          source: e.source || '',
          isOriginal: e.isOriginal ?? false,
          pageStart: e.pageStart,
          pageEnd: e.pageEnd,
          isNew: false,
          isDirty: false,
          isSaving: false,
        })),
        isNew: false,
        isDirty: false,
      };
    });
  },
  { immediate: true },
);

// 添加新组
function handleAddGroup() {
  const newOrder = groupList.value.length + 1;
  groupList.value.push({
    key: `new-group-${Date.now()}`,
    groupName: `第 ${newOrder} 组`,
    order: newOrder,
    proofContent: '',
    proofPurpose: '',
    evidences: [
      {
        key: `new-evidence-${Date.now()}`,
        name: '',
        source: '',
        isOriginal: false,
        isNew: true,
        isDirty: true,
      },
    ],
    isNew: true,
    isDirty: true,
  });
}

// 在组内添加证据
function handleAddEvidence(group: GroupData) {
  group.evidences.push({
    key: `new-evidence-${Date.now()}`,
    name: '',
    source: '',
    isOriginal: false,
    isNew: true,
    isDirty: true,
  });
  group.isDirty = true;
}

// 删除组内证据
async function handleDeleteEvidence(
  group: GroupData,
  evidence: EvidenceRow,
  evidenceIndex: number,
) {
  if (evidence.id) {
    try {
      await deleteEvidence(evidence.id);
      message.success('删除成功');
    } catch (error: any) {
      message.error(error.message || '删除失败');
      return;
    }
  }
  group.evidences.splice(evidenceIndex, 1);
  // 如果组内没有证据了，删除整个组
  if (group.evidences.length === 0) {
    const groupIndex = groupList.value.findIndex((g) => g.key === group.key);
    if (groupIndex !== -1) {
      groupList.value.splice(groupIndex, 1);
      updateGroupOrder();
    }
  }
}

// 删除整个组
async function handleDeleteGroup(group: GroupData, groupIndex: number) {
  // 删除组内所有证据
  for (const evidence of group.evidences) {
    if (evidence.id) {
      try {
        await deleteEvidence(evidence.id);
      } catch (error: any) {
        message.error(`删除证据失败: ${error.message}`);
      }
    }
  }
  groupList.value.splice(groupIndex, 1);
  updateGroupOrder();
  message.success('删除成功');
}

// 标记组为已修改
function markGroupDirty(group: GroupData) {
  group.isDirty = true;
}

// 标记证据为已修改
function markEvidenceDirty(group: GroupData, evidence: EvidenceRow) {
  evidence.isDirty = true;
  group.isDirty = true;
}

// 保存单个组
async function handleSaveGroup(group: GroupData) {
  // 验证：至少有一个证据有名称
  const validEvidences = group.evidences.filter((e) => e.name.trim());
  if (validEvidences.length === 0) {
    message.warning('请至少输入一个证据名称');
    return;
  }

  saving.value = true;
  let successCount = 0;
  let failCount = 0;

  for (const evidence of group.evidences) {
    if (!evidence.name.trim()) continue;

    evidence.isSaving = true;
    try {
      if (evidence.isNew) {
        const command: CreateEvidenceCommand = {
          matterId: props.matterId,
          name: evidence.name,
          evidenceType: 'DOCUMENTARY',
          source: evidence.source,
          groupName: group.groupName,
          provePurpose: group.proofPurpose,
          description: group.proofContent,
          isOriginal: evidence.isOriginal,
          pageStart: evidence.pageStart,
          pageEnd: evidence.pageEnd,
        };
        await createEvidence(command);
        evidence.isNew = false;
        successCount++;
      } else if (evidence.id && evidence.isDirty) {
        const command: UpdateEvidenceCommand = {
          name: evidence.name,
          source: evidence.source,
          groupName: group.groupName,
          provePurpose: group.proofPurpose,
          description: group.proofContent,
          isOriginal: evidence.isOriginal,
          pageStart: evidence.pageStart,
          pageEnd: evidence.pageEnd,
        };
        await updateEvidence(evidence.id, command);
        successCount++;
      }
      evidence.isDirty = false;
    } catch {
      failCount++;
    } finally {
      evidence.isSaving = false;
    }
  }

  saving.value = false;
  group.isDirty = false;
  group.isNew = false;

  if (successCount > 0) {
    message.success(`保存成功`);
    emit('refresh');
  }
  if (failCount > 0) {
    message.error(`${failCount} 条证据保存失败`);
  }
}

// 保存全部
async function handleSaveAll() {
  const dirtyGroups = groupList.value.filter((g) => g.isDirty);
  if (dirtyGroups.length === 0) {
    message.info('没有需要保存的修改');
    return;
  }

  for (const group of dirtyGroups) {
    await handleSaveGroup(group);
  }
}

// 组上移
function handleMoveGroupUp(index: number) {
  if (index === 0) return;
  const temp = groupList.value[index];
  groupList.value[index] = groupList.value[index - 1]!;
  groupList.value[index - 1] = temp!;
  updateGroupOrder();
  groupList.value[index]!.isDirty = true;
  groupList.value[index - 1]!.isDirty = true;
}

// 组下移
function handleMoveGroupDown(index: number) {
  if (index === groupList.value.length - 1) return;
  const temp = groupList.value[index];
  groupList.value[index] = groupList.value[index + 1]!;
  groupList.value[index + 1] = temp!;
  updateGroupOrder();
  groupList.value[index]!.isDirty = true;
  groupList.value[index + 1]!.isDirty = true;
}

// 更新组序号
function updateGroupOrder() {
  groupList.value.forEach((group, index) => {
    group.order = index + 1;
    group.groupName = `第 ${index + 1} 组`;
  });
}

// 检查是否有未保存的修改
function hasDirtyItems() {
  return groupList.value.some((g) => g.isDirty);
}

// 导出
async function handleExport(format: 'pdf' | 'word') {
  if (groupList.value.length === 0) {
    message.warning('暂无证据可导出');
    return;
  }

  const token = await getAccessToken();
  if (!token) {
    message.error('请先登录');
    return;
  }

  // 转换为导出格式
  const exportItems: EvidenceExportItem[] = [];
  groupList.value.forEach((group) => {
    group.evidences.forEach((evidence) => {
      if (evidence.name.trim()) {
        exportItems.push({
          id: evidence.id,
          name: evidence.name,
          source: evidence.source || '',
          provePurpose: group.proofPurpose || group.proofContent || '',
          isOriginal: evidence.isOriginal,
          pageStart: evidence.pageStart,
          pageEnd: evidence.pageEnd,
          listOrder: exportItems.length + 1,
        });
      }
    });
  });

  try {
    message.loading('正在下载...', 0);
    await downloadEvidenceListDirect(
      props.matterId,
      exportItems,
      format,
      token,
    );
    message.destroy();
    message.success('导出成功');
  } catch (error: any) {
    message.destroy();
    message.error(error.message || '导出失败');
  }
}

// === 保存到卷宗 ===
const showDossierModal = ref(false);
const dossierItems = ref<MatterDossierItem[]>([]);
const selectedDossierItemId = ref<number | undefined>(undefined);
const savingToDossier = ref(false);

// 加载卷宗目录
async function loadDossierItems() {
  try {
    const items = await getMatterDossierItems(props.matterId);
    // 只显示文件夹类型
    dossierItems.value = items.filter(
      (item: MatterDossierItem) => item.itemType === 'FOLDER',
    );
  } catch {
    message.error('加载卷宗目录失败');
  }
}

// 打开保存到卷宗弹窗
async function openSaveToDossierModal() {
  if (!props.listId) {
    message.warning('请先保存证据清单');
    return;
  }
  if (groupList.value.length === 0) {
    message.warning('暂无证据可保存');
    return;
  }
  await loadDossierItems();
  selectedDossierItemId.value = undefined;
  showDossierModal.value = true;
}

// 确认保存到卷宗
async function confirmSaveToDossier() {
  if (!selectedDossierItemId.value) {
    message.warning('请选择目标文件夹');
    return;
  }
  if (!props.listId) {
    message.warning('证据清单ID不存在');
    return;
  }

  savingToDossier.value = true;
  try {
    await saveEvidenceListToDossier(props.listId, selectedDossierItemId.value);
    message.success('证据清单PDF已保存到卷宗');
    showDossierModal.value = false;
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    savingToDossier.value = false;
  }
}

// 打印 - 文档式格式（清单式特有的打印样式）
function handlePrint() {
  if (groupList.value.length === 0) {
    message.warning('暂无证据可打印');
    return;
  }

  // 创建打印窗口
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器弹窗设置');
    return;
  }

  // 生成打印内容 - 文档式格式
  let printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>证据清单</title>
      <style>
        @media print {
          @page {
            size: A4;
            margin: 2cm;
          }
        }
        body {
          font-family: "Microsoft YaHei", "SimSun", serif;
          font-size: 14px;
          line-height: 1.8;
          color: #000;
          padding: 20px;
        }
        .print-header {
          text-align: center;
          margin-bottom: 40px;
        }
        .print-header h1 {
          font-size: 24px;
          font-weight: bold;
          letter-spacing: 8px;
          margin: 0 0 20px 0;
        }
        .group-section {
          margin-bottom: 35px;
          page-break-inside: avoid;
        }
        .group-title {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 15px;
          padding-bottom: 8px;
        }
        .evidence-list {
          padding-left: 0;
          margin: 0;
        }
        .evidence-item {
          margin-bottom: 18px;
          padding-left: 20px;
        }
        .evidence-name {
          font-size: 15px;
          font-weight: bold;
          margin-bottom: 6px;
          color: #000;
        }
        .evidence-details {
          display: flex;
          flex-wrap: wrap;
          gap: 20px;
          font-size: 13px;
          color: #333;
          padding-left: 10px;
        }
        .evidence-detail-item {
          display: inline-flex;
          align-items: center;
        }
        .detail-label {
          color: #666;
          margin-right: 5px;
        }
        .detail-value {
          color: #000;
        }
        .proof-section {
          margin-top: 15px;
          padding-left: 20px;
        }
        .proof-section .proof-item {
          margin-bottom: 8px;
        }
        .proof-section .proof-item:last-child {
          margin-bottom: 0;
        }
        .proof-section .label {
          font-weight: bold;
          color: #333;
          margin-right: 10px;
        }
        .proof-section .value {
          color: #000;
        }
        .footer-signature {
          margin-top: 50px;
          padding-top: 20px;
          display: flex;
          justify-content: space-between;
        }
        .signature-item {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        .print-case-info {
          font-size: 16px;
          color: #333;
          margin-bottom: 15px;
        }
      </style>
    </head>
    <body>
      <div class="print-header">
        <h1>证 据 清 单</h1>
        ${props.matterName ? `<div class="print-case-info">${escapeHtml(props.matterName)}</div>` : ''}
      </div>
  `;

  // 添加分组内容 - 文档式列表格式
  groupList.value.forEach((group) => {
    printContent += `
      <div class="group-section">
        <div class="group-title">${escapeHtml(group.groupName)}</div>
        <div class="evidence-list">
    `;

    let evidenceIndex = 0;
    group.evidences.forEach((evidence) => {
      if (evidence.name.trim()) {
        evidenceIndex++;
        const pageRange =
          evidence.pageStart && evidence.pageEnd
            ? `第${evidence.pageStart}-${evidence.pageEnd}页`
            : '—';
        printContent += `
          <div class="evidence-item">
            <div class="evidence-name">证据${evidenceIndex}：${escapeHtml(evidence.name)}</div>
            <div class="evidence-details">
              <span class="evidence-detail-item">
                <span class="detail-label">来源：</span>
                <span class="detail-value">${escapeHtml(evidence.source || '—')}</span>
              </span>
              <span class="evidence-detail-item">
                <span class="detail-label">形式：</span>
                <span class="detail-value">${evidence.isOriginal ? '原件' : '复印件'}</span>
              </span>
              <span class="evidence-detail-item">
                <span class="detail-label">页码：</span>
                <span class="detail-value">${escapeHtml(pageRange)}</span>
              </span>
            </div>
          </div>
        `;
      }
    });

    printContent += `
        </div>
    `;

    // 添加证明内容和证明目的
    if (group.proofContent || group.proofPurpose) {
      printContent += `
        <div class="proof-section">
      `;
      if (group.proofContent) {
        printContent += `
          <div class="proof-item">
            <span class="label">证明内容：</span>
            <span class="value">${escapeHtml(group.proofContent)}</span>
          </div>
        `;
      }
      if (group.proofPurpose) {
        printContent += `
          <div class="proof-item">
            <span class="label">证明目的：</span>
            <span class="value">${escapeHtml(group.proofPurpose)}</span>
          </div>
        `;
      }
      printContent += `
        </div>
      `;
    }

    printContent += `
      </div>
    `;
  });

  // 添加签名
  printContent += `
      <div class="footer-signature">
        <div class="signature-item">
          <span>证据提交人：</span>
          <span>${escapeHtml(submitterName.value) || '_______________'}</span>
        </div>
        <div class="signature-item">
          <span>日期：</span>
          <span>${escapeHtml(submitDateStr.value) || '______年____月____日'}</span>
        </div>
      </div>
    </body>
    </html>
  `;

  printWindow.document.write(printContent);
  printWindow.document.close();

  // 等待内容加载后打印
  printWindow.addEventListener(
    'load',
    () => {
      setTimeout(() => {
        printWindow.print();
        // 打印后关闭窗口（可选）
        // printWindow.close();
      }, 250);
    },
    { once: true },
  );
}
</script>

<template>
  <div class="evidence-list-display">
    <!-- 工具栏 -->
    <div class="toolbar">
      <Space>
        <Button v-if="!readonly" type="primary" @click="handleAddGroup">
          <Plus class="h-4 w-4" />
          添加证据组
        </Button>
        <Button
          v-if="!readonly && hasDirtyItems()"
          type="primary"
          ghost
          :loading="saving"
          @click="handleSaveAll"
        >
          <Save class="h-4 w-4" />
          保存全部
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
        <Button type="primary" @click="openSaveToDossierModal">
          📁 保存到卷宗
        </Button>
      </Space>
    </div>

    <!-- 保存到卷宗弹窗 -->
    <Modal
      v-model:open="showDossierModal"
      title="保存证据清单到卷宗"
      :confirm-loading="savingToDossier"
      @ok="confirmSaveToDossier"
    >
      <div style="margin-bottom: 16px">选择要保存到的卷宗文件夹：</div>
      <Select
        v-model:value="selectedDossierItemId"
        placeholder="请选择目标文件夹"
        style="width: 100%"
        :options="
          dossierItems.map((item) => ({
            value: item.id,
            label: item.name,
          }))
        "
      />
      <div style="margin-top: 12px; font-size: 12px; color: #888">
        将生成证据清单PDF文件并保存到选中的卷宗文件夹中
      </div>
    </Modal>

    <!-- 清单标题 -->
    <div class="list-header">
      <h2>证 据 清 单</h2>
    </div>

    <!-- 分组列表 -->
    <div v-if="groupList.length > 0" class="group-list">
      <div
        v-for="(group, groupIndex) in groupList"
        :key="group.key"
        class="group-item"
        :class="{ 'is-dirty': group.isDirty, 'is-new': group.isNew }"
      >
        <!-- 组标题栏 -->
        <div class="group-header">
          <div class="group-title">
            <Tag :color="group.isNew ? 'green' : 'blue'" class="group-tag">
              {{ group.groupName }}
            </Tag>
            <Tag v-if="group.isDirty && !group.isNew" color="orange">
              未保存
            </Tag>
            <!-- eslint-disable-next-line prettier/prettier -->
            <span class="evidence-count"
              >（{{ group.evidences.length }} 项证据）</span
            >
          </div>
          <Space v-if="!readonly" class="group-actions">
            <Tooltip title="上移">
              <Button
                type="text"
                size="small"
                :disabled="groupIndex === 0"
                @click="handleMoveGroupUp(groupIndex)"
              >
                <ChevronUp class="h-4 w-4" />
              </Button>
            </Tooltip>
            <Tooltip title="下移">
              <Button
                type="text"
                size="small"
                :disabled="groupIndex === groupList.length - 1"
                @click="handleMoveGroupDown(groupIndex)"
              >
                <ChevronDown class="h-4 w-4" />
              </Button>
            </Tooltip>
            <Button
              type="primary"
              size="small"
              :disabled="!group.isDirty"
              @click="handleSaveGroup(group)"
            >
              保存本组
            </Button>
            <Popconfirm
              title="确定删除整个证据组吗？组内所有证据都会被删除！"
              @confirm="handleDeleteGroup(group, groupIndex)"
            >
              <Button type="text" size="small" danger>
                <Trash class="h-4 w-4" />
              </Button>
            </Popconfirm>
          </Space>
        </div>

        <!-- 组内证据列表 -->
        <div class="evidence-list">
          <div class="evidence-list-header">
            <span class="col-index">#</span>
            <span class="col-name">证据名称 *</span>
            <span class="col-source">来源</span>
            <span class="col-original">原件</span>
            <span class="col-pages">页码</span>
            <span class="col-action">操作</span>
          </div>
          <div
            v-for="(evidence, evidenceIndex) in group.evidences"
            :key="evidence.key"
            class="evidence-row"
            :class="{ 'is-new': evidence.isNew }"
          >
            <span class="col-index">{{ evidenceIndex + 1 }}</span>
            <div class="col-name">
              <Input
                v-model:value="evidence.name"
                :disabled="readonly"
                placeholder="证据名称"
                size="small"
                @change="markEvidenceDirty(group, evidence)"
              />
            </div>
            <div class="col-source">
              <Input
                v-model:value="evidence.source"
                :disabled="readonly"
                placeholder="来源"
                size="small"
                @change="markEvidenceDirty(group, evidence)"
              />
            </div>
            <div class="col-original">
              <Switch
                v-model:checked="evidence.isOriginal"
                :disabled="readonly"
                size="small"
                @change="markEvidenceDirty(group, evidence)"
              />
            </div>
            <div class="col-pages">
              <Space size="small">
                <InputNumber
                  v-model:value="evidence.pageStart"
                  :disabled="readonly"
                  :min="1"
                  placeholder="起"
                  size="small"
                  style="width: 60px"
                  @change="markEvidenceDirty(group, evidence)"
                />
                <span>-</span>
                <InputNumber
                  v-model:value="evidence.pageEnd"
                  :disabled="readonly"
                  :min="evidence.pageStart || 1"
                  placeholder="止"
                  size="small"
                  style="width: 60px"
                  @change="markEvidenceDirty(group, evidence)"
                />
              </Space>
            </div>
            <div class="col-action">
              <Popconfirm
                v-if="!readonly"
                title="确定删除此证据吗？"
                @confirm="handleDeleteEvidence(group, evidence, evidenceIndex)"
              >
                <Button type="text" size="small" danger>
                  <Trash class="h-3 w-3" />
                </Button>
              </Popconfirm>
            </div>
          </div>

          <!-- 添加证据按钮 -->
          <div v-if="!readonly" class="add-evidence-row">
            <Button
              type="dashed"
              size="small"
              block
              @click="handleAddEvidence(group)"
            >
              <Plus class="h-3 w-3" />
              添加证据到本组
            </Button>
          </div>
        </div>

        <Divider style="margin: 12px 0" />

        <!-- 组级别的证明内容和证明目的 -->
        <div class="group-fields">
          <div class="field-row">
            <div class="field-item proof-content">
              <label class="field-label">证明内容</label>
              <Input.TextArea
                v-model:value="group.proofContent"
                :disabled="readonly"
                placeholder="该组证据说明了什么事实"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                @change="markGroupDirty(group)"
              />
            </div>
          </div>
          <div class="field-row">
            <div class="field-item proof-purpose">
              <label class="field-label">证明目的</label>
              <Input.TextArea
                v-model:value="group.proofPurpose"
                :disabled="readonly"
                placeholder="该组证据用于证明什么"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                @change="markGroupDirty(group)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <Empty v-else description="暂无证据，点击上方「添加证据组」开始">
      <Button v-if="!readonly" type="primary" @click="handleAddGroup">
        <Plus class="h-4 w-4" />
        添加第一组证据
      </Button>
    </Empty>

    <!-- 底部签名 -->
    <div v-if="groupList.length > 0" class="footer-signature">
      <div class="signature-row">
        <div class="signature-item">
          <span class="signature-label">证据提交人：</span>
          <Input
            v-if="!readonly"
            v-model:value="submitterName"
            placeholder="输入提交人姓名"
            style="width: 150px"
          />
          <span v-else class="signature-value">{{
            submitterName || '_______________'
          }}</span>
        </div>
        <div class="signature-item">
          <span class="signature-label">日期：</span>
          <Input
            v-if="!readonly"
            v-model:value="submitDateStr"
            placeholder="YYYY年MM月DD日"
            style="width: 150px"
          />
          <span v-else class="signature-value">{{
            submitDateStr || '______年____月____日'
          }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.evidence-list-display {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.list-header {
  margin-bottom: 24px;
  text-align: center;
}

.list-header h2 {
  margin-bottom: 8px;
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 8px;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.group-item {
  padding: 16px;
  background: #fff;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.group-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 12px rgb(24 144 255 / 15%);
}

.group-item.is-dirty {
  background: #fffbe6;
  border-color: #faad14;
}

.group-item.is-new {
  background: #f6ffed;
  border-color: #52c41a;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.group-title {
  display: flex;
  gap: 8px;
  align-items: center;
}

.group-tag {
  padding: 4px 12px;
  font-size: 14px;
}

.evidence-count {
  font-size: 13px;
  color: #666;
}

.group-actions {
  opacity: 0.6;
  transition: opacity 0.2s;
}

.group-item:hover .group-actions {
  opacity: 1;
}

.group-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-row {
  display: flex;
  gap: 16px;
}

.field-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: #666;
}

/* 证据列表表格样式 */
.evidence-list {
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
}

.evidence-list-header {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 4px;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}

.evidence-row {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.evidence-row:hover {
  background: #fff;
}

.evidence-row.is-new {
  background: #f6ffed;
}

.col-index {
  width: 30px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.col-name {
  flex: 2;
  min-width: 150px;
}

.col-source {
  flex: 1;
  min-width: 100px;
}

.col-original {
  width: 50px;
  text-align: center;
}

.col-pages {
  width: 160px;
}

.col-action {
  width: 40px;
  text-align: center;
}

.add-evidence-row {
  padding: 4px;
  margin-top: 8px;
}

.footer-signature {
  padding-top: 16px;
  margin-top: 32px;
  border-top: 1px solid #e8e8e8;
}

.signature-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.signature-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.signature-label {
  font-weight: 500;
}

.signature-value {
  color: #333;
}
</style>
