<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="24"><Files /></el-icon>
        <span>档案管理系统</span>
      </div>
      
      <el-menu
        :default-active="$route.path"
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
        <el-menu-item index="/locations">
          <el-icon><Location /></el-icon>
          <span>存放位置</span>
        </el-menu-item>
        <el-menu-item index="/borrows">
          <el-icon><Reading /></el-icon>
          <span>借阅管理</span>
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计分析</span>
        </el-menu-item>
        <el-sub-menu index="settings">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/sources">来源管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="user-info">
          <el-dropdown>
            <span class="el-dropdown-link">
              <el-avatar :size="32" icon="User" />
              <span style="margin-left: 8px">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
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
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    
    .el-icon {
      margin-right: 8px;
    }
  }
  
  .el-menu {
    border-right: none;
  }
}

.header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .breadcrumb {
    display: flex;
    align-items: center;
  }
  
  .user-info {
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
}
</style>
