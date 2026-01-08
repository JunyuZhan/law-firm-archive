<script setup lang="ts">
/**
 * 证据列表项组件
 * 用于列表视图中展示单个证据
 */
import { computed } from 'vue';
import { Tag, Space, Button, Popconfirm } from 'ant-design-vue';
import { Eye, X, ArrowDown, GripVertical } from '@vben/icons';
import EvidenceThumbnail from './EvidenceThumbnail.vue';
import type { EvidenceItem } from './types';
import { formatFileSize } from './types';

const props = defineProps<{
  evidence: EvidenceItem;
  selected?: boolean;
  draggable?: boolean;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'click', evidence: EvidenceItem): void;
  (e: 'edit', evidence: EvidenceItem): void;
  (e: 'delete', evidence: EvidenceItem): void;
  (e: 'preview', evidence: EvidenceItem): void;
  (e: 'download', evidence: EvidenceItem): void;
}>();

const statusColor = computed(() => {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    IN_PROGRESS: 'processing',
    COMPLETED: 'success',
  };
  return colorMap[props.evidence.crossExamStatus || ''] || 'default';
});

const fileSizeDisplay = computed(() => {
  return props.evidence.fileSizeDisplay || formatFileSize(props.evidence.fileSize);
});
</script>

<template>
  <div
    class="evidence-list-item"
    :class="{ selected, draggable }"
    @click="emit('click', evidence)"
  >
    <!-- 拖拽手柄 -->
    <div v-if="draggable && !readonly" class="drag-handle">
      <GripVertical class="w-4 h-4" />
    </div>

    <!-- 缩略图 -->
    <div class="thumbnail-col">
      <EvidenceThumbnail :evidence="evidence" size="small" :showPreview="false" />
    </div>

    <!-- 基本信息 -->
    <div class="info-col">
      <div class="evidence-name">{{ evidence.name }}</div>
      <div class="evidence-meta">
        <span class="meta-item">{{ evidence.evidenceNo }}</span>
        <span class="meta-divider">|</span>
        <span class="meta-item">{{ evidence.evidenceTypeName }}</span>
        <span v-if="evidence.fileName" class="meta-divider">|</span>
        <span v-if="evidence.fileName" class="meta-item">{{ fileSizeDisplay }}</span>
      </div>
    </div>

    <!-- 证明目的 -->
    <div class="purpose-col">
      <div v-if="evidence.provePurpose" class="purpose-text" :title="evidence.provePurpose">
        {{ evidence.provePurpose }}
      </div>
      <div v-else class="purpose-empty">-</div>
    </div>

    <!-- 状态 -->
    <div class="status-col">
      <Tag v-if="evidence.crossExamStatusName" :color="statusColor">
        {{ evidence.crossExamStatusName }}
      </Tag>
    </div>

    <!-- 操作 -->
    <div class="action-col" @click.stop>
      <Space>
        <Button
          v-if="evidence.fileUrl"
          type="text"
          size="small"
          @click="emit('preview', evidence)"
        >
          <template #icon><Eye class="w-4 h-4" /></template>
        </Button>
        <Button
          v-if="evidence.fileUrl"
          type="text"
          size="small"
          @click="emit('download', evidence)"
        >
          <template #icon><ArrowDown class="w-4 h-4" /></template>
        </Button>
        <Button
          v-if="!readonly"
          type="text"
          size="small"
          @click="emit('edit', evidence)"
        >
          编辑
        </Button>
        <Popconfirm
          v-if="!readonly"
          title="确定删除该证据？"
          @confirm="emit('delete', evidence)"
        >
          <Button type="text" size="small" danger>
            <template #icon><X class="w-4 h-4" /></template>
          </Button>
        </Popconfirm>
      </Space>
    </div>
  </div>
</template>

<style scoped lang="less">
.evidence-list-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #91d5ff;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
  }

  &.selected {
    border-color: #1890ff;
    background: #e6f7ff;
  }

  &.draggable {
    cursor: grab;

    &:active {
      cursor: grabbing;
    }
  }

  .drag-handle {
    width: 24px;
    color: #bfbfbf;
    cursor: grab;
    margin-right: 8px;

    &:hover {
      color: #1890ff;
    }
  }

  .thumbnail-col {
    flex-shrink: 0;
    margin-right: 16px;
  }

  .info-col {
    flex: 1;
    min-width: 0;
    margin-right: 16px;

    .evidence-name {
      font-size: 14px;
      font-weight: 500;
      color: #262626;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .evidence-meta {
      font-size: 12px;
      color: #8c8c8c;

      .meta-divider {
        margin: 0 8px;
        color: #d9d9d9;
      }
    }
  }

  .purpose-col {
    width: 200px;
    flex-shrink: 0;
    margin-right: 16px;

    .purpose-text {
      font-size: 13px;
      color: #595959;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .purpose-empty {
      color: #bfbfbf;
    }
  }

  .status-col {
    width: 80px;
    flex-shrink: 0;
    margin-right: 16px;
  }

  .action-col {
    flex-shrink: 0;
  }
}
</style>
