<script setup lang="ts">
/**
 * 证据详情面板组件
 */
import { computed, ref, watch } from 'vue';
import { Descriptions, DescriptionsItem, Button, Space, Image, Tag } from 'ant-design-vue';
import { Eye, X, ArrowDown } from '@vben/icons';
import type { EvidenceItem } from './types';
import { formatFileSize, getFileTypeInfo } from './types';
import { getEvidencePreviewUrl } from '#/api/evidence';

const props = defineProps<{
  evidence: EvidenceItem | null;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'edit', evidence: EvidenceItem): void;
  (e: 'delete', evidence: EvidenceItem): void;
  (e: 'download', evidence: EvidenceItem): void;
  (e: 'preview', evidence: EvidenceItem): void;
}>();

const fileTypeInfo = computed(() => getFileTypeInfo(props.evidence?.fileType));

const statusColor = computed(() => {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    IN_PROGRESS: 'processing',
    COMPLETED: 'success',
  };
  return colorMap[props.evidence?.crossExamStatus || ''] || 'default';
});

// 预签名 URL（用于图片和音频预览）
const presignedUrl = ref<string | null>(null);

// 监听 evidence 变化，获取预签名 URL
watch(() => props.evidence, async (newEvidence) => {
  presignedUrl.value = null;
  if (newEvidence?.id && newEvidence.fileUrl) {
    // 图片和音频需要预签名 URL
    if (newEvidence.fileType === 'image' || newEvidence.fileType === 'audio') {
      try {
        const result = await getEvidencePreviewUrl(newEvidence.id);
        presignedUrl.value = result.fileUrl;
      } catch (e) {
        console.error('获取预签名URL失败', e);
      }
    }
  }
}, { immediate: true });

function handleDownload() {
  if (props.evidence) emit('download', props.evidence);
}

function handlePreview() {
  if (props.evidence) emit('preview', props.evidence);
}
</script>

<template>
  <div v-if="evidence" class="evidence-detail">
    <!-- 头部操作 -->
    <div class="detail-header">
      <span class="detail-title">{{ evidence.name }}</span>
      <Space v-if="!readonly">
        <Button type="link" size="small" @click="emit('edit', evidence)">
          编辑
        </Button>
        <Button type="link" size="small" danger @click="emit('delete', evidence)">
          <template #icon><X class="w-4 h-4" /></template>
        </Button>
      </Space>
    </div>

    <!-- 文件预览区 -->
    <div v-if="evidence.fileUrl" class="preview-area">
      <!-- 图片预览 -->
      <template v-if="evidence.fileType === 'image'">
        <Image v-if="presignedUrl" :src="presignedUrl" :preview="true" class="preview-image" />
        <div v-else class="loading-preview">加载中...</div>
      </template>
      <!-- 视频预览 -->
      <template v-else-if="evidence.fileType === 'video'">
        <div class="video-preview" @click="handlePreview">
          <Eye class="play-icon w-12 h-12" />
          <span>点击播放视频</span>
        </div>
      </template>
      <!-- 音频预览 -->
      <template v-else-if="evidence.fileType === 'audio'">
        <audio v-if="presignedUrl" :src="presignedUrl" controls class="audio-player" />
        <div v-else class="loading-preview">加载中...</div>
      </template>
      <!-- 其他文件 -->
      <template v-else>
        <div class="file-preview">
          <div class="file-icon" :style="{ color: fileTypeInfo.color }">
            {{ evidence.fileType === 'pdf' ? '📄' : evidence.fileType === 'word' ? '📝' : evidence.fileType === 'excel' ? '📊' : '📎' }}
          </div>
          <div class="file-name">{{ evidence.fileName }}</div>
          <Space>
            <Button v-if="fileTypeInfo.canPreview" size="small" @click="handlePreview">预览</Button>
            <Button size="small" @click="handleDownload">
              <template #icon><ArrowDown class="w-4 h-4" /></template>
              下载
            </Button>
          </Space>
        </div>
      </template>
    </div>

    <!-- 证据信息 -->
    <Descriptions :column="1" size="small" class="detail-info">
      <DescriptionsItem label="证据编号">{{ evidence.evidenceNo }}</DescriptionsItem>
      <DescriptionsItem label="证据类型">{{ evidence.evidenceTypeName || '-' }}</DescriptionsItem>
      <DescriptionsItem label="证据来源">{{ evidence.source || '-' }}</DescriptionsItem>
      <DescriptionsItem label="分组">{{ evidence.groupName || '未分组' }}</DescriptionsItem>
      <DescriptionsItem v-if="evidence.provePurpose" label="证明目的">
        {{ evidence.provePurpose }}
      </DescriptionsItem>
      <DescriptionsItem v-if="evidence.pageRange" label="页码范围">
        {{ evidence.pageRange }}
      </DescriptionsItem>
      <DescriptionsItem v-if="evidence.fileSize" label="文件大小">
        {{ formatFileSize(evidence.fileSize) }}
      </DescriptionsItem>
      <DescriptionsItem v-if="evidence.crossExamStatusName" label="质证状态">
        <Tag :color="statusColor">{{ evidence.crossExamStatusName }}</Tag>
      </DescriptionsItem>
      <DescriptionsItem v-if="evidence.description" label="描述">
        {{ evidence.description }}
      </DescriptionsItem>
    </Descriptions>
  </div>
  <div v-else class="empty-detail">
    <span>请选择证据查看详情</span>
  </div>
</template>

<style scoped lang="less">
.evidence-detail {
  height: 100%;
  display: flex;
  flex-direction: column;

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 12px;

    .detail-title {
      font-weight: 500;
      font-size: 14px;
      color: #262626;
    }
  }

  .preview-area {
    margin-bottom: 16px;
    background: #f5f5f5;
    border-radius: 8px;
    overflow: hidden;

    .preview-image {
      width: 100%;
      max-height: 200px;
      object-fit: contain;
    }

    .video-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 150px;
      cursor: pointer;
      color: #722ed1;

      .play-icon {
        font-size: 48px;
        margin-bottom: 8px;
      }

      &:hover {
        background: #e6e6e6;
      }
    }

    .audio-player {
      width: 100%;
      padding: 16px;
    }

    .loading-preview {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100px;
      color: #8c8c8c;
    }

    .file-preview {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px;

      .file-icon {
        font-size: 48px;
        margin-bottom: 8px;
      }

      .file-name {
        font-size: 12px;
        color: #595959;
        margin-bottom: 12px;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .detail-info {
    flex: 1;
    overflow-y: auto;
  }
}

.empty-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #bfbfbf;
}
</style>
