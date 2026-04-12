<template>
  <div class="backup-center">
    <div class="page-header">
      <div>
        <h1>备份中心</h1>
        <p>
          为电子档案系统配置可恢复的备份目标。当前已支持本地目录与 SMB/NAS 的目标管理、真实验证、备份执行和备份集浏览。
        </p>
      </div>
      <el-button type="primary" @click="openCreateDialog">
        新增备份目标
      </el-button>
    </div>

    <div class="overview-grid">
      <el-card v-for="item in overviewCards" :key="item.label" class="overview-card" shadow="never">
        <div class="overview-value">{{ item.value }}</div>
        <div class="overview-label">{{ item.label }}</div>
      </el-card>
    </div>

    <el-row :gutter="20">
      <el-col :lg="14" :xs="24">
        <el-card shadow="never">
          <template #header>
            <div class="section-header">
              <span>备份目标</span>
              <el-tag type="info">阶段：{{ overview.currentPhase || 'FOUNDATION' }}</el-tag>
            </div>
          </template>

          <el-table :data="targets" v-loading="loadingTargets" border>
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="targetType" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="row.targetType === 'LOCAL' ? 'success' : 'warning'">
                  {{ row.targetType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="目标地址" min-width="260">
              <template #default="{ row }">
                <span v-if="row.targetType === 'LOCAL'">{{ row.localPath }}</span>
                <span v-else>{{ row.smbHost }} / {{ row.smbShare }} / {{ row.smbSubPath || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="验证状态" width="120">
              <template #default="{ row }">
                <el-tag :type="verifyTagType(row.verifyStatus)">
                  {{ row.verifyStatus || 'PENDING' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" disabled />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button text type="success" @click="handleVerify(row)">验证</el-button>
                <el-button text type="warning" @click="handleRun(row)">执行备份</el-button>
                <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :lg="10" :xs="24">
        <el-card shadow="never" class="guideline-card">
          <template #header>
            <span>第一阶段说明</span>
          </template>
          <ul class="plain-list">
            <li>支持本地目录与 SMB/NAS 备份目标配置。</li>
            <li>本地目录可执行真实可写性验证。</li>
            <li>SMB 目标可执行共享连通、目录创建、探针写入与删除校验。</li>
            <li>手动备份会生成完整备份集，可用于后续恢复中心选择恢复源。</li>
          </ul>
        </el-card>

        <el-card shadow="never" class="job-card">
          <template #header>
            <span>最近备份任务</span>
          </template>
          <el-empty v-if="!backupJobs.length" description="暂无备份任务记录" />
          <div v-else class="job-list">
            <div v-for="job in backupJobs" :key="job.id" class="job-item">
              <div class="job-title">{{ job.backupNo }}</div>
              <div class="job-meta">{{ job.targetName || '未命名目标' }} · {{ job.status }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑备份目标' : '新增备份目标'" width="640px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="目标名称">
          <el-input v-model="form.name" placeholder="例如：律所 NAS 备份" />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-radio-group v-model="form.targetType">
            <el-radio label="LOCAL">本地目录</el-radio>
            <el-radio label="SMB">SMB / NAS</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <template v-if="form.targetType === 'LOCAL'">
          <el-form-item label="本地目录">
            <el-input v-model="form.localPath" placeholder="/data/archive-backups" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="SMB 主机">
            <el-input v-model="form.smbHost" placeholder="192.168.50.5" />
          </el-form-item>
          <el-form-item label="共享名称">
            <el-input v-model="form.smbShare" placeholder="archive-backup" />
          </el-form-item>
          <el-form-item label="共享子目录">
            <el-input v-model="form.smbSubPath" placeholder="law-firm-archive/prod" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="form.smbUsername" placeholder="backup-user" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.smbPassword" show-password placeholder="编辑时留空表示不更新密码" />
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createBackupTarget,
  deleteBackupTarget,
  getBackupJobs,
  getBackupOverview,
  getBackupTargets,
  runBackup,
  updateBackupTarget,
  verifyBackupTarget
} from '@/api/backup'

const loadingTargets = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const targets = ref([])
const backupJobs = ref([])
const overview = ref({})

const form = reactive({
  name: '',
  targetType: 'LOCAL',
  enabled: true,
  localPath: '',
  smbHost: '',
  smbShare: '',
  smbUsername: '',
  smbPassword: '',
  smbSubPath: '',
  remarks: ''
})

const overviewCards = computed(() => [
  { label: '启用目标', value: overview.value.enabledTargetCount ?? 0 },
  { label: '已验证目标', value: overview.value.verifiedTargetCount ?? 0 },
  { label: '待执行备份', value: overview.value.pendingBackupJobs ?? 0 },
  { label: '7天内恢复失败', value: overview.value.recentRestoreFailures ?? 0 }
])

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loadingTargets.value = true
  try {
    const [overviewRes, targetsRes, jobsRes] = await Promise.all([
      getBackupOverview(),
      getBackupTargets(),
      getBackupJobs({ pageNum: 1, pageSize: 5 })
    ])
    overview.value = overviewRes.data || {}
    targets.value = targetsRes.data || []
    backupJobs.value = jobsRes.data?.records || []
  } finally {
    loadingTargets.value = false
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    name: '',
    targetType: 'LOCAL',
    enabled: true,
    localPath: '',
    smbHost: '',
    smbShare: '',
    smbUsername: '',
    smbPassword: '',
    smbSubPath: '',
    remarks: ''
  })
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    targetType: row.targetType,
    enabled: row.enabled,
    localPath: row.localPath || '',
    smbHost: row.smbHost || '',
    smbShare: row.smbShare || '',
    smbUsername: row.smbUsername || '',
    smbPassword: '',
    smbSubPath: row.smbSubPath || '',
    remarks: row.remarks || ''
  })
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (editingId.value) {
      await updateBackupTarget(editingId.value, form)
    } else {
      await createBackupTarget(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

const handleVerify = async (row) => {
  await verifyBackupTarget(row.id)
  ElMessage.success('验证已完成')
  await loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除备份目标“${row.name}”吗？`, '删除确认', { type: 'warning' })
  await deleteBackupTarget(row.id)
  ElMessage.success('已删除')
  await loadData()
}

const handleRun = async (row) => {
  await ElMessageBox.confirm(`确认立即对“${row.name}”执行一次手动备份吗？`, '执行确认', { type: 'warning' })
  await runBackup(row.id)
  ElMessage.success('备份任务已启动')
  await loadData()
}

const verifyTagType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}
</script>

<style lang="scss" scoped>
.backup-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  max-width: 760px;
  line-height: 1.6;
  color: #606266;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  border-radius: 10px;

  :deep(.el-card__body) {
    min-height: 116px;
    padding: 22px 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .overview-value {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
  }

  .overview-label {
    margin-top: 8px;
    font-size: 14px;
    color: #909399;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guideline-card,
.job-card {
  margin-bottom: 20px;
  border-radius: 10px;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
  color: #4b5563;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.job-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7fafc;
}

.job-title {
  font-weight: 600;
}

.job-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

@media (max-width: 1024px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
