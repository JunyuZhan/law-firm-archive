<template>
  <div class="borrow-link-list">
    <div class="page-header">
      <h2>借阅链接管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button @click="handleUpdateExpired">
          <el-icon><Timer /></el-icon>
          更新过期状态
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          <div class="stat-label">总链接数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-active">
          <div class="stat-value">{{ stats.activeCount || 0 }}</div>
          <div class="stat-label">有效链接</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-access">
          <div class="stat-value">{{ stats.totalAccessCount || 0 }}</div>
          <div class="stat-label">总访问次数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-download">
          <div class="stat-value">{{ stats.totalDownloadCount || 0 }}</div>
          <div class="stat-label">总下载次数</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索筛选 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="已撤销" value="REVOKED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 链接列表 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="linkList"
        stripe
        style="width: 100%"
      >
        <el-table-column label="档案号" width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="goToArchive(row.archiveId)">
              {{ row.archiveNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="借阅人" width="120" prop="sourceUserName" />
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'LAW_FIRM' ? 'warning' : ''">
              {{ row.sourceType === 'LAW_FIRM' ? '律所系统' : '内部' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="借阅目的" min-width="200" prop="borrowPurpose" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="过期时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.expireAt) }}
          </template>
        </el-table-column>
        <el-table-column label="访问/下载" width="100">
          <template #default="{ row }">
            {{ row.accessCount || 0 }} / {{ row.downloadCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleCopyLink(row)">
              <el-icon><Link /></el-icon>
              复制链接
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="danger"
              link
              @click="handleRevoke(row)"
            >
              <el-icon><Close /></el-icon>
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 撤销对话框 -->
    <el-dialog v-model="revokeDialogVisible" title="撤销链接" width="400px">
      <el-form>
        <el-form-item label="撤销原因">
          <el-input
            v-model="revokeReason"
            type="textarea"
            :rows="3"
            placeholder="请输入撤销原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmRevoke">确认撤销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh,
  Timer,
  Link,
  Close
} from '@element-plus/icons-vue'
import {
  getBorrowLinks,
  getLinkStats,
  revokeLink,
  updateExpiredLinks
} from '@/api/borrowLink'

const router = useRouter()

const loading = ref(false)
const linkList = ref([])
const stats = ref({})

const searchForm = reactive({
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const revokeDialogVisible = ref(false)
const revokeReason = ref('')
const revokingLink = ref(null)

onMounted(() => {
  loadData()
  loadStats()
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      status: searchForm.status || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const res = await getBorrowLinks(params)
    if (res.success) {
      linkList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载链接列表失败', error)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getLinkStats()
    if (res.success) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  searchForm.status = ''
  handleSearch()
}

function handleRefresh() {
  loadData()
  loadStats()
}

async function handleUpdateExpired() {
  try {
    const res = await updateExpiredLinks()
    if (res.success) {
      ElMessage.success(`已更新 ${res.data.updatedCount} 条过期链接`)
      loadData()
      loadStats()
    }
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

function handleCopyLink(row) {
  const url = `${window.location.origin}/borrow/access/${row.accessToken}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制')
    ElMessageBox.alert(url, '访问链接', { confirmButtonText: '关闭' })
  })
}

function handleRevoke(row) {
  revokingLink.value = row
  revokeReason.value = ''
  revokeDialogVisible.value = true
}

async function confirmRevoke() {
  if (!revokingLink.value) return
  
  try {
    const res = await revokeLink(revokingLink.value.id, revokeReason.value)
    if (res.success) {
      ElMessage.success('链接已撤销')
      revokeDialogVisible.value = false
      loadData()
      loadStats()
    }
  } catch (error) {
    ElMessage.error('撤销失败')
  }
}

function goToArchive(archiveId) {
  router.push(`/archives/${archiveId}`)
}

function getStatusType(status) {
  const types = {
    ACTIVE: 'success',
    EXPIRED: 'info',
    REVOKED: 'danger'
  }
  return types[status] || 'info'
}

function getStatusName(status) {
  const names = {
    ACTIVE: '有效',
    EXPIRED: '已过期',
    REVOKED: '已撤销'
  }
  return names[status] || status
}

function formatDateTime(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.borrow-link-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px 0;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-active .stat-value { color: #67c23a; }
.stat-access .stat-value { color: #409eff; }
.stat-download .stat-value { color: #e6a23c; }

.search-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
