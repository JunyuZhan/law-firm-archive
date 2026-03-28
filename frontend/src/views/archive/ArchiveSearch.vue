<template>
  <div class="archive-search">
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
          <div
            class="item-title"
            v-html="getHighlightedTitle(item)"
          />
          <div class="item-meta">
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
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Loading, Briefcase, User, Avatar, Clock } from '@element-plus/icons-vue'
import { searchArchives } from '@/api/archive'
import {
  getArchiveTypeName,
  getStatusName,
  getStatusType,
  getArchiveTypeOptions,
  getRetentionOptions,
  ARCHIVE_STATUS
} from '@/utils/archiveEnums'
import { escapeHtml, escapeRegExp } from '@/utils/security'

const router = useRouter()

// 下拉选项
const archiveTypeOptions = getArchiveTypeOptions()
const retentionOptions = getRetentionOptions()
const statusOptions = Object.entries(ARCHIVE_STATUS)
  .filter(([key]) => ['RECEIVED', 'STORED', 'BORROWED'].includes(key))
  .map(([value, label]) => ({ value, label }))
const keyword = ref('')
const showAdvanced = ref([])
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const filters = reactive({
  archiveType: '',
  retentionPeriod: '',
  status: '',
  dateRange: null
})

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
         filters.status || filters.dateRange
}

// 获取结果
const fetchResults = async () => {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value,
      archiveType: filters.archiveType,
      retentionPeriod: filters.retentionPeriod,
      status: filters.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    
    if (filters.dateRange?.length === 2) {
      params.archiveDateStart = filters.dateRange[0]
      params.archiveDateEnd = filters.dateRange[1]
    }
    
    const res = await searchArchives({
      ...params,
      highlight: true
    })
    results.value = (res.data.hits || []).filter(Boolean)
    total.value = res.data.total || 0
  } catch (e) {
    console.error('搜索失败', e)
    ElMessage.error(e.response?.data?.message || '搜索失败，请重试')
  } finally {
    loading.value = false
  }
}

// 分页
const handlePageChange = () => {
  fetchResults()
}

// 跳转详情
const goToDetail = (item) => {
  router.push(`/archives/${item.id}`)
}

// 高亮关键词 (安全版本：防止正则注入和 XSS)
const highlightKeyword = (text) => {
  if (!keyword.value || !text) return escapeHtml(text)
  // 1. 先对文本进行 HTML 转义，防止 XSS
  const safeText = escapeHtml(text)
  // 2. 对关键词进行正则转义，防止正则注入
  const safeKeyword = escapeRegExp(keyword.value)
  // 3. 执行高亮替换
  const regex = new RegExp(`(${safeKeyword})`, 'gi')
  return safeText.replace(regex, '<mark>$1</mark>')
}

// 清理后端高亮结果，只保留安全的 <mark> 标签
const sanitizeHighlight = (html) => {
  if (!html) return ''
  // 先提取 <em> 或 <mark> 标签内的内容及位置
  // ES 默认使用 <em> 高亮，替换为 <mark>
  const processed = html
    .replace(/<em>/gi, '\x00MARK_START\x00')
    .replace(/<\/em>/gi, '\x00MARK_END\x00')
    .replace(/<mark>/gi, '\x00MARK_START\x00')
    .replace(/<\/mark>/gi, '\x00MARK_END\x00')
  // 转义所有 HTML
  const escaped = escapeHtml(processed)
  // 恢复 mark 标签
  return escaped
    .replace(/\x00MARK_START\x00/g, '<mark>')
    .replace(/\x00MARK_END\x00/g, '</mark>')
}

const getHighlightedTitle = (item) => {
  const highlight = item?.highlights?.title
  if (highlight && highlight.length > 0) {
    // 后端返回的高亮结果需要清理，只保留安全的 mark 标签
    return sanitizeHighlight(highlight[0])
  }
  return highlightKeyword(item?.title)
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
</script>

<style lang="scss" scoped>
.archive-search {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 20px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  
  .el-input {
    flex: 1;
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
