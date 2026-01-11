<script setup lang="ts">
import type { EvidenceGroup, EvidenceItem, ViewMode } from './types';

import type { EvidenceDTO } from '#/api/evidence';

/**
 * 证据管理主组件
 * 支持网格/列表视图、分组树、拖拽排序、证据详情
 */
import { computed, onMounted, ref, watch } from 'vue';

import { LayoutGrid, List, Plus, RotateCw } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  message,
  Modal,
  Segmented,
  Space,
  Spin,
  Tooltip,
} from 'ant-design-vue';
import draggable from 'vuedraggable';

import {
  deleteEvidence,
  downloadEvidenceAsZip,
  getEvidenceByMatter,
  getEvidenceGroups,
  updateEvidenceSort,
} from '#/api/evidence';

import EvidenceDetail from './EvidenceDetail.vue';
import EvidenceForm from './EvidenceForm.vue';
import EvidenceGridItem from './EvidenceGridItem.vue';
import EvidenceListItem from './EvidenceListItem.vue';

const props = defineProps<{
  matterId: number;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'export'): void;
}>();

// 状态
const loading = ref(false);
const evidenceList = ref<EvidenceItem[]>([]);
const groups = ref<string[]>([]);
const selectedGroupKey = ref<string>('all');
const selectedEvidence = ref<EvidenceItem | null>(null);
const viewMode = ref<ViewMode>('grid');
const formVisible = ref(false);
const editingEvidence = ref<EvidenceItem | null>(null);
const dragEnabled = ref(false);

// 批量选择状态
const selectedEvidenceIds = ref<Set<number>>(new Set());
const batchDownloading = ref(false);
const selectMode = ref(false);

// 分组树数据
const groupTreeData = computed(() => {
  const data: EvidenceGroup[] = [
    {
      key: 'all',
      title: `全部 (${evidenceList.value.length})`,
      count: evidenceList.value.length,
    },
    {
      key: 'ungrouped',
      title: `未分组 (${evidenceList.value.filter((e) => !e.groupName).length})`,
      count: evidenceList.value.filter((e) => !e.groupName).length,
    },
  ];
  groups.value.forEach((g) => {
    const count = evidenceList.value.filter((e) => e.groupName === g).length;
    data.push({ key: g, title: `${g} (${count})`, count });
  });
  return data;
});

// 当前分组的证据列表
const currentEvidences = computed(() => {
  if (selectedGroupKey.value === 'all') {
    return [...evidenceList.value].sort(
      (a, b) => (a.sortOrder || 0) - (b.sortOrder || 0),
    );
  }
  if (selectedGroupKey.value === 'ungrouped') {
    return evidenceList.value
      .filter((e) => !e.groupName)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  }
  return evidenceList.value
    .filter((e) => e.groupName === selectedGroupKey.value)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
});

// 视图模式选项
const viewModeOptions = [
  { value: 'grid', icon: LayoutGrid, title: '网格视图' },
  { value: 'list', icon: List, title: '列表视图' },
];

