<template>
  <div class="archive-receive">
    <el-card>
      <template #header>
        <span>手动录入档案</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 800px"
      >
        <el-divider content-position="left">基本信息</el-divider>

        <el-form-item label="档案名称" prop="archiveName">
          <el-input v-model="form.archiveName" placeholder="请输入档案名称" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="档案类型" prop="archiveType">
              <el-select v-model="form.archiveType" placeholder="请选择">
                <el-option label="诉讼案件" value="LITIGATION" />
                <el-option label="非诉项目" value="NON_LITIGATION" />
                <el-option label="咨询" value="CONSULTATION" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="档案分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择">
                <el-option label="案件档案" value="CASE" />
                <el-option label="合同档案" value="CONTRACT" />
                <el-option label="人事档案" value="PERSONNEL" />
                <el-option label="财务档案" value="FINANCE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入档案描述" />
        </el-form-item>

        <el-divider content-position="left">关联信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称">
              <el-input v-model="form.clientName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主办人">
              <el-input v-model="form.responsiblePerson" placeholder="请输入主办人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="结案日期">
              <el-date-picker
                v-model="form.caseCloseDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保管期限" prop="retentionPeriod">
              <el-select v-model="form.retentionPeriod" placeholder="请选择">
                <el-option label="永久" value="PERMANENT" />
                <el-option label="30年" value="30_YEARS" />
                <el-option label="15年" value="15_YEARS" />
                <el-option label="10年" value="10_YEARS" />
                <el-option label="5年" value="5_YEARS" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">物理信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="卷数">
              <el-input-number v-model="form.volumeCount" :min="1" :max="999" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="页数">
              <el-input-number v-model="form.pageCount" :min="0" :max="99999" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="档案目录">
          <el-input v-model="form.catalog" type="textarea" :rows="4" placeholder="请输入档案目录" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交录入
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { archiveApi } from '@/api/archive'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  sourceType: 'MANUAL',
  archiveName: '',
  archiveType: 'OTHER',
  category: 'CASE',
  description: '',
  clientName: '',
  responsiblePerson: '',
  caseCloseDate: '',
  retentionPeriod: '10_YEARS',
  volumeCount: 1,
  pageCount: null,
  catalog: '',
  remarks: ''
})

const rules = {
  archiveName: [
    { required: true, message: '请输入档案名称', trigger: 'blur' }
  ],
  archiveType: [
    { required: true, message: '请选择档案类型', trigger: 'change' }
  ],
  category: [
    { required: true, message: '请选择档案分类', trigger: 'change' }
  ],
  retentionPeriod: [
    { required: true, message: '请选择保管期限', trigger: 'change' }
  ]
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    
    const res = await archiveApi.receive(form)
    ElMessage.success('档案录入成功')
    router.push(`/archives/${res.data.id}`)
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleReset = () => {
  formRef.value.resetFields()
}
</script>

<style lang="scss" scoped>
.archive-receive {
  max-width: 900px;
  margin: 0 auto;
}
</style>
