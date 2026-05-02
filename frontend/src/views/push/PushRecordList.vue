<template>
  <div class="push-record-list">
    <div class="page-header">
      <h1>推送记录</h1>
      <p>查看档案向来源系统或内部来源的推送结果、失败原因和重试情况，便于排查同步链路。</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-value">
            {{ statistics.total || 0 }}
          </div>
          <div class="stat-label">
            总推送数
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-today"
        >
          <div class="stat-value">
            {{ statistics.today || 0 }}
          </div>
          <div class="stat-label">
            今日推送
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-pending"
        >
          <div class="stat-value">
            {{ statistics.pending || 0 }}
          </div>
          <div class="stat-label">
            待处理
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-success"
        >
          <div class="stat-value">
            {{ statistics.success || 0 }}
          </div>
          <div class="stat-label">
            成功
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-failed"
        >
          <div class="stat-value">
            {{ statistics.failed || 0 }}
          </div>
          <div class="stat-label">
            失败
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-partial"
        >
          <div class="stat-value">
            {{ statistics.partial || 0 }}
          </div>
          <div class="stat-label">
            部分成功
          </div>
        </el-card>
      </div>
    </div>

    <!-- 搜索区域 -->
    <el-card
      class="search-card"
      shadow="never"
    >
      <el-form
        :model="searchForm"
        inline
      >
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="标题/来源ID/档案号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select
            v-model="searchForm.sourceType"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in sourceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="推送状态">
          <el-select
            v-model="searchForm.pushStatus"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in pushStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input
            v-model="searchForm.pushBatchNo"
            placeholder="推送批次号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="推送时间">
          <el-date-picker
            v-model="searchForm.pushedAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card
      shadow="never"
      class="table-card"
    >
      <template #header>
        <div class="card-header">
          <span>推送列表</span>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column
          prop="pushBatchNo"
          label="批次号"
          width="200"
        />
        <el-table-column
          prop="title"
          label="档案标题"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="sourceType"
          label="来源类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getSourceTypeTag(row.sourceType)">
              {{ getSourceTypeLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="archiveNo"
          label="档案号"
          width="150"
        />
        <el-table-column
          prop="pushStatus"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.pushStatus === 'SUCCESS'"
              type="success"
            >
              成功
            </el-tag>
            <el-tag
              v-else-if="row.pushStatus === 'FAILED'"
              type="danger"
            >
              失败
            </el-tag>
            <el-tag
              v-else-if="row.pushStatus === 'PENDING'"
              type="info"
            >
              待处理
            </el-tag>
            <el-tag
              v-else-if="row.pushStatus === 'PROCESSING'"
              type="warning"
            >
              处理中
            </el-tag>
            <el-tag
              v-else-if="row.pushStatus === 'PARTIAL'"
              type="warning"
            >
              部分成功
            </el-tag>
            <el-tag v-else>
              {{ row.pushStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="文件进度"
          width="120"
        >
          <template #default="{ row }">
            <span>{{ row.successFiles || 0 }}/{{ row.totalFiles || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="pushedAt"
          label="推送时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.pushedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleView(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.pushStatus === 'FAILED'"
              link
              type="warning"
              @click="handleRetry(row)"
            >
              重试
            </el-button>
            <el-button
              v-if="row.archiveId"
              link
              type="success"
              @click="goToArchive(row)"
            >
              查看档案
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="推送记录详情"
      width="700px"
    >
      <el-descriptions
        v-if="currentRecord"
        :column="2"
        border
      >
        <el-descriptions-item label="批次号">
          {{ currentRecord.pushBatchNo }}
        </el-descriptions-item>
        <el-descriptions-item label="推送状态">
          <el-tag :type="getStatusType(currentRecord.pushStatus)">
            {{ getStatusText(currentRecord.pushStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          label="档案标题"
          :span="2"
        >
          {{ currentRecord.title }}
        </el-descriptions-item>
        <el-descriptions-item label="来源类型">
          {{ getSourceTypeLabel(currentRecord.sourceType) }}
        </el-descriptions-item>
        <el-descriptions-item label="来源ID">
          {{ currentRecord.sourceId }}
        </el-descriptions-item>
        <el-descriptions-item label="档案号">
          {{ currentRecord.archiveNo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="档案ID">
          {{ currentRecord.archiveId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="文件统计">
          成功 {{ currentRecord.successFiles || 0 }} / 总计 {{ currentRecord.totalFiles || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="推送时间">
          {{ formatDateTime(currentRecord.pushedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间">
          {{ formatDateTime(currentRecord.processedAt) }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentRecord.errorMessage"
          label="错误信息"
          :span="2"
        >
          <el-text type="danger">
            {{ currentRecord.errorMessage }}
          </el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  getPushRecordList,
  getPushRecordStatistics,
  getPushRecordDetail,
  retryPushRecord
} from '@/api/pushRecord'
import {
  getPushStatusName,
  getPushStatusType,
  getPushStatusOptions
} from '@/utils/archiveEnums'

const router = useRouter()

// 下拉选项
const pushStatusOptions = getPushStatusOptions()
const sourceTypeOptions = [
  { value: 'LAW_FIRM', label: '律所系统' },
  { value: 'MANUAL', label: '手动录入' },
  { value: 'IMPORT', label: '批量导入' },
  { value: 'TRANSFER', label: '移交' }
]

// 统计数据
const statistics = ref({})

// 搜索表单
const searchForm = reactive({
  keyword: '',
  sourceType: '',
  pushStatus: '',
  pushBatchNo: '',
  pushedAtRange: null
})

// 表格数据
const loading = ref(false)
const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 详情对话框
const detailVisible = ref(false)
const currentRecord = ref(null)

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getPushRecordStatistics()
    statistics.value = res.data || {}
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载列表数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      sourceType: searchForm.sourceType,
      pushStatus: searchForm.pushStatus,
      pushBatchNo: searchForm.pushBatchNo
    }
    if (searchForm.pushedAtRange?.length === 2) {
      params.pushedAtStart = searchForm.pushedAtRange[0]
      params.pushedAtEnd = searchForm.pushedAtRange[1]
    } else {
      delete params.pushedAtRange
    }
    const res = await getPushRecordList(params)
    tableData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.sourceType = ''
  searchForm.pushStatus = ''
  searchForm.pushBatchNo = ''
  searchForm.pushedAtRange = null
  handleSearch()
}

// 刷新
const handleRefresh = () => {
  loadStatistics()
  loadData()
}

// 分页
const handleSizeChange = (val) => {
  pagination.pageSize = val
  loadData()
}

const handlePageChange = (val) => {
  pagination.pageNum = val
  loadData()
}

// 查看详情
const handleView = async (row) => {
  currentRecord.value = null
  try {
    const res = await getPushRecordDetail(row.id)
    currentRecord.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

// 重试
const handleRetry = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重试该推送记录吗？', '确认重试', {
      type: 'warning'
    })
    await retryPushRecord(row.id)
    ElMessage.success('已重置为待处理状态')
    handleRefresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重试失败')
    }
  }
}

// 跳转到档案详情
const goToArchive = (row) => {
  if (row.archiveId) {
    router.push(`/archives/${row.archiveId}`)
  }
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 注：getPushStatusType, getPushStatusName 已从 archiveEnums.js 导入
const getStatusType = getPushStatusType
const getStatusText = getPushStatusName

const getSourceTypeLabel = (sourceType) => {
  const match = sourceTypeOptions.find(item => item.value === sourceType)
  return match?.label || sourceType || '-'
}

const getSourceTypeTag = (sourceType) => {
  const map = {
    LAW_FIRM: 'primary',
    MANUAL: 'info',
    IMPORT: 'warning',
    TRANSFER: 'success'
  }
  return map[sourceType] || ''
}

onMounted(() => {
  loadStatistics()
  loadData()
})
</script>

<style scoped>
.push-record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(128px, 1fr));
  gap: 12px;
}

.stat-card,
.search-card,
.table-card {
  border-radius: 10px;
}

.stat-card {
  border-radius: 10px;

  :deep(.el-card__body) {
    min-height: 88px;
    padding: 16px 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    text-align: center;
  }
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.stat-today .stat-value {
  color: #409EFF;
}

.stat-pending .stat-value {
  color: #909399;
}

.stat-success .stat-value {
  color: #67C23A;
}

.stat-failed .stat-value {
  color: #F56C6C;
}

.stat-partial .stat-value {
  color: #E6A23C;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
