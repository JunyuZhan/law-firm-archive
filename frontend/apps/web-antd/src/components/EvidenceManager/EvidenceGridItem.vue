<script setup lang="ts">
import type { EvidenceItem } from './types';

/**
 * 证据网格项组件
 * 用于网格视图中展示单个证据
 */
import { computed } from 'vue';

import { Tag, Tooltip } from 'ant-design-vue';

import EvidenceThumbnail from './EvidenceThumbnail.vue';

const props = defineProps<{
  draggable?: boolean;
  evidence: EvidenceItem;
  selected?: boolean;
}>();

const emit = defineEmits<{
  (e: 'click', evidence: EvidenceItem): void;
  (e: 'dblclick', evidence: EvidenceItem): void;
}>();

const statusColor = computed(() => {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    IN_PROGRESS: 'processing',
    COMPLETED: 'success',
  };
  return colorMap[props.evidence.crossExamStatus || ''] || 'default';
});
</script>

<template>
  <div
    class="evidence-grid-item"
    :class="{ selected, draggable }"
    @click="emit('click', evidence)"
    @dblclick="emit('dblclick', evidence)"
  >
    <div class="thumbnail-wrapper">
      <EvidenceThumbnail
        :evidence="evidence"
        size="medium"
        :show-preview="false"
      />
      <Tag
        v-if="evidence.crossExamStatusName"
        :color="statusColor"
        class="status-tag"
      >
        {{ evidence.crossExamStatusName }}
      </Tag>
    </div>
    <Tooltip :title="evidence.name" placement="bottom">
      <div class="evidence-name">{{ evidence.name }}</div>
    </Tooltip>
    <div class="evidence-type">{{ evidence.evidenceTypeName || '-' }}</div>
  </div>
</template>

<style scoped lang="less">
.evidence-grid-item {
  width: 100px;
  padding: 8px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;

  &:hover {
    border-color: #91d5ff;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
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

  .thumbnail-wrapper {
    position: relative;
    margin-bottom: 8px;

    .status-tag {
      position: absolute;
      top: 4px;
      right: 4px;
      font-size: 10px;
      padding: 0 4px;
      line-height: 16px;
    }
  }

  .evidence-name {
    font-size: 12px;
    font-weight: 500;
    color: #262626;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
  }

  .evidence-type {
    font-size: 11px;
    color: #8c8c8c;
    text-align: center;
  }
}
</style>
