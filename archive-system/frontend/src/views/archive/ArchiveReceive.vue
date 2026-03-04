<template>
  <div class="archive-receive">
    <el-page-header @back="goBack">
      <template #content>
        <span class="page-title">新建档案</span>
      </template>
    </el-page-header>

    <el-card
      shadow="never"
      class="form-card"
    >
      <el-steps
        :active="currentStep"
        finish-status="success"
        align-center
        class="steps"
      >
        <el-step
          title="上传文件"
          description="选择要归档的电子文件"
        />
        <el-step
          title="填写信息"
          description="填写档案基本信息"
        />
        <el-step
          title="确认提交"
          description="确认并提交"
        />
      </el-steps>

      <!-- 步骤1：上传文件 -->
      <div
        v-show="currentStep === 0"
        class="step-content"
      >
        <BatchUpload
          ref="batchUploadRef"
          :allowed-types="allowedFileTypes"
          :max-file-size="maxFileSize"
          :max-files="20"
          :concurrent="3"
          :auto-upload="false"
          @change="handleUploadChange"
          @complete="handleUploadComplete"
        />

        <div class="step-actions">
          <el-button
            type="primary"
            :disabled="uploadedFileIds.length === 0 && !hasSelectedFiles"
            @click="nextStep"
          >
            下一步
          </el-button>
          <el-button @click="skipUpload">
            跳过上传
          </el-button>
        </div>
      </div>

      <!-- 步骤2：填写信息 -->
      <div
        v-show="currentStep === 1"
        class="step-content"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="archive-form"
        >
          <el-divider content-position="left">
            基本信息
          </el-divider>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item
                label="档案形式"
                prop="archiveForm"
              >
                <el-radio-group v-model="form.archiveForm">
                  <el-radio-button value="ELECTRONIC">
                    电子档案
                  </el-radio-button>
                  <el-radio-button value="PHYSICAL">
                    纸质档案
                  </el-radio-button>
                  <el-radio-button value="HYBRID">
                    混合档案
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item
                label="档案类型"
                prop="archiveType"
              >
                <el-select
                  v-model="form.archiveType"
                  placeholder="请选择档案类型"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in archiveTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item
                label="保管期限"
                prop="retentionPeriod"
              >
                <el-select
                  v-model="form.retentionPeriod"
                  placeholder="请选择保管期限"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in retentionOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 纸质档案存放位置（仅纸质/混合档案显示） -->
          <el-row
            v-if="form.archiveForm !== 'ELECTRONIC'"
            :gutter="20"
          >
            <el-col :span="12">
              <el-form-item
                label="存放位置"
                :prop="form.archiveForm !== 'ELECTRONIC' ? 'locationId' : ''"
                :rules="form.archiveForm !== 'ELECTRONIC' ? [{ required: true, message: '请选择存放位置', trigger: 'change' }] : []"
              >
                <el-select
                  v-model="form.locationId"
                  placeholder="请选择存放位置"
                  style="width: 100%"
                  filterable
                >
                  <el-option
                    v-for="item in locationOptions"
                    :key="item.id"
                    :label="`${item.locationName}（${item.roomName || ''}${item.shelfNo ? ' ' + item.shelfNo + '架' : ''}）`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="盒号">
                <el-input
                  v-model="form.boxNo"
                  placeholder="档案盒编号"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item
            label="题名"
            prop="title"
          >
            <el-input
              v-model="form.title"
              placeholder="请输入档案题名"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="责任者">
                <el-input
                  v-model="form.responsibility"
                  placeholder="责任者/作者"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="文件日期">
                <el-date-picker
                  v-model="form.documentDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="密级">
                <el-select
                  v-model="form.securityLevel"
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option
                    label="公开"
                    value="PUBLIC"
                  />
                  <el-option
                    label="内部"
                    value="INTERNAL"
                  />
                  <el-option
                    label="秘密"
                    value="CONFIDENTIAL"
                  />
                  <el-option
                    label="机密"
                    value="SECRET"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="页数">
                <el-input-number
                  v-model="form.pageCount"
                  :min="0"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="件数">
                <el-input-number
                  v-model="form.piecesCount"
                  :min="1"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">
            业务关联（可选）
          </el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="案件编号">
                <el-input
                  v-model="form.caseNo"
                  placeholder="关联案件编号"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="案件名称">
                <el-input
                  v-model="form.caseName"
                  placeholder="案件名称"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="委托人">
                <el-input
                  v-model="form.clientName"
                  placeholder="委托人姓名"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="主办律师">
                <el-input
                  v-model="form.lawyerName"
                  placeholder="主办律师姓名"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">
            其他信息
          </el-divider>

          <el-form-item label="关键词">
            <el-input
              v-model="form.keywords"
              placeholder="多个关键词用逗号分隔"
            />
          </el-form-item>

          <el-form-item label="摘要">
            <el-input
              v-model="form.archiveAbstract"
              type="textarea"
              :rows="3"
              placeholder="档案内容摘要"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="备注">
            <el-input
              v-model="form.remarks"
              type="textarea"
              :rows="2"
              placeholder="备注信息"
            />
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">
            上一步
          </el-button>
          <el-button
            type="primary"
            @click="nextStep"
          >
            下一步
          </el-button>
        </div>
      </div>

      <!-- 步骤3：确认提交 -->
      <div
        v-show="currentStep === 2"
        class="step-content"
      >
        <el-descriptions
          title="档案信息确认"
          :column="2"
          border
        >
          <el-descriptions-item label="档案类型">
            {{ getArchiveTypeName(form.archiveType) }}
          </el-descriptions-item>
          <el-descriptions-item label="保管期限">
            {{ getRetentionName(form.retentionPeriod) }}
          </el-descriptions-item>
          <el-descriptions-item
            label="题名"
            :span="2"
          >
            {{ form.title }}
          </el-descriptions-item>
          <el-descriptions-item label="责任者">
            {{ form.responsibility || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="文件日期">
            {{ form.documentDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="密级">
            {{ getSecurityLevelName(form.securityLevel) }}
          </el-descriptions-item>
          <el-descriptions-item label="上传文件">
            {{ uploadedFileIds.length }} 个
          </el-descriptions-item>
        </el-descriptions>

        <div
          v-if="uploadedFiles.length > 0"
          class="file-preview"
        >
          <h4>已上传文件：</h4>
          <el-tag
            v-for="file in uploadedFiles"
            :key="file.id"
            class="file-tag"
            type="success"
          >
            {{ file.name }}
          </el-tag>
        </div>

        <div class="step-actions">
          <el-button @click="prevStep">
            上一步
          </el-button>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '提交中...' : '确认提交' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createArchive } from '@/api/archive'
import { getAvailableLocations } from '@/api/location'
import BatchUpload from '@/components/BatchUpload.vue'
import {
  getArchiveTypeName,
  getRetentionName,
  getSecurityName,
  getArchiveTypeOptions,
  getRetentionOptions
} from '@/utils/archiveEnums'

const router = useRouter()

// 下拉选项
const archiveTypeOptions = getArchiveTypeOptions()
const retentionOptions = getRetentionOptions()

const currentStep = ref(0)
const batchUploadRef = ref(null)
const formRef = ref(null)
const submitting = ref(false)
const uploadedFileIds = ref([])

// 允许的文件类型
const allowedFileTypes = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'jpg', 'jpeg', 'png', 'gif', 'zip', 'rar', 'ofd', 'tif', 'tiff']
const maxFileSize = 100 * 1024 * 1024 // 100MB

const form = reactive({
  archiveForm: 'ELECTRONIC',
  archiveType: 'DOCUMENT',
  retentionPeriod: 'Y10',
  title: '',
  responsibility: '',
  documentDate: '',
  securityLevel: 'INTERNAL',
  pageCount: null,
  piecesCount: 1,
  caseNo: '',
  caseName: '',
  clientName: '',
  lawyerName: '',
  keywords: '',
  archiveAbstract: '',
  remarks: '',
  locationId: null,
  boxNo: ''
})

const locationOptions = ref([])

const rules = {
  archiveType: [{ required: true, message: '请选择档案类型', trigger: 'change' }],
  retentionPeriod: [{ required: true, message: '请选择保管期限', trigger: 'change' }],
  title: [{ required: true, message: '请输入档案题名', trigger: 'blur' }]
}

// 计算是否有选中的文件（待上传或已上传）
const hasSelectedFiles = computed(() => {
  return batchUploadRef.value?.fileQueue?.length > 0
})

// 返回
const goBack = () => {
  router.push('/archives')
}

// 上传状态变化
const handleUploadChange = (ids) => {
  uploadedFileIds.value = ids
}

// 上传完成
const handleUploadComplete = (result) => {
  if (result.error > 0) {
    ElMessage.warning(`${result.success} 个文件上传成功，${result.error} 个文件上传失败`)
  } else {
    ElMessage.success(`${result.success} 个文件上传成功`)
  }
}

// 跳过上传
const skipUpload = () => {
  batchUploadRef.value?.clearAll()
  uploadedFileIds.value = []
  currentStep.value = 1
}

// 下一步
const nextStep = async () => {
  // 步骤0：上传文件
  if (currentStep.value === 0) {
    // 检查是否有待上传的文件
    const pendingFiles = batchUploadRef.value?.fileQueue?.filter(f => f.status === 'pending') || []
    if (pendingFiles.length > 0) {
      // 开始上传
      await batchUploadRef.value.startUpload()
      
      // 等待上传完成
      await new Promise(resolve => {
        const checkComplete = setInterval(() => {
          const queue = batchUploadRef.value?.fileQueue || []
          const uploading = queue.some(f => f.status === 'pending' || f.status === 'uploading')
          if (!uploading) {
            clearInterval(checkComplete)
            resolve()
          }
        }, 500)
      })
      
      // 更新已上传文件ID
      uploadedFileIds.value = batchUploadRef.value?.uploadedFileIds || []
    }
  }
  
  // 步骤1：验证表单
  if (currentStep.value === 1) {
    try {
      await formRef.value.validate()
    } catch (e) {
      return
    }
  }
  
  currentStep.value++
}

// 上一步
const prevStep = () => {
  currentStep.value--
}

// 提交
const handleSubmit = async () => {
  submitting.value = true
  
  try {
    // 创建档案
    const archiveData = {
      ...form,
      fileIds: uploadedFileIds.value
    }
    
    const res = await createArchive(archiveData)
    
    ElMessage.success('档案创建成功')
    router.push(`/archives/${res.data.id}`)
    
  } catch (e) {
    console.error('提交失败', e)
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 已上传的文件列表
const uploadedFiles = computed(() => {
  return (batchUploadRef.value?.fileQueue || [])
    .filter(f => f.status === 'success')
})

// 注：getArchiveTypeName, getRetentionName, getSecurityName 已从 archiveEnums.js 导入
// 为兼容模板中的调用，保留别名
const getSecurityLevelName = getSecurityName

// 加载可用的存放位置
const loadLocations = async () => {
  try {
    const res = await getAvailableLocations()
    if (res.code === 0 && res.data) {
      locationOptions.value = res.data
    }
  } catch (e) {
    console.error('加载存放位置失败', e)
  }
}

onMounted(() => {
  loadLocations()
})
</script>

<style lang="scss" scoped>
.archive-receive {
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.form-card {
  margin-top: 20px;
}

.steps {
  margin-bottom: 30px;
}

.step-content {
  min-height: 400px;
}


.archive-form {
  max-width: 900px;
  margin: 0 auto;
}

.step-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.file-preview {
  margin-top: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  
  h4 {
    margin: 0 0 12px 0;
    color: #666;
  }
  
  .file-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}
</style>
