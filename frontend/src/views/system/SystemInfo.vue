<template>
  <div class="system-info-page">
    <div class="page-header">
      <h1>系统信息</h1>
      <p>查看版本与依赖状态；向已配置的镜像仓库查询是否存在可用更新。</p>
    </div>

    <div class="info-grid">
      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <span>版本信息</span>
            <el-button
              text
              @click="loadRuntimeInfo"
            >
              刷新
            </el-button>
          </div>
        </template>
        <el-descriptions
          :column="1"
          border
        >
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
          <el-descriptions-item label="提交">
            {{ runtimeInfo.commitSha || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <span>镜像仓库更新</span>
            <el-button
              text
              :loading="registryCheckLoading"
              @click="runRegistryCheck"
            >
              检查
            </el-button>
          </div>
        </template>
        <div class="registry-check-head">
          <el-tag
            v-if="registryCheck.updateAvailable === true"
            type="danger"
            size="small"
          >
            有可用更新
          </el-tag>
          <el-tag
            v-else-if="registryCheck.updateAvailable === false"
            type="success"
            size="small"
          >
            暂无更新
          </el-tag>
          <el-tag
            v-else
            type="info"
            size="small"
          >
            未判定
          </el-tag>
          <span
            v-if="registryCheck.checkedAt"
            class="registry-checked-at"
          >{{ registryCheck.checkedAt }}</span>
        </div>
        <p class="registry-check-message">
          {{ registryCheck.message || '点击「检查」向镜像仓库查询。' }}
        </p>
        <p
          v-if="registryCheck.detail"
          class="registry-check-detail"
        >
          {{ registryCheck.detail }}
        </p>
      </el-card>

      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <span>依赖状态</span>
            <el-button
              text
              @click="loadDependencyStatus"
            >
              刷新
            </el-button>
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
              <el-tag
                :type="item.status === 'UP' ? 'success' : item.status === 'UNKNOWN' ? 'info' : 'danger'"
                size="small"
              >
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
import { onMounted, reactive, ref } from 'vue'
import packageJson from '../../../package.json'
import { ElMessage } from 'element-plus'
import { checkRegistryUpdate, getDependencyStatus, getRuntimeInfo } from '@/api/config'

const frontendVersion = packageJson.version

const runtimeInfo = reactive({
  productVersion: '',
  backendVersion: '',
  buildTime: '',
  commitSha: ''
})

const registryCheckLoading = ref(false)
const registryCheck = reactive({
  updateAvailable: null,
  message: '',
  detail: '',
  checkedAt: ''
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
      buildTime: res?.data?.buildTime || '',
      commitSha: res?.data?.commitSha || ''
    })
  } catch (error) {
    ElMessage.error('版本信息加载失败')
  }
}

const loadDependencyStatus = async () => {
  try {
    const res = await getDependencyStatus()
    dependencyStatus.overallStatus = res?.data?.overallStatus || 'UNKNOWN'
    dependencyStatus.items = Array.isArray(res?.data?.items) ? res.data.items : []
  } catch (error) {
    ElMessage.error('依赖状态加载失败')
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

const runRegistryCheck = async () => {
  registryCheckLoading.value = true
  try {
    const res = await checkRegistryUpdate()
    const data = res?.data || {}
    registryCheck.updateAvailable = data.updateAvailable === true
      ? true
      : data.updateAvailable === false
        ? false
        : null
    registryCheck.message = data.message || ''
    registryCheck.detail = data.detail || ''
    registryCheck.checkedAt = data.checkedAt || ''
  } catch (error) {
    ElMessage.error('镜像仓库检查失败')
    registryCheck.updateAvailable = null
    registryCheck.message = ''
    registryCheck.detail = ''
    registryCheck.checkedAt = ''
  } finally {
    registryCheckLoading.value = false
  }
}

onMounted(() => {
  loadRuntimeInfo()
  loadDependencyStatus()
  runRegistryCheck()
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

.registry-check-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.registry-checked-at {
  font-size: 12px;
  color: #909399;
}

.registry-check-detail {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
  word-break: break-word;
}

.registry-check-message {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
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
</style>
