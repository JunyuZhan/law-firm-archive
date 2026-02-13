import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // 状态
  const userId = ref(null)
  const username = ref('')
  const realName = ref('')
  const userType = ref('')
  const isLoggedIn = ref(false)

  // 计算属性
  const isAdmin = computed(() => userType.value === 'SYSTEM_ADMIN')
  const isArchivist = computed(() => ['SYSTEM_ADMIN', 'ARCHIVIST'].includes(userType.value))

  // 初始化 - 从localStorage恢复状态
  function init() {
    const token = localStorage.getItem('accessToken')
    if (token) {
      const savedUser = localStorage.getItem('userInfo')
      if (savedUser) {
        try {
          const user = JSON.parse(savedUser)
          userId.value = user.userId
          username.value = user.username
          realName.value = user.realName
          userType.value = user.userType
          isLoggedIn.value = true
        } catch (e) {
          console.error('解析用户信息失败', e)
          clearUser()
        }
      }
    }
  }

  // 登录
  async function login(credentials) {
    const res = await loginApi(credentials)
    const data = res.data
    
    // 保存Token
    localStorage.setItem('accessToken', data.accessToken)
    sessionStorage.setItem('refreshToken', data.refreshToken)
    
    // 保存用户信息
    const userInfo = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      userType: data.userType
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    
    // 更新状态
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    userType.value = data.userType
    isLoggedIn.value = true
    
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
    localStorage.removeItem('accessToken')
    localStorage.removeItem('userInfo')
    sessionStorage.removeItem('refreshToken')
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
      userId.value = data.userId
      username.value = data.username
      realName.value = data.realName
      userType.value = data.userType
      isLoggedIn.value = true
      
      // 更新localStorage
      const userInfo = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        userType: data.userType
      }
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      
      return data
    } catch (e) {
      console.error('获取用户信息失败', e)
      clearUser()
      throw e
    }
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
    fetchCurrentUser
  }
})
