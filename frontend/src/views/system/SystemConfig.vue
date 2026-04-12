<template>
  <div class="system-config">
    <div class="page-header">
      <h1>系统配置</h1>
      <p>统一维护档案号规则、站点信息和运行参数，修改后请按需刷新缓存并验证关键流程。</p>
    </div>

    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="card-header">
          <span class="title">系统配置</span>
          <div class="actions">
            <el-button
              :loading="refreshing"
              @click="refreshCache"
            >
              <el-icon><Refresh /></el-icon>
              刷新缓存
            </el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="!hasChanges"
              @click="saveChanges"
            >
              <el-icon><Check /></el-icon>
              保存修改
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs
        v-model="activeTab"
        @tab-change="handleTabChange"
      >
        <!-- 档案号规则 -->
        <el-tab-pane
          label="档案号规则"
          name="ARCHIVE_NO"
        >
          <div class="config-section">
            <p class="section-desc">
              配置档案号生成规则，包括各类档案的前缀、日期格式和序号位数。
            </p>
            <el-form
              label-width="180px"
              class="config-form"
            >
              <el-form-item 
                v-for="config in archiveNoConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey"
              >
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
        <el-tab-pane
          label="保管期限"
          name="RETENTION"
        >
          <div class="config-section">
            <p class="section-desc">
              配置档案保管期限相关参数。
            </p>
            <el-form
              label-width="180px"
              class="config-form"
            >
              <el-form-item 
                v-for="config in retentionConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey"
              >
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

            <el-card
              shadow="never"
              class="retention-card"
            >
              <template #header>
                <span>保管期限管理</span>
              </template>
              <el-table
                :data="retentionPeriods"
                stripe
              >
                <el-table-column
                  prop="periodCode"
                  label="期限代码"
                  width="120"
                />
                <el-table-column
                  prop="periodName"
                  label="期限名称"
                  width="120"
                />
                <el-table-column
                  prop="periodYears"
                  label="年限"
                >
                  <template #default="{ row }">
                    {{ row.periodYears ? row.periodYears + '年' : '永久' }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="sortOrder"
                  label="排序"
                  width="100"
                />
                <el-table-column
                  label="状态"
                  width="100"
                >
                  <template #default="{ row }">
                    <el-tag
                      :type="row.status === 'ACTIVE' ? 'success' : 'info'"
                      size="small"
                    >
                      {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>

        <!-- 系统参数 -->
        <el-tab-pane
          label="系统参数"
          name="SYSTEM"
        >
          <div class="config-section">
            <p class="section-desc">
              配置系统运行参数。
            </p>
            
            <!-- 文件上传配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Upload /></el-icon> 文件上传
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in uploadConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <template v-if="config.configType === 'NUMBER' && config.configKey.includes('size')">
                    <el-input-number
                      v-model.number="editedConfigs[config.configKey]"
                      :disabled="!config.editable"
                      :min="0"
                      :step="1048576"
                      style="width: 200px"
                      @change="markChanged(config.configKey)"
                    />
                    <span class="config-hint">({{ formatBytes(editedConfigs[config.configKey]) }})</span>
                  </template>
                  <template v-else>
                    <el-input 
                      v-model="editedConfigs[config.configKey]"
                      :placeholder="config.configValue"
                      :disabled="!config.editable"
                      style="width: 400px"
                      @input="markChanged(config.configKey)"
                    />
                  </template>
                </el-form-item>
              </el-form>
            </div>

            <!-- 借阅配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Document /></el-icon> 借阅管理
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in borrowConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <el-input-number
                    v-model.number="editedConfigs[config.configKey]"
                    :disabled="!config.editable"
                    :min="1"
                    style="width: 200px"
                    @change="markChanged(config.configKey)"
                  />
                  <span
                    v-if="config.configKey.includes('days')"
                    class="config-hint"
                  >天</span>
                  <span
                    v-if="config.configKey.includes('times')"
                    class="config-hint"
                  >次</span>
                </el-form-item>
              </el-form>
            </div>

            <!-- 水印配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Picture /></el-icon> 水印设置
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in watermarkConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
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
                      :min="1"
                      style="width: 200px"
                      @change="markChanged(config.configKey)"
                    />
                  </template>
                  <template v-else>
                    <el-input 
                      v-model="editedConfigs[config.configKey]"
                      :placeholder="config.configValue"
                      :disabled="!config.editable"
                      style="width: 200px"
                      @input="markChanged(config.configKey)"
                    />
                  </template>
                </el-form-item>
              </el-form>
            </div>

            <!-- 回调配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Connection /></el-icon> 回调通知
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in callbackConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <el-input-number
                    v-model.number="editedConfigs[config.configKey]"
                    :disabled="!config.editable"
                    :min="1"
                    style="width: 200px"
                    @change="markChanged(config.configKey)"
                  />
                  <span
                    v-if="config.configKey.includes('interval') || config.configKey.includes('timeout')"
                    class="config-hint"
                  >毫秒</span>
                  <span
                    v-if="config.configKey.includes('max') && !config.configKey.includes('timeout')"
                    class="config-hint"
                  >次</span>
                </el-form-item>
              </el-form>
            </div>

            <!-- 安全配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Lock /></el-icon> 安全设置
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in securityConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <template v-if="config.configType === 'BOOLEAN'">
                    <el-switch 
                      v-model="editedConfigs[config.configKey]"
                      :disabled="!config.editable"
                      active-value="true"
                      inactive-value="false"
                      @change="markChanged(config.configKey)"
                    />
                  </template>
                  <template v-else>
                    <el-input-number
                      v-model.number="editedConfigs[config.configKey]"
                      :disabled="!config.editable"
                      :min="1"
                      style="width: 200px"
                      @change="markChanged(config.configKey)"
                    />
                    <span
                      v-if="config.configKey.includes('minutes')"
                      class="config-hint"
                    >分钟</span>
                    <span
                      v-if="config.configKey.includes('length')"
                      class="config-hint"
                    >位</span>
                    <span
                      v-if="config.configKey.includes('attempts')"
                      class="config-hint"
                    >次</span>
                  </template>
                </el-form-item>
              </el-form>
            </div>

            <!-- 通知配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Bell /></el-icon> 通知设置
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in notifyConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <template v-if="config.configType === 'BOOLEAN'">
                    <el-switch 
                      v-model="editedConfigs[config.configKey]"
                      :disabled="!config.editable"
                      active-value="true"
                      inactive-value="false"
                      @change="markChanged(config.configKey)"
                    />
                  </template>
                  <template v-else>
                    <el-input-number
                      v-model.number="editedConfigs[config.configKey]"
                      :disabled="!config.editable"
                      :min="1"
                      style="width: 200px"
                      @change="markChanged(config.configKey)"
                    />
                    <span
                      v-if="config.configKey.includes('days')"
                      class="config-hint"
                    >天</span>
                  </template>
                </el-form-item>
              </el-form>
            </div>

            <!-- 搜索配置 -->
            <div class="config-group">
              <h4 class="group-title">
                <el-icon><Search /></el-icon> 搜索设置
              </h4>
              <el-form
                label-width="180px"
                class="config-form"
              >
                <el-form-item 
                  v-for="config in searchConfigs" 
                  :key="config.configKey"
                  :label="config.description || config.configKey"
                >
                  <el-input-number
                    v-model.number="editedConfigs[config.configKey]"
                    :disabled="!config.editable"
                    :min="1"
                    style="width: 200px"
                    @change="markChanged(config.configKey)"
                  />
                  <span
                    v-if="config.configKey.includes('size')"
                    class="config-hint"
                  >条/页</span>
                  <span
                    v-if="config.configKey.includes('results')"
                    class="config-hint"
                  >条</span>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 站点信息 -->
        <el-tab-pane
          label="站点信息"
          name="SITE"
        >
          <div class="config-section">
            <p class="section-desc">
              配置系统基础信息，如名称、备案号、版权等。
            </p>
            <el-form
              label-width="180px"
              class="config-form"
            >
              <el-form-item 
                v-for="config in siteConfigs" 
                :key="config.configKey"
                :label="config.description || config.configKey"
              >
                <template v-if="config.configKey === 'system.site.logo'">
                  <div class="site-logo-config">
                    <div
                      v-if="editedConfigs[config.configKey]"
                      class="site-logo-preview"
                    >
                      <img
                        :src="editedConfigs[config.configKey]"
                        alt="Logo 预览"
                      >
                    </div>
                    <div class="site-logo-actions">
                      <el-input 
                        v-model="editedConfigs[config.configKey]"
                        :placeholder="config.configValue || '未设置'"
                        :disabled="!config.editable"
                        style="width: 400px"
                        @input="markChanged(config.configKey)"
                      />
                      <el-upload
                        :show-file-list="false"
                        :auto-upload="false"
                        :http-request="handleLogoUpload"
                        :accept="LOGO_ACCEPT_TYPES"
                      >
                        <el-button :loading="logoUploading">
                          <el-icon><Upload /></el-icon>
                          上传Logo
                        </el-button>
                      </el-upload>
                    </div>
                    <div class="config-hint">
                      {{ LOGO_UPLOAD_HINT }} 也可直接填写外部 URL；上传后会自动写回当前配置。
                    </div>
                  </div>
                </template>
                <template v-else>
                  <el-input 
                    v-model="editedConfigs[config.configKey]"
                    :placeholder="config.configValue || '未设置'"
                    :disabled="!config.editable"
                    style="width: 400px"
                    @input="markChanged(config.configKey)"
                  />
                </template>
              </el-form-item>
            </el-form>
            <el-alert
              type="info"
              :closable="false"
              style="margin-top: 20px"
            >
              <template #title>
                提示：ICP备案号将显示在登录页和页面底部；系统名称将显示在侧边栏和浏览器标题。
              </template>
            </el-alert>
          </div>
        </el-tab-pane>

      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Check, Upload, Document, Picture, Connection, Lock, Bell, Search } from '@element-plus/icons-vue'
import { 
  getConfigsGrouped, 
  batchUpdateConfigs, 
  refreshConfigCache,
  uploadSiteLogo
} from '@/api/config'
import { useAppStore } from '@/stores/app'
import request from '@/utils/request'
import { LOGO_ACCEPT_TYPES, LOGO_UPLOAD_HINT, validateLogoFile } from '@/utils/logoUpload'

const activeTab = ref('ARCHIVE_NO')
const loading = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const logoUploading = ref(false)
const appStore = useAppStore()

// 配置数据
const allConfigs = ref({})
const editedConfigs = reactive({})
const changedKeys = ref(new Set())

// 保管期限数据
const retentionPeriods = ref([])

// 计算属性：分组配置
const archiveNoConfigs = computed(() => allConfigs.value['ARCHIVE_NO'] || [])
const retentionConfigs = computed(() => allConfigs.value['RETENTION'] || [])

// 系统参数分组
const systemConfigs = computed(() => allConfigs.value['SYSTEM'] || [])

const uploadConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('upload'))
)
const borrowConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('borrow'))
)
const watermarkConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('watermark'))
)
const callbackConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('callback'))
)
const securityConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('password') || c.configKey.includes('login'))
)
const notifyConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('notify'))
)
const searchConfigs = computed(() => 
  systemConfigs.value.filter(c => c.configKey.includes('search'))
)

