<template>
  <div class="role-manage">
    <div class="page-header">
      <h1>角色管理</h1>
      <p>集中维护角色定义与权限边界，保证各类后台角色的职责清晰且可审计。</p>
    </div>

    <el-card
      shadow="never"
      class="table-card"
    >
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="roleCode"
          label="角色代码"
          width="150"
        />
        <el-table-column
          prop="roleName"
          label="角色名称"
          width="180"
        />
        <el-table-column
          prop="description"
          label="描述"
          min-width="250"
          show-overflow-tooltip
        />
        <el-table-column
          prop="userCount"
          label="用户数"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              type="info"
              size="small"
            >
              {{ row.userCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
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
          prop="createdAt"
          label="创建时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              @click="handlePermission(row)"
            >
              权限
            </el-button>
            <el-popconfirm
              v-if="!isBuiltInRole(row)"
              title="确定删除该角色吗？"
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
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="角色代码"
          prop="roleCode"
        >
          <el-input
            v-model="form.roleCode"
            placeholder="如：SYSTEM_ADMIN、ARCHIVE_MANAGER"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item
          label="角色名称"
          prop="roleName"
        >
          <el-input
            v-model="form.roleName"
            placeholder="角色名称"
          />
        </el-form-item>
        <el-form-item
          v-if="isEdit"
          label="状态"
        >
          <el-radio-group v-model="form.status">
            <el-radio label="ACTIVE">
              启用
            </el-radio>
            <el-radio
              label="DISABLED"
              :disabled="isEditingBuiltInRole"
            >
              停用
            </el-radio>
          </el-radio-group>
          <div
            v-if="isEditingBuiltInRole"
            class="form-tip"
          >
            系统内置角色不可停用，避免影响系统授权链路。
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="角色描述"
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRoleList, createRole, updateRole, deleteRole } from '@/api/role'

// 状态
const loading = ref(false)
const saving = ref(false)
const tableData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const currentRole = ref(null)

const formRef = ref(null)
const form = reactive({
  roleCode: '',
  roleName: '',
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
  roleCode: [
    requireTrimmedText('请输入角色代码'),
    { pattern: /^[A-Z_]+$/, message: '只能包含大写字母和下划线', trigger: 'blur' }
  ],
  roleName: [requireTrimmedText('请输入角色名称')]
}

const builtInRoleCodes = new Set([
  'SYSTEM_ADMIN',
  'SECURITY_ADMIN',
  'AUDIT_ADMIN',
  'ARCHIVE_MANAGER',
  'ARCHIVE_REVIEWER',
  'USER'
])
const isEditingBuiltInRole = computed(() => isEdit.value && isBuiltInRole(currentRole.value))

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList()
    tableData.value = res.data || []
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  Object.assign(form, {
    roleCode: '',
    roleName: '',
    status: 'ACTIVE',
    description: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  currentRole.value = row
  Object.assign(form, {
    roleCode: row.roleCode,
    roleName: row.roleName,
    status: row.status || 'ACTIVE',
    description: row.description
  })
  dialogVisible.value = true
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const payload = {
      ...form,
      roleCode: form.roleCode.trim(),
      roleName: form.roleName.trim(),
      description: form.description?.trim() || '',
      status: isEditingBuiltInRole.value ? 'ACTIVE' : form.status
    }

    let res
    if (isEdit.value) {
      res = await updateRole(currentRole.value.id, payload)
      const roleName = res?.data?.roleName || form.roleName
      ElMessage.success(roleName ? `已更新角色：${roleName}` : '更新成功')
    } else {
      res = await createRole(payload)
      const roleName = res?.data?.roleName || form.roleName
      ElMessage.success(roleName ? `已创建角色：${roleName}` : '创建成功')
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
  if (isBuiltInRole(row)) {
    ElMessage.warning('系统内置角色不可删除')
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success(`已删除角色：${row.roleName}`)
    loadData()
  } catch (e) {
    console.error('删除失败', e)
  }
}

const isBuiltInRole = (row) => builtInRoleCodes.has(row?.roleCode)

// 权限配置
const handlePermission = async (row) => {
  currentRole.value = row
  await ElMessageBox.alert(
    `角色「${row.roleName}」的权限持久化接口尚未接入，当前版本不支持在此页面保存权限配置。`,
    '功能未接入',
    {
      confirmButtonText: '知道了'
    }
  )
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.role-manage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  h1 {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  p {
    margin: 0;
    line-height: 1.6;
    color: #606266;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  border-radius: 10px;
}
</style>
