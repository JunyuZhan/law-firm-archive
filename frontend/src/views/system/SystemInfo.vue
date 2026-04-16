<template>
  <div class="system-info-page">
    <div class="page-header">
      <h1>系统信息</h1>
      <p>集中查看系统版本、镜像更新检测与依赖状态，便于日常检查与升级判断。</p>
    </div>

    <el-alert
      v-if="imageUpgrade.upgradeRecommended"
      type="warning"
      show-icon
      :closable="false"
      class="upgrade-banner"
    >
      <template #title>
        检测到私有仓库中当前标签的后端或前端镜像与运行环境摘要不一致，建议按《部署与升级手册》执行镜像升级与冒烟验证。
      </template>
    </el-alert>

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
          <el-descriptions-item label="Git 提交">
            {{ runtimeInfo.commitSha || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="升级检测方式">
            {{ runtimeInfo.upgradeModeDescription || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card
        shadow="never"
        class="info-card"
      >
        <template #header>
          <div class="card-header">
            <span>镜像更新检测</span>
            <el-button
              text
              :loading="imageUpgradeLoading"
              @click="loadImageUpgradeStatus"
            >
              检测
            </el-button>
          </div>
        </template>
        <p class="image-upgrade-intro">
          与系统配置「镜像升级检测」中的仓库地址及镜像路径一致；标签与产品版本（APP_VERSION）相同，未设置时为 latest。运行中摘要由环境变量 RUNNING_BACKEND_DIGEST / RUNNING_FRONTEND_DIGEST 提供。
        </p>
        <el-descriptions
          v-if="imageUpgrade.registryBaseUrl"
          :column="1"
          border
          class="image-upgrade-meta"
        >
          <el-descriptions-item label="仓库根地址">
            {{ imageUpgrade.registryBaseUrl }}
          </el-descriptions-item>
          <el-descriptions-item label="比对标签">
            {{ imageUpgrade.imageTag || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="检测时间">
            {{ imageUpgrade.checkedAt || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <div
          v-for="c in imageUpgrade.components"
          :key="c.role"
          class="image-component"
        >
          <div class="image-component-head">
            <span>{{ c.role === 'BACKEND' ? '后端镜像' : '前端镜像' }}</span>
            <el-tag
              v-if="c.upgradeAvailable === true"
              type="danger"
              size="small"
            >
              有新镜像
            </el-tag>
            <el-tag
              v-else-if="c.upgradeAvailable === false"
              type="success"
              size="small"
            >
              已是最新
            </el-tag>
            <el-tag
              v-else
              type="info"
              size="small"
            >
              无法判定
            </el-tag>
          </div>
          <div class="image-component-body">
            <div>仓库路径：{{ c.repository }}</div>
            <div>远端摘要：{{ c.remoteDigest || '—' }}</div>
            <div>运行摘要：{{ c.runningDigest || '—' }}</div>
            <div class="image-component-msg">
              {{ c.message }}
            </div>
          </div>
        </div>
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
import { getDependencyStatus, getImageUpgradeStatus, getRuntimeInfo } from '@/api/config'

const frontendVersion = packageJson.version

const runtimeInfo = reactive({
  productVersion: '',
  backendVersion: '',
  buildTime: '',
  commitSha: '',
  upgradeModeDescription: ''
})

const imageUpgradeLoading = ref(false)
const imageUpgrade = reactive({
  registryBaseUrl: '',
  imageTag: '',
  checkedAt: '',
  components: [],
  upgradeRecommended: false
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
      commitSha: res?.data?.commitSha || '',
      upgradeModeDescription: res?.data?.upgradeModeDescription || ''
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

const loadImageUpgradeStatus = async () => {
  imageUpgradeLoading.value = true
  try {
    const res = await getImageUpgradeStatus()
    const data = res?.data || {}
    imageUpgrade.registryBaseUrl = data.registryBaseUrl || ''
    imageUpgrade.imageTag = data.imageTag || ''
    imageUpgrade.checkedAt = data.checkedAt || ''
    imageUpgrade.components = Array.isArray(data.components) ? data.components : []
    imageUpgrade.upgradeRecommended = Boolean(data.upgradeRecommended)
  } catch (error) {
    ElMessage.error('镜像更新检测失败')
    imageUpgrade.components = []
  } finally {
    imageUpgradeLoading.value = false
  }
}

onMounted(() => {
  loadRuntimeInfo()
  loadDependencyStatus()
  loadImageUpgradeStatus()
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

.upgrade-banner {
  border-radius: 10px;
}

.image-upgrade-intro {
  margin: 0 0 16px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.image-upgrade-meta {
  margin-bottom: 16px;
}

.image-component {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.image-component-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  color: #303133;
}

.image-component-body {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.image-component-msg {
  margin-top: 6px;
  color: #909399;
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
