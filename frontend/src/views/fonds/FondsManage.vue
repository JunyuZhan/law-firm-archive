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
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="全宗名称"
          prop="fondsName"
        >
          <el-input
            v-model="form.fondsName"
            placeholder="全宗名称"
            maxlength="200"
            show-word-limit
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
              v-for="item in fondsTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
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
  getFondsDetail,
  getFondsStatistics
} from '@/api/fonds'
import {
  getFondsTypeName,
  getFondsTypeTag,
  getFondsTypeOptions
} from '@/utils/archiveEnums'

// 下拉选项
const fondsTypeOptions = getFondsTypeOptions()

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

const requireTrimmedText = (message) => ({
  validator: (_rule, value, callback) => {
    if (!value?.trim()) {
      callback(new Error(message))
      return
    }
    callback()
  },
  trigger: 'blur'
})

const rules = {
  fondsNo: [requireTrimmedText('请输入全宗号')],
  fondsName: [requireTrimmedText('请输入全宗名称')],
  fondsType: [{ required: true, message: '请选择全宗类型', trigger: 'change' }]
}

// 注：getFondsTypeName, getFondsTypeTag 已从 archiveEnums.js 导入
const getTypeName = getFondsTypeName
const getTypeTag = getFondsTypeTag

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
  currentFonds.value = null
  statistics.value = {
    archiveCount: row.archiveCount || 0
  }

  const [detailRes, statsRes] = await Promise.allSettled([
      getFondsDetail(row.id),
      getFondsStatistics(row.id)
    ])

  currentFonds.value = detailRes.status === 'fulfilled'
    ? (detailRes.value.data || row)
    : row

  if (statsRes.status === 'fulfilled') {
    statistics.value = {
      archiveCount: row.archiveCount || 0,
      ...(statsRes.value.data || {})
    }
  }

  if (detailRes.status === 'rejected' && statsRes.status === 'rejected') {
    ElMessage.error('加载全宗详情失败')
  }

  detailVisible.value = true
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const payload = {
      ...form,
      fondsNo: form.fondsNo.trim(),
      fondsName: form.fondsName.trim(),
      description: form.description?.trim() || ''
    }

    let res
    if (isEdit.value) {
      res = await updateFonds(currentFonds.value.id, payload)
      const fondsName = res?.data?.fondsName || form.fondsName
      ElMessage.success(fondsName ? `已更新全宗：${fondsName}` : '更新成功')
    } else {
      res = await createFonds(payload)
      const fondsName = res?.data?.fondsName || form.fondsName
      ElMessage.success(fondsName ? `已创建全宗：${fondsName}` : '创建成功')
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
    ElMessage.success(`已删除全宗：${row.fondsName}`)
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
