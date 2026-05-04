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
        v-if="!setupMode"
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

      <el-form
        v-else
        ref="setupFormRef"
        :model="setupForm"
        :rules="setupRules"
        class="login-form"
      >
        <el-form-item>
          <div class="setup-panel">
            <div class="setup-title">
              首次初始化
            </div>
            <p class="setup-desc">
              系统检测到当前还没有管理员账号。请先为 <code>admin</code> 设置初始密码，完成后再使用该账号登录。
            </p>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="setupForm.password"
            type="password"
            prefix-icon="Lock"
            placeholder="设置 admin 初始密码"
            size="large"
            show-password
            maxlength="50"
            @input="onSetupInputChange"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="setupForm.confirmPassword"
            type="password"
            prefix-icon="Lock"
            placeholder="再次输入密码"
            size="large"
            show-password
            maxlength="50"
            @keyup.enter="handleBootstrapInitialize"
            @input="onSetupInputChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="bootstrapLoading"
            style="width: 100%"
            @click="handleBootstrapInitialize"
          >
            设置 admin 初始密码
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p
          v-if="isDev && !setupMode"
          class="dev-hint"
        >
          开发环境：如未初始化，请先设置 admin 初始密码
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
import { getBootstrapStatus, initializeBootstrap } from '@/api/auth'
import { isValidUrl, secureStorage } from '@/utils/security'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()
const formRef = ref()
const setupFormRef = ref()
const loading = ref(false)
const bootstrapLoading = ref(false)
const captchaCanvas = ref()
const setupMode = ref(false)

// 是否开发环境
const isDev = import.meta.env.DEV

// 登录失败计数
const failedAttempts = ref(0)
const isLocked = ref(false)
const lockEndTime = ref(0)
const lockCountdown = ref(0)

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

const setupForm = reactive({
  password: '',
  confirmPassword: ''
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

const rules = {
  username: [
    requireTrimmedText('请输入用户名'),
    { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    requireNonBlankValue('请输入密码'),
    { min: 6, max: 50, message: '密码长度6-50位', trigger: 'blur' }
  ],
  captcha: [
    requireTrimmedText('请输入验证码'),
    { len: 4, message: '验证码为4位', trigger: 'blur' }
  ]
}

const setupRules = {
  password: [
    requireNonBlankValue('请输入初始密码'),
    { min: 8, max: 50, message: '密码长度8-50位', trigger: 'blur' }
  ],
  confirmPassword: [
    requireNonBlankValue('请再次输入密码'),
    {
      validator: (_rule, value, callback) => {
        if (value !== setupForm.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// 登录按钮文字
const loginButtonText = computed(() => {
  if (loading.value) return '登录中...'
  if (isLocked.value) return `请等待 ${lockCountdown.value} 秒`
  return '登 录'
})

// 检查是否已登录
onMounted(async () => {
  userStore.init()
  appStore.loadSiteConfig()

  const initialized = await loadBootstrapStatus()
  if (!initialized) {
    userStore.clearUser()
    return
  }

  if (secureStorage.getAccessToken()) {
    try {
      await userStore.fetchCurrentUser()
      safeRedirect()
      return
    } catch {
      // 会话无效时保留在登录页，继续初始化验证码与锁定状态
    }
  } else if (secureStorage.getRefreshToken()) {
    try {
      await userStore.refreshSession()
      safeRedirect()
      return
    } catch {
      // 刷新令牌失效时保留在登录页
    }
  }
  
  // 恢复锁定状态
  restoreLockState()

  // 首次进入登录页即生成验证码
  nextTick(() => refreshCaptcha())
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
      } catch {
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

const loadBootstrapStatus = async () => {
  try {
    const res = await getBootstrapStatus()
    setupMode.value = !res.data.initialized
    if (setupMode.value) {
      securityAlert.show = true
      securityAlert.type = 'info'
      securityAlert.message = '系统尚未初始化，请先设置 admin 初始密码'
    }
    return res.data.initialized
  } catch (e) {
    console.error('获取初始化状态失败', e)
    securityAlert.show = true
    securityAlert.type = 'error'
    securityAlert.message = '无法获取系统初始化状态，请稍后重试'
    return true
  }
}

// 开始锁定倒计时
const startLockCountdown = () => {
  const updateCountdown = () => {
    const remaining = Math.ceil((lockEndTime.value - Date.now()) / 1000)
    if (remaining > 0) {
      lockCountdown.value = remaining
      setTimeout(updateCountdown, 1000)
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
  form.captcha = value.trim().toUpperCase()
}

const onSetupInputChange = () => {
  if (securityAlert.show) {
    securityAlert.show = false
  }
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
      if (form.captcha.trim().toUpperCase() !== captchaText.value) {
        ElMessage.error('验证码错误')
        refreshCaptcha()
        return
      }
    }
    
    loading.value = true
    securityAlert.show = false
    
    await userStore.login({
      username: form.username.trim(),
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

const handleBootstrapInitialize = async () => {
  try {
    await setupFormRef.value.validate()
    bootstrapLoading.value = true
    securityAlert.show = false

    await initializeBootstrap(setupForm.password)

    setupMode.value = false
    form.username = 'admin'
    form.password = ''
    form.captcha = ''
    setupForm.password = ''
    setupForm.confirmPassword = ''
    failedAttempts.value = 0
    sessionStorage.removeItem('loginFailedAttempts')
    localStorage.removeItem('loginLockEndTime')
    isLocked.value = false

    ElMessage.success('初始化成功，请使用 admin 登录')
    nextTick(() => refreshCaptcha())
  } catch (e) {
    console.error('初始化失败', e)
    securityAlert.show = true
    securityAlert.type = 'error'
    securityAlert.message = e?.message || '初始化失败'
  } finally {
    bootstrapLoading.value = false
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

.setup-panel {
  width: 100%;
  padding: 14px 16px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;

  .setup-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }

  .setup-desc {
    margin: 8px 0 0;
    font-size: 13px;
    line-height: 1.6;
    color: #6b7280;
  }

  code {
    padding: 1px 6px;
    border-radius: 4px;
    background: #e5eef9;
    color: #1d4ed8;
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
