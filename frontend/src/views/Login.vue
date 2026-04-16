<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <img 
          :src="appStore.systemConfig.logoUrl || '/logo.png'" 
          alt="Logo" 
          class="login-logo"
        >
        <h1>{{ appStore.systemConfig.systemName }}</h1>
        <p class="subtitle">
          {{ appStore.systemConfig.systemNameEn }}
        </p>
      </div>
      
      <!-- 安全提示 -->
      <el-alert
        v-if="securityAlert.show"
        :title="securityAlert.message"
        :type="securityAlert.type"
        show-icon
        :closable="false"
        class="security-alert"
      />
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model.trim="form.username"
            prefix-icon="User"
            placeholder="用户名"
            size="large"
            clearable
            maxlength="50"
            @input="onInputChange"
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
            maxlength="50"
            @keyup.enter="handleLogin"
            @input="onInputChange"
          />
        </el-form-item>
        
        <!-- 验证码 -->
        <el-form-item
          v-if="showCaptcha"
          prop="captcha"
        >
          <div class="captcha-group">
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="请输入右侧4位验证码"
                size="large"
                maxlength="4"
                class="captcha-input"
                @input="onCaptchaInput"
                @keyup.enter="handleLogin"
              />
              <div
                class="captcha-img"
                title="点击刷新验证码"
                role="button"
                aria-label="点击刷新验证码"
                tabindex="0"
                @click="refreshCaptcha"
                @keydown.enter.prevent="refreshCaptcha"
                @keydown.space.prevent="refreshCaptcha"
              >
                <canvas
                  ref="captchaCanvas"
                  width="120"
                  height="40"
                />
              </div>
            </div>
            <p class="captcha-hint">
              看不清？点击右侧验证码刷新
            </p>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="isLocked"
            style="width: 100%"
            @click="handleLogin"
          >
            {{ loginButtonText }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p
          v-if="isDev"
          class="dev-hint"
        >
          开发环境：admin / admin123
        </p>
        <div class="footer-info">
          <a 
            v-if="appStore.systemConfig.icp"
            :href="appStore.systemConfig.icpLink || 'https://beian.miit.gov.cn/'" 
            target="_blank"
            class="icp-link"
          >
            {{ appStore.systemConfig.icp }}
          </a>
          <span
            v-if="appStore.systemConfig.icp && appStore.systemConfig.copyright"
            class="divider"
          >|</span>
          <span class="copyright">{{ appStore.systemConfig.copyright }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { isValidUrl } from '@/utils/security'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const formRef = ref()
const loading = ref(false)
const captchaCanvas = ref()

// 是否开发环境
const isDev = import.meta.env.DEV

// 登录失败计数
const failedAttempts = ref(0)
const isLocked = ref(false)
const lockEndTime = ref(0)
const lockCountdown = ref(0)
let lockTimer = null

// 验证码相关
const showCaptcha = computed(() => true)
const captchaText = ref('')

// 安全提示
const securityAlert = reactive({
  show: false,
  type: 'warning',
  message: ''
})

const form = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度6-50位', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码为4位', trigger: 'blur' }
  ]
}

// 登录按钮文字
const loginButtonText = computed(() => {
  if (loading.value) return '登录中...'
  if (isLocked.value) return `请等待 ${lockCountdown.value} 秒`
  return '登 录'
})

// 检查是否已登录
onMounted(() => {
  userStore.init()
  if (userStore.isLoggedIn) {
    safeRedirect()
  }
  
  // 恢复锁定状态
  restoreLockState()

  // 首次进入登录页即生成验证码
  nextTick(() => refreshCaptcha())
  
  // 加载系统配置
  appStore.loadSiteConfig()
})

// 安全重定向（防止开放重定向漏洞）
const safeRedirect = () => {
  const redirect = route.query.redirect
  
  // 验证重定向URL安全性
  if (redirect) {
    // 只允许相对路径或同源URL
    if (redirect.startsWith('/') && !redirect.startsWith('//')) {
      router.push(redirect)
      return
    }
    // 检查是否同源
    if (isValidUrl(redirect)) {
      try {
        const url = new URL(redirect, window.location.origin)
        if (url.origin === window.location.origin) {
          router.push(redirect)
          return
        }
      } catch (e) {
        // URL解析失败，使用默认
      }
    }
  }
  
  // 默认跳转首页
  router.push('/')
}

// 恢复锁定状态
const restoreLockState = () => {
  const savedLockEnd = localStorage.getItem('loginLockEndTime')
  if (savedLockEnd) {
    const endTime = parseInt(savedLockEnd, 10)
    if (endTime > Date.now()) {
      lockEndTime.value = endTime
      isLocked.value = true
      startLockCountdown()
    } else {
      localStorage.removeItem('loginLockEndTime')
    }
  }
  
  const savedAttempts = sessionStorage.getItem('loginFailedAttempts')
  if (savedAttempts) {
    failedAttempts.value = parseInt(savedAttempts, 10)
    if (failedAttempts.value >= 3) {
      nextTick(() => refreshCaptcha())
    }
  }
}

