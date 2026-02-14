<template>
  <div class="archive-list">
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
    <el-card shadow="never">
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
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArchiveList, deleteArchive } from '@/api/archive'
import { 
  getArchiveTypeName, 
  getStatusName, 
  getStatusType, 
  getRetentionName,
  getArchiveTypeOptions,
  ARCHIVE_STATUS 
} from '@/utils/archiveEnums'

const router = useRouter()

// 下拉选项
const archiveTypeOptions = getArchiveTypeOptions()
const statusOptions = Object.entries(ARCHIVE_STATUS)
  .filter(([key]) => ['RECEIVED', 'CATALOGING', 'STORED', 'BORROWED'].includes(key))
  .map(([value, label]) => ({ value, label }))
const loading = ref(false)
const tableData = ref([])

const searchForm = reactive({
  keyword: '',
  archiveType: '',
  status: ''
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
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
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
  searchForm.status = ''
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
  fetchData()
})
</script>

<style lang="scss" scoped>
.archive-list {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
  
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.toolbar {
  margin-bottom: 16px;
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
