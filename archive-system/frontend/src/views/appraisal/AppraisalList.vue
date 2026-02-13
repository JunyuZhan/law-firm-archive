<template>
  <div class="appraisal-list">
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="鉴定列表" name="all" />
      <el-tab-pane label="待审批" name="pending" />
      <el-tab-pane label="到期预警" name="expiring" />
    </el-tabs>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card" v-if="activeTab === 'all'">
      <el-form inline>
        <el-form-item label="鉴定类型">
          <el-select v-model="filters.type" placeholder="全部" clearable style="width: 120px">
            <el-option label="密级鉴定" value="SECURITY" />
            <el-option label="期限鉴定" value="RETENTION" />
            <el-option label="开放鉴定" value="OPEN" />
            <el-option label="价值鉴定" value="VALUE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审批" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="handleCreate">发起鉴定</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 到期预警筛选 -->
    <el-card shadow="never" class="filter-card" v-if="activeTab === 'expiring'">
      <el-form inline>
        <el-form-item label="预警天数">
          <el-select v-model="expiringDays" style="width: 120px" @change="fetchExpiringData">
            <el-option label="30天内" :value="30" />
            <el-option label="60天内" :value="60" />
            <el-option label="90天内" :value="90" />
            <el-option label="180天内" :value="180" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" plain @click="showExpired = !showExpired">
            {{ showExpired ? '显示即将到期' : '显示已到期' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 鉴定列表 -->
    <el-card shadow="never" v-if="activeTab !== 'expiring'">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="鉴定类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.appraisalType)" size="small">
              {{ getTypeName(row.appraisalType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="archiveId" label="档案ID" width="100" />
        <el-table-column label="变更内容" min-width="200">
          <template #default="{ row }">
            <span class="change-info">
              <span class="old-value">{{ row.originalValue || '-' }}</span>
              <el-icon><Right /></el-icon>
              <span class="new-value">{{ row.newValue }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="appraisalReason" label="鉴定原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="appraiserName" label="鉴定人" width="100" />
        <el-table-column label="鉴定时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.appraisedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link size="small" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button type="danger" link size="small" @click="handleReject(row)">
                拒绝
              </el-button>
            </template>
            <el-button link size="small" @click="handleView(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="pagination.total > pagination.pageSize">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 到期预警列表 -->
    <el-card shadow="never" v-if="activeTab === 'expiring'">
      <template #header>
        <span>{{ showExpired ? '已到期档案' : '即将到期档案' }}</span>
        <el-tag class="ml-2" type="danger">{{ expiringData.length }} 条</el-tag>
      </template>
      <el-table :data="expiringData" v-loading="loading" stripe>
        <el-table-column prop="archiveNo" label="档案号" width="150" />
        <el-table-column prop="title" label="题名" min-width="200" show-overflow-tooltip />
        <el-table-column label="保管期限" width="100">
          <template #default="{ row }">
            {{ getRetentionName(row.retentionPeriod) }}
          </template>
        </el-table-column>
        <el-table-column label="到期日期" width="120">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isExpired(row.retentionExpireDate) }">
              {{ row.retentionExpireDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余天数" width="100">
          <template #default="{ row }">
            <span :class="getDaysClass(row.retentionExpireDate)">
              {{ getDaysRemaining(row.retentionExpireDate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleExtend(row)">
              延期
            </el-button>
            <el-button type="warning" link size="small" @click="handleAppraisal(row)">
              鉴定
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发起鉴定弹窗 -->
    <el-dialog v-model="createDialogVisible" title="发起鉴定" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="档案ID" prop="archiveId">
          <el-input v-model.number="createForm.archiveId" placeholder="请输入档案ID" />
        </el-form-item>
        <el-form-item label="鉴定类型" prop="appraisalType">
          <el-select v-model="createForm.appraisalType" placeholder="请选择" style="width: 100%">
            <el-option label="密级鉴定" value="SECURITY" />
            <el-option label="期限鉴定" value="RETENTION" />
            <el-option label="开放鉴定" value="OPEN" />
            <el-option label="价值鉴定" value="VALUE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原值">
          <el-input v-model="createForm.originalValue" placeholder="当前值（可选）" />
        </el-form-item>
        <el-form-item label="新值" prop="newValue">
          <el-select v-model="createForm.newValue" placeholder="请选择新值" style="width: 100%"
                     v-if="createForm.appraisalType === 'SECURITY'">
            <el-option label="公开" value="PUBLIC" />
            <el-option label="内部" value="INTERNAL" />
            <el-option label="秘密" value="SECRET" />
            <el-option label="机密" value="CONFIDENTIAL" />
          </el-select>
          <el-select v-model="createForm.newValue" placeholder="请选择新值" style="width: 100%"
                     v-else-if="createForm.appraisalType === 'RETENTION'">
            <el-option label="永久" value="PERMANENT" />
            <el-option label="30年" value="Y30" />
            <el-option label="15年" value="Y15" />
            <el-option label="10年" value="Y10" />
            <el-option label="5年" value="Y5" />
          </el-select>
          <el-select v-model="createForm.newValue" placeholder="请选择" style="width: 100%"
                     v-else-if="createForm.appraisalType === 'OPEN'">
            <el-option label="开放" value="OPEN" />
            <el-option label="不开放" value="CLOSED" />
          </el-select>
          <el-input v-model="createForm.newValue" placeholder="请输入新值" v-else />
        </el-form-item>
        <el-form-item label="鉴定原因" prop="appraisalReason">
          <el-input v-model="createForm.appraisalReason" type="textarea" :rows="3" placeholder="请输入鉴定原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveDialogVisible" title="审批" width="500px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝鉴定" width="500px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectForm.comment" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 延期弹窗 -->
    <el-dialog v-model="extendDialogVisible" title="延长保管期限" width="500px">
      <el-form :model="extendForm" label-width="100px">
        <el-form-item label="档案信息">
          <div>{{ currentArchive?.archiveNo }} - {{ currentArchive?.title }}</div>
        </el-form-item>
        <el-form-item label="当前期限">
          {{ getRetentionName(currentArchive?.retentionPeriod) }}
        </el-form-item>
        <el-form-item label="新保管期限" required>
          <el-select v-model="extendForm.newRetentionPeriod" placeholder="请选择" style="width: 100%">
            <el-option label="永久" value="PERMANENT" />
            <el-option label="30年" value="Y30" />
            <el-option label="15年" value="Y15" />
            <el-option label="10年" value="Y10" />
            <el-option label="5年" value="Y5" />
          </el-select>
        </el-form-item>
        <el-form-item label="延期原因" required>
          <el-input v-model="extendForm.reason" type="textarea" :rows="3" placeholder="请输入延期原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExtend">确认延期</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="鉴定详情" width="600px">
      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="鉴定ID">{{ detailData.id }}</el-descriptions-item>
        <el-descriptions-item label="档案ID">{{ detailData.archiveId }}</el-descriptions-item>
        <el-descriptions-item label="鉴定类型">
          <el-tag :type="getTypeTagType(detailData.appraisalType)" size="small">
            {{ getTypeName(detailData.appraisalType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)" size="small">
            {{ getStatusName(detailData.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原值">{{ detailData.originalValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="新值">{{ detailData.newValue }}</el-descriptions-item>
        <el-descriptions-item label="鉴定原因" :span="2">{{ detailData.appraisalReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="鉴定人">{{ detailData.appraiserName }}</el-descriptions-item>
        <el-descriptions-item label="鉴定时间">{{ formatDateTime(detailData.appraisedAt) }}</el-descriptions-item>
        <el-descriptions-item label="审批人" v-if="detailData.approverName">{{ detailData.approverName }}</el-descriptions-item>
        <el-descriptions-item label="审批时间" v-if="detailData.approvedAt">{{ formatDateTime(detailData.approvedAt) }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2" v-if="detailData.approvalComment">{{ detailData.approvalComment }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Right } from '@element-plus/icons-vue'
import {
  getAppraisalList, getPendingAppraisals, getAppraisalDetail,
  createAppraisal, approveAppraisal, rejectAppraisal,
  getExpiringArchives, getExpiredArchives, extendRetention
} from '@/api/appraisal'

const activeTab = ref('all')
const loading = ref(false)
const tableData = ref([])
const expiringData = ref([])
const expiringDays = ref(90)
const showExpired = ref(false)

const filters = reactive({
  type: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 发起鉴定
const createDialogVisible = ref(false)
const createFormRef = ref(null)
const submitting = ref(false)
const createForm = ref({
  archiveId: null,
  appraisalType: '',
  originalValue: '',
  newValue: '',
  appraisalReason: ''
})
const createRules = {
  archiveId: [{ required: true, message: '请输入档案ID', trigger: 'blur' }],
  appraisalType: [{ required: true, message: '请选择鉴定类型', trigger: 'change' }],
  newValue: [{ required: true, message: '请输入新值', trigger: 'blur' }],
  appraisalReason: [{ required: true, message: '请输入鉴定原因', trigger: 'blur' }]
}

// 审批
const approveDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const currentRow = ref(null)
const approveForm = reactive({ comment: '' })
const rejectForm = reactive({ comment: '' })

// 延期
const extendDialogVisible = ref(false)
const currentArchive = ref(null)
const extendForm = reactive({
  newRetentionPeriod: '',
  reason: ''
})

// 详情
const detailDialogVisible = ref(false)
const detailData = ref(null)

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'all') {
      res = await getAppraisalList({
        type: filters.type,
        status: filters.status,
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    } else if (activeTab.value === 'pending') {
      res = await getPendingAppraisals({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      })
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
}

// 获取到期预警数据
const fetchExpiringData = async () => {
  loading.value = true
  try {
    let res
    if (showExpired.value) {
      res = await getExpiredArchives()
    } else {
      res = await getExpiringArchives(expiringDays.value)
    }
    expiringData.value = res.data || []
  } catch (e) {
    console.error('获取到期预警数据失败', e)
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  pagination.pageNum = 1
  if (activeTab.value === 'expiring') {
    fetchExpiringData()
  } else {
    fetchData()
  }
}

// 监听showExpired变化
watch(showExpired, () => {
  fetchExpiringData()
})

// 发起鉴定
const handleCreate = () => {
  createForm.value = {
    archiveId: null,
    appraisalType: '',
    originalValue: '',
    newValue: '',
    appraisalReason: ''
  }
  createDialogVisible.value = true
}

const submitCreate = async () => {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
    submitting.value = true
    await createAppraisal(createForm.value)
    ElMessage.success('鉴定申请已提交')
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
  try {
    await approveAppraisal(currentRow.value.id, approveForm.comment)
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

const handleReject = (row) => {
  currentRow.value = row
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!rejectForm.comment) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await rejectAppraisal(currentRow.value.id, rejectForm.comment)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

// 查看详情
const handleView = async (row) => {
  try {
    const res = await getAppraisalDetail(row.id)
    detailData.value = res.data
    detailDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

// 延期
const handleExtend = (archive) => {
  currentArchive.value = archive
  extendForm.newRetentionPeriod = ''
  extendForm.reason = ''
  extendDialogVisible.value = true
}

const confirmExtend = async () => {
  if (!extendForm.newRetentionPeriod || !extendForm.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await extendRetention(currentArchive.value.id, extendForm)
    ElMessage.success('保管期限已延长')
    extendDialogVisible.value = false
    fetchExpiringData()
  } catch (e) {
    console.error(e)
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

// 从到期预警发起鉴定
const handleAppraisal = (archive) => {
  createForm.value = {
    archiveId: archive.id,
    appraisalType: 'RETENTION',
    originalValue: archive.retentionPeriod,
    newValue: '',
    appraisalReason: '保管期限到期，需要重新鉴定'
  }
  createDialogVisible.value = true
}

// 工具函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 16)
}

const getTypeName = (type) => {
  const map = {
    SECURITY: '密级鉴定',
    RETENTION: '期限鉴定',
    OPEN: '开放鉴定',
    VALUE: '价值鉴定'
  }
  return map[type] || type
}

const getTypeTagType = (type) => {
  const map = {
    SECURITY: 'danger',
    RETENTION: 'warning',
    OPEN: 'success',
    VALUE: ''
  }
  return map[type] || ''
}

const getStatusName = (status) => {
  const map = {
    PENDING: '待审批',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || ''
}

const getRetentionName = (code) => {
  const map = {
    PERMANENT: '永久',
    Y30: '30年',
    Y15: '15年',
    Y10: '10年',
    Y5: '5年'
  }
  return map[code] || code
}

const isExpired = (dateStr) => {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}

const getDaysRemaining = (dateStr) => {
  if (!dateStr) return '-'
  const diff = new Date(dateStr) - new Date()
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
  if (days < 0) return `已过期 ${-days} 天`
  return `${days} 天`
}

const getDaysClass = (dateStr) => {
  if (!dateStr) return ''
  const diff = new Date(dateStr) - new Date()
  const days = Math.ceil(diff / (1000 * 60 * 60 * 24))
  if (days < 0) return 'text-danger'
  if (days <= 30) return 'text-warning'
  return ''
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.appraisal-list {
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

.change-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .old-value {
    color: #909399;
    text-decoration: line-through;
  }

  .new-value {
    color: #409eff;
    font-weight: 500;
  }
}

.text-danger {
  color: #f56c6c;
  font-weight: bold;
}

.text-warning {
  color: #e6a23c;
  font-weight: bold;
}

.ml-2 {
  margin-left: 8px;
}
</style>
