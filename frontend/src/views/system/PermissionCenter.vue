<template>
  <div class="center-page">
    <div class="page-header">
      <h1>权限管理</h1>
      <p>将用户和角色集中到同一入口，由具备系统管理权限的账号统一维护权限边界。</p>
    </div>

    <el-alert
      title="权限敏感区"
      type="warning"
      :closable="false"
      description="当前版本仅具备系统管理权限的账号可进入权限管理并维护用户、角色和授权。"
    />

    <el-card
      shadow="hover"
      class="tabs-card"
    >
      <el-tabs
        v-model="activeTab"
        class="center-tabs"
      >
        <el-tab-pane
          v-for="tab in visibleTabs"
          :key="tab.name"
          :name="tab.name"
          :label="tab.label"
          lazy
        >
          <component :is="tab.component" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tabs = [
  {
    name: 'users',
    label: '用户管理',
    allowed: () => userStore.isAdmin,
    component: defineAsyncComponent(() => import('@/views/system/UserManage.vue'))
  },
  {
    name: 'roles',
    label: '角色管理',
    allowed: () => userStore.isAdmin,
    component: defineAsyncComponent(() => import('@/views/system/RoleManage.vue'))
  }
]

const visibleTabs = computed(() => tabs.filter((tab) => tab.allowed()))
const fallbackTab = computed(() => visibleTabs.value[0]?.name || 'users')
const resolveRequestedTab = () => {
  const tab = Array.isArray(route.query.tab) ? route.query.tab[0] : route.query.tab
  return typeof tab === 'string' ? tab : ''
}

const replaceTabQuery = (tab) => {
  const nextTab = visibleTabs.value.some((item) => item.name === tab) ? tab : fallbackTab.value
  if (resolveRequestedTab() === nextTab) {
    return
  }
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: nextTab
    },
    hash: route.hash
  })
}

const activeTab = computed({
  get: () => {
    const requested = resolveRequestedTab()
    return visibleTabs.value.some((tab) => tab.name === requested) ? requested : fallbackTab.value
  },
  set: (value) => {
    replaceTabQuery(value)
  }
})

watch(
  () => route.query.tab,
  () => {
    const requested = resolveRequestedTab()
    if (!visibleTabs.value.some((tab) => tab.name === requested)) {
      replaceTabQuery(fallbackTab.value)
    }
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.center-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  h1 {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  p {
    margin: 0;
    color: #606266;
    line-height: 1.6;
  }
}

.tabs-card {
  :deep(.el-card__body) {
    padding-top: 16px;
  }
}
</style>
