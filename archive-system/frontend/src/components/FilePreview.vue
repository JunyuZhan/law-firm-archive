<template>
  <el-dialog
    v-model="visible"
    :title="fileName"
    width="80%"
    destroy-on-close
    top="5vh"
    class="file-preview-dialog"
    @close="handleClose"
  >
    <div class="preview-container">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrapper">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- PDF预览 -->
      <iframe
        v-else-if="previewType === 'pdf'"
        :src="previewUrl"
        class="pdf-viewer"
      />

      <!-- 图片预览 -->
      <div v-else-if="previewType === 'image'" class="image-viewer">
        <img
          :src="previewUrl"
          :alt="fileName"
          @load="handleImageLoad"
          @error="handleError"
        />
      </div>

      <!-- 视频预览 -->
      <video
        v-else-if="previewType === 'video'"
        :src="previewUrl"
        controls
        class="video-viewer"
      />

      <!-- 音频预览 -->
      <audio
        v-else-if="previewType === 'audio'"
        :src="previewUrl"
        controls
        class="audio-viewer"
      />

      <!-- 不支持预览 -->
      <div v-else class="not-supported">
        <el-icon :size="64"><Document /></el-icon>
        <p>该文件类型不支持在线预览</p>
        <el-button type="primary" @click="handleDownload">
          <el-icon><Download /></el-icon>
          下载文件
        </el-button>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="handleDownload">
          <el-icon><Download /></el-icon>
          下载
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Document, Download } from '@element-plus/icons-vue'
import { getFilePreviewUrl, getFileDownloadUrl } from '@/api/archive'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  fileId: {
    type: [Number, String],
    default: null
  },
  fileName: {
    type: String,
    default: '文件预览'
  },
  fileExtension: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const previewUrl = ref('')
const downloadUrl = ref('')

// 根据扩展名判断预览类型
const previewType = computed(() => {
  const ext = props.fileExtension?.toLowerCase() || ''
  
  if (ext === 'pdf') return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)) return 'image'
  if (['mp4', 'webm', 'ogg'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'ogg', 'flac'].includes(ext)) return 'audio'
  
  return 'unsupported'
})

// 监听fileId变化
watch(() => props.fileId, async (newVal) => {
  if (newVal && props.modelValue) {
    await loadPreview()
  }
}, { immediate: true })

// 监听弹窗打开
watch(visible, async (newVal) => {
  if (newVal && props.fileId) {
    await loadPreview()
  } else {
    previewUrl.value = ''
    downloadUrl.value = ''
  }
})

// 加载预览
const loadPreview = async () => {
  if (!props.fileId) return
  
  loading.value = true
  try {
    // 获取预览URL
    const previewRes = await getFilePreviewUrl(props.fileId)
    if (previewRes.data?.url) {
      previewUrl.value = previewRes.data.url
    }
    
    // 获取下载URL
    const downloadRes = await getFileDownloadUrl(props.fileId)
    if (downloadRes.data?.url) {
      downloadUrl.value = downloadRes.data.url
    }
  } catch (e) {
    console.error('加载预览失败', e)
    ElMessage.error('加载预览失败')
  } finally {
    loading.value = false
  }
}

const handleImageLoad = () => {
  loading.value = false
}

const handleError = () => {
  loading.value = false
  ElMessage.error('文件加载失败')
}

const handleDownload = () => {
  if (downloadUrl.value) {
    window.open(downloadUrl.value, '_blank')
  } else {
    ElMessage.warning('下载链接未就绪')
  }
}

const handleClose = () => {
  visible.value = false
}
</script>

<style lang="scss" scoped>
.file-preview-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
    height: 70vh;
    overflow: hidden;
  }
}

.preview-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #909399;
}

.pdf-viewer {
  width: 100%;
  height: 100%;
  border: none;
}

.image-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  
  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
}

.video-viewer {
  max-width: 100%;
  max-height: 100%;
}

.audio-viewer {
  width: 80%;
}

.not-supported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #909399;
  
  p {
    margin: 0;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
