<template>
  <div class="backup-center">
    <div
      v-if="!props.embedded"
      class="page-header"
    >
      <div>
        <h1>备份中心</h1>
        <p>
          在此维护备份目标并发起验证与手动备份；恢复请到「备份恢复中心」选择备份集。
        </p>
      </div>
      <el-tooltip
        v-if="targetDialogActionBlockedReason"
        :content="targetDialogActionBlockedReason"
        placement="top"
      >
        <span class="action-tooltip-trigger">
          <el-button
            type="primary"
            :disabled="true"
          >
            新增备份目标
          </el-button>
        </span>
      </el-tooltip>
      <el-button
        v-else
        type="primary"
        :disabled="isTargetDialogActive"
        @click="openCreateDialog"
      >
        新增备份目标
      </el-button>
    </div>

    <div class="overview-grid">
      <el-card
        v-for="item in overviewCards"
        :key="item.label"
        class="overview-card"
        shadow="never"
      >
        <div class="overview-value">
          {{ item.value }}
        </div>
        <div class="overview-label">
          {{ item.label }}
        </div>
      </el-card>
    </div>

    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <div class="section-header">
          <div class="section-header-main">
            <span>备份目标</span>
            <el-tag
              v-if="overview.currentPhase"
              type="info"
              size="small"
            >
              {{ currentPhaseLabel }}
            </el-tag>
          </div>
          <el-tooltip
            v-if="props.embedded && targetDialogActionBlockedReason"
            :content="targetDialogActionBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger">
              <el-button
                type="primary"
                size="small"
                :disabled="true"
              >
                新增备份目标
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else-if="props.embedded"
            type="primary"
            size="small"
            :disabled="isTargetDialogActive"
            @click="openCreateDialog"
          >
            新增备份目标
          </el-button>
        </div>
      </template>
      <div
        v-if="targets.length"
        class="target-readiness-grid"
      >
        <el-card
          v-for="item in targetReadinessCards"
          :key="item.label"
          shadow="never"
          class="target-readiness-card"
        >
          <div class="readiness-value">
            {{ item.value }}
          </div>
          <div class="readiness-label">
            {{ item.label }}
          </div>
          <div
            v-if="item.hint"
            class="readiness-hint"
          >
            {{ item.hint }}
          </div>
        </el-card>
      </div>
      <el-alert
        v-if="backupTargetSetupGuidance"
        :title="backupTargetSetupGuidance.title"
        :type="backupTargetSetupGuidance.type"
        :closable="false"
        :description="backupTargetSetupGuidance.description"
        class="target-readiness-alert"
      />
      <el-alert
        v-else-if="unverifiedEnabledTargetCount > 0"
        title="存在已启用但未通过验证的备份目标"
        type="info"
        :closable="false"
        :description="`当前有 ${unverifiedEnabledTargetCount} 个启用目标尚未完成验证；建议先执行验证，再发起手动备份。`"
        class="target-readiness-alert"
      />
      <p
        v-if="targets.length"
        class="table-scroll-hint"
      >
        表格字段较多，窄屏设备可左右滑动查看更多列。
      </p>

      <el-table
        v-loading="loadingTargets"
        :data="targets"
        border
        table-layout="auto"
      >
        <template #empty>
          <el-empty :description="backupTargetsEmptyState.description">
            <el-tooltip
              v-if="backupTargetsEmptyState.showAction && targetDialogActionBlockedReason"
              :content="targetDialogActionBlockedReason"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  type="primary"
                  :disabled="true"
                >
                  新增备份目标
                </el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else-if="backupTargetsEmptyState.showAction"
              type="primary"
              :disabled="isTargetDialogActive"
              @click="openCreateDialog"
            >
              新增备份目标
            </el-button>
          </el-empty>
        </template>
        <el-table-column
          prop="name"
          label="名称"
          min-width="140"
        />
        <el-table-column
          v-if="!isCompactLayout"
          prop="targetType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.targetType === 'LOCAL' ? 'success' : 'warning'">
              {{ backupTargetTypeLabel(row.targetType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="目标地址"
          min-width="220"
        >
          <template #default="{ row }">
            <span>{{ row.displayAddress || row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="验证状态"
          min-width="220"
        >
          <template #default="{ row }">
            <div class="verify-cell">
              <el-tag :type="verifyTagType(row.verifyStatus)">
                {{ backupTargetVerifyStatusLabel(row.verifyStatus) }}
              </el-tag>
              <span class="verify-message">
                {{ row.verifyMessage || '-' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="!isCompactLayout"
          label="启用"
          width="88"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              disabled
            />
          </template>
        </el-table-column>
        <el-table-column
          label="下一步"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="verify-cell">
              <el-tag :type="backupNextStepTagType(row)">
                {{ backupNextStepLabel(row) }}
              </el-tag>
              <span class="verify-message">
                {{ backupNextStepHint(row) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          min-width="200"
          fixed="right"
        >
          <template #default="{ row }">
            <el-tooltip
              :disabled="!tableTargetActionBlockedReason(row.id)"
              :content="tableTargetActionBlockedReason(row.id)"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  text
                  type="primary"
                  :loading="isLoadingEditTarget(row.id)"
                  :disabled="isTargetDialogActive || isBusyTarget(row.id)"
                  @click="openEditDialog(row)"
                >
                  编辑
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :disabled="!tableTargetActionBlockedReason(row.id)"
              :content="tableTargetActionBlockedReason(row.id)"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  text
                  type="success"
                  :loading="isVerifyingTarget(row.id)"
                  :disabled="isTargetDialogActive || isBusyTarget(row.id)"
                  @click="handleVerify(row)"
                >
                  验证
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :disabled="!backupRunActionBlockedReason(row)"
              :content="backupRunActionBlockedReason(row)"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  text
                  type="warning"
                  :loading="isRunningTarget(row.id)"
                  :disabled="!canRunBackup(row) || isTargetDialogActive || isBusyTarget(row.id)"
                  @click="handleRun(row)"
                >
                  执行备份
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :disabled="!tableTargetActionBlockedReason(row.id)"
              :content="tableTargetActionBlockedReason(row.id)"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  text
                  type="danger"
                  :loading="isDeletingTarget(row.id)"
                  :disabled="isTargetDialogActive || isBusyTarget(row.id)"
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card
      shadow="never"
      class="section-card"
    >
      <template #header>
        <span>最近备份任务</span>
      </template>
      <el-alert
        v-if="failedBackupJobCount > 0 || followUpBackupJobCount > 0"
        :title="backupJobAttentionTitle"
        :type="failedBackupJobCount > 0 ? 'error' : 'warning'"
        :closable="false"
        :description="backupJobAttentionDescription"
        class="job-attention-alert"
      />
      <el-empty
        v-if="!backupJobs.length"
        description="暂无备份任务记录"
      />
      <div
        v-else
        class="job-list"
      >
        <div
          v-for="job in backupJobs"
          :key="job.backupNo"
          :class="['job-item', `job-item--${backupJobSeverity(job)}`]"
        >
          <div class="job-head">
            <div class="job-title">
              {{ job.backupNo }}
            </div>
            <el-tag
              :type="backupJobSeverityTagType(job)"
              size="small"
            >
              {{ backupJobSeverityLabel(job) }}
            </el-tag>
          </div>
          <div class="job-meta">
            {{ job.targetName || '未命名目标' }} · {{ backupJobStatusLabel(job) }}
          </div>
          <div class="job-desc">
            {{ backupJobSummary(job) }}
          </div>
        </div>
      </div>
    </el-card>

    <section
      class="backup-guidelines"
      aria-labelledby="backup-guidelines-title"
    >
      <div class="guidelines-header">
        <h2
          id="backup-guidelines-title"
          class="guidelines-title"
        >
          范围与校验说明
        </h2>
        <p class="guidelines-intro">
          建议按“配置目标 -> 执行验证 -> 发起备份”的顺序操作，先确认目标可写，再生成恢复可用的完整备份集。
        </p>
      </div>
      <div class="guideline-steps">
        <article
          v-for="item in backupGuidelineSteps"
          :key="item.title"
          class="guideline-step"
        >
          <div class="guideline-step-index">
            {{ item.step }}
          </div>
          <div class="guideline-step-body">
            <h3 class="guideline-step-title">
              {{ item.title }}
            </h3>
            <p class="guideline-step-desc">
              {{ item.description }}
            </p>
            <p class="guideline-step-note">
              {{ item.note }}
            </p>
          </div>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑备份目标' : '新增备份目标'"
      :width="isCompactLayout ? 'calc(100vw - 24px)' : '640px'"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      class="backup-target-dialog"
    >
      <el-form
        :model="form"
        :label-width="isCompactLayout ? 'auto' : '120px'"
        :label-position="isCompactLayout ? 'top' : 'right'"
      >
        <el-form-item label="目标名称">
          <el-input
            v-model="form.name"
            placeholder="例如：律所 NAS 备份"
          />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-radio-group v-model="form.targetType">
            <el-radio label="LOCAL">
              本地目录
            </el-radio>
            <el-radio label="SMB">
              SMB / NAS
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>

        <template v-if="form.targetType === 'LOCAL'">
          <div class="form-section">
            <div class="form-section-title">
              备份位置
            </div>
            <p class="form-section-desc">
              指定本机或挂载目录中用于保存备份集的路径。
            </p>
          </div>
          <el-form-item label="本地目录">
            <el-input
              v-model="form.localPath"
              placeholder="/data/archive-backups"
            />
          </el-form-item>
        </template>

        <template v-else>
          <div class="form-section">
            <div class="form-section-title">
              SMB 连接配置
            </div>
            <p class="form-section-desc">
              先填写共享地址与路径信息，再补充访问凭据。
            </p>
          </div>
          <el-form-item label="SMB 主机">
            <el-input
              v-model="form.smbHost"
              placeholder="192.168.50.5"
            />
          </el-form-item>
          <el-form-item label="SMB 端口">
            <el-input-number
              v-model="form.smbPort"
              :min="1"
              :max="65535"
              :step="1"
              controls-position="right"
              class="port-input"
            />
          </el-form-item>
          <el-form-item label="共享名称">
            <el-input
              v-model="form.smbShare"
              placeholder="archive-backup"
            />
          </el-form-item>
          <el-form-item label="共享子目录">
            <el-input
              v-model="form.smbSubPath"
              placeholder="law-firm-archive/prod"
            />
          </el-form-item>
          <div class="form-section">
            <div class="form-section-title">
              访问凭据
            </div>
            <p class="form-section-desc">
              使用具备读写权限的共享账号；编辑时密码可留空以保持不变。
            </p>
          </div>
          <el-form-item label="用户名">
            <el-input
              v-model="form.smbUsername"
              placeholder="backup-user"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.smbPassword"
              show-password
              placeholder="编辑时留空表示不更新密码"
            />
          </el-form-item>
        </template>

        <el-form-item label="备注">
          <el-input
            v-model="form.remarks"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-alert
          v-if="backupTargetSubmitBlockedReason"
          :title="backupTargetSubmitBlockedReason"
          type="warning"
          :closable="false"
          class="backup-target-submit-alert"
        />
      </el-form>

      <template #footer>
        <div class="dialog-footer-actions">
          <el-button
            :disabled="saving"
            @click="dialogVisible = false"
          >
            取消
          </el-button>
          <el-tooltip
            v-if="backupTargetSubmitBlockedReason"
            :content="backupTargetSubmitBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger">
              <el-button
                type="primary"
                :loading="saving"
                :disabled="true"
              >
                保存
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            type="primary"
            :loading="saving"
            :disabled="saving"
            @click="handleSave"
          >
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createBackupTarget,
  deleteBackupTarget,
  getBackupJobs,
  getBackupOverview,
  getBackupTarget,
  getBackupTargets,
  runBackup,
  updateBackupTarget,
  verifyBackupTarget
} from '@/api/backup'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const loadingTargets = ref(false)
const isCompactLayout = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const editingOriginalTargetType = ref('')
const loadingEditTargetId = ref(null)
const verifyingTargetIds = ref([])
const runningTargetIds = ref([])
const deletingTargetIds = ref([])
const targets = ref([])
const backupJobs = ref([])
const overview = ref({})
const hasLoadedTargets = ref(false)
let editDialogRequestId = 0
let backupPageRequestId = 0

const form = reactive({
  name: '',
  targetType: 'LOCAL',
  enabled: true,
  localPath: '',
  smbHost: '',
  smbPort: 445,
  smbShare: '',
  smbUsername: '',
  smbPassword: '',
  smbSubPath: '',
  remarks: ''
})

const phaseLabelMap = {
  RUNNING: '任务处理中',
  SETUP_REQUIRED: '待配置',
  VERIFY_REQUIRED: '待验证',
  READY: '已就绪'
}

const currentPhaseLabel = computed(() => phaseLabelMap[overview.value.currentPhase] || overview.value.currentPhase)
const isTargetDialogActive = computed(() =>
  dialogVisible.value || saving.value || loadingEditTargetId.value !== null
)
const targetDialogActionBlockedReason = computed(() => {
  if (saving.value) {
    return '当前正在保存备份目标，请稍后'
  }
  if (loadingEditTargetId.value !== null) {
    return '正在加载备份目标详情，请稍后'
  }
  if (dialogVisible.value) {
    return '请先关闭当前备份目标弹窗'
  }
  return ''
})
const enabledTargetCount = computed(() => targets.value.filter(target => target.enabled === true).length)
const verifiedEnabledTargetCount = computed(() =>
  targets.value.filter(target => target.enabled === true && target.verifyStatus === 'SUCCESS').length
)
const unverifiedEnabledTargetCount = computed(() =>
  targets.value.filter(target => target.enabled === true && target.verifyStatus !== 'SUCCESS').length
)
const runnableTargetCount = computed(() => targets.value.filter(canRunBackup).length)
const overviewCards = computed(() => [
  { label: '启用目标', value: overview.value.enabledTargetCount ?? 0 },
  { label: '已验证目标', value: overview.value.verifiedTargetCount ?? 0 },
  { label: '待执行/进行中备份', value: overview.value.pendingBackupJobs ?? 0 },
  { label: '7天内备份失败', value: overview.value.recentBackupFailures ?? 0 },
  { label: '7天内恢复失败', value: overview.value.recentRestoreFailures ?? 0 }
])
const targetReadinessCards = computed(() => [
  {
    label: '已配置目标',
    value: targets.value.length,
    hint: targets.value.length ? '包含本地目录与 SMB / NAS 目标' : '当前还没有备份目标'
  },
  {
    label: '已启用目标',
    value: enabledTargetCount.value,
    hint: enabledTargetCount.value ? '启用后才允许发起手动备份' : '请先启用至少一个目标'
  },
  {
    label: '已启用且已验证',
    value: verifiedEnabledTargetCount.value,
    hint: verifiedEnabledTargetCount.value ? '这些目标更适合直接执行备份' : '当前还没有通过验证的启用目标'
  },
  {
    label: '当前可直接执行',
    value: runnableTargetCount.value,
    hint: runnableTargetCount.value ? '满足当前手动备份开启条件' : '当前无可执行备份目标'
  }
])
const backupTargetSetupGuidance = computed(() => {
  if (!hasLoadedTargets.value) {
    return null
  }
  if (!enabledTargetCount.value) {
    return {
      title: '当前没有可执行备份的启用目标',
      type: 'warning',
      description: '已存在备份目标，但都处于停用状态；请先启用至少一个目标，再执行验证或手动备份。'
    }
  }
  return null
})
const backupTargetsEmptyState = computed(() => {
  if (!hasLoadedTargets.value) {
    return {
      description: '正在加载备份目标...',
      showAction: false
    }
  }
  if (targets.value.length === 0) {
    return {
      description: '当前还没有备份目标，请先新增一个目标',
      showAction: true
    }
  }
  return {
    description: '当前没有匹配的备份目标',
    showAction: false
  }
})
const backupTargetSubmitBlockedReason = computed(() => {
  if (!form.name?.trim()) {
    return '请输入目标名称'
  }
  if (form.targetType === 'LOCAL' && !form.localPath?.trim()) {
    return '本地目录备份目标必须填写目录路径'
  }
  if (form.targetType === 'SMB' && !form.smbHost?.trim()) {
    return 'SMB 备份目标必须填写主机地址'
  }
  if (form.targetType === 'SMB' && !form.smbShare?.trim()) {
    return 'SMB 备份目标必须填写共享名称'
  }
  if (form.targetType === 'SMB' && !form.smbUsername?.trim()) {
    return 'SMB 备份目标必须填写用户名'
  }
  if (
    form.targetType === 'SMB'
    && !form.smbPassword?.trim()
    && (!editingId.value || editingOriginalTargetType.value !== 'SMB')
  ) {
    return '新增 SMB 备份目标必须填写密码'
  }
  return ''
})
const failedBackupJobCount = computed(() => backupJobs.value.filter(job => job?.status === 'FAILED').length)
const followUpBackupJobCount = computed(() =>
  backupJobs.value.filter(job => job?.status === 'SUCCESS' && job?.followUpRequired === true).length
)
const backupJobAttentionTitle = computed(() => {
  if (failedBackupJobCount.value > 0) {
    return '最近备份任务存在失败记录'
  }
  return '最近备份任务存在待处理项'
})
const backupJobAttentionDescription = computed(() => {
  if (failedBackupJobCount.value > 0 && followUpBackupJobCount.value > 0) {
    return `最近 5 条任务中有 ${failedBackupJobCount.value} 条失败，另有 ${followUpBackupJobCount.value} 条成功但需跟进。`
  }
  if (failedBackupJobCount.value > 0) {
    return `最近 5 条任务中有 ${failedBackupJobCount.value} 条失败，建议优先检查对应目标与状态消息。`
  }
  return `最近 5 条任务中有 ${followUpBackupJobCount.value} 条成功但需跟进，请检查状态消息中的后续处理提示。`
})
const backupGuidelineSteps = [
  {
    step: '01',
    title: '选择目标类型',
    description: '支持本地目录与 SMB / NAS 两类目标，可按部署环境选择可长期访问的备份位置。',
    note: '本地目标更适合单机或同机房场景；SMB / NAS 更适合集中存储与跨机恢复。'
  },
  {
    step: '02',
    title: '先执行真实验证',
    description: '本地目录会检查路径存在与可写；SMB 会校验共享可达、可建目录以及探针文件写入删除。',
    note: '验证通过后再执行手动备份，能减少生成不可恢复备份集的风险。'
  },
  {
    step: '03',
    title: '生成完整备份集',
    description: '手动备份会生成供“备份恢复中心”识别的完整备份集，并进入后续完整性校验流程。',
    note: '如果任务成功但提示需跟进，请优先检查最近任务列表中的状态消息。'
  }
]

onMounted(() => {
  updateCompactLayout()
  window.addEventListener('resize', updateCompactLayout)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateCompactLayout)
})

const updateCompactLayout = () => {
  isCompactLayout.value = window.innerWidth <= 640
}

const refreshBackupPageAfterAction = async () => {
  try {
    await loadData()
  } catch {
    ElMessage.warning('列表刷新失败，已保留最近一次成功数据，请稍后手动刷新')
  }
}

const loadData = async () => {
  const requestId = ++backupPageRequestId
  loadingTargets.value = true
  try {
    const [overviewRes, targetsRes, jobsRes] = await Promise.allSettled([
      getBackupOverview(),
      getBackupTargets(),
      getBackupJobs({ pageNum: 1, pageSize: 5 })
    ])
    let hasFailedSection = false

    if (overviewRes.status === 'fulfilled') {
      if (requestId === backupPageRequestId) {
        overview.value = overviewRes.value.data || {}
      }
    } else {
      hasFailedSection = true
    }

    if (targetsRes.status === 'fulfilled') {
      if (requestId === backupPageRequestId) {
        targets.value = targetsRes.value.data || []
        hasLoadedTargets.value = true
      }
    } else {
      hasFailedSection = true
    }

    if (jobsRes.status === 'fulfilled') {
      if (requestId === backupPageRequestId) {
        backupJobs.value = jobsRes.value.data?.records || []
      }
    } else {
      hasFailedSection = true
    }

    if (requestId === backupPageRequestId && hasFailedSection) {
      ElMessage.warning('部分备份数据刷新失败，已保留最近一次成功数据，请稍后重试')
    }
  } finally {
    if (requestId === backupPageRequestId) {
      loadingTargets.value = false
    }
  }
}

const resetForm = () => {
  editingId.value = null
  editingOriginalTargetType.value = ''
  Object.assign(form, {
    name: '',
    targetType: 'LOCAL',
    enabled: true,
    localPath: '',
    smbHost: '',
    smbPort: 445,
    smbShare: '',
    smbUsername: '',
    smbPassword: '',
    smbSubPath: '',
    remarks: ''
  })
}

const isVerifyingTarget = (targetId) => verifyingTargetIds.value.includes(targetId)
const isRunningTarget = (targetId) => runningTargetIds.value.includes(targetId)
const isDeletingTarget = (targetId) => deletingTargetIds.value.includes(targetId)
const isLoadingEditTarget = (targetId) => loadingEditTargetId.value === targetId
const isBusyTarget = (targetId) =>
  isLoadingEditTarget(targetId) || isVerifyingTarget(targetId) || isRunningTarget(targetId) || isDeletingTarget(targetId)
const tableTargetActionBlockedReason = (targetId) => {
  if (loadingEditTargetId.value === targetId) {
    return '正在加载该备份目标详情，请稍后'
  }
  if (dialogVisible.value) {
    return '请先关闭当前备份目标弹窗'
  }
  if (isBusyTarget(targetId)) {
    return '该备份目标正在处理中，请稍后'
  }
  return ''
}
const backupRunActionBlockedReason = (target) => {
  return tableTargetActionBlockedReason(target?.id) || backupRunBlockedReason(target)
}

const trackTargetAction = async (targetIdsRef, targetId, action) => {
  if (targetIdsRef.value.includes(targetId)) {
    return
  }
  targetIdsRef.value = [...targetIdsRef.value, targetId]
  try {
    await action()
  } finally {
    targetIdsRef.value = targetIdsRef.value.filter(id => id !== targetId)
  }
}

const openCreateDialog = () => {
  editDialogRequestId += 1
  loadingEditTargetId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  if (loadingEditTargetId.value === row.id) {
    return
  }
  const requestId = ++editDialogRequestId
  loadingEditTargetId.value = row.id
  try {
    const res = await getBackupTarget(row.id)
    if (requestId !== editDialogRequestId) {
      return
    }
    const detail = res.data || {}
    editingId.value = row.id
    editingOriginalTargetType.value = detail.targetType || ''
    Object.assign(form, {
      name: detail.name,
      targetType: detail.targetType,
      enabled: detail.enabled,
      localPath: detail.localPath || '',
      smbHost: detail.smbHost || '',
      smbPort: detail.smbPort || 445,
      smbShare: detail.smbShare || '',
      smbUsername: detail.smbUsername || '',
      smbPassword: '',
      smbSubPath: detail.smbSubPath || '',
      remarks: detail.remarks || ''
    })
    dialogVisible.value = true
  } catch {
    if (requestId === editDialogRequestId) {
      ElMessage.warning('加载备份目标详情失败，请稍后重试')
    }
  } finally {
    if (requestId === editDialogRequestId) {
      loadingEditTargetId.value = null
    }
  }
}

watch(dialogVisible, (visible) => {
  if (visible) {
    return
  }
  resetForm()
})

const handleSave = async () => {
  if (!form.name?.trim()) {
    ElMessage.warning('请输入目标名称')
    return
  }
  if (form.targetType === 'LOCAL' && !form.localPath?.trim()) {
    ElMessage.warning('本地目录备份目标必须填写目录路径')
    return
  }
  if (form.targetType === 'SMB' && (!form.smbHost?.trim() || !form.smbShare?.trim())) {
    ElMessage.warning('SMB 备份目标必须填写主机和共享名称')
    return
  }
  if (form.targetType === 'SMB' && !form.smbUsername?.trim()) {
    ElMessage.warning('SMB 备份目标必须填写用户名')
    return
  }
  if (
    form.targetType === 'SMB' &&
    !form.smbPassword?.trim() &&
    (!editingId.value || editingOriginalTargetType.value !== 'SMB')
  ) {
    ElMessage.warning('新增 SMB 备份目标必须填写密码')
    return
  }

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      targetType: form.targetType,
      enabled: form.enabled,
      remarks: form.remarks?.trim() || ''
    }
    if (form.targetType === 'LOCAL') {
      payload.localPath = form.localPath?.trim() || ''
    } else {
      payload.smbHost = form.smbHost?.trim() || ''
      payload.smbPort = form.smbPort || 445
      payload.smbShare = form.smbShare?.trim() || ''
      payload.smbUsername = form.smbUsername?.trim() || ''
      payload.smbPassword = form.smbPassword || ''
      payload.smbSubPath = form.smbSubPath?.trim() || ''
    }
    let res
    if (editingId.value) {
      res = await updateBackupTarget(editingId.value, payload)
    } else {
      res = await createBackupTarget(payload)
    }
    const targetName = res?.data?.name || payload.name
    ElMessage.success(targetName ? `已保存备份目标：${targetName}` : '保存成功')
    dialogVisible.value = false
    await refreshBackupPageAfterAction()
  } finally {
    saving.value = false
  }
}

const handleVerify = async (row) => {
  await trackTargetAction(verifyingTargetIds, row.id, async () => {
    const res = await verifyBackupTarget(row.id)
    const verifyStatus = res?.data?.verifyStatus
    const verifyMessage = res?.data?.verifyMessage
    if (verifyStatus === 'SUCCESS') {
      ElMessage.success(verifyMessage || '验证成功')
    } else if (verifyStatus === 'FAILED') {
      ElMessage.warning(verifyMessage || '验证失败，请检查目标配置')
    } else if (verifyStatus === 'PENDING') {
      ElMessage.info(verifyMessage || '验证处理中，请稍后刷新查看结果')
    } else {
      ElMessage.info(verifyMessage || '验证状态未知，请刷新后确认结果')
    }
    await refreshBackupPageAfterAction()
  })
}

const handleDelete = async (row) => {
  await trackTargetAction(deletingTargetIds, row.id, async () => {
    try {
      await ElMessageBox.confirm(`确认删除备份目标“${row.name}”吗？`, '删除确认', { type: 'warning' })
    } catch {
      return
    }
    await deleteBackupTarget(row.id)
    ElMessage.success('已删除')
    await refreshBackupPageAfterAction()
  })
}

const handleRun = async (row) => {
  await trackTargetAction(runningTargetIds, row.id, async () => {
    const blockedReason = backupRunBlockedReason(row)
    if (blockedReason) {
      ElMessage.warning(blockedReason)
      return
    }
    try {
      await ElMessageBox.confirm(`确认立即对“${row.name}”执行一次手动备份吗？`, '执行确认', { type: 'warning' })
    } catch {
      return
    }
    const res = await runBackup(row.id)
    const backupNo = res?.data?.backupNo
    const status = res?.data?.status
    const statusMessage = res?.data?.statusMessage
    const followUpRequired = res?.data?.followUpRequired === true
    const summaryText = backupNo
      ? `${statusMessage || `备份任务状态：${status || 'UNKNOWN'}`}，任务编号：${backupNo}`
      : (statusMessage || `备份任务状态：${status || 'UNKNOWN'}`)
    if (followUpRequired) {
      ElMessage.warning(summaryText)
    } else if (status === 'SUCCESS') {
      ElMessage.success(summaryText)
    } else if (status === 'FAILED') {
      ElMessage.error(summaryText)
    } else {
      ElMessage.warning(summaryText)
    }
    await refreshBackupPageAfterAction()
  })
}

const verifyTagType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

const backupTargetVerifyStatusLabel = (status) => {
  if (status === 'SUCCESS') return '已通过'
  if (status === 'FAILED') return '未通过'
  if (status === 'PENDING') return '待验证'
  return status || '未知'
}

const backupTargetTypeLabel = (targetType) => {
  if (targetType === 'LOCAL') return '本地'
  if (targetType === 'SMB') return 'SMB'
  return targetType || '未知'
}

const backupRunBlockedReason = (target) => {
  if (target?.enabled !== true) {
    return '请先启用备份目标'
  }
  if (target?.verifyStatus !== 'SUCCESS') {
    return '请先完成目标验证，再执行手动备份'
  }
  return ''
}

const canRunBackup = (target) => {
  return !backupRunBlockedReason(target)
}

const backupNextStepLabel = (target) => {
  if (target?.enabled !== true) {
    return '先启用目标'
  }
  if (target?.verifyStatus !== 'SUCCESS') {
    return '先执行验证'
  }
  return '可直接备份'
}

const backupNextStepHint = (target) => {
  if (target?.enabled !== true) {
    return '启用后才能发起手动备份'
  }
  if (target?.verifyStatus !== 'SUCCESS') {
    return target?.verifyMessage || '建议先完成可写性与连通性验证'
  }
  return '当前已满足手动备份开启条件'
}

const backupNextStepTagType = (target) => {
  if (target?.enabled !== true) {
    return 'warning'
  }
  if (target?.verifyStatus !== 'SUCCESS') {
    return 'info'
  }
  return 'success'
}

const backupJobStatusLabel = (jobOrStatus) => {
  const status = typeof jobOrStatus === 'string' ? jobOrStatus : jobOrStatus?.status
  const followUpRequired = typeof jobOrStatus === 'object' && jobOrStatus?.followUpRequired === true
  if (status === 'SUCCESS' && followUpRequired) return '成功（需处理）'
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '执行中'
  if (status === 'PENDING') return '等待中'
  return status || '未知'
}

const backupJobSummary = (job) => {
  if (job?.status === 'FAILED') {
    return job?.statusMessage || '备份失败，请优先检查目标配置与可写性。'
  }
  if (job?.status === 'SUCCESS' && job?.followUpRequired === true) {
    return job?.statusMessage || '备份已完成，但仍需处理后续提示。'
  }
  if (job?.status === 'RUNNING' || job?.status === 'PENDING') {
    return job?.statusMessage || '任务仍在处理中，请稍后刷新确认结果。'
  }
  if (job?.status === 'SUCCESS') {
    return job?.statusMessage || '备份集已生成，可在恢复中心继续校验与恢复。'
  }
  return job?.statusMessage || '请刷新后确认任务状态。'
}

const backupJobSeverity = (job) => {
  if (job?.status === 'FAILED') return 'danger'
  if (job?.status === 'SUCCESS' && job?.followUpRequired === true) return 'warning'
  if (job?.status === 'RUNNING' || job?.status === 'PENDING') return 'info'
  return 'normal'
}

const backupJobSeverityLabel = (job) => {
  if (job?.status === 'FAILED') return '失败'
  if (job?.status === 'SUCCESS' && job?.followUpRequired === true) return '需跟进'
  if (job?.status === 'RUNNING') return '执行中'
  if (job?.status === 'PENDING') return '等待中'
  return '正常'
}

const backupJobSeverityTagType = (job) => {
  if (job?.status === 'FAILED') return 'danger'
  if (job?.status === 'SUCCESS' && job?.followUpRequired === true) return 'warning'
  if (job?.status === 'RUNNING' || job?.status === 'PENDING') return 'info'
  return 'success'
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
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.overview-card {
  border-radius: 10px;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;

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

.overview-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.section-card {
  border-radius: 10px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-header-main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.target-readiness-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.target-readiness-card {
  border-radius: 10px;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;

  :deep(.el-card__body) {
    display: grid;
    gap: 6px;
    padding: 16px 18px;
  }
}

.target-readiness-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.readiness-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.readiness-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.readiness-hint {
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.target-readiness-alert {
  margin-bottom: 16px;
}

.table-scroll-hint {
  display: none;
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.backup-guidelines {
  padding: 18px 20px 20px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
}

.guidelines-header {
  margin-bottom: 16px;
}

.guidelines-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.guidelines-intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}

.guideline-steps {
  display: grid;
  gap: 12px;
}

.guideline-step {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.guideline-step:hover {
  transform: translateY(-1px);
  background: #f3f7fb;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.guideline-step-index {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  border-radius: 12px;
  background: #e0f2fe;
  color: #075985;
  font-size: 13px;
  font-weight: 700;
}

.guideline-step-body {
  min-width: 0;
}

.guideline-step-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.guideline-step-desc,
.guideline-step-note {
  margin: 6px 0 0;
  line-height: 1.85;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.guideline-step-note {
  color: var(--el-text-color-secondary);
}

.job-attention-alert {
  margin-bottom: 16px;
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
  border: 1px solid transparent;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.job-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.job-item--danger {
  background: #fff4f4;
  border-color: #fecaca;
}

.job-item--warning {
  background: #fff8eb;
  border-color: #fcd9a3;
}

.job-item--info {
  background: #f5f9ff;
  border-color: #bfdbfe;
}

.job-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.job-title {
  font-weight: 600;
}

.job-meta {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.verify-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.verify-message {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

.action-tooltip-trigger {
  display: inline-flex;
}

.form-section {
  margin: 4px 0 8px;
}

.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.form-section-desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.backup-target-submit-alert {
  margin-top: 4px;
}

.port-input {
  width: 100%;
}

.job-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.section-card .el-table .cell) {
  line-height: 1.6;
}

:deep(.section-card .el-table__row > td) {
  transition: background-color 0.18s ease;
}

:deep(.section-card .el-table__body tr:hover > td) {
  background: #f8fafc;
}

:deep(.backup-target-dialog .el-dialog) {
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}

:deep(.backup-target-dialog .el-dialog__body) {
  overflow-y: auto;
}

@media (max-width: 1024px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .target-readiness-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .table-scroll-hint {
    display: block;
  }

  .section-header {
    flex-direction: column;
    align-items: stretch;
  }

  .section-header-main {
    justify-content: space-between;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .target-readiness-grid {
    grid-template-columns: 1fr;
  }

  .guideline-step {
    grid-template-columns: 1fr;
  }

  .job-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .dialog-footer-actions {
    flex-direction: column-reverse;
  }

  .dialog-footer-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  :deep(.backup-target-dialog) {
    margin-top: 6vh;
  }

  :deep(.backup-target-dialog .el-dialog) {
    max-height: 90vh;
  }

  :deep(.backup-target-dialog .el-dialog__body) {
    padding-top: 12px;
    padding-bottom: 12px;
  }
}
</style>
