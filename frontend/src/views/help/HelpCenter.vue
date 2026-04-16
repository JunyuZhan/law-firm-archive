<template>
  <div class="help-doc-root">
    <header class="doc-hero">
      <h1 class="doc-hero-title">
        使用指南
      </h1>
      <p class="doc-hero-lead">
        帮助中心用于说明<strong>在本系统中如何完成日常工作</strong>：从接收与整理档案，到检索与借阅，以及管理员侧的配置与维护。下列内容与您的登录角色一致，仅展示您有权使用的功能。
      </p>
      <div class="doc-hero-meta">
        <span>当前角色：{{ currentRoleLabel }}</span>
        <span class="doc-meta-sep">·</span>
        <span>功能说明 {{ accessibleEntries.length }} 条</span>
        <span class="doc-meta-sep">·</span>
        <span>左侧目录可跳转，上方搜索可筛选正文</span>
      </div>
    </header>

    <div class="doc-frame">
      <aside class="doc-sidebar">
        <div class="doc-sidebar-head">
          目录
        </div>
        <el-input
          v-model.trim="keyword"
          class="doc-sidebar-search"
          size="small"
          placeholder="搜索文档…"
          clearable
        />
        <nav
          class="doc-toc"
          aria-label="文档目录"
        >
          <button
            v-for="item in tocItems"
            :key="item.id"
            type="button"
            class="doc-toc-item"
            :class="[
              `doc-toc-level-${item.level}`,
              { 'is-active': activeId === item.id }
            ]"
            @click="scrollToAnchor(item.id)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <div class="doc-body">
        <div class="doc-body-inner">
          <section
            v-if="visibleBusinessGroups.length"
            id="help-business"
            class="doc-chapter"
          >
            <h2 class="doc-chapter-title">
              业务功能
            </h2>
            <p class="doc-chapter-intro">
              按业务链路组织：入库与接收、保存与整理、借阅与利用。每一节给出操作步骤与注意点，并可直接进入对应页面。
            </p>
            <template
              v-for="group in visibleBusinessGroups"
              :key="group.key"
            >
              <div
                :id="'nav-' + group.key"
                class="doc-section"
              >
                <h3 class="doc-section-title">
                  {{ group.title }}
                </h3>
                <p class="doc-section-lead">
                  {{ group.description }}
                </p>
                <div
                  v-for="(entry, i) in group.entries"
                  :id="entryAnchorId('biz', group.key, i)"
                  :key="entry.route + '-' + i"
                  class="doc-entry"
                >
                  <h4 class="doc-entry-title">
                    {{ entry.title }}
                  </h4>
                  <p class="doc-p">
                    {{ entry.description }}
                  </p>
                  <template v-if="entry.steps?.length">
                    <div class="doc-label">
                      操作步骤
                    </div>
                    <ol class="doc-ol">
                      <li
                        v-for="step in entry.steps"
                        :key="step"
                      >
                        {{ step }}
                      </li>
                    </ol>
                  </template>
                  <template v-if="entry.tips?.length">
                    <div class="doc-label">
                      注意事项
                    </div>
                    <ul class="doc-ul">
                      <li
                        v-for="tip in entry.tips"
                        :key="tip"
                      >
                        {{ tip }}
                      </li>
                    </ul>
                  </template>
                  <button
                    type="button"
                    class="doc-inline-action"
                    @click="router.push(entry.route)"
                  >
                    在系统中打开此功能
                  </button>
                </div>
              </div>
            </template>
          </section>

          <section
            v-if="visibleAdminGroups.length"
            id="help-admin"
            class="doc-chapter"
          >
            <h2 class="doc-chapter-title">
              系统管理
            </h2>
            <p class="doc-chapter-intro">
              面向系统管理员：权限、站点与参数、备份恢复、日志与运行信息等后台能力。
            </p>
            <template
              v-for="group in visibleAdminGroups"
              :key="group.key"
            >
              <div
                :id="'admin-nav-' + group.key"
                class="doc-section"
              >
                <h3 class="doc-section-title">
                  {{ group.title }}
                </h3>
                <p class="doc-section-lead">
                  {{ group.description }}
                </p>
                <div
                  v-for="(entry, i) in group.entries"
                  :id="entryAnchorId('adm', group.key, i)"
                  :key="entry.route + '-' + i"
                  class="doc-entry"
                >
                  <h4 class="doc-entry-title">
                    {{ entry.title }}
                  </h4>
                  <p class="doc-p">
                    {{ entry.description }}
                  </p>
                  <template v-if="entry.steps?.length">
                    <div class="doc-label">
                      操作步骤
                    </div>
                    <ol class="doc-ol">
                      <li
                        v-for="step in entry.steps"
                        :key="step"
                      >
                        {{ step }}
                      </li>
                    </ol>
                  </template>
                  <template v-if="entry.tips?.length">
                    <div class="doc-label">
                      注意事项
                    </div>
                    <ul class="doc-ul">
                      <li
                        v-for="tip in entry.tips"
                        :key="tip"
                      >
                        {{ tip }}
                      </li>
                    </ul>
                  </template>
                  <button
                    type="button"
                    class="doc-inline-action"
                    @click="router.push(entry.route)"
                  >
                    在系统中打开此功能
                  </button>
                </div>
              </div>
            </template>
          </section>

          <section
            id="help-permissions"
            class="doc-chapter"
          >
            <h2 class="doc-chapter-title">
              权限与菜单可见性
            </h2>
            <p class="doc-chapter-intro">
              下列为常见模块与角色关系；「当前账号」一栏反映您登录后的实际可见范围。
            </p>
            <div class="doc-table-wrap">
              <table class="doc-table">
                <thead>
                  <tr>
                    <th>模块</th>
                    <th>适用角色</th>
                    <th>当前账号</th>
                    <th>说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="row in permissionRows"
                    :key="row.module"
                  >
                    <td>{{ row.module }}</td>
                    <td>{{ row.roles }}</td>
                    <td>
                      <span
                        class="doc-badge"
                        :class="row.enabled ? 'doc-badge-ok' : 'doc-badge-muted'"
                      >{{ row.status }}</span>
                    </td>
                    <td>{{ row.description }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section
            v-if="visibleRoleGuides.length"
            id="help-roles"
            class="doc-chapter"
          >
            <h2 class="doc-chapter-title">
              按角色速览
            </h2>
            <p class="doc-chapter-intro">
              从角色视角概括典型操作与提醒，便于新成员建立整体认识。
            </p>
            <div
              v-for="guide in visibleRoleGuides"
              :id="'guide-' + guide.key"
              :key="guide.key"
              class="doc-role-block"
            >
              <h3 class="doc-section-title">
                {{ guide.title }}
              </h3>
              <p class="doc-p">
                {{ guide.summary }}
              </p>
              <div class="doc-label">
                典型操作
              </div>
              <ul class="doc-ul">
                <li
                  v-for="scenario in guide.scenarios"
                  :key="scenario"
                >
                  {{ scenario }}
                </li>
              </ul>
              <div class="doc-label">
                使用提醒
              </div>
              <ul class="doc-ul">
                <li
                  v-for="caution in guide.cautions"
                  :key="caution"
                >
                  {{ caution }}
                </li>
              </ul>
            </div>
          </section>

          <section
            id="help-faq"
            class="doc-chapter doc-chapter-last"
          >
            <h2 class="doc-chapter-title">
              常见问题
            </h2>
            <p class="doc-chapter-intro">
              以下为使用过程中的高频疑问；仍可通过左侧搜索缩小范围。
            </p>
            <dl class="doc-faq">
              <template
                v-for="item in filteredFaqItems"
                :key="item.question"
              >
                <dt>{{ item.question }}</dt>
                <dd>{{ item.answer }}</dd>
              </template>
            </dl>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { ADMIN_GROUPS, FAQ_ITEMS, HELP_GROUPS, ROLE_GUIDES, ROLE_LABELS } from '@/utils/helpCenter'
import { ROLES, hasPermission } from '@/utils/permission'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const activeId = ref('')

let scrollRootEl = null
let scrollHandler = null

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
    module: '站点、规则与备份',
    roles: '系统管理员',
    enabled: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value),
    status: hasPermission([ROLES.SYSTEM_ADMIN], currentRole.value) ? '可操作' : '不可见',
    description: '站点与展示：名称与 Logo 等外观。规则与运行参数：档案号、上传借阅与安全等策略。备份恢复：数据备份与还原。'
  }
])

