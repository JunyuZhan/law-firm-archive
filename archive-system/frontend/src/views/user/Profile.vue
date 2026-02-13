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
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profileForm.realName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="部门">
              <el-input v-model="profileForm.department" placeholder="请输入部门" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateProfile" :loading="profileLoading">
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
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password"
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password"
                placeholder="请输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
          
          <el-alert 
            type="info" 
            :closable="false"
            style="margin-top: 16px"
          >
            <template #title>
              密码要求：至少8位，建议包含字母、数字和特殊字符
            </template>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 账户信息 -->
    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>账户信息</span>
        </div>
      </template>
      
      <el-descriptions :column="3" border>
        <el-descriptions-item label="用户类型">
          <el-tag>{{ getUserTypeName(userStore.userType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账户状态">
          <el-tag type="success">正常</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上次登录">
          {{ userStore.lastLoginAt || '首次登录' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword, updateUser, getCurrentUser } from '@/api/user'

const userStore = useUserStore()
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
    { min: 8, message: '密码长度至少8位', trigger: 'blur' }
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
    'SECURITY_ADMIN': '安全保密员',
    'AUDIT_ADMIN': '安全审计员',
    'ARCHIVIST': '档案员',
    'USER': '普通用户'
  }
  return map[type] || type
}

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
    passwordLoading.value = true
    
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    
    ElMessage.success('密码修改成功，请重新登录')
    
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    
    // 退出登录
    setTimeout(() => {
      userStore.logout()
    }, 1500)
  } catch (e) {
    console.error('修改密码失败', e)
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
  
  .el-form {
    max-width: 400px;
  }
}
</style>
