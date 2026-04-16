<template>
  <el-container class="main-layout">
    <!-- 全局加载遮罩 -->
    <div
      v-if="appStore.globalLoading"
      class="global-loading"
    >
      <el-icon class="loading-icon">
        <Loading />
      </el-icon>
      <span>{{ appStore.loadingText }}</span>
    </div>
    
    <!-- 侧边栏 -->
    <el-aside
      :width="appStore.sidebarCollapsed ? '64px' : '220px'"
      class="sidebar"
    >
      <div class="logo">
        <img
          :src="appStore.systemConfig.logoUrl || '/logo.png'"
          class="logo-img"
        >
        <span v-show="!appStore.sidebarCollapsed">{{ appStore.systemConfig.systemName }}</span>
      </div>
      
      <el-menu
        :default-active="$route.path"
        :collapse="appStore.sidebarCollapsed"
        router
        class="nav-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item
          v-if="canAccess(REPORT_ROLES)"
          index="/statistics"
        >
          <el-icon><DataAnalysis /></el-icon>
          <span>统计概览</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(REPORT_ROLES)"
          index="/reports"
        >
          <el-icon><Download /></el-icon>
          <span>报表导出</span>
        </el-menu-item>
        <el-menu-item index="/archives">
          <el-icon><Document /></el-icon>
          <span>档案列表</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>档案检索</span>
        </el-menu-item>
        <el-menu-item index="/receive">
          <el-icon><Upload /></el-icon>
          <span>档案接收</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(MANAGER_ROLES)"
          index="/push-records"
        >
          <el-icon><Connection /></el-icon>
          <span>推送记录</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(BORROW_ROLES)"
          index="/borrows"
        >
          <el-icon><Reading /></el-icon>
          <span>借阅管理</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(MANAGER_ROLES)"
          index="/borrow-links"
        >
          <el-icon><Link /></el-icon>
          <span>借阅链接</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(MANAGER_ROLES)"
          index="/appraisals"
        >
          <el-icon><Stamp /></el-icon>
          <span>鉴定管理</span>
        </el-menu-item>
        <el-menu-item
          v-if="canAccess(MANAGER_ROLES)"
          index="/destructions"
        >
          <el-icon><Delete /></el-icon>
          <span>销毁管理</span>
        </el-menu-item>
        
        <el-menu-item
          v-if="canAccess(MANAGER_ROLES)"
          index="/archive-settings"
        >
          <el-icon><FolderOpened /></el-icon>
          <span>档案设置</span>
        </el-menu-item>

        <el-menu-item index="/help">
          <el-icon><QuestionFilled /></el-icon>
          <span>帮助中心</span>
        </el-menu-item>
        
        <el-sub-menu
          v-if="showSystemMenu"
          index="system"
        >
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/permissions"
          >
            权限管理
          </el-menu-item>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/setup"
          >
            站点与展示
          </el-menu-item>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/config"
          >
            系统配置
          </el-menu-item>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/info"
          >
            系统信息
          </el-menu-item>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/recovery"
          >
            备份恢复
          </el-menu-item>
          <el-menu-item
            v-if="canAccess([ROLES.SYSTEM_ADMIN])"
            index="/system/logs"
          >
            操作日志
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-button
            class="header-collapse-btn"
            text
            circle
            size="large"
            :aria-label="appStore.sidebarCollapsed ? '展开导航菜单' : '折叠导航菜单'"
            @click="appStore.toggleSidebar"
          >
            <el-icon :size="22">
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 上传进度提示 -->
          <el-badge
            v-if="appStore.uploadingCount > 0"
            :value="appStore.uploadingCount"
            class="upload-badge"
          >
            <el-button
              :icon="Upload"
              circle
              size="small"
              @click="showUploadPanel = true"
            />
          </el-badge>
          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar
                :size="32"
                icon="User"
              />
              <span style="margin-left: 8px">{{ userStore.realName || userStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item
                  divided
                  @click="handleLogout"
                >
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <el-alert
          v-if="showSetupReminder"
          type="warning"
          :closable="true"
          class="setup-reminder"
          @close="dismissSetupReminder"
        >
          <template #title>
            当前系统仍存在默认站点信息，建议尽快前往“系统管理 / 站点与展示”完善系统名称、Logo、备案号和版权信息。
          </template>
        </el-alert>
        <el-alert
          v-if="showRegistryUpdateReminder"
          type="warning"
          :closable="true"
          class="setup-reminder"
          @close="dismissRegistryUpdateReminder"
        >
          <template #title>
            镜像仓库检查显示存在可用更新，请到「系统管理 — 系统信息」查看说明并安排升级。
          </template>
        </el-alert>
        <router-view />
      </el-main>
      <div class="layout-footer">
        <span>{{ appStore.systemConfig.systemName || '档案管理系统' }}</span>
        <span v-if="canViewRuntimeInfo && layoutDisplayVersion">版本 {{ layoutDisplayVersion }}</span>
        <span v-if="canViewRuntimeInfo && runtimeInfo.commitSha && runtimeInfo.commitSha !== 'unknown'">提交 {{ runtimeInfo.commitSha }}</span>
      </div>
    </el-container>
    
    <!-- 上传进度面板 -->
    <el-drawer
      v-model="showUploadPanel"
      title="上传队列"
      direction="rtl"
      size="400px"
    >
      <div
        v-if="appStore.uploadQueue.length === 0"
        class="empty-upload"
      >
        <el-empty description="暂无上传任务" />
      </div>
      <div
        v-else
        class="upload-list"
      >
        <div
          v-for="item in appStore.uploadQueue"
          :key="item.id"
          class="upload-item"
        >
          <div class="upload-item-info">
            <el-icon><Document /></el-icon>
            <span class="file-name">{{ item.name }}</span>
          </div>
          <el-progress 
            :percentage="item.progress" 
            :status="item.status === 'error' ? 'exception' : item.status === 'success' ? 'success' : ''"
          />
          <div class="upload-item-actions">
            <el-button 
              v-if="item.status === 'error'" 
              type="primary" 
              size="small" 
              text
            >
              重试
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              text 
              @click="appStore.removeFromUploadQueue(item.id)"
            >
              移除
            </el-button>
          </div>
        </div>
        <el-button 
          v-if="appStore.uploadQueue.some(i => i.status === 'success' || i.status === 'error')" 
          type="primary" 
          text 
          @click="appStore.clearCompletedUploads"
        >
          清除已完成
        </el-button>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Upload,
  Search,
  Reading,
  DataAnalysis,
  FolderOpened,
  Setting,
  QuestionFilled,
  Fold,
  Expand,
  Loading,
  Stamp,
  Delete,
  Connection,
  Link,
  Download
} from '@element-plus/icons-vue'
import { useUserStore, useAppStore } from '@/stores'
import { checkRegistryUpdate, getRuntimeInfo } from '@/api/config'
import { APP_PRODUCT_VERSION } from '@/config/appProductVersion'
import { BORROW_ROLES, MANAGER_ROLES, REPORT_ROLES, ROLES, hasPermission } from '@/utils/permission'

const userStore = useUserStore()
const appStore = useAppStore()
const showUploadPanel = ref(false)
const setupReminderDismissed = ref(sessionStorage.getItem('setupReminderDismissed') === '1')
const registryUpdateReminderDismissed = ref(sessionStorage.getItem('registryUpdateReminderDismissed') === '1')
const registryHasUpdate = ref(false)
const runtimeInfo = reactive({
  productVersion: '',
  commitSha: ''
})
const showSetupReminder = computed(() =>
  userStore.isAdmin && appStore.needsInitialSetup && !setupReminderDismissed.value
)
const showRegistryUpdateReminder = computed(() =>
  canViewRuntimeInfo.value && registryHasUpdate.value && !registryUpdateReminderDismissed.value
)
const canViewRuntimeInfo = computed(() => canAccess([ROLES.SYSTEM_ADMIN]))
const showSystemMenu = computed(() => canAccess([ROLES.SYSTEM_ADMIN]))
const layoutDisplayVersion = computed(
  () => (runtimeInfo.productVersion || APP_PRODUCT_VERSION || '').trim()
)

// 初始化
onMounted(() => {
  userStore.init()
  appStore.init()
  appStore.loadSiteConfig()
  if (canViewRuntimeInfo.value) {
    loadRuntimeInfo()
    loadRegistryUpdateHint()
  }
})

const loadRuntimeInfo = async () => {
  try {
    const res = await getRuntimeInfo()
    const data = res?.data || {}
    runtimeInfo.productVersion = data.productVersion || APP_PRODUCT_VERSION || ''
    runtimeInfo.commitSha = data.commitSha || ''
  } catch (error) {
    runtimeInfo.productVersion = ''
    runtimeInfo.commitSha = ''
  }
}

const loadRegistryUpdateHint = async () => {
  try {
    const res = await checkRegistryUpdate()
    registryHasUpdate.value = res?.data?.updateAvailable === true
  } catch (error) {
    registryHasUpdate.value = false
  }
}

const dismissSetupReminder = () => {
  setupReminderDismissed.value = true
  sessionStorage.setItem('setupReminderDismissed', '1')
}

const dismissRegistryUpdateReminder = () => {
  registryUpdateReminderDismissed.value = true
  sessionStorage.setItem('registryUpdateReminderDismissed', '1')
}

const canAccess = (roles) => hasPermission(roles, userStore.userType)

// 退出登录
const handleLogout = async () => {
  await userStore.logout()
  ElMessage.success('已退出登录')
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  position: relative;
}

.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
  
  .loading-icon {
    font-size: 48px;
    color: #409eff;
    animation: rotate 1s linear infinite;
  }
  
  span {
    margin-top: 16px;
    color: #606266;
    font-size: 14px;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.sidebar {
  background: linear-gradient(180deg, #243447 0%, #304156 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-x: hidden;
  overflow-y: auto;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  
  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  .logo {
    height: 60px;
    min-height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;
    background: rgba(0, 0, 0, 0.15);
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    letter-spacing: 1px;
    
    .el-icon {
      margin-right: 10px;
      flex-shrink: 0;
      color: #409eff;
    }
    
    .logo-img {
      height: 32px;
      margin-right: 10px;
      flex-shrink: 0;
    }
  }
  
  .el-menu {
    border-right: none;
    flex: 1;
    
    // 菜单项 hover 效果
    :deep(.el-menu-item) {
      margin: 4px 8px;
      border-radius: 6px;
      transition: all 0.2s ease;
      
      &:hover {
        background-color: rgba(64, 158, 255, 0.15) !important;
      }
      
      &.is-active {
        background-color: rgba(64, 158, 255, 0.2) !important;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 3px;
          height: 60%;
          background: #409eff;
          border-radius: 0 2px 2px 0;
        }
      }
    }
    
    :deep(.el-sub-menu__title) {
      margin: 4px 8px;
      border-radius: 6px;
      transition: all 0.2s ease;
      font-weight: 600;
      
      &:hover {
        background-color: rgba(64, 158, 255, 0.15) !important;
      }
    }

    :deep(.el-sub-menu .el-menu) {
      margin: 2px 12px 10px 18px;
      padding: 6px 0;
      background: rgba(14, 24, 36, 0.24) !important;
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.06);
    }

    :deep(.el-sub-menu .el-menu-item) {
      min-width: auto;
      height: 38px;
      line-height: 38px;
      margin: 2px 8px;
      padding-left: 18px !important;
      border-radius: 8px;
      font-size: 13px;
      color: #d7e1eb !important;
      background: transparent !important;

      &:hover {
        background: rgba(97, 174, 255, 0.14) !important;
      }

      &.is-active {
        color: #ecf5ff !important;
        background: linear-gradient(90deg, rgba(64, 158, 255, 0.24), rgba(64, 158, 255, 0.08)) !important;
      }
    }
  }
  
  .el-menu--collapse {
    width: 64px;
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      margin: 4px;
    }
  }
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid #f0f0f0;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .header-collapse-btn {
      flex-shrink: 0;
      color: #606266;

      &:hover {
        color: #409eff;
      }

      :deep(.el-icon) {
        margin: 0;
      }
    }
    
    :deep(.el-breadcrumb) {
      font-size: 14px;
      
      .el-breadcrumb__inner {
        color: #909399;
        
        &.is-link:hover {
          color: #409eff;
        }
      }
      
      .el-breadcrumb__item:last-child .el-breadcrumb__inner {
        color: #303133;
        font-weight: 500;
      }
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .upload-badge {
      margin-right: 8px;
    }
    
    .el-dropdown-link {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 6px;
      transition: background 0.2s ease;
      
      &:hover {
        background: #f5f7fa;
      }
      
      span {
        margin-left: 8px;
        color: #606266;
        font-size: 14px;
      }
    }
  }
}

.layout-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding: 0 24px 14px;
  color: #909399;
  font-size: 12px;
  border-top: 1px solid #f0f2f5;
  background: #fff;
}

.setup-reminder {
  margin-bottom: 16px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  padding-bottom: 8px;
  overflow-y: auto;
}

.upload-list {
  .upload-item {
    padding: 12px;
    border-bottom: 1px solid #ebeef5;
    transition: background 0.2s ease;
    
    &:hover {
      background: #fafafa;
    }
    
    .upload-item-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      
      .el-icon {
        color: #909399;
      }
      
      .file-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 14px;
      }
    }
    
    .upload-item-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 8px;
    }
  }
}

.empty-upload {
  padding: 40px 0;
}
</style>
