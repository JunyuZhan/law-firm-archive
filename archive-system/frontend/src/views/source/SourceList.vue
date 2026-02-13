<template>
  <div class="source-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>档案来源管理</span>
          <el-button type="primary">
            <el-icon><Plus /></el-icon> 新增来源
          </el-button>
        </div>
      </template>

      <el-alert
        title="来源配置说明"
        type="info"
        description="配置可接收档案的外部系统，如律所管理系统、法院系统等。启用后，对应系统可通过API推送档案到本系统。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <el-table :data="sources" v-loading="loading" stripe>
        <el-table-column prop="sourceCode" label="来源编码" width="150" />
        <el-table-column prop="sourceName" label="来源名称" width="200" />
        <el-table-column prop="sourceType" label="类型" width="120">
          <template #default="{ row }">
            {{ getTypeName(row.sourceType) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="apiUrl" label="API地址" width="200" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncAt" label="最后同步" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link>编辑</el-button>
            <el-button type="info" link>测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sourceApi } from '@/api/archive'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const sources = ref([])

const getTypeName = (type) => {
  const map = {
    LAW_FIRM: '律所系统',
    COURT: '法院系统',
    ENTERPRISE: '企业系统',
    OTHER: '其他'
  }
  return map[type] || type
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await sourceApi.list()
    sources.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleToggle = async (row) => {
  try {
    await sourceApi.toggle(row.id, row.enabled)
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = !row.enabled // 回滚
    console.error(e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
