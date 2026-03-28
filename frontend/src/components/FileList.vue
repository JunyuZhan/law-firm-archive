<template>
  <div class="file-list-component">
    <!-- 文件列表头部 -->
    <div
      v-if="showHeader"
      class="file-list-header"
    >
      <span class="file-count">共 {{ files.length }} 个文件</span>
      <div class="view-toggle">
        <el-button-group size="small">
          <el-button
            :type="viewMode === 'list' ? 'primary' : 'default'"
            @click="viewMode = 'list'"
          >
            <el-icon><List /></el-icon>
          </el-button>
          <el-button
            :type="viewMode === 'grid' ? 'primary' : 'default'"
            @click="viewMode = 'grid'"
          >
            <el-icon><Grid /></el-icon>
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="files.length === 0"
      description="暂无文件"
    />

    <!-- 列表视图 -->
    <div
      v-else-if="viewMode === 'list'"
      class="file-list-view"
    >
      <div 
        v-for="file in files" 
        :key="file.id" 
        class="file-item"
        :class="{ 'is-selected': selectedIds.includes(file.id) }"
        @click="handleSelect(file)"
      >
        <div
          class="file-icon"
          :class="getFileIconClass(file)"
        >
          <el-icon :size="32">
            <component :is="getFileIcon(file)" />
          </el-icon>
        </div>
        <div class="file-info">
          <div
            class="file-name"
            :title="file.originalName || file.fileName"
          >
            {{ file.originalName || file.fileName }}
          </div>
          <div class="file-meta">
            <span class="file-size">{{ formatSize(file.fileSize) }}</span>
            <span
              v-if="file.uploadAt"
              class="file-date"
            >{{ formatDate(file.uploadAt) }}</span>
          </div>
        </div>
        <div class="file-actions">
          <el-button 
            v-if="canPreview(file)" 
            type="primary" 
            link 
            size="small"
            @click.stop="handlePreview(file)"
          >
            <el-icon><View /></el-icon>
            预览
          </el-button>
          <el-button 
            type="primary" 
            link 
            size="small"
            @click.stop="handleDownload(file)"
          >
            <el-icon><Download /></el-icon>
            下载
          </el-button>
          <el-button 
            v-if="showDelete" 
            type="danger" 
            link 
            size="small"
            @click.stop="handleDelete(file)"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 网格视图 -->
    <div
      v-else
      class="file-grid-view"
    >
      <div 
        v-for="file in files" 
        :key="file.id" 
        class="file-card"
        :class="{ 'is-selected': selectedIds.includes(file.id) }"
        @click="handleSelect(file)"
        @dblclick="handlePreview(file)"
      >
        <div class="file-card-preview">
          <!-- 图片缩略图 -->
          <img 
            v-if="isImage(file) && file.thumbnailUrl" 
            :src="file.thumbnailUrl" 
            :alt="file.originalName"
          >
          <!-- 文件图标 -->
          <div
            v-else
            class="file-card-icon"
            :class="getFileIconClass(file)"
          >
            <el-icon :size="48">
              <component :is="getFileIcon(file)" />
            </el-icon>
          </div>
        </div>
        <div class="file-card-info">
          <div
            class="file-name"
            :title="file.originalName || file.fileName"
          >
            {{ file.originalName || file.fileName }}
          </div>
          <div class="file-size">
            {{ formatSize(file.fileSize) }}
          </div>
        </div>
        <div class="file-card-actions">
          <el-button 
            v-if="canPreview(file)" 
            :icon="View" 
            circle 
            size="small"
            @click.stop="handlePreview(file)"
          />
          <el-button 
            :icon="Download" 
            circle 
            size="small"
            @click.stop="handleDownload(file)"
          />
          <el-button 
            v-if="showDelete" 
            :icon="Delete" 
            circle 
            size="small" 
            type="danger"
            @click.stop="handleDelete(file)"
          />
        </div>
      </div>
    </div>

    <!-- 文件预览弹窗 -->
    <FilePreview
      v-model="previewVisible"
      :file-id="previewFile?.id"
      :file-name="previewFile?.originalName || previewFile?.fileName"
      :file-extension="previewFile?.fileExtension"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Document, Picture, VideoPlay, Folder, Headset,
  View, Download, Delete, List, Grid 
} from '@element-plus/icons-vue'
import FilePreview from './FilePreview.vue'
import { getFileDownloadUrl, deleteFile } from '@/api/archive'

const props = defineProps({
  // 文件列表
  files: {
    type: Array,
    default: () => []
  },
  // 是否显示头部
  showHeader: {
    type: Boolean,
    default: true
  },
  // 是否显示删除按钮
  showDelete: {
    type: Boolean,
    default: false
  },
  // 是否可多选
  selectable: {
    type: Boolean,
    default: false
  },
  // 已选中的文件ID列表
  selected: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:selected', 'delete', 'preview', 'download'])

const viewMode = ref('list')
const previewVisible = ref(false)
const previewFile = ref(null)

// 已选中的ID列表
const selectedIds = computed({
  get: () => props.selected,
  set: (val) => emit('update:selected', val)
})

// 判断是否为图片
const isImage = (file) => {
  const ext = (file.fileExtension || '').toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)
}

// 判断是否可预览
const canPreview = (file) => {
  const ext = (file.fileExtension || '').toLowerCase()
  const previewable = ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'mp4', 'webm', 'mp3', 'wav', 'ofd']
  return previewable.includes(ext)
}

