<template>
  <div class="archive-search">
    <div class="page-header">
      <h1>档案检索</h1>
      <p>提供关键词与高级条件组合检索，适合在电子档案存量中快速定位案件材料和正文内容。</p>
    </div>

    <!-- 搜索区域 -->
    <el-card
      shadow="never"
      class="search-card"
    >
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="输入关键词搜索档案（题名、档案号、案件编号、委托人等）"
          size="large"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button
          type="primary"
          size="large"
          @click="handleSearch"
        >
          搜索
        </el-button>
      </div>

      <!-- 高级筛选 -->
      <el-collapse v-model="showAdvanced">
        <el-collapse-item
          title="高级筛选"
          name="advanced"
        >
            <el-form
              :model="filters"
              inline
              label-width="80px"
            >
            <el-form-item label="档案类型">
              <el-select
                v-model="filters.archiveType"
                placeholder="全部"
                clearable
              >
                <el-option
                  v-for="item in archiveTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="所属全宗">
              <el-select
                v-model="filters.fondsId"
                placeholder="全部"
                clearable
                filterable
              >
                <el-option
                  v-for="item in fondsOptions"
                  :key="item.id"
                  :label="`${item.fondsNo}｜${item.fondsName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="档案分类">
              <el-select
                v-model="filters.categoryId"
                placeholder="全部"
                clearable
                filterable
              >
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.id"
                  :label="item.fullPath || `${item.categoryCode}｜${item.categoryName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="保管期限">
              <el-select
                v-model="filters.retentionPeriod"
                placeholder="全部"
                clearable
              >
                <el-option
                  v-for="item in retentionOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="密级">
              <el-select
                v-model="filters.securityLevel"
                placeholder="全部"
                clearable
              >
                <el-option
                  v-for="item in securityOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="来源">
              <el-select
                v-model="filters.sourceType"
                placeholder="全部"
                clearable
              >
                <el-option
                  v-for="item in sourceTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="filters.status"
                placeholder="全部"
                clearable
              >
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="归档日期">
              <el-date-picker
                v-model="filters.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
            <el-form-item label="检索正文">
              <el-switch v-model="filters.includeFileContent" />
            </el-form-item>
            <el-form-item class="advanced-actions">
              <el-button
                type="primary"
                @click="handleSearch"
              >
                搜索
              </el-button>
              <el-button @click="resetFilters">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 搜索结果 -->
    <el-card
      v-if="searched"
      shadow="never"
      class="result-card"
    >
      <template #header>
        <div class="result-header">
          <span>搜索结果（共 {{ total }} 条）</span>
        </div>
      </template>

      <div
        v-if="loading"
        class="loading"
      >
        <el-icon
          class="is-loading"
          :size="32"
        >
          <Loading />
        </el-icon>
        <p>搜索中...</p>
      </div>

      <div
        v-else-if="results.length === 0"
        class="no-result"
      >
        <el-empty description="未找到相关档案" />
      </div>

      <div
        v-else
        class="result-list"
      >
        <div
          v-for="item in results"
          :key="item.id"
          class="result-item"
          @click="goToDetail(item)"
        >
          <div class="item-header">
            <el-tag
              size="small"
              :type="getTypeColor(item.archiveType)"
            >
              {{ getTypeName(item.archiveType) }}
            </el-tag>
            <span class="archive-no">{{ item.archiveNo }}</span>
            <el-tag
              size="small"
              :type="getStatusColor(item.status)"
              class="status-tag"
            >
              {{ getStatusName(item.status) }}
            </el-tag>
          </div>
          <div class="item-title">
            <template
              v-for="(segment, index) in getTitleSegments(item)"
              :key="`${item.id || item.archiveNo || 'title'}-${index}`"
            >
              <mark v-if="segment.highlight">{{ segment.text }}</mark>
              <span v-else>{{ segment.text }}</span>
            </template>
          </div>
          <div class="item-meta">
            <span v-if="item.fondsNo">
              全宗 {{ item.fondsNo }}
            </span>
            <span v-if="item.categoryCode">
              分类 {{ item.categoryCode }}
            </span>
            <span v-if="item.caseNo">
              <el-icon><Briefcase /></el-icon>
              {{ item.caseNo }}
            </span>
            <span v-if="item.clientName">
              <el-icon><User /></el-icon>
              {{ item.clientName }}
            </span>
            <span v-if="item.lawyerName">
              <el-icon><Avatar /></el-icon>
              {{ item.lawyerName }}
            </span>
            <span>
              <el-icon><Clock /></el-icon>
              {{ formatDate(item.receivedAt) }}
            </span>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handlePageSizeChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Loading, Briefcase, User, Avatar, Clock } from '@element-plus/icons-vue'
