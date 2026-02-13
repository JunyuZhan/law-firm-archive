<template>
  <div class="borrow-list">
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="我的借阅" name="my" />
      <el-tab-pane label="待审批" name="pending" />
      <el-tab-pane label="逾期提醒" name="overdue" />
    </el-tabs>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card" v-if="activeTab === 'my'">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="借出中" value="BORROWED" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="applicationNo" label="申请编号" width="150" />
        <el-table-column prop="archiveNo" label="档案号" width="150" />
        <el-table-column prop="archiveTitle" label="档案题名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" width="100" v-if="activeTab !== 'my'" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预计归还" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row) }">
              {{ row.expectedReturnDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.applyTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="activeTab === 'my'">
              <el-button 
                v-if="row.status === 'PENDING'" 
                type="danger" link size="small" 
                @click="handleCancel(row)"
              >
                取消
              </el-button>
              <el-button 
                v-if="row.status === 'BORROWED'" 
                type="primary" link size="small" 
                @click="handleReturn(row)"
              >
                归还
              </el-button>
              <el-button 
                v-if="row.status === 'BORROWED'" 
                link size="small" 
                @click="handleRenew(row)"
              >
                续借
              </el-button>
            </template>
            <template v-if="activeTab === 'pending'">
              <el-button type="success" link size="small" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button type="danger" link size="small" @click="handleReject(row)">
                拒绝
              </el-button>
            </template>
            <template v-if="activeTab === 'overdue'">
              <el-button type="primary" link size="small" @click="handleReturn(row)">
                归还
              </el-button>
            </template>
            <el-button link size="small" @click="handleView(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="activeTab !== 'overdue'">
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
    <el-dialog v-model="approveDialogVisible" title="审批" width="500px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.remarks" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="500px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 续借弹窗 -->
    <el-dialog v-model="renewDialogVisible" title="续借" width="500px">
      <el-form :model="renewForm" label-width="100px">
        <el-form-item label="新归还日期" required>
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
        <el-button @click="renewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRenew">确认续借</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getMyBorrows, getPendingBorrows, getOverdueBorrows,
  cancelBorrow, approveBorrow, rejectBorrow, returnArchive, renewBorrow
} from '@/api/borrow'

const activeTab = ref('my')
const loading = ref(false)
const tableData = ref([])

const filters = reactive({
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const renewDialogVisible = ref(false)
const currentRow = ref(null)

const approveForm = reactive({ remarks: '' })
const rejectForm = reactive({ reason: '' })
const renewForm = reactive({ newReturnDate: '' })

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'my') {
      res = await getMyBorrows({
        status: filters.status,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'pending') {
      res = await getPendingBorrows({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'overdue') {
      res = await getOverdueBorrows()
      tableData.value = res.data
    }
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  pagination.pageNum = 1
  fetchData()
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
  try {
    await approveBorrow(currentRow.value.id, approveForm.remarks)
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

// 审批拒绝
const handleReject = (row) => {
  currentRow.value = row
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.reason) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await rejectBorrow(currentRow.value.id, rejectForm.reason)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
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
    if (e !== 'cancel') console.error(e)
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
  try {
    await renewBorrow(currentRow.value.id, renewForm.newReturnDate)
    ElMessage.success('续借成功')
    renewDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

const disabledRenewDate = (date) => {
  if (!currentRow.value) return true
  const expected = new Date(currentRow.value.expectedReturnDate)
  return date <= expected
}

// 查看详情
const handleView = (row) => {
  // TODO: 弹窗显示详情
  console.log('查看详情', row)
}

// 工具函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

const getStatusName = (status) => {
  const map = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    BORROWED: '借出中',
    RETURNED: '已归还',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    BORROWED: '',
    RETURNED: 'info',
    CANCELLED: 'info'
  }
  return map[status] || ''
}

const isOverdue = (row) => {
  if (row.status !== 'BORROWED') return false
  return new Date(row.expectedReturnDate) < new Date()
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.borrow-list {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;
  
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
</style>
