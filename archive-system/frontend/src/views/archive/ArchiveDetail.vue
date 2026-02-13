<template>
  <div class="archive-detail" v-loading="loading">
    <el-page-header @back="$router.back()">
      <template #content>
        <span>档案详情 - {{ archive.archiveNo }}</span>
      </template>
    </el-page-header>

    <div class="detail-content" v-if="archive.id">
      <!-- 基本信息 -->
      <el-card class="info-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="档案编号">{{ archive.archiveNo }}</el-descriptions-item>
          <el-descriptions-item label="档案名称" :span="2">{{ archive.archiveName }}</el-descriptions-item>
          <el-descriptions-item label="档案类型">{{ archive.archiveTypeName }}</el-descriptions-item>
          <el-descriptions-item label="档案分类">{{ archive.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(archive.status)">{{ archive.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ archive.clientName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主办人">{{ archive.responsiblePerson || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结案日期">{{ archive.caseCloseDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="卷数">{{ archive.volumeCount }}</el-descriptions-item>
          <el-descriptions-item label="页数">{{ archive.pageCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电子档案">
            <el-tag :type="archive.hasElectronic ? 'success' : 'info'">
              {{ archive.hasElectronic ? '有' : '无' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="3">{{ archive.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 来源信息 -->
      <el-card class="info-card">
        <template #header>
          <span>来源信息</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="来源类型">{{ archive.sourceTypeName }}</el-descriptions-item>
          <el-descriptions-item label="来源编号">{{ archive.sourceNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源系统ID">{{ archive.sourceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收时间">{{ archive.receivedAt }}</el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ archive.storedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入库人">{{ archive.storedByName || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 保管信息 -->
      <el-card class="info-card">
        <template #header>
          <span>保管信息</span>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="存放位置">{{ archive.locationName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="盒号">{{ archive.boxNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="保管期限">{{ archive.retentionPeriodName }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ archive.retentionExpireDate || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 电子档案 -->
      <el-card class="info-card" v-if="archive.hasElectronic && archive.files?.length">
        <template #header>
          <span>电子档案文件 ({{ archive.files.length }}个)</span>
        </template>
        <el-table :data="archive.files" stripe>
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="fileType" label="类型" width="120" />
          <el-table-column label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" width="160" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { archiveApi } from '@/api/archive'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const archive = ref({})

const getStatusType = (status) => {
  const map = {
    RECEIVED: 'info',
    PENDING: 'warning',
    STORED: 'success',
    BORROWED: 'danger'
  }
  return map[status] || 'info'
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  return bytes.toFixed(1) + ' ' + units[i]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await archiveApi.getById(route.params.id)
    archive.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleDownload = (file) => {
  if (file.downloadUrl) {
    window.open(file.downloadUrl)
  } else {
    ElMessage.warning('文件暂时无法下载')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.archive-detail {
  .el-page-header {
    margin-bottom: 20px;
  }

  .info-card {
    margin-bottom: 20px;
  }
}
</style>
