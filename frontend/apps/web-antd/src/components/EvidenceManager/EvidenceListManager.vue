<script setup lang="ts">
import type { EvidenceItem } from './types';

import type {
  EvidenceDTO,
  EvidenceExportItem,
  EvidenceListDTO,
} from '#/api/evidence';

/**
 * 证据整理管理组件
 * 每个证据清单对应一个独立的表格，多个清单垂直排列
 */
import { ref, watch } from 'vue';

import { Plus, RotateCw, Trash } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Segmented,
  Select,
  SelectOption,
  Spin,
  Tag,
} from 'ant-design-vue';

import {
  createEvidenceList,
  deleteEvidenceList,
  EVIDENCE_LIST_TYPE_OPTIONS,
  exportEvidenceList,
  getEvidenceByMatter,
  getEvidenceListDetail,
  getEvidenceListsByMatter,
} from '#/api/evidence';

import EvidenceListDisplay from './EvidenceListDisplay.vue';
import EvidenceTableEditor from './EvidenceTableEditor.vue';

type EditMode = 'list' | 'table';

const props = defineProps<{
  matterId: number;
  matterName?: string; // 案件名称，用于打印显示
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'change'): void;
}>();

// 状态
const loading = ref(false);
const allEvidences = ref<EvidenceItem[]>([]); // 案件的所有证据
const evidenceLists = ref<EvidenceListDTO[]>([]); // 案件的所有证据清单
const editMode = ref<EditMode>('table');

// 新建清单弹窗
const showCreateModal = ref(false);
const createForm = ref({
  name: '',
  listType: 'SUBMISSION' as string,
});

// 模式选项
const modeOptions = [
  { value: 'table', label: '📋 表格式' },
  { value: 'list', label: '📝 清单式' },
];

// 根据清单ID获取该清单的证据列表
function getEvidencesForList(list: EvidenceListDTO): EvidenceItem[] {
  const idList = list.evidenceIdList || [];
  if (idList.length === 0) {
    return [];
  }
  const idSet = new Set(idList);
  return allEvidences.value.filter((e) => idSet.has(e.id));
}

