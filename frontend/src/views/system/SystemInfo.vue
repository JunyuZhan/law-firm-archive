<template>
  <div class="system-info-page">
    <div class="page-header">
      <h1>系统信息</h1>
      <p>集中查看系统版本与运行状态，便于日常检查和问题排查。</p>
    </div>

    <div class="info-grid">
      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <span>版本信息</span>
            <el-button text @click="loadRuntimeInfo">刷新</el-button>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="产品版本">
            {{ runtimeInfo.productVersion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="前端版本">
            {{ frontendVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="后端版本">
            {{ runtimeInfo.backendVersion || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="构建时间">
            {{ runtimeInfo.buildTime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="info-card">
        <template #header>
          <div class="card-header">
            <span>依赖状态</span>
            <el-button text @click="loadDependencyStatus">刷新</el-button>
          </div>
        </template>
        <div class="dependency-summary">
          <span>总体状态</span>
          <el-tag :type="dependencyStatus.overallStatus === 'UP' ? 'success' : dependencyStatus.overallStatus === 'UNKNOWN' ? 'info' : 'warning'">
            {{ formatDependencyStatus(dependencyStatus.overallStatus) }}
          </el-tag>
        </div>
        <div class="dependency-list">
          <div
            v-for="item in dependencyStatus.items"
            :key="item.key"
            class="dependency-item"
          >
            <div class="dependency-row">
              <span class="dependency-name">{{ item.label }}</span>
              <el-tag :type="item.status === 'UP' ? 'success' : item.status === 'UNKNOWN' ? 'info' : 'danger'" size="small">
                {{ formatDependencyStatus(item.status) }}
              </el-tag>
            </div>
            <div class="dependency-meta">
              <span
                v-for="(value, detailKey) in normalizeDependencyDetails(item.details)"
                :key="detailKey"
                class="meta-item"
              >
                {{ detailKey }}: {{ value }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import packageJson from '../../../package.json'
import { ElMessage } from 'element-plus'
import { getDependencyStatus, getRuntimeInfo } from '@/api/config'

const frontendVersion = packageJson.version

const runtimeInfo = reactive({
  productVersion: '',
  backendVersion: '',
  buildTime: ''
})

const dependencyStatus = reactive({
  overallStatus: 'UNKNOWN',
  items: []
})

const loadRuntimeInfo = async () => {
  try {
    const res = await getRuntimeInfo()
    Object.assign(runtimeInfo, {
      productVersion: res?.data?.productVersion || '',
      backendVersion: res?.data?.backendVersion || '',
      buildTime: res?.data?.buildTime || ''
    })
  } catch (error) {
    ElMessage.error('加载系统版本信息失败')
  }
}

const loadDependencyStatus = async () => {
  try {
    const res = await getDependencyStatus()
    dependencyStatus.overallStatus = res?.data?.overallStatus || 'UNKNOWN'
    dependencyStatus.items = Array.isArray(res?.data?.items) ? res.data.items : []
  } catch (error) {
    ElMessage.error('加载依赖状态失败')
  }
}

const formatDependencyStatus = (status) => {
  const mapping = {
    UP: '正常',
    DOWN: '异常',
    OUT_OF_SERVICE: '停服',
    UNKNOWN: '未知',
    DEGRADED: '部分异常'
  }
  return mapping[status] || status || '未知'
}

const normalizeDependencyDetails = (details) => {
  if (!details || typeof details !== 'object' || Array.isArray(details)) {
    return { message: '无额外信息' }
  }
  if (Object.keys(details).length === 0) {
    return { message: '无额外信息' }
  }
  return Object.fromEntries(
    Object.entries(details).map(([key, value]) => [key, String(value)])
  )
}

onMounted(() => {
  loadRuntimeInfo()
  loadDependencyStatus()
})
</script>

<style scoped>
.system-info-page {
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
  color: #606266;
  line-height: 1.6;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.info-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dependency-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
}

.dependency-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dependency-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.dependency-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dependency-name {
  font-weight: 600;
  color: #303133;
}

.dependency-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 8px;
}

.meta-item {
  font-size: 12px;
  color: #909399;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}

.doc-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.doc-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.doc-title {
  font-weight: 600;
  color: #303133;
}

.doc-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
}

.doc-path {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  word-break: break-all;
}
</style>
