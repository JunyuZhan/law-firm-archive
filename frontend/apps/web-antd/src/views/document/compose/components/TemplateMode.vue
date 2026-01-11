<script setup lang="ts">
/**
 * 模板制作模式组件
 * 使用模板生成文书：选择模板 → 选择项目 → 填写变量 → 预览生成
 */
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  Card,
  Steps,
  Step,
  Button,
  Space,
  Select,
  Form,
  FormItem,
  Input,
  Row,
  Col,
  Empty,
  Spin,
  Result,
  Divider,
  Tag,
  List,
  ListItem,
} from 'ant-design-vue';
import { getActiveTemplateList, generateDocument, previewTemplate } from '#/api/document/template';
import type { DocumentTemplateDTO, GenerateDocumentCommand } from '#/api/document/template-types';
import { printDocument, type DocumentPrintData } from '@vben/utils';
import MatterSelector from './MatterSelector.vue';
import { TEMPLATE_TYPE_MAP, BUSINESS_TYPE_OPTIONS, VARIABLE_NAME_MAP } from '../types';

defineOptions({ name: 'TemplateMode' });

const emit = defineEmits<{
  (e: 'success'): void;
  (e: 'reset'): void;
}>();

const router = useRouter();

// 状态
const loading = ref(false);
const currentStep = ref(0);
const templates = ref<DocumentTemplateDTO[]>([]);
const selectedTemplate = ref<DocumentTemplateDTO | null>(null);
const templateCategory = ref<string>('');
const templatePreviewContent = ref('');
const templatePreviewLoading = ref(false);
const templateGenerating = ref(false);
const generateSuccess = ref(false);

// 项目选择状态
const selectedMatterId = ref<number | undefined>(undefined);
const selectedDossierId = ref<number | undefined>(undefined);
const isPersonalDoc = ref(false);

// 表单数据
const templateFormData = reactive<{
  variables: Record<string, string>;
  fileName: string;
}>({
  variables: {},
  fileName: '',
});

// 步骤定义
const steps = [
  { title: '选择模板', description: '选择文书模板' },
  { title: '关联项目', description: '选择保存位置' },
  { title: '填写内容', description: '补充文书内容' },
  { title: '预览生成', description: '预览并确认生成' },
];

// 计算属性
const filteredTemplates = computed(() => {
  if (!templateCategory.value) return templates.value;
  return templates.value.filter(t => t.businessType === templateCategory.value);
});

const successSubTitle = computed(() => {
  return isPersonalDoc.value ? '文书已保存到"我的文书"中' : '文书已保存到项目卷宗中';
});

const viewButtonText = computed(() => {
  return isPersonalDoc.value ? '查看我的文书' : '查看项目卷宗';
});

// 方法
async function loadTemplates() {
  loading.value = true;
  try {
    // 使用公共接口，无需 doc:template:list 权限
    const res = await getActiveTemplateList({ pageNum: 1, pageSize: 100 });
    templates.value = res.list || [];
  } catch (error: any) {
    message.error(error.message || '加载模板失败');
  } finally {
    loading.value = false;
  }
}

function formatVariableName(key: string): string {
  return VARIABLE_NAME_MAP[key] || key;
}

function handleSelectTemplate(template: DocumentTemplateDTO) {
  selectedTemplate.value = template;
  templateFormData.variables = {};
  templateFormData.fileName = `${template.name}_${new Date().toLocaleDateString()}`;
  
  if (template.content) {
    const matches = template.content.match(/\{\{(\w+)\}\}|\$\{(\w+(?:\.\w+)*)\}/g);
    if (matches) {
      const uniqueVars = [...new Set(matches.map(m => {
        return m.replace(/\{\{|\}\}|\$\{|\}/g, '');
      }))];
      uniqueVars.forEach(v => {
        templateFormData.variables[v] = '';
      });
    }
  }
}

function handleNext() {
  if (currentStep.value === 0 && !selectedTemplate.value) {
    message.warning('请选择一个模板');
    return;
  }
  if (currentStep.value === 1 && !isPersonalDoc.value && !selectedMatterId.value) {
    message.warning('请选择一个项目，或切换为"个人文书"模式');
    return;
  }
  if (currentStep.value === 2) {
    handlePreview();
  }
  currentStep.value++;
}

function handlePrev() {
  currentStep.value--;
}

