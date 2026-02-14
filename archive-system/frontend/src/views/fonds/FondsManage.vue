<template>
  <div class="fonds-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>全宗管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            新增全宗
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键字">
          <el-input
            v-model="searchKeyword"
            placeholder="全宗号/名称"
            clearable
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="loadData"
          >
            查询
          </el-button>
          <el-button @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="fondsNo"
          label="全宗号"
          width="120"
        />
        <el-table-column
          prop="fondsName"
          label="全宗名称"
          min-width="200"
        />
        <el-table-column
          prop="fondsType"
          label="类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.fondsType)">
              {{ getTypeName(row.fondsType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="archiveCount"
          label="档案数"
          width="100"
          align="center"
        />
        <el-table-column
          prop="description"
          label="描述"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ACTIVE' ? 'success' : 'info'"
              size="small"
            >
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              link
              type="primary"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除该全宗吗？"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  link
                  type="danger"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="全宗号"
          prop="fondsNo"
        >
          <el-input
            v-model="form.fondsNo"
            placeholder="如：QZ001"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item
          label="全宗名称"
          prop="fondsName"
        >
          <el-input
            v-model="form.fondsName"
            placeholder="全宗名称"
          />
        </el-form-item>
        <el-form-item
          label="全宗类型"
          prop="fondsType"
        >
          <el-select
            v-model="form.fondsType"
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option
              label="内部全宗"
              value="INTERNAL"
            />
            <el-option
              label="外部全宗"
              value="EXTERNAL"
            />
            <el-option
              label="历史全宗"
              value="HISTORICAL"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">
              启用
            </el-radio>
            <el-radio label="INACTIVE">
              停用
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="全宗描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="全宗详情"
      width="600px"
    >
      <el-descriptions
        v-if="currentFonds"
        :column="2"
        border
      >
        <el-descriptions-item label="全宗号">
          {{ currentFonds.fondsNo }}
        </el-descriptions-item>
        <el-descriptions-item label="全宗名称">
          {{ currentFonds.fondsName }}
        </el-descriptions-item>
        <el-descriptions-item label="全宗类型">
          {{ getTypeName(currentFonds.fondsType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ currentFonds.status === 'ACTIVE' ? '启用' : '停用' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="档案数量"
          :span="2"
        >
          {{ statistics.archiveCount || 0 }} 个
        </el-descriptions-item>
        <el-descriptions-item
          label="描述"
          :span="2"
        >
          {{ currentFonds.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item
          label="创建时间"
          :span="2"
        >
          {{ currentFonds.createdAt }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getFondsPage,
  createFonds,
  updateFonds,
  deleteFonds,
  getFondsStatistics
} from '@/api/fonds'

// 状态
const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const detailVisible = ref(false)
const currentFonds = ref(null)
const statistics = ref({})

const formRef = ref(null)
const form = reactive({
  fondsNo: '',
  fondsName: '',
  fondsType: 'INTERNAL',
  status: 'ACTIVE',
  description: ''
})

const rules = {
  fondsNo: [{ required: true, message: '请输入全宗号', trigger: 'blur' }],
  fondsName: [{ required: true, message: '请输入全宗名称', trigger: 'blur' }],
  fondsType: [{ required: true, message: '请选择全宗类型', trigger: 'change' }]
}

// 类型映射
const getTypeName = (type) => {
  const map = {
    INTERNAL: '内部全宗',
    EXTERNAL: '外部全宗',
    HISTORICAL: '历史全宗'
  }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = {
    INTERNAL: 'primary',
    EXTERNAL: 'success',
    HISTORICAL: 'info'
  }
  return map[type] || 'default'
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getFondsPage({
      keyword: searchKeyword.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchKeyword.value = ''
  pageNum.value = 1
  loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增全宗'
  Object.assign(form, {
    fondsNo: '',
    fondsName: '',
    fondsType: 'INTERNAL',
    status: 'ACTIVE',
    description: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑全宗'
  Object.assign(form, {
    fondsNo: row.fondsNo,
    fondsName: row.fondsName,
    fondsType: row.fondsType,
    status: row.status,
    description: row.description
  })
  currentFonds.value = row
  dialogVisible.value = true
}

// 查看
const handleView = async (row) => {
  currentFonds.value = row
  try {
    const res = await getFondsStatistics(row.id)
    statistics.value = res.data
  } catch (e) {
    statistics.value = {}
  }
  detailVisible.value = true
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    if (isEdit.value) {
      await updateFonds(currentFonds.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createFonds(form)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (e) {
    if (e !== false) {
      console.error('保存失败', e)
    }
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await deleteFonds(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error('删除失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.fonds-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
