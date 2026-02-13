<template>
  <div class="borrow-list">
    <el-card>
      <template #header>
        <span>借阅管理</span>
      </template>

      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="待审批" name="PENDING" />
        <el-tab-pane label="借出中" name="BORROWED" />
        <el-tab-pane label="已归还" name="RETURNED" />
        <el-tab-pane label="全部" name="" />
      </el-tabs>

      <el-table :data="borrows" v-loading="loading" stripe>
        <el-table-column prop="borrowNo" label="借阅编号" width="140" />
        <el-table-column prop="archiveNo" label="档案编号" width="140" />
        <el-table-column prop="borrowerName" label="借阅人" width="100" />
        <el-table-column prop="borrowerDept" label="部门" width="120" />
        <el-table-column prop="borrowReason" label="借阅原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="expectedReturnDate" label="预计归还" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" link @click="handleApprove(row, true)">
              同意
            </el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" link @click="handleApprove(row, false)">
              拒绝
            </el-button>
            <el-button v-if="row.status === 'BORROWED'" type="primary" link @click="handleReturn(row)">
              归还
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { borrowApi } from '@/api/archive'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const borrows = ref([])
const activeTab = ref('PENDING')

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    BORROWED: 'primary',
    RETURNED: 'info',
    OVERDUE: 'danger'
  }
  return map[status] || 'info'
}

const getStatusName = (status) => {
  const map = {
    PENDING: '待审批',
    APPROVED: '已批准',
    REJECTED: '已拒绝',
    BORROWED: '借出中',
    RETURNED: '已归还',
    OVERDUE: '已逾期'
  }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await borrowApi.list({ status: activeTab.value })
    borrows.value = res.data?.list || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row, approved) => {
  try {
    const action = approved ? '同意' : '拒绝'
    const { value } = await ElMessageBox.prompt(`请输入${action}意见`, '审批确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await borrowApi.approve(row.id, approved, value)
    ElMessage.success('审批成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleReturn = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入归还说明', '归还确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await borrowApi.return(row.id, value)
    ElMessage.success('归还成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>
