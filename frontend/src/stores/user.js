import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser, refreshToken as refreshTokenApi } from '@/api/auth'
import router from '@/router'
import { secureStorage, setupIdleTimeout, escapeHtml } from '@/utils/security'
import { ROLES, hasPermission, normalizeUserType } from '@/utils/permission'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const userType = ref('')
  const isLoggedIn = ref(false)
  
  // 空闲超时清理函数
  let cleanupIdleTimeout = null

  // 计算属性
  const isAdmin = computed(() => hasPermission([ROLES.SYSTEM_ADMIN], userType.value))
  const isArchivist = computed(() => hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER], userType.value))

  function persistUserInfo(data) {
    const userInfo = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      userType: data.userType
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
  }

  // 初始化 - 从localStorage恢复状态
  function init() {
    const token = secureStorage.getAccessToken()
    const refreshToken = secureStorage.getRefreshToken()
    if (!token) {
      if (!refreshToken) {
        clearUser()
      } else {
        userId.value = null
        username.value = ''
        realName.value = ''
        userType.value = ''
        isLoggedIn.value = false
      }
      return
    }

    const savedUser = localStorage.getItem('userInfo')
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser)
        // 对用户信息进行安全处理
        userId.value = user.userId
        username.value = escapeHtml(user.username)
        realName.value = escapeHtml(user.realName)
        userType.value = normalizeUserType(user.userType)
        isLoggedIn.value = true
        
        // 设置空闲超时
        setupIdleLogout()
        return
      } catch (e) {
        console.error('解析用户信息失败', e)
        localStorage.removeItem('userInfo')
      }
    }

    userId.value = null
    username.value = ''
    realName.value = ''
    userType.value = ''
    isLoggedIn.value = false
  }

  // 设置空闲超时自动登出
  function setupIdleLogout() {
    if (cleanupIdleTimeout) {
      cleanupIdleTimeout()
    }
    // 30分钟无操作自动登出
    cleanupIdleTimeout = setupIdleTimeout(30, () => {
      logout()
    })
  }

  // 登录
  async function login(credentials) {
    const res = await loginApi(credentials)
    const data = res.data
    
    // 使用安全存储保存Token
    secureStorage.setAccessToken(data.accessToken)
    secureStorage.setRefreshToken(data.refreshToken)
    
    // 保存用户信息（非敏感，可存localStorage）
    const effectiveType = normalizeUserType(data.userType)
    persistUserInfo({
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      userType: effectiveType
    })
    
    // 更新状态
    userId.value = data.userId
    username.value = escapeHtml(data.username)
    realName.value = escapeHtml(data.realName)
    userType.value = effectiveType
    isLoggedIn.value = true
    
    // 设置空闲超时
    setupIdleLogout()
    
    return data
  }

  // 登出
  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      console.error('登出请求失败', e)
    } finally {
      clearUser()
      router.push('/login')
    }
  }

  // 清除用户状态
  function clearUser() {
    // 使用安全存储清除Token
    secureStorage.clearTokens()
    localStorage.removeItem('userInfo')
    
    // 清理空闲超时
    if (cleanupIdleTimeout) {
      cleanupIdleTimeout()
      cleanupIdleTimeout = null
    }
    
    userId.value = null
    username.value = ''
    realName.value = ''
    userType.value = ''
    isLoggedIn.value = false
  }

  // 获取当前用户信息
  async function fetchCurrentUser() {
    try {
      const res = await getCurrentUser()
      const data = res.data
      const effectiveType = normalizeUserType(data.userType)
      userId.value = data.userId
      // 安全处理：与 login 保持一致，防止 XSS
      username.value = escapeHtml(data.username)
      realName.value = escapeHtml(data.realName)
      userType.value = effectiveType
      isLoggedIn.value = true
      
      // 更新localStorage（与后端一致的产品角色，展示时会再次转义）
      persistUserInfo({
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        userType: effectiveType
      })

      setupIdleLogout()
      
      return data
    } catch (e) {
      console.error('获取用户信息失败', e)
      clearUser()
      throw e
    }
  }

  async function refreshSession() {
    const currentRefreshToken = secureStorage.getRefreshToken()
    if (!currentRefreshToken) {
      clearUser()
      throw new Error('缺少刷新令牌')
    }

    try {
      const res = await refreshTokenApi(currentRefreshToken)
      const data = res.data
      const effectiveType = normalizeUserType(data.userType)

      secureStorage.setAccessToken(data.accessToken)
      if (data.refreshToken) {
        secureStorage.setRefreshToken(data.refreshToken)
      }

      userId.value = data.userId
      username.value = escapeHtml(data.username)
      realName.value = escapeHtml(data.realName)
      userType.value = effectiveType
      isLoggedIn.value = true

      persistUserInfo({
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        userType: effectiveType
      })

      setupIdleLogout()
      return data
    } catch (e) {
      clearUser()
      throw e
    }
  }

  function updateProfileSnapshot(profile) {
    if (!isLoggedIn.value) return

    const nextRealName = profile?.realName ?? realName.value
    realName.value = escapeHtml(nextRealName || '')

    persistUserInfo({
      userId: userId.value,
      username: username.value,
      realName: nextRealName || '',
      userType: userType.value
    })
  }

  return {
    userId,
    username,
    realName,
    userType,
    isLoggedIn,
    isAdmin,
    isArchivist,
    init,
    login,
    logout,
    clearUser,
    fetchCurrentUser,
    refreshSession,
    updateProfileSnapshot
  }
})