// 开始锁定倒计时
const startLockCountdown = () => {
  const updateCountdown = () => {
    const remaining = Math.ceil((lockEndTime.value - Date.now()) / 1000)
    if (remaining > 0) {
      lockCountdown.value = remaining
      lockTimer = setTimeout(updateCountdown, 1000)
    } else {
      isLocked.value = false
      lockCountdown.value = 0
      localStorage.removeItem('loginLockEndTime')
    }
  }
  updateCountdown()
}

// 生成验证码
const refreshCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  captchaText.value = code
  form.captcha = ''
  
  nextTick(() => {
    drawCaptcha(code)
  })
}

// 绘制验证码
const drawCaptcha = (code) => {
  const canvas = captchaCanvas.value
  if (!canvas) return
  
  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height
  
  // 背景
  ctx.fillStyle = '#f0f0f0'
  ctx.fillRect(0, 0, width, height)
  
  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgb(${Math.random() * 150}, ${Math.random() * 150}, ${Math.random() * 150})`
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }
  
  // 干扰点
  for (let i = 0; i < 30; i++) {
    ctx.fillStyle = `rgb(${Math.random() * 255}, ${Math.random() * 255}, ${Math.random() * 255})`
    ctx.beginPath()
    ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI)
    ctx.fill()
  }
  
  // 文字
  ctx.font = 'bold 28px Arial'
  ctx.textBaseline = 'middle'
  for (let i = 0; i < code.length; i++) {
    ctx.fillStyle = `rgb(${Math.random() * 100}, ${Math.random() * 100}, ${Math.random() * 100})`
    const x = 15 + i * 26
    const y = height / 2 + (Math.random() - 0.5) * 10
    const rotation = (Math.random() - 0.5) * 0.4
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(rotation)
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }
}

// 输入变化时清除安全提示
const onInputChange = () => {
  if (securityAlert.show && securityAlert.type === 'error') {
    securityAlert.show = false
  }
}

const onCaptchaInput = (value) => {
  form.captcha = value.toUpperCase()
}

// 处理登录失败
const handleLoginFailed = (error) => {
  failedAttempts.value++
  sessionStorage.setItem('loginFailedAttempts', failedAttempts.value.toString())
  
  // 解析错误信息
  const errorMsg = error?.response?.data?.message || error?.message || '登录失败'
  
  // 检查是否账号被锁定
  if (errorMsg.includes('锁定') || error?.response?.data?.code === '1006') {
    securityAlert.show = true
    securityAlert.type = 'error'
    securityAlert.message = errorMsg
    
    // 前端也锁定，防止继续尝试
    const lockDuration = 30 * 60 * 1000 // 30分钟
    lockEndTime.value = Date.now() + lockDuration
    localStorage.setItem('loginLockEndTime', lockEndTime.value.toString())
    isLocked.value = true
    startLockCountdown()
    return
  }
  
  // 显示剩余尝试次数
  if (errorMsg.includes('还剩')) {
    securityAlert.show = true
    securityAlert.type = 'warning'
    securityAlert.message = errorMsg
  } else {
    securityAlert.show = true
    securityAlert.type = 'error'
    securityAlert.message = errorMsg
  }
  
  // 登录失败后刷新验证码
  nextTick(() => refreshCaptcha())
}

const handleLogin = async () => {
  if (isLocked.value) {
    ElMessage.warning('账号已被临时锁定，请稍后再试')
    return
  }
  
  try {
    await formRef.value.validate()
    
    // 验证码校验
    if (showCaptcha.value) {
      if (form.captcha.toUpperCase() !== captchaText.value) {
        ElMessage.error('验证码错误')
        refreshCaptcha()
        return
      }
    }
    
    loading.value = true
    securityAlert.show = false
    
    await userStore.login({
      username: form.username,
      password: form.password
    })
    
    // 登录成功，清除失败记录
    failedAttempts.value = 0
    sessionStorage.removeItem('loginFailedAttempts')
    
    ElMessage.success('登录成功')
    safeRedirect()
    
  } catch (e) {
    console.error('登录失败', e)
    handleLoginFailed(e)
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
  
  .login-logo {
    width: 64px;
    height: 64px;
    object-fit: contain;
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

.security-alert {
  margin-bottom: 20px;
  
  :deep(.el-alert__title) {
    font-size: 13px;
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
    
    &.is-disabled {
      background-color: #a0cfff;
      border-color: #a0cfff;
    }
  }
}

.captcha-group {
  width: 100%;
}

.captcha-row {
  display: flex;
  gap: 12px;
  
  .captcha-input {
    flex: 1;
  }
  
  .captcha-img {
    width: 120px;
    height: 40px;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
    border: 1px solid #dcdfe6;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
    
    &:hover,
    &:focus-visible {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
      outline: none;
    }
    
    canvas {
      display: block;
    }
  }
}

.captcha-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  
  .dev-hint {
    margin: 0 0 8px 0;
    font-size: 12px;
    color: #9ca3af;
  }
  
  .footer-info {
    font-size: 12px;
    color: #9ca3af;
    
    .icp-link {
      color: #9ca3af;
      text-decoration: none;
      
      &:hover {
        color: #409eff;
      }
    }
    
    .divider {
      margin: 0 8px;
      color: #d1d5db;
    }
    
    .copyright {
      color: #9ca3af;
    }
  }
}
</style>
