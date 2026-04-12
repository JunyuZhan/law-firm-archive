<template>
  <div class="center-page">
    <div class="page-header">
      <h1>备份恢复</h1>
      <p>将备份与恢复放在同一入口，方便系统管理员在一个上下文里完成备份校验与恢复接管。</p>
    </div>

    <el-alert
      title="高风险操作"
      type="warning"
      :closable="false"
      description="恢复会影响全系统，请先确认维护模式、备份集完整性和恢复目标环境。"
    />

    <el-card shadow="never" class="tabs-card">
      <el-tabs v-model="activeTab" class="center-tabs" @tab-change="handleTabChange">
        <el-tab-pane
          v-for="tab in tabs"
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

const route = useRoute()
const router = useRouter()

const tabs = [
  {
    name: 'backup',
    label: '备份中心',
    component: defineAsyncComponent(() => import('@/views/system/BackupCenter.vue'))
  },
  {
    name: 'restore',
    label: '恢复中心',
    component: defineAsyncComponent(() => import('@/views/system/RestoreCenter.vue'))
  }
]

const fallbackTab = 'backup'
const activeTab = computed({
  get: () => (tabs.some((tab) => tab.name === route.query.tab) ? route.query.tab : fallbackTab),
  set: (value) => {
    router.replace({ path: '/system/recovery', query: { tab: value } })
  }
})

watch(
  () => route.query.tab,
  () => {
    if (!tabs.some((tab) => tab.name === route.query.tab)) {
      router.replace({ path: '/system/recovery', query: { tab: fallbackTab } })
    }
  },
  { immediate: true }
)

const handleTabChange = (name) => {
  activeTab.value = name
}
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
