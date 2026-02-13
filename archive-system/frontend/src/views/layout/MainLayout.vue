<template>
  <el-container class="main-layout">
    <!-- 全局加载遮罩 -->
    <div v-if="appStore.globalLoading" class="global-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>{{ appStore.loadingText }}</span>
    </div>
    
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon :size="24"><Files /></el-icon>
        <span v-show="!appStore.sidebarCollapsed">档案管理系统</span>
      </div>
      
      <el-menu
        :default-active="$route.path"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/archives">
          <el-icon><Document /></el-icon>
          <span>档案列表</span>
        </el-menu-item>
        <el-menu-item index="/receive">
          <el-icon><Upload /></el-icon>
          <span>档案接收</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>档案检索</span>
        </el-menu-item>
        <el-menu-item index="/borrows">
          <el-icon><Reading /></el-icon>
          <span>借阅管理</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>
        
        <el-sub-menu index="archive-settings">
          <template #title>
            <el-icon><FolderOpened /></el-icon>
            <span>档案设置</span>
          </template>
          <el-menu-item index="/categories">分类管理</el-menu-item>
          <el-menu-item index="/fonds">全宗管理</el-menu-item>
          <el-menu-item index="/locations">存放位置</el-menu-item>
          <el-menu-item index="/sources">来源管理</el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/users">用户管理</el-menu-item>
          <el-menu-item index="/system/roles">角色管理</el-menu-item>
          <el-menu-item index="/system/config">系统配置</el-menu-item>
          <el-menu-item index="/system/logs">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 上传进度提示 -->
          <el-badge v-if="appStore.uploadingCount > 0" :value="appStore.uploadingCount" class="upload-badge">
            <el-button :icon="Upload" circle size="small" @click="showUploadPanel = true" />
          </el-badge>
          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar :size="32" icon="User" />
              <span style="margin-left: 8px">{{ userStore.realName || userStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
    
    <!-- 上传进度面板 -->
    <el-drawer v-model="showUploadPanel" title="上传队列" direction="rtl" size="400px">
      <div v-if="appStore.uploadQueue.length === 0" class="empty-upload">
        <el-empty description="暂无上传任务" />
      </div>
      <div v-else class="upload-list">
        <div v-for="item in appStore.uploadQueue" :key="item.id" class="upload-item">
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
            >重试</el-button>
            <el-button 
              type="danger" 
              size="small" 
              text 
              @click="appStore.removeFromUploadQueue(item.id)"
            >移除</el-button>
          </div>
        </div>
        <el-button 
          v-if="appStore.uploadQueue.some(i => i.status === 'success' || i.status === 'error')" 
          type="primary" 
          text 
          @click="appStore.clearCompletedUploads"
        >清除已完成</el-button>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Upload,
  Search,
  Reading,
  DataAnalysis,
  FolderOpened,
  Setting,
  Files,
  Fold,
  Expand,
  Loading
} from '@element-plus/icons-vue'
import { useUserStore, useAppStore } from '@/stores'

const userStore = useUserStore()
const appStore = useAppStore()
const showUploadPanel = ref(false)

// 初始化
onMounted(() => {
  userStore.init()
  appStore.init()
})

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
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  
  .loading-icon {
    font-size: 48px;
    color: #409eff;
    animation: rotate 1s linear infinite;
  }
  
  span {
    margin-top: 16px;
    color: #606266;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    white-space: nowrap;
    
    .el-icon {
      margin-right: 8px;
      flex-shrink: 0;
    }
  }
  
  .el-menu {
    border-right: none;
  }
  
  .el-menu--collapse {
    width: 64px;
  }
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #606266;
      transition: color 0.3s;
      
      &:hover {
        color: #409eff;
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
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

.upload-list {
  .upload-item {
    padding: 12px;
    border-bottom: 1px solid #ebeef5;
    
    .upload-item-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      
      .file-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
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