const entryAnchorId = (prefix, groupKey, index) => `help-entry-${prefix}-${groupKey}-${index}`

const tocItems = computed(() => {
  const items = []
  if (visibleBusinessGroups.value.length) {
    items.push({ id: 'help-business', label: '业务功能', level: 0 })
    for (const g of visibleBusinessGroups.value) {
      items.push({ id: `nav-${g.key}`, label: g.title, level: 1 })
      g.entries.forEach((e, i) => {
        items.push({ id: entryAnchorId('biz', g.key, i), label: e.title, level: 2 })
      })
    }
  }
  if (visibleAdminGroups.value.length) {
    items.push({ id: 'help-admin', label: '系统管理', level: 0 })
    for (const g of visibleAdminGroups.value) {
      items.push({ id: `admin-nav-${g.key}`, label: g.title, level: 1 })
      g.entries.forEach((e, i) => {
        items.push({ id: entryAnchorId('adm', g.key, i), label: e.title, level: 2 })
      })
    }
  }
  items.push({ id: 'help-permissions', label: '权限与可见性', level: 0 })
  if (visibleRoleGuides.value.length) {
    items.push({ id: 'help-roles', label: '按角色速览', level: 0 })
    for (const guide of visibleRoleGuides.value) {
      items.push({ id: `guide-${guide.key}`, label: guide.title.replace(/手册$/, ''), level: 1 })
    }
  }
  items.push({ id: 'help-faq', label: '常见问题', level: 0 })
  return items
})

