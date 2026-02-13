<template>
  <el-dialog
    v-model="visible"
    :title="fileName"
    width="90%"
    destroy-on-close
    top="3vh"
    class="file-preview-dialog"
    :fullscreen="isFullscreen"
    @close="handleClose"
  >
    <div class="preview-container" ref="containerRef">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-wrapper">
        <el-icon class="is-loading" :size="48"><Loading /></el-icon>
        <p>加载中...</p>
      </div>

      <!-- PDF预览 -->
      <div v-else-if="previewType === 'pdf'" class="pdf-viewer-wrapper">
        <iframe
          :src="pdfViewerUrl"
          class="pdf-viewer"
        />
      </div>

      <!-- 图片预览 -->
      <div v-else-if="previewType === 'image'" class="image-viewer" @wheel.prevent="handleWheel">
        <div 
          class="image-wrapper"
          :style="imageTransformStyle"
          @mousedown="handleMouseDown"
        >
          <img
            ref="imageRef"
            :src="previewUrl"
            :alt="fileName"
            @load="handleImageLoad"
            @error="handleError"
            draggable="false"
          />
        </div>
        
        <!-- 图片工具栏 -->
        <div class="image-toolbar">
          <el-button-group>
            <el-tooltip content="放大" placement="top">
              <el-button :icon="ZoomIn" @click="zoomIn" />
            </el-tooltip>
            <el-tooltip content="缩小" placement="top">
              <el-button :icon="ZoomOut" @click="zoomOut" />
            </el-tooltip>
            <el-tooltip content="原始大小" placement="top">
              <el-button @click="resetZoom">1:1</el-button>
            </el-tooltip>
            <el-tooltip content="适应窗口" placement="top">
              <el-button :icon="FullScreen" @click="fitToWindow" />
            </el-tooltip>
          </el-button-group>
          <el-button-group style="margin-left: 8px">
            <el-tooltip content="向左旋转" placement="top">
              <el-button :icon="RefreshLeft" @click="rotateLeft" />
            </el-tooltip>
            <el-tooltip content="向右旋转" placement="top">
              <el-button :icon="RefreshRight" @click="rotateRight" />
            </el-tooltip>
          </el-button-group>
          <span class="zoom-info">{{ Math.round(imageScale * 100) }}%</span>
        </div>
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

      <!-- Office文档提示 -->
      <div v-else-if="previewType === 'office'" class="office-preview">
        <el-icon :size="64"><Document /></el-icon>
        <p>Office文档预览</p>
        <p class="tip">请下载后使用本地软件打开，或使用在线Office服务</p>
        <el-button type="primary" @click="handleDownload">
          <el-icon><Download /></el-icon>
          下载文件
        </el-button>
      </div>

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
        <el-button :icon="isFullscreen ? 'Minus' : 'FullScreen'" @click="toggleFullscreen">
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </el-button>
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
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Loading, Document, Download, 
  ZoomIn, ZoomOut, FullScreen, RefreshLeft, RefreshRight 
} from '@element-plus/icons-vue'
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
  },
  // 直接传入URL（不调用API）
  url: {
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
const containerRef = ref(null)
const imageRef = ref(null)
const isFullscreen = ref(false)

// 图片操作状态
const imageScale = ref(1)
const imageRotate = ref(0)
const imagePosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })

// 根据扩展名判断预览类型
const previewType = computed(() => {
  const ext = props.fileExtension?.toLowerCase() || ''
  
  if (ext === 'pdf' || ext === 'ofd') return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'tif', 'tiff'].includes(ext)) return 'image'
  if (['mp4', 'webm', 'ogg', 'mov'].includes(ext)) return 'video'
  if (['mp3', 'wav', 'ogg', 'flac', 'm4a'].includes(ext)) return 'audio'
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) return 'office'
  
  return 'unsupported'
})

// PDF 预览 URL（使用浏览器内置或 PDF.js）
const pdfViewerUrl = computed(() => {
  if (!previewUrl.value) return ''
  // 使用浏览器内置 PDF 查看器
  return previewUrl.value
})

// 图片变换样式
const imageTransformStyle = computed(() => ({
  transform: `translate(${imagePosition.value.x}px, ${imagePosition.value.y}px) scale(${imageScale.value}) rotate(${imageRotate.value}deg)`,
  transition: isDragging.value ? 'none' : 'transform 0.2s ease'
}))

// 监听fileId变化
watch(() => props.fileId, async (newVal) => {
  if (newVal && props.modelValue) {
    await loadPreview()
  }
}, { immediate: true })