async function handlePreview() {
  if (!selectedTemplate.value) return;
  
  templatePreviewLoading.value = true;
  try {
    const data = await previewTemplate({
      templateId: selectedTemplate.value.id,
      matterId: selectedMatterId.value,
      variables: templateFormData.variables,
    });
    templatePreviewContent.value = data.content || data.preview || '预览内容生成中...';
  } catch (error: any) {
    message.error(error.message || '预览失败');
    templatePreviewContent.value = '预览加载失败';
  } finally {
    templatePreviewLoading.value = false;
  }
}

async function handleGenerate() {
  if (!selectedTemplate.value) {
    message.error('请选择模板');
    return;
  }
  
  templateGenerating.value = true;
  try {
    const command: GenerateDocumentCommand = {
      templateId: selectedTemplate.value.id,
      matterId: selectedMatterId.value,
      variables: templateFormData.variables,
      fileName: templateFormData.fileName,
      dossierItemId: selectedDossierId.value,
    };
    
    await generateDocument(command);
    generateSuccess.value = true;
    message.success('文书生成成功！');
    emit('success');
  } catch (error: any) {
    message.error(error.message || '生成失败');
  } finally {
    templateGenerating.value = false;
  }
}

function handlePrint() {
  if (!templatePreviewContent.value) {
    message.warning('暂无内容可打印');
    return;
  }
  
  try {
    const printData: DocumentPrintData = {
      title: selectedTemplate.value?.name || '文书预览',
      content: templatePreviewContent.value,
      documentType: selectedTemplate.value?.name,
      preserveFormat: true,
    };
    printDocument(printData);
  } catch (error: any) {
    message.error(error.message || '打印失败');
  }
}

function handleReset() {
  currentStep.value = 0;
  selectedTemplate.value = null;
  selectedMatterId.value = undefined;
  selectedDossierId.value = undefined;
  isPersonalDoc.value = false;
  templateFormData.variables = {};
  templateFormData.fileName = '';
  templatePreviewContent.value = '';
  generateSuccess.value = false;
  emit('reset');
}

function goToDocuments() {
  if (selectedMatterId.value) {
    router.push(`/matter/detail/${selectedMatterId.value}?tab=dossier`);
  } else {
    router.push('/document/my');
  }
}

// 初始化
loadTemplates();

// 暴露
defineExpose({
  handleReset,
  loadTemplates,
});
</script>