// 获取文件图标组件
const getFileIcon = (file) => {
  const ext = (file.fileExtension || '').toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'tif', 'tiff'].includes(ext)) {
    return Picture
  }
  if (['mp4', 'avi', 'mov', 'wmv', 'webm'].includes(ext)) {
    return VideoPlay
  }
  if (['mp3', 'wav', 'ogg', 'flac', 'm4a'].includes(ext)) {
    return Headset
  }
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) {
    return Folder
  }
  return Document
}

// 获取文件图标样式类
const getFileIconClass = (file) => {
  const ext = (file.fileExtension || '').toLowerCase()
  
  if (ext === 'pdf') return 'icon-pdf'
  if (['doc', 'docx'].includes(ext)) return 'icon-word'
  if (['xls', 'xlsx'].includes(ext)) return 'icon-excel'
  if (['ppt', 'pptx'].includes(ext)) return 'icon-ppt'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return 'icon-image'
  if (['mp4', 'avi', 'mov'].includes(ext)) return 'icon-video'
  if (['mp3', 'wav', 'ogg'].includes(ext)) return 'icon-audio'
  if (['zip', 'rar', '7z'].includes(ext)) return 'icon-archive'
  return 'icon-default'
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 选择文件
const handleSelect = (file) => {
  if (!props.selectable) return
  
  const idx = selectedIds.value.indexOf(file.id)
  if (idx > -1) {
    selectedIds.value = selectedIds.value.filter(id => id !== file.id)
  } else {
    selectedIds.value = [...selectedIds.value, file.id]
  }
}

// 预览文件
const handlePreview = (file) => {
  if (!canPreview(file)) {
    ElMessage.info('该文件类型不支持预览')
    return
  }
  previewFile.value = file
  previewVisible.value = true
  emit('preview', file)
}

// 下载文件
const handleDownload = async (file) => {
  try {
    const res = await getFileDownloadUrl(file.id)
    if (res.data) {
      window.open(res.data, '_blank')
    }
    emit('download', file)
  } catch (e) {
    console.error('获取下载链接失败', e)
    ElMessage.error('获取下载链接失败')
  }
}

// 删除文件
const handleDelete = async (file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalName || file.fileName}" 吗？`,
      '删除确认',
      { type: 'warning' }
    )
    
    await deleteFile(file.id)
    ElMessage.success('删除成功')
    emit('delete', file)
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }
}

// 暴露方法
defineExpose({
  handlePreview,
  handleDownload
})
</script>

<style lang="scss" scoped>
.file-list-component {
  .file-list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .file-count {
      color: #909399;
      font-size: 14px;
    }
  }
}

// 列表视图
.file-list-view {
  .file-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    margin-bottom: 8px;
    transition: all 0.3s;
    cursor: default;
    
    &:hover {
      background-color: #f5f7fa;
      border-color: #409eff;
    }
    
    &.is-selected {
      background-color: #ecf5ff;
      border-color: #409eff;
    }
    
    .file-icon {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      margin-right: 16px;
      
      &.icon-pdf { background: #fff2f0; color: #f5222d; }
      &.icon-word { background: #e6f7ff; color: #1890ff; }
      &.icon-excel { background: #f6ffed; color: #52c41a; }
      &.icon-ppt { background: #fff7e6; color: #fa8c16; }
      &.icon-image { background: #f9f0ff; color: #722ed1; }
      &.icon-video { background: #fff0f6; color: #eb2f96; }
      &.icon-audio { background: #e6fffb; color: #13c2c2; }
      &.icon-archive { background: #fffbe6; color: #faad14; }
      &.icon-default { background: #f0f0f0; color: #8c8c8c; }
    }
    
    .file-info {
      flex: 1;
      min-width: 0;
      
      .file-name {
        font-size: 14px;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-bottom: 4px;
      }
      
      .file-meta {
        font-size: 12px;
        color: #909399;
        
        .file-size {
          margin-right: 16px;
        }
      }
    }
    
    .file-actions {
      display: flex;
      gap: 8px;
      opacity: 0;
      transition: opacity 0.3s;
    }
    
    &:hover .file-actions {
      opacity: 1;
    }
  }
}

// 网格视图
.file-grid-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  
  .file-card {
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
    transition: all 0.3s;
    cursor: default;
    
    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      border-color: #409eff;
      
      .file-card-actions {
        opacity: 1;
      }
    }
    
    &.is-selected {
      border-color: #409eff;
      background-color: #ecf5ff;
    }
    
    .file-card-preview {
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      position: relative;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      .file-card-icon {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        
        &.icon-pdf { color: #f5222d; }
        &.icon-word { color: #1890ff; }
        &.icon-excel { color: #52c41a; }
        &.icon-ppt { color: #fa8c16; }
        &.icon-image { color: #722ed1; }
        &.icon-video { color: #eb2f96; }
        &.icon-audio { color: #13c2c2; }
        &.icon-archive { color: #faad14; }
        &.icon-default { color: #8c8c8c; }
      }
    }
    
    .file-card-info {
      padding: 12px;
      
      .file-name {
        font-size: 13px;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        margin-bottom: 4px;
      }
      
      .file-size {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .file-card-actions {
      display: flex;
      justify-content: center;
      gap: 8px;
      padding: 8px;
      border-top: 1px solid #ebeef5;
      opacity: 0;
      transition: opacity 0.3s;
    }
  }
}
</style>
