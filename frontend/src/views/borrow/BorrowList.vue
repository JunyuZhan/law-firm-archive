<template>
  <div class="borrow-list">
    <div class="page-header">
      <h1>借阅管理</h1>
      <p>统一处理我的借阅、审批、借出和逾期提醒，保证电子档案借阅过程可控、可追踪。</p>
    </div>

    <!-- 标签页 -->
    <el-card
      shadow="never"
      class="tab-card"
    >
      <el-tabs
        v-model="activeTab"
        @tab-change="handleTabChange"
      >
        <el-tab-pane
          label="我的借阅"
          name="my"
        />
        <el-tab-pane
          v-if="canManageBorrows"
          label="待审批"
          name="pending"
        />
        <el-tab-pane
          v-if="canManageBorrows"
          label="待借出"
          name="lend"
        />
        <el-tab-pane
          v-if="canManageBorrows"
          label="逾期提醒"
          name="overdue"
        />
      </el-tabs>
    </el-card>

    <!-- 筛选 -->
    <el-card
      v-if="activeTab !== 'overdue'"
      shadow="never"
      class="filter-card"
    >
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="申请编号/档案号/题名/申请人"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="借阅方式">
          <el-select
            v-model="filters.borrowType"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="item in borrowTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="activeTab === 'my'"
          label="状态"
        >
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
              label="借出中"
              value="BORROWED"
            />
            <el-option
              label="已归还"
              value="RETURNED"
            />
            <el-option
              label="已取消"
              value="CANCELLED"
            />
            <el-option
              label="已拒绝"
              value="REJECTED"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
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
      >
        <el-table-column
          prop="applicationNo"
          label="申请编号"
          width="150"
        />
        <el-table-column
          prop="archiveNo"
          label="档案号"
          width="150"
        />
        <el-table-column
          prop="archiveTitle"
          label="档案题名"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          v-if="activeTab !== 'my'"
          prop="applicantName"
          label="申请人"
          width="100"
        />
        <el-table-column
          prop="applicantDept"
          label="申请部门"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          label="借阅方式"
          width="110"
        >
          <template #default="{ row }">
            {{ getBorrowTypeName(row.borrowType) }}
          </template>
        </el-table-column>
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
          label="预计归还"
          width="120"
        >
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row) }">
              {{ row.expectedReturnDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="applyTime"
          label="申请时间"
          width="170"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.applyTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="200"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="activeTab === 'my'">
              <el-button 
                v-if="row.status === 'PENDING'" 
                type="danger"
                link
                size="small" 
                @click="handleCancel(row)"
              >
                取消
              </el-button>
              <el-button 
                v-if="row.status === 'BORROWED'" 
                type="primary"
                link
                size="small" 
                @click="handleReturn(row)"
              >
                归还
              </el-button>
              <el-button 
                v-if="row.status === 'BORROWED'" 
                link
                size="small" 
                @click="handleRenew(row)"
              >
                续借
              </el-button>
            </template>
            <template v-if="activeTab === 'pending'">
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
            <template v-if="activeTab === 'lend'">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleLend(row)"
              >
                借出
              </el-button>
            </template>
            <template v-if="activeTab === 'overdue'">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleReturn(row)"
              >
                归还
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

      <!-- 分页 -->
      <div
        v-if="activeTab !== 'overdue' && pagination.total > pagination.pageSize"
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
            v-model="approveForm.remarks"
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
      title="拒绝申请"
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
            v-model="rejectForm.reason"
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

    <!-- 续借弹窗 -->
    <el-dialog
      v-model="renewDialogVisible"
      title="续借"
      width="500px"
    >
      <el-form
        :model="renewForm"
        label-width="100px"
      >
        <el-form-item
          label="新归还日期"
          required
        >
          <el-date-picker
            v-model="renewForm.newReturnDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledRenewDate"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renewDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="renewSubmitting"
          @click="confirmRenew"
        >
          确认续借
        </el-button>
      </template>
    </el-dialog>

    <!-- 借阅详情弹窗 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="借阅详情"
      width="760px"
      class="borrow-detail-dialog"
    >
      <div v-loading="detailLoading">
        <div
          v-if="detailData"
          class="borrow-detail-content"
        >
          <div class="borrow-detail-hero">
            <div>
              <div class="hero-label">
                档案号
              </div>
              <div class="hero-value">
                {{ detailData.archiveNo }}
              </div>
            </div>
            <div class="hero-main">
              <div class="hero-label">
                档案题名
              </div>
              <div class="hero-title">
                {{ detailData.archiveTitle }}
              </div>
            </div>
            <div>
              <div class="hero-label">
                当前状态
              </div>
              <el-tag
                :type="getStatusType(detailData.status)"
                size="small"
              >
                {{ getStatusName(detailData.status) }}
              </el-tag>
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">
              申请信息
            </div>
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item label="申请编号">
                {{ detailData.applicationNo }}
              </el-descriptions-item>
              <el-descriptions-item label="借阅方式">
                {{ getBorrowTypeName(detailData.borrowType) }}
              </el-descriptions-item>
              <el-descriptions-item label="申请人">
                {{ detailData.applicantName }}
              </el-descriptions-item>
              <el-descriptions-item label="申请部门">
                {{ detailData.applicantDept || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="申请时间">
                {{ formatDateTime(detailData.applyTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="预计归还">
                {{ detailData.expectedReturnDate }}
              </el-descriptions-item>
              <el-descriptions-item
                label="借阅目的"
                :span="2"
              >
                <div class="detail-text-block">
                  {{ detailData.borrowPurpose || '-' }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="detailData.remarks"
                label="备注"
                :span="2"
              >
                <div class="detail-text-block">
                  {{ detailData.remarks }}
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">
              审批与借出
            </div>
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item label="审批人">
                {{ detailData.approverName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="审批时间">
                {{ formatDateTime(detailData.approveTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="借出时间">
                {{ formatDateTime(detailData.borrowTime) }}
              </el-descriptions-item>
              <el-descriptions-item label="实际归还">
                {{ detailData.actualReturnDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="续借次数">
                {{ detailData.renewCount ? `${detailData.renewCount} 次` : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="拒绝原因">
                <span :class="{ 'text-danger': !!detailData.rejectReason }">
                  {{ detailData.rejectReason || '-' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item
                label="审批意见"
                :span="2"
              >
                <div class="detail-text-block">
                  {{ detailData.approveRemarks || '-' }}
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  getMyBorrows, getPendingBorrows, getApprovedBorrows, getOverdueBorrows,
  cancelBorrow, approveBorrow, rejectBorrow, returnArchive, renewBorrow,
  lendArchive, getBorrowDetail
} from '@/api/borrow'
import {
  getBorrowStatusName,
  getBorrowStatusType,
  getBorrowTypeName,
  getBorrowTypeOptions
} from '@/utils/archiveEnums'

const userStore = useUserStore()
const activeTab = ref('my')
const loading = ref(false)
const tableData = ref([])
const canManageBorrows = computed(() => userStore.isArchivist)

const filters = reactive({
  status: '',
  borrowType: '',
  keyword: ''
})
const borrowTypeOptions = getBorrowTypeOptions()

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const renewDialogVisible = ref(false)
const currentRow = ref(null)

// 操作 loading 状态
const approveSubmitting = ref(false)
const rejectSubmitting = ref(false)
const renewSubmitting = ref(false)
const lendSubmitting = ref(false)

const approveForm = reactive({ remarks: '' })
const rejectForm = reactive({ reason: '' })
const renewForm = reactive({ newReturnDate: '' })

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    if (!canManageBorrows.value && activeTab.value !== 'my') {
      activeTab.value = 'my'
    }
    let res
    if (activeTab.value === 'my') {
      res = await getMyBorrows({
        status: filters.status,
        borrowType: filters.borrowType,
        keyword: filters.keyword,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'pending') {
      res = await getPendingBorrows({
        borrowType: filters.borrowType,
        keyword: filters.keyword,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'lend') {
      res = await getApprovedBorrows({
        borrowType: filters.borrowType,
        keyword: filters.keyword,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'overdue') {
      res = await getOverdueBorrows()
      tableData.value = res.data || []
      pagination.total = tableData.value.length
    }
  } catch (e) {
    console.error('获取数据失败', e)
    ElMessage.error(e.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  if (!canManageBorrows.value && activeTab.value !== 'my') {
    activeTab.value = 'my'
  }
  pagination.pageNum = 1
  filters.status = ''
  filters.borrowType = ''
  filters.keyword = ''
  fetchData()
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchData()
}

const handleReset = () => {
  filters.status = ''
  filters.borrowType = ''
  filters.keyword = ''
  handleSearch()
}

// 取消申请
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该借阅申请?', '提示')
    await cancelBorrow(row.id)
    ElMessage.success('已取消')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// 审批通过
const handleApprove = (row) => {
  currentRow.value = row
  approveForm.remarks = ''
  approveDialogVisible.value = true
}

const confirmApprove = async () => {
  approveSubmitting.value = true
  try {
    await approveBorrow(currentRow.value.id, approveForm.remarks)
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

// 审批拒绝
const handleReject = (row) => {
  currentRow.value = row
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.reason?.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  if (rejectForm.reason.length < 2) {
    ElMessage.warning('拒绝原因至少2个字')
    return
  }
  rejectSubmitting.value = true
  try {
    await rejectBorrow(currentRow.value.id, { reason: rejectForm.reason })
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

// 归还
const handleReturn = async (row) => {
  try {
    await ElMessageBox.confirm('确认归还该档案?', '提示')
    await returnArchive(row.id, '')
    ElMessage.success('归还成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('归还失败', e)
      ElMessage.error(e.response?.data?.message || '归还失败')
    }
  }
}

// 续借
const handleRenew = (row) => {
  currentRow.value = row
  renewForm.newReturnDate = ''
  renewDialogVisible.value = true
}

const confirmRenew = async () => {
  if (!renewForm.newReturnDate) {
    ElMessage.warning('请选择新的归还日期')
    return
  }
  renewSubmitting.value = true
  try {
    await renewBorrow(currentRow.value.id, renewForm.newReturnDate)
    ElMessage.success('续借成功')
    renewDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error('续借失败', e)
    ElMessage.error(e.response?.data?.message || '续借失败')
  } finally {
    renewSubmitting.value = false
  }
}

const disabledRenewDate = (date) => {
  if (!currentRow.value) return true
  const expected = parseLocalDate(currentRow.value.expectedReturnDate)
  const candidate = parseLocalDate(date)
  if (!expected || !candidate) return true
  return candidate <= expected
}

// 借出
const handleLend = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认将档案【${row.archiveTitle}】借出给【${row.applicantName}】？`, 
      '确认借出',
      { type: 'warning' }
    )
    lendSubmitting.value = true
    await lendArchive(row.id)
    ElMessage.success('借出成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('借出失败', e)
      ElMessage.error(e.response?.data?.message || '借出失败')
    }
  } finally {
    lendSubmitting.value = false
  }
}

// 查看详情
const detailDialogVisible = ref(false)
const detailData = ref(null)
const detailLoading = ref(false)

const handleView = async (row) => {
  detailData.value = null
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const res = await getBorrowDetail(row.id)
    detailData.value = res.data
  } catch (e) {
    console.error('获取详情失败', e)
    ElMessage.error('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 工具函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

const parseLocalDate = (value) => {
  if (!value) return null
  if (value instanceof Date) {
    const normalized = new Date(value)
    normalized.setHours(0, 0, 0, 0)
    return normalized
  }

  if (typeof value === 'string') {
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (match) {
      const [, year, month, day] = match
      return new Date(Number(year), Number(month) - 1, Number(day))
    }
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return null
  parsed.setHours(0, 0, 0, 0)
  return parsed
}

// 注：getBorrowStatusName, getBorrowStatusType 已从 archiveEnums.js 导入
const getStatusName = getBorrowStatusName
const getStatusType = getBorrowStatusType

const isOverdue = (row) => {
  if (row.status !== 'BORROWED') return false
  if (!row.expectedReturnDate) return false

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const expected = parseLocalDate(row.expectedReturnDate)
  if (!expected) return false

  return expected < today
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.borrow-list {
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

.tab-card,
.filter-card,
.table-card {
  border-radius: 10px;
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

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.borrow-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.borrow-detail-hero {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) 120px;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f4f6f9 100%);
}

.hero-label {
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.hero-value {
  font-weight: 600;
  color: #8c6b1f;
}

.hero-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  color: #303133;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #34495e;
}

.detail-text-block {
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .borrow-detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
