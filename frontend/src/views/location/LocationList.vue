<template>
  <div class="location-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>存放位置管理</span>
          <div class="header-actions">
            <el-input
              v-model="filters.keyword"
              placeholder="位置编码/名称/区域/架号"
              clearable
              style="width: 220px; margin-right: 10px"
              @keyup.enter="handleSearch"
            />
            <el-select
              v-model="filters.roomName"
              placeholder="选择库房"
              clearable
              style="width: 150px; margin-right: 10px"
              @change="handleSearch"
            >
              <el-option
                v-for="room in rooms"
                :key="room"
                :label="room"
                :value="room"
              />
            </el-select>
            <el-select
              v-model="filters.status"
              placeholder="状态"
              clearable
              style="width: 100px; margin-right: 10px"
              @change="handleSearch"
            >
              <el-option
                label="可用"
                value="AVAILABLE"
              />
              <el-option
                label="已满"
                value="FULL"
              />
              <el-option
                label="停用"
                value="DISABLED"
              />
            </el-select>
            <el-button
              type="primary"
              @click="handleCreate"
            >
              <el-icon><Plus /></el-icon> 新增位置
            </el-button>
            <el-button @click="handleReset">
              重置
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="locations"
        stripe
      >
        <el-table-column
          prop="locationCode"
          label="位置编码"
          width="150"
        />
        <el-table-column
          prop="locationName"
          label="位置名称"
          min-width="200"
        />
        <el-table-column
          prop="roomName"
          label="库房"
          width="120"
        />
        <el-table-column
          prop="area"
          label="区域"
          width="80"
        />
        <el-table-column
          prop="shelfNo"
          label="架号"
          width="80"
        />
        <el-table-column
          prop="layerNo"
          label="层号"
          width="80"
        />
        <el-table-column
          label="容量"
          width="120"
        >
          <template #default="{ row }">
            {{ row.usedCapacity || 0 }} / {{ row.totalCapacity || 0 }}
          </template>
        </el-table-column>
        <el-table-column
          label="使用率"
          width="150"
        >
          <template #default="{ row }">
            <el-progress
              :percentage="getUsagePercent(row)"
              :status="row.status === 'FULL' ? 'exception' : ''"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              删除
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
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑位置' : '新增位置'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item
          label="位置编码"
          prop="locationCode"
        >
          <el-input
            v-model="form.locationCode"
            placeholder="请输入位置编码"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item
          label="位置名称"
          prop="locationName"
        >
          <el-input
            v-model="form.locationName"
            placeholder="请输入位置名称"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item
              label="库房名称"
              prop="roomName"
            >
              <el-input
                v-model="form.roomName"
                placeholder="请输入库房名称"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区域">
              <el-input
                v-model="form.area"
                placeholder="如：A区"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="架号">
              <el-input
                v-model="form.shelfNo"
                placeholder="如：001"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="层号">
              <el-input
                v-model="form.layerNo"
                placeholder="如：01"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总容量">
              <el-input-number
                v-model="form.totalCapacity"
                :min="0"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select
                v-model="form.status"
                style="width: 100%"
              >
                <el-option
                  label="可用"
                  value="AVAILABLE"
                />
                <el-option
                  label="已满"
                  value="FULL"
                />
                <el-option
                  label="停用"
                  value="DISABLED"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input
            v-model="form.remarks"
            type="textarea"
            :rows="2"
            placeholder="备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { locationApi } from '@/api/archive'

const loading = ref(false)
const locations = ref([])
const rooms = ref([])

const filters = reactive({
  roomName: '',
  status: '',
  keyword: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const currentId = ref(null)

const form = ref({
  locationCode: '',
  locationName: '',
  roomName: '',
  area: '',
  shelfNo: '',
  layerNo: '',
  totalCapacity: 0,
  status: 'AVAILABLE',
  remarks: ''
})

const formRules = {
  locationCode: [{ required: true, message: '请输入位置编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入位置名称', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入库房名称', trigger: 'blur' }]
}

// 获取使用率
const getUsagePercent = (row) => {
  if (!row.totalCapacity || row.totalCapacity === 0) return 0
  return Math.round((row.usedCapacity || 0) / row.totalCapacity * 100)
}

const getStatusName = (status) => {
  const map = {
    AVAILABLE: '可用',
    FULL: '已满',
    DISABLED: '停用'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    AVAILABLE: 'success',
    FULL: 'danger',
    DISABLED: 'info'
  }
  return map[status] || ''
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await locationApi.list({
      roomName: filters.roomName,
      status: filters.status,
      keyword: filters.keyword,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    locations.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || locations.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 加载库房列表
const loadRooms = async () => {
  try {
    const res = await locationApi.getRooms()
    rooms.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  filters.roomName = ''
  filters.status = ''
  filters.keyword = ''
  handleSearch()
}

// 新增
const handleCreate = () => {
  isEdit.value = false
  currentId.value = null
  form.value = {
    locationCode: '',
    locationName: '',
    roomName: '',
    area: '',
    shelfNo: '',
    layerNo: '',
    totalCapacity: 0,
    status: 'AVAILABLE',
    remarks: ''
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  currentId.value = row.id
  form.value = {
    locationCode: row.locationCode,
    locationName: row.locationName,
    roomName: row.roomName || '',
    area: row.area || '',
    shelfNo: row.shelfNo || '',
    layerNo: row.layerNo || '',
    totalCapacity: row.totalCapacity || 0,
    status: row.status || 'AVAILABLE',
    remarks: row.remarks || ''
  }
  dialogVisible.value = true
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await locationApi.update(currentId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await locationApi.create(form.value)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
    loadRooms()
  } catch (e) {
    if (e !== false) {
      console.error(e)
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除位置【${row.locationName}】吗？`, '提示', {
      type: 'warning'
    })
    await locationApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
    loadRooms()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadData()
  loadRooms()
})
</script>

<style lang="scss" scoped>
.location-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
