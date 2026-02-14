<template>
  <div class="batch-upload">
    <!-- 上传区域 -->
    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      multiple
      :accept="acceptTypes"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileAdd"
      :disabled="uploading"
    >
      <el-icon
        class="el-icon--upload"
        :size="48"
      >
        <UploadFilled />
      </el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或 <em>点击选择文件</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 {{ acceptTip }}，单文件最大 {{ formatSize(maxFileSize) }}
        </div>
      </template>
    </el-upload>

    <!-- 文件队列 -->
    <div
      v-if="fileQueue.length > 0"
      class="file-queue"
    >
      <div class="queue-header">
        <span class="queue-title">
          上传队列 ({{ uploadedCount }}/{{ fileQueue.length }})
        </span>
        <div class="queue-actions">
          <el-button 
            v-if="!uploading && pendingCount > 0" 
            type="primary" 
            size="small"
            @click="startUpload"
          >
            开始上传
          </el-button>
          <el-button 
            v-if="uploading" 
            type="warning" 
            size="small"
            @click="pauseUpload"
          >
            暂停
          </el-button>
          <el-button 
            v-if="completedCount > 0"
            size="small"
            @click="clearCompleted"
          >
            清除已完成
          </el-button>
          <el-button 
            type="danger" 
            size="small" 
            text
            @click="clearAll"
          >
            清空
          </el-button>
        </div>
      </div>

      <!-- 总进度 -->
      <div
        v-if="uploading || uploadedCount > 0"
        class="total-progress"
      >
        <el-progress 
          :percentage="totalProgress" 
          :status="allCompleted ? 'success' : ''"
          :stroke-width="10"
        />
      </div>

      <!-- 文件列表 -->
      <div class="file-list">
        <div 
          v-for="(file, index) in fileQueue" 
          :key="file.id" 
          class="file-item"
          :class="{ 'is-error': file.status === 'error' }"
        >
          <div class="file-info">
            <el-icon
              class="file-icon"
              :class="getFileIconClass(file.name)"
            >
              <component :is="getFileIcon(file.name)" />
            </el-icon>
            <div class="file-details">
              <span
                class="file-name"
                :title="file.name"
              >{{ file.name }}</span>
              <span class="file-size">{{ formatSize(file.size) }}</span>
            </div>
          </div>
          
          <div class="file-status">
            <template v-if="file.status === 'pending'">
              <span class="status-text">等待上传</span>
            </template>
            <template v-else-if="file.status === 'uploading'">
              <el-progress 
                :percentage="file.progress" 
                :stroke-width="6" 
                style="width: 120px"
              />
            </template>
            <template v-else-if="file.status === 'success'">
              <el-icon class="status-icon success">
                <CircleCheck />
              </el-icon>
              <span class="status-text success">上传成功</span>
            </template>
            <template v-else-if="file.status === 'error'">
              <el-icon class="status-icon error">
                <CircleClose />
              </el-icon>
              <el-tooltip
                :content="file.error || '上传失败'"
                placement="top"
              >
                <span class="status-text error">上传失败</span>
              </el-tooltip>
            </template>
          </div>

          <div class="file-actions">
            <el-button 
              v-if="file.status === 'error'"
              type="primary"
              size="small"
              link
              @click="retryUpload(index)"
            >
              重试
            </el-button>
            <el-button 
              type="danger"
              size="small"
              link
              :disabled="file.status === 'uploading'"
              @click="removeFile(index)"
            >
              移除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  UploadFilled, 
  Document, 
  Picture, 
  VideoPlay,
  Folder,
  CircleCheck, 
  CircleClose 
} from '@element-plus/icons-vue'
import { uploadFile } from '@/api/archive'

const props = defineProps({
  // 允许的文件类型（扩展名数组）
  allowedTypes: {
    type: Array,
    default: () => ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'jpg', 'jpeg', 'png', 'gif', 'zip', 'rar']
  },
  // 最大文件大小（字节）
  maxFileSize: {
    type: Number,
    default: 100 * 1024 * 1024 // 100MB
  },
  // 最大文件数
  maxFiles: {
    type: Number,
    default: 20
  },
  // 并发上传数
  concurrent: {
    type: Number,
    default: 3
  },
  // 自动开始上传
  autoUpload: {
    type: Boolean,
    default: false
  },
  // 文件分类
  fileCategory: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['change', 'success', 'error', 'complete'])

