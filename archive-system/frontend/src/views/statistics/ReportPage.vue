<template>
  <div class="report-page">
    <el-card class="page-header">
      <template #header>
        <div class="header-content">
          <span class="title">报表导出</span>
          <span class="subtitle">导出Excel格式的统计报表</span>
        </div>
      </template>
      
      <!-- 报表类型选择 -->
      <el-tabs v-model="activeTab" class="report-tabs">
        <!-- 统计概览报表 -->
        <el-tab-pane label="统计概览" name="overview">
          <div class="report-section">
            <p class="report-desc">导出档案统计概览报表，包含档案总量、类型分布、月度趋势等数据。</p>
            
            <el-form :inline="true" class="filter-form">
              <el-form-item label="统计年份">
                <el-date-picker
                  v-model="overviewYear"
                  type="year"
                  placeholder="选择年份"
                  value-format="YYYY"
                  :clearable="true"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="exportOverview" :loading="loading.overview">
                  <el-icon><Download /></el-icon>
                  导出报表
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        
        <!-- 档案清单 -->
        <el-tab-pane label="档案清单" name="archives">
          <div class="report-section">
            <p class="report-desc">导出档案清单，支持按条件筛选后导出。最多导出10000条记录。</p>
            
            <el-form :model="archiveFilter" label-width="80px" class="filter-form">
              <el-row :gutter="16">
                <el-col :span="6">
                  <el-form-item label="档案类型">
                    <el-select v-model="archiveFilter.archiveType" placeholder="全部" clearable>
                      <el-option label="文书档案" value="DOCUMENT" />
                      <el-option label="科技档案" value="SCIENCE" />
                      <el-option label="会计档案" value="ACCOUNTING" />
                      <el-option label="人事档案" value="PERSONNEL" />
                      <el-option label="专业档案" value="SPECIAL" />
                      <el-option label="声像档案" value="AUDIOVISUAL" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="档案状态">
                    <el-select v-model="archiveFilter.status" placeholder="全部" clearable>
                      <el-option label="已接收" value="RECEIVED" />
                      <el-option label="已归档" value="STORED" />
                      <el-option label="借出中" value="BORROWED" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="归档日期">
                    <el-date-picker
                      v-model="archiveDateRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="4">
                  <el-form-item>
                    <el-button type="primary" @click="exportArchives" :loading="loading.archives">
                      <el-icon><Download /></el-icon>
                      导出清单
                    </el-button>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>
        </el-tab-pane>
        
        <!-- 借阅统计 -->
        <el-tab-pane label="借阅统计" name="borrow">
          <div class="report-section">
            <p class="report-desc">导出借阅申请记录，包含申请人、审批状态、归还情况等信息。</p>
            
            <el-form :inline="true" class="filter-form">
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="borrowDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="exportBorrow" :loading="loading.borrow">
                  <el-icon><Download /></el-icon>
                  导出报表
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        
        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="log">
          <div class="report-section">
            <p class="report-desc">导出系统操作日志，用于审计和追溯。最多导出10000条记录。</p>
            
            <el-form :inline="true" class="filter-form">
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="logDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="exportLog" :loading="loading.log">
                  <el-icon><Download /></el-icon>
                  导出日志
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 导出历史（可选） -->
    <el-card class="export-tips">
      <template #header>
        <span>导出说明</span>
      </template>
      <ul class="tips-list">
        <li>所有报表均导出为 Excel (.xlsx) 格式</li>
        <li>档案清单和操作日志最多导出 10000 条记录</li>
        <li>建议使用筛选条件缩小导出范围以提高效率</li>
        <li>导出过程中请勿关闭或刷新页面</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import request from '@/utils/request'

const activeTab = ref('overview')

// 加载状态
const loading = reactive({
  overview: false,
  archives: false,
  borrow: false,
  log: false
})

// 筛选条件
const overviewYear = ref(null)
const archiveFilter = reactive({
  archiveType: '',
  status: '',
  keyword: ''
})
const archiveDateRange = ref([])
const borrowDateRange = ref([])
const logDateRange = ref([])

// 下载文件
const downloadFile = async (url, params, filename) => {
  try {
    const response = await request({
      url,
      method: 'get',
      params,
      responseType: 'blob'
    })
    
    // 创建下载链接
    const blob = new Blob([response], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
    
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败', error)
    ElMessage.error('导出失败，请稍后重试')
  }
}

// 导出统计概览
const exportOverview = async () => {
  loading.overview = true
  try {
    const params = {}
    if (overviewYear.value) {
      params.year = overviewYear.value
    }
    await downloadFile('/statistics/export/overview', params, '档案统计概览报表.xlsx')
  } finally {
    loading.overview = false
  }
}

// 导出档案清单
const exportArchives = async () => {
  loading.archives = true
  try {
    const params = { ...archiveFilter }
    if (archiveDateRange.value && archiveDateRange.value.length === 2) {
      params.archiveDateStart = archiveDateRange.value[0]
      params.archiveDateEnd = archiveDateRange.value[1]
    }
    await downloadFile('/statistics/export/archives', params, '档案清单.xlsx')
  } finally {
    loading.archives = false
  }
}

// 导出借阅统计
const exportBorrow = async () => {
  loading.borrow = true
  try {
    const params = {}
    if (borrowDateRange.value && borrowDateRange.value.length === 2) {
      params.startDate = borrowDateRange.value[0]
      params.endDate = borrowDateRange.value[1]
    }
    await downloadFile('/statistics/export/borrow', params, '借阅统计报表.xlsx')
  } finally {
    loading.borrow = false
  }
}

// 导出操作日志
const exportLog = async () => {
  loading.log = true
  try {
    const params = {}
    if (logDateRange.value && logDateRange.value.length === 2) {
      params.startDate = logDateRange.value[0]
      params.endDate = logDateRange.value[1]
    }
    await downloadFile('/statistics/export/operation-log', params, '操作日志报表.xlsx')
  } finally {
    loading.log = false
  }
}
</script>

<style scoped>
.report-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.report-tabs {
  margin-top: 10px;
}

.report-section {
  padding: 20px 0;
}

.report-desc {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
}

.filter-form {
  background: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
}

.export-tips {
  margin-top: 20px;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 2;
}

.tips-list li {
  font-size: 14px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
}
</style>