<template>
  <div class="template-mode">
    <Steps :current="currentStep" class="steps-bar">
      <Step v-for="step in steps" :key="step.title" :title="step.title" :description="step.description" />
    </Steps>

    <div class="step-content">
      <!-- 步骤1：选择模板 -->
      <div v-show="currentStep === 0">
        <div class="filter-bar">
          <Space>
            <span>📝 选择文书模板：</span>
            <Select
              v-model:value="templateCategory"
              placeholder="按业务类型筛选"
              style="width: 180px"
              allowClear
              :options="BUSINESS_TYPE_OPTIONS"
            />
          </Space>
        </div>
        
        <Spin :spinning="loading">
          <List
            v-if="filteredTemplates.length > 0"
            :grid="{ gutter: 16, column: 3 }"
            :data-source="filteredTemplates"
          >
            <template #renderItem="{ item }">
              <ListItem>
                <Card
                  hoverable
                  :class="{ 'selected-card': selectedTemplate?.id === item.id }"
                  @click="handleSelectTemplate(item)"
                >
                  <template #title>
                    <Space>
                      <span>{{ item.name }}</span>
                      <Tag v-if="selectedTemplate?.id === item.id" color="blue">已选择</Tag>
                    </Space>
                  </template>
                  <p class="template-desc">
                    {{ item.description || '暂无描述' }}
                  </p>
                  <Space>
                    <Tag>{{ TEMPLATE_TYPE_MAP[item.templateType || ''] || item.templateType }}</Tag>
                    <Tag color="green">使用 {{ item.useCount || 0 }} 次</Tag>
                  </Space>
                </Card>
              </ListItem>
            </template>
          </List>
          <Empty v-else description="暂无可用模板" />
        </Spin>
      </div>

      <!-- 步骤2：选择项目 -->
      <div v-show="currentStep === 1">
        <Row :gutter="24">
          <Col :span="16">
            <MatterSelector
              v-model="selectedMatterId"
              v-model:is-personal-doc="isPersonalDoc"
              v-model:dossier-id="selectedDossierId"
            />
          </Col>
          <Col :span="8">
            <div class="template-summary">
              <p class="summary-label">已选模板</p>
              <strong>{{ selectedTemplate?.name }}</strong>
            </div>
          </Col>
        </Row>
      </div>

      <!-- 步骤3：填写内容 -->
      <div v-show="currentStep === 2">
        <Form layout="vertical">
          <FormItem label="文件名称" required>
            <Input v-model:value="templateFormData.fileName" placeholder="请输入文件名称" style="max-width: 500px" />
          </FormItem>
          
          <Divider>模板变量</Divider>
          
          <template v-if="Object.keys(templateFormData.variables).length > 0">
            <Row :gutter="16">
              <Col v-for="(_value, key) in templateFormData.variables" :key="key" :span="12">
                <FormItem :label="formatVariableName(String(key))">
                  <Input v-model:value="templateFormData.variables[key]" :placeholder="`请输入 ${formatVariableName(String(key))}`" />
                </FormItem>
              </Col>
            </Row>
          </template>
          <Empty v-else description="该模板无需填写变量" />
        </Form>
      </div>

      <!-- 步骤4：预览确认 -->
      <div v-show="currentStep === 3">
        <template v-if="!generateSuccess">
          <Row :gutter="24">
            <Col :span="16">
              <div class="preview-header">
                <h4 style="margin: 0;">📄 文书预览</h4>
                <Button 
                  v-if="templatePreviewContent" 
                  type="primary" 
                  ghost
                  @click="handlePrint"
                >
                  🖨️ 打印
                </Button>
              </div>
              <Spin :spinning="templatePreviewLoading">
                <Card class="preview-card">
                  <pre class="preview-content">{{ templatePreviewContent }}</pre>
                </Card>
              </Spin>
            </Col>
            <Col :span="8">
              <h4 class="info-title">📋 生成信息</h4>
              <Card>
                <p><strong>模板：</strong>{{ selectedTemplate?.name }}</p>
                <p>
                  <strong>保存位置：</strong>
                  <span v-if="isPersonalDoc">我的文书</span>
                  <span v-else-if="selectedMatterId">项目卷宗</span>
                  <span v-else>-</span>
                </p>
                <p><strong>文件名：</strong>{{ templateFormData.fileName }}</p>
              </Card>
            </Col>
          </Row>
        </template>
        
        <Result v-else status="success" title="文书生成成功！" :sub-title="successSubTitle">
          <template #extra>
            <Space>
              <Button type="primary" @click="handleReset">继续制作</Button>
              <Button @click="goToDocuments">{{ viewButtonText }}</Button>
            </Space>
          </template>
        </Result>
      </div>
    </div>

    <Divider />

    <div class="action-bar">
      <Space>
        <Button v-if="currentStep > 0 && !generateSuccess" @click="handlePrev">上一步</Button>
        <Button v-if="currentStep < 3" type="primary" @click="handleNext">下一步</Button>
        <Button v-if="currentStep === 3 && !generateSuccess" type="primary" :loading="templateGenerating" @click="handleGenerate">
          生成文书
        </Button>
      </Space>
    </div>
  </div>
</template>

<style scoped>
.template-mode {
  width: 100%;
}

.steps-bar {
  margin-bottom: 24px;
}

.step-content {
  min-height: 400px;
}

.filter-bar {
  margin-bottom: 16px;
}

.template-desc {
  min-height: 36px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
}

.selected-card {
  border-color: #1890ff;
  box-shadow: 0 0 8px rgb(24 144 255 / 30%);
}

.template-summary {
  padding: 16px;
  margin-top: 24px;
  background: #e6f7ff;
  border-radius: 8px;
}

.summary-label {
  margin: 0 0 8px;
  font-size: 12px;
  color: #666;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.preview-card {
  min-height: 300px;
  background: #fafafa;
}

.preview-content {
  font-family: inherit;
  white-space: pre-wrap;
}

.info-title {
  margin-bottom: 16px;
}

.action-bar {
  text-align: center;
}

:deep(.ant-list-item) {
  padding: 0;
}

:deep(.ant-card-body) {
  padding: 16px;
}
</style>

