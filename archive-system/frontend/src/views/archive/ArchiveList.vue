<template>
  <div class="archive-list">
    <!-- 搜索区 -->
    <el-card class="search-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="档案编号/名称/客户" clearable />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="queryForm.sourceType" placeholder="请选择" clearable>
            <el-option label="律所系统" value="LAW_FIRM" />
            <el-option label="手动录入" value="MANUAL" />
            <el-option label="批量导入" value="IMPORT" />
            <el-option label="外部系统" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable>
            <el-option label="已接收" value="RECEIVED" />
            <el-option label="待入库" value="PENDING" />
            <el-option label="已入库" value="STORED" />
            <el-option label="借出中" value="BORROWED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>档案列表</span>
          <el-button type="primary" @click="$router.push('/receive')">
            <el-icon><Plus /></el-icon> 手动录入
          </el-button>
        </div>
      </template>

      <el-table :data="archiveList" v-loading="loading" stripe>
        <el-table-column prop="archiveNo" label="档案编号" width="140" />
        <el-table-column prop="archiveName" label="档案名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sourceTypeName" label="来源" width="100" />
        <el-table-column prop="clientName" label="客户" width="120" show-overflow-tooltip />
        <el-table-column prop="archiveTypeName" label="类型" width="100" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hasElectronic" label="电子档案" width="100" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.hasElectronic" color="#67c23a"><Check /></el-icon>
            <el-icon v-else color="#909399"><Close /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="receivedAt" label="接收时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button 
              v-if="row.status === 'RECEIVED'" 
              type="success" 
              link 
              @click="handleStore(row)"
            >
              入库
            </el-button>
            <el-button 
              v-if="row.status === 'STORED'" 
              type="warning" 
              link 
              @click="handleBorrow(row)"
            >
              借阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 入库对话框 -->
    <el-dialog v-model="storeDialogVisible" title="档案入库" width="500px">
      <el-form :model="storeForm" label-width="100px">
        <el-form-item label="存放位置" required>
          <el-select v-model="storeForm.locationId" placeholder="请选择存放位置">
            <el-option
              v-for="loc in locations"
              :key="loc.id"
              :label="loc.locationName"
              :value="loc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="盒号">
          <el-input v-model="storeForm.boxNo" placeholder="请输入盒号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="storeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStore">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { archiveApi, locationApi } from '@/api/archive'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const archiveList = ref([])
const total = ref(0)
const locations = ref([])

const queryForm = reactive({
  keyword: '',
  sourceType: '',
  status: '',
  pageNum: 1,
  pageSize: 20
})

const storeDialogVisible = ref(false)
const storeForm = reactive({
  archiveId: null,
  locationId: null,
  boxNo: ''
})

const getStatusType = (status) => {
  const map = {
    RECEIVED: 'info',
    PENDING: 'warning',
    STORED: 'success',
    BORROWED: 'danger'
  }
  return map[status] || 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await archiveApi.list(queryForm)
    archiveList.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadLocations = async () => {
  try {
    const res = await locationApi.getAvailable()
    locations.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const handleSearch = () => {
  queryForm.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    keyword: '',
    sourceType: '',
    status: '',
    pageNum: 1
  })
  loadData()
}

const handleView = (row) => {
  router.push(`/archives/${row.id}`)
}

const handleStore = (row) => {
  storeForm.archiveId = row.id
  storeForm.locationId = null
  storeForm.boxNo = ''
  storeDialogVisible.value = true
}

const confirmStore = async () => {
  if (!storeForm.locationId) {
    ElMessage.warning('请选择存放位置')
    return
  }
  try {
    await archiveApi.store(storeForm.archiveId, storeForm.locationId, storeForm.boxNo)
    ElMessage.success('入库成功')
    storeDialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  }
}

const handleBorrow = (row) => {
  ElMessage.info('借阅功能开发中')
}

onMounted(() => {
  loadData()
  loadLocations()
})
</script>

<style lang="scss" scoped>
.archive-list {
  .search-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