import { searchArchives } from '@/api/archive'
import { getFondsList } from '@/api/fonds'
import { getCategoryTree } from '@/api/category'
import {
  getArchiveTypeName,
  getStatusName,
  getStatusType,
  getArchiveTypeOptions,
  getRetentionOptions,
  getSecurityOptions,
  ARCHIVE_STATUS,
  SOURCE_TYPES
} from '@/utils/archiveEnums'
import { escapeHtml, escapeRegExp } from '@/utils/security'

const router = useRouter()

// 下拉选项
const archiveTypeOptions = getArchiveTypeOptions()
const retentionOptions = getRetentionOptions()
const securityOptions = getSecurityOptions()
const sourceTypeOptions = Object.entries(SOURCE_TYPES).map(([value, label]) => ({ value, label }))
const statusOptions = Object.entries(ARCHIVE_STATUS)
  .filter(([key]) => ['RECEIVED', 'STORED', 'BORROWED'].includes(key))
  .map(([value, label]) => ({ value, label }))
const fondsOptions = ref([])
const categoryOptions = ref([])
const keyword = ref('')
const showAdvanced = ref([])
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
let activeSearchController = null
let activeSearchToken = 0

const filters = reactive({
  fondsId: null,
  categoryId: null,
  archiveType: '',
  retentionPeriod: '',
  securityLevel: '',
  sourceType: '',
  status: '',
  dateRange: null,
  includeFileContent: false
})

const flattenCategoryTree = (nodes = [], result = []) => {
  nodes.forEach((node) => {
    result.push(node)
    if (node.children?.length) {
      flattenCategoryTree(node.children, result)
    }
  })
  return result
}

const loadFonds = async () => {
  try {
    const res = await getFondsList()
    fondsOptions.value = res.data || []
  } catch (e) {
    console.error('加载全宗失败', e)
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryTree(filters.archiveType)
    categoryOptions.value = flattenCategoryTree(res.data || [])
  } catch (e) {
    console.error('加载分类失败', e)
    categoryOptions.value = []
  }
}

watch(
  () => filters.archiveType,
  async () => {
    filters.categoryId = null
    await loadCategories()
  }
)

// 搜索
const handleSearch = async () => {
  if (!keyword.value.trim() && !hasFilters()) {
    return
  }
  
  searched.value = true
  loading.value = true
  pageNum.value = 1
  
  await fetchResults()
}

// 是否有筛选条件
const hasFilters = () => {
  return filters.archiveType || filters.retentionPeriod ||
         filters.fondsId || filters.categoryId ||
         filters.securityLevel || filters.sourceType ||
         filters.status || filters.dateRange ||
         filters.includeFileContent
}

