<template>
  <div class="user-manage">
    <div class="page-header">
      <h1>用户管理</h1>
      <p>统一维护系统用户和账号状态；当前版本的权限仍以用户类型为准，适合系统管理员、安全保密员和安全审计员日常使用。</p>
    </div>

    <!-- 搜索区域 -->
    <el-card
      class="search-card"
      shadow="never"
    >
      <el-form
        :model="queryParams"
        inline
      >
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="用户名/姓名/手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select
            v-model="queryParams.userType"
            placeholder="全部"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="option in userTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryParams.status"
            placeholder="全部"
            clearable
            style="width: 100px"
          >
            <el-option
              label="启用"
              value="ACTIVE"
            />
            <el-option
              label="禁用"
              value="DISABLED"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :icon="Search"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button
            :icon="Refresh"
            @click="handleReset"
          >
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作按钮 -->
    <el-card
      class="table-card"
      shadow="never"
    >
      <template #header>
        <div class="card-header">
          <span>用户列表</span>
          <el-button
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增用户
          </el-button>
        </div>
      </template>

      <!-- 用户表格 -->
      <el-table
        v-loading="loading"
        :data="userList"
        stripe
      >
        <el-table-column
          prop="username"
          label="用户名"
          width="120"
        />
        <el-table-column
          prop="realName"
          label="姓名"
          width="100"
        />
        <el-table-column
          prop="phone"
          label="手机号"
          width="130"
        />
        <el-table-column
          prop="email"
          label="邮箱"
          width="180"
        />
        <el-table-column
          prop="userType"
          label="用户类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.userType)">
              {{ getUserTypeLabel(row.userType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="department"
          label="部门"
          width="120"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="创建时间"
          width="160"
        >
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <div class="operation-buttons">
              <el-button
                link
                type="primary"
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="handleAssignRole(row)"
              >
                分配
              </el-button>
              <el-button
                link
                type="warning"
                size="small"
                @click="handleResetPassword(row)"
              >
                重置
              </el-button>
              <el-button
                link
                type="info"
                size="small"
                @click="handleCheckLockStatus(row)"
              >
                锁定
              </el-button>
              <el-button 
                v-if="!isBuiltInUser(row)"
                link 
                :type="row.status === 'ACTIVE' ? 'danger' : 'success'" 
                size="small" 
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-popconfirm
                v-if="!isBuiltInUser(row)"
                title="确定删除？"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button
                    link
                    type="danger"
                    size="small"
                  >
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        class="pagination"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchUsers"
        @current-change="fetchUsers"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="680px"
      destroy-on-close
      class="user-edit-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="96px"
        class="user-edit-form"
      >
        <div class="form-section-title">
          账号信息
        </div>
        <div class="user-form-grid">
          <el-form-item
            label="用户名"
            prop="username"
          >
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :disabled="!!form.id"
            />
          </el-form-item>
          <el-form-item
            v-if="!form.id"
            label="密码"
            prop="password"
          >
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入初始密码"
              show-password
            />
          </el-form-item>
        </div>
        <div class="form-section-title">
          身份信息
        </div>
        <div class="user-form-grid">
          <el-form-item
            label="姓名"
            prop="realName"
          >
            <el-input
              v-model="form.realName"
              placeholder="请输入姓名"
            />
          </el-form-item>
          <el-form-item
            label="部门"
            prop="department"
          >
            <el-input
              v-model="form.department"
              placeholder="请输入部门"
            />
          </el-form-item>
          <el-form-item
            label="手机号"
            prop="phone"
          >
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
            />
          </el-form-item>
          <el-form-item
            label="邮箱"
            prop="email"
          >
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
            />
          </el-form-item>
          <el-form-item
            label="用户类型"
            prop="userType"
            class="span-2"
          >
            <el-select
              v-model="form.userType"
              placeholder="请选择用户类型"
              :disabled="isEditingBuiltInUser"
              style="width: 100%"
            >
              <el-option
                v-for="option in userTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <div class="form-tip">
              按职责划分系统管理员、安全保密员、安全审计员、档案审核员、档案管理员和普通用户。
            </div>
            <div
              v-if="isEditingBuiltInUser"
              class="form-tip"
            >
              系统内置账号的用户类型不可修改，避免影响默认授权链路。
            </div>
          </el-form-item>
        </div>
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

    <!-- 分配角色弹窗 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="分配角色"
      width="560px"
      destroy-on-close
      class="role-assign-dialog"
    >
      <div class="role-dialog-header">
        <span>用户：</span><strong>{{ currentUser?.realName || currentUser?.username }}</strong>
      </div>
      <el-checkbox-group
        v-model="selectedRoles"
        class="role-checkbox-group"
      >
        <el-checkbox
          v-for="role in roleList"
          :key="role.id"
          :label="role.id"
          :disabled="role.status !== 'ACTIVE'"
        >
          {{ role.roleName }}
          <span
            v-if="role.description"
            style="color: #999; font-size: 12px"
          >({{ role.description }})</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmitRoles"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPwdVisible"
      title="重置密码"
      width="400px"
      destroy-on-close
    >
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-width="80px"
      >
        <el-form-item
          label="新密码"
          prop="newPassword"
        >
          <el-input
            v-model="resetPwdForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item
          label="确认密码"
          prop="confirmPassword"
        >
          <el-input
            v-model="resetPwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmitResetPwd"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, resetPassword, updateUserStatus, getUserLockStatus, unlockUser } from '@/api/user'
import { getRoleOptions } from '@/api/role'
import { normalizeUserType } from '@/utils/permission'

const userTypeOptions = [
  { value: 'SYSTEM_ADMIN', label: '系统管理员' },
  { value: 'SECURITY_ADMIN', label: '安全保密员' },
  { value: 'AUDIT_ADMIN', label: '安全审计员' },
  { value: 'ARCHIVE_REVIEWER', label: '档案审核员' },
  { value: 'ARCHIVE_MANAGER', label: '档案管理员' },
  { value: 'USER', label: '普通用户' }
]

// 查询参数
const queryParams = reactive({
  keyword: '',
  userType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const roleList = ref([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = computed(() => form.value.id ? '编辑用户' : '新增用户')
const formRef = ref()
const form = ref({})
const submitting = ref(false)
const editingUser = ref(null)
const builtInUsernames = new Set(['admin', 'security', 'auditor'])
const isEditingBuiltInUser = computed(() => isBuiltInUser(form.value))

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

const requireNonBlankValue = (message) => ({
  validator: (_rule, value, callback) => {
    if (!value?.trim()) {
      callback(new Error(message))
      return
    }
    callback()
  },
  trigger: 'blur'
})

const formRules = {
  username: [requireTrimmedText('请输入用户名')],
  password: [requireNonBlankValue('请输入密码')],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

// 角色弹窗
const roleDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoles = ref([])

// 重置密码弹窗
const resetPwdVisible = ref(false)
const resetPwdFormRef = ref()
const resetPwdForm = reactive({ newPassword: '', confirmPassword: '' })
const resetPwdRules = {
  newPassword: [requireNonBlankValue('请输入新密码')],
  confirmPassword: [
    requireNonBlankValue('请再次输入新密码'),
    {
      validator: (rule, value, callback) => {
        if (value !== resetPwdForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    userList.value = (res.data.records || []).map((item) => ({
      ...item,
      userType: normalizeUserType(item.userType)
    }))
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  try {
    const res = await getRoleOptions()
    roleList.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchUsers()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.userType = ''
  queryParams.status = ''
  queryParams.pageNum = 1
  fetchUsers()
}

function handleAdd() {
  editingUser.value = null
  form.value = { userType: 'USER' }
  dialogVisible.value = true
}

function handleEdit(row) {
  editingUser.value = row
  form.value = {
    ...row,
    userType: normalizeUserType(row.userType)
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  
  submitting.value = true
  try {
    const payload = {
      ...form.value,
      username: form.value.username?.trim(),
      realName: form.value.realName?.trim() || '',
      email: form.value.email?.trim() || '',
      phone: form.value.phone?.trim() || '',
      department: form.value.department?.trim() || '',
      userType: isEditingBuiltInUser.value
        ? normalizeUserType(editingUser.value?.userType)
        : form.value.userType
    }

    let res
    if (form.value.id) {
      res = await updateUser(form.value.id, payload)
      const userName = res?.data?.realName || res?.data?.username || payload.realName || payload.username
      ElMessage.success(userName ? `已更新用户：${userName}` : '更新成功')
    } else {
      res = await createUser(payload)
      const userName = res?.data?.realName || res?.data?.username || payload.realName || payload.username
      ElMessage.success(userName ? `已创建用户：${userName}` : '创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  if (isBuiltInUser(row)) {
    ElMessage.warning('系统内置账号不可删除')
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success(`已删除用户：${row.realName || row.username}`)
    fetchUsers()
  } catch (e) {
    console.error(e)
  }
}

async function handleToggleStatus(row) {
  if (isBuiltInUser(row)) {
    ElMessage.warning('系统内置账号不可禁用')
    return
  }
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(newStatus === 'ACTIVE' ? '已启用' : '已禁用')
    fetchUsers()
  } catch (e) {
    console.error(e)
  }
}

async function handleAssignRole(row) {
  await ElMessageBox.alert(
    `用户「${row.realName || row.username}」的权限仍由“用户类型”决定，当前版本暂未将这里分配的角色接入登录授权链路。为避免误导，角色分配功能已停用。`,
    '功能未接入',
    {
      confirmButtonText: '知道了',
      type: 'warning'
    }
  )
}

async function handleSubmitRoles() {
  await ElMessageBox.alert(
    '当前版本暂未将用户角色分配接入登录授权链路，请改用“用户类型”控制权限。',
    '功能未接入',
    {
      confirmButtonText: '知道了',
      type: 'warning'
    }
  )
}

function handleResetPassword(row) {
  currentUser.value = row
  resetPwdForm.newPassword = ''
  resetPwdForm.confirmPassword = ''
  resetPwdVisible.value = true
}

async function handleSubmitResetPwd() {
  if (!resetPwdFormRef.value) return
  await resetPwdFormRef.value.validate()
  
  submitting.value = true
  try {
    await resetPassword(currentUser.value.id, resetPwdForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPwdVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handleCheckLockStatus(row) {
  let confirmedUnlock = false
  try {
    const res = await getUserLockStatus(row.id)
    const status = res?.data || {}
    const locked = status.locked === true
    const remainingAttempts = Number(status.remainingAttempts || 0)
    const remainingLockoutSeconds = Number(status.remainingLockoutSeconds || 0)

    if (!locked) {
      ElMessageBox.alert(
        `账号当前未锁定。\n\n剩余尝试次数：${remainingAttempts}`,
        `账号状态：${row.realName || row.username}`,
        {
          confirmButtonText: '知道了'
        }
      )
      return
    }

    await ElMessageBox.confirm(
      `账号当前已锁定。\n\n剩余锁定时间：${formatDurationMinutes(remainingLockoutSeconds)}\n剩余尝试次数：${remainingAttempts}\n\n是否立即解锁？`,
      `账号状态：${row.realName || row.username}`,
      {
        type: 'warning',
        confirmButtonText: '立即解锁',
        cancelButtonText: '取消'
      }
    )

    confirmedUnlock = true
    await unlockUser(row.id)
    ElMessage.success('账号已解锁')
    fetchUsers()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error(e)
      ElMessage.error(e.message || (confirmedUnlock ? '账号解锁失败' : '获取锁定状态失败'))
    }
  }
}

function getUserTypeLabel(type) {
  const normalizedType = normalizeUserType(type)
  const map = {
    SYSTEM_ADMIN: '系统管理员',
    SECURITY_ADMIN: '安全保密员',
    AUDIT_ADMIN: '安全审计员',
    ARCHIVE_REVIEWER: '档案审核员',
    ARCHIVE_MANAGER: '档案管理员',
    USER: '普通用户'
  }
  return map[normalizedType] || normalizedType
}

function getUserTypeTag(type) {
  const normalizedType = normalizeUserType(type)
  const map = {
    SYSTEM_ADMIN: 'danger',
    SECURITY_ADMIN: 'warning',
    AUDIT_ADMIN: 'success',
    ARCHIVE_REVIEWER: 'warning',
    ARCHIVE_MANAGER: 'info',
    USER: ''
  }
  return map[normalizedType] || ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function isBuiltInUser(user) {
  return builtInUsernames.has(String(user?.username || '').trim().toLowerCase())
}

function formatDurationMinutes(seconds) {
  if (!seconds || seconds <= 0) return '不到 1 分钟'
  const minutes = Math.ceil(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  return remainMinutes > 0 ? `${hours} 小时 ${remainMinutes} 分钟` : `${hours} 小时`
}
</script>

<style scoped>
.user-manage {
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

.user-edit-form .form-section-title {
  margin: 4px 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #34495e;
}

.user-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.form-tip {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #8c8c8c;
}

.role-dialog-header {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f5f7fa;
  color: #606266;
}

.role-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow: auto;
}

@media (max-width: 768px) {
  .user-form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}

.search-card {
  border-radius: 10px;
}

.table-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.el-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
