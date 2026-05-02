<template>
  <div class="restore-center">
    <div
      v-if="!props.embedded"
      class="page-header"
    >
      <div>
        <h1>恢复中心</h1>
        <p>
          恢复中心用于在新环境中重新接管电子档案系统。当前已支持本地目录与 SMB/NAS 恢复源浏览、备份集校验、维护模式控制和恢复任务台账。
        </p>
      </div>
    </div>

    <el-alert
      v-if="!props.embedded"
      title="高危能力"
      type="warning"
      :closable="false"
      description="恢复操作只能由具备系统管理权限的账号执行，正式恢复前必须进入维护模式并完成完整性校验。"
    />

    <el-card shadow="never">
      <div class="maintenance-bar">
        <div>
          <div class="toolbar-title">
            维护模式
          </div>
          <div class="toolbar-desc">
            {{ maintenanceStatus.message || '恢复前需进入维护模式，避免业务写入干扰恢复过程。' }}
          </div>
        </div>
        <div class="maintenance-actions">
          <el-tag :type="maintenanceStatus.enabled ? 'warning' : 'info'">
            {{ maintenanceStatus.enabled ? '已开启' : '未开启' }}
          </el-tag>
          <el-tooltip
            v-if="maintenanceActionBlockedReason"
            :content="maintenanceActionBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger">
              <el-button
                :type="maintenanceStatus.enabled ? 'danger' : 'primary'"
                :disabled="true"
              >
                {{ maintenanceStatus.enabled ? '退出维护模式' : '进入维护模式' }}
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            :type="maintenanceStatus.enabled ? 'danger' : 'primary'"
            :loading="maintenanceSwitching"
            :disabled="restoreDialogVisible || maintenanceSwitching || runningRestore"
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
          <div class="toolbar-title">
            恢复源浏览
          </div>
          <div class="toolbar-desc">
            选择已配置的备份目标，读取其中可识别的本地或 SMB 备份集。
          </div>
        </div>
        <div class="toolbar-actions">
          <el-tooltip
            v-if="restoreSourceInteractionBlockedReason"
            :content="restoreSourceInteractionBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger target-select-trigger">
              <el-select
                v-model="selectedTargetId"
                clearable
                filterable
                :placeholder="restoreSourceSelectPlaceholder"
                class="target-select"
                :disabled="true"
              >
                <el-option
                  v-for="target in enabledBackupTargets"
                  :key="target.id"
                  :label="`${target.name} (${backupTargetTypeLabel(target.targetType)})`"
                  :value="target.id"
                />
              </el-select>
            </span>
          </el-tooltip>
          <el-select
            v-else
            v-model="selectedTargetId"
            clearable
            filterable
            :placeholder="restoreSourceSelectPlaceholder"
            class="target-select"
            :disabled="loadingSets"
          >
            <el-option
              v-for="target in enabledBackupTargets"
              :key="target.id"
              :label="`${target.name} (${backupTargetTypeLabel(target.targetType)})`"
              :value="target.id"
            />
          </el-select>
          <el-tooltip
            v-if="restoreSourceInteractionBlockedReason"
            :content="restoreSourceInteractionBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger">
              <el-button :disabled="true">
                刷新备份集
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            :loading="loadingSets"
            :disabled="restoreDialogVisible || loadingSets || runningRestore"
            @click="handleRefreshBackupSets"
          >
            刷新备份集
          </el-button>
        </div>
      </div>
      <el-alert
        v-if="restoreSourceGuidance"
        :title="restoreSourceGuidance.title"
        :type="restoreSourceGuidance.type"
        :closable="false"
        :description="restoreSourceGuidance.description"
        class="restore-source-alert"
      />
      <div
        v-if="backupSets.length"
        class="restore-overview-grid"
      >
        <el-card
          v-for="item in restoreOverviewCards"
          :key="item.label"
          shadow="never"
          class="restore-overview-card"
        >
          <div class="overview-value">
            {{ item.value }}
          </div>
          <div class="overview-label">
            {{ item.label }}
          </div>
          <div
            v-if="item.hint"
            class="overview-hint"
          >
            {{ item.hint }}
          </div>
        </el-card>
      </div>
      <el-alert
        v-if="maintenanceBlockedRunnableBackupSetCount > 0"
        title="存在可恢复备份集，但当前被维护模式拦截"
        type="info"
        :closable="false"
        :description="`已有 ${maintenanceBlockedRunnableBackupSetCount} 个备份集通过完整性校验；进入维护模式后即可发起恢复。`"
        class="restore-sets-alert"
      />
      <p
        v-if="backupSets.length"
        class="table-scroll-hint"
      >
        恢复源字段较多，窄屏设备可左右滑动查看更多列。
      </p>
      <el-table
        v-if="showRestoreSetsTable"
        v-loading="loadingSets"
        :data="backupSets"
        border
      >
        <template #empty>
          <el-empty
            v-if="restoreTableEmptyState"
            :description="restoreTableEmptyState.description"
          >
            <el-button
              v-if="restoreTableEmptyState.action === 'clear-filter'"
              @click="selectedTargetId = null"
            >
              查看全部目标
            </el-button>
            <el-button
              v-else-if="restoreTableEmptyState.action === 'refresh'"
              @click="handleRefreshBackupSets"
            >
              刷新备份集
            </el-button>
          </el-empty>
        </template>
        <el-table-column
          prop="backupSetName"
          label="备份集"
          min-width="180"
        />
        <el-table-column
          prop="targetName"
          label="来源目标"
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
          v-if="!isCompactLayout"
          prop="createdAt"
          label="创建时间"
          min-width="170"
        />
        <el-table-column
          v-if="!isCompactLayout"
          prop="databaseMode"
          label="数据库"
          width="120"
        >
          <template #default="{ row }">
            {{ databaseModeLabel(row.databaseMode) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="fileCount"
          label="文件数"
          width="90"
        />
        <el-table-column
          v-if="!isCompactLayout"
          prop="objectCount"
          label="对象数"
          width="90"
        />
        <el-table-column
          prop="totalBytes"
          label="大小"
          min-width="120"
        >
          <template #default="{ row }">
            {{ formatBytes(row.totalBytes) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="verifyStatus"
          label="状态"
          min-width="220"
        >
          <template #default="{ row }">
            <div class="verify-cell">
              <el-tag :type="setTagType(row.verifyStatus)">
                {{ restoreVerifyStatusLabel(row.verifyStatus) }}
              </el-tag>
              <span class="verify-message">
                {{ row.verifyMessage || '-' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="恢复范围"
          min-width="190"
        >
          <template #default="{ row }">
            <div class="capability-cell">
              <template v-if="restoreCapabilityTags(row).length">
                <el-tag
                  v-for="item in restoreCapabilityTags(row)"
                  :key="item.label"
                  :type="item.type"
                  size="small"
                >
                  {{ item.label }}
                </el-tag>
              </template>
              <span
                v-else
                class="capability-empty"
              >
                无可恢复范围
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="120"
          fixed="right"
        >
          <template #default="{ row }">
            <el-tooltip
              :disabled="canOpenRestoreDialog(row)"
              :content="restoreDialogBlockedReason(row)"
              placement="top"
            >
              <span class="action-tooltip-trigger">
                <el-button
                  text
                  type="primary"
                  :disabled="!canOpenRestoreDialog(row)"
                  @click="openRestoreDialog(row)"
                >
                  执行恢复
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        v-if="backupSets.length && !hasRunnableBackupSet"
        title="当前备份集均不可恢复"
        type="warning"
        :closable="false"
        description="已读取到备份集，但它们未通过完整性校验，或不包含可恢复的数据库、电子文件、系统配置。"
        class="restore-sets-alert"
      />
    </el-card>

    <el-row :gutter="20">
      <el-col
        class="restore-jobs-col"
        :lg="14"
        :xs="24"
      >
        <el-card shadow="never">
          <template #header>
            <span>恢复任务台账</span>
          </template>
          <el-alert
            v-if="failedRestoreJobCount > 0 || followUpRestoreJobCount > 0"
            :title="restoreJobAttentionTitle"
            :type="failedRestoreJobCount > 0 ? 'error' : 'warning'"
            :closable="false"
            :description="restoreJobAttentionDescription"
            class="restore-jobs-alert"
          />
          <p
            v-if="restoreJobs.length"
            class="table-scroll-hint"
          >
            任务台账列较多，窄屏设备可左右滑动查看更多列。
          </p>
          <el-table
            v-loading="loading"
            :data="restoreJobs"
            border
          >
            <template #empty>
              <el-empty description="暂无恢复任务记录" />
            </template>
            <el-table-column
              prop="restoreNo"
              label="恢复单号"
              min-width="160"
            />
            <el-table-column
              prop="targetName"
              label="恢复源"
              min-width="160"
            />
            <el-table-column
              prop="backupSetName"
              label="备份集"
              min-width="180"
            />
            <el-table-column
              prop="status"
              label="状态"
              min-width="220"
            >
              <template #default="{ row }">
                <div class="verify-cell">
                  <el-tag :type="restoreJobTagType(row)">
                    {{ restoreJobStatusLabel(row) }}
                  </el-tag>
                  <span class="verify-message">
                    {{ restoreJobSummary(row) }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              label="关注"
              width="110"
            >
              <template #default="{ row }">
                <el-tag :type="restoreJobAttentionTagType(row)">
                  {{ restoreJobAttentionLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-if="!isCompactLayout"
              prop="verifyStatus"
              label="校验"
              width="100"
            >
              <template #default="{ row }">
                <el-tag :type="setTagType(row.verifyStatus)">
                  {{ restoreVerifyStatusLabel(row.verifyStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-if="!isCompactLayout"
              prop="rebuildIndexStatus"
              label="索引"
              width="110"
            >
              <template #default="{ row }">
                <el-tag :type="restoreJobRebuildIndexTagType(row)">
                  {{ restoreJobRebuildIndexStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              v-if="!isCompactLayout"
              prop="createdAt"
              label="创建时间"
              min-width="170"
            />
            <el-table-column
              label="报告"
              width="100"
              fixed="right"
            >
              <template #default="{ row }">
                <el-tooltip
                  :disabled="Boolean(row.restoreReport)"
                  content="当前任务还没有可查看的恢复报告"
                  placement="top"
                >
                  <span class="action-tooltip-trigger">
                    <el-button
                      text
                      type="primary"
                      :disabled="!row.restoreReport"
                      @click="openReport(row)"
                    >
                      查看
                    </el-button>
                  </span>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col
        class="restore-side-col"
        :lg="10"
        :xs="24"
      >
        <section class="restore-principles">
          <div class="guidelines-header">
            <h2 class="guidelines-title">
              恢复执行原则
            </h2>
            <p class="guidelines-intro">
              恢复属于高危接管操作，建议按固定顺序执行，先确认环境与备份完整性，再进入维护模式并逐步恢复。
            </p>
          </div>
          <div class="guideline-steps">
            <article
              v-for="item in restorePrincipleSteps"
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
        <el-card shadow="never">
          <template #header>
            <span>恢复后检查项</span>
          </template>
          <ul class="restore-checklist">
            <li>确认数据库、电子文件、系统配置是否都按预期恢复。</li>
            <li>检查 Elasticsearch 索引状态，必要时重新触发重建。</li>
            <li>确认是否已成功退出维护模式，再恢复业务入口。</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="restoreDialogVisible"
      title="执行系统恢复"
      :width="isCompactLayout ? 'calc(100vw - 24px)' : '620px'"
      :close-on-click-modal="!runningRestore"
      :close-on-press-escape="!runningRestore"
      :show-close="!runningRestore"
      class="restore-run-dialog"
    >
      <div
        v-if="selectedBackupSet"
        class="restore-summary"
      >
        <div><span>备份集：</span>{{ selectedBackupSet.backupSetName }}</div>
        <div><span>来源目标：</span>{{ selectedBackupSet.targetName }}</div>
        <div><span>目标类型：</span>{{ backupTargetTypeLabel(selectedBackupSet.targetType) }}</div>
        <div><span>创建时间：</span>{{ selectedBackupSet.createdAt || '-' }}</div>
        <div class="path-row">
          <span>备份位置：</span>{{ selectedBackupSet.displayPath || selectedBackupSet.backupSetName || '-' }}
        </div>
      </div>

      <el-form
        :label-width="isCompactLayout ? 'auto' : '150px'"
        :label-position="isCompactLayout ? 'top' : 'right'"
      >
        <div class="form-section">
          <div class="form-section-title">
            恢复范围
          </div>
          <p class="form-section-desc">
            仅能选择当前备份集实际包含的内容；不可恢复的范围会自动禁用。
          </p>
        </div>
        <el-form-item label="恢复数据库">
          <el-switch
            v-model="restoreForm.restoreDatabase"
            :disabled="!isDatabaseRestoreAvailable(selectedBackupSet)"
          />
          <span
            v-if="selectedBackupSet && !isDatabaseRestoreAvailable(selectedBackupSet)"
            class="form-hint"
          >
            当前备份集未包含可恢复的数据库备份
          </span>
        </el-form-item>
        <el-form-item label="恢复电子文件">
          <el-switch
            v-model="restoreForm.restoreFiles"
            :disabled="!isFilesRestoreAvailable(selectedBackupSet)"
          />
          <span
            v-if="selectedBackupSet && !isFilesRestoreAvailable(selectedBackupSet)"
            class="form-hint"
          >
            当前备份集未包含可恢复的电子文件索引
          </span>
        </el-form-item>
        <el-form-item label="恢复系统配置">
          <el-switch
            v-model="restoreForm.restoreConfig"
            :disabled="!isConfigRestoreAvailable(selectedBackupSet)"
          />
          <span
            v-if="selectedBackupSet && !isConfigRestoreAvailable(selectedBackupSet)"
            class="form-hint"
          >
            当前备份集未包含可恢复的系统配置
          </span>
        </el-form-item>
        <div class="form-section">
          <div class="form-section-title">
            恢复收尾
          </div>
          <p class="form-section-desc">
            这些选项用于控制恢复完成后的索引与维护模式处理。
          </p>
        </div>
        <el-form-item label="重建搜索索引">
          <el-switch v-model="restoreForm.rebuildIndex" />
        </el-form-item>
        <el-form-item label="成功后退出维护">
          <el-switch v-model="restoreForm.exitMaintenanceAfterSuccess" />
        </el-form-item>
        <div class="form-section">
          <div class="form-section-title">
            最终确认
          </div>
          <p class="form-section-desc">
            输入确认口令后才会提交恢复任务。
          </p>
        </div>
        <el-form-item label="确认口令">
          <el-input
            v-model="restoreForm.confirmationText"
            placeholder="输入 RESTORE 后才可提交"
          />
        </el-form-item>
        <el-alert
          v-if="restoreSubmitBlockedReason"
          :title="restoreSubmitBlockedReason"
          type="warning"
          :closable="false"
          class="restore-submit-alert"
        />
      </el-form>

      <template #footer>
        <div class="dialog-footer-actions">
          <el-button
            :disabled="runningRestore"
            @click="restoreDialogVisible = false"
          >
            取消
          </el-button>
          <el-tooltip
            v-if="restoreSubmitBlockedReason"
            :content="restoreSubmitBlockedReason"
            placement="top"
          >
            <span class="action-tooltip-trigger">
              <el-button
                type="danger"
                :loading="runningRestore"
                :disabled="true"
              >
                开始恢复
              </el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            type="danger"
            :loading="runningRestore"
            :disabled="runningRestore"
            @click="handleRunRestore"
          >
            开始恢复
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportDialogVisible"
      title="恢复报告"
      :width="isCompactLayout ? 'calc(100vw - 24px)' : '680px'"
      class="restore-report-dialog"
    >
      <el-alert
        :title="selectedReportSummary"
        type="info"
        :closable="false"
        class="report-summary"
      />
      <div
        v-if="selectedReportHighlights.length"
        class="report-highlight-grid"
      >
        <el-card
          v-for="item in selectedReportHighlights"
          :key="item.label"
          shadow="never"
          class="report-highlight-card"
        >
          <div class="report-highlight-label">
            {{ item.label }}
          </div>
          <div class="report-highlight-value">
            {{ item.value }}
          </div>
        </el-card>
      </div>
      <pre class="report-view">{{ selectedReport }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRestoreBackupSets,
  getBackupTargets,
  getRestoreJobs,
  getRestoreMaintenanceStatus,
  runRestore,
  updateRestoreMaintenanceStatus
} from '@/api/backup'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const isCompactLayout = ref(false)
const loading = ref(false)
const loadingSets = ref(false)
const restoreJobs = ref([])
const backupSets = ref([])
const backupTargets = ref([])
const hasLoadedBackupTargets = ref(false)
const selectedTargetId = ref(null)
const maintenanceStatus = ref({})
const restoreDialogVisible = ref(false)
const runningRestore = ref(false)
const maintenanceSwitching = ref(false)
const selectedBackupSet = ref(null)
const reportDialogVisible = ref(false)
const selectedReport = ref('')
const selectedReportSummary = ref('')
const selectedReportHighlights = ref([])
const restoreForm = ref({
  restoreDatabase: true,
  restoreFiles: true,
  restoreConfig: true,
  rebuildIndex: true,
  exitMaintenanceAfterSuccess: true,
  confirmationText: ''
})
let backupSetsRequestId = 0
let lastSuccessfulBackupSetsFilterKey = null
let restoreJobsRequestId = 0
let maintenanceStatusRequestId = 0
const enabledBackupTargets = computed(() => backupTargets.value.filter(target => target.enabled))
const hasRunnableBackupSet = computed(() => backupSets.value.some(canRunRestore))
const integrityReadyBackupSetCount = computed(() => backupSets.value.filter(item => item.verifyStatus === 'READY').length)
const maintenanceActionBlockedReason = computed(() => {
  if (runningRestore.value) {
    return '恢复任务执行期间不能切换维护模式'
  }
  if (restoreDialogVisible.value) {
    return '请先关闭恢复弹窗，再切换维护模式'
  }
  return ''
})
const maintenanceBlockedRunnableBackupSetCount = computed(() => {
  if (!maintenanceStatus.value.restoreRequiresMaintenance || maintenanceStatus.value.enabled) {
    return 0
  }
  return backupSets.value.filter(item => !restoreOpenBlockedReason(item)).length
})
const dialogRunnableBackupSetCount = computed(() => backupSets.value.filter(canOpenRestoreDialog).length)
const restoreSourceActionBlockedReason = computed(() => {
  if (!hasLoadedBackupTargets.value) {
    return ''
  }
  if (backupTargets.value.length === 0) {
    return '请先新增备份目标'
  }
  if (enabledBackupTargets.value.length === 0) {
    return '请先启用至少一个备份目标'
  }
  return ''
})
const restoreSourceInteractionBlockedReason = computed(() => {
  if (runningRestore.value) {
    return '恢复任务执行期间不能切换恢复源或刷新备份集'
  }
  if (restoreDialogVisible.value) {
    return '请先关闭恢复弹窗，再切换恢复源或刷新备份集'
  }
  return restoreSourceActionBlockedReason.value
})
const canBrowseRestoreSources = computed(() => !restoreSourceActionBlockedReason.value)
const showRestoreSetsTable = computed(() => {
  return loadingSets.value || backupSets.value.length > 0 || canBrowseRestoreSources.value
})
const restoreSourceSelectPlaceholder = computed(() => {
  return restoreSourceInteractionBlockedReason.value || '全部备份目标'
})
const restoreEmptyStateDescription = computed(() => {
  if (selectedTargetId.value) {
    return '当前筛选目标下还没有可识别的备份集'
  }
  return '当前未识别到备份集'
})
const restoreTableEmptyState = computed(() => {
  if (!hasLoadedBackupTargets.value || loadingSets.value || backupSets.value.length > 0 || restoreSourceGuidance.value) {
    return null
  }
  if (selectedTargetId.value) {
    return {
      description: restoreEmptyStateDescription.value,
      action: 'clear-filter'
    }
  }
  if (canBrowseRestoreSources.value) {
    return {
      description: restoreEmptyStateDescription.value,
      action: 'refresh'
    }
  }
  return null
})
const restoreSourceGuidance = computed(() => {
  if (!hasLoadedBackupTargets.value) {
    return null
  }
  if (backupTargets.value.length === 0) {
    return {
      title: '当前还没有备份目标',
      type: 'info',
      description: props.embedded
        ? '请先切换到上方“备份中心”新增一个备份目标，完成验证后再返回此处读取备份集。'
        : '请先到“备份中心”新增一个备份目标，完成验证后再返回此处读取备份集。'
    }
  }
  if (enabledBackupTargets.value.length === 0) {
    return {
      title: '当前没有已启用的备份目标',
      type: 'warning',
      description: props.embedded
        ? '已存在备份目标，但都处于停用状态；请先在上方“备份中心”启用至少一个目标。'
        : '已存在备份目标，但都处于停用状态；请先到“备份中心”启用至少一个目标。'
    }
  }
  return null
})
const restoreSubmitBlockedReason = computed(() => {
  if (!selectedBackupSet.value) {
    return '请先选择一个可恢复的备份集'
  }
  if (maintenanceStatus.value.restoreRequiresMaintenance && !maintenanceStatus.value.enabled) {
    return '请先进入维护模式，再执行恢复'
  }
  if (!restoreForm.value.restoreDatabase && !restoreForm.value.restoreFiles && !restoreForm.value.restoreConfig) {
    return '至少需要选择一个恢复范围'
  }
  if (restoreForm.value.restoreDatabase && !isDatabaseRestoreAvailable(selectedBackupSet.value)) {
    return '当前备份集未包含可恢复的数据库备份'
  }
  if (restoreForm.value.restoreFiles && !isFilesRestoreAvailable(selectedBackupSet.value)) {
    return '当前备份集未包含可恢复的电子文件索引'
  }
  if (restoreForm.value.restoreConfig && !isConfigRestoreAvailable(selectedBackupSet.value)) {
    return '当前备份集未包含可恢复的系统配置'
  }
  if (restoreForm.value.confirmationText?.trim().toUpperCase() !== 'RESTORE') {
    return '请输入确认口令 RESTORE'
  }
  return ''
})
const restoreOverviewCards = computed(() => [
  {
    label: '已识别备份集',
    value: backupSets.value.length,
    hint: selectedTargetId.value ? '当前已按备份目标筛选' : '当前展示全部已启用目标'
  },
  {
    label: '通过完整性校验',
    value: integrityReadyBackupSetCount.value,
    hint: '仅完整备份集允许进入恢复流程'
  },
  {
    label: '当前可直接恢复',
    value: dialogRunnableBackupSetCount.value,
    hint: dialogRunnableBackupSetCount.value ? '已满足恢复弹窗开启条件' : '当前无可直接恢复的备份集'
  },
  {
    label: '受维护模式拦截',
    value: maintenanceBlockedRunnableBackupSetCount.value,
    hint: maintenanceBlockedRunnableBackupSetCount.value ? '进入维护模式后可执行恢复' : '当前无维护模式阻塞项'
  }
])
const failedRestoreJobCount = computed(() => restoreJobs.value.filter(row => row?.status === 'FAILED').length)
const followUpRestoreJobCount = computed(() => restoreJobs.value.filter(getRestoreFollowUpRequired).length)
const restoreJobAttentionTitle = computed(() => {
  if (failedRestoreJobCount.value > 0) {
    return '恢复任务台账存在失败记录'
  }
  return '恢复任务台账存在待处理项'
})
const restoreJobAttentionDescription = computed(() => {
  if (failedRestoreJobCount.value > 0 && followUpRestoreJobCount.value > 0) {
    return `最近恢复任务中有 ${failedRestoreJobCount.value} 条失败，另有 ${followUpRestoreJobCount.value} 条完成后仍需跟进。`
  }
  if (failedRestoreJobCount.value > 0) {
    return `最近恢复任务中有 ${failedRestoreJobCount.value} 条失败，建议优先查看恢复报告与失败步骤。`
  }
  return `最近恢复任务中有 ${followUpRestoreJobCount.value} 条完成后仍需跟进，请重点关注维护模式退出与索引重建结果。`
})
const restorePrincipleSteps = [
  {
    step: '01',
    title: '确认目标环境',
    description: '先确认新环境的数据库、中间件、文件存储与搜索服务已经准备就绪，并且不存在业务写入。',
    note: '如果运行环境本身未就绪，恢复过程即使成功也可能无法正常接管业务。'
  },
  {
    step: '02',
    title: '读取并校验备份集',
    description: '选择恢复源后，优先确认备份集已通过 manifest、校验码和文件完整性检查。',
    note: '只有完整性通过且包含可恢复范围的备份集，才适合进入恢复流程。'
  },
  {
    step: '03',
    title: '进入维护模式',
    description: '正式恢复前先切换系统到维护模式，避免业务侧继续写入造成恢复结果被污染。',
    note: '如果页面提示存在维护模式拦截项，先切换维护模式再执行恢复。'
  },
  {
    step: '04',
    title: '恢复并完成收尾',
    description: '按需恢复数据库、电子文件和系统配置，随后重建搜索索引并核对维护模式是否成功退出。',
    note: '任务完成后仍应查看恢复报告，确认是否存在需跟进的异常步骤。'
  }
]

const refreshPageData = async () => {
  const results = await Promise.allSettled([
    loadRestoreJobs(),
    loadTargets(),
    loadBackupSets(),
    loadMaintenanceStatus()
  ])
  if (results.some(result => result.status === 'rejected')) {
    ElMessage.warning('部分恢复数据刷新失败，已保留最近一次成功数据，请稍后重试')
  }
}

const refreshRestoreStatus = async () => {
  const results = await Promise.allSettled([
    loadRestoreJobs(),
    loadMaintenanceStatus()
  ])
  if (results.some(result => result.status === 'rejected')) {
    ElMessage.warning('恢复状态刷新失败，已保留最近一次成功数据，请稍后重试')
  }
}

const refreshRestoreStatusAfterAction = async () => {
  try {
    await refreshRestoreStatus()
  } catch {
    ElMessage.warning('恢复状态刷新失败，已保留最近一次成功数据，请稍后手动刷新')
  }
}

const refreshMaintenanceStatusAfterAction = async () => {
  try {
    await loadMaintenanceStatus()
  } catch {
    ElMessage.warning('维护模式状态刷新失败，已保留最近一次成功数据，请稍后手动刷新')
  }
}

const handleBackupSetsLoadFailure = (error) => {
  if (error?.backupSetsPreserved === false) {
    ElMessage.warning('备份集刷新失败，当前筛选结果不可用，请稍后重试')
    return
  }
  ElMessage.warning('备份集刷新失败，已保留最近一次成功数据，请稍后重试')
}

const handleRefreshBackupSets = async () => {
  try {
    await loadBackupSets()
  } catch (error) {
    handleBackupSetsLoadFailure(error)
  }
}

const resetRestoreForm = () => {
  restoreForm.value = {
    restoreDatabase: true,
    restoreFiles: true,
    restoreConfig: true,
    rebuildIndex: true,
    exitMaintenanceAfterSuccess: true,
    confirmationText: ''
  }
}

onMounted(async () => {
  updateCompactLayout()
  window.addEventListener('resize', updateCompactLayout)
  await refreshPageData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateCompactLayout)
})

const updateCompactLayout = () => {
  isCompactLayout.value = window.innerWidth <= 640
}

const tagType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

const getRestoreFollowUpRequired = (row) => {
  if (!row || row.status !== 'SUCCESS') {
    return false
  }
  if (row.followUpRequired === true) {
    return true
  }
  return hasFailedRestoreStep(row.restoreReport, 'MAINTENANCE')
}

const restoreJobTagType = (row) => {
  if (getRestoreFollowUpRequired(row)) {
    return 'warning'
  }
  return tagType(row?.status)
}

const setTagType = (status) => {
  if (status === 'READY') return 'success'
  if (status === 'INCOMPLETE') return 'warning'
  return 'info'
}

const restoreJobStatusLabel = (row) => {
  const status = typeof row === 'string' ? row : row?.status
  if (status === 'SUCCESS') {
    if (getRestoreFollowUpRequired(row)) return '成功（需处理）'
    return '成功'
  }
  if (status === 'FAILED') return '失败'
  if (status === 'RUNNING') return '执行中'
  if (status === 'PENDING') return '等待中'
  return status || '未知'
}

const restoreVerifyStatusLabel = (status) => {
  if (status === 'READY') return '通过'
  if (status === 'INCOMPLETE') return '未通过'
  return status || '未知'
}

const backupTargetTypeLabel = (targetType) => {
  if (targetType === 'LOCAL') return '本地'
  if (targetType === 'SMB') return 'SMB'
  return targetType || '未知'
}

const databaseModeLabel = (mode) => {
  if (mode === 'PG_DUMP') return '可恢复'
  if (mode === 'PLACEHOLDER') return '占位文件'
  return mode || '-'
}

const rebuildIndexTagType = (status) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'NOT_STARTED') return 'info'
  if (status === 'SKIPPED') return 'info'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

const rebuildIndexStatusLabel = (status) => {
  if (status === 'SUCCESS') return '完成'
  if (status === 'FAILED') return '失败'
  if (status === 'NOT_STARTED') return '未开始'
  if (status === 'SKIPPED') return '跳过'
  if (status === 'PENDING') return '等待中'
  return status || '未知'
}

const restoreJobRebuildIndexTagType = (row) => {
  if (getRestoreFollowUpRequired(row) && row?.rebuildIndexStatus === 'SUCCESS') {
    return 'warning'
  }
  return rebuildIndexTagType(row?.rebuildIndexStatus)
}

const restoreJobRebuildIndexStatusLabel = (row) => {
  if (getRestoreFollowUpRequired(row) && row?.rebuildIndexStatus === 'SUCCESS') {
    return '已完成'
  }
  return rebuildIndexStatusLabel(row?.rebuildIndexStatus)
}

const loadRestoreJobs = async () => {
  const requestId = ++restoreJobsRequestId
  loading.value = true
  try {
    const res = await getRestoreJobs({ pageNum: 1, pageSize: 10 })
    if (requestId === restoreJobsRequestId) {
      restoreJobs.value = res.data?.records || []
    }
  } finally {
    if (requestId === restoreJobsRequestId) {
      loading.value = false
    }
  }
}

const loadTargets = async () => {
  const res = await getBackupTargets()
  backupTargets.value = res.data || []
  hasLoadedBackupTargets.value = true
  if (selectedTargetId.value && !enabledBackupTargets.value.some(target => target.id === selectedTargetId.value)) {
    selectedTargetId.value = null
  }
}

const loadMaintenanceStatus = async () => {
  const requestId = ++maintenanceStatusRequestId
  const res = await getRestoreMaintenanceStatus()
  if (requestId === maintenanceStatusRequestId) {
    maintenanceStatus.value = res.data || {}
  }
}

const loadBackupSets = async () => {
  const requestId = ++backupSetsRequestId
  const requestFilterKey = selectedTargetId.value ? `TARGET:${selectedTargetId.value}` : 'ALL'
  loadingSets.value = true
  try {
    const params = selectedTargetId.value ? { targetId: selectedTargetId.value } : {}
    const res = await getRestoreBackupSets(params)
    if (requestId === backupSetsRequestId) {
      backupSets.value = res.data || []
      lastSuccessfulBackupSetsFilterKey = requestFilterKey
      syncSelectedBackupSet()
    }
  } catch (error) {
    if (requestId === backupSetsRequestId && requestFilterKey !== lastSuccessfulBackupSetsFilterKey) {
      backupSets.value = []
      syncSelectedBackupSet()
      error.backupSetsPreserved = false
    } else {
      error.backupSetsPreserved = true
    }
    throw error
  } finally {
    if (requestId === backupSetsRequestId) {
      loadingSets.value = false
    }
  }
}

watch(selectedTargetId, () => {
  loadBackupSets().catch((error) => {
    handleBackupSetsLoadFailure(error)
  })
})

watch(restoreDialogVisible, (visible) => {
  if (visible) {
    return
  }
  selectedBackupSet.value = null
  resetRestoreForm()
})

watch(reportDialogVisible, (visible) => {
  if (visible) {
    return
  }
  selectedReport.value = ''
  selectedReportSummary.value = ''
  selectedReportHighlights.value = []
})

const toggleMaintenance = async () => {
  if (maintenanceSwitching.value) {
    return
  }
  maintenanceSwitching.value = true
  try {
    const nextEnabled = !maintenanceStatus.value.enabled
    const res = await updateRestoreMaintenanceStatus(nextEnabled)
    maintenanceStatus.value = {
      ...maintenanceStatus.value,
      ...(res?.data || {}),
      enabled: res?.data?.enabled ?? nextEnabled
    }
    ElMessage.success(nextEnabled ? '已进入维护模式' : '已退出维护模式')
    await refreshMaintenanceStatusAfterAction()
  } finally {
    maintenanceSwitching.value = false
  }
}

const openRestoreDialog = (row) => {
  const blockedReason = restoreDialogBlockedReason(row)
  if (blockedReason) {
    ElMessage.warning(blockedReason)
    return
  }
  selectedBackupSet.value = row
  restoreForm.value = {
    restoreDatabase: isDatabaseRestoreAvailable(row),
    restoreFiles: isFilesRestoreAvailable(row),
    restoreConfig: isConfigRestoreAvailable(row),
    rebuildIndex: true,
    exitMaintenanceAfterSuccess: true,
    confirmationText: ''
  }
  restoreDialogVisible.value = true
}

const syncSelectedBackupSet = () => {
  if (!selectedBackupSet.value) {
    return
  }
  const latestBackupSet = backupSets.value.find(item =>
    item.targetId === selectedBackupSet.value.targetId
    && item.backupSetName === selectedBackupSet.value.backupSetName
  )
  if (!latestBackupSet) {
    selectedBackupSet.value = null
    if (restoreDialogVisible.value) {
      restoreDialogVisible.value = false
      if (!runningRestore.value) {
        ElMessage.warning('所选备份集已不可用，恢复弹窗已关闭，请重新选择')
      }
    }
    return
  }
  selectedBackupSet.value = latestBackupSet
  if (restoreDialogVisible.value) {
    if (!canRunRestore(latestBackupSet)) {
      restoreDialogVisible.value = false
      if (!runningRestore.value) {
        ElMessage.warning('所选备份集已不再满足恢复条件，恢复弹窗已关闭，请重新选择')
      }
      return
    }
    restoreForm.value = {
      ...restoreForm.value,
      restoreDatabase: restoreForm.value.restoreDatabase && isDatabaseRestoreAvailable(latestBackupSet),
      restoreFiles: restoreForm.value.restoreFiles && isFilesRestoreAvailable(latestBackupSet),
      restoreConfig: restoreForm.value.restoreConfig && isConfigRestoreAvailable(latestBackupSet)
    }
  }
}

const resolveRestoreCapability = (backupSet, capabilityKey) => {
  if (!backupSet) {
    return false
  }
  return backupSet[capabilityKey] === true
}

const isDatabaseRestoreAvailable = (backupSet) =>
  resolveRestoreCapability(
    backupSet,
    'databaseRestorable'
  )

const isFilesRestoreAvailable = (backupSet) =>
  resolveRestoreCapability(
    backupSet,
    'filesRestorable'
  )

const isConfigRestoreAvailable = (backupSet) =>
  resolveRestoreCapability(
    backupSet,
    'configRestorable'
  )

const restoreCapabilityTags = (backupSet) => {
  const items = []
  if (isDatabaseRestoreAvailable(backupSet)) {
    items.push({ label: '数据库', type: 'success' })
  }
  if (isFilesRestoreAvailable(backupSet)) {
    items.push({ label: '电子文件', type: 'warning' })
  }
  if (isConfigRestoreAvailable(backupSet)) {
    items.push({ label: '系统配置', type: 'info' })
  }
  return items
}

const canRunRestore = (backupSet) => {
  return !restoreOpenBlockedReason(backupSet)
}

const canOpenRestoreDialog = (backupSet) => {
  return !restoreDialogBlockedReason(backupSet)
}

const restoreOpenBlockedReason = (backupSet) => {
  if (!backupSet || backupSet.verifyStatus !== 'READY') {
    return '当前备份集未通过完整性校验，不能执行恢复'
  }
  if (!isDatabaseRestoreAvailable(backupSet)
    && !isFilesRestoreAvailable(backupSet)
    && !isConfigRestoreAvailable(backupSet)) {
    return '当前备份集未包含可恢复的数据库、电子文件或系统配置'
  }
  return ''
}

const restoreDialogBlockedReason = (backupSet) => {
  const restoreBlockedReason = restoreOpenBlockedReason(backupSet)
  if (restoreBlockedReason) {
    return restoreBlockedReason
  }
  if (maintenanceStatus.value.restoreRequiresMaintenance && !maintenanceStatus.value.enabled) {
    return '请先进入维护模式，再执行恢复'
  }
  return ''
}

const handleRunRestore = async () => {
  if (!selectedBackupSet.value) return
  if (maintenanceStatus.value.restoreRequiresMaintenance && !maintenanceStatus.value.enabled) {
    ElMessage.warning('执行恢复前必须先进入维护模式')
    return
  }
  if (!restoreForm.value.restoreDatabase && !restoreForm.value.restoreFiles && !restoreForm.value.restoreConfig) {
    ElMessage.warning('至少需要选择一个恢复范围')
    return
  }
  if (restoreForm.value.restoreDatabase && !isDatabaseRestoreAvailable(selectedBackupSet.value)) {
    ElMessage.warning('当前备份集未包含可恢复的数据库备份')
    return
  }
  if (restoreForm.value.restoreFiles && !isFilesRestoreAvailable(selectedBackupSet.value)) {
    ElMessage.warning('当前备份集未包含可恢复的电子文件索引')
    return
  }
  if (restoreForm.value.restoreConfig && !isConfigRestoreAvailable(selectedBackupSet.value)) {
    ElMessage.warning('当前备份集未包含可恢复的系统配置')
    return
  }
  if (restoreForm.value.confirmationText?.trim().toUpperCase() !== 'RESTORE') {
    ElMessage.warning('请输入确认口令 RESTORE')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认对备份集“${selectedBackupSet.value.backupSetName}”执行恢复吗？此操作用于新环境接管，属于高危操作。`,
      '恢复确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  runningRestore.value = true
  try {
    const res = await runRestore({
      targetId: selectedBackupSet.value.targetId,
      backupSetName: selectedBackupSet.value.backupSetName,
      ...restoreForm.value
    })
    const restoreNo = res?.data?.restoreNo
    const status = res?.data?.status
    const statusMessage = res?.data?.statusMessage
    const followUpRequired = res?.data?.followUpRequired === true
    const summaryText = restoreNo
      ? `${statusMessage || `恢复任务状态：${status || 'UNKNOWN'}`}，任务编号：${restoreNo}`
      : (statusMessage || `恢复任务状态：${status || 'UNKNOWN'}`)
    const messageType = restoreSubmissionMessageType(status, followUpRequired)
    if (messageType === 'success') {
      ElMessage.success(summaryText)
    } else if (messageType === 'error') {
      ElMessage.error(summaryText)
    } else {
      ElMessage.warning(summaryText)
    }
    restoreDialogVisible.value = false
    await refreshRestoreStatusAfterAction()
  } finally {
    runningRestore.value = false
  }
}

const openReport = (row) => {
  selectedReportSummary.value = buildReportSummary(row)
  selectedReportHighlights.value = buildReportHighlights(row)
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

const parseRestoreReport = (value) => {
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const buildReportSummary = (row) => {
  if (!row?.restoreReport) {
    return row?.statusMessage || '暂无恢复报告'
  }
  try {
    const report = parseRestoreReport(row.restoreReport)
    const failedStepLabels = getFailedRestoreStepLabels(report)
    if (row?.status === 'FAILED') {
      if (failedStepLabels.length) {
        return `${row.statusMessage || '恢复执行失败'}，失败步骤：${failedStepLabels.join('、')}`
      }
      return row?.statusMessage || '恢复执行失败'
    }
    if (row?.status === 'SUCCESS' && failedStepLabels.length) {
      return `${row.statusMessage || '恢复任务已完成'}，异常步骤：${failedStepLabels.join('、')}`
    }
    return row?.statusMessage || '恢复报告详情'
  } catch {
    return row?.statusMessage || '恢复报告详情'
  }
}

const buildReportHighlights = (row) => {
  const report = parseRestoreReport(row?.restoreReport)
  const failedStepLabels = getFailedRestoreStepLabels(report)
  const stepCount = Array.isArray(report?.steps) ? report.steps.length : 0
  const highlights = [
    {
      label: '任务结论',
      value: restoreJobAttentionLabel(row)
    },
    {
      label: '失败/异常步骤',
      value: failedStepLabels.length ? failedStepLabels.join('、') : '无'
    }
  ]

  if (stepCount > 0) {
    highlights.push({
      label: '报告步骤数',
      value: `${stepCount} 项`
    })
  }

  if (row?.rebuildIndexStatus) {
    highlights.push({
      label: '索引状态',
      value: restoreJobRebuildIndexStatusLabel(row)
    })
  }

  return highlights
}

const restoreJobSummary = (row) => {
  const report = parseRestoreReport(row?.restoreReport)
  const failedStepLabels = getFailedRestoreStepLabels(report)
  if (row?.status === 'FAILED') {
    if (failedStepLabels.length) {
      return `优先检查：${failedStepLabels.join('、')}`
    }
    return row?.statusMessage || '恢复失败，请先查看恢复报告。'
  }
  if (getRestoreFollowUpRequired(row)) {
    if (failedStepLabels.length) {
      return `任务已完成，但仍需复核：${failedStepLabels.join('、')}`
    }
    return row?.statusMessage || '恢复已完成，但仍有后续处理项。'
  }
  if (row?.status === 'RUNNING' || row?.status === 'PENDING') {
    return row?.statusMessage || '恢复仍在处理中，请稍后刷新确认结果。'
  }
  if (row?.status === 'SUCCESS') {
    return '恢复已完成，建议核对索引状态与维护模式。'
  }
  return row?.statusMessage || '请刷新后确认恢复结果。'
}

const describeRestoreStep = (step) => {
  if (step === 'DATABASE') return '数据库恢复'
  if (step === 'FILES') return '电子文件恢复'
  if (step === 'CONFIG') return '系统配置恢复'
  if (step === 'INDEX') return '索引重建'
  if (step === 'MAINTENANCE') return '退出维护模式'
  return step || '未知步骤'
}

const hasFailedRestoreStep = (restoreReport, stepName) => {
  const report = parseRestoreReport(restoreReport)
  const steps = Array.isArray(report?.steps) ? report.steps : []
  return steps.some(step => step?.step === stepName && step?.status === 'FAILED')
}

const getFailedRestoreStepLabels = (report) => {
  const steps = Array.isArray(report?.steps) ? report.steps : []
  return steps
    .filter(step => step?.status === 'FAILED')
    .map(step => describeRestoreStep(step?.step))
    .filter(Boolean)
}

const restoreSubmissionMessageType = (status, followUpRequired) => {
  if (followUpRequired === true) return 'warning'
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'error'
  return 'warning'
}

const restoreJobAttentionLabel = (row) => {
  if (row?.status === 'FAILED') return '失败'
  if (getRestoreFollowUpRequired(row)) return '需跟进'
  if (row?.status === 'RUNNING') return '执行中'
  if (row?.status === 'PENDING') return '等待中'
  return '正常'
}

const restoreJobAttentionTagType = (row) => {
  if (row?.status === 'FAILED') return 'danger'
  if (getRestoreFollowUpRequired(row)) return 'warning'
  if (row?.status === 'RUNNING' || row?.status === 'PENDING') return 'info'
  return 'success'
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

.restore-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.restore-overview-card {
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

.restore-overview-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.overview-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.overview-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.overview-hint {
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.restore-sets-alert {
  margin-top: 16px;
}

.restore-source-alert {
  margin-bottom: 16px;
}

.restore-jobs-alert {
  margin-bottom: 16px;
}

.table-scroll-hint {
  display: none;
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.restore-principles {
  padding: 18px 20px 20px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);
  margin-bottom: 16px;
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
  background: #fdf8ec;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.guideline-step-index {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  border-radius: 12px;
  background: #fef3c7;
  color: #92400e;
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

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
}

.report-summary {
  margin-bottom: 12px;
}

.report-highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.report-highlight-card {
  border-radius: 12px;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;

  :deep(.el-card__body) {
    display: grid;
    gap: 6px;
    padding: 14px 16px;
  }
}

.report-highlight-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.07);
}

.report-highlight-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.report-highlight-value {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--el-text-color-primary);
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

.target-select-trigger {
  width: 240px;
}

.page-header p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.restore-checklist {
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

.form-hint {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
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

.capability-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.capability-empty {
  font-size: 12px;
  color: var(--el-text-color-secondary);
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

.restore-submit-alert {
  margin-top: 4px;
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

:deep(.restore-run-dialog .el-dialog),
:deep(.restore-report-dialog .el-dialog) {
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}

:deep(.restore-run-dialog .el-dialog__body),
:deep(.restore-report-dialog .el-dialog__body) {
  overflow-y: auto;
}

.action-tooltip-trigger {
  display: inline-flex;
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

  .restore-overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .guideline-step {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .table-scroll-hint {
    display: block;
  }

  .dialog-footer-actions {
    flex-direction: column-reverse;
  }

  .dialog-footer-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .restore-side-col {
    order: -1;
  }

  .restore-overview-grid {
    grid-template-columns: 1fr;
  }

  .report-highlight-grid {
    grid-template-columns: 1fr;
  }

  :deep(.restore-run-dialog),
  :deep(.restore-report-dialog) {
    margin-top: 6vh;
  }

  :deep(.restore-run-dialog .el-dialog),
  :deep(.restore-report-dialog .el-dialog) {
    max-height: 90vh;
  }

  :deep(.restore-run-dialog .el-dialog__body),
  :deep(.restore-report-dialog .el-dialog__body) {
    padding-top: 12px;
    padding-bottom: 12px;
  }
}
</style>