// 监听URL变化
watch(() => props.url, (newVal) => {
  if (newVal) {
    previewUrl.value = newVal
    downloadUrl.value = newVal
  }
}, { immediate: true })

// 监听弹窗打开
watch(visible, async (newVal) => {
  if (newVal) {
    if (props.url) {
      previewUrl.value = props.url
      downloadUrl.value = props.url
    } else if (props.fileId) {
      await loadPreview()
    }
    // 重置图片状态
    resetImageState()
  } else {
    if (!props.url) {
      previewUrl.value = ''
      downloadUrl.value = ''
    }
  }
})

// 加载预览
const loadPreview = async () => {
  if (!props.fileId) return
  
  loading.value = true
  try {
    // 获取预览URL
    const previewRes = await getFilePreviewUrl(props.fileId)
    previewUrl.value = previewRes.data || ''
    
    // 获取下载URL
    const downloadRes = await getFileDownloadUrl(props.fileId)
    downloadUrl.value = downloadRes.data || ''
  } catch (e) {
    console.error('加载预览失败', e)
    ElMessage.error('加载预览失败')
  } finally {
    loading.value = false
  }
}

// 重置图片状态
const resetImageState = () => {
  imageScale.value = 1
  imageRotate.value = 0
  imagePosition.value = { x: 0, y: 0 }
}

// 图片操作
const zoomIn = () => {
  imageScale.value = Math.min(imageScale.value * 1.25, 5)
}

const zoomOut = () => {
  imageScale.value = Math.max(imageScale.value / 1.25, 0.1)
}

const resetZoom = () => {
  imageScale.value = 1
  imagePosition.value = { x: 0, y: 0 }
}

const fitToWindow = () => {
  if (!imageRef.value || !containerRef.value) return
  
  const img = imageRef.value
  const container = containerRef.value
  const containerRect = container.getBoundingClientRect()
  
  const scaleX = (containerRect.width - 100) / img.naturalWidth
  const scaleY = (containerRect.height - 150) / img.naturalHeight
  
  imageScale.value = Math.min(scaleX, scaleY, 1)
  imagePosition.value = { x: 0, y: 0 }
}

const rotateLeft = () => {
  imageRotate.value -= 90
}

const rotateRight = () => {
  imageRotate.value += 90
}

// 滚轮缩放
const handleWheel = (e) => {
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

// 拖拽操作
const handleMouseDown = (e) => {
  if (imageScale.value <= 1) return
  
  isDragging.value = true
  dragStart.value = {
    x: e.clientX - imagePosition.value.x,
    y: e.clientY - imagePosition.value.y
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const handleMouseMove = (e) => {
  if (!isDragging.value) return
  
  imagePosition.value = {
    x: e.clientX - dragStart.value.x,
    y: e.clientY - dragStart.value.y
  }
}

const handleMouseUp = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
}

// 全屏切换
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
}

const handleImageLoad = () => {
  loading.value = false
  // 自动适应窗口
  setTimeout(fitToWindow, 100)
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

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})
</script>

<style lang="scss" scoped>
.file-preview-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
    height: 75vh;
    overflow: hidden;
  }
  
  &.is-fullscreen {
    :deep(.el-dialog__body) {
      height: calc(100vh - 120px);
    }
  }
}

.preview-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a1a;
  position: relative;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #909399;
}

.pdf-viewer-wrapper {
  width: 100%;
  height: 100%;
  
  .pdf-viewer {
    width: 100%;
    height: 100%;
    border: none;
    background: #fff;
  }
}

.image-viewer {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  
  .image-wrapper {
    cursor: grab;
    user-select: none;
    
    &:active {
      cursor: grabbing;
    }
    
    img {
      display: block;
      max-width: none;
      max-height: none;
    }
  }
  
  .image-toolbar {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.6);
    padding: 8px 16px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
    z-index: 10;
    
    :deep(.el-button) {
      background: transparent;
      border-color: rgba(255, 255, 255, 0.3);
      color: #fff;
      
      &:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(255, 255, 255, 0.5);
      }
    }
    
    .zoom-info {
      color: #fff;
      font-size: 12px;
      min-width: 50px;
      text-align: center;
    }
  }
}

.video-viewer {
  max-width: 100%;
  max-height: 100%;
  background: #000;
}

.audio-viewer {
  width: 80%;
  max-width: 500px;
}

.office-preview,
.not-supported {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #909399;
  padding: 40px;
  
  p {
    margin: 0;
    text-align: center;
  }
  
  .tip {
    font-size: 12px;
    color: #c0c4cc;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
