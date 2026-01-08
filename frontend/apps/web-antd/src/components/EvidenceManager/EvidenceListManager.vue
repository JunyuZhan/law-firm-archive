<script setup lang="ts">
/**
 * 证据整理管理组件
 * 支持两种编辑模式：表格式 和 清单式
 */
import { ref, watch, onMounted } from 'vue';
import { Spin, Empty, Segmented, message } from 'ant-design-vue';
import EvidenceTableEditor from './EvidenceTableEditor.vue';
import EvidenceListDisplay from './EvidenceListDisplay.vue';
import type { EvidenceItem } from './types';
import { getEvidenceByMatter, exportEvidenceList, type EvidenceDTO, type EvidenceExportItem } from '#/api/evidence';

type EditMode = 'table' | 'list';

const props = defineProps<{
  matterId: number;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'change'): void;
}>();

// 状态
const loading = ref(false);
const evidenceList = ref<EvidenceItem[]>([]);
const editMode = ref<EditMode>('table');

// 模式选项
const modeOptions = [
  { value: 'table', label: '📋 表格式' },
  { value: 'list', label: '📝 清单式' },
];

// 加载数据
async function loadData() {
  if (!props.matterId) return;
  
  loading.value = true;
  try {
    const evidences = await getEvidenceByMatter(props.matterId);
    evidenceList.value = (evidences || []).map(mapEvidenceDTO);
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
    pageRange: dto.pageStart && dto.pageEnd ? `${dto.pageStart}-${dto.pageEnd}` : undefined,
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

// 刷新数据
function handleRefresh() {
  loadData();
  emit('change');
}

// 导出
async function handleExport(format: 'word' | 'pdf') {
  if (evidenceList.value.length === 0) {
    message.warning('暂无证据可导出');
    return;
  }

  try {
    message.loading('正在导出...', 0);
    
    const items: EvidenceExportItem[] = evidenceList.value.map((e, index) => ({
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

// 监听 matterId 变化
watch(() => props.matterId, () => {
  if (props.matterId) {
    loadData();
  }
}, { immediate: true });

// 暴露刷新方法
defineExpose({
  refresh: loadData,
});
</script>

<template>
  <div class="evidence-list-manager">
    <Spin :spinning="loading">
      <!-- 模式切换 -->
      <div class="mode-switcher" style="margin-bottom: 16px; display: flex; justify-content: center;">
        <Segmented v-model:value="editMode" :options="modeOptions" />
      </div>

      <!-- 表格式 -->
      <EvidenceTableEditor
        v-if="editMode === 'table'"
        :matter-id="matterId"
        :evidences="evidenceList"
        :readonly="readonly"
        @refresh="handleRefresh"
        @export="handleExport"
      />

      <!-- 清单式 -->
      <EvidenceListDisplay
        v-else
        :matter-id="matterId"
        :evidences="evidenceList"
        :readonly="readonly"
        @refresh="handleRefresh"
        @export="handleExport"
      />
    </Spin>
  </div>
</template>

<style scoped>
.evidence-list-manager {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}
</style>

