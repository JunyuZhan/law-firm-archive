<template>
  <div class="statistics-page">
    <div class="page-header">
      <h1>统计分析</h1>
      <p>查看电子档案规模、借阅动态、扫描批次和推送表现，用于管理层掌握系统运行情况。</p>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-grid">
      <div class="overview-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div
              class="stat-icon"
              style="background: #409eff;"
            >
              <el-icon :size="28">
                <Folder />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ overview.totalArchives || 0 }}
              </div>
              <div class="stat-label">
                档案总数
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div class="overview-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div
              class="stat-icon"
              style="background: #67c23a;"
            >
              <el-icon :size="28">
                <Document />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ overview.totalFiles || 0 }}
              </div>
              <div class="stat-label">
                电子文件
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div class="overview-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div
              class="stat-icon"
              style="background: #e6a23c;"
            >
              <el-icon :size="28">
                <Reading />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ overview.borrowing || 0 }}
              </div>
              <div class="stat-label">
                借阅中
              </div>
            </div>
          </div>
        </el-card>
      </div>
      <div class="overview-grid-item">
        <el-card
          shadow="never"
          class="stat-card"
        >
          <div class="stat-content">
            <div
              class="stat-icon"
              style="background: #f56c6c;"
            >
              <el-icon :size="28">
                <Bell />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ overview.pendingApproval || 0 }}
              </div>
              <div class="stat-label">
                待审批
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 图表区域 -->
    <el-row
      :gutter="16"
      class="chart-row"
    >
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            档案类型分布
          </template>
          <div
            ref="typeChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            保管期限分布
          </template>
          <div
            ref="retentionChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row
      :gutter="16"
      class="chart-row"
    >
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="chart-header">
              <span>月度接收趋势</span>
              <el-date-picker
                v-model="trendYear"
                type="year"
                placeholder="选择年份"
                value-format="YYYY"
                style="width: 120px"
                @change="loadTrend"
              />
            </div>
          </template>
          <div
            ref="trendChartRef"
            class="chart-container"
          />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            存储统计
          </template>
          <div class="storage-stats">
            <div class="storage-item">
              <div class="storage-label">
                文件总数
              </div>
              <div class="storage-value">
                {{ storage.fileCount || 0 }} 个
              </div>
            </div>
            <div class="storage-item">
              <div class="storage-label">
                存储空间
              </div>
              <div class="storage-value">
                {{ storage.totalSizeFormatted || '0 B' }}
              </div>
            </div>
          </div>
        </el-card>
        <el-card
          shadow="never"
          style="margin-top: 16px;"
        >
          <template #header>
            借阅统计
          </template>
          <div class="borrow-stats">
            <div class="borrow-item">
              <span class="label">总借阅次数</span>
              <span class="value">{{ borrowStats.totalBorrows || 0 }}</span>
            </div>
            <div class="borrow-item">
              <span class="label">本月借阅</span>
              <span class="value">{{ borrowStats.monthlyBorrows || 0 }}</span>
            </div>
            <div class="borrow-item">
              <span class="label">逾期未还</span>
              <span class="value text-danger">{{ borrowStats.overdue || 0 }}</span>
            </div>
          </div>
        </el-card>
        <el-card
          shadow="never"
          style="margin-top: 16px;"
        >
          <template #header>
            推送统计
          </template>
          <div class="push-stats">
            <div class="push-item">
              <span class="label">总推送数</span>
              <span class="value">{{ pushStats.total || 0 }}</span>
            </div>
            <div class="push-item">
              <span class="label">今日推送</span>
              <span class="value text-primary">{{ pushStats.today || 0 }}</span>
            </div>
            <div class="push-item">
              <span class="label">成功</span>
              <span class="value text-success">{{ pushStats.success || 0 }}</span>
            </div>
            <div class="push-item">
              <span class="label">失败</span>
              <span class="value text-danger">{{ pushStats.failed || 0 }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="chart-header">
          <span>最近扫描批次</span>
          <el-input
            v-model="scanBatchKeyword"
            placeholder="输入批次号筛选"
            clearable
            style="width: 240px"
            @keyup.enter="loadScanBatches"
            @clear="loadScanBatches"
          />
        </div>
      </template>
      <div class="scan-batch-summary">
        <div class="summary-item">
          <div class="summary-label">
            批次数
          </div>
          <div class="summary-value">
            {{ scanBatchSummary.batchCount }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">
            文件总数
          </div>
          <div class="summary-value">
            {{ scanBatchSummary.fileCount }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">
            待复核
          </div>
          <div class="summary-value warning">
            {{ scanBatchSummary.pendingCount }}
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-label">
            平均通过率
          </div>
          <div class="summary-value success">
            {{ formatPercent(scanBatchSummary.averagePassRate) }}
          </div>
        </div>
      </div>
      <el-table
        v-loading="loading.scanBatch"
        :data="scanBatches"
        stripe
        border
        size="small"
        empty-text="暂无扫描批次数据"
      >
        <el-table-column
          prop="scanBatchNo"
          label="扫描批次号"
          min-width="180"
        />
        <el-table-column
          prop="archiveCount"
          label="关联档案"
          width="100"
          align="center"
        />
        <el-table-column
          prop="fileCount"
          label="文件数"
          width="90"
          align="center"
        />
        <el-table-column
          label="扫描覆盖"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="rate-cell">
              <div class="rate-text">
                {{ row.scannedFileCount }}/{{ row.fileCount }}
              </div>
              <el-progress
                :percentage="toDisplayRate(row.scanCoverageRate)"
                :stroke-width="10"
                :show-text="false"
                color="#409eff"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="复核完成"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="rate-cell">
              <div class="rate-text">
                {{ row.reviewedCount }}/{{ row.fileCount }}
              </div>
              <el-progress
                :percentage="toDisplayRate(row.reviewCompletionRate)"
                :stroke-width="10"
                :show-text="false"
                color="#e6a23c"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="passedCount"
          label="已通过"
          width="90"
          align="center"
        />
        <el-table-column
          prop="pendingCount"
          label="待复核"
          width="90"
          align="center"
        />
        <el-table-column
          prop="failedCount"
          label="未通过"
          width="90"
          align="center"
        />
        <el-table-column
          prop="latestScanTime"
          label="最近扫描时间"
          width="180"
        />
        <el-table-column
          label="通过率"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="getPassRateTagType(row.passRate)"
              size="small"
            >
              {{ formatPercent(row.passRate) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="120"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="goToBatchArchives(row)"
            >
              查看档案
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无扫描批次台账">
            <div class="empty-tip">
              扫描上传时填写批次号、扫描人和复核状态后，这里会自动形成入库批次台账。
            </div>
          </el-empty>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Folder, Document, Reading, Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { init } from 'echarts/core'
import { getOverview, countByType, countByRetention, getTrend, getBorrowStats, getStorageStats, getScanBatchStats } from '@/api/statistics'
import { getPushRecordStatistics } from '@/api/pushRecord'

use([CanvasRenderer, PieChart, BarChart, TooltipComponent, GridComponent, LegendComponent])

const router = useRouter()
const overview = reactive({})
const storage = reactive({})
const borrowStats = reactive({})
const pushStats = reactive({})
const trendYear = ref(new Date().getFullYear().toString())
const scanBatches = ref([])
const scanBatchKeyword = ref('')
const scanBatchSummary = computed(() => {
  const summary = {
    batchCount: scanBatches.value.length,
    fileCount: 0,
    pendingCount: 0,
    averagePassRate: 0
  }
  if (scanBatches.value.length === 0) {
    return summary
  }

  let passRateTotal = 0
  scanBatches.value.forEach(item => {
    summary.fileCount += Number(item.fileCount || 0)
    summary.pendingCount += Number(item.pendingCount || 0)
    passRateTotal += Number(item.passRate || 0)
  })
  summary.averagePassRate = passRateTotal / scanBatches.value.length
  return summary
})

// Loading 状态
const loading = reactive({
  overview: false,
  storage: false,
  borrow: false,
  push: false,
  typeChart: false,
  retentionChart: false,
  trend: false,
  scanBatch: false
})

const typeChartRef = ref(null)
const retentionChartRef = ref(null)
const trendChartRef = ref(null)

let typeChart = null
let retentionChart = null
let trendChart = null

// 加载概览数据
const loadOverview = async () => {
  loading.overview = true
  try {
    const res = await getOverview()
    Object.assign(overview, res.data)
  } catch (e) {
    console.error('加载概览失败', e)
    ElMessage.error('加载概览数据失败')
  } finally {
    loading.overview = false
  }
}

// 加载存储统计
const loadStorage = async () => {
  loading.storage = true
  try {
    const res = await getStorageStats()
    Object.assign(storage, res.data)
  } catch (e) {
    console.error('加载存储统计失败', e)
    ElMessage.error('加载存储统计失败')
  } finally {
    loading.storage = false
  }
}

// 加载借阅统计
const loadBorrowStats = async () => {
  loading.borrow = true
  try {
    const res = await getBorrowStats()
    Object.assign(borrowStats, res.data)
  } catch (e) {
    console.error('加载借阅统计失败', e)
    ElMessage.error('加载借阅统计失败')
  } finally {
    loading.borrow = false
  }
}

// 加载推送统计
const loadPushStats = async () => {
  loading.push = true
  try {
    const res = await getPushRecordStatistics()
    if (res.success) {
      Object.assign(pushStats, res.data)
    }
  } catch (e) {
    console.error('加载推送统计失败', e)
    ElMessage.error('加载推送统计失败')
  } finally {
    loading.push = false
  }
}

const loadScanBatches = async () => {
  loading.scanBatch = true
  try {
    const res = await getScanBatchStats(scanBatchKeyword.value)
    scanBatches.value = res.data || []
  } catch (e) {
    console.error('加载扫描批次失败', e)
    ElMessage.error('加载扫描批次统计失败')
  } finally {
    loading.scanBatch = false
  }
}

// 加载档案类型图表
const loadTypeChart = async () => {
  loading.typeChart = true
  try {
    const res = await countByType()
    const data = res.data.map(item => ({
      name: item.name,
      value: item.count
    }))

    typeChart = init(typeChartRef.value)
    typeChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  } catch (e) {
    console.error('加载类型图表失败', e)
    ElMessage.error('加载档案类型分布失败')
  } finally {
    loading.typeChart = false
  }
}

// 加载保管期限图表
const loadRetentionChart = async () => {
  loading.retentionChart = true
  try {
    const res = await countByRetention()
    const data = res.data.map(item => ({
      name: item.name,
      value: item.count
    }))

    retentionChart = init(retentionChartRef.value)
    retentionChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '70%',
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
  } catch (e) {
    console.error('加载保管期限图表失败', e)
    ElMessage.error('加载保管期限分布失败')
  } finally {
    loading.retentionChart = false
  }
}

// 加载趋势图表
const loadTrend = async () => {
  loading.trend = true
  try {
    const res = await getTrend(parseInt(trendYear.value))
    const months = res.data.map(item => item.monthName)
    const counts = res.data.map(item => item.count)

    if (!trendChart) {
      trendChart = init(trendChartRef.value)
    }
    
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: months
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        data: counts,
        type: 'bar',
        itemStyle: {
          color: '#409eff'
        }
      }]
    })
  } catch (e) {
    console.error('加载趋势图表失败', e)
    ElMessage.error('加载月度趋势失败')
  } finally {
    loading.trend = false
  }
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  typeChart?.resize()
  retentionChart?.resize()
  trendChart?.resize()
}

const goToBatchArchives = (row) => {
  router.push({
    path: '/archives',
    query: {
      scanBatchNo: row.scanBatchNo
    }
  })
}

const toDisplayRate = (value) => Number(Number(value || 0).toFixed(1))

const formatPercent = (value) => `${toDisplayRate(value)}%`

const getPassRateTagType = (value) => {
  const rate = Number(value || 0)
  if (rate >= 95) return 'success'
  if (rate >= 80) return 'warning'
  return 'danger'
}

onMounted(() => {
  loadOverview()
  loadStorage()
  loadBorrowStats()
  loadPushStats()
  loadTypeChart()
  loadRetentionChart()
  loadTrend()
  loadScanBatches()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  retentionChart?.dispose()
  trendChart?.dispose()
})
</script>

<style lang="scss" scoped>
.statistics-page {
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

.overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.stat-card {
  border-radius: 10px;

  :deep(.el-card__body) {
    min-height: 88px;
    padding: 16px 14px;
  }
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
  
  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-top: 4px;
  }
}

.chart-row {
  margin-bottom: 0;
}

.scan-batch-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.summary-label {
  font-size: 13px;
  color: #909399;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;

  &.warning {
    color: #e6a23c;
  }

  &.success {
    color: #67c23a;
  }
}

.chart-container {
  height: 300px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.storage-stats {
  .storage-item {
    padding: 16px 0;
    border-bottom: 1px solid #eee;
    
    &:last-child {
      border-bottom: none;
    }
    
    .storage-label {
      color: #909399;
      font-size: 14px;
    }
    
    .storage-value {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
      margin-top: 8px;
    }
  }
}

.borrow-stats {
  .borrow-item {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #eee;
    
    &:last-child {
      border-bottom: none;
    }
    
    .label {
      color: #606266;
    }
    
    .value {
      font-weight: 600;
      
      &.text-danger {
        color: #f56c6c;
      }
    }
  }
}

.push-stats {
  .push-item {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #eee;
    
    &:last-child {
      border-bottom: none;
    }
    
    .label {
      color: #606266;
    }
    
    .value {
      font-weight: 600;
      
      &.text-primary {
        color: #409eff;
      }
      
      &.text-success {
        color: #67c23a;
      }
      
      &.text-danger {
        color: #f56c6c;
      }
    }
  }
}

.rate-cell {
  min-width: 140px;
}

.rate-text {
  margin-bottom: 6px;
  font-size: 12px;
  color: #606266;
}

.empty-tip {
  max-width: 320px;
  margin: 8px auto 0;
  font-size: 13px;
  line-height: 1.6;
  color: #909399;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .scan-batch-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .scan-batch-summary {
    grid-template-columns: 1fr;
  }
}
</style>
