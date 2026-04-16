<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 个人信息 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>个人信息</span>
            </div>
          </template>
          
          <el-form 
            ref="profileFormRef" 
            :model="profileForm" 
            :rules="profileRules"
            label-width="80px"
          >
            <el-form-item label="用户名">
              <el-input
                v-model="profileForm.username"
                disabled
              />
            </el-form-item>
            <el-form-item
              label="姓名"
              prop="realName"
            >
              <el-input
                v-model="profileForm.realName"
                placeholder="请输入姓名"
                maxlength="50"
              />
            </el-form-item>
            <el-form-item
              label="邮箱"
              prop="email"
            >
              <el-input
                v-model="profileForm.email"
                placeholder="请输入邮箱"
                maxlength="100"
              />
            </el-form-item>
            <el-form-item
              label="手机号"
              prop="phone"
            >
              <el-input
                v-model="profileForm.phone"
                placeholder="请输入手机号"
                maxlength="11"
              />
            </el-form-item>
            <el-form-item label="部门">
              <el-input
                v-model="profileForm.department"
                placeholder="请输入部门"
                maxlength="100"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="profileLoading"
                @click="handleUpdateProfile"
              >
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      
      <!-- 修改密码 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>
          
          <el-form 
            ref="passwordFormRef" 
            :model="passwordForm" 
            :rules="passwordRules"
            label-width="100px"
          >
            <el-form-item
              label="当前密码"
              prop="oldPassword"
            >
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password"
                placeholder="请输入当前密码"
                show-password
                maxlength="50"
              />
            </el-form-item>
            <el-form-item
              label="新密码"
              prop="newPassword"
            >
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password"
                placeholder="请输入新密码"
                show-password
                maxlength="50"
                @input="checkPasswordStrength"
              />
              <!-- 密码强度指示器 -->
              <div
                v-if="passwordForm.newPassword"
                class="password-strength"
              >
                <div class="strength-bar">
                  <div 
                    class="strength-level" 
                    :class="passwordStrength.level"
                    :style="{ width: passwordStrength.percent + '%' }"
                  />
                </div>
                <span
                  class="strength-text"
                  :class="passwordStrength.level"
                >
                  {{ passwordStrength.text }}
                </span>
              </div>
              <!-- 密码要求提示 -->
              <div
                v-if="passwordForm.newPassword"
                class="password-requirements"
              >
                <div :class="{ met: hasMinLength }">
                  <el-icon><component :is="hasMinLength ? 'Check' : 'Close'" /></el-icon>
                  至少8位字符
                </div>
                <div :class="{ met: hasLetter }">
                  <el-icon><component :is="hasLetter ? 'Check' : 'Close'" /></el-icon>
                  包含字母
                </div>
                <div :class="{ met: hasNumber }">
                  <el-icon><component :is="hasNumber ? 'Check' : 'Close'" /></el-icon>
                  包含数字
                </div>
                <div :class="{ met: hasSpecial }">
                  <el-icon><component :is="hasSpecial ? 'Check' : 'Close'" /></el-icon>
                  包含特殊字符
                </div>
              </div>
            </el-form-item>
            <el-form-item
              label="确认新密码"
              prop="confirmPassword"
            >
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password"
                placeholder="请再次输入新密码"
                show-password
                maxlength="50"
              />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                :loading="passwordLoading" 
                :disabled="!isPasswordValid"
                @click="handleChangePassword"
              >
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 与「个人信息」区分：此处为系统侧只读记录（角色、启用状态、最近登录） -->
    <el-card
      shadow="never"
      style="margin-top: 20px"
    >
      <template #header>
        <div class="card-header">
          <span>角色与登录信息</span>
        </div>
      </template>
      <p class="account-readonly-hint">
        以下由系统根据管理员配置与您的登录行为生成，仅供查看；姓名、联系方式等在上方「个人信息」中修改。
      </p>
      <el-descriptions
        :column="2"
        border
      >
        <el-descriptions-item label="系统角色">
          <el-tag>{{ getUserTypeName(userStore.userType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="accountStatusTagType">
            {{ accountStatusLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最近登录时间">
          {{ formattedLastLoginAt }}
        </el-descriptions-item>
        <el-descriptions-item label="最近登录 IP">
          {{ accountSummary.lastLoginIp || '—' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword, updateUser, getCurrentUser } from '@/api/user'
import { validatePasswordStrength } from '@/utils/security'

const userStore = useUserStore()
/** 来自 /users/current 的系统侧字段（与可编辑的个人信息区分） */
const accountSummary = reactive({
  status: 'ACTIVE',
  lastLoginAt: null,
  lastLoginIp: ''
})
const profileFormRef = ref()
const passwordFormRef = ref()
const profileLoading = ref(false)
const passwordLoading = ref(false)

// 个人信息表单
const profileForm = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  department: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码强度
const passwordStrength = reactive({
  level: '',
  text: '',
  percent: 0
})

// 密码要求检查
const hasMinLength = computed(() => passwordForm.newPassword.length >= 8)
const hasLetter = computed(() => /[a-zA-Z]/.test(passwordForm.newPassword))
const hasNumber = computed(() => /\d/.test(passwordForm.newPassword))
const hasSpecial = computed(() => /[!@#$%^&*(),.?":{}|<>]/.test(passwordForm.newPassword))

// 密码是否有效
const isPasswordValid = computed(() => {
  return hasMinLength.value && hasLetter.value && hasNumber.value &&
         passwordForm.oldPassword && 
         passwordForm.newPassword === passwordForm.confirmPassword
})

// 检查密码强度
const checkPasswordStrength = () => {
  const password = passwordForm.newPassword
  if (!password) {
    passwordStrength.level = ''
    passwordStrength.text = ''
    passwordStrength.percent = 0
    return
  }
  
  const result = validatePasswordStrength(password)
  
  if (result.score <= 2) {
    passwordStrength.level = 'weak'
    passwordStrength.text = '弱'
    passwordStrength.percent = 33
  } else if (result.score <= 3) {
    passwordStrength.level = 'medium'
    passwordStrength.text = '中'
    passwordStrength.percent = 66
  } else {
    passwordStrength.level = 'strong'
    passwordStrength.text = '强'
    passwordStrength.percent = 100
  }
}

// 个人信息校验规则
const profileRules = {
  realName: [
    { max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 密码校验规则
const validateNewPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度至少8位'))
  } else if (!/[a-zA-Z]/.test(value)) {
    callback(new Error('密码必须包含字母'))
  } else if (!/\d/.test(value)) {
    callback(new Error('密码必须包含数字'))
  } else if (value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与原密码相同'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 用户类型名称
const getUserTypeName = (type) => {
  const map = {
    'SYSTEM_ADMIN': '系统管理员',
    'ARCHIVE_REVIEWER': '档案审核员',
    'ARCHIVE_MANAGER': '档案管理员',
    'USER': '普通用户'
  }
  return map[type] || type
}

const formatDateTime = (value) => {
  if (value == null || value === '') {
    return '—'
  }
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    return String(value)
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(d)
}

const accountStatusLabel = computed(() => {
  const s = (accountSummary.status || '').toUpperCase()
  if (s === 'ACTIVE') {
    return '正常'
  }
  if (s === 'DISABLED') {
    return '已停用'
  }
  return s || '未知'
})

const accountStatusTagType = computed(() => {
  const s = (accountSummary.status || '').toUpperCase()
  if (s === 'ACTIVE') {
    return 'success'
  }
  if (s === 'DISABLED') {
    return 'danger'
  }
  return 'info'
})

const formattedLastLoginAt = computed(() => formatDateTime(accountSummary.lastLoginAt))

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const res = await getCurrentUser()
    const user = res.data
    profileForm.username = user.username
    profileForm.realName = user.realName || ''
    profileForm.email = user.email || ''
    profileForm.phone = user.phone || ''
    profileForm.department = user.department || ''
    accountSummary.status = user.status || 'ACTIVE'
    accountSummary.lastLoginAt = user.lastLoginAt ?? null
    accountSummary.lastLoginIp = user.lastLoginIp || ''
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

// 更新个人信息
const handleUpdateProfile = async () => {
  try {
    await profileFormRef.value.validate()
    profileLoading.value = true
    
    await updateUser(userStore.userId, {
      realName: profileForm.realName,
      email: profileForm.email,
      phone: profileForm.phone,
      department: profileForm.department
    })
    
    // 更新 store
    userStore.realName = profileForm.realName
    
    ElMessage.success('个人信息更新成功')
  } catch (e) {
    console.error('更新失败', e)
  } finally {
    profileLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    // 二次确认
    await ElMessageBox.confirm(
      '修改密码后需要重新登录，确定要修改吗？',
      '安全提示',
      {
        confirmButtonText: '确定修改',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    passwordLoading.value = true
    
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    
    ElMessage.success('密码修改成功，请重新登录')
    
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordStrength.level = ''
    passwordStrength.text = ''
    passwordStrength.percent = 0
    
    // 退出登录
    setTimeout(() => {
      userStore.logout()
    }, 1500)
  } catch (e) {
    if (e !== 'cancel') {
      console.error('修改密码失败', e)
    }
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style lang="scss" scoped>
.profile-page {
  .card-header {
    font-weight: 500;
  }

  .account-readonly-hint {
    margin: 0 0 14px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--el-text-color-secondary);
  }

  .el-form {
    max-width: 400px;
  }
}

// 密码强度指示器
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  
  .strength-bar {
    flex: 1;
    height: 4px;
    background: #e5e7eb;
    border-radius: 2px;
    overflow: hidden;
    
    .strength-level {
      height: 100%;
      transition: width 0.3s, background-color 0.3s;
      
      &.weak { background: #ef4444; }
      &.medium { background: #f59e0b; }
      &.strong { background: #10b981; }
    }
  }
  
  .strength-text {
    font-size: 12px;
    min-width: 20px;
    
    &.weak { color: #ef4444; }
    &.medium { color: #f59e0b; }
    &.strong { color: #10b981; }
  }
}

// 密码要求提示
.password-requirements {
  margin-top: 10px;
  font-size: 12px;
  
  > div {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #9ca3af;
    margin-bottom: 4px;
    
    .el-icon {
      font-size: 14px;
      color: #ef4444;
    }
    
    &.met {
      color: #10b981;
      
      .el-icon {
        color: #10b981;
      }
    }
  }
}
</style>
