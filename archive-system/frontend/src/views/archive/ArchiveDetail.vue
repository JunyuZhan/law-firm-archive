<template>
  <div class="archive-detail">
    <div class="page-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="title">{{ archive?.title || '档案详情' }}</span>
          <el-tag
            v-if="archive"
            :type="getStatusType(archive.status)"
            class="ml-2"
          >
            {{ getStatusName(archive.status) }}
          </el-tag>
        </template>
        <template #extra>
          <el-button-group>
            <el-button
              v-if="files.length > 0"
              type="success"
              :loading="isDownloading"
              @click="handleDownloadAll"
            >
              <el-icon><Download /></el-icon>
              打包下载
            </el-button>
            <el-button
              v-if="canBorrow"
              type="warning"
              @click="handleApplyBorrow"
            >
              <el-icon><Reading /></el-icon>
              申请借阅
            </el-button>
          </el-button-group>
        </template>
      </el-page-header>
    </div>

    <el-skeleton
      :loading="loading"
      animated
      :rows="10"
    >
      <template #default>
        <div
          v-if="archive"
          class="content"
        >
          <!-- 基本信息 -->
          <el-card
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </div>
            </template>
            <el-descriptions
              :column="3"
              border
            >
              <el-descriptions-item label="档案号">
                {{ archive.archiveNo }}
              </el-descriptions-item>
              <el-descriptions-item label="档案类型">
                <el-tag size="small">
                  {{ getArchiveTypeName(archive.archiveType) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="保管期限">
                {{ getRetentionName(archive.retentionPeriod) }}
              </el-descriptions-item>
              <el-descriptions-item
                label="题名"
                :span="3"
              >
                {{ archive.title }}
              </el-descriptions-item>
              <el-descriptions-item label="责任者">
                {{ archive.responsibility || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="文件日期">
                {{ archive.documentDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="密级">
                {{ getSecurityName(archive.securityLevel) }}
              </el-descriptions-item>
              <el-descriptions-item label="页数">
                {{ archive.pageCount || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="件数">
                {{ archive.piecesCount || 1 }}
              </el-descriptions-item>
              <el-descriptions-item label="文件数量">
                {{ archive.fileCount || 0 }} 个
              </el-descriptions-item>
              <el-descriptions-item label="档案形式">
                <el-tag :type="getArchiveFormType(archive.archiveForm)">
                  {{ getArchiveFormName(archive.archiveForm) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="archive.archiveForm !== 'ELECTRONIC'"
                label="存放位置"
              >
                {{ archive.storageLocation || '-' }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="archive.archiveForm !== 'ELECTRONIC' && archive.boxNo"
                label="盒号"
              >
                {{ archive.boxNo }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 业务关联 -->
          <el-card
            v-if="archive.caseNo || archive.clientName"
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Briefcase /></el-icon>
                <span>业务关联</span>
              </div>
            </template>
            <el-descriptions
              :column="3"
              border
            >
              <el-descriptions-item label="案件编号">
                {{ archive.caseNo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="案件名称">
                {{ archive.caseName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="结案日期">
                {{ archive.caseCloseDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="委托人">
                {{ archive.clientName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="主办律师">
                {{ archive.lawyerName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="来源">
                {{ getSourceName(archive.sourceType) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 电子文件 -->
          <el-card
            shadow="never"
            class="info-card files-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Folder /></el-icon>
                <span>电子文件</span>
                <el-tag
                  class="ml-2"
                  size="small"
                >
                  {{ files.length }} 个文件
                </el-tag>
              </div>
            </template>
            
            <!-- 按分类分组展示文件 -->
            <el-collapse
              v-if="groupedFiles.length > 0"
              v-model="activeCategories"
              class="file-categories"
            >
              <el-collapse-item
                v-for="group in groupedFiles"
                :key="group.category"
                :name="group.category"
              >
                <template #title>
                  <div class="category-header">
                    <span
                      class="category-icon"
                      :style="{ backgroundColor: group.color + '20', color: group.color }"
                    >
                      <el-icon v-if="group.category === 'COVER'"><Picture /></el-icon>
                      <el-icon v-else-if="group.category === 'CATALOG'"><Document /></el-icon>
                      <el-icon v-else-if="group.category === 'MAIN'"><Document /></el-icon>
                      <el-icon v-else><Folder /></el-icon>
                    </span>
                    <span class="category-name">{{ group.name }}</span>
                    <el-tag
                      size="small"
                      :color="group.color + '20'"
                      :style="{ color: group.color, borderColor: group.color }"
                    >
                      {{ group.files.length }} 个
                    </el-tag>
                  </div>
                </template>
                
                <div class="file-list">
                  <div
                    v-for="(file, index) in group.files"
                    :key="file.id"
                    class="file-item"
                  >
                    <div class="file-index">{{ index + 1 }}</div>
                    <div class="file-icon-wrapper">
                      <el-icon :class="getFileIconClass(file.fileExtension)">
                        <component :is="getFileIcon(file.fileExtension)" />
                      </el-icon>
                    </div>
                    <div class="file-info">
                      <div class="file-name-text">{{ file.originalName || file.fileName }}</div>
                      <div class="file-meta">
                        <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
                        <span
                          v-if="file.isLongTermFormat"
                          class="long-term-badge"
                        >长期保存格式</span>
                      </div>
                    </div>
                    <div class="file-actions">
                      <el-button
                        type="primary"
                        link
                        size="small"
                        @click="handlePreview(file)"
                      >
                        预览
                      </el-button>
                      <el-button
                        type="primary"
                        link
                        size="small"
                        @click="handleDownload(file)"
                      >
                        下载
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
            
            <el-empty
              v-else
              description="暂无电子文件"
            />
          </el-card>

          <!-- 操作记录 -->
          <el-card
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Clock /></el-icon>
                <span>操作记录</span>
              </div>
            </template>
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item label="接收时间">
                {{ formatDateTime(archive.receivedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="接收人">
                {{ archive.receivedByName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="编目时间">
                {{ formatDateTime(archive.catalogedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="编目人">
                {{ archive.catalogedByName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="归档时间">
                {{ formatDateTime(archive.archivedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="归档人">
                {{ archive.archivedByName || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </template>
    </el-skeleton>

    <!-- 文件预览组件 -->
    <FilePreview
      v-model="previewVisible"
      :file-id="previewFileId"
      :file-name="previewFileName"
      :file-extension="previewFileExtension"
    />

    <!-- 申请借阅弹窗 -->
    <el-dialog
      v-model="borrowDialogVisible"
      title="申请借阅"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="borrowFormRef"
        :model="borrowForm"
        :rules="borrowRules"
        label-width="100px"
      >
        <el-form-item label="档案信息">
          <div class="borrow-archive-info">
            <span class="archive-no">{{ archive?.archiveNo }}</span>
            <span class="archive-title">{{ archive?.title }}</span>
          </div>
        </el-form-item>
        <el-form-item
          label="借阅目的"
          prop="purpose"
        >
          <el-input 
            v-model="borrowForm.purpose" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入借阅目的"
          />
        </el-form-item>
        <el-form-item
          label="预计归还"
          prop="expectedReturnDate"
        >
          <el-date-picker
            v-model="borrowForm.expectedReturnDate"
            type="date"
            placeholder="选择预计归还日期"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="borrowForm.remarks" 
            type="textarea" 
            :rows="2" 
            placeholder="备注信息（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="borrowSubmitting"
          @click="submitBorrowApply"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, Folder, Clock, Briefcase, Download,
  Picture, VideoPlay, Headset, FolderOpened, Reading 
} from '@element-plus/icons-vue'
import { getArchiveDetail, getFileDownloadUrl, getArchiveDownloadUrl } from '@/api/archive'
import FilePreview from '@/components/FilePreview.vue'
import { checkBorrowAvailable, applyBorrow } from '@/api/borrow'
import {
  getArchiveTypeName,
  getStatusName,
  getStatusType,
  getRetentionName,
  getSecurityName,
  getSourceName,
  getArchiveFormName,
  getArchiveFormType,
  FILE_CATEGORY_ORDER,
  getFileCategoryName,
  getFileCategoryColor
} from '@/utils/archiveEnums'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const archive = ref(null)
const files = ref([])
const previewVisible = ref(false)
const previewFileId = ref(null)
const previewFileName = ref('')
const previewFileExtension = ref('')
const isDownloading = ref(false)

// 借阅相关
const canBorrow = ref(false)
const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const borrowFormRef = ref(null)
const borrowForm = ref({
  purpose: '',
  expectedReturnDate: null,
  remarks: ''
})
const borrowRules = {
  purpose: [{ required: true, message: '请输入借阅目的', trigger: 'blur' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还日期', trigger: 'change' }]
}

// 按分类分组的文件列表
const groupedFiles = computed(() => {
  const groups = {}
  
  // 初始化所有分类
  FILE_CATEGORY_ORDER.forEach(category => {
    groups[category] = []
  })
  // 添加"其他"分类用于未分类的文件
  groups['OTHER'] = []
  
  // 将文件分组
  files.value.forEach(file => {
    const category = file.fileCategory || 'MAIN' // 默认归类为正文
    if (groups[category]) {
      groups[category].push(file)
    } else {
      groups['OTHER'].push(file)
    }
  })
  
  // 返回有文件的分类，按顺序排列
  const result = []
  FILE_CATEGORY_ORDER.forEach(category => {
    if (groups[category].length > 0) {
      result.push({
        category,
        name: getFileCategoryName(category),
        color: getFileCategoryColor(category),
        files: groups[category]
      })
    }
  })
  // 如果有"其他"分类的文件，添加到最后
  if (groups['OTHER'].length > 0) {
    result.push({
      category: 'OTHER',
      name: '其他',
      color: '#909399',
      files: groups['OTHER']
    })
  }
  
  return result
})

// 默认展开的分类
const activeCategories = computed(() => {
  return groupedFiles.value.map(g => g.category)
})

// 获取档案详情
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getArchiveDetail(route.params.id)
    archive.value = res.data
    files.value = res.data.files || []
    // 检查是否可借阅
    checkCanBorrow()
  } catch (e) {
    console.error('获取档案详情失败', e)
    ElMessage.error('获取档案详情失败')
  } finally {
    loading.value = false
  }
}

// 检查档案是否可借阅
const checkCanBorrow = async () => {
  try {
    const res = await checkBorrowAvailable(route.params.id)
    canBorrow.value = res.data?.available === true
  } catch (e) {
    canBorrow.value = false
  }
}

// 申请借阅
const handleApplyBorrow = () => {
  borrowForm.value = {
    purpose: '',
    expectedReturnDate: null,
    remarks: ''
  }
  borrowDialogVisible.value = true
}

// 禁用过去日期
const disablePastDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 提交借阅申请
const submitBorrowApply = async () => {
  if (!borrowFormRef.value) return
  
  try {
    await borrowFormRef.value.validate()
    borrowSubmitting.value = true
    
    const data = {
      archiveId: route.params.id,
      borrowPurpose: borrowForm.value.purpose,
      expectedReturnDate: borrowForm.value.expectedReturnDate.toISOString().split('T')[0],
      remarks: borrowForm.value.remarks
    }
    
    await applyBorrow(data)
    ElMessage.success('借阅申请已提交，请等待审批')
    borrowDialogVisible.value = false
    // 刷新数据，更新可借阅状态
    checkCanBorrow()
  } catch (e) {
    if (e !== 'cancel' && e !== false) {
      console.error('提交借阅申请失败', e)
      ElMessage.error(e.response?.data?.message || '提交申请失败')
    }
  } finally {
    borrowSubmitting.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/archives')
}

// 打包下载所有文件
const handleDownloadAll = async () => {
  if (files.value.length === 0) {
    ElMessage.warning('该档案暂无文件可下载')
    return
  }
  
  isDownloading.value = true
  try {
    const res = await getArchiveDownloadUrl(route.params.id)
    if (res.data?.url) {
      // 打开下载链接
      window.open(res.data.url, '_blank')
      ElMessage.success('开始下载档案文件包')
    } else {
      ElMessage.error('获取下载链接失败')
    }
  } catch (e) {
    console.error('打包下载失败', e)
    ElMessage.error(e.response?.data?.message || '打包下载失败')
  } finally {
    isDownloading.value = false
  }
}

// 预览文件
const handlePreview = (file) => {
  previewFileId.value = file.id
  previewFileName.value = file.originalName || file.fileName
  previewFileExtension.value = file.fileExtension || ''
  previewVisible.value = true
}

// 下载文件
const handleDownload = async (file) => {
  try {
    const res = await getFileDownloadUrl(file.id)
    if (res.data?.url) {
      window.open(res.data.url, '_blank')
    } else {
      ElMessage.error('获取下载链接失败')
    }
  } catch (e) {
    console.error('获取下载链接失败', e)
    ElMessage.error(e.response?.data?.message || '获取下载链接失败')
  }
}

// 格式化函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
// 注：getArchiveTypeName, getStatusName, getStatusType, getRetentionName, 
// getSecurityName, getSourceName 已从 archiveEnums.js 导入

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

.files-card {
  :deep(.el-card__body) {
    padding: 16px;
  }
}

// 文件分类折叠面板
.file-categories {
  border: none;
  
  :deep(.el-collapse-item__header) {
    height: 52px;
    background: #fafafa;
    border-radius: 6px;
    margin-bottom: 8px;
    padding: 0 16px;
    border: 1px solid #ebeef5;
    
    &:hover {
      background: #f5f7fa;
    }
  }
  
  :deep(.el-collapse-item__wrap) {
    border: none;
  }
  
  :deep(.el-collapse-item__content) {
    padding: 0 0 16px 0;
  }
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  
  .category-icon {
    width: 32px;
    height: 32px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
  }
  
  .category-name {
    font-weight: 500;
    font-size: 15px;
    color: #303133;
  }
}

// 文件列表
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 8px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.2s;
  
  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
    
    .file-actions {
      opacity: 1;
    }
  }
  
  .file-index {
    width: 24px;
    height: 24px;
    background: #f0f2f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #909399;
    margin-right: 12px;
    flex-shrink: 0;
  }
  
  .file-icon-wrapper {
    width: 40px;
    height: 40px;
    background: #f5f7fa;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    flex-shrink: 0;
    
    .file-icon {
      font-size: 20px;
      color: #909399;
      
      &-image { color: #67c23a; }
      &-pdf { color: #f56c6c; }
      &-word { color: #409eff; }
      &-excel { color: #67c23a; }
    }
  }
  
  .file-info {
    flex: 1;
    min-width: 0;
    
    .file-name-text {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 4px;
    }
    
    .file-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .file-size {
        font-size: 12px;
        color: #909399;
      }
      
      .long-term-badge {
        font-size: 11px;
        color: #67c23a;
        background: #f0f9eb;
        padding: 2px 6px;
        border-radius: 4px;
      }
    }
  }
  
  .file-actions {
    display: flex;
    gap: 4px;
    opacity: 0.6;
    transition: opacity 0.2s;
  }
}

.preview-iframe {
  width: 100%;
  height: 70vh;
  border: none;
}

.borrow-archive-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .archive-no {
    font-size: 12px;
    color: #909399;
  }
  
  .archive-title {
    font-weight: 500;
    color: #303133;
  }
}
</style>
