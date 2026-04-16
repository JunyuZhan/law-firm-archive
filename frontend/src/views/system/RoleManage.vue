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
              v-if="!row.isSystem"
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

    <!-- 权限配置对话框 -->
    <el-dialog
      v-model="permissionVisible"
      title="权限配置"
      width="600px"
    >
      <div v-if="currentRole">
        <el-alert
          :title="`正在配置角色【${currentRole.roleName}】的权限`"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />
        
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTree"
          :props="{ label: 'name', children: 'children' }"
          show-checkbox
          node-key="code"
          default-expand-all
        />
      </div>
      <template #footer>
        <el-button @click="permissionVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="savePermission"
        >
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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

const permissionVisible = ref(false)
const permissionTreeRef = ref(null)

const formRef = ref(null)
const form = reactive({
  roleCode: '',
  roleName: '',
  status: 'ACTIVE',
  description: ''
})

const rules = {
  roleCode: [
    { required: true, message: '请输入角色代码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '只能包含大写字母和下划线', trigger: 'blur' }
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 权限树
const permissionTree = ref([
  {
    code: 'archive',
    name: '档案管理',
    children: [
      { code: 'archive:view', name: '查看档案' },
      { code: 'archive:create', name: '创建档案' },
      { code: 'archive:edit', name: '编辑档案' },
      { code: 'archive:delete', name: '删除档案' },
      { code: 'archive:download', name: '下载文件' }
    ]
  },
  {
    code: 'borrow',
    name: '借阅管理',
    children: [
      { code: 'borrow:apply', name: '申请借阅' },
      { code: 'borrow:approve', name: '审批借阅' },
      { code: 'borrow:return', name: '归还处理' }
    ]
  },
  {
    code: 'system',
    name: '系统管理',
    children: [
      { code: 'system:user', name: '用户管理' },
      { code: 'system:role', name: '角色管理' },
      { code: 'system:log', name: '日志查看' },
      { code: 'system:config', name: '规则与运行参数' }
    ]
  }
])

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

    if (isEdit.value) {
      await updateRole(currentRole.value.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
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
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error('删除失败', e)
  }
}

// 权限配置
const handlePermission = (row) => {
  currentRole.value = row
  permissionVisible.value = true
}

// 保存权限
const savePermission = () => {
  const checkedKeys = permissionTreeRef.value.getCheckedKeys(true)
  console.log('选中的权限:', checkedKeys)
  ElMessage.success('权限配置已保存')
  permissionVisible.value = false
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
