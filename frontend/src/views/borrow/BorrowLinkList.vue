<template>
  <div class="borrow-link-list">
    <div class="page-header">
      <div>
        <h1>借阅链接管理</h1>
        <p>集中查看外发借阅链接的有效期、访问情况和撤销状态，便于在线查阅过程可控可追踪。</p>
      </div>
      <div class="header-actions">
        <el-button
          type="primary"
          @click="handleRefresh"
        >
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          v-if="userStore.isAdmin"
          @click="handleUpdateExpired"
        >
          <el-icon><Timer /></el-icon>
          更新过期状态
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-value">
            {{ stats.totalCount || 0 }}
          </div>
          <div class="stat-label">
            总链接数
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-active"
        >
          <div class="stat-value">
            {{ stats.activeCount || 0 }}
          </div>
          <div class="stat-label">
            有效链接
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-access"
        >
          <div class="stat-value">
            {{ stats.totalAccessCount || 0 }}
          </div>
          <div class="stat-label">
            总访问次数
          </div>
        </el-card>
      </div>
      <div class="stats-grid-item">
        <el-card
          shadow="never"
          class="stat-card stat-download"
        >
          <div class="stat-value">
            {{ stats.totalDownloadCount || 0 }}
          </div>
          <div class="stat-label">
            总下载次数
          </div>
        </el-card>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <el-card
      shadow="never"
      class="search-card"
    >
      <el-form
        :inline="true"
        :model="searchForm"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            clearable
            placeholder="档案号/借阅人/来源/用途"
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            clearable
            placeholder="全部"
            style="width: 120px"
          >
            <el-option
              label="有效"
              value="ACTIVE"
            />
            <el-option
              label="已过期"
              value="EXPIRED"
            />
            <el-option
              label="已撤销"
              value="REVOKED"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="下载权限">
          <el-select
            v-model="searchForm.allowDownload"
            clearable
            placeholder="全部"
            style="width: 140px"
          >
            <el-option
              label="允许下载"
              :value="true"
            />
            <el-option
              label="仅在线查阅"
              :value="false"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select
            v-model="searchForm.sourceType"
            clearable
            placeholder="全部"
            style="width: 140px"
          >
            <el-option
              label="内部申请"
              value="INTERNAL"
            />
            <el-option
              label="律所系统"
              value="LAW_FIRM"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 链接列表 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <el-table
        v-loading="loading"
        :data="linkList"
        stripe
        style="width: 100%"
      >
        <el-table-column
          label="档案号"
          width="150"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              @click="goToArchive(row.archiveId)"
            >
              {{ row.archiveNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="借阅人"
          width="120"
          prop="sourceUserName"
        />
        <el-table-column
          label="来源"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.sourceType === 'LAW_FIRM' ? 'warning' : ''"
            >
              {{ row.sourceType === 'LAW_FIRM' ? '律所系统' : '内部' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="借阅目的"
          min-width="200"
          prop="borrowPurpose"
          show-overflow-tooltip
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="下载权限"
          width="110"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.allowDownload ? 'success' : 'info'"
              size="small"
            >
              {{ row.allowDownload ? '允许' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="访问上限"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            {{ row.maxAccessCount || '不限' }}
          </template>
        </el-table-column>
        <el-table-column
          label="过期时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.expireAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="访问/下载"
          width="100"
        >
          <template #default="{ row }">
            {{ row.accessCount || 0 }} / {{ row.downloadCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column
          label="最后访问"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.lastAccessAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="访问IP"
          width="140"
          prop="lastAccessIp"
          show-overflow-tooltip
        />
        <el-table-column
          label="创建时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleCopyLink(row)"
            >
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
    <el-dialog
      v-model="revokeDialogVisible"
      title="撤销链接"
      width="400px"
    >
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
        <el-button @click="revokeDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="danger"
          @click="confirmRevoke"
        >
          确认撤销
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
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
const userStore = useUserStore()

const loading = ref(false)
const linkList = ref([])
const stats = ref({})

const searchForm = reactive({
  status: '',
  allowDownload: '',
  sourceType: '',
  keyword: ''
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
      allowDownload: searchForm.allowDownload === '' ? undefined : searchForm.allowDownload,
      sourceType: searchForm.sourceType || undefined,
      keyword: searchForm.keyword || undefined,
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
  searchForm.allowDownload = ''
  searchForm.sourceType = ''
  searchForm.keyword = ''
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.header-actions {
  display: flex;
  gap: 12px;
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

.stat-active .stat-value { color: #67c23a; }
.stat-access .stat-value { color: #409eff; }
.stat-download .stat-value { color: #e6a23c; }

.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
