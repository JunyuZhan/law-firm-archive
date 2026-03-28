<template>
  <div class="push-record-list">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ statistics.total || 0 }}</div>
          <div class="stat-label">总推送数</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card stat-today">
          <div class="stat-value">{{ statistics.today || 0 }}</div>
          <div class="stat-label">今日推送</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card stat-pending">
          <div class="stat-value">{{ statistics.pending || 0 }}</div>
          <div class="stat-label">待处理</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card stat-success">
          <div class="stat-value">{{ statistics.success || 0 }}</div>
          <div class="stat-label">成功</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card stat-failed">
          <div class="stat-value">{{ statistics.failed || 0 }}</div>
          <div class="stat-label">失败</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card stat-partial">
          <div class="stat-value">{{ statistics.partial || 0 }}</div>
          <div class="stat-label">部分成功</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
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
            <el-option label="律所系统" value="LAW_FIRM" />
            <el-option label="手动上传" value="MANUAL" />
            <el-option label="数据迁移" value="IMPORT" />
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column prop="pushBatchNo" label="批次号" width="200" />
      <el-table-column prop="title" label="档案标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sourceType" label="来源类型" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'LAW_FIRM'" type="primary">律所系统</el-tag>
          <el-tag v-else-if="row.sourceType === 'MANUAL'" type="info">手动上传</el-tag>
          <el-tag v-else>{{ row.sourceType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="archiveNo" label="档案号" width="150" />
      <el-table-column prop="pushStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.pushStatus === 'SUCCESS'" type="success">成功</el-tag>
          <el-tag v-else-if="row.pushStatus === 'FAILED'" type="danger">失败</el-tag>
          <el-tag v-else-if="row.pushStatus === 'PENDING'" type="info">待处理</el-tag>
          <el-tag v-else-if="row.pushStatus === 'PROCESSING'" type="warning">处理中</el-tag>
          <el-tag v-else-if="row.pushStatus === 'PARTIAL'" type="warning">部分成功</el-tag>
          <el-tag v-else>{{ row.pushStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件进度" width="120">
        <template #default="{ row }">
          <span>{{ row.successFiles || 0 }}/{{ row.totalFiles || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="pushedAt" label="推送时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.pushedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">详情</el-button>
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

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="推送记录详情" width="700px">
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="批次号">{{ currentRecord.pushBatchNo }}</el-descriptions-item>
        <el-descriptions-item label="推送状态">
          <el-tag :type="getStatusType(currentRecord.pushStatus)">
            {{ getStatusText(currentRecord.pushStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="档案标题" :span="2">{{ currentRecord.title }}</el-descriptions-item>
        <el-descriptions-item label="来源类型">{{ currentRecord.sourceType }}</el-descriptions-item>
        <el-descriptions-item label="来源ID">{{ currentRecord.sourceId }}</el-descriptions-item>
        <el-descriptions-item label="档案号">{{ currentRecord.archiveNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="档案ID">{{ currentRecord.archiveId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件统计">
          成功 {{ currentRecord.successFiles || 0 }} / 总计 {{ currentRecord.totalFiles || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="推送时间">{{ formatDateTime(currentRecord.pushedAt) }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ formatDateTime(currentRecord.processedAt) }}</el-descriptions-item>
        <el-descriptions-item label="回调地址" :span="2">{{ currentRecord.callbackUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentRecord.errorMessage">
          <el-text type="danger">{{ currentRecord.errorMessage }}</el-text>
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

// 统计数据
const statistics = ref({})

// 搜索表单
const searchForm = reactive({
  keyword: '',
  sourceType: '',
  pushStatus: ''
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
    if (res.code === 200) {
      statistics.value = res.data
    }
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
      ...searchForm
    }
    const res = await getPushRecordList(params)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
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
  try {
    const res = await getPushRecordDetail(row.id)
    if (res.code === 200) {
      currentRecord.value = res.data
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }
}

// 重试
const handleRetry = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重试该推送记录吗？', '确认重试', {
      type: 'warning'
    })
    const res = await retryPushRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('已重置为待处理状态')
      handleRefresh()
    }
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

onMounted(() => {
  loadStatistics()
  loadData()
})
</script>

<style scoped>
.push-record-list {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-3px);
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
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
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