// 获取结果
const fetchResults = async () => {
  loading.value = true
  activeSearchToken += 1
  const currentToken = activeSearchToken
  activeSearchController?.abort()
  activeSearchController = new AbortController()

  try {
    const params = {
      keyword: keyword.value,
      fondsId: filters.fondsId,
      categoryId: filters.categoryId,
      archiveType: filters.archiveType,
      retentionPeriod: filters.retentionPeriod,
      securityLevel: filters.securityLevel,
      sourceType: filters.sourceType,
      status: filters.status,
      includeFileContent: filters.includeFileContent,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    if (filters.dateRange?.length === 2) {
      params.archiveDateStart = filters.dateRange[0]
      params.archiveDateEnd = filters.dateRange[1]
    }
    
    const res = await searchArchives(
      {
        ...params,
        highlight: true
      },
      {
        signal: activeSearchController.signal
      }
    )
    if (currentToken !== activeSearchToken) {
      return
    }
    results.value = (res.data.hits || []).filter(Boolean)
    total.value = res.data.total || 0
  } catch (e) {
    if (e.name === 'CanceledError' || e.code === 'ERR_CANCELED') {
      return
    }
    console.error('搜索失败', e)
    ElMessage.error(e.response?.data?.message || '搜索失败，请重试')
  } finally {
    if (currentToken === activeSearchToken) {
      loading.value = false
    }
  }
}

const resetFilters = () => {
  activeSearchToken += 1
  keyword.value = ''
  filters.fondsId = null
  filters.categoryId = null
  filters.archiveType = ''
  filters.retentionPeriod = ''
  filters.securityLevel = ''
  filters.sourceType = ''
  filters.status = ''
  filters.dateRange = null
  filters.includeFileContent = false
  results.value = []
  total.value = 0
  pageNum.value = 1
  searched.value = false
  loading.value = false
  activeSearchController?.abort()
  activeSearchController = null
}

// 分页
const handlePageChange = () => {
  fetchResults()
}

const handlePageSizeChange = () => {
  pageNum.value = 1
  fetchResults()
}

// 跳转详情
const goToDetail = (item) => {
  router.push(`/archives/${item.id}`)
}

const escapePlainText = (text) => {
  if (!text) return ''
  return escapeHtml(text)
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#x27;/g, "'")
    .replace(/&#x2F;/g, '/')
    .replace(/&#x60;/g, '`')
    .replace(/&#x3D;/g, '=')
}

const buildSegmentsFromHighlight = (text) => {
  if (!text) return []

  const normalized = text.replace(/<\/?(?:em|mark)>/gi, tag =>
    tag.startsWith('</') ? '[[MARK_END]]' : '[[MARK_START]]'
  )
  const safeText = escapePlainText(normalized)
  const rawSegments = safeText.split(/(\[\[MARK_START\]\]|\[\[MARK_END\]\])/g)
  const segments = []
  let highlight = false

  for (const segment of rawSegments) {
    if (!segment) continue
    if (segment === '[[MARK_START]]') {
      highlight = true
      continue
    }
    if (segment === '[[MARK_END]]') {
      highlight = false
      continue
    }
    segments.push({ text: segment, highlight })
  }

  return segments
}

const buildSegmentsFromKeyword = (text) => {
  const content = escapePlainText(text)
  if (!keyword.value || !content) {
    return content ? [{ text: content, highlight: false }] : []
  }

  const regex = new RegExp(`(${escapeRegExp(keyword.value)})`, 'gi')
  return content
    .split(regex)
    .filter(Boolean)
    .map(segment => ({
      text: segment,
      highlight: segment.toLowerCase() === keyword.value.toLowerCase()
    }))
}

const getTitleSegments = (item) => {
  const highlight = item?.highlights?.title
  if (highlight && highlight.length > 0) {
    return buildSegmentsFromHighlight(highlight[0])
  }
  return buildSegmentsFromKeyword(item?.title)
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

// 使用 archiveEnums 中导入的 getArchiveTypeName 和 getStatusName
const getTypeName = getArchiveTypeName

const getTypeColor = (type) => {
  const map = {
    DOCUMENT: '',
    SCIENCE: 'success',
    ACCOUNTING: 'warning',
    PERSONNEL: 'danger',
    SPECIAL: 'info'
  }
  return map[type] || ''
}

const getStatusColor = getStatusType

onMounted(() => {
  loadFonds()
  loadCategories()
})
</script>

<style lang="scss" scoped>
.archive-search {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

.search-card {
  border-radius: 10px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  
  .el-input {
    flex: 1;
  }
}

.advanced-actions {
  :deep(.el-form-item__content) {
    gap: 8px;
  }
}

.result-header {
  font-weight: 500;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.no-result {
  padding: 40px 0;
}

.result-list {
  .result-item {
    padding: 16px;
    border-bottom: 1px solid #eee;
    cursor: pointer;
    transition: background-color 0.2s;
    
    &:hover {
      background-color: #f5f7fa;
    }
    
    &:last-child {
      border-bottom: none;
    }
  }
  
  .item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    
    .archive-no {
      color: #409eff;
      font-family: monospace;
    }
    
    .status-tag {
      margin-left: auto;
    }
  }
  
  .item-title {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 8px;
    
    :deep(mark) {
      background-color: #ffeeba;
      padding: 0 2px;
    }
  }
  
  .item-meta {
    display: flex;
    gap: 16px;
    color: #909399;
    font-size: 13px;
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