// 加载数据
async function loadData() {
  if (!props.matterId) return;

  loading.value = true;
  try {
    const [evidences, groupList] = await Promise.all([
      getEvidenceByMatter(props.matterId),
      getEvidenceGroups(props.matterId),
    ]);
    evidenceList.value = (evidences || []).map(mapEvidenceDTO);
    groups.value = groupList || [];
  } catch (error: any) {
    console.error('加载证据列表失败:', error);
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

// 选择分组
function handleGroupSelect(key: string) {
  selectedGroupKey.value = key;
  selectedEvidence.value = null;
}

// 选择证据
function handleEvidenceClick(evidence: EvidenceItem) {
  selectedEvidence.value = evidence;
}

// 双击预览
function handleEvidenceDblClick(evidence: EvidenceItem) {
  handlePreview(evidence);
}

// 添加证据
function handleAdd() {
  editingEvidence.value = null;
  formVisible.value = true;
}

// 编辑证据
function handleEdit(evidence: EvidenceItem) {
  editingEvidence.value = evidence;
  formVisible.value = true;
}

// 删除证据
function handleDelete(evidence: EvidenceItem) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除证据 "${evidence.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteEvidence(evidence.id);
        message.success('删除成功');
        if (selectedEvidence.value?.id === evidence.id) {
          selectedEvidence.value = null;
        }
        await loadData();
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

// 预览文件
async function handlePreview(evidence: EvidenceItem) {
  if (!evidence.fileUrl) {
    message.warning('该证据没有关联文件');
    return;
  }

  try {
    // 获取预签名URL
    const { getEvidencePreviewUrl } = await import('#/api/evidence');
    const result = await getEvidencePreviewUrl(evidence.id);
    const previewUrl = result.fileUrl;

    // 图片直接用 antd Image 预览
    if (evidence.fileType === 'image') {
      // 已在 EvidenceDetail 中处理
      return;
    }

    // 视频/音频打开新窗口播放
    if (evidence.fileType === 'video' || evidence.fileType === 'audio') {
      window.open(previewUrl, '_blank');
      return;
    }

    // PDF 直接打开
    if (evidence.fileType === 'pdf') {
      window.open(previewUrl, '_blank');
      return;
    }

    // Office 文档：使用本地 OnlyOffice 预览
    if (['excel', 'ppt', 'word'].includes(evidence.fileType || '')) {
      // 获取 OnlyOffice 可访问的 URL
      const { getEvidenceOnlyOfficeUrl } = await import('#/api/evidence');
      const onlyOfficeResult = await getEvidenceOnlyOfficeUrl(evidence.id);

      // 使用 OnlyOffice Document Server 预览
      const docType =
        evidence.fileType === 'word'
          ? 'word'
          : evidence.fileType === 'excel'
            ? 'cell'
            : 'slide';
      const fileExt =
        evidence.fileName?.split('.').pop()?.toLowerCase() || 'docx';

      // 打开 OnlyOffice 预览窗口
      const onlyofficeUrl = `/office-preview?url=${encodeURIComponent(onlyOfficeResult.fileUrl)}&filename=${encodeURIComponent(evidence.fileName || 'document')}&type=${docType}&ext=${fileExt}`;
      window.open(onlyofficeUrl, '_blank', 'width=1200,height=800');
      return;
    }

    // 其他文件下载
    handleDownload(evidence);
  } catch {
    message.error('获取预览链接失败');
  }
}

// 下载文件
async function handleDownload(evidence: EvidenceItem) {
  if (!evidence.fileUrl) {
    message.warning('该证据没有关联文件');
    return;
  }

  try {
    // 获取预签名下载URL
    const { getEvidenceDownloadUrl } = await import('#/api/evidence');
    const result = await getEvidenceDownloadUrl(evidence.id);

    const link = document.createElement('a');
    link.href = result.downloadUrl;
    link.download = result.fileName || 'download';
    link.click();
  } catch {
    message.error('获取下载链接失败');
  }
}

// 批量选择相关方法
function toggleSelectMode() {
  selectMode.value = !selectMode.value;
  if (!selectMode.value) {
    selectedEvidenceIds.value = new Set();
  }
}

function toggleEvidenceSelection(evidenceId: number, event?: Event) {
  event?.stopPropagation();
  if (selectedEvidenceIds.value.has(evidenceId)) {
    selectedEvidenceIds.value.delete(evidenceId);
  } else {
    selectedEvidenceIds.value.add(evidenceId);
  }
  selectedEvidenceIds.value = new Set(selectedEvidenceIds.value);
}

function isEvidenceSelected(evidenceId: number) {
  return selectedEvidenceIds.value.has(evidenceId);
}

function isAllEvidenceSelected() {
  return (
    currentEvidences.value.length > 0 &&
    currentEvidences.value.every((e) => selectedEvidenceIds.value.has(e.id))
  );
}

function toggleSelectAllEvidence() {
  if (isAllEvidenceSelected()) {
    currentEvidences.value.forEach((e) =>
      selectedEvidenceIds.value.delete(e.id),
    );
  } else {
    currentEvidences.value.forEach((e) => selectedEvidenceIds.value.add(e.id));
  }
  selectedEvidenceIds.value = new Set(selectedEvidenceIds.value);
}

function clearEvidenceSelection() {
  selectedEvidenceIds.value = new Set();
  selectMode.value = false;
}

// 批量下载证据
async function handleBatchDownload() {
  const ids = [...selectedEvidenceIds.value];
  if (ids.length === 0) {
    message.warning('请先选择要下载的证据');
    return;
  }

  if (ids.length > 100) {
    message.warning('单次最多下载100个证据文件');
    return;
  }

  batchDownloading.value = true;
  try {
    const fileName = `证据材料_${new Date().toISOString().slice(0, 10)}.zip`;
    await downloadEvidenceAsZip(ids, fileName);
    message.success(`成功下载 ${ids.length} 个证据文件`);
    clearEvidenceSelection();
  } catch (error: any) {
    message.error(`批量下载失败：${error.message || '未知错误'}`);
  } finally {
    batchDownloading.value = false;
  }
}

// 拖拽排序结束
async function handleDragEnd() {
  // 更新排序
  const updates = currentEvidences.value.map((item, index) => ({
    id: item.id,
    sortOrder: index + 1,
  }));

  try {
    await Promise.all(
      updates.map((u) => updateEvidenceSort(u.id, u.sortOrder)),
    );
    message.success('排序已保存');
  } catch {
    message.error('保存排序失败');
    await loadData();
  }
}

// 表单提交成功
function handleFormSuccess() {
  loadData();
}

// 监听 matterId 变化
watch(
  () => props.matterId,
  () => {
    loadData();
  },
  { immediate: true },
);

onMounted(() => {
  if (props.matterId) {
    loadData();
  }
});

defineExpose({
  reload: loadData,
});
</script>

<template>
  <div class="evidence-manager">
    <Spin :spinning="loading">
      <div class="manager-layout">
        <!-- 左侧：分组树 -->
        <Card class="group-panel" size="small" title="证据分组">
          <div class="group-list">
            <div
              v-for="group in groupTreeData"
              :key="group.key"
              class="group-item"
              :class="{ active: selectedGroupKey === group.key }"
              @click="handleGroupSelect(group.key)"
            >
              {{ group.title }}
            </div>
          </div>
          <template #extra>
            <Button
              v-if="!readonly"
              type="link"
              size="small"
              @click="handleAdd"
            >
              <Plus class="h-4 w-4" /> 添加
            </Button>
          </template>
        </Card>

        <!-- 中间：证据列表 -->
        <Card class="evidence-panel" size="small">
          <template #title>
            <div class="panel-header">
              <span>{{
                selectedGroupKey === 'all'
                  ? '全部证据'
                  : selectedGroupKey === 'ungrouped'
                    ? '未分组'
                    : selectedGroupKey
              }}</span>
              <span class="count">({{ currentEvidences.length }})</span>
            </div>
          </template>
          <template #extra>
            <Space>
              <!-- 批量操作按钮 -->
              <template v-if="selectMode">
                <Button
                  v-if="selectedEvidenceIds.size > 0"
                  type="primary"
                  size="small"
                  :loading="batchDownloading"
                  @click="handleBatchDownload"
                >
                  批量下载 ({{ selectedEvidenceIds.size }})
                </Button>
                <Button size="small" @click="toggleSelectAllEvidence">
                  {{ isAllEvidenceSelected() ? '取消全选' : '全选' }}
                </Button>
                <Button size="small" @click="clearEvidenceSelection">
                  取消选择
                </Button>
              </template>
              <Tooltip :title="selectMode ? '退出选择' : '批量选择'">
                <Button
                  type="text"
                  size="small"
                  :class="{ 'select-active': selectMode }"
                  @click="toggleSelectMode"
                >
                  ☑
                </Button>
              </Tooltip>
              <Tooltip title="刷新">
                <Button type="text" size="small" @click="loadData">
                  <template #icon><RotateCw class="h-4 w-4" /></template>
                </Button>
              </Tooltip>
              <Segmented
                v-model:value="viewMode"
                :options="
                  viewModeOptions.map((o) => ({
                    value: o.value,
                    title: o.title,
                  }))
                "
                size="small"
              >
                <template #label="{ value }">
                  <component
                    :is="viewModeOptions.find((o) => o.value === value)?.icon"
                    class="h-4 w-4"
                  />
                </template>
              </Segmented>
              <Tooltip
                v-if="!readonly"
                :title="dragEnabled ? '关闭排序' : '拖拽排序'"
              >
                <Button
                  type="text"
                  size="small"
                  :class="{ 'drag-active': dragEnabled }"
                  @click="dragEnabled = !dragEnabled"
                >
                  ⋮⋮
                </Button>
              </Tooltip>
              <Button
                v-if="!readonly"
                type="primary"
                size="small"
                @click="handleAdd"
              >
                <Plus class="h-4 w-4" /> 添加证据
              </Button>
            </Space>
          </template>

          <!-- 网格视图 -->
          <div v-if="viewMode === 'grid'" class="grid-view">
            <draggable
              v-if="currentEvidences.length > 0"
              :list="currentEvidences"
              item-key="id"
              :disabled="!dragEnabled || readonly"
              class="grid-container"
              ghost-class="ghost"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <div
                  class="grid-item-wrapper"
                  :class="{ 'item-selected': isEvidenceSelected(element.id) }"
                >
                  <div
                    v-if="selectMode"
                    class="item-checkbox"
                    @click.stop="toggleEvidenceSelection(element.id, $event)"
                  >
                    <input
                      type="checkbox"
                      :checked="isEvidenceSelected(element.id)"
                      @click.stop
                      @change="toggleEvidenceSelection(element.id)"
                    />
                  </div>
                  <EvidenceGridItem
                    :evidence="element"
                    :selected="selectedEvidence?.id === element.id"
                    :draggable="dragEnabled && !readonly"
                    @click="
                      selectMode
                        ? toggleEvidenceSelection(element.id)
                        : handleEvidenceClick(element)
                    "
                    @dblclick="handleEvidenceDblClick"
                  />
                </div>
              </template>
            </draggable>
            <Empty v-else description="暂无证据" />
          </div>

          <!-- 列表视图 -->
          <div v-else class="list-view">
            <draggable
              v-if="currentEvidences.length > 0"
              :list="currentEvidences"
              item-key="id"
              :disabled="!dragEnabled || readonly"
              class="list-container"
              ghost-class="ghost"
              handle=".drag-handle"
              @end="handleDragEnd"
            >
              <template #item="{ element }">
                <div
                  class="list-item-wrapper"
                  :class="{ 'item-selected': isEvidenceSelected(element.id) }"
                >
                  <div
                    v-if="selectMode"
                    class="item-checkbox"
                    @click.stop="toggleEvidenceSelection(element.id, $event)"
                  >
                    <input
                      type="checkbox"
                      :checked="isEvidenceSelected(element.id)"
                      @click.stop
                      @change="toggleEvidenceSelection(element.id)"
                    />
                  </div>
                  <EvidenceListItem
                    :evidence="element"
                    :selected="selectedEvidence?.id === element.id"
                    :draggable="dragEnabled && !readonly"
                    :readonly="readonly"
                    @click="
                      selectMode
                        ? toggleEvidenceSelection(element.id)
                        : handleEvidenceClick(element)
                    "
                    @edit="handleEdit"
                    @delete="handleDelete"
                    @preview="handlePreview"
                    @download="handleDownload"
                  />
                </div>
              </template>
            </draggable>
            <Empty v-else description="暂无证据" />
          </div>
        </Card>

        <!-- 右侧：详情面板 -->
        <Card class="detail-panel" size="small" title="证据详情">
          <EvidenceDetail
            :evidence="selectedEvidence"
            :readonly="readonly"
            @edit="handleEdit"
            @delete="handleDelete"
            @download="handleDownload"
            @preview="handlePreview"
          />
        </Card>
      </div>
    </Spin>

    <!-- 证据表单弹窗 -->
    <EvidenceForm
      v-model:open="formVisible"
      :evidence="editingEvidence"
      :matter-id="matterId"
      :groups="groups"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="less">
.evidence-manager {
  height: 100%;
  min-height: 500px;

  .manager-layout {
    display: flex;
    gap: 16px;
    height: 100%;
  }

  .group-panel {
    width: 200px;
    flex-shrink: 0;

    .group-list {
      .group-item {
        padding: 8px 12px;
        cursor: pointer;
        border-radius: 4px;
        margin-bottom: 4px;
        transition: all 0.2s;

        &:hover {
          background: #f5f5f5;
        }

        &.active {
          background: #e6f7ff;
          color: #1890ff;
        }
      }
    }
  }

  .evidence-panel {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;

    .panel-header {
      .count {
        color: #8c8c8c;
        margin-left: 4px;
      }
    }

    .drag-active,
    .select-active {
      color: #1890ff;
      background: #e6f7ff;
    }

    .grid-view {
      .grid-container {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
        gap: 12px;
      }
    }

    .list-view {
      .list-container {
        display: flex;
        flex-direction: column;
      }
    }

    .ghost {
      opacity: 0.5;
      background: #e6f7ff;
    }

    // 批量选择样式
    .grid-item-wrapper,
    .list-item-wrapper {
      position: relative;

      &.item-selected {
        background-color: #e6f7ff;
        border-radius: 4px;
      }

      .item-checkbox {
        position: absolute;
        top: 4px;
        left: 4px;
        z-index: 10;
        background: rgba(255, 255, 255, 0.9);
        border-radius: 4px;
        padding: 2px 4px;

        input[type='checkbox'] {
          width: 16px;
          height: 16px;
          cursor: pointer;
        }
      }
    }

    .list-item-wrapper {
      display: flex;
      align-items: center;

      .item-checkbox {
        position: relative;
        top: auto;
        left: auto;
        margin-right: 8px;
        padding: 8px;
      }
    }
  }

  .detail-panel {
    width: 300px;
    flex-shrink: 0;
  }
}

:deep(.ant-card-body) {
  flex: 1;
  overflow: auto;
}
</style>