const getScrollRoot = () => document.querySelector('.main-content')

const syncActiveId = () => {
  const root = scrollRootEl || getScrollRoot()
  if (!root) {
    return
  }
  const rootRect = root.getBoundingClientRect()
  const line = rootRect.top + 72
  const ids = tocItems.value.map((i) => i.id)
  let hit = ids[0] || ''
  for (const id of ids) {
    const el = document.getElementById(id)
    if (!el) {
      continue
    }
    const top = el.getBoundingClientRect().top
    if (top <= line) {
      hit = id
    }
  }
  activeId.value = hit
}

const detachScrollSpy = () => {
  if (scrollRootEl && scrollHandler) {
    scrollRootEl.removeEventListener('scroll', scrollHandler)
  }
  scrollRootEl = null
  scrollHandler = null
}

const attachScrollSpy = () => {
  detachScrollSpy()
  scrollRootEl = getScrollRoot()
  if (!scrollRootEl) {
    return
  }
  scrollHandler = () => syncActiveId()
  scrollRootEl.addEventListener('scroll', scrollHandler, { passive: true })
  syncActiveId()
}

const scrollToAnchor = (id) => {
  const el = document.getElementById(id)
  const root = getScrollRoot()
  if (!el || !root) {
    return
  }
  activeId.value = id
  const elTop = el.getBoundingClientRect().top
  const rootTop = root.getBoundingClientRect().top
  const nextTop = root.scrollTop + (elTop - rootTop) - 12
  root.scrollTo({ top: Math.max(0, nextTop), behavior: 'smooth' })
}

watch([tocItems, keyword], () => {
  nextTick(() => {
    attachScrollSpy()
    syncActiveId()
  })
})

onMounted(() => {
  nextTick(() => {
    attachScrollSpy()
    syncActiveId()
  })
})

onUnmounted(() => {
  detachScrollSpy()
})
</script>

<style lang="scss" scoped>
/* 文档站式布局：抵消主内容区内边距，形成独立阅读区 */
.help-doc-root {
  margin: -20px -20px -8px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 0;
  min-height: calc(100vh - 120px);
}

