<template>
  <div class="location-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>存放位置管理</span>
          <el-button type="primary">
            <el-icon><Plus /></el-icon> 新增位置
          </el-button>
        </div>
      </template>

      <el-table :data="locations" v-loading="loading" stripe>
        <el-table-column prop="locationCode" label="位置编码" width="150" />
        <el-table-column prop="locationName" label="位置名称" min-width="200" />
        <el-table-column prop="roomName" label="库房" width="120" />
        <el-table-column prop="area" label="区域" width="80" />
        <el-table-column prop="shelfNo" label="架号" width="80" />
        <el-table-column label="容量" width="120">
          <template #default="{ row }">
            {{ row.usedCapacity }} / {{ row.totalCapacity }}
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round(row.usedCapacity / row.totalCapacity * 100)"
              :status="row.usedCapacity >= row.totalCapacity ? 'exception' : ''"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'AVAILABLE' ? 'success' : (row.status === 'FULL' ? 'danger' : 'info')">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link>编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { locationApi } from '@/api/archive'

const loading = ref(false)
const locations = ref([])

const getStatusName = (status) => {
  const map = {
    AVAILABLE: '可用',
    FULL: '已满',
    DISABLED: '停用'
  }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await locationApi.list()
    locations.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
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
