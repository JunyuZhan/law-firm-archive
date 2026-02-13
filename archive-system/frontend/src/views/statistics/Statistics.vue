<template>
  <div class="statistics">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #409eff;">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalCount || 0 }}</div>
            <div class="stat-label">档案总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #67c23a;">
            <el-icon :size="32"><FolderChecked /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('STORED') }}</div>
            <div class="stat-label">已入库</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #e6a23c;">
            <el-icon :size="32"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('RECEIVED') }}</div>
            <div class="stat-label">待入库</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f56c6c;">
            <el-icon :size="32"><Reading /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ getStatusCount('BORROWED') }}</div>
            <div class="stat-label">借出中</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>按状态统计</span>
          </template>
          <div class="chart-placeholder">
            <el-table :data="statusStats" stripe>
              <el-table-column prop="statusName" label="状态" />
              <el-table-column prop="count" label="数量" width="100" />
              <el-table-column label="占比" width="150">
                <template #default="{ row }">
                  <el-progress
                    :percentage="Math.round(row.count / stats.totalCount * 100) || 0"
                    :stroke-width="10"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>按来源统计</span>
          </template>
          <div class="chart-placeholder">
            <el-table :data="sourceStats" stripe>
              <el-table-column prop="sourceTypeName" label="来源" />
              <el-table-column prop="count" label="数量" width="100" />
              <el-table-column label="占比" width="150">
                <template #default="{ row }">
                  <el-progress
                    :percentage="Math.round(row.count / stats.totalCount * 100) || 0"
                    :stroke-width="10"
                    color="#67c23a"
                  />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { archiveApi } from '@/api/archive'

const stats = ref({})

const statusNameMap = {
  RECEIVED: '已接收',
  PENDING: '待入库',
  STORED: '已入库',
  BORROWED: '借出中',
  PENDING_DESTROY: '待销毁',
  DESTROYED: '已销毁'
}

const sourceNameMap = {
  LAW_FIRM: '律所系统',
  MANUAL: '手动录入',
  IMPORT: '批量导入',
  EXTERNAL: '外部系统'
}

const statusStats = computed(() => {
  return (stats.value.byStatus || []).map(item => ({
    ...item,
    statusName: statusNameMap[item.status] || item.status
  }))
})

const sourceStats = computed(() => {
  return (stats.value.bySourceType || []).map(item => ({
    ...item,
    sourceTypeName: sourceNameMap[item.source_type] || item.source_type
  }))
})

const getStatusCount = (status) => {
  const item = (stats.value.byStatus || []).find(s => s.status === status)
  return item?.count || 0
}

const loadData = async () => {
  try {
    const res = await archiveApi.getStatistics()
    stats.value = res.data
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.statistics {
  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;

    .stat-icon {
      width: 64px;
      height: 64px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      margin-right: 20px;
    }

    .stat-content {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }
      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 5px;
      }
    }
  }

  .chart-placeholder {
    min-height: 200px;
  }
}
</style>
