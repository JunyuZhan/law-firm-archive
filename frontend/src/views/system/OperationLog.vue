<template>
  <div class="operation-log">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form
        :model="queryParams"
        inline
      >
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="操作描述/操作人/对象ID"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="对象类型">
          <el-select
            v-model="queryParams.objectType"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              label="档案"
              value="ARCHIVE"
            />
            <el-option
              label="文件"
              value="FILE"
            />
            <el-option
              label="借阅"
              value="BORROW"
            />
            <el-option
              label="鉴定"
              value="APPRAISAL"
            />
            <el-option
              label="系统"
              value="SYSTEM"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="queryParams.operationType"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              label="创建"
              value="CREATE"
            />
            <el-option
              label="更新"
              value="UPDATE"
            />
            <el-option
              label="删除"
              value="DELETE"
            />
            <el-option
              label="查看"
              value="VIEW"
            />
            <el-option
              label="下载"
              value="DOWNLOAD"
            />
            <el-option
              label="打印"
              value="PRINT"
            />
            <el-option
              label="导出"
              value="EXPORT"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :icon="Search"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button
            :icon="Refresh"
            @click="handleReset"
          >
            重置
          </el-button>
          <el-button
            type="success"
            :icon="Download"
            @click="handleExport"
          >
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row
      :gutter="16"
      class="stat-row"
    >
      <el-col
        v-for="(stat, key) in statistics"
        :key="key"
        :span="4"
      >
        <el-card
          class="stat-card"
          shadow="hover"
        >
          <div class="stat-value">
            {{ stat }}
          </div>
          <div class="stat-label">
            {{ getOperationLabel(key) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志表格 -->
    <el-card class="table-card">
      <template #header>
        <span>操作日志</span>
      </template>

      <el-table
        v-loading="loading"
        :data="logList"
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="objectType"
          label="对象类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ getObjectTypeLabel(row.objectType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="objectId"
          label="对象ID"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          prop="operationType"
          label="操作类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="getOperationTypeTag(row.operationType)"
            >
              {{ getOperationLabel(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="operationDesc"
          label="操作描述"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="operatorName"
          label="操作人"
          width="100"
        />
        <el-table-column
          prop="operatorIp"
          label="IP地址"
          width="130"
        />
        <el-table-column
          prop="operatedAt"
          label="操作时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDate(row.operatedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="80"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleViewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        class="pagination"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchLogs"
        @current-change="fetchLogs"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="操作日志详情"
      width="600px"
    >
      <el-descriptions
        v-if="currentLog"
        :column="2"
        border
      >
        <el-descriptions-item label="ID">
          {{ currentLog.id }}
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ formatDate(currentLog.operatedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="对象类型">
          {{ getObjectTypeLabel(currentLog.objectType) }}
        </el-descriptions-item>
        <el-descriptions-item label="对象ID">
          {{ currentLog.objectId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作类型">
          {{ getOperationLabel(currentLog.operationType) }}
        </el-descriptions-item>
        <el-descriptions-item label="档案ID">
          {{ currentLog.archiveId }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentLog.operatorName }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人ID">
          {{ currentLog.operatorId }}
        </el-descriptions-item>
        <el-descriptions-item
          label="IP地址"
          :span="2"
        >
          {{ currentLog.operatorIp }}
        </el-descriptions-item>
        <el-descriptions-item
          label="操作描述"
          :span="2"
        >
          {{ currentLog.operationDesc }}
        </el-descriptions-item>
        <el-descriptions-item
          label="User-Agent"
          :span="2"
        >
          <div style="word-break: break-all; font-size: 12px; color: #666">
            {{ currentLog.operatorUa }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentLog.operationDetail"
          label="操作详情"
          :span="2"
        >
          <pre style="white-space: pre-wrap; word-break: break-all; margin: 0; font-size: 12px">{{ JSON.stringify(currentLog.operationDetail, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { getOperationLogs, getLogStatistics, exportLogs } from '@/api/log'

// 查询参数
const queryParams = reactive({
  keyword: '',
  objectType: '',
  operationType: '',
  pageNum: 1,
  pageSize: 20
})
const dateRange = ref([])

const loading = ref(false)
const logList = ref([])
const total = ref(0)
const statistics = ref({})

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref(null)

// 计算日期参数
const startDate = computed(() => dateRange.value?.[0] || null)
const endDate = computed(() => dateRange.value?.[1] || null)

onMounted(() => {
  fetchLogs()
  fetchStatistics()
})

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      ...queryParams,
      startDate: startDate.value,
      endDate: endDate.value
    }
    const res = await getOperationLogs(params)
    logList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function fetchStatistics() {
  try {
    const res = await getLogStatistics({
      startDate: startDate.value,
      endDate: endDate.value
    })
    statistics.value = res.data || {}
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchLogs()
  fetchStatistics()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.objectType = ''
  queryParams.operationType = ''
  queryParams.pageNum = 1
  dateRange.value = []
  fetchLogs()
  fetchStatistics()
}

async function handleExport() {
  try {
    const res = await exportLogs({
      objectType: queryParams.objectType,
      operationType: queryParams.operationType,
      startDate: startDate.value,
      endDate: endDate.value
    })
    
    // 下载文件
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('导出失败')
  }
}

function handleViewDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

function getObjectTypeLabel(type) {
  const map = {
    ARCHIVE: '档案',
    FILE: '文件',
    BORROW: '借阅',
    APPRAISAL: '鉴定',
    SYSTEM: '系统'
  }
  return map[type] || type
}

function getOperationLabel(type) {
  const map = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    VIEW: '查看',
    DOWNLOAD: '下载',
    PRINT: '打印',
    EXPORT: '导出'
  }
  return map[type] || type
}

function getOperationTypeTag(type) {
  const map = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    VIEW: '',
    DOWNLOAD: 'info',
    PRINT: 'info',
    EXPORT: 'info'
  }
  return map[type] || ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.operation-log {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 4px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
