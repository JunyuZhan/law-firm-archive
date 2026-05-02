<template>
  <div class="source-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>档案来源管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon> 新增来源
          </el-button>
        </div>
      </template>

      <el-alert
        title="来源配置说明"
        type="info"
        description="配置可接收档案的来源系统或外部业务平台，如管理系统、法院系统等。启用后，对应系统可通过 API 向档案系统推送档案。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <el-table
        v-loading="loading"
        :data="sources"
        stripe
      >
        <el-table-column
          prop="sourceCode"
          label="来源编码"
          width="150"
        />
        <el-table-column
          prop="sourceName"
          label="来源名称"
          width="180"
        />
        <el-table-column
          prop="sourceType"
          label="类型"
          width="120"
        >
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.sourceType)">
              {{ getTypeName(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="apiUrl"
          label="API地址"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="authType"
          label="认证方式"
          width="100"
        >
          <template #default="{ row }">
            {{ getAuthTypeName(row.authType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="enabled"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-switch 
              v-model="row.enabled" 
              :loading="row._toggling"
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="lastSyncStatus"
          label="同步状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.lastSyncStatus"
              :type="getSyncStatusType(row.lastSyncStatus)"
              size="small"
            >
              {{ row.lastSyncStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
            <span
              v-else
              class="text-gray"
            >-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="lastSyncAt"
          label="最后同步"
          width="160"
        >
          <template #default="{ row }">
            {{ row.lastSyncAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="250"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="success"
              link
              :loading="row._testing"
              @click="handleTest(row)"
            >
              测试
            </el-button>
            <el-button
              type="warning"
              link
              @click="handleRegenerateKey(row)"
            >
              重置密钥
            </el-button>
            <el-popconfirm
              title="确定删除该来源？"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  type="danger"
                  link
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑来源' : '新增来源'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="来源编码"
          prop="sourceCode"
        >
          <el-input
            v-model="formData.sourceCode"
            placeholder="如：LAW_FIRM_MAIN"
            :disabled="isEdit"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="来源名称"
          prop="sourceName"
        >
          <el-input
            v-model="formData.sourceName"
            placeholder="如：律所管理系统"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          label="来源类型"
          prop="sourceType"
        >
          <el-select
            v-model="formData.sourceType"
            placeholder="请选择"
            style="width: 100%"
          >
            <el-option
              label="律所系统"
              value="LAW_FIRM"
            />
            <el-option
              label="法院系统"
              value="COURT"
            />
            <el-option
              label="企业系统"
              value="ENTERPRISE"
            />
            <el-option
              label="其他"
              value="OTHER"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="API地址"
          prop="apiUrl"
        >
          <el-input
            v-model="formData.apiUrl"
            placeholder="如：http://localhost:8080/api"
          />
        </el-form-item>
        <el-form-item
          label="API密钥"
          prop="apiKey"
        >
          <el-input 
            v-model="formData.apiKey" 
            placeholder="用于验证来源身份的密钥"
            show-password
          />
          <div class="form-tip">
            此密钥需与来源系统配置一致，用于签名验证
          </div>
        </el-form-item>
        <el-form-item
          label="认证方式"
          prop="authType"
        >
          <el-select
            v-model="formData.authType"
            placeholder="请选择"
            style="width: 100%"
            disabled
          >
            <el-option
              label="API Key"
              value="API_KEY"
            />
          </el-select>
          <div class="form-tip">
            当前版本开放接口仅支持 API Key 认证方式。
          </div>
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input
            v-model="formData.callbackUrl"
            placeholder="处理完成后回调来源系统的通知地址（可选）"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="来源说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { sourceApi } from '@/api/archive'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const sources = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

const formData = reactive({
  id: null,
  sourceCode: '',
  sourceName: '',
  sourceType: 'LAW_FIRM',
  apiUrl: '',
  apiKey: '',
  authType: 'API_KEY',
  callbackUrl: '',
  description: ''
})

const requireTrimmedText = (message) => ({
  validator: (_rule, value, callback) => {
    if (!value?.trim()) {
      callback(new Error(message))
      return
    }
    callback()
  },
  trigger: 'blur'
})

const rules = {
  sourceCode: [requireTrimmedText('请输入来源编码')],
  sourceName: [requireTrimmedText('请输入来源名称')],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  authType: [{ required: true, message: '认证方式不能为空', trigger: 'change' }]
}

const getTypeName = (type) => {
  const map = {
    LAW_FIRM: '律所系统',
    COURT: '法院系统',
    ENTERPRISE: '企业系统',
    OTHER: '其他'
  }
  return map[type] || type
}

const getTypeTagType = (type) => {
  const map = {
    LAW_FIRM: 'primary',
    COURT: 'success',
    ENTERPRISE: 'warning',
    OTHER: 'info'
  }
  return map[type] || 'info'
}

const getAuthTypeName = (type) => {
  const map = {
    API_KEY: 'API Key',
    OAUTH2: 'OAuth 2.0',
    BASIC: 'Basic'
  }
  return map[type] || type
}

const getSyncStatusType = (status) => {
  return status === 'SUCCESS' ? 'success' : 'danger'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await sourceApi.list()
    sources.value = (res.data || []).map(item => ({
      ...item,
      _toggling: false,
      _testing: false
    }))
  } catch (e) {
    ElMessage.error('加载失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(formData, {
    id: null,
    sourceCode: '',
    sourceName: '',
    sourceType: 'LAW_FIRM',
    apiUrl: '',
    apiKey: '',
    authType: 'API_KEY',
    callbackUrl: '',
    description: ''
  })
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  loadSourceDetail(row.id)
}

const loadSourceDetail = async (id) => {
  try {
    const res = await sourceApi.get(id)
    const row = res.data || {}
    Object.assign(formData, {
      id: row.id,
      sourceCode: row.sourceCode,
      sourceName: row.sourceName,
      sourceType: row.sourceType,
      apiUrl: row.apiUrl || '',
      apiKey: '', // 不回显密钥
      authType: row.authType || 'API_KEY',
      callbackUrl: row.extraConfig?.callbackUrl || '',
      description: row.description || ''
    })
    isEdit.value = true
    dialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载来源详情失败')
    console.error(e)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const data = {
      sourceCode: formData.sourceCode.trim(),
      sourceName: formData.sourceName.trim(),
      sourceType: formData.sourceType,
      apiUrl: formData.apiUrl?.trim() || '',
      authType: formData.authType,
      description: formData.description?.trim() || '',
      extraConfig: {
        callbackUrl: formData.callbackUrl?.trim() || ''
      }
    }
    // 只在有输入时才传密钥
    if (formData.apiKey?.trim()) {
      data.apiKey = formData.apiKey.trim()
    }

    if (isEdit.value) {
      await sourceApi.update(formData.id, data)
      ElMessage.success(`已更新来源：${formData.sourceName}`)
    } else {
      const res = await sourceApi.create(data)
      const sourceName = res?.data?.sourceName || formData.sourceName
      ElMessage.success(sourceName ? `已创建来源：${sourceName}` : '创建成功')
      if (res?.data?.apiKey) {
        await ElMessageBox.alert(
          `请立即保存以下 API Key，该值仅在创建时返回一次：\n\n${res.data.apiKey}`,
          '来源已创建',
          {
            confirmButtonText: '我已保存'
          }
        )
      }
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggle = async (row) => {
  row._toggling = true
  try {
    await sourceApi.toggle(row.id, row.enabled)
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
  } catch {
    row.enabled = !row.enabled
    ElMessage.error('操作失败')
  } finally {
    row._toggling = false
  }
}

const handleTest = async (row) => {
  row._testing = true
  try {
    await sourceApi.test(row.id)
    ElMessage.success('连接测试成功')
  } catch (e) {
    ElMessage.error(e.message || '连接测试失败')
  } finally {
    row._testing = false
    await loadData()
  }
}

const handleRegenerateKey = async (row) => {
  try {
    await ElMessageBox.confirm(
      `将为来源「${row.sourceName}」重新生成 API Key，原密钥会立即失效。是否继续？`,
      '重置 API Key',
      {
        type: 'warning',
        confirmButtonText: '继续重置',
        cancelButtonText: '取消'
      }
    )

    const res = await sourceApi.regenerateKey(row.id)
    const apiKey = res?.data?.apiKey
    if (!apiKey) {
      ElMessage.error('新 API Key 获取失败')
      return
    }

    await ElMessageBox.alert(
      `请立即保存以下 API Key，原密钥已失效：\n\n${apiKey}`,
      'API Key 已重置',
      {
        confirmButtonText: '我已保存'
      }
    )
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.message || '重置 API Key 失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await sourceApi.delete(row.id)
    ElMessage.success(`已删除来源：${row.sourceName}`)
    loadData()
  } catch {
    ElMessage.error('删除失败')
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

.text-gray {
  color: #999;
}

.form-tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
