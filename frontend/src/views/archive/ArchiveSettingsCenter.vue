<template>
  <div class="settings-center">
    <div class="page-header">
      <h1>档案设置</h1>
      <p>将分类、全宗、位置与来源集中管理，减少菜单长度，同时保留权限边界。</p>
    </div>

    <el-alert
      title="基础数据工作台"
      type="info"
      :closable="false"
      description="建议先完成全宗和分类，再维护存放位置与来源映射。"
    />

    <el-card
      shadow="hover"
      class="tabs-card"
    >
      <el-tabs
        v-model="activeTab"
        class="settings-tabs"
        @tab-change="handleTabChange"
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
import { useUserStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tabs = [
  {
    name: 'categories',
    label: '分类管理',
    allowed: () => userStore.isArchivist,
    component: defineAsyncComponent(() => import('@/views/category/CategoryManage.vue'))
  },
  {
    name: 'fonds',
    label: '全宗管理',
    allowed: () => userStore.isArchivist,
    component: defineAsyncComponent(() => import('@/views/fonds/FondsManage.vue'))
  },
  {
    name: 'locations',
    label: '存放位置',
    allowed: () => userStore.isArchivist,
    component: defineAsyncComponent(() => import('@/views/location/LocationList.vue'))
  },
  {
    name: 'sources',
    label: '来源管理',
    allowed: () => userStore.isArchivist,
    component: defineAsyncComponent(() => import('@/views/source/SourceList.vue'))
  }
]

const visibleTabs = computed(() => tabs.filter((tab) => tab.allowed()))
const fallbackTab = computed(() => visibleTabs.value[0]?.name || 'categories')
const activeTab = computed({
  get: () => {
    const requested = route.query.tab
    return visibleTabs.value.some((tab) => tab.name === requested) ? requested : fallbackTab.value
  },
  set: (value) => {
    router.replace({ path: '/archive-settings', query: { tab: value } })
  }
})

watch(
  () => route.query.tab,
  () => {
    if (!visibleTabs.value.some((tab) => tab.name === route.query.tab)) {
      router.replace({ path: '/archive-settings', query: { tab: fallbackTab.value } })
    }
  },
  { immediate: true }
)

const handleTabChange = (name) => {
  activeTab.value = name
}
</script>

<style lang="scss" scoped>
.settings-center {
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

.settings-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}
</style>
