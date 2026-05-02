<template>
  <div class="borrow-access">
    <!-- 水印层 -->
    <div
      v-if="isValid && borrowerInfo"
      class="watermark-layer"
    >
      <div class="watermark-text">
        {{ borrowerInfo.userName }} · {{ formatDate(new Date()) }}
      </div>
    </div>

    <!-- 加载中 -->
    <div
      v-if="loading"
      class="loading-container"
    >
      <el-icon
        class="is-loading"
        :size="48"
      >
        <Loading />
      </el-icon>
      <p>正在加载档案信息...</p>
    </div>

    <!-- 无效链接 -->
    <div
      v-else-if="!isValid"
      class="invalid-container"
    >
      <el-result
        icon="warning"
        :title="invalidReason || '链接无效'"
        sub-title="该借阅链接已失效，无法访问档案"
      >
        <template #extra>
          <el-button @click="handleClose">
            关闭页面
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- 档案内容 -->
    <div
      v-else
      class="content-container"
    >
      <!-- 顶部信息栏 -->
      <div class="header-bar">
        <div class="archive-title">
          <el-icon><Document /></el-icon>
          <span>{{ archiveInfo?.title }}</span>
        </div>
        <div class="expire-info">
          <el-tag
            v-if="linkInfo?.remainingSeconds > 0"
            type="warning"
            effect="plain"
          >
            <el-icon><Clock /></el-icon>
            剩余时间: {{ formatRemainingTime(linkInfo.remainingSeconds) }}
          </el-tag>
          <el-tag
            v-else
            type="danger"
          >
            链接即将过期
          </el-tag>
        </div>
      </div>

      <!-- 档案基本信息 -->
      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            <span>档案信息</span>
          </div>
        </template>
        <el-descriptions
          :column="3"
          border
        >
          <el-descriptions-item label="档案号">
            {{ archiveInfo?.archiveNo }}
          </el-descriptions-item>
          <el-descriptions-item label="档案类型">
            <el-tag size="small">
              {{ getArchiveTypeName(archiveInfo?.archiveType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="密级">
            {{ getSecurityName(archiveInfo?.securityLevel) }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="archiveInfo?.caseName"
            label="案件名称"
          >
            {{ archiveInfo.caseName }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="archiveInfo?.caseNo"
            label="案件编号"
          >
            {{ archiveInfo.caseNo }}
          </el-descriptions-item>
          <el-descriptions-item label="文件数量">
            {{ archiveInfo?.fileCount || 0 }} 个
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 借阅信息 -->
      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <el-icon><User /></el-icon>
            <span>借阅信息</span>
          </div>
        </template>
        <el-descriptions
          :column="3"
          border
        >
          <el-descriptions-item label="借阅人">
            {{ borrowerInfo?.userName }}
          </el-descriptions-item>
          <el-descriptions-item label="借阅目的">
            {{ borrowerInfo?.purpose }}
          </el-descriptions-item>
          <el-descriptions-item label="借阅方式">
            {{ getBorrowTypeName(borrowerInfo?.borrowType) }}
          </el-descriptions-item>
          <el-descriptions-item label="应还日期">
            {{ formatDateValue(borrowerInfo?.expectedReturnDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="访问次数">
            {{ linkInfo?.accessCount || 0 }}
            <span v-if="linkInfo?.maxAccessCount"> / {{ linkInfo.maxAccessCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下载权限">
            <el-tag
              :type="linkInfo?.allowDownload ? 'success' : 'info'"
              size="small"
            >
              {{ linkInfo?.allowDownload ? '允许下载' : '仅在线查阅' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="失效时间">
            {{ formatDateTime(linkInfo?.expireAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 文件列表 -->
      <el-card
        shadow="never"
        class="files-card"
      >
        <template #header>
          <div class="card-header">
            <el-icon><Folder /></el-icon>
            <span>电子文件</span>
            <el-tag
              class="ml-2"
              size="small"
              type="info"
            >
              {{ files.length }} 个文件
            </el-tag>
          </div>
        </template>

        <el-table
          :data="files"
          stripe
          style="width: 100%"
        >
          <el-table-column
            label="文件名"
            min-width="300"
          >
            <template #default="{ row }">
              <div class="file-name">
                <el-icon :class="getFileIconClass(row.fileExtension)">
                  <component :is="getFileIcon(row.fileExtension)" />
                </el-icon>
                <span>{{ row.fileName }}</span>
                <el-tag
                  v-if="row.isLongTermFormat"
                  size="small"
                  type="success"
                  class="ml-1"
                >
                  长期保存
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            label="类型"
            width="100"
          >
            <template #default="{ row }">
              {{ row.fileExtension?.toUpperCase() || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            label="大小"
            width="120"
          >
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="200"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                @click="handlePreview(row)"
              >
                <el-icon><View /></el-icon>
                预览
              </el-button>
              <el-button
                v-if="linkInfo?.allowDownload"
                type="success"
                link
                @click="handleDownload(row)"
              >
                <el-icon><Download /></el-icon>
                下载
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog
      v-model="previewVisible"
      :title="previewFile?.fileName"
      width="90%"
      top="5vh"
      destroy-on-close
    >
      <div class="preview-container">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          class="preview-frame"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document,
  Loading,
  Clock,
  InfoFilled,
  User,
  Folder,
  View,
  Download,
  Picture,
  Document as DocumentIcon,
  Folder as FolderIcon
} from '@element-plus/icons-vue'
import {
  accessArchive,
  getBorrowFileDownloadUrl,
  getBorrowFilePreviewUrl,
  recordDownload
} from '@/api/borrowLink'
import { ARCHIVE_TYPES, SECURITY_LEVELS } from '@/utils/archiveEnums'

const route = useRoute()
const BORROW_TYPES = {
  ONLINE: '在线查阅',
  DOWNLOAD: '允许下载',
  COPY: '复制利用'
}

const loading = ref(true)
const isValid = ref(false)
const invalidReason = ref('')
const archiveInfo = ref(null)
const files = ref([])
const linkInfo = ref(null)
const borrowerInfo = ref(null)

const previewVisible = ref(false)
const previewFile = ref(null)
const previewUrl = ref('')

const token = computed(() => route.params.token)

onMounted(async () => {
  await loadArchiveData()
})

watch(token, async (newToken, oldToken) => {
  if (newToken && newToken !== oldToken) {
    await loadArchiveData()
  }
})

function resetArchiveState() {
  isValid.value = false
  archiveInfo.value = null
  files.value = []
  linkInfo.value = null
  borrowerInfo.value = null
  previewVisible.value = false
  previewFile.value = null
  previewUrl.value = ''
}

async function loadArchiveData() {
  resetArchiveState()
  loading.value = true

  if (!token.value) {
    invalidReason.value = '缺少访问令牌'
    loading.value = false
    return
  }

  try {
    const res = await accessArchive(token.value)
    if (res.data?.valid) {
      isValid.value = true
      invalidReason.value = ''
      archiveInfo.value = res.data.archive
      files.value = res.data.files || []
      linkInfo.value = res.data.linkInfo
      borrowerInfo.value = res.data.borrower
    } else {
      invalidReason.value = res.data?.invalidReason || res.message || '链接无效'
    }
  } catch (error) {
    console.error('加载档案数据失败', error)
    invalidReason.value = error?.response?.data?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function handlePreview(file) {
  previewFile.value = file
  loadPreviewUrl(file)
}

async function handleDownload(file) {
  if (!linkInfo.value?.allowDownload) {
    ElMessage.warning('该文件不允许下载')
    return
  }

  try {
    const res = await getBorrowFileDownloadUrl(token.value, file.fileId)
    const downloadUrl = res?.data?.url
    if (!downloadUrl) {
      ElMessage.warning(res.message || '该文件不允许下载')
      return
    }

    window.open(downloadUrl, '_blank')

    try {
      await recordDownload(token.value, file.fileId)
    } catch (recordError) {
      console.error('记录下载日志失败', recordError)
      ElMessage.warning('下载已开始，但记录下载日志失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '下载失败')
  }
}

function handleClose() {
  window.close()
}

function getArchiveTypeName(type) {
  return ARCHIVE_TYPES[type] || type || '-'
}

function getSecurityName(level) {
  return SECURITY_LEVELS[level] || level || '内部'
}

function getBorrowTypeName(type) {
  return BORROW_TYPES[type] || type || '在线查阅'
}

function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function formatRemainingTime(seconds) {
  if (!seconds || seconds <= 0) return '已过期'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
}

function formatDate(date) {
  return date.toLocaleDateString('zh-CN')
}

function formatDateValue(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadPreviewUrl(file) {
  try {
    const res = await getBorrowFilePreviewUrl(token.value, file.fileId)
    if (!res.data?.url) {
      ElMessage.warning(res.message || '该文件暂不支持预览')
      return
    }
    previewUrl.value = res.data.url
    previewVisible.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '预览失败')
  }
}

function getFileIcon(extension) {
  const ext = extension?.toLowerCase()
  if (['pdf'].includes(ext)) return DocumentIcon
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return Picture
  if (['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(ext)) return DocumentIcon
  return FolderIcon
}

function getFileIconClass(extension) {
  const ext = extension?.toLowerCase()
  if (['pdf'].includes(ext)) return 'icon-pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) return 'icon-image'
  if (['doc', 'docx'].includes(ext)) return 'icon-word'
  if (['xls', 'xlsx'].includes(ext)) return 'icon-excel'
  if (['ppt', 'pptx'].includes(ext)) return 'icon-ppt'
  return 'icon-file'
}
</script>

<style scoped>
.borrow-access {
  min-height: 100vh;
  background-color: #f5f7fa;
  position: relative;
}

.watermark-layer {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1000;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-around;
  align-content: space-around;
  transform: rotate(-30deg);
  opacity: 0.08;
}

.watermark-text {
  font-size: 16px;
  color: #333;
  white-space: nowrap;
  padding: 50px 100px;
}

.loading-container,
.invalid-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
}

.loading-container .el-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.archive-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.expire-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-card,
.files-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-pdf { color: #e94747; }
.icon-image { color: #67c23a; }
.icon-word { color: #409eff; }
.icon-excel { color: #67c23a; }
.icon-ppt { color: #e6a23c; }
.icon-file { color: #909399; }

.preview-container {
  height: 70vh;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.ml-1 { margin-left: 4px; }
.ml-2 { margin-left: 8px; }
</style>
