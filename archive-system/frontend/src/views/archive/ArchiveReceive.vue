<template>
  <div class="archive-receive">
    <el-page-header @back="goBack">
      <template #content>
        <span class="page-title">新建档案</span>
      </template>
    </el-page-header>

    <el-card shadow="never" class="form-card">
      <el-steps :active="currentStep" finish-status="success" align-center class="steps">
        <el-step title="上传文件" description="选择要归档的电子文件" />
        <el-step title="填写信息" description="填写档案基本信息" />
        <el-step title="确认提交" description="确认并提交" />
      </el-steps>

      <!-- 步骤1：上传文件 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-upload
          ref="uploadRef"
          class="file-uploader"
          drag
          multiple
          :auto-upload="false"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处，或 <em>点击选择文件</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 PDF、Word、Excel、图片等格式，单文件不超过100MB
            </div>
          </template>
        </el-upload>

        <div class="file-list" v-if="fileList.length > 0">
          <div class="file-list-header">
            <span>已选择 {{ fileList.length }} 个文件</span>
            <el-button type="danger" link size="small" @click="clearFiles">清空</el-button>
          </div>
        </div>

        <div class="step-actions">
          <el-button type="primary" @click="nextStep" :disabled="fileList.length === 0">
            下一步
          </el-button>
          <el-button @click="skipUpload">跳过上传</el-button>
        </div>
      </div>

      <!-- 步骤2：填写信息 -->
      <div v-show="currentStep === 1" class="step-content">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="archive-form"
        >
          <el-divider content-position="left">基本信息</el-divider>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="档案类型" prop="archiveType">
                <el-select v-model="form.archiveType" placeholder="请选择档案类型" style="width: 100%">
                  <el-option label="文书档案" value="DOCUMENT" />
                  <el-option label="科技档案" value="SCIENCE" />
                  <el-option label="会计档案" value="ACCOUNTING" />
                  <el-option label="人事档案" value="PERSONNEL" />
                  <el-option label="专业档案" value="SPECIAL" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="保管期限" prop="retentionPeriod">
                <el-select v-model="form.retentionPeriod" placeholder="请选择保管期限" style="width: 100%">
                  <el-option label="永久" value="PERMANENT" />
                  <el-option label="30年" value="Y30" />
                  <el-option label="15年" value="Y15" />
                  <el-option label="10年" value="Y10" />
                  <el-option label="5年" value="Y5" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="题名" prop="title">
            <el-input v-model="form.title" placeholder="请输入档案题名" maxlength="500" show-word-limit />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="责任者">
                <el-input v-model="form.responsibility" placeholder="责任者/作者" />
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
                <el-select v-model="form.securityLevel" placeholder="请选择" style="width: 100%">
                  <el-option label="公开" value="PUBLIC" />
                  <el-option label="内部" value="INTERNAL" />
                  <el-option label="秘密" value="CONFIDENTIAL" />
                  <el-option label="机密" value="SECRET" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="页数">
                <el-input-number v-model="form.pageCount" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="件数">
                <el-input-number v-model="form.piecesCount" :min="1" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">业务关联（可选）</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="案件编号">
                <el-input v-model="form.caseNo" placeholder="关联案件编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="案件名称">
                <el-input v-model="form.caseName" placeholder="案件名称" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="委托人">
                <el-input v-model="form.clientName" placeholder="委托人姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="主办律师">
                <el-input v-model="form.lawyerName" placeholder="主办律师姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">其他信息</el-divider>

          <el-form-item label="关键词">
            <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
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
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep">下一步</el-button>
        </div>
      </div>

      <!-- 步骤3：确认提交 -->
      <div v-show="currentStep === 2" class="step-content">
        <el-descriptions title="档案信息确认" :column="2" border>
          <el-descriptions-item label="档案类型">{{ getArchiveTypeName(form.archiveType) }}</el-descriptions-item>
          <el-descriptions-item label="保管期限">{{ getRetentionName(form.retentionPeriod) }}</el-descriptions-item>
          <el-descriptions-item label="题名" :span="2">{{ form.title }}</el-descriptions-item>
          <el-descriptions-item label="责任者">{{ form.responsibility || '-' }}</el-descriptions-item>
          <el-descriptions-item label="文件日期">{{ form.documentDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ form.securityLevel || '内部' }}</el-descriptions-item>
          <el-descriptions-item label="上传文件">{{ fileList.length }} 个</el-descriptions-item>
        </el-descriptions>

        <div class="file-preview" v-if="fileList.length > 0">
          <h4>待上传文件：</h4>
          <el-tag v-for="file in fileList" :key="file.uid" class="file-tag">
            {{ file.name }}
          </el-tag>
        </div>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ submitting ? '提交中...' : '确认提交' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { createArchive, uploadFile } from '@/api/archive'

const router = useRouter()

const currentStep = ref(0)
const uploadRef = ref(null)
const formRef = ref(null)
const fileList = ref([])
const submitting = ref(false)

const form = reactive({
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
  remarks: ''
})

const rules = {
  archiveType: [{ required: true, message: '请选择档案类型', trigger: 'change' }],
  retentionPeriod: [{ required: true, message: '请选择保管期限', trigger: 'change' }],
  title: [{ required: true, message: '请输入档案题名', trigger: 'blur' }]
}

// 返回
const goBack = () => {
  router.push('/archives')
}

// 文件选择变化
const handleFileChange = (file, files) => {
  fileList.value = files
}

// 文件移除
const handleFileRemove = (file, files) => {
  fileList.value = files
}

// 清空文件
const clearFiles = () => {
  fileList.value = []
  uploadRef.value?.clearFiles()
}

// 跳过上传
const skipUpload = () => {
  fileList.value = []
  currentStep.value = 1
}

// 下一步
const nextStep = async () => {
  if (currentStep.value === 1) {
    // 验证表单
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
    // 1. 先上传文件（如果有）
    const uploadedFileIds = []
    if (fileList.value.length > 0) {
      for (const file of fileList.value) {
        try {
          const res = await uploadFile(file.raw, null, null)
          if (res.data?.id) {
            uploadedFileIds.push(res.data.id)
          }
        } catch (e) {
          console.error('文件上传失败:', file.name, e)
        }
      }
    }

    // 2. 创建档案
    const archiveData = {
      ...form,
      fileIds: uploadedFileIds
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

// 格式化函数
const getArchiveTypeName = (type) => {
  const map = {
    DOCUMENT: '文书档案',
    SCIENCE: '科技档案',
    ACCOUNTING: '会计档案',
    PERSONNEL: '人事档案',
    SPECIAL: '专业档案'
  }
  return map[type] || type
}

const getRetentionName = (code) => {
  const map = {
    PERMANENT: '永久',
    Y30: '30年',
    Y15: '15年',
    Y10: '10年',
    Y5: '5年'
  }
  return map[code] || code
}
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

.file-uploader {
  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 40px;
  }
}

.file-list {
  margin-top: 20px;
  
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #666;
  }
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