// 站点信息配置
const siteConfigs = computed(() => allConfigs.value['SITE'] || [])

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

const handleLogoUpload = async ({ file }) => {
  if (!validateLogoFile(file)) {
    return
  }
  logoUploading.value = true
  try {
    const res = await uploadSiteLogo(file)
    const logoUrl = res?.data?.logoUrl || ''
    editedConfigs['system.site.logo'] = logoUrl
    changedKeys.value.delete('system.site.logo')
    ElMessage.success('Logo上传成功')
    await loadConfigs()
    await appStore.loadSiteConfig()
  } catch (error) {
    ElMessage.error('Logo上传失败: ' + (error.message || '未知错误'))
  } finally {
    logoUploading.value = false
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
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.config-card {
  border-radius: 10px;
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

.config-group {
  margin-bottom: 32px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
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

.site-logo-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.site-logo-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.site-logo-preview {
  width: 96px;
  height: 96px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.site-logo-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.retention-card {
  margin-top: 20px;
}

.deploy-overview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.runtime-descriptions {
  margin-bottom: 24px;
}

.dependency-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.dependency-card {
  border-radius: 10px;
}

.dependency-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dependency-name {
  font-weight: 600;
  color: #303133;
}

.dependency-detail-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dependency-detail-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.dependency-detail-key {
  color: #909399;
}

.dependency-detail-value {
  color: #606266;
  text-align: right;
  word-break: break-word;
}

.deploy-overview-card {
  border-radius: 10px;
}

.deploy-card-label {
  font-size: 13px;
  color: #909399;
}

.deploy-card-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.deploy-card-hint {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.8;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-tabs__item) {
  font-size: 15px;
}
</style>
