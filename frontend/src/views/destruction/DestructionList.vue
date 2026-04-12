<template>
  <div class="destruction-list">
    <div class="page-header">
      <h1>销毁管理</h1>
      <p>管理档案销毁申请、审批与执行记录，确保电子档案销毁过程有据可查、可审批、可追溯。</p>
    </div>

    <!-- 标签页 -->
    <el-card
      shadow="never"
      class="tabs-card"
    >
      <el-tabs
        v-model="activeTab"
        @tab-change="handleTabChange"
      >
        <el-tab-pane
          label="全部记录"
          name="all"
        />
        <el-tab-pane
          label="待审批"
          name="pending"
        />
        <el-tab-pane
          label="待执行"
          name="approved"
        />
      </el-tabs>
    </el-card>

    <!-- 筛选 -->
    <el-card
      v-if="activeTab === 'all'"
      shadow="never"
      class="filter-card"
    >
      <el-form inline>
        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              label="待审批"
              value="PENDING"
            />
            <el-option
              label="已通过"
              value="APPROVED"
            />
            <el-option
              label="已拒绝"
              value="REJECTED"
            />
            <el-option
              label="已执行"
              value="EXECUTED"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="fetchData"
          >
            查询
          </el-button>
          <el-button @click="handleCreate">
            申请销毁
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 待执行操作栏 -->
    <el-card
      v-if="activeTab === 'approved'"
      shadow="never"
      class="filter-card"
    >
      <el-button
        type="danger"
        :disabled="selectedRows.length === 0"
        @click="handleBatchExecute"
      >
        批量执行销毁 ({{ selectedRows.length }})
      </el-button>
    </el-card>

    <!-- 列表 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <el-table 
        v-loading="loading" 
        :data="tableData" 
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          v-if="activeTab === 'approved'"
          type="selection"
          width="50"
        />
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="destructionBatchNo"
          label="批次号"
          width="150"
        />
        <el-table-column
          prop="archiveId"
          label="档案ID"
          width="100"
        />
        <el-table-column
          label="销毁方式"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.destructionMethod === 'PHYSICAL' ? 'danger' : ''"
              size="small"
            >
              {{ row.destructionMethod === 'PHYSICAL' ? '物理销毁' : '逻辑删除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="destructionReason"
          label="销毁原因"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="proposerName"
          label="提议人"
          width="100"
        />
        <el-table-column
          label="提议时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.proposedAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button
                type="success"
                link
                size="small"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                type="danger"
                link
                size="small"
                @click="handleReject(row)"
              >
                拒绝
              </el-button>
            </template>
            <template v-if="row.status === 'APPROVED'">
              <el-button
                type="danger"
                link
                size="small"
                @click="handleExecute(row)"
              >
                执行销毁
              </el-button>
            </template>
            <el-button
              link
              size="small"
              @click="handleView(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="pagination.total > pagination.pageSize"
        class="pagination"
      >
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 申请销毁弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      title="申请销毁"
      width="600px"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
      >
        <el-form-item
          label="选择档案"
          prop="archiveId"
          class="span-2"
        >
          <el-select
            v-model="createForm.archiveId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="输入档案号或题名搜索"
            :remote-method="searchArchiveOptions"
            :loading="archiveSearchLoading"
            style="width: 100%"
            @change="handleArchiveSelect"
          >
            <el-option
              v-for="item in archiveOptions"
              :key="item.id"
              :label="`${item.archiveNo}｜${item.title}`"
              :value="item.id"
            >
              <div class="archive-option">
                <span class="archive-option-no">{{ item.archiveNo }}</span>
                <span class="archive-option-title">{{ item.title }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="form-tip">
            支持按档案号或题名检索，避免手工输入内部 ID。
          </div>
        </el-form-item>
        <div
          v-if="selectedArchiveSummary"
          class="archive-summary-card"
        >
          <div class="summary-main">
            <span class="archive-no">{{ selectedArchiveSummary.archiveNo }}</span>
            <span class="archive-title">{{ selectedArchiveSummary.title }}</span>
          </div>
          <div class="summary-meta">
            <span>保管期限：{{ selectedArchiveSummary.retentionPeriod || '-' }}</span>
            <span>档案形式：{{ selectedArchiveSummary.archiveForm || '-' }}</span>
          </div>
        </div>
        <el-form-item
          label="销毁方式"
          prop="destructionMethod"
        >
          <el-radio-group v-model="createForm.destructionMethod">
            <el-radio value="LOGICAL">
              逻辑删除
            </el-radio>
            <el-radio value="PHYSICAL">
              物理销毁
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="销毁原因"
          prop="destructionReason"
        >
          <el-input
            v-model="createForm.destructionReason"
            type="textarea"
            :rows="3"
            placeholder="请输入销毁原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="submitCreate"
        >
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog
      v-model="approveDialogVisible"
      title="审批"
      width="500px"
    >
      <el-form
        :model="approveForm"
        label-width="80px"
      >
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="approveSubmitting"
          @click="confirmApprove"
        >
          确认通过
        </el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝销毁"
      width="500px"
    >
      <el-form
        :model="rejectForm"
        label-width="80px"
      >
        <el-form-item
          label="拒绝原因"
          required
        >
          <el-input
            v-model="rejectForm.comment"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因（至少2个字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="danger"
          :loading="rejectSubmitting"
          @click="confirmReject"
        >
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="销毁记录详情"
      width="600px"
    >
      <el-descriptions
        v-if="detailData"
        :column="2"
        border
      >
        <el-descriptions-item label="记录ID">
          {{ detailData.id }}
        </el-descriptions-item>
        <el-descriptions-item label="批次号">
          {{ detailData.destructionBatchNo }}
        </el-descriptions-item>
        <el-descriptions-item label="档案ID">
          {{ detailData.archiveId }}
        </el-descriptions-item>
        <el-descriptions-item label="销毁方式">
          <el-tag
            :type="detailData.destructionMethod === 'PHYSICAL' ? 'danger' : ''"
            size="small"
          >
            {{ detailData.destructionMethod === 'PHYSICAL' ? '物理销毁' : '逻辑删除' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag
            :type="getStatusType(detailData.status)"
            size="small"
          >
            {{ getStatusName(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          label="销毁原因"
          :span="2"
        >
          {{ detailData.destructionReason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提议人">
          {{ detailData.proposerName }}
        </el-descriptions-item>
        <el-descriptions-item label="提议时间">
          {{ formatDateTime(detailData.proposedAt) }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailData.approverName"
          label="审批人"
        >
          {{ detailData.approverName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailData.approvedAt"
          label="审批时间"
        >
          {{ formatDateTime(detailData.approvedAt) }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailData.approvalComment"
          label="审批意见"
          :span="2"
        >
          {{ detailData.approvalComment }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailData.executorName"
          label="执行人"
        >
          {{ detailData.executorName }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailData.executedAt"
          label="执行时间"
        >
          {{ formatDateTime(detailData.executedAt) }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDestructionList, getPendingDestructions, getApprovedDestructions,
  getDestructionDetail, applyDestruction, approveDestruction, rejectDestruction,
  executeDestruction, batchExecuteDestruction
} from '@/api/destruction'
import { getArchiveList } from '@/api/archive'
import {
  getDestructionStatusName,
  getDestructionStatusType
} from '@/utils/archiveEnums'

const activeTab = ref('all')
const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])

const filters = reactive({
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 申请销毁
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const submitting = ref(false)
const createForm = ref({
  archiveId: null,
  destructionMethod: 'LOGICAL',
  destructionReason: ''
})
const createRules = {
  archiveId: [{ required: true, message: '请选择档案', trigger: 'change' }],
  destructionMethod: [{ required: true, message: '请选择销毁方式', trigger: 'change' }],
  destructionReason: [{ required: true, message: '请输入销毁原因', trigger: 'blur' }]
}
const archiveOptions = ref([])
const archiveSearchLoading = ref(false)
const selectedArchiveSummary = ref(null)

// 审批
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentRow = ref(null)
const approveForm = reactive({ comment: '' })
const rejectForm = reactive({ comment: '' })

// 操作 loading 状态
const approveSubmitting = ref(false)
const rejectSubmitting = ref(false)

// 详情
const detailDialogVisible = ref(false)
const detailData = ref(null)

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'all') {
      res = await getDestructionList({
        status: filters.status,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
    } else if (activeTab.value === 'pending') {
      res = await getPendingDestructions({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
    } else if (activeTab.value === 'approved') {
      res = await getApprovedDestructions({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
    }
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) {
    console.error('获取数据失败', e)
    ElMessage.error(e.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  pagination.pageNum = 1
  selectedRows.value = []
  fetchData()
}

// 选择变化
const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

// 申请销毁
const handleCreate = () => {
  createForm.value = {
    archiveId: null,
    destructionMethod: 'LOGICAL',
    destructionReason: ''
  }
  selectedArchiveSummary.value = null
  archiveOptions.value = []
  createDialogVisible.value = true
}

const searchArchiveOptions = async (keyword) => {
  const term = keyword?.trim()
  if (!term) {
    archiveOptions.value = []
    return
  }
  archiveSearchLoading.value = true
  try {
    const res = await getArchiveList({
      keyword: term,
      pageNum: 1,
      pageSize: 20
    })
    archiveOptions.value = res.data?.records || []
  } catch (e) {
    console.error('搜索档案失败', e)
    archiveOptions.value = []
  } finally {
    archiveSearchLoading.value = false
  }
}

const handleArchiveSelect = (archiveId) => {
  if (!archiveId) {
    selectedArchiveSummary.value = null
    return
  }
  const selected = archiveOptions.value.find(item => item.id === archiveId)
  if (selected) {
    selectedArchiveSummary.value = selected
  }
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
    submitting.value = true
    await applyDestruction(createForm.value)
    ElMessage.success('销毁申请已提交')
    createDialogVisible.value = false
    fetchData()
  } catch (e) {
    if (e !== false) {
      console.error(e)
      ElMessage.error(e.response?.data?.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

// 审批
const handleApprove = (row) => {
  currentRow.value = row
  approveForm.comment = ''
  approveDialogVisible.value = true
}

const confirmApprove = async () => {
  approveSubmitting.value = true
  try {
    await approveDestruction(currentRow.value.id, approveForm.comment)
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('审批失败', e)
    ElMessage.error(e.response?.data?.message || '审批失败')
  } finally {
    approveSubmitting.value = false
  }
}

const handleReject = (row) => {
  currentRow.value = row
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.comment?.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  rejectSubmitting.value = true
  try {
    await rejectDestruction(currentRow.value.id, rejectForm.comment)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('拒绝失败', e)
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    rejectSubmitting.value = false
  }
}

// 执行销毁
const handleExecute = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认执行销毁？此操作不可逆！\n档案ID: ${row.archiveId}`,
      '危险操作',
      { type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await executeDestruction(row.id, '')
    ElMessage.success('销毁已执行')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('执行销毁失败', e)
      ElMessage.error(e.response?.data?.message || '执行失败')
    }
  }
}

// 批量执行
const handleBatchExecute = async () => {
  try {
    await ElMessageBox.confirm(
      `确认批量执行销毁 ${selectedRows.value.length} 条记录？此操作不可逆！`,
      '危险操作',
      { type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await batchExecuteDestruction({
      ids: selectedRows.value.map(r => r.id),
      remarks: ''
    })
    ElMessage.success('批量销毁已执行')
    selectedRows.value = []
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量销毁失败', e)
      ElMessage.error(e.response?.data?.message || '批量执行失败')
    }
  }
}

// 查看详情
const handleView = async (row) => {
  try {
    const res = await getDestructionDetail(row.id)
    detailData.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    console.error('获取详情失败', e)
    ElMessage.error(e.response?.data?.message || '获取详情失败')
  }
}

// 工具函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

// 注：getDestructionStatusName, getDestructionStatusType 已从 archiveEnums.js 导入
// 销毁状态需要自定义显示名，'APPROVED' 在销毁场景显示为 '待执行'
const getStatusName = (status) => {
  const customMap = {
    APPROVED: '待执行' // 覆盖默认的 '已批准'
  }
  return customMap[status] || getDestructionStatusName(status)
}

const getStatusType = (status) => {
  const customMap = {
    REJECTED: 'info' // 覆盖默认的 'danger'
  }
  return customMap[status] || getDestructionStatusType(status)
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.destruction-list {
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
  line-height: 1.6;
  color: #606266;
}

.tabs-card,
.filter-card,
.table-card {
  border-radius: 10px;
}

.tabs-card {
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.filter-card {
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.archive-summary-card {
  margin: -4px 0 18px;
  padding: 14px 16px;
  border: 1px solid #e5eaf3;
  border-radius: 12px;
  background: #f8fafc;
}

.summary-main {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 8px;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #606266;
}

.archive-no {
  font-size: 12px;
  font-weight: 600;
  color: #8c6b1f;
}

.archive-title {
  font-weight: 600;
  color: #303133;
}

.archive-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.4;
}

.archive-option-no {
  font-size: 12px;
  color: #909399;
}

.archive-option-title {
  color: #303133;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #8c8c8c;
}

.span-2 {
  :deep(.el-form-item__content) {
    display: block;
  }
}
</style>
