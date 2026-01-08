<script setup lang="ts">
/**
 * 证据文件上传组件
 */
import { ref, computed } from 'vue';
import { Upload, message, Progress } from 'ant-design-vue';
import { Inbox, X } from '@vben/icons';
import type { UploadProps, UploadFile } from 'ant-design-vue';
import { uploadEvidenceFile } from '#/api/evidence';
import { formatFileSize, getFileTypeInfo } from './types';

export interface UploadResult {
  fileUrl: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  thumbnailUrl: string | null;
}

const props = defineProps<{
  modelValue?: UploadResult | null;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: UploadResult | null): void;
  (e: 'success', result: UploadResult): void;
}>();

const uploading = ref(false);
const uploadProgress = ref(0);
const fileList = ref<UploadFile[]>([]);

const hasFile = computed(() => !!props.modelValue?.fileUrl);
const fileTypeInfo = computed(() => getFileTypeInfo(props.modelValue?.fileType));

const customRequest: UploadProps['customRequest'] = async (options) => {
  const { file, onSuccess, onError, onProgress } = options;
  
  try {
    uploading.value = true;
    uploadProgress.value = 0;
    
    // 模拟进度
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += 10;
        onProgress?.({ percent: uploadProgress.value });
      }
    }, 200);

    const result = await uploadEvidenceFile(file as File);
    clearInterval(progressInterval);
    
    // 处理响应数据
    const responseData = (result as any)?.data || result;
    const uploadResult: UploadResult = responseData?.data || responseData;
    
    if (uploadResult?.fileUrl) {
      uploadProgress.value = 100;
      emit('update:modelValue', uploadResult);
      emit('success', uploadResult);
      message.success('文件上传成功');
      onSuccess?.(uploadResult);
    } else {
      throw new Error('上传响应格式错误');
    }
  } catch (error: any) {
    message.error(error.message || '文件上传失败');
    onError?.(error);
  } finally {
    uploading.value = false;
    uploadProgress.value = 0;
  }
};

function handleRemove() {
  emit('update:modelValue', null);
  fileList.value = [];
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  // 文件大小限制 100MB
  const maxSize = 100 * 1024 * 1024;
  if (file.size > maxSize) {
    message.error('文件大小不能超过 100MB');
    return false;
  }
  return true;
};
</script>

<template>
  <div class="evidence-uploader">
    <!-- 已上传文件展示 -->
    <div v-if="hasFile" class="uploaded-file">
      <div class="file-info">
        <span class="file-icon" :style="{ color: fileTypeInfo.color }">
          {{ modelValue?.fileType === 'image' ? '🖼️' : modelValue?.fileType === 'pdf' ? '📄' : modelValue?.fileType === 'word' ? '📝' : modelValue?.fileType === 'video' ? '🎥' : '📎' }}
        </span>
        <div class="file-detail">
          <div class="file-name">{{ modelValue?.fileName }}</div>
          <div class="file-size">{{ formatFileSize(modelValue?.fileSize) }}</div>
        </div>
      </div>
      <X v-if="!disabled" class="remove-btn w-4 h-4" @click="handleRemove" />
    </div>

    <!-- 上传区域 -->
    <Upload.Dragger
      v-else
      v-model:file-list="fileList"
      :custom-request="customRequest"
      :before-upload="beforeUpload"
      :disabled="disabled || uploading"
      :show-upload-list="false"
      :multiple="false"
    >
      <div class="upload-content">
        <p class="upload-icon">
          <Inbox class="w-12 h-12" />
        </p>
        <p class="upload-text">点击或拖拽文件到此区域上传</p>
        <p class="upload-hint">支持图片、文档、音视频等格式，最大 100MB</p>
        <Progress v-if="uploading" :percent="uploadProgress" size="small" style="width: 80%; margin-top: 12px" />
      </div>
    </Upload.Dragger>
  </div>
</template>

<style scoped lang="less">
.evidence-uploader {
  .uploaded-file {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f5f5f5;
    border-radius: 8px;
    border: 1px solid #e8e8e8;

    .file-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .file-icon {
        font-size: 32px;
      }

      .file-detail {
        .file-name {
          font-size: 14px;
          color: #262626;
          max-width: 300px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .file-size {
          font-size: 12px;
          color: #8c8c8c;
        }
      }
    }

    .remove-btn {
      color: #ff4d4f;
      cursor: pointer;
      font-size: 16px;

      &:hover {
        color: #ff7875;
      }
    }
  }

  .upload-content {
    padding: 20px;

    .upload-icon {
      font-size: 48px;
      color: #1890ff;
      margin-bottom: 8px;
    }

    .upload-text {
      font-size: 14px;
      color: #262626;
      margin-bottom: 4px;
    }

    .upload-hint {
      font-size: 12px;
      color: #8c8c8c;
    }
  }
}
</style>