const uploadRef = ref(null)
const fileQueue = ref([])
const uploading = ref(false)
const paused = ref(false)

// 计算属性
const acceptTypes = computed(() => {
  return props.allowedTypes.map(t => `.${t}`).join(',')
})

const acceptTip = computed(() => {
  const types = props.allowedTypes.slice(0, 5).map(t => t.toUpperCase())
  if (props.allowedTypes.length > 5) {
    types.push('等')
  }
  return types.join('、')
})

const pendingCount = computed(() => 
  fileQueue.value.filter(f => f.status === 'pending').length
)

const uploadingCount = computed(() => 
  fileQueue.value.filter(f => f.status === 'uploading').length
)

const uploadedCount = computed(() => 
  fileQueue.value.filter(f => f.status === 'success').length
)

const errorCount = computed(() => 
  fileQueue.value.filter(f => f.status === 'error').length
)

const completedCount = computed(() => uploadedCount.value + errorCount.value)

const allCompleted = computed(() => 
  fileQueue.value.length > 0 && pendingCount.value === 0 && uploadingCount.value === 0
)

const totalProgress = computed(() => {
  if (fileQueue.value.length === 0) return 0
  const total = fileQueue.value.reduce((sum, f) => sum + f.progress, 0)
  return Math.round(total / fileQueue.value.length)
})

// 已上传的文件ID列表
const uploadedFileIds = computed(() => 
  fileQueue.value
    .filter(f => f.status === 'success' && f.fileId)
    .map(f => f.fileId)
)

// 监听变化，通知父组件
watch(uploadedFileIds, (ids) => {
  emit('change', ids)
}, { deep: true })

// 添加文件
const handleFileAdd = (uploadFile) => {
  const file = uploadFile.raw
  
  // 验证文件数量
  if (fileQueue.value.length >= props.maxFiles) {
    ElMessage.warning(`最多只能上传 ${props.maxFiles} 个文件`)
    return
  }
  
  // 验证文件大小
  if (file.size > props.maxFileSize) {
    ElMessage.warning(`文件 ${file.name} 超出大小限制`)
    return
  }
  
  // 验证文件类型
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!props.allowedTypes.includes(ext)) {
    ElMessage.warning(`不支持的文件类型: ${ext}`)
    return
  }
  
  // 检查重复
  if (fileQueue.value.some(f => f.name === file.name && f.size === file.size)) {
    ElMessage.warning(`文件 ${file.name} 已存在`)
    return
  }
  
  // 添加到队列
  fileQueue.value.push({
    id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    file,
    name: file.name,
    size: file.size,
    type: file.type,
    status: 'pending', // pending, uploading, success, error
    progress: 0,
    error: null,
    fileId: null
  })
  
  // 自动上传
  if (props.autoUpload && !uploading.value) {
    startUpload()
  }
}

// 开始上传
const startUpload = async () => {
  if (uploading.value) return
  
  uploading.value = true
  paused.value = false
  
  await processQueue()
}

// 处理队列
const processQueue = async () => {
  while (!paused.value) {
    // 获取待上传的文件
    const pendingFiles = fileQueue.value.filter(f => f.status === 'pending')
    if (pendingFiles.length === 0) break
    
    // 获取当前正在上传的数量
    const currentUploading = fileQueue.value.filter(f => f.status === 'uploading').length
    const availableSlots = props.concurrent - currentUploading
    
    if (availableSlots <= 0) {
      // 等待有空位
      await new Promise(resolve => setTimeout(resolve, 100))
      continue
    }
    
    // 取出可上传的文件
    const filesToUpload = pendingFiles.slice(0, availableSlots)
    
    // 并发上传
    await Promise.all(filesToUpload.map(fileItem => uploadSingleFile(fileItem)))
  }
  
  uploading.value = false
  
  // 检查是否全部完成
  if (allCompleted.value) {
    emit('complete', {
      success: uploadedCount.value,
      error: errorCount.value,
      fileIds: uploadedFileIds.value
    })
  }
}