// 加载数据
async function loadData() {
  if (!props.matterId) return;

  loading.value = true;
  try {
    // 并行加载证据和清单
    const [evidences, lists] = await Promise.all([
      getEvidenceByMatter(props.matterId),
      getEvidenceListsByMatter(props.matterId),
    ]);

    allEvidences.value = (evidences || []).map((e) => mapEvidenceDTO(e));

    // 获取每个清单的详情（包含 evidenceIdList）
    const detailedLists = await Promise.all(
      (lists || []).map((list) =>
        getEvidenceListDetail(list.id).catch(() => list),
      ),
    );
    evidenceLists.value = detailedLists;
  } catch (error: any) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

function mapEvidenceDTO(dto: EvidenceDTO): EvidenceItem {
  return {
    id: dto.id,
    evidenceNo: dto.evidenceNo,
    name: dto.name,
    matterId: dto.matterId,
    matterName: dto.matterName,
    evidenceType: dto.evidenceType,
    evidenceTypeName: dto.evidenceTypeName,
    source: dto.source,
    groupName: dto.groupName,
    provePurpose: dto.provePurpose,
    description: dto.description,
    isOriginal: dto.isOriginal,
    originalCount: dto.originalCount,
    copyCount: dto.copyCount,
    pageStart: dto.pageStart,
    pageEnd: dto.pageEnd,
    pageRange:
      dto.pageStart && dto.pageEnd
        ? `${dto.pageStart}-${dto.pageEnd}`
        : undefined,
    fileUrl: dto.fileUrl,
    fileName: dto.fileName,
    fileSize: dto.fileSize,
    fileSizeDisplay: dto.fileSizeDisplay,
    fileType: dto.fileType,
    thumbnailUrl: dto.thumbnailUrl,
    crossExamStatus: dto.status,
    crossExamStatusName: dto.statusName,
    sortOrder: dto.sortOrder,
    createdAt: dto.createdAt,
  };
}

// 打开新建清单弹窗
function openCreateModal() {
  createForm.value = {
    name: '',
    listType: 'SUBMISSION',
  };
  showCreateModal.value = true;
}

// 创建新清单
async function handleCreateList() {
  if (!createForm.value.name.trim()) {
    message.warning('请输入清单名称');
    return;
  }

  try {
    await createEvidenceList({
      matterId: props.matterId,
      name: createForm.value.name,
      listType: createForm.value.listType,
      evidenceIds: [], // 新建空清单
    });
    message.success('创建成功');
    showCreateModal.value = false;
    loadData();
    emit('change');
  } catch (error: any) {
    message.error(error.message || '创建失败');
  }
}

// 删除清单
async function handleDeleteList(listId: number) {
  try {
    await deleteEvidenceList(listId);
    message.success('删除成功');
    loadData();
    emit('change');
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 刷新单个清单
async function handleRefreshList(listId: number) {
  try {
    const detail = await getEvidenceListDetail(listId);
    const index = evidenceLists.value.findIndex((l) => l.id === listId);
    if (index !== -1) {
      evidenceLists.value[index] = detail;
    }
  } catch (error: any) {
    console.error('刷新清单失败:', error);
  }
  // 同时刷新证据数据
  const evidences = await getEvidenceByMatter(props.matterId);
  allEvidences.value = (evidences || []).map((e) => mapEvidenceDTO(e));
  emit('change');
}

// 导出清单
async function handleExportList(list: EvidenceListDTO, format: 'pdf' | 'word') {
  const evidences = getEvidencesForList(list);
  if (evidences.length === 0) {
    message.warning('该清单暂无证据可导出');
    return;
  }

  try {
    message.loading('正在导出...', 0);

    const items: EvidenceExportItem[] = evidences.map((e, index) => ({
      id: e.id,
      name: e.name,
      source: e.source || '',
      provePurpose: e.provePurpose || '',
      isOriginal: e.isOriginal,
      pageStart: e.pageStart,
      pageEnd: e.pageEnd,
      listOrder: index + 1,
    }));

    await exportEvidenceList(props.matterId, items, format);
    message.destroy();
    message.success('导出成功');
  } catch (error: any) {
    message.destroy();
    message.error(error.message || '导出失败');
  }
}

// 获取类型名称
function getTypeName(type?: string) {
  return (
    EVIDENCE_LIST_TYPE_OPTIONS.find((o) => o.value === type)?.label ||
    type ||
    '未分类'
  );
}

// 获取类型颜色
function getTypeColor(type?: string) {
  switch (type) {
    case 'COURT': {
      return 'orange';
    }
    case 'EXCHANGE': {
      return 'green';
    }
    case 'SUBMISSION': {
      return 'blue';
    }
    default: {
      return 'default';
    }
  }
}

// 监听 matterId 变化
watch(
  () => props.matterId,
  () => {
    if (props.matterId) {
      loadData();
    }
  },
  { immediate: true },
);

// 暴露刷新方法
defineExpose({
  refresh: loadData,
});
</script>

<template>
  <div class="evidence-list-manager">
    <Spin :spinning="loading">
      <!-- 顶部操作栏 -->
      <div class="top-toolbar">
        <div class="left">
          <span class="title">📋 证据清单</span>
          <Tag v-if="evidenceLists.length > 0" color="blue">
            {{ evidenceLists.length }} 个
          </Tag>
        </div>
        <div class="right">
          <!-- 模式切换 -->
          <Segmented
            v-model:value="editMode"
            :options="modeOptions"
            size="small"
          />
          <Button v-if="!readonly" type="primary" @click="openCreateModal">
            <Plus class="mr-1 h-4 w-4" /> 新建清单
          </Button>
        </div>
      </div>

      <!-- 无清单提示 -->
      <Empty
        v-if="evidenceLists.length === 0"
        description="暂无证据清单，点击上方按钮创建"
        style="margin: 40px 0"
      />

      <!-- 多个清单表格 -->
      <div v-else class="lists-container">
        <Card
          v-for="list in evidenceLists"
          :key="list.id"
          class="list-card"
          :bordered="true"
        >
          <!-- 清单标题栏 -->
          <template #title>
            <div class="list-header">
              <div class="list-info">
                <span class="list-name">{{ list.name }}</span>
                <Tag :color="getTypeColor(list.listType)" size="small">
                  {{ getTypeName(list.listType) }}
                </Tag>
                <span class="list-meta">
                  编号: {{ list.listNo }} | 证据:
                  {{ list.evidenceIdList?.length || 0 }} 项
                </span>
              </div>
            </div>
          </template>

          <!-- 清单操作按钮 -->
          <template #extra>
            <div class="list-actions">
              <Button size="small" @click="() => handleRefreshList(list.id)">
                <RotateCw class="h-3 w-3" />
              </Button>
              <Popconfirm
                v-if="!readonly"
                title="确定删除此清单吗？"
                ok-text="删除"
                cancel-text="取消"
                @confirm="handleDeleteList(list.id)"
              >
                <Button size="small" danger>
                  <Trash class="h-3 w-3" />
                </Button>
              </Popconfirm>
            </div>
          </template>

          <!-- 清单内容：表格式或清单式 -->
          <EvidenceTableEditor
            v-if="editMode === 'table'"
            :matter-id="matterId"
            :matter-name="matterName"
            :evidences="getEvidencesForList(list)"
            :readonly="readonly"
            :list-id="list.id"
            :list-name="list.name"
            @refresh="() => handleRefreshList(list.id)"
            @export="(format) => handleExportList(list, format)"
          />

          <EvidenceListDisplay
            v-else
            :matter-id="matterId"
            :matter-name="matterName"
            :evidences="getEvidencesForList(list)"
            :readonly="readonly"
            :list-id="list.id"
            :list-name="list.name"
            @refresh="() => handleRefreshList(list.id)"
            @export="(format) => handleExportList(list, format)"
          />
        </Card>
      </div>
    </Spin>

    <!-- 新建清单弹窗 -->
    <Modal
      v-model:open="showCreateModal"
      title="新建证据清单"
      @ok="handleCreateList"
      :width="450"
    >
      <Form layout="vertical" :model="createForm" style="margin-top: 16px">
        <FormItem label="清单名称" required>
          <Input
            v-model:value="createForm.name"
            placeholder="如：一审原告证据清单、二审补充证据"
          />
        </FormItem>
        <FormItem label="清单类型">
          <Select v-model:value="createForm.listType">
            <SelectOption
              v-for="opt in EVIDENCE_LIST_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </SelectOption>
          </Select>
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.evidence-list-manager {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.top-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.top-toolbar .left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.top-toolbar .title {
  font-size: 16px;
  font-weight: 600;
}

.top-toolbar .right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.lists-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.list-card {
  border-radius: 8px;
}

.list-card :deep(.ant-card-head) {
  background: #fafafa;
  border-radius: 8px 8px 0 0;
}

.list-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.list-info {
  display: flex;
  gap: 8px;
  align-items: center;
}

.list-name {
  font-size: 15px;
  font-weight: 600;
}

.list-meta {
  font-size: 12px;
  font-weight: normal;
  color: #999;
}

.list-actions {
  display: flex;
  gap: 8px;
}
</style>
