<template>
  <div class="restore-center">
    <div class="page-header">
      <div>
        <h1>恢复中心</h1>
        <p>
          恢复中心用于在新环境中重新接管电子档案系统。当前已支持本地目录与 SMB/NAS 恢复源浏览、备份集校验、维护模式控制和恢复任务台账。
        </p>
      </div>
    </div>

    <el-alert
      title="高危能力"
      type="warning"
      :closable="false"
      description="恢复操作只能由系统管理员执行，正式恢复前必须进入维护模式并完成完整性校验。"
    />

    <el-card shadow="never">
      <div class="maintenance-bar">
        <div>
          <div class="toolbar-title">维护模式</div>
          <div class="toolbar-desc">
            {{ maintenanceStatus.message || '恢复前需进入维护模式，避免业务写入干扰恢复过程。' }}
          </div>
        </div>
        <div class="maintenance-actions">
          <el-tag :type="maintenanceStatus.enabled ? 'warning' : 'info'">
            {{ maintenanceStatus.enabled ? '已开启' : '未开启' }}
          </el-tag>
          <el-button
            :type="maintenanceStatus.enabled ? 'danger' : 'primary'"
            @click="toggleMaintenance"
          >
            {{ maintenanceStatus.enabled ? '退出维护模式' : '进入维护模式' }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <div class="source-toolbar">
        <div>
          <div class="toolbar-title">恢复源浏览</div>
          <div class="toolbar-desc">选择已配置的备份目标，读取其中可识别的本地或 SMB 备份集。</div>
        </div>
        <div class="toolbar-actions">
          <el-select
            v-model="selectedTargetId"
            clearable
            filterable
            placeholder="全部备份目标"
            class="target-select"
          >
            <el-option
              v-for="target in backupTargets"
              :key="target.id"
              :label="`${target.name} (${target.targetType})`"
              :value="target.id"
            />
          </el-select>
          <el-button @click="loadBackupSets">刷新备份集</el-button>
        </div>
      </div>
      <el-table :data="backupSets" v-loading="loadingSets" border>
        <el-table-column prop="backupSetName" label="备份集" min-width="180" />
        <el-table-column prop="targetName" label="来源目标" min-width="140" />
        <el-table-column prop="targetType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.targetType === 'LOCAL' ? 'success' : 'warning'">{{ row.targetType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column prop="databaseMode" label="数据库" width="120" />
        <el-table-column prop="fileCount" label="档案数" width="90" />
        <el-table-column prop="objectCount" label="对象数" width="90" />
        <el-table-column prop="totalBytes" label="大小" min-width="120">
          <template #default="{ row }">
            {{ formatBytes(row.totalBytes) }}
          </template>
        </el-table-column>
        <el-table-column prop="verifyStatus" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="setTagType(row.verifyStatus)">{{ row.verifyStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              text
              type="primary"
              :disabled="row.verifyStatus !== 'READY'"
              @click="openRestoreDialog(row)"
            >
              执行恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!backupSets.length && !loadingSets" description="当前未识别到可恢复备份集" />
    </el-card>

    <el-row :gutter="20">
      <el-col :lg="14" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>恢复任务台账</span>
          </template>
          <el-table :data="restoreJobs" v-loading="loading" border>
            <el-table-column prop="restoreNo" label="恢复单号" min-width="160" />
            <el-table-column prop="targetName" label="恢复源" min-width="160" />
            <el-table-column prop="backupSetName" label="备份集" min-width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="verifyStatus" label="校验" width="100" />
            <el-table-column prop="rebuildIndexStatus" label="索引" width="110" />
            <el-table-column prop="createdAt" label="创建时间" min-width="170" />
            <el-table-column label="报告" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :disabled="!row.restoreReport" @click="openReport(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!restoreJobs.length && !loading" description="暂无恢复任务记录" />
        </el-card>
      </el-col>

      <el-col :lg="10" :xs="24">
        <el-card shadow="never">
          <template #header>
            <span>恢复执行原则</span>
          </template>
          <ol class="step-list">
            <li>确认目标运行环境已准备就绪。</li>
            <li>配置备份源并读取可恢复备份集。</li>
            <li>校验 manifest、校验码与文件完整性。</li>
            <li>进入维护模式，停止业务写入。</li>
            <li>恢复数据库、电子文件、系统配置。</li>
            <li>重建 Elasticsearch 索引后开放服务。</li>
          </ol>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="restoreDialogVisible" title="执行系统恢复" width="620px">
      <div v-if="selectedBackupSet" class="restore-summary">
        <div><span>备份集：</span>{{ selectedBackupSet.backupSetName }}</div>
        <div><span>来源目标：</span>{{ selectedBackupSet.targetName }}</div>
        <div><span>目标类型：</span>{{ selectedBackupSet.targetType }}</div>
        <div><span>创建时间：</span>{{ selectedBackupSet.createdAt || '-' }}</div>
        <div class="path-row"><span>备份路径：</span>{{ selectedBackupSet.backupSetPath || '-' }}</div>
      </div>

      <el-form label-width="150px">
        <el-form-item label="恢复数据库">
          <el-switch v-model="restoreForm.restoreDatabase" />
        </el-form-item>
        <el-form-item label="恢复电子文件">
          <el-switch v-model="restoreForm.restoreFiles" />
        </el-form-item>
        <el-form-item label="恢复系统配置">
          <el-switch v-model="restoreForm.restoreConfig" />
        </el-form-item>
        <el-form-item label="重建搜索索引">
          <el-switch v-model="restoreForm.rebuildIndex" />
        </el-form-item>
        <el-form-item label="成功后退出维护">
          <el-switch v-model="restoreForm.exitMaintenanceAfterSuccess" />
        </el-form-item>
        <el-form-item label="确认口令">
          <el-input v-model="restoreForm.confirmationText" placeholder="请输入 RESTORE" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="restoreDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="runningRestore" @click="handleRunRestore">
          开始恢复
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialogVisible" title="恢复报告" width="680px">
      <pre class="report-view">{{ selectedReport }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBackupSets,
  getBackupTargets,
  getRestoreJobs,
  getRestoreMaintenanceStatus,
  runRestore,
  updateRestoreMaintenanceStatus
} from '@/api/backup'

const loading = ref(false)
const loadingSets = ref(false)
const restoreJobs = ref([])
const backupSets = ref([])
const backupTargets = ref([])
const selectedTargetId = ref(null)
const maintenanceStatus = ref({})
const restoreDialogVisible = ref(false)
const runningRestore = ref(false)
const selectedBackupSet = ref(null)
const reportDialogVisible = ref(false)
const selectedReport = ref('')
const restoreForm = ref({
  restoreDatabase: true,
  restoreFiles: true,
  restoreConfig: true,
  rebuildIndex: true,
  exitMaintenanceAfterSuccess: true,
  confirmationText: ''
})

onMounted(async () => {
  await Promise.all([loadRestoreJobs(), loadTargets(), loadBackupSets(), loadMaintenanceStatus()])
})

const tagType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

const setTagType = (status) => {
  if (status === 'READY') return 'success'
  if (status === 'INCOMPLETE') return 'warning'
  return 'info'
}

const loadRestoreJobs = async () => {
  loading.value = true
  try {
    const res = await getRestoreJobs({ pageNum: 1, pageSize: 10 })
    restoreJobs.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

const loadTargets = async () => {
  const res = await getBackupTargets()
  backupTargets.value = res.data || []
}

const loadMaintenanceStatus = async () => {
  const res = await getRestoreMaintenanceStatus()
  maintenanceStatus.value = res.data || {}
}

const loadBackupSets = async () => {
  loadingSets.value = true
  try {
    const params = selectedTargetId.value ? { targetId: selectedTargetId.value } : {}
    const res = await getBackupSets(params)
    backupSets.value = res.data || []
  } finally {
    loadingSets.value = false
  }
}

watch(selectedTargetId, () => {
  loadBackupSets()
})

const toggleMaintenance = async () => {
  const nextEnabled = !maintenanceStatus.value.enabled
  await updateRestoreMaintenanceStatus(nextEnabled)
  ElMessage.success(nextEnabled ? '已进入维护模式' : '已退出维护模式')
  await loadMaintenanceStatus()
}

const openRestoreDialog = (row) => {
  selectedBackupSet.value = row
  restoreForm.value = {
    restoreDatabase: true,
    restoreFiles: true,
    restoreConfig: true,
    rebuildIndex: true,
    exitMaintenanceAfterSuccess: true,
    confirmationText: ''
  }
  restoreDialogVisible.value = true
}

const handleRunRestore = async () => {
  if (!selectedBackupSet.value) return
  await ElMessageBox.confirm(
    `确认对备份集“${selectedBackupSet.value.backupSetName}”执行恢复吗？此操作用于新环境接管，属于高危操作。`,
    '恢复确认',
    { type: 'warning' }
  )
  runningRestore.value = true
  try {
    await runRestore({
      targetId: selectedBackupSet.value.targetId,
      backupSetName: selectedBackupSet.value.backupSetName,
      ...restoreForm.value
    })
    ElMessage.success('恢复任务已启动')
    restoreDialogVisible.value = false
    await Promise.all([loadRestoreJobs(), loadMaintenanceStatus()])
  } finally {
    runningRestore.value = false
  }
}

const openReport = (row) => {
  selectedReport.value = formatReport(row.restoreReport)
  reportDialogVisible.value = true
}

const formatBytes = (value) => {
  if (!value) return '-'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`
}

const formatReport = (value) => {
  if (!value) return '暂无恢复报告'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}
</script>

<style lang="scss" scoped>
.restore-center {
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

.maintenance-bar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.source-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
}

.toolbar-desc {
  margin-top: 4px;
  color: #6b7280;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.maintenance-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.target-select {
  width: 240px;
}

.page-header p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.step-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
}

.restore-summary {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f7fafc;
  color: #4b5563;
}

.restore-summary span {
  color: #111827;
  font-weight: 600;
}

.path-row {
  word-break: break-all;
}

.report-view {
  margin: 0;
  padding: 16px;
  border-radius: 14px;
  background: #0f172a;
  color: #e5eefc;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .maintenance-bar,
  .source-toolbar,
  .toolbar-actions,
  .maintenance-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .target-select {
    width: 100%;
  }
}
</style>
