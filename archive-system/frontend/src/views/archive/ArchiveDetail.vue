<template>
  <div class="archive-detail">
    <div class="page-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="title">{{ archive?.title || '档案详情' }}</span>
          <el-tag v-if="archive" :type="getStatusType(archive.status)" class="ml-2">
            {{ getStatusName(archive.status) }}
          </el-tag>
        </template>
        <template #extra>
          <el-button-group>
            <el-button type="primary" @click="handleEdit" v-if="!isEditing">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="success" @click="handleSave" v-if="isEditing">
              <el-icon><Check /></el-icon>
              保存
            </el-button>
            <el-button @click="handleCancel" v-if="isEditing">
              取消
            </el-button>
          </el-button-group>
        </template>
      </el-page-header>
    </div>

    <el-skeleton :loading="loading" animated :rows="10">
      <template #default>
        <div class="content" v-if="archive">
          <!-- 基本信息 -->
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </div>
            </template>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="档案号">{{ archive.archiveNo }}</el-descriptions-item>
              <el-descriptions-item label="档案类型">
                <el-tag size="small">{{ getArchiveTypeName(archive.archiveType) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="保管期限">{{ getRetentionName(archive.retentionPeriod) }}</el-descriptions-item>
              <el-descriptions-item label="题名" :span="3">{{ archive.title }}</el-descriptions-item>
              <el-descriptions-item label="责任者">{{ archive.responsibility || '-' }}</el-descriptions-item>
              <el-descriptions-item label="文件日期">{{ archive.documentDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="密级">{{ getSecurityName(archive.securityLevel) }}</el-descriptions-item>
              <el-descriptions-item label="页数">{{ archive.pageCount || '-' }}</el-descriptions-item>
              <el-descriptions-item label="件数">{{ archive.piecesCount || 1 }}</el-descriptions-item>
              <el-descriptions-item label="文件数量">{{ archive.fileCount || 0 }} 个</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 业务关联 -->
          <el-card shadow="never" class="info-card" v-if="archive.caseNo || archive.clientName">
            <template #header>
              <div class="card-header">
                <el-icon><Briefcase /></el-icon>
                <span>业务关联</span>
              </div>
            </template>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="案件编号">{{ archive.caseNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="案件名称">{{ archive.caseName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="结案日期">{{ archive.caseCloseDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="委托人">{{ archive.clientName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="主办律师">{{ archive.lawyerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源">{{ getSourceName(archive.sourceType) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 电子文件 -->
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Folder /></el-icon>
                <span>电子文件</span>
                <el-tag class="ml-2" size="small">{{ files.length }} 个文件</el-tag>
              </div>
            </template>
            
            <el-upload
              v-if="isEditing"
              class="file-upload"
              drag
              :action="uploadUrl"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :show-file-list="false"
              multiple
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处，或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 PDF、Word、Excel、图片等格式，单文件不超过100MB
                </div>
              </template>
            </el-upload>

            <el-table :data="files" v-if="files.length > 0" class="file-table">
              <el-table-column type="index" width="50" />
              <el-table-column label="文件名" min-width="200">
                <template #default="{ row }">
                  <div class="file-name">
                    <el-icon :class="getFileIconClass(row.fileExtension)">
                      <component :is="getFileIcon(row.fileExtension)" />
                    </el-icon>
                    <span>{{ row.originalName || row.fileName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="fileSizeFormatted" label="大小" width="100" />
              <el-table-column prop="formatName" label="格式" width="150" />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handlePreview(row)">
                    预览
                  </el-button>
                  <el-button type="primary" link size="small" @click="handleDownload(row)">
                    下载
                  </el-button>
                  <el-button v-if="isEditing" type="danger" link size="small" @click="handleDeleteFile(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <el-empty v-else description="暂无电子文件" />
          </el-card>

          <!-- 操作记录 -->
          <el-card shadow="never" class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Clock /></el-icon>
                <span>操作记录</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="接收时间">{{ formatDateTime(archive.receivedAt) }}</el-descriptions-item>
              <el-descriptions-item label="接收人">{{ archive.receivedByName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="编目时间">{{ formatDateTime(archive.catalogedAt) }}</el-descriptions-item>
              <el-descriptions-item label="编目人">{{ archive.catalogedByName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归档时间">{{ formatDateTime(archive.archivedAt) }}</el-descriptions-item>
              <el-descriptions-item label="归档人">{{ archive.archivedByName || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </template>
    </el-skeleton>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewVisible" title="文件预览" width="80%" destroy-on-close>
      <iframe v-if="previewUrl" :src="previewUrl" class="preview-iframe" />
      <el-empty v-else description="该文件不支持预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Document, Edit, Check, Folder, Clock, Briefcase, 
  UploadFilled, Picture, VideoPlay, Headset, FolderOpened 
} from '@element-plus/icons-vue'
import { getArchiveDetail, getFileDownloadUrl, getFilePreviewUrl, deleteFile } from '@/api/archive'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const archive = ref(null)
const files = ref([])
const isEditing = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')

// 上传配置
const uploadUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${baseUrl}/archives/${route.params.id}/files`
})
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('accessToken')}`
}))

// 获取档案详情
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getArchiveDetail(route.params.id)
    archive.value = res.data
    files.value = res.data.files || []
  } catch (e) {
    console.error('获取档案详情失败', e)
    ElMessage.error('获取档案详情失败')
  } finally {
    loading.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/archives')
}

// 编辑
const handleEdit = () => {
  isEditing.value = true
}

// 保存
const handleSave = async () => {
  // TODO: 实现保存逻辑
  isEditing.value = false
  ElMessage.success('保存成功')
}

// 取消编辑
const handleCancel = () => {
  isEditing.value = false
}

// 上传成功
const handleUploadSuccess = (response) => {
  if (response.success) {
    ElMessage.success('上传成功')
    fetchData()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('上传失败')
}

// 预览文件
const handlePreview = async (file) => {
  try {
    const res = await getFilePreviewUrl(file.id)
    if (res.data?.url) {
      previewUrl.value = res.data.url
      previewVisible.value = true
    } else {
      ElMessage.warning('该文件不支持预览')
    }
  } catch (e) {
    console.error('获取预览链接失败', e)
  }
}

// 下载文件
const handleDownload = async (file) => {
  try {
    const res = await getFileDownloadUrl(file.id)
    if (res.data?.url) {
      window.open(res.data.url, '_blank')
    }
  } catch (e) {
    console.error('获取下载链接失败', e)
  }
}

// 删除文件
const handleDeleteFile = async (file) => {
  try {
    await ElMessageBox.confirm('确定要删除该文件吗？', '提示', {
      type: 'warning'
    })
    await deleteFile(file.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

// 格式化函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

const getArchiveTypeName = (type) => {
  const map = {
    DOCUMENT: '文书档案',
    SCIENCE: '科技档案',
    ACCOUNTING: '会计档案',
    PERSONNEL: '人事档案',
    SPECIAL: '专业档案',
    AUDIOVISUAL: '声像档案'
  }
  return map[type] || type
}

const getStatusName = (status) => {
  const map = {
    DRAFT: '草稿',
    RECEIVED: '已接收',
    CATALOGING: '整理中',
    STORED: '已归档',
    BORROWED: '借出中'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    RECEIVED: 'warning',
    CATALOGING: '',
    STORED: 'success',
    BORROWED: 'danger'
  }
  return map[status] || ''
}

const getRetentionName = (code) => {
  const map = {
    PERMANENT: '永久',
    Y30: '30年',
    Y15: '15年',
    Y10: '10年',
    Y5: '5年'
  }
  return map[code] || code
}

const getSecurityName = (level) => {
  const map = {
    PUBLIC: '公开',
    INTERNAL: '内部',
    CONFIDENTIAL: '秘密',
    SECRET: '机密'
  }
  return map[level] || level || '内部'
}

const getSourceName = (source) => {
  const map = {
    LAW_FIRM: '律所系统',
    MANUAL: '手动录入',
    IMPORT: '批量导入',
    TRANSFER: '移交'
  }
  return map[source] || source
}

const getFileIcon = (ext) => {
  if (!ext) return Document
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
  const videoExts = ['mp4', 'avi', 'mov', 'wmv']
  const audioExts = ['mp3', 'wav', 'flac']
  
  ext = ext.toLowerCase()
  if (imageExts.includes(ext)) return Picture
  if (videoExts.includes(ext)) return VideoPlay
  if (audioExts.includes(ext)) return Headset
  if (['zip', 'rar', '7z'].includes(ext)) return FolderOpened
  return Document
}

const getFileIconClass = (ext) => {
  if (!ext) return 'file-icon'
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp']
  ext = ext.toLowerCase()
  if (imageExts.includes(ext)) return 'file-icon file-icon-image'
  if (['pdf'].includes(ext)) return 'file-icon file-icon-pdf'
  if (['doc', 'docx'].includes(ext)) return 'file-icon file-icon-word'
  if (['xls', 'xlsx'].includes(ext)) return 'file-icon file-icon-excel'
  return 'file-icon'
}

onMounted(() => {
  fetchData()
  if (route.query.edit === 'true') {
    isEditing.value = true
  }
})
</script>

<style lang="scss" scoped>
.archive-detail {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  
  .title {
    font-size: 18px;
    font-weight: 600;
  }
  
  .ml-2 {
    margin-left: 8px;
  }
}

.content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
}

.file-upload {
  margin-bottom: 16px;
}

.file-table {
  margin-top: 16px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .file-icon {
    font-size: 18px;
    color: #909399;
    
    &-image { color: #67c23a; }
    &-pdf { color: #f56c6c; }
    &-word { color: #409eff; }
    &-excel { color: #67c23a; }
  }
}

.preview-iframe {
  width: 100%;
  height: 70vh;
  border: none;
}
</style>
