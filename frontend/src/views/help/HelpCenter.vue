<template>
  <div class="help-center-page">
    <div class="page-header">
      <h1>帮助中心</h1>
      <p>根据当前角色展示可用功能、办理流程和后台管理说明，便于快速上手系统。</p>
    </div>

    <div class="summary-grid">
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">当前角色</div>
        <div class="summary-value">{{ currentRoleLabel }}</div>
        <div class="summary-hint">不同角色看到的功能说明会自动收口。</div>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">可访问页面</div>
        <div class="summary-value">{{ accessibleEntries.length }}</div>
        <div class="summary-hint">仅统计当前账号在系统中可进入的业务页面。</div>
      </el-card>
      <el-card shadow="never" class="summary-card">
        <div class="summary-label">重点链路</div>
        <div class="summary-value">入库 / 保存 / 借阅</div>
        <div class="summary-hint">帮助内容围绕电子档案三条主流程组织。</div>
      </el-card>
    </div>

    <el-card shadow="never" class="help-card">
      <el-input
        v-model.trim="keyword"
        placeholder="搜索功能名称、步骤或问题，例如：借阅、档案接收、权限管理"
        clearable
      />
    </el-card>

    <el-card shadow="never" class="help-card">
      <template #header>
        <span>我的功能导航</span>
      </template>
      <div class="group-list">
        <section
          v-for="group in visibleBusinessGroups"
          :key="group.key"
          class="help-group"
        >
          <div class="group-head">
            <h3>{{ group.title }}</h3>
            <p>{{ group.description }}</p>
          </div>
          <div class="entry-grid">
            <el-card
              v-for="entry in group.entries"
              :key="entry.route"
              shadow="never"
              class="entry-card"
            >
              <div class="entry-head">
                <div class="entry-title">{{ entry.title }}</div>
                <el-tag size="small" type="success">可用</el-tag>
              </div>
              <div class="entry-desc">{{ entry.description }}</div>
              <div class="entry-actions">
                <el-button text @click="selectEntry(entry)">查看说明</el-button>
                <el-button text @click="router.push(entry.route)">进入页面</el-button>
              </div>
            </el-card>
          </div>
        </section>
      </div>
    </el-card>

    <el-card
      v-if="visibleAdminGroups.length"
      shadow="never"
      class="help-card"
    >
      <template #header>
        <span>管理员手册</span>
      </template>
      <div class="group-list">
        <section
          v-for="group in visibleAdminGroups"
          :key="group.key"
          class="help-group"
        >
          <div class="group-head">
            <h3>{{ group.title }}</h3>
            <p>{{ group.description }}</p>
          </div>
          <div class="entry-grid">
            <el-card
              v-for="entry in group.entries"
              :key="entry.route"
              shadow="never"
              class="entry-card"
            >
              <div class="entry-head">
                <div class="entry-title">{{ entry.title }}</div>
                <el-tag size="small" type="warning">管理</el-tag>
              </div>
              <div class="entry-desc">{{ entry.description }}</div>
              <div class="entry-actions">
                <el-button text @click="selectEntry(entry)">查看说明</el-button>
                <el-button text @click="router.push(entry.route)">进入页面</el-button>
              </div>
            </el-card>
          </div>
        </section>
      </div>
    </el-card>

    <el-card shadow="never" class="help-card">
      <template #header>
        <span>关键权限说明</span>
      </template>
      <el-table :data="permissionRows" stripe>
        <el-table-column prop="module" label="模块" min-width="160" />
        <el-table-column prop="roles" label="适用角色" min-width="180" />
        <el-table-column prop="status" label="当前账号" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="280" />
      </el-table>
    </el-card>

    <el-card
      v-if="visibleRoleGuides.length"
      shadow="never"
      class="help-card"
    >
      <template #header>
        <span>按角色使用手册</span>
      </template>
      <div class="guide-grid">
        <el-card
          v-for="guide in visibleRoleGuides"
          :key="guide.key"
          shadow="never"
          class="guide-card"
        >
          <div class="guide-title">{{ guide.title }}</div>
          <div class="guide-summary">{{ guide.summary }}</div>
          <div class="guide-section-title">典型操作</div>
          <ul class="tip-list">
            <li v-for="scenario in guide.scenarios" :key="scenario">{{ scenario }}</li>
          </ul>
          <div class="guide-section-title">使用提醒</div>
          <ul class="tip-list">
            <li v-for="caution in guide.cautions" :key="caution">{{ caution }}</li>
          </ul>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="help-card">
      <template #header>
        <span>常见问题</span>
      </template>
      <el-collapse accordion>
        <el-collapse-item
          v-for="item in filteredFaqItems"
          :key="item.question"
          :title="item.question"
        >
          <div class="faq-answer">{{ item.answer }}</div>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="使用说明"
      width="720px"
    >
      <template v-if="selectedEntry">
        <div class="detail-block">
          <div class="detail-title">{{ selectedEntry.title }}</div>
          <div class="detail-desc">{{ selectedEntry.description }}</div>
        </div>

        <div class="detail-block">
          <div class="detail-subtitle">操作步骤</div>
          <ol class="detail-list">
            <li v-for="step in selectedEntry.steps || []" :key="step">{{ step }}</li>
          </ol>
        </div>

        <div class="detail-block" v-if="selectedEntry.tips?.length">
          <div class="detail-subtitle">注意事项</div>
          <ul class="tip-list">
            <li v-for="tip in selectedEntry.tips" :key="tip">{{ tip }}</li>
          </ul>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="selectedEntry" type="primary" @click="goToSelected">进入页面</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { ADMIN_GROUPS, FAQ_ITEMS, HELP_GROUPS, ROLE_GUIDES, ROLE_LABELS } from '@/utils/helpCenter'
