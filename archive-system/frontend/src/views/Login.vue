<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <el-icon :size="48" color="#409eff"><Files /></el-icon>
        <h1>档案管理系统</h1>
        <p class="subtitle">Archive Management System</p>
      </div>
      
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="User"
            placeholder="用户名"
            size="large"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            prefix-icon="Lock"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>默认管理员: admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

// 检查是否已登录
onMounted(() => {
  userStore.init()
  if (userStore.isLoggedIn) {
    router.push(route.query.redirect || '/')
  }
})

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    
    await userStore.login(form)
    
    ElMessage.success('登录成功')
    
    // 跳转到之前的页面或首页
    const redirect = route.query.redirect || '/'
    router.push(redirect)
    
  } catch (e) {
    console.error('登录失败', e)
    // 错误消息已在API拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1e3a5f 0%, #2c5282 50%, #3182ce 100%);
  position: relative;
  
  // 简单的背景纹理
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
      radial-gradient(circle at 20% 80%, rgba(255, 255, 255, 0.05) 0%, transparent 50%),
      radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.05) 0%, transparent 50%);
    pointer-events: none;
  }
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  
  .el-icon {
    background: linear-gradient(135deg, #409eff 0%, #2563eb 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  h1 {
    margin-top: 16px;
    margin-bottom: 6px;
    font-size: 22px;
    color: #1f2937;
    font-weight: 600;
    letter-spacing: 1px;
  }
  
  .subtitle {
    margin: 0;
    font-size: 12px;
    color: #9ca3af;
    letter-spacing: 0.5px;
  }
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }
  
  :deep(.el-input__wrapper) {
    border-radius: 6px;
    
    &:hover {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }
  
  :deep(.el-button--primary) {
    border-radius: 6px;
    font-weight: 500;
    letter-spacing: 2px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  
  p {
    margin: 0;
    font-size: 12px;
    color: #9ca3af;
  }
}
</style>
