<template>
  <div class="system-config">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">系统配置</span>
          <div class="actions">
            <el-button @click="refreshCache" :loading="refreshing">
              <el-icon><Refresh /></el-icon>
              刷新缓存
            </el-button>
            <el-button type="primary" @click="saveChanges" :loading="saving" :disabled="!hasChanges">
              <el-icon><Check /></el-icon>
              保存修改
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 档案号规则 -->
        <el-tab-pane label="档案号规则" name="ARCHIVE_NO">
          <div class="config-section">
            <p class="section-desc">配置档案号生成规则，包括各类档案的前缀、日期格式和序号位数。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item 
                v-for="config in archiveNoConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey">
                <el-input 
                  v-model="editedConfigs[config.configKey]"
                  :placeholder="config.configValue"
                  :disabled="!config.editable"
                  style="width: 200px"
                  @input="markChanged(config.configKey)"
                />
                <span class="config-key">{{ config.configKey }}</span>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 保管期限 -->
        <el-tab-pane label="保管期限" name="RETENTION">
          <div class="config-section">
            <p class="section-desc">配置档案保管期限相关参数。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item 
                v-for="config in retentionConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey">
                <el-input 
                  v-model="editedConfigs[config.configKey]"
                  :placeholder="config.configValue"
                  :disabled="!config.editable"
                  style="width: 200px"
                  @input="markChanged(config.configKey)"
                />
                <span class="config-key">{{ config.configKey }}</span>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 系统参数 -->
        <el-tab-pane label="系统参数" name="SYSTEM">
          <div class="config-section">
            <p class="section-desc">配置系统运行参数，如文件上传限制、借阅期限等。</p>
            <el-form label-width="180px" class="config-form">
              <el-form-item 
                v-for="config in systemConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey">
                <template v-if="config.configType === 'BOOLEAN'">
                  <el-switch 
                    v-model="editedConfigs[config.configKey]"
                    :disabled="!config.editable"
                    active-value="true"
                    inactive-value="false"
                    @change="markChanged(config.configKey)"
                  />
                </template>
                <template v-else-if="config.configType === 'NUMBER'">
                  <el-input-number
                    v-model.number="editedConfigs[config.configKey]"
                    :disabled="!config.editable"
                    :min="0"
                    style="width: 200px"
                    @change="markChanged(config.configKey)"
                  />
                  <span v-if="config.configKey.includes('size')" class="config-hint">
                    ({{ formatBytes(editedConfigs[config.configKey]) }})
                  </span>
                </template>
                <template v-else>
                  <el-input 
                    v-model="editedConfigs[config.configKey]"
                    :placeholder="config.configValue"
                    :disabled="!config.editable"
                    style="width: 300px"
                    @input="markChanged(config.configKey)"
                  />
                </template>
                <span class="config-key">{{ config.configKey }}</span>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 保管期限管理 -->
    <el-card class="retention-card">
      <template #header>
        <span>保管期限管理</span>
      </template>
      <el-table :data="retentionPeriods" stripe>
        <el-table-column prop="periodCode" label="期限代码" width="120" />
        <el-table-column prop="periodName" label="期限名称" width="120" />
        <el-table-column prop="periodYears" label="年限">
          <template #default="{ row }">
            {{ row.periodYears ? row.periodYears + '年' : '永久' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import { 
  getConfigsGrouped, 
  batchUpdateConfigs, 
  refreshConfigCache 
} from '@/api/config'
import request from '@/utils/request'

const activeTab = ref('ARCHIVE_NO')
const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)

// 配置数据
const allConfigs = ref({})
const editedConfigs = reactive({})
const changedKeys = ref(new Set())

// 保管期限数据
const retentionPeriods = ref([])

// 计算属性：分组配置
const archiveNoConfigs = computed(() => allConfigs.value['ARCHIVE_NO'] || [])
const retentionConfigs = computed(() => allConfigs.value['RETENTION'] || [])
const systemConfigs = computed(() => allConfigs.value['SYSTEM'] || [])

// 是否有修改
const hasChanges = computed(() => changedKeys.value.size > 0)

// 加载配置
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await getConfigsGrouped()
    allConfigs.value = res.data || {}
    
    // 初始化编辑数据
    Object.values(allConfigs.value).flat().forEach(config => {
      editedConfigs[config.configKey] = config.configValue
    })
    
    changedKeys.value.clear()
  } catch (error) {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

// 加载保管期限
const loadRetentionPeriods = async () => {
  try {
    const res = await request.get('/retention-periods')
    retentionPeriods.value = res.data || []
  } catch (error) {
    console.error('加载保管期限失败', error)
  }
}

// 标记修改
const markChanged = (key) => {
  changedKeys.value.add(key)
}

// 保存修改
const saveChanges = async () => {
  if (changedKeys.value.size === 0) {
    ElMessage.info('没有修改需要保存')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要保存 ${changedKeys.value.size} 项配置修改吗？`,
      '确认保存',
      { type: 'warning' }
    )
  } catch {
    return
  }

  saving.value = true
  try {
    const updates = {}
    changedKeys.value.forEach(key => {
      updates[key] = String(editedConfigs[key])
    })
    
    await batchUpdateConfigs(updates)
    ElMessage.success('保存成功')
    changedKeys.value.clear()
    
    // 重新加载配置
    await loadConfigs()
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 刷新缓存
const refreshCache = async () => {
  refreshing.value = true
  try {
    await refreshConfigCache()
    ElMessage.success('缓存刷新成功')
    await loadConfigs()
  } catch (error) {
    ElMessage.error('刷新缓存失败')
  } finally {
    refreshing.value = false
  }
}

// Tab切换
const handleTabChange = () => {
  // 可以在这里添加未保存提示
}

// 格式化字节
const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

onMounted(() => {
  loadConfigs()
  loadRetentionPeriods()
})
</script>

<style scoped>
.system-config {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 12px;
}

.config-section {
  padding: 20px 0;
}

.section-desc {
  margin-bottom: 20px;
  color: #606266;
  font-size: 14px;
}

.config-form {
  max-width: 800px;
}

.config-key {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
  font-family: monospace;
}

.config-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.retention-card {
  margin-top: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
}
</style>