import { ROLES, hasPermission } from '@/utils/permission'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const detailVisible = ref(false)
const selectedEntry = ref(null)

const currentRole = computed(() => userStore.userType || ROLES.USER)
const currentRoleLabel = computed(() => ROLE_LABELS[currentRole.value] || '普通用户')

const matchesKeyword = (entry) => {
  if (!keyword.value) {
    return true
  }
  const haystack = [entry.title, entry.description, ...(entry.steps || []), ...(entry.tips || [])].join(' ')
  return haystack.includes(keyword.value)
}

const filterEntries = (groups) => groups
  .map((group) => ({
    ...group,
    entries: group.entries.filter((entry) => hasPermission(entry.roles, currentRole.value) && matchesKeyword(entry))
  }))
  .filter((group) => group.entries.length > 0)

const visibleBusinessGroups = computed(() => filterEntries(HELP_GROUPS))
const visibleAdminGroups = computed(() => filterEntries(ADMIN_GROUPS))
const visibleRoleGuides = computed(() => ROLE_GUIDES.filter((guide) => hasPermission(guide.roles, currentRole.value)))
const filteredFaqItems = computed(() => {
  if (!keyword.value) {
    return FAQ_ITEMS
  }
  return FAQ_ITEMS.filter((item) => `${item.question} ${item.answer}`.includes(keyword.value))
})

const accessibleEntries = computed(() => [
  ...visibleBusinessGroups.value.flatMap((group) => group.entries),
  ...visibleAdminGroups.value.flatMap((group) => group.entries)
])

const permissionRows = computed(() => [
  {
    module: '档案接收',
    roles: '系统管理员、档案审核员、档案管理员、普通用户',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_REVIEWER, ROLES.ARCHIVE_MANAGER, ROLES.USER], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_REVIEWER, ROLES.ARCHIVE_MANAGER, ROLES.USER], currentRole.value) ? '可操作' : '不可见',
    description: '用于提交档案材料和入库申请，是否直接入库取决于来源策略。'
  },
  {
    module: '档案设置',
    roles: '系统管理员、档案管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER], currentRole.value) ? '可操作' : '不可见',
    description: '维护分类、全宗、来源、库位等基础档案参数。'
  },
  {
    module: '借阅链接',
    roles: '系统管理员、档案管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER], currentRole.value) ? '可操作' : '不可见',
    description: '用于发放电子借阅外链，并控制有效期和次数。'
  },
  {
    module: '权限管理',
    roles: '系统管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value) ? '可操作' : '不可见',
    description: '用于维护账号、角色和授权边界。'
  },
  {
    module: '操作日志',
    roles: '系统管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value) ? '可操作' : '不可见',
    description: '用于查看关键操作记录和追溯责任。'
  },
  {
    module: '系统配置与恢复',
    roles: '系统管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value) ? '可操作' : '不可见',
    description: '用于维护系统参数、站点信息、备份与恢复。'
  }
])

const selectEntry = (entry) => {
  selectedEntry.value = entry
  detailVisible.value = true
}

const goToSelected = () => {
  if (!selectedEntry.value) {
    return
  }
  detailVisible.value = false
  router.push(selectedEntry.value.route)
}
</script>

<style lang="scss" scoped>
.help-center-page {
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.summary-card {
  border-radius: 10px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.summary-hint {
  margin-top: 8px;
  color: #909399;
  line-height: 1.6;
  font-size: 12px;
}

.help-card {
  border-radius: 10px;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.help-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-head {
  h3 {
    margin: 0 0 6px;
    color: #303133;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: #606266;
    line-height: 1.6;
  }
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.entry-card {
  border-radius: 10px;
}

.entry-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.entry-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.entry-desc {
  margin: 12px 0;
  color: #606266;
  line-height: 1.7;
  min-height: 48px;
}

.entry-actions {
  display: flex;
  gap: 12px;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.guide-card {
  border-radius: 10px;
}

.guide-title {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.guide-summary {
  margin-top: 8px;
  color: #606266;
  line-height: 1.7;
}

.guide-section-title {
  margin: 16px 0 8px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.faq-answer {
  color: #606266;
  line-height: 1.8;
}

.detail-block + .detail-block {
  margin-top: 20px;
}

.detail-title {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.detail-desc {
  margin-top: 8px;
  color: #606266;
  line-height: 1.7;
}

.detail-subtitle {
  margin-bottom: 10px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.detail-list,
.tip-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.8;
}
</style>
