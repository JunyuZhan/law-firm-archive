<script setup lang="ts">
import type { EvidenceItem } from './types';

/**
 * 证据缩略图组件
 * 根据文件类型显示不同的缩略图/图标
 */
import { computed } from 'vue';

import { Eye } from '@vben/icons';

import { Image } from 'ant-design-vue';

import { getFileTypeInfo } from './types';

const props = defineProps<{
  evidence: EvidenceItem;
  showPreview?: boolean;
  size?: 'large' | 'medium' | 'small';
}>();

const sizeMap = {
  small: { width: 60, height: 60, iconSize: 24 },
  medium: { width: 80, height: 80, iconSize: 32 },
  large: { width: 120, height: 120, iconSize: 48 },
};

const dimensions = computed(() => sizeMap[props.size || 'medium']);

const fileTypeInfo = computed(() => getFileTypeInfo(props.evidence.fileType));

const thumbnailUrl = computed(() => {
  // 图片文件优先使用缩略图，否则使用原图
  if (props.evidence.fileType === 'image') {
    return props.evidence.thumbnailUrl || props.evidence.fileUrl;
  }
  return null;
});

// 文件类型对应的 emoji 图标
const fileTypeEmoji = computed(() => {
  const emojiMap: Record<string, string> = {
    image: '🖼️',
    pdf: '📄',
    word: '📝',
    excel: '📊',
    ppt: '📽️',
    video: '🎥',
    audio: '🎵',
    other: '📎',
  };
  return emojiMap[props.evidence.fileType || 'other'] || '📎';
});
</script>

<template>
  <div
    class="evidence-thumbnail"
    :style="{
      width: `${dimensions.width}px`,
      height: `${dimensions.height}px`,
    }"
  >
    <!-- 图片类型：显示缩略图 -->
    <template v-if="evidence.fileType === 'image' && thumbnailUrl">
      <Image
        :src="thumbnailUrl"
        :width="dimensions.width"
        :height="dimensions.height"
        :preview="showPreview !== false"
        :preview-src="evidence.fileUrl"
        class="thumbnail-image"
      />
    </template>

    <!-- 视频类型：显示播放图标 -->
    <template v-else-if="evidence.fileType === 'video'">
      <div
        class="icon-container video-icon"
        :style="{ background: fileTypeInfo.color }"
      >
        <Eye
          :style="{
            width: `${dimensions.iconSize}px`,
            height: `${dimensions.iconSize}px`,
            color: '#fff',
          }"
        />
      </div>
    </template>

    <!-- 音频类型：显示音频图标 -->
    <template v-else-if="evidence.fileType === 'audio'">
      <div
        class="icon-container audio-icon"
        :style="{ background: fileTypeInfo.color }"
      >
        <span :style="{ fontSize: `${dimensions.iconSize}px` }">{{
          fileTypeEmoji
        }}</span>
      </div>
    </template>

    <!-- 其他文档类型：显示对应图标 -->
    <template v-else>
      <div class="icon-container" :style="{ background: '#f5f5f5' }">
        <span :style="{ fontSize: `${dimensions.iconSize}px` }">{{
          fileTypeEmoji
        }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped lang="less">
.evidence-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;

  .thumbnail-image {
    object-fit: cover;
    width: 100%;
    height: 100%;
  }

  .icon-container {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;

    &.video-icon,
    &.audio-icon {
      position: relative;
    }
  }

  :deep(.ant-image) {
    width: 100%;
    height: 100%;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}
</style>
