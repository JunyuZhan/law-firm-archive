<template>
  <div class="center-page">
    <div class="page-header">
      <h1>备份恢复</h1>
      <p>将备份与恢复放在同一入口，方便具备系统管理权限的账号在一个上下文里完成备份校验与恢复接管。</p>
    </div>

    <el-alert
      :title="activeTabAlert.title"
      :type="activeTabAlert.type"
      :closable="false"
      :description="activeTabAlert.description"
    />

    <el-card
      shadow="never"
      class="tabs-card"
    >
      <el-tabs
        v-model="activeTab"
        class="center-tabs"
      >
        <el-tab-pane
          v-for="tab in tabs"
          :key="tab.name"
          :name="tab.name"
          :label="tab.label"
          lazy
        >
          <component
            :is="tab.component"
            v-bind="tab.props || {}"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tabs = [
  {
    name: 'backup',
    label: '备份中心',
    component: defineAsyncComponent(() => import('@/views/system/BackupCenter.vue')),
    props: {
      embedded: true
    }
  },
  {
    name: 'restore',
    label: '恢复中心',
    component: defineAsyncComponent(() => import('@/views/system/RestoreCenter.vue')),
    props: {
      embedded: true
    }
  }
]

const fallbackTab = 'backup'
const resolveRequestedTab = () => {
  const tab = Array.isArray(route.query.tab) ? route.query.tab[0] : route.query.tab
  return typeof tab === 'string' ? tab : ''
}

const replaceTabQuery = (tab) => {
  const nextTab = tabs.some((item) => item.name === tab) ? tab : fallbackTab
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
    const requestedTab = resolveRequestedTab()
    return tabs.some((tab) => tab.name === requestedTab) ? requestedTab : fallbackTab
  },
  set: (value) => {
    replaceTabQuery(value)
  }
})

const activeTabAlert = computed(() => {
  if (activeTab.value === 'restore') {
    return {
      title: '高风险操作',
      type: 'warning',
      description: '恢复会影响全系统，请先确认维护模式、备份集完整性和恢复目标环境。'
    }
  }
  return {
    title: '操作建议',
    type: 'info',
    description: '先验证备份目标可写性，再执行手动备份；可恢复的备份集会自动出现在“恢复中心”。'
  }
})

watch(
  () => route.query.tab,
  () => {
    const requestedTab = resolveRequestedTab()
    if (!tabs.some((tab) => tab.name === requestedTab)) {
      replaceTabQuery(fallbackTab)
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
  border-radius: 10px;

  :deep(.el-card__body) {
    padding-top: 16px;
  }
}
</style>