// 上传单个文件
const uploadSingleFile = async (fileItem) => {
  fileItem.status = 'uploading'
  fileItem.progress = 0
  
  try {
    const res = await uploadFile(
      fileItem.file, 
      null, 
      props.fileCategory,
      (progressEvent) => {
        if (progressEvent.total) {
          fileItem.progress = Math.round((progressEvent.loaded / progressEvent.total) * 100)
        }
      }
    )
    
    fileItem.status = 'success'
    fileItem.progress = 100
    fileItem.fileId = res.data?.id
    
    emit('success', { file: fileItem, response: res })
    
  } catch (error) {
    fileItem.status = 'error'
    fileItem.error = error.message || '上传失败'
    
    emit('error', { file: fileItem, error })
  }
}

// 暂停上传
const pauseUpload = () => {
  paused.value = true
}

// 重试上传
const retryUpload = (index) => {
  const file = fileQueue.value[index]
  if (file) {
    file.status = 'pending'
    file.progress = 0
    file.error = null
    
    if (!uploading.value) {
      startUpload()
    }
  }
}

// 移除文件
const removeFile = (index) => {
  fileQueue.value.splice(index, 1)
}

// 清除已完成
const clearCompleted = () => {
  fileQueue.value = fileQueue.value.filter(f => 
    f.status === 'pending' || f.status === 'uploading'
  )
}

// 清空全部
const clearAll = () => {
  if (uploading.value) {
    paused.value = true
  }
  fileQueue.value = []
}

// 获取文件图标
const getFileIcon = (filename) => {
  const ext = filename.split('.').pop()?.toLowerCase()
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) {
    return Picture
  }
  if (['mp4', 'avi', 'mov', 'wmv'].includes(ext)) {
    return VideoPlay
  }
  if (['zip', 'rar', '7z'].includes(ext)) {
    return Folder
  }
  return Document
}

// 获取文件图标样式类
const getFileIconClass = (filename) => {
  const ext = filename.split('.').pop()?.toLowerCase()
  if (['pdf'].includes(ext)) return 'icon-pdf'
  if (['doc', 'docx'].includes(ext)) return 'icon-word'
  if (['xls', 'xlsx'].includes(ext)) return 'icon-excel'
  if (['ppt', 'pptx'].includes(ext)) return 'icon-ppt'
  if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) return 'icon-image'
  return ''
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 暴露方法和属性给父组件
defineExpose({
  fileQueue,
  uploadedFileIds,
  startUpload,
  clearAll,
  clearCompleted
})
</script>

<style lang="scss" scoped>
.batch-upload {
  .upload-area {
    :deep(.el-upload-dragger) {
      width: 100%;
      padding: 30px;
    }
  }
}

.file-queue {
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  
  .queue-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #ebeef5;
    
    .queue-title {
      font-weight: 500;
      color: #303133;
    }
    
    .queue-actions {
      display: flex;
      gap: 8px;
    }
  }
  
  .total-progress {
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;
  }
  
  .file-list {
    max-height: 400px;
    overflow-y: auto;
  }
  
  .file-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #ebeef5;
    transition: background-color 0.3s;
    
    &:last-child {
      border-bottom: none;
    }
    
    &:hover {
      background-color: #f5f7fa;
    }
    
    &.is-error {
      background-color: #fef0f0;
    }
    
    .file-info {
      flex: 1;
      display: flex;
      align-items: center;
      min-width: 0;
      
      .file-icon {
        font-size: 24px;
        margin-right: 12px;
        color: #909399;
        
        &.icon-pdf { color: #f56c6c; }
        &.icon-word { color: #409eff; }
        &.icon-excel { color: #67c23a; }
        &.icon-ppt { color: #e6a23c; }
        &.icon-image { color: #909399; }
      }
      
      .file-details {
        display: flex;
        flex-direction: column;
        min-width: 0;
        
        .file-name {
          font-size: 14px;
          color: #303133;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .file-size {
          font-size: 12px;
          color: #909399;
          margin-top: 2px;
        }
      }
    }
    
    .file-status {
      display: flex;
      align-items: center;
      margin: 0 16px;
      min-width: 120px;
      
      .status-icon {
        font-size: 16px;
        margin-right: 4px;
        
        &.success { color: #67c23a; }
        &.error { color: #f56c6c; }
      }
      
      .status-text {
        font-size: 12px;
        color: #909399;
        
        &.success { color: #67c23a; }
        &.error { color: #f56c6c; }
      }
    }
    
    .file-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
