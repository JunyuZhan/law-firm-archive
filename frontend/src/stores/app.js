import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getPublicSiteConfig } from '@/api/config'

export const useAppStore = defineStore('app', () => {
  const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

  // 侧边栏状态
  const sidebarCollapsed = ref(false)
  
  // 全局加载状态
  const globalLoading = ref(false)
  const loadingText = ref('')
  
  // 主题设置
  const theme = ref(localStorage.getItem('theme') || 'light')
  
  // 系统配置（从后端获取）
  const systemConfig = ref({
    systemName: '档案管理系统',
    systemNameEn: 'Archive Management System',
    logoUrl: '',
    icp: '',
    copyright: '© 2024 档案管理系统',
    retentionPeriods: [],
    archiveTypes: [],
    maxUploadSize: 100 * 1024 * 1024, // 100MB
    allowedFileTypes: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'jpg', 'jpeg', 'png', 'gif']
  })
  
  // 面包屑
  const breadcrumbs = ref([])
  
  // 上传队列
  const uploadQueue = ref([])
  const uploadingCount = computed(() => 
    uploadQueue.value.filter(item => item.status === 'uploading').length
  )

  const setupDefaults = {
    systemName: '档案管理系统',
    systemNameEn: 'Archive Management System',
    copyright: '© 2024 档案管理系统'
  }
  
  // 计算属性
  const isDarkMode = computed(() => theme.value === 'dark')
  const needsInitialSetup = computed(() => {
    const config = systemConfig.value
    return !config.systemName ||
      config.systemName === setupDefaults.systemName ||
      !config.logoUrl ||
      !config.copyright ||
      config.copyright === setupDefaults.copyright
  })
  
  // 切换侧边栏
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('sidebarCollapsed', sidebarCollapsed.value)
  }
  
  // 设置侧边栏状态
  function setSidebarCollapsed(collapsed) {
    sidebarCollapsed.value = collapsed
    localStorage.setItem('sidebarCollapsed', collapsed)
  }
  
  // 显示全局加载
  function showLoading(text = '加载中...') {
    globalLoading.value = true
    loadingText.value = text
  }
  
  // 隐藏全局加载
  function hideLoading() {
    globalLoading.value = false
    loadingText.value = ''
  }
  
  // 设置主题
  function setTheme(newTheme) {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
    document.documentElement.setAttribute('data-theme', newTheme)
  }
  
  // 切换主题
  function toggleTheme() {
    setTheme(theme.value === 'light' ? 'dark' : 'light')
  }
  
  // 设置面包屑
  function setBreadcrumbs(items) {
    breadcrumbs.value = items
  }

  function normalizePublicAssetUrl(url) {
    if (!url || typeof url !== 'string') return ''
    if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
      return url
    }
    if (apiBaseURL !== '/api' && url.startsWith('/api/')) {
      return `${apiBaseURL}${url.slice(4)}`
    }
    return url
  }

  function denormalizePublicAssetUrl(url) {
    if (!url || typeof url !== 'string') return ''
    if (/^https?:\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
      return url
    }
    if (apiBaseURL !== '/api' && url.startsWith(`${apiBaseURL}/`)) {
      return `/api${url.slice(apiBaseURL.length)}`
    }
    return url
  }
  
  // 设置系统配置
  function setSystemConfig(config) {
    const nextConfig = { ...config }
    if ('logoUrl' in nextConfig) {
      nextConfig.logoUrl = normalizePublicAssetUrl(nextConfig.logoUrl)
    }
    systemConfig.value = { ...systemConfig.value, ...nextConfig }
  }
  
  // 加载 SITE 分组配置（使用公开接口，无需认证）
  async function loadSiteConfig() {
    try {
      const res = await getPublicSiteConfig()
      const configs = res?.data || res || []
      if (configs && configs.length > 0) {
        const configMap = {}
        configs.forEach(item => {
          configMap[item.configKey] = item.configValue
        })
        
        setSystemConfig({
          systemName: configMap['system.site.name'] || '档案管理系统',
          systemNameEn: configMap['system.site.name.en'] || 'Archive Management System',
          logoUrl: configMap['system.site.logo'] || '',
          icp: configMap['system.site.icp'] || '',
          copyright: configMap['system.site.copyright'] || '© 2024 档案管理系统'
        })
      }
    } catch (error) {
      console.error('加载站点配置失败:', error)
    }
  }
  
  // 上传队列操作
  function addToUploadQueue(file) {
    const item = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      file,
      name: file.name,
      size: file.size,
      type: file.type,
      progress: 0,
      status: 'pending', // pending, uploading, success, error, cancelled
      error: null,
      archiveId: null,
      startTime: null,
      endTime: null
    }
    uploadQueue.value.push(item)
    return item.id
  }
  
  function updateUploadProgress(id, progress) {
    const item = uploadQueue.value.find(i => i.id === id)
    if (item) {
      item.progress = progress
      if (progress > 0 && item.status === 'pending') {
        item.status = 'uploading'
        item.startTime = Date.now()
      }
    }
  }
  
  function setUploadStatus(id, status, error = null) {
    const item = uploadQueue.value.find(i => i.id === id)
    if (item) {
      item.status = status
      item.error = error
      if (status === 'success' || status === 'error') {
        item.endTime = Date.now()
      }
    }
  }
  
  function removeFromUploadQueue(id) {
    const index = uploadQueue.value.findIndex(i => i.id === id)
    if (index > -1) {
      uploadQueue.value.splice(index, 1)
    }
  }
  
  function clearCompletedUploads() {
    uploadQueue.value = uploadQueue.value.filter(
      item => item.status !== 'success' && item.status !== 'error'
    )
  }
  
  // 初始化
  function init() {
    // 恢复侧边栏状态
    const savedCollapsed = localStorage.getItem('sidebarCollapsed')
    if (savedCollapsed !== null) {
      sidebarCollapsed.value = savedCollapsed === 'true'
    }
    
    // 恢复主题
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme) {
      theme.value = savedTheme
      document.documentElement.setAttribute('data-theme', savedTheme)
    }
  }
  
  return {
    // 状态
    sidebarCollapsed,
    globalLoading,
    loadingText,
    theme,
    systemConfig,
    breadcrumbs,
    uploadQueue,
    uploadingCount,
    isDarkMode,
    needsInitialSetup,
    
    // 方法
    toggleSidebar,
    setSidebarCollapsed,
    showLoading,
    hideLoading,
    setTheme,
    toggleTheme,
    setBreadcrumbs,
    setSystemConfig,
    normalizePublicAssetUrl,
    denormalizePublicAssetUrl,
    loadSiteConfig,
    addToUploadQueue,
    updateUploadProgress,
    setUploadStatus,
    removeFromUploadQueue,
    clearCompletedUploads,
    init
  }
})
