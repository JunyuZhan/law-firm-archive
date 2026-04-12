<template>
  <div class="archive-list">
    <div class="page-header">
      <h1>档案列表</h1>
      <p>按档案号、题名、来源和状态快速筛选电子档案，支持直接进入详情、接收和后续流转。</p>
    </div>

    <el-alert
      title="业务提示"
      type="info"
      :closable="false"
      show-icon
    >
      <template #default>
        普通用户提交的档案会先进入待审核。审核通过后才会正式入库，已接收或整理中的档案仍需完成后续归档动作后才能借阅。
      </template>
    </el-alert>

    <el-alert
      v-if="route.query.created === '1'"
      title="档案已创建完成"
      type="success"
      :closable="false"
      show-icon
    >
      <template #default>
        {{ route.query.submitted === '1' ? '入库申请已提交，当前进入待审核状态。' : '新建档案已保存。建议继续检查全宗、分类、电子文件和纸质位置，并尽快执行正式归档。' }}
      </template>
    </el-alert>

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
            placeholder="档案号/题名/案件编号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="档案类型">
          <el-select
            v-model="searchForm.archiveType"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in archiveTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="档案形式">
          <el-select
            v-model="searchForm.archiveForm"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in archiveFormOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="保管期限">
          <el-select
            v-model="searchForm.retentionPeriod"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in retentionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密级">
          <el-select
            v-model="searchForm.securityLevel"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in securityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select
            v-model="searchForm.sourceType"
            placeholder="全部"
            clearable
            style="width: 130px"
          >
            <el-option
              v-for="item in sourceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="扫描批次">
          <el-input
            v-model="searchForm.scanBatchNo"
            placeholder="如 SCAN-20260330-01"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="归档日期">
          <el-date-picker
            v-model="searchForm.archiveDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
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

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button
        type="primary"
        @click="handleCreate"
      >
        <el-icon><Plus /></el-icon>
        新建档案
      </el-button>
      <el-button @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column
          prop="archiveNo"
          label="档案号"
          width="180"
          fixed
        />
        <el-table-column
          prop="fondsNo"
          label="全宗"
          width="120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.fondsNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="categoryCode"
          label="分类"
          width="140"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.categoryCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="题名"
          min-width="250"
          show-overflow-tooltip
        />
        <el-table-column
          prop="archiveType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ getArchiveTypeName(row.archiveType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="archiveForm"
          label="形式"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="getArchiveFormType(row.archiveForm)"
            >
              {{ getArchiveFormName(row.archiveForm) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="retentionPeriod"
          label="保管期限"
          width="100"
        >
          <template #default="{ row }">
            {{ getRetentionName(row.retentionPeriod) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="fileCount"
          label="文件数"
          width="80"
          align="center"
        />
        <el-table-column
          prop="sourceType"
          label="来源"
          width="100"
        >
          <template #default="{ row }">
            {{ getSourceName(row.sourceType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="receivedAt"
          label="接收时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.receivedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="210"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="canApprove(row)"
              type="success"
              link
              size="small"
              :loading="approvingId === row.id"
              @click.stop="handleApprove(row)"
            >
              审核通过
            </el-button>
            <el-button
              v-if="canStore(row)"
              type="success"
              link
              size="small"
              :loading="storingId === row.id"
              @click.stop="handleStore(row)"
            >
              归档
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="canManageArchive"
              type="primary"
              link
              size="small"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              v-if="canManageArchive"
              title="确定要删除该档案吗？"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  type="danger"
                  link
                  size="small"
                  @click.stop
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArchiveList, deleteArchive, updateArchiveStatus, approveArchive } from '@/api/archive'
import { useUserStore } from '@/stores/user'
import { 
  getArchiveTypeName, 
  getArchiveFormName,
  getArchiveFormType,
  getStatusName, 
  getStatusType, 
  getRetentionName,
  getArchiveTypeOptions,
  getArchiveFormOptions,
  getRetentionOptions,
  getSecurityOptions,
  ARCHIVE_STATUS,
  SOURCE_TYPES
} from '@/utils/archiveEnums'
import { MANAGER_ROLES, REVIEW_ROLES } from '@/utils/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 下拉选项
const archiveTypeOptions = getArchiveTypeOptions()
const archiveFormOptions = getArchiveFormOptions()
const retentionOptions = getRetentionOptions()
const securityOptions = getSecurityOptions()
const sourceTypeOptions = Object.entries(SOURCE_TYPES).map(([value, label]) => ({ value, label }))
const statusOptions = Object.entries(ARCHIVE_STATUS)
  .filter(([key]) => ['PENDING_REVIEW', 'RECEIVED', 'CATALOGING', 'STORED', 'BORROWED', 'REJECTED'].includes(key))
  .map(([value, label]) => ({ value, label }))
const loading = ref(false)
const tableData = ref([])
const storingId = ref(null)
const approvingId = ref(null)
const canManageArchive = computed(() => MANAGER_ROLES.includes(userStore.userType))
const canReviewArchive = computed(() => REVIEW_ROLES.includes(userStore.userType))

const searchForm = reactive({
  keyword: '',
  archiveType: '',
  archiveForm: '',
  retentionPeriod: '',
  securityLevel: '',
  sourceType: '',
  status: '',
  scanBatchNo: '',
  archiveDateRange: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.keyword,
      archiveType: searchForm.archiveType,
      archiveForm: searchForm.archiveForm,
      retentionPeriod: searchForm.retentionPeriod,
      securityLevel: searchForm.securityLevel,
      sourceType: searchForm.sourceType,
      status: searchForm.status,
      scanBatchNo: searchForm.scanBatchNo,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    if (searchForm.archiveDateRange?.length === 2) {
      params.archiveDateStart = searchForm.archiveDateRange[0]
      params.archiveDateEnd = searchForm.archiveDateRange[1]
    }
    const res = await getArchiveList(params)
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) {
    console.error('获取档案列表失败', e)
    ElMessage.error(e.response?.data?.message || '获取档案列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.archiveType = ''
  searchForm.archiveForm = ''
  searchForm.retentionPeriod = ''
  searchForm.securityLevel = ''
  searchForm.sourceType = ''
  searchForm.status = ''
  searchForm.scanBatchNo = ''
  searchForm.archiveDateRange = null
  pagination.pageNum = 1
  fetchData()
}

// 刷新
const handleRefresh = () => {
  fetchData()
}

// 新建
const handleCreate = () => {
  router.push('/receive')
}

// 查看
const handleView = (row) => {
  router.push(`/archives/${row.id}`)
}

// 编辑
const handleEdit = (row) => {
  router.push(`/archives/${row.id}?edit=true`)
}

const canStore = (row) => canManageArchive.value && ['RECEIVED', 'CATALOGING'].includes(row.status)
const canApprove = (row) => canReviewArchive.value && row.status === 'PENDING_REVIEW'

const handleStore = async (row) => {
  if (!canStore(row)) return

  storingId.value = row.id
  try {
    await updateArchiveStatus(row.id, 'STORED')
    ElMessage.success(`档案 ${row.archiveNo} 已正式归档`)
    await fetchData()
  } catch (e) {
    console.error('正式归档失败', e)
    ElMessage.error(e.response?.data?.message || '正式归档失败')
  } finally {
    storingId.value = null
  }
}

const handleApprove = async (row) => {
  if (!canApprove(row)) return

  approvingId.value = row.id
  try {
    await approveArchive(row.id)
    ElMessage.success(`档案 ${row.archiveNo} 已审核通过并正式入库`)
    await fetchData()
  } catch (e) {
    console.error('审核通过失败', e)
    ElMessage.error(e.response?.data?.message || '审核通过失败')
  } finally {
    approvingId.value = null
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await deleteArchive(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    console.error('删除失败', e)
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

// 行点击
const handleRowClick = (row) => {
  router.push(`/archives/${row.id}`)
}

// 分页
const handleSizeChange = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleCurrentChange = () => {
  fetchData()
}

// 格式化函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}
// 注：getArchiveTypeName, getStatusName, getStatusType, getRetentionName 
// 已从 archiveEnums.js 导入

const getSourceName = (source) => {
  const map = {
    LAW_FIRM: '律所系统',
    MANUAL: '手动录入',
    IMPORT: '批量导入',
    TRANSFER: '移交'
  }
  return map[source] || source
}

onMounted(() => {
  if (route.query.scanBatchNo) {
    searchForm.scanBatchNo = String(route.query.scanBatchNo)
  }
  fetchData()
})
</script>

<style lang="scss" scoped>
.archive-list {
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

.search-card {
  border-radius: 10px;
  
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.toolbar {
  display: flex;
  gap: 12px;
}

.table-card {
  border-radius: 10px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  cursor: pointer;
}
</style>
