<script setup lang="ts">
/**
 * 证据详情面板组件
 */
import { computed, ref, watch } from 'vue';
import { Descriptions, DescriptionsItem, Button, Space, Image, Tag, Spin, Tooltip, message, Modal, Textarea } from 'ant-design-vue';
import { Eye, X, ArrowDown } from '@vben/icons';
import { IconifyIcon, Copy } from '@vben/icons';
import type { EvidenceItem } from './types';
import { formatFileSize, getFileTypeInfo } from './types';
import { getEvidencePreviewUrl } from '#/api/evidence';
import { recognizeTextByUrl, type OcrResultDTO } from '#/api/ocr';

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

// OCR识别状态
const ocrLoading = ref(false);
const ocrResult = ref<OcrResultDTO | null>(null);
const ocrModalVisible = ref(false);

// 监听 evidence 变化，获取预签名 URL
watch(() => props.evidence, async (newEvidence) => {
  presignedUrl.value = null;
  ocrResult.value = null;
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

// OCR提取图片文字
async function handleOcrExtract() {
  if (!presignedUrl.value) {
    message.warning('请等待图片加载完成');
    return;
  }
  
  ocrLoading.value = true;
  try {
    const result = await recognizeTextByUrl(presignedUrl.value);
    ocrResult.value = result;
    
    if (result.success && result.rawText) {
      ocrModalVisible.value = true;
      message.success('文字提取成功');
    } else if (result.success) {
      message.info('未识别到文字内容');
    } else {
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || 'OCR识别失败');
  } finally {
    ocrLoading.value = false;
  }
}

// 复制OCR结果
function copyOcrText() {
  if (ocrResult.value?.rawText) {
    navigator.clipboard.writeText(ocrResult.value.rawText);
    message.success('已复制到剪贴板');
  }
}

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
        <!-- OCR提取文字按钮 -->
        <div v-if="presignedUrl" class="ocr-action">
          <Tooltip title="使用OCR识别图片中的文字">
            <Button 
              size="small" 
              type="primary" 
              ghost
              :loading="ocrLoading"
              @click="handleOcrExtract"
            >
              <template #icon><IconifyIcon icon="ant-design:scan-outlined" /></template>
              提取文字
            </Button>
          </Tooltip>
        </div>
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

  <!-- OCR结果弹窗 -->
  <Modal
    v-model:open="ocrModalVisible"
    title="OCR文字识别结果"
    width="600px"
  >
    <template #footer>
      <Space>
        <Button @click="ocrModalVisible = false">关闭</Button>
        <Button type="primary" @click="copyOcrText">
          <template #icon><Copy class="size-4" /></template>
          复制文字
        </Button>
      </Space>
    </template>
    <div v-if="ocrResult?.success" class="ocr-result">
      <div class="ocr-confidence mb-2">
        <Tag color="green">置信度: {{ Math.round((ocrResult.confidence || 0) * 100) }}%</Tag>
      </div>
      <Textarea 
        :value="ocrResult.rawText" 
        :rows="12" 
        readonly
        class="ocr-text"
      />
    </div>
    <div v-else class="text-center text-gray-500 py-8">
      未识别到文字内容
    </div>
  </Modal>
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

    .ocr-action {
      padding: 8px;
      text-align: center;
      background: #fafafa;
      border-top: 1px solid #f0f0f0;
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
