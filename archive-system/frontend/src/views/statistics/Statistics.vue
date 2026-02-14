<template>
  <div class="statistics-page">
    <!-- 概览卡片 -->
    <el-row
      :gutter="16"
      class="overview-row"
    >
      <el-col :span="6">
        <el-card
          shadow="hover"
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
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
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
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
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
      </el-col>
      <el-col :span="6">
        <el-card
          shadow="hover"
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
      </el-col>
    </el-row>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { Folder, Document, Reading, Bell } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getOverview, countByType, countByRetention, getTrend, getBorrowStats, getStorageStats } from '@/api/statistics'
import { getPushRecordStatistics } from '@/api/pushRecord'

const overview = reactive({})
const storage = reactive({})
const borrowStats = reactive({})
const pushStats = reactive({})
const trendYear = ref(new Date().getFullYear().toString())

const typeChartRef = ref(null)
const retentionChartRef = ref(null)
const trendChartRef = ref(null)

let typeChart = null
let retentionChart = null
let trendChart = null

// 加载概览数据
const loadOverview = async () => {
  try {
    const res = await getOverview()
    Object.assign(overview, res.data)
  } catch (e) {
    console.error('加载概览失败', e)
  }
}

// 加载存储统计
const loadStorage = async () => {
  try {
    const res = await getStorageStats()
    Object.assign(storage, res.data)
  } catch (e) {
    console.error('加载存储统计失败', e)
  }
}

// 加载借阅统计
const loadBorrowStats = async () => {
  try {
    const res = await getBorrowStats()
    Object.assign(borrowStats, res.data)
  } catch (e) {
    console.error('加载借阅统计失败', e)
  }
}

// 加载推送统计
const loadPushStats = async () => {
  try {
    const res = await getPushRecordStatistics()
    if (res.code === 200) {
      Object.assign(pushStats, res.data)
    }
  } catch (e) {
    console.error('加载推送统计失败', e)
  }
}

// 加载档案类型图表
const loadTypeChart = async () => {
  try {
    const res = await countByType()
    const data = res.data.map(item => ({
      name: item.name,
      value: item.count
    }))

    typeChart = echarts.init(typeChartRef.value)
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
  }
}

// 加载保管期限图表
const loadRetentionChart = async () => {
  try {
    const res = await countByRetention()
    const data = res.data.map(item => ({
      name: item.name,
      value: item.count
    }))

    retentionChart = echarts.init(retentionChartRef.value)
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
  }
}

// 加载趋势图表
const loadTrend = async () => {
  try {
    const res = await getTrend(parseInt(trendYear.value))
    const months = res.data.map(item => item.monthName)
    const counts = res.data.map(item => item.count)

    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
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
  }
}

// 窗口大小变化时重绘图表
const handleResize = () => {
  typeChart?.resize()
  retentionChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadOverview()
  loadStorage()
  loadBorrowStats()
  loadPushStats()
  loadTypeChart()
  loadRetentionChart()
  loadTrend()
  
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
  padding: 20px;
}

.overview-row {
  margin-bottom: 16px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }
  
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }
}

.chart-row {
  margin-bottom: 16px;
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
</style>