.doc-hero {
  padding: 28px 32px 22px;
  border-bottom: 1px solid #e5e6eb;
  background: linear-gradient(180deg, #fafbfc 0%, #fff 100%);
}

.doc-hero-title {
  margin: 0 0 12px;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #111827;
  line-height: 1.25;
}

.doc-hero-lead {
  margin: 0 0 14px;
  max-width: 920px;
  font-size: 15px;
  line-height: 1.75;
  color: #4b5563;
}

.doc-hero-meta {
  font-size: 13px;
  line-height: 1.6;
  color: #6b7280;
}

.doc-meta-sep {
  margin: 0 8px;
  color: #d1d5db;
}

.doc-frame {
  display: flex;
  align-items: stretch;
}

.doc-sidebar {
  width: 260px;
  flex-shrink: 0;
  padding: 20px 12px 48px 20px;
  border-right: 1px solid #e5e6eb;
  background: #f9fafb;
  position: sticky;
  top: 0;
  align-self: flex-start;
  max-height: calc(100vh - 72px);
  overflow: auto;
}

.doc-sidebar-head {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #9ca3af;
  margin-bottom: 10px;
}

.doc-sidebar-search {
  margin-bottom: 16px;

  :deep(.el-input__wrapper) {
    border-radius: 4px;
    box-shadow: 0 0 0 1px #e5e7eb inset;
  }
}

.doc-toc {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.doc-toc-item {
  display: block;
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.45;
  padding: 7px 10px 7px 8px;
  border-radius: 4px;
  border-left: 2px solid transparent;
  color: #374151;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: rgba(64, 158, 255, 0.08);
    color: var(--el-color-primary);
  }

  &.is-active {
    background: rgba(64, 158, 255, 0.1);
    border-left-color: var(--el-color-primary);
    color: var(--el-color-primary);
    font-weight: 500;
  }
}

.doc-toc-level-0 {
  margin-top: 10px;
  font-weight: 600;
  color: #111827;

  &:first-child {
    margin-top: 0;
  }
}

.doc-toc-level-1 {
  padding-left: 14px;
  font-weight: 400;
  color: #4b5563;
  font-size: 13px;
}

.doc-toc-level-2 {
  padding-left: 22px;
  font-size: 12px;
  color: #6b7280;
}

.doc-body {
  flex: 1;
  min-width: 0;
  background: #fff;
}

.doc-body-inner {
  max-width: 800px;
  padding: 28px 40px 56px 36px;
}

.doc-chapter {
  scroll-margin-top: 16px;
  padding-bottom: 8px;
  margin-bottom: 40px;
  border-bottom: 1px solid #e5e6eb;

  &.doc-chapter-last {
    border-bottom: none;
    margin-bottom: 0;
  }
}

.doc-chapter-title {
  margin: 0 0 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 22px;
  font-weight: 600;
  color: #111827;
  letter-spacing: -0.02em;
}

.doc-chapter-intro {
  margin: 0 0 24px;
  font-size: 14px;
  line-height: 1.75;
  color: #6b7280;
}

.doc-section {
  scroll-margin-top: 16px;
  margin-bottom: 32px;

  &:last-child {
    margin-bottom: 0;
  }
}

.doc-section-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
}

.doc-section-lead {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  color: #6b7280;
}

.doc-entry {
  scroll-margin-top: 16px;
  padding: 20px 0 22px;
  border-top: 1px solid #f3f4f6;

  &:first-of-type {
    border-top: none;
    padding-top: 0;
  }
}

.doc-entry-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.doc-p {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
}

.doc-label {
  margin: 14px 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: #9ca3af;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.doc-ol,
.doc-ul {
  margin: 0 0 12px;
  padding-left: 1.2em;
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
}

.doc-inline-action {
  margin-top: 6px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-color-primary);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.doc-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
}

.doc-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  line-height: 1.55;
  color: #374151;

  th,
  td {
    padding: 10px 14px;
    text-align: left;
    border-bottom: 1px solid #f3f4f6;
    vertical-align: top;
  }

  th {
    background: #f9fafb;
    font-weight: 600;
    color: #111827;
    white-space: nowrap;
  }

  tr:last-child td {
    border-bottom: none;
  }
}

.doc-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.doc-badge-ok {
  background: #ecfdf5;
  color: #047857;
}

.doc-badge-muted {
  background: #f3f4f6;
  color: #6b7280;
}

.doc-role-block {
  scroll-margin-top: 16px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px dashed #e5e6eb;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }
}

.doc-faq {
  margin: 0;
}

.doc-faq dt {
  margin: 20px 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  line-height: 1.5;

  &:first-child {
    margin-top: 0;
  }
}

.doc-faq dd {
  margin: 0 0 4px;
  padding-left: 0;
  font-size: 14px;
  line-height: 1.8;
  color: #4b5563;
}

@media (max-width: 960px) {
  .help-doc-root {
    margin: -12px -12px 0;
  }

  .doc-hero {
    padding: 20px 16px 16px;
  }

  .doc-hero-title {
    font-size: 22px;
  }

  .doc-frame {
    flex-direction: column;
  }

  .doc-sidebar {
    position: relative;
    max-height: none;
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e5e6eb;
  }

  .doc-toc {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 6px;
  }

  .doc-toc-item {
    width: auto;
    border-left: none;
    padding: 6px 10px;

    &.is-active {
      border-left: none;
    }
  }

  .doc-toc-level-1,
  .doc-toc-level-2 {
    padding-left: 10px;
  }

  .doc-body-inner {
    padding: 20px 16px 40px;
  }
}
</style>
