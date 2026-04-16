<template>
  <div class="initial-setup-page">
    <div class="page-header">
      <h1>系统基础设置</h1>
      <p>用于维护系统名称、站点标识、备案信息与登录页、页脚等基础展示内容。</p>
    </div>

    <el-alert
      :title="setupStatus.title"
      :type="setupStatus.type"
      :closable="false"
      class="setup-alert"
    />

    <el-card
      shadow="never"
      class="setup-card"
    >
      <template #header>
        <div class="card-header">
          <span>基础信息设置</span>
          <el-button
            type="primary"
            :loading="saving"
            @click="saveSetup"
          >
            保存基础设置
          </el-button>
        </div>
      </template>

      <el-form
        label-width="140px"
        class="setup-form"
      >
        <el-form-item label="系统名称">
          <el-input
            v-model.trim="form.systemName"
            maxlength="50"
            placeholder="例如：某某律师事务所电子档案系统"
          />
        </el-form-item>
        <el-form-item label="英文名称">
          <el-input
            v-model.trim="form.systemNameEn"
            maxlength="80"
            placeholder="例如：Law Firm Archive System"
          />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <div class="logo-config">
            <div class="logo-preview">
              <img
                v-if="form.logoUrl"
                :src="form.logoUrl"
                alt="Logo 预览"
              >
              <span v-else>未上传</span>
            </div>
            <div class="logo-actions">
              <el-input
                v-model.trim="form.logoUrl"
                placeholder="可直接填写 Logo URL，或使用右侧上传"
              />
              <el-upload
                :show-file-list="false"
                :auto-upload="false"
                :http-request="handleLogoUpload"
                :accept="LOGO_ACCEPT_TYPES"
              >
                <el-button :loading="logoUploading">
                  上传 Logo
                </el-button>
              </el-upload>
            </div>
            <div class="form-hint">
              {{ LOGO_UPLOAD_HINT }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="ICP备案号">
          <el-input
            v-model.trim="form.icp"
            maxlength="80"
            placeholder="例如：京ICP备12345678号-1"
          />
        </el-form-item>
        <el-form-item label="版权说明">
          <el-input
            v-model.trim="form.copyright"
            maxlength="120"
            placeholder="例如：© 2026 某某律师事务所"
          />
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="preview-block">
        <div class="preview-title">
          效果预览
        </div>
        <div class="preview-box">
          <div class="preview-brand">
            <img
              v-if="form.logoUrl"
              :src="form.logoUrl"
              alt="预览 Logo"
              class="preview-logo"
            >
            <div>
              <div class="preview-name">
                {{ form.systemName || '档案管理系统' }}
              </div>
              <div class="preview-subname">
                {{ form.systemNameEn || 'Archive Management System' }}
              </div>
            </div>
          </div>
          <div class="preview-footer">
            <span>{{ form.icp || '未设置ICP备案号' }}</span>
            <span>{{ form.copyright || '未设置版权说明' }}</span>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="next-step-block">
        <div class="preview-title">
          建议下一步
        </div>
        <ul class="plain-list">
          <li>确认系统名称、Logo 和版权信息是否符合当前机构的管理规范。</li>
          <li>确认 ICP 备案号是否需要在登录页和页脚展示。</li>
          <li>基础设置保存后，可继续在系统配置中完善上传限制、借阅策略和安全参数。</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchUpdateConfigs, getConfigsGrouped, uploadSiteLogo } from '@/api/config'
import { useAppStore } from '@/stores/app'
import { LOGO_ACCEPT_TYPES, LOGO_UPLOAD_HINT, validateLogoFile } from '@/utils/logoUpload'

const appStore = useAppStore()
const saving = ref(false)
const logoUploading = ref(false)

const form = reactive({
  systemName: '',
  systemNameEn: '',
  logoUrl: '',
  icp: '',
  copyright: ''
})

const defaultValues = {
  systemName: '档案管理系统',
  systemNameEn: 'Archive Management System',
  copyright: '© 2024 档案管理系统'
}

const setupStatus = computed(() => {
  const initialized = form.systemName && form.systemName !== defaultValues.systemName
  if (initialized) {
    return {
      type: 'success',
      title: '当前站点基础信息已配置完成，可根据管理需要继续调整。'
    }
  }
  return {
    type: 'warning',
    title: '当前仍接近默认站点信息，建议尽快完成基础设置。'
  }
})

const loadSiteConfigs = async () => {
  try {
    const res = await getConfigsGrouped()
    const siteConfigs = res?.data?.SITE || []
    const configMap = {}
    siteConfigs.forEach(item => {
      configMap[item.configKey] = item.configValue
    })
    form.systemName = configMap['system.site.name'] || defaultValues.systemName
    form.systemNameEn = configMap['system.site.name.en'] || defaultValues.systemNameEn
    form.logoUrl = configMap['system.site.logo'] || ''
    form.icp = configMap['system.site.icp'] || ''
    form.copyright = configMap['system.site.copyright'] || defaultValues.copyright
  } catch (error) {
    ElMessage.error('加载基础设置失败')
  }
}

const saveSetup = async () => {
  if (!form.systemName) {
    ElMessage.warning('请先填写系统名称')
    return
  }
  saving.value = true
  try {
    await batchUpdateConfigs({
      'system.site.name': form.systemName,
      'system.site.name.en': form.systemNameEn || defaultValues.systemNameEn,
      'system.site.logo': form.logoUrl,
      'system.site.icp': form.icp,
      'system.site.copyright': form.copyright || defaultValues.copyright
    })
    await appStore.loadSiteConfig()
    ElMessage.success('基础设置保存成功')
  } catch (error) {
    ElMessage.error('基础设置保存失败')
  } finally {
    saving.value = false
  }
}

const handleLogoUpload = async ({ file }) => {
  if (!validateLogoFile(file)) {
    return
  }
  logoUploading.value = true
  try {
    const res = await uploadSiteLogo(file)
    form.logoUrl = res?.data?.logoUrl || ''
    await appStore.loadSiteConfig()
    ElMessage.success('Logo 上传成功')
  } catch (error) {
    ElMessage.error('Logo 上传失败')
  } finally {
    logoUploading.value = false
  }
}

onMounted(() => {
  loadSiteConfigs()
})
</script>

<style scoped>
.initial-setup-page {
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
  color: #606266;
  line-height: 1.6;
}

.setup-alert {
  margin-bottom: 0;
}

.setup-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.setup-form {
  max-width: 860px;
}

.logo-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.logo-preview {
  width: 100px;
  height: 100px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #909399;
  font-size: 12px;
}

.logo-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.logo-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.form-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.preview-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.preview-box {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
}

.preview-brand {
  display: flex;
  align-items: center;
  gap: 16px;
}

.preview-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
}

.preview-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.preview-subname {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.preview-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  margin-top: 18px;
  font-size: 12px;
  color: #909399;
}

.plain-list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.9;
}
</style>
