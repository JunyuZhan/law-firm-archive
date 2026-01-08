<script setup lang="ts">
/**
 * 文书制作页面
 * 支持两种模式：
 * 1. 模板制作：选择模板 → 选择项目（可选）→ 填写内容 → 预览生成
 * 2. AI 制作：描述需求 → 选择项目（可选）→ AI 生成 → 保存
 * 
 * 文书保存位置：
 * - 关联项目：保存到项目卷宗
 * - 不关联项目：保存到个人文书库（"我的文书"中查看）
 */
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
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
  Textarea,
  Row,
  Col,
  Empty,
  Spin,
  Result,
  Divider,
  Tag,
  List,
  ListItem,
  Alert,
  Switch,
  Radio,
  Checkbox,
  Collapse,
  CollapsePanel,
  Tooltip,
} from 'ant-design-vue';
import { getTemplateList, generateDocument, previewTemplate } from '#/api/document/template';
import { 
  checkAiStatus, 
  aiGenerateDocument, 
  collectSelectiveContext,
  getAvailableDocuments,
  maskWithMapping,
  restoreMaskedText,
  type MatterContextDTO,
  type MaskingMappingDTO,
  type DocumentInfo,
  type CollectOptions,
  type AiModelInfo,
} from '#/api/document/ai';
import { getMyMatters } from '#/api/matter';
import { getMatterDossierItems, type MatterDossierItem } from '#/api/document/dossier';
import type { DocumentTemplateDTO, GenerateDocumentCommand } from '#/api/document/template-types';
import type { MatterDTO } from '#/api/matter/types';

defineOptions({ name: 'DocumentCompose' });

const router = useRouter();

// 制作模式
const composeMode = ref<'template' | 'ai'>('template');

// ========== 通用状态 ==========
const loading = ref(false);
const matters = ref<MatterDTO[]>([]);
const dossierItems = ref<MatterDossierItem[]>([]);
const selectedMatterId = ref<number | undefined>(undefined);
const selectedDossierId = ref<number | undefined>(undefined);
const isPersonalDoc = ref(false);
const generateSuccess = ref(false);

// ========== 模板模式状态 ==========
const currentStep = ref(0);
const templateSteps = [
  { title: '选择模板', description: '选择文书模板' },
  { title: '关联项目', description: '选择保存位置' },
  { title: '填写内容', description: '补充文书内容' },
  { title: '预览生成', description: '预览并确认生成' },
];
const templates = ref<DocumentTemplateDTO[]>([]);
const selectedTemplate = ref<DocumentTemplateDTO | null>(null);
const templateFormData = reactive<{
  variables: Record<string, string>;
  fileName: string;
}>({
  variables: {},
  fileName: '',
});
const templatePreviewContent = ref('');
const templatePreviewLoading = ref(false);
const templateGenerating = ref(false);

// ========== AI 模式状态 ==========
const aiStep = ref(0);
const aiSteps = [
  { title: '描述需求', description: '输入文书需求' },
  { title: '关联项目', description: '选择保存位置' },
  { title: '生成预览', description: 'AI 生成并预览' },
];
const aiStatus = ref<{ available: boolean; model?: string; message?: string; defaultId?: number; models?: AiModelInfo[] }>({ available: false });
const availableModels = ref<AiModelInfo[]>([]); // 可用的 AI 模型列表
const selectedModelId = ref<number | undefined>(undefined); // 用户选中的模型 ID
const aiFormData = reactive({
  documentType: '',
  requirement: '',
  fileName: '',
  additionalContext: '',
  tone: '',
});
const aiGeneratedContent = ref('');
const aiGenerating = ref(false);
const aiSaving = ref(false);

// ========== 项目上下文收集相关状态 ==========
const matterContext = ref<MatterContextDTO | null>(null);
const matterContextMasked = ref<MatterContextDTO | null>(null);
const maskingMapping = ref<MaskingMappingDTO | null>(null); // 脱敏映射，用于还原
const contextCollecting = ref(false);
const contextMasking = ref(false);
const useMatterContext = ref(false);
const enableMasking = ref(true); // 默认开启脱敏
const showContextPreview = ref(false);

// 选择性收集相关状态
const availableDocuments = ref<DocumentInfo[]>([]); // 项目可选文档列表
const selectedDocumentIds = ref<number[]>([]); // 选中的文档ID
const collectOptions = reactive<CollectOptions>({
  includeMatterInfo: true,
  includeClients: true,
  includeParticipants: true,
  includeDocuments: true,
  extractDocumentContent: false, // 是否提取文档内容
  selectedDocumentIds: [],
});

// 脱敏还原相关状态
const aiContentRestored = ref(false); // AI 内容是否已还原
const restoredContent = ref(''); // 还原后的内容
const restoring = ref(false); // 还原中

// 文书类型选项
const documentTypeOptions = [
  { label: '起诉状', value: '起诉状' },
  { label: '答辩状', value: '答辩状' },
  { label: '上诉状', value: '上诉状' },
  { label: '法律意见书', value: '法律意见书' },
  { label: '律师函', value: '律师函' },
  { label: '合同', value: '合同' },
  { label: '授权委托书', value: '授权委托书' },
  { label: '代理词', value: '代理词' },
  { label: '辩护词', value: '辩护词' },
  { label: '其他', value: '其他' },
];

// 语气风格选项
const toneOptions = [
  { label: '正式严谨', value: '正式严谨' },
  { label: '温和友好', value: '温和友好' },
  { label: '强硬有力', value: '强硬有力' },
  { label: '简洁明了', value: '简洁明了' },
];

// 模板类型映射
const templateTypeMap: Record<string, string> = {
  WORD: 'Word文档',
  EXCEL: 'Excel表格',
  PDF: 'PDF文档',
};

// 业务类型选项
const businessTypeOptions = [
  { label: '全部', value: '' },
  { label: '诉讼文书', value: 'LITIGATION' },
  { label: '非诉文书', value: 'NON_LITIGATION' },
  { label: '合同文书', value: 'CONTRACT' },
  { label: '函件文书', value: 'LETTER' },
  { label: '其他', value: 'OTHER' },
];

// 变量名映射
const variableNameMap: Record<string, string> = {
  'matter.name': '项目名称',
  'matter.no': '项目编号',
  'client.name': '客户名称',
  'client.contact': '联系人',
  'client.phone': '联系电话',
  'client.address': '客户地址',
  'user.name': '当前用户',
  'firm.name': '律所名称',
  'firm.address': '律所地址',
  'date': '日期',
  'today': '今日日期',
};

function formatVariableName(key: string): string {
  return variableNameMap[key] || key;
}

// 模板分类过滤
const templateCategory = ref<string>('');
const filteredTemplates = computed(() => {
  if (!templateCategory.value) return templates.value;
  return templates.value.filter(t => t.businessType === templateCategory.value);
});

// 选中的项目
const selectedMatter = computed(() => {
  return matters.value.find(m => m.id === selectedMatterId.value) || null;
});

// 项目选择选项
const matterOptions = computed(() => {
  return matters.value.map(m => ({
    label: `[${m.matterNo}] ${m.name}`,
    value: m.id,
  }));
});

// 卷宗树形选项
const dossierOptions = computed(() => {
  const buildOptions = (parentId: number, level: number = 0): any[] => {
    return dossierItems.value
      .filter(item => item.parentId === parentId)
      .map(item => ({
        label: '　'.repeat(level) + '📁 ' + item.name,
        value: item.id,
        children: buildOptions(item.id, level + 1),
      }));
  };
  return [
    { label: '项目根目录', value: undefined },
    ...buildOptions(0),
  ];
});

// 项目筛选函数
function filterMatterOption(input: string, option: any) {
  return option.label.toLowerCase().includes(input.toLowerCase());
}

// ========== 加载数据 ==========
async function loadTemplates() {
  loading.value = true;
  try {
    const res = await getTemplateList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' });
    templates.value = res.list || [];
  } catch (error: any) {
    message.error(error.message || '加载模板失败');
  } finally {
    loading.value = false;
  }
}

async function loadMatters() {
  try {
    const res = await getMyMatters({ pageNum: 1, pageSize: 100 });
    matters.value = res.list || [];
  } catch (error: any) {
    message.error(error.message || '加载项目失败');
  }
}

async function loadDossierItems(matterId: number) {
  try {
    const items = await getMatterDossierItems(matterId);
    dossierItems.value = (items || []).filter(item => item.itemType === 'FOLDER');
  } catch (error: any) {
    message.error(error.message || '加载卷宗目录失败');
  }
}

async function loadAiStatus() {
  try {
    const status = await checkAiStatus();
    aiStatus.value = status;
    
    // 设置可用模型列表
    if (status.models && status.models.length > 0) {
      availableModels.value = status.models;
      // 默认选中第一个模型
      const firstModel = status.models[0];
      selectedModelId.value = status.defaultId || (firstModel ? firstModel.id : undefined);
    }
  } catch (error: any) {
    aiStatus.value = { available: false, message: '检查 AI 状态失败' };
  }
}

/**
 * 获取当前选中的模型信息
 */
const selectedModel = computed(() => {
  if (!selectedModelId.value || !availableModels.value.length) {
    return null;
  }
  return availableModels.value.find(m => m.id === selectedModelId.value) || null;
});

/**
 * 模型选项（带分组和图标）
 */
const modelOptions = computed(() => {
  return availableModels.value.map(m => {
    const icon = getModelIcon(m.code);
    const tag = getModelTag(m.code);
    return {
      label: m.modelName ? `${icon} ${m.name} (${m.modelName})` : `${icon} ${m.name}`,
      value: m.id,
      tag,
    };
  });
});

/**
 * 获取模型图标
 */
function getModelIcon(code: string): string {
  if (!code) return '🤖';
  if (code.includes('OPENAI')) return '🟢';
  if (code.includes('CLAUDE')) return '🟣';
  if (code.includes('DEEPSEEK_R1')) return '🧠'; // R1 推理模型用大脑图标
  if (code.includes('DEEPSEEK')) return '🔵';
  if (code.includes('QWEN')) return '🟠';
  if (code.includes('ZHIPU')) return '🔴';
  if (code.includes('MOONSHOT')) return '🌙';
  if (code.includes('YI')) return '🟡';
  if (code.includes('MINIMAX')) return '🟤';
  if (code.includes('DIFY')) return '🏠';
  if (code.includes('OLLAMA')) return '🦙';
  if (code.includes('LOCALAI')) return '💻';
  if (code.includes('VLLM')) return '⚡';
  if (code.includes('XINFERENCE')) return '🚀';
  if (code.includes('ONEAPI')) return '🔗';
  if (code.includes('CUSTOM')) return '⚙️';
  return '🤖';
}

/**
 * 获取模型标签（云端/本地）
 */
function getModelTag(code: string | undefined): string {
  if (!code) return '';
  const localModels = ['DIFY', 'OLLAMA', 'LOCALAI', 'VLLM', 'XINFERENCE', 'ONEAPI', 'COMPATIBLE', 'CUSTOM'];
  return localModels.some(m => code.includes(m)) ? '本地' : '云端';
}

/**
 * 获取模型标签颜色
 */
function getModelTagColor(code: string | undefined): string {
  if (!code) return 'default';
  if (code.includes('OPENAI')) return 'green';
  if (code.includes('CLAUDE')) return 'purple';
  if (code.includes('DEEPSEEK')) return 'blue';
  if (code.includes('QWEN')) return 'orange';
  if (code.includes('ZHIPU')) return 'red';
  if (code.includes('MOONSHOT')) return 'geekblue';
  if (code.includes('YI')) return 'gold';
  if (code.includes('MINIMAX')) return 'volcano';
  if (code.includes('DIFY')) return 'cyan';
  if (code.includes('OLLAMA')) return 'lime';
  return 'default';
}

// ========== 事件处理 ==========
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

function handleMatterChange(value: any) {
  const numValue = typeof value === 'number' ? value : undefined;
  selectedMatterId.value = numValue;
  selectedDossierId.value = undefined;
  dossierItems.value = [];
  
  // 清空之前收集的项目上下文
  matterContext.value = null;
  matterContextMasked.value = null;
  maskingMapping.value = null;
  useMatterContext.value = false;
  showContextPreview.value = false;
  availableDocuments.value = [];
  selectedDocumentIds.value = [];
  
  // 重置脱敏还原状态
  aiContentRestored.value = false;
  restoredContent.value = '';
  
  if (numValue) {
    loadDossierItems(numValue);
    loadAvailableDocuments(numValue); // 加载可选文档
  }
}

function handlePersonalDocChange(checked: boolean | string | number) {
  isPersonalDoc.value = Boolean(checked);
  if (checked) {
    selectedMatterId.value = undefined;
    selectedDossierId.value = undefined;
    dossierItems.value = [];
    // 清空项目上下文
    matterContext.value = null;
    matterContextMasked.value = null;
    useMatterContext.value = false;
  }
}

// ========== 项目上下文收集功能 ==========

/**
 * 加载项目可选文档列表
 */
async function loadAvailableDocuments(matterId: number) {
  try {
    const docs = await getAvailableDocuments(matterId);
    availableDocuments.value = docs || [];
    // 默认全选
    selectedDocumentIds.value = docs?.map(d => d.id!).filter(Boolean) || [];
  } catch (error: any) {
    console.error('加载项目文档列表失败', error);
    availableDocuments.value = [];
  }
}

/**
 * 一键收集项目信息（选择性）
 */
async function handleCollectContext() {
  if (!selectedMatterId.value) {
    message.warning('请先选择一个项目');
    return;
  }
  
  contextCollecting.value = true;
  try {
    // 更新选中的文档ID
    collectOptions.selectedDocumentIds = selectedDocumentIds.value;
    
    const context = await collectSelectiveContext(selectedMatterId.value, collectOptions);
    matterContext.value = context;
    useMatterContext.value = true;
    
    const docsCount = context.documents?.length || 0;
    const hasContent = context.documents?.some(d => d.content);
    message.success(`项目信息收集成功！${docsCount > 0 ? `包含 ${docsCount} 个文档` : ''}${hasContent ? '（已提取内容）' : ''}`);
    
    // 如果开启了脱敏，自动脱敏（带映射）
    if (enableMasking.value) {
      await handleMaskContextWithMapping();
    }
  } catch (error: any) {
    message.error(error.message || '项目信息收集失败');
  } finally {
    contextCollecting.value = false;
  }
}

/**
 * 脱敏项目信息（带映射，用于还原）
 */
async function handleMaskContextWithMapping() {
  if (!matterContext.value) {
    message.warning('请先收集项目信息');
    return;
  }
  
  contextMasking.value = true;
  try {
    const result = await maskWithMapping(matterContext.value);
    matterContextMasked.value = result.maskedContext;
    maskingMapping.value = result.mapping;
    
    const mappingCount = result.mapping?.mappings?.length || 0;
    message.success(`数据脱敏完成！${mappingCount > 0 ? `（${mappingCount} 项敏感信息已脱敏）` : ''}`);
  } catch (error: any) {
    message.error(error.message || '数据脱敏失败');
  } finally {
    contextMasking.value = false;
  }
}

/**
 * 脱敏还原
 */
async function handleRestoreContent() {
  if (!aiGeneratedContent.value || !maskingMapping.value) {
    message.warning('没有可还原的内容');
    return;
  }
  
  restoring.value = true;
  try {
    const restored = await restoreMaskedText(aiGeneratedContent.value, maskingMapping.value);
    restoredContent.value = restored;
    aiContentRestored.value = true;
    message.success('脱敏还原完成！');
  } catch (error: any) {
    message.error(error.message || '脱敏还原失败');
  } finally {
    restoring.value = false;
  }
}

/**
 * 切换显示原始/还原内容
 */
function toggleRestoredContent() {
  aiContentRestored.value = !aiContentRestored.value;
}

/**
 * 获取当前显示的内容
 */
const displayedContent = computed(() => {
  if (aiContentRestored.value && restoredContent.value) {
    return restoredContent.value;
  }
  return aiGeneratedContent.value;
});

/**
 * 切换脱敏开关
 */
function handleMaskingChange(checked: boolean | string | number) {
  enableMasking.value = Boolean(checked);
  // 如果已收集上下文且开启脱敏，执行脱敏
  if (enableMasking.value && matterContext.value && !matterContextMasked.value) {
    handleMaskContextWithMapping();
  }
}

/**
 * 切换文档内容提取开关
 */
function handleExtractContentChange(checked: boolean | string | number) {
  collectOptions.extractDocumentContent = Boolean(checked);
}

/**
 * 切换文档选择
 */
function handleDocumentSelect(docId: number, checked: boolean) {
  if (checked) {
    if (!selectedDocumentIds.value.includes(docId)) {
      selectedDocumentIds.value.push(docId);
    }
  } else {
    selectedDocumentIds.value = selectedDocumentIds.value.filter(id => id !== docId);
  }
}

/**
 * 全选/取消全选文档
 */
function handleSelectAllDocuments(checked: boolean) {
  if (checked) {
    selectedDocumentIds.value = availableDocuments.value.map(d => d.id!).filter(Boolean);
  } else {
    selectedDocumentIds.value = [];
  }
}

/**
 * 获取当前使用的上下文（脱敏或原始）
 */
const currentContext = computed(() => {
  if (!useMatterContext.value || !matterContext.value) {
    return null;
  }
  return enableMasking.value && matterContextMasked.value 
    ? matterContextMasked.value 
    : matterContext.value;
});

/**
 * 格式化上下文信息用于显示
 */
function formatContextForPreview(context: MatterContextDTO | null): string {
  if (!context) return '';
  
  const lines: string[] = [];
  
  if (context.matter) {
    lines.push('【项目信息】');
    lines.push(`项目名称：${context.matter.name || '-'}`);
    lines.push(`项目编号：${context.matter.matterNo || '-'}`);
    if (context.matter.matterType) lines.push(`项目类型：${context.matter.matterType}`);
    if (context.matter.caseType) lines.push(`案件类型：${context.matter.caseType}`);
    if (context.matter.description) lines.push(`案情概述：${context.matter.description}`);
    if (context.matter.opposingParty) lines.push(`对方当事人：${context.matter.opposingParty}`);
    lines.push('');
  }
  
  if (context.clients && context.clients.length > 0) {
    lines.push('【当事人信息】');
    context.clients.forEach(client => {
      const primaryLabel = client.isPrimary ? '（主要）' : '';
      lines.push(`${client.role || '委托人'}${primaryLabel}：${client.name || '-'}`);
      if (client.idCard) lines.push(`  身份证号：${client.idCard}`);
      if (client.creditCode) lines.push(`  统一社会信用代码：${client.creditCode}`);
      if (client.contactPhone) lines.push(`  联系电话：${client.contactPhone}`);
      if (client.registeredAddress) lines.push(`  地址：${client.registeredAddress}`);
    });
    lines.push('');
  }
  
  if (context.participants && context.participants.length > 0) {
    lines.push('【代理律师信息】');
    context.participants.forEach(p => {
      lines.push(`${p.role || '律师'}：${p.name || '-'}`);
      if (p.lawyerLicenseNo) lines.push(`  执业证号：${p.lawyerLicenseNo}`);
    });
    lines.push('');
  }
  
  if (context.documents && context.documents.length > 0) {
    lines.push('【相关文档】');
    context.documents.forEach(doc => {
      lines.push(`- ${doc.title || doc.fileName} (${doc.fileType})`);
    });
  }
  
  if (context.masked) {
    lines.push('');
    lines.push('⚠️ 以上信息已进行脱敏处理，带 * 的部分为脱敏内容');
  }
  
  return lines.join('\n');
}

// ========== 模板模式步骤 ==========
function handleTemplateNext() {
  if (currentStep.value === 0 && !selectedTemplate.value) {
    message.warning('请选择一个模板');
    return;
  }
  if (currentStep.value === 1 && !isPersonalDoc.value && !selectedMatterId.value) {
    message.warning('请选择一个项目，或切换为"个人文书"模式');
    return;
  }
  if (currentStep.value === 2) {
    handleTemplatePreview();
  }
  currentStep.value++;
}

function handleTemplatePrev() {
  currentStep.value--;
}

async function handleTemplatePreview() {
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

async function handleTemplateGenerate() {
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
  } catch (error: any) {
    message.error(error.message || '生成失败');
  } finally {
    templateGenerating.value = false;
  }
}

// ========== AI 模式步骤 ==========
function handleAiNext() {
  if (aiStep.value === 0) {
    if (!aiFormData.documentType) {
      message.warning('请选择文书类型');
      return;
    }
    if (!aiFormData.requirement || aiFormData.requirement.length < 10) {
      message.warning('请详细描述您的文书需求（至少10个字）');
      return;
    }
  }
  if (aiStep.value === 1 && !isPersonalDoc.value && !selectedMatterId.value) {
    message.warning('请选择一个项目，或切换为"个人文书"模式');
    return;
  }
  if (aiStep.value === 1) {
    handleAiGenerate();
  }
  aiStep.value++;
}

function handleAiPrev() {
  aiStep.value--;
}

async function handleAiGenerate() {
  if (!aiStatus.value.available) {
    message.error('AI 服务不可用');
    return;
  }
  
  aiGenerating.value = true;
  aiGeneratedContent.value = '';
  // 重置还原状态
  aiContentRestored.value = false;
  restoredContent.value = '';
  
  try {
    const result = await aiGenerateDocument({
      documentType: aiFormData.documentType,
      requirement: aiFormData.requirement,
      matterId: selectedMatterId.value,
      additionalContext: aiFormData.additionalContext,
      tone: aiFormData.tone,
      previewOnly: true,
      // 指定使用的 AI 模型
      aiIntegrationId: selectedModelId.value,
      // 使用收集的项目上下文
      useMatterContext: useMatterContext.value && !!currentContext.value,
      matterContext: currentContext.value || undefined,
      enableMasking: enableMasking.value,
    });
    aiGeneratedContent.value = result.content;
  } catch (error: any) {
    message.error(error.message || 'AI 生成失败');
    aiGeneratedContent.value = '生成失败，请重试';
  } finally {
    aiGenerating.value = false;
  }
}

async function handleAiSave() {
  aiSaving.value = true;
  
  try {
    const result = await aiGenerateDocument({
      documentType: aiFormData.documentType,
      requirement: aiFormData.requirement,
      matterId: selectedMatterId.value,
      dossierItemId: selectedDossierId.value,
      fileName: aiFormData.fileName || `${aiFormData.documentType}_AI生成`,
      additionalContext: aiFormData.additionalContext,
      tone: aiFormData.tone,
      previewOnly: false,
      // 指定使用的 AI 模型
      aiIntegrationId: selectedModelId.value,
      // 使用收集的项目上下文
      useMatterContext: useMatterContext.value && !!currentContext.value,
      matterContext: currentContext.value || undefined,
      enableMasking: enableMasking.value,
    });
    
    if (result.document) {
      generateSuccess.value = true;
      message.success('AI 文书保存成功！');
    }
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    aiSaving.value = false;
  }
}

// ========== 通用操作 ==========
function handleReset() {
  // 重置通用状态
  selectedMatterId.value = undefined;
  selectedDossierId.value = undefined;
  isPersonalDoc.value = false;
  generateSuccess.value = false;
  dossierItems.value = [];
  
  // 重置模板模式
  currentStep.value = 0;
  selectedTemplate.value = null;
  templateFormData.variables = {};
  templateFormData.fileName = '';
  templatePreviewContent.value = '';
  
  // 重置 AI 模式
  aiStep.value = 0;
  aiFormData.documentType = '';
  aiFormData.requirement = '';
  aiFormData.fileName = '';
  aiFormData.additionalContext = '';
  aiFormData.tone = '';
  aiGeneratedContent.value = '';
  // 恢复默认模型选择
  if (aiStatus.value.defaultId) {
    selectedModelId.value = aiStatus.value.defaultId;
  }
  
  // 重置项目上下文相关状态
  matterContext.value = null;
  matterContextMasked.value = null;
  maskingMapping.value = null;
  useMatterContext.value = false;
  enableMasking.value = true;
  showContextPreview.value = false;
  
  // 重置选择性收集状态
  availableDocuments.value = [];
  selectedDocumentIds.value = [];
  collectOptions.includeMatterInfo = true;
  collectOptions.includeClients = true;
  collectOptions.includeParticipants = true;
  collectOptions.includeDocuments = true;
  collectOptions.extractDocumentContent = false;
  collectOptions.selectedDocumentIds = [];
  
  // 重置脱敏还原状态
  aiContentRestored.value = false;
  restoredContent.value = '';
}

function goToDocuments() {
  if (selectedMatterId.value) {
    router.push(`/matter/detail/${selectedMatterId.value}?tab=dossier`);
  } else {
    router.push('/document/my');
  }
}

const successSubTitle = computed(() => {
  return isPersonalDoc.value ? '文书已保存到"我的文书"中' : '文书已保存到项目卷宗中';
});

const viewButtonText = computed(() => {
  return isPersonalDoc.value ? '查看我的文书' : '查看项目卷宗';
});

function handleModeChange() {
  // 切换模式时重置状态
  handleReset();
}

onMounted(() => {
  loadTemplates();
  loadMatters();
  loadAiStatus();
});
</script>

<template>
  <Page title="文书制作" description="使用模板或 AI 快速制作法律文书">
    <Card>
      <!-- 模式选择 -->
      <div style="margin-bottom: 24px; text-align: center;">
        <Radio.Group v-model:value="composeMode" button-style="solid" size="large" @change="handleModeChange">
          <Radio.Button value="template">
            📄 模板制作
          </Radio.Button>
          <Radio.Button value="ai">
            🤖 AI 智能生成
          </Radio.Button>
        </Radio.Group>
      </div>

      <!-- AI 不可用提示 -->
      <Alert 
        v-if="composeMode === 'ai' && !aiStatus.available" 
        type="warning" 
        show-icon
        style="margin-bottom: 16px"
      >
        <template #message>
          AI 服务未配置
        </template>
        <template #description>
          {{ aiStatus.message || '请在「系统管理 → 外部集成」中配置并启用 AI 大模型' }}
        </template>
      </Alert>

      <!-- AI 可用提示 -->
      <Alert 
        v-if="composeMode === 'ai' && aiStatus.available" 
        type="success" 
        show-icon
        style="margin-bottom: 16px"
      >
        <template #message>
          <Space>
            ⚡ AI 服务已就绪
            <Tag color="blue">{{ aiStatus.model }}</Tag>
          </Space>
        </template>
      </Alert>

      <Divider />

      <!-- ========== 模板模式 ========== -->
      <template v-if="composeMode === 'template'">
        <Steps :current="currentStep" style="margin-bottom: 24px">
          <Step v-for="step in templateSteps" :key="step.title" :title="step.title" :description="step.description" />
        </Steps>

        <div style="min-height: 400px">
          <!-- 步骤1：选择模板 -->
          <div v-show="currentStep === 0">
            <div style="margin-bottom: 16px">
              <Space>
                <span>📝 选择文书模板：</span>
                <Select
                  v-model:value="templateCategory"
                  placeholder="按业务类型筛选"
                  style="width: 180px"
                  allowClear
                  :options="businessTypeOptions"
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
                      <p style="color: #666; margin-bottom: 8px; font-size: 12px; min-height: 36px;">
                        {{ item.description || '暂无描述' }}
                      </p>
                      <Space>
                        <Tag>{{ templateTypeMap[item.templateType || ''] || item.templateType }}</Tag>
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
                <div style="margin-bottom: 24px">
                  <Space align="center">
                    <span>个人文书（不关联项目）：</span>
                    <Switch :checked="isPersonalDoc" @change="handlePersonalDocChange" />
                  </Space>
                  <p style="color: #999; margin-top: 8px; font-size: 12px;">
                    开启后，文书将保存到"我的文书"中，不会关联到任何项目
                  </p>
                </div>

                <template v-if="!isPersonalDoc">
                  <h4 style="margin-bottom: 16px">📁 选择关联项目</h4>
                  <Select
                    v-model:value="selectedMatterId"
                    placeholder="请选择项目"
                    style="width: 100%"
                    allowClear
                    show-search
                    :filter-option="filterMatterOption"
                    :options="matterOptions"
                    @change="handleMatterChange"
                  />
                  
                  <div v-if="selectedMatter" style="margin-top: 16px; padding: 16px; background: #f5f5f5; border-radius: 8px;">
                    <p style="margin: 0 0 8px 0;"><strong>{{ selectedMatter.name }}</strong></p>
                    <p style="margin: 0; font-size: 12px; color: #666;">
                      编号：{{ selectedMatter.matterNo }} | 
                      客户：{{ selectedMatter.clientName || '-' }} | 
                      类型：{{ selectedMatter.matterTypeName || selectedMatter.matterType || '-' }}
                    </p>
                  </div>
                </template>
                
                <Alert 
                  v-else 
                  type="info" 
                  message="个人文书模式" 
                  description="文书将保存到【我的文书】列表中"
                  show-icon
                  style="margin-top: 24px"
                />
              </Col>
              <Col :span="8">
                <template v-if="selectedMatterId && !isPersonalDoc">
                  <h4 style="margin-bottom: 16px">📂 保存到卷宗目录</h4>
                  <Select
                    v-model:value="selectedDossierId"
                    placeholder="选择卷宗目录（可选）"
                    style="width: 100%"
                    allowClear
                    :options="dossierOptions"
                  />
                  <p style="color: #999; margin-top: 8px; font-size: 12px">不选择则保存到项目根目录</p>
                </template>
                
                <div style="margin-top: 24px; padding: 16px; background: #e6f7ff; border-radius: 8px;">
                  <p style="margin: 0 0 8px 0; font-size: 12px; color: #666;">已选模板</p>
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
                  <h4 style="margin-bottom: 16px">📄 文书预览</h4>
                  <Spin :spinning="templatePreviewLoading">
                    <Card style="min-height: 300px; background: #fafafa">
                      <pre style="white-space: pre-wrap; font-family: inherit">{{ templatePreviewContent }}</pre>
                    </Card>
                  </Spin>
                </Col>
                <Col :span="8">
                  <h4 style="margin-bottom: 16px">📋 生成信息</h4>
                  <Card>
                    <p><strong>模板：</strong>{{ selectedTemplate?.name }}</p>
                    <p>
                      <strong>保存位置：</strong>
                      <span v-if="isPersonalDoc">我的文书</span>
                      <span v-else-if="selectedMatter">{{ selectedMatter.name }}</span>
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

        <div style="text-align: center">
          <Space>
            <Button v-if="currentStep > 0 && !generateSuccess" @click="handleTemplatePrev">上一步</Button>
            <Button v-if="currentStep < 3" type="primary" @click="handleTemplateNext">下一步</Button>
            <Button v-if="currentStep === 3 && !generateSuccess" type="primary" :loading="templateGenerating" @click="handleTemplateGenerate">
              生成文书
            </Button>
          </Space>
        </div>
      </template>

      <!-- ========== AI 模式 ========== -->
      <template v-if="composeMode === 'ai'">
        <Steps :current="aiStep" style="margin-bottom: 24px">
          <Step v-for="step in aiSteps" :key="step.title" :title="step.title" :description="step.description" />
        </Steps>

        <div style="min-height: 400px">
          <!-- 步骤1：描述需求 -->
          <div v-show="aiStep === 0">
            <Form layout="vertical">
              <!-- AI 模型选择 -->
              <FormItem label="选择 AI 大模型">
                <Row :gutter="16" align="middle">
                  <Col :span="16">
                    <Select
                      v-model:value="selectedModelId"
                      placeholder="请选择 AI 大模型"
                      :options="modelOptions"
                      style="width: 100%"
                      :disabled="!aiStatus.available || availableModels.length <= 1"
                      optionFilterProp="label"
                      showSearch
                    >
                      <template #suffixIcon>
                        <span>🤖</span>
                      </template>
                    </Select>
                  </Col>
                  <Col :span="8">
                    <Space>
                      <Tag v-if="selectedModel" :color="getModelTagColor(selectedModel.code)">
                        {{ selectedModel.modelName || selectedModel.code }}
                      </Tag>
                      <Tag v-if="selectedModel" :color="getModelTag(selectedModel.code) === '本地' ? 'green' : 'blue'">
                        {{ getModelTag(selectedModel.code) }}
                      </Tag>
                    </Space>
                  </Col>
                </Row>
                <div v-if="availableModels.length > 1" style="margin-top: 8px;">
                  <Space>
                    <Tag color="cyan">{{ availableModels.length }} 个可用</Tag>
                    <span style="color: #666; font-size: 12px;">
                      💡 可选择云端或本地部署的 AI 模型
                    </span>
                  </Space>
                </div>
                <Alert 
                  v-if="selectedModel && getModelTag(selectedModel.code) === '本地'" 
                  type="info" 
                  style="margin-top: 8px;"
                >
                  <template #message>
                    🏠 本地模型：数据不会上传到云端，适合处理敏感信息
                  </template>
                </Alert>
              </FormItem>
              
              <Row :gutter="24">
                <Col :span="12">
                  <FormItem label="文书类型" required>
                    <Select
                      v-model:value="aiFormData.documentType"
                      placeholder="请选择文书类型"
                      :options="documentTypeOptions"
                      style="width: 100%"
                    />
                  </FormItem>
                </Col>
                <Col :span="12">
                  <FormItem label="语气风格">
                    <Select
                      v-model:value="aiFormData.tone"
                      placeholder="请选择语气风格（可选）"
                      :options="toneOptions"
                      allowClear
                      style="width: 100%"
                    />
                  </FormItem>
                </Col>
              </Row>

              <FormItem label="文书需求描述" required>
                <Textarea
                  v-model:value="aiFormData.requirement"
                  placeholder="请详细描述您的文书需求，例如：&#10;- 案件背景和当事人信息&#10;- 主要诉求或目的&#10;- 需要包含的关键内容&#10;- 其他特殊要求"
                  :rows="6"
                  show-count
                  :maxlength="2000"
                />
              </FormItem>

              <FormItem label="补充信息（可选）">
                <Textarea
                  v-model:value="aiFormData.additionalContext"
                  placeholder="提供更多背景信息，如：&#10;- 相关法律条文&#10;- 证据情况&#10;- 对方情况等"
                  :rows="4"
                  :maxlength="1000"
                />
              </FormItem>

              <FormItem label="文件名称">
                <Input v-model:value="aiFormData.fileName" placeholder="留空将自动生成" style="max-width: 400px" />
              </FormItem>
            </Form>
          </div>

          <!-- 步骤2：选择项目 & 收集信息 -->
          <div v-show="aiStep === 1">
            <Row :gutter="24">
              <Col :span="12">
                <div style="margin-bottom: 24px">
                  <Space align="center">
                    <span>个人文书（不关联项目）：</span>
                    <Switch :checked="isPersonalDoc" @change="handlePersonalDocChange" />
                  </Space>
                  <p style="color: #999; margin-top: 8px; font-size: 12px;">
                    开启后，文书将保存到"我的文书"中，不会关联到任何项目
                  </p>
                </div>

                <template v-if="!isPersonalDoc">
                  <h4 style="margin-bottom: 16px">📁 选择关联项目</h4>
                  <Select
                    v-model:value="selectedMatterId"
                    placeholder="请选择项目"
                    style="width: 100%"
                    allowClear
                    show-search
                    :filter-option="filterMatterOption"
                    :options="matterOptions"
                    @change="handleMatterChange"
                  />
                  
                  <div v-if="selectedMatter" style="margin-top: 16px; padding: 16px; background: #f5f5f5; border-radius: 8px;">
                    <p style="margin: 0 0 8px 0;"><strong>{{ selectedMatter.name }}</strong></p>
                    <p style="margin: 0; font-size: 12px; color: #666;">
                      编号：{{ selectedMatter.matterNo }} | 
                      客户：{{ selectedMatter.clientName || '-' }}
                    </p>
                  </div>

                  <!-- 一键收集项目信息 -->
                  <div v-if="selectedMatterId" style="margin-top: 16px;">
                    <Divider style="margin: 16px 0;" />
                    <h4 style="margin-bottom: 12px">📋 收集项目信息</h4>
                    <p style="color: #666; font-size: 12px; margin-bottom: 12px;">
                      选择要收集的信息类型和文档，提供给 AI 生成更精准的文书
                    </p>
                    
                    <!-- 选择性收集选项 -->
                    <Collapse ghost style="margin-bottom: 16px;">
                      <CollapsePanel key="options" header="📌 选择收集内容">
                        <div style="padding: 8px 0;">
                          <p style="margin: 0 0 12px 0; font-weight: 500;">信息类型：</p>
                          <Space direction="vertical" style="width: 100%;">
                            <Checkbox v-model:checked="collectOptions.includeMatterInfo">项目基本信息</Checkbox>
                            <Checkbox v-model:checked="collectOptions.includeClients">客户/当事人信息</Checkbox>
                            <Checkbox v-model:checked="collectOptions.includeParticipants">参与律师信息</Checkbox>
                            <Checkbox v-model:checked="collectOptions.includeDocuments">相关文档</Checkbox>
                          </Space>
                          
                          <!-- 文档内容提取开关 -->
                          <div v-if="collectOptions.includeDocuments" style="margin-top: 16px; padding: 12px; background: #e6f7ff; border-radius: 8px;">
                            <Space align="center">
                              <span style="font-weight: 500;">📄 提取文档内容：</span>
                              <Switch :checked="collectOptions.extractDocumentContent" @change="handleExtractContentChange" />
                              <Tooltip title="提取 Word、PDF、图片等文档的文本内容，供 AI 参考">
                                <Tag color="blue">?</Tag>
                              </Tooltip>
                            </Space>
                            <p style="color: #096dd9; font-size: 12px; margin: 8px 0 0 0;">
                              {{ collectOptions.extractDocumentContent ? '将提取 Word、PDF、图片（OCR）的文本内容' : '仅收集文档元信息（标题、类型）' }}
                            </p>
                          </div>
                          
                          <!-- 文档选择列表 -->
                          <div v-if="collectOptions.includeDocuments && availableDocuments.length > 0" style="margin-top: 16px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                              <span style="font-weight: 500;">选择文档（{{ selectedDocumentIds.length }}/{{ availableDocuments.length }}）：</span>
                              <Space>
                                <Button size="small" @click="handleSelectAllDocuments(true)">全选</Button>
                                <Button size="small" @click="handleSelectAllDocuments(false)">取消全选</Button>
                              </Space>
                            </div>
                            <div style="max-height: 200px; overflow-y: auto; border: 1px solid #d9d9d9; border-radius: 8px; padding: 8px;">
                              <div v-for="doc in availableDocuments" :key="doc.id" style="padding: 4px 0;">
                                <Checkbox 
                                  :checked="selectedDocumentIds.includes(doc.id!)" 
                                  @change="(e: any) => handleDocumentSelect(doc.id!, e.target.checked)"
                                >
                                  <Space>
                                    <span>{{ doc.title || doc.fileName }}</span>
                                    <Tag size="small">{{ doc.fileType }}</Tag>
                                  </Space>
                                </Checkbox>
                              </div>
                            </div>
                          </div>
                          <Alert 
                            v-else-if="collectOptions.includeDocuments" 
                            type="info" 
                            message="该项目暂无文档" 
                            show-icon
                            style="margin-top: 12px"
                          />
                        </div>
                      </CollapsePanel>
                    </Collapse>
                    
                    <Space>
                      <Button 
                        type="primary" 
                        :loading="contextCollecting"
                        @click="handleCollectContext"
                      >
                        🔍 收集选中信息
                      </Button>
                      <Tag v-if="useMatterContext && matterContext" color="success">
                        ✓ 已收集
                      </Tag>
                    </Space>
                    
                    <!-- 脱敏开关 -->
                    <div v-if="useMatterContext && matterContext" style="margin-top: 16px; padding: 12px; background: #fff7e6; border-radius: 8px; border: 1px solid #ffd591;">
                      <Space align="center">
                        <span style="font-weight: 500;">🔒 数据脱敏：</span>
                        <Switch :checked="enableMasking" @change="handleMaskingChange" />
                        <Tag v-if="enableMasking && matterContextMasked" color="blue">已脱敏</Tag>
                        <Tag v-else-if="!enableMasking" color="orange">未脱敏</Tag>
                      </Space>
                      <p style="color: #ad6800; font-size: 12px; margin: 8px 0 0 0;">
                        {{ enableMasking ? '敏感信息将被脱敏后发送给AI（推荐用于云端大模型）' : '原始信息将直接发送给AI（适用于本地部署大模型）' }}
                      </p>
                      <p v-if="maskingMapping && maskingMapping.mappings?.length" style="color: #52c41a; font-size: 12px; margin: 4px 0 0 0;">
                        ✓ 已记录 {{ maskingMapping.mappings.length }} 项脱敏映射，生成后可还原
                      </p>
                    </div>
                  </div>
                </template>
                
                <Alert 
                  v-else 
                  type="info" 
                  message="个人文书模式" 
                  description="文书将保存到【我的文书】列表中"
                  show-icon
                  style="margin-top: 24px"
                />
              </Col>
              <Col :span="12">
                <!-- 卷宗目录选择 -->
                <template v-if="selectedMatterId && !isPersonalDoc">
                  <h4 style="margin-bottom: 16px">📂 保存到卷宗目录</h4>
                  <Select
                    v-model:value="selectedDossierId"
                    placeholder="选择卷宗目录（可选）"
                    style="width: 100%"
                    allowClear
                    :options="dossierOptions"
                  />
                  <p style="color: #999; margin-top: 8px; font-size: 12px">不选择则保存到项目根目录</p>
                </template>
                
                <!-- 收集的信息预览 -->
                <div v-if="useMatterContext && currentContext" style="margin-top: 24px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <h4 style="margin: 0;">👁️ 收集的信息预览</h4>
                    <Button 
                      type="link" 
                      size="small"
                      @click="showContextPreview = !showContextPreview"
                    >
                      {{ showContextPreview ? '收起' : '展开' }}
                    </Button>
                  </div>
                  <Card 
                    v-show="showContextPreview"
                    size="small" 
                    style="max-height: 350px; overflow-y: auto; background: #fafafa;"
                  >
                    <pre style="white-space: pre-wrap; font-size: 12px; font-family: inherit; margin: 0; line-height: 1.6;">{{ formatContextForPreview(currentContext) }}</pre>
                  </Card>
                  <Alert 
                    v-if="enableMasking && matterContextMasked?.masked"
                    type="success" 
                    style="margin-top: 12px;"
                  >
                    <template #message>
                      <span>✓ 以上信息已脱敏，带 * 的内容为脱敏后的数据</span>
                    </template>
                  </Alert>
                </div>
                
                <!-- 文书信息摘要 -->
                <div style="margin-top: 24px; padding: 16px; background: #f6ffed; border-radius: 8px;">
                  <p style="margin: 0 0 8px 0; font-size: 12px; color: #666;">文书信息</p>
                  <p style="margin: 0;"><strong>类型：</strong>{{ aiFormData.documentType }}</p>
                  <p style="margin: 8px 0 0 0; font-size: 12px; color: #999;">{{ aiFormData.requirement.substring(0, 50) }}...</p>
                  <p v-if="useMatterContext" style="margin: 8px 0 0 0;">
                    <Tag color="blue">📋 包含项目信息</Tag>
                    <Tag v-if="enableMasking" color="green">🔒 已脱敏</Tag>
                  </p>
                </div>
              </Col>
            </Row>
          </div>

          <!-- 步骤3：AI 生成预览 -->
          <div v-show="aiStep === 2">
            <template v-if="!generateSuccess">
              <Row :gutter="24">
                <Col :span="16">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                    <h4 style="margin: 0;">
                      🤖 AI 生成内容
                      <Tag v-if="aiContentRestored" color="green" style="margin-left: 8px;">已还原</Tag>
                      <Tag v-else-if="enableMasking && maskingMapping?.mappings?.length" color="blue" style="margin-left: 8px;">脱敏中</Tag>
                    </h4>
                    <Space>
                      <!-- 脱敏还原按钮 -->
                      <Button 
                        v-if="enableMasking && maskingMapping?.mappings?.length && aiGeneratedContent && !aiContentRestored"
                        type="primary"
                        ghost
                        :loading="restoring"
                        @click="handleRestoreContent"
                      >
                        🔓 脱敏还原
                      </Button>
                      <Button 
                        v-if="aiContentRestored"
                        type="link"
                        @click="toggleRestoredContent"
                      >
                        {{ aiContentRestored ? '查看原始' : '查看还原' }}
                      </Button>
                      <Button type="link" :loading="aiGenerating" @click="handleAiGenerate">
                        ⚡ 重新生成
                      </Button>
                    </Space>
                  </div>
                  
                  <!-- 脱敏还原提示 -->
                  <Alert 
                    v-if="enableMasking && maskingMapping?.mappings?.length && aiGeneratedContent && !aiContentRestored"
                    type="info"
                    style="margin-bottom: 16px;"
                  >
                    <template #message>
                      <Space>
                        <span>💡 当前显示的是脱敏后的内容，点击"脱敏还原"可恢复真实信息</span>
                        <Tag color="blue">{{ maskingMapping.mappings.length }} 处待还原</Tag>
                      </Space>
                    </template>
                  </Alert>
                  
                  <Spin :spinning="aiGenerating" tip="AI 正在生成文书，请稍候...">
                    <Card style="min-height: 400px; background: #fafafa">
                      <pre style="white-space: pre-wrap; font-family: inherit; line-height: 1.8;">{{ displayedContent || '正在生成...' }}</pre>
                    </Card>
                  </Spin>
                </Col>
                <Col :span="8">
                  <h4 style="margin-bottom: 16px">📋 保存信息</h4>
                  <Card>
                    <p><strong>文书类型：</strong>{{ aiFormData.documentType }}</p>
                    <p><strong>语气风格：</strong>{{ aiFormData.tone || '默认' }}</p>
                    <p>
                      <strong>保存位置：</strong>
                      <span v-if="isPersonalDoc">我的文书</span>
                      <span v-else-if="selectedMatter">{{ selectedMatter.name }}</span>
                      <span v-else>-</span>
                    </p>
                    <p><strong>文件名：</strong>{{ aiFormData.fileName || '自动生成' }}</p>
                    <Divider />
                    <p style="color: #999; font-size: 12px;">
                      🤖 使用 {{ selectedModel?.name || aiStatus.model }} 
                      <Tag v-if="selectedModel?.modelName" size="small">{{ selectedModel.modelName }}</Tag>
                    </p>
                    
                    <!-- 脱敏状态显示 -->
                    <div v-if="useMatterContext" style="margin-top: 12px; padding: 8px; background: #f6ffed; border-radius: 4px;">
                      <Tag color="blue">📋 使用项目信息</Tag>
                      <Tag v-if="enableMasking" :color="aiContentRestored ? 'green' : 'orange'">
                        {{ aiContentRestored ? '🔓 已还原' : '🔒 脱敏中' }}
                      </Tag>
                    </div>
                  </Card>
                  
                  <!-- 脱敏映射详情 -->
                  <Card v-if="maskingMapping?.mappings?.length" style="margin-top: 16px;" size="small">
                    <template #title>
                      <span style="font-size: 12px;">🔐 脱敏映射（{{ maskingMapping.mappings.length }} 项）</span>
                    </template>
                    <div style="max-height: 150px; overflow-y: auto; font-size: 12px;">
                      <div v-for="(m, i) in maskingMapping.mappings.slice(0, 10)" :key="i" style="padding: 4px 0; border-bottom: 1px solid #f0f0f0;">
                        <span style="color: #999;">{{ m.fieldName }}：</span>
                        <span style="color: #ff4d4f; text-decoration: line-through;">{{ m.maskedValue }}</span>
                        <span style="color: #52c41a; margin-left: 4px;">→ {{ m.originalValue }}</span>
                      </div>
                      <p v-if="maskingMapping.mappings.length > 10" style="color: #999; margin: 8px 0 0 0;">
                        ...还有 {{ maskingMapping.mappings.length - 10 }} 项
                      </p>
                    </div>
                  </Card>
                </Col>
              </Row>
            </template>
            
            <Result v-else status="success" title="AI 文书保存成功！" :sub-title="successSubTitle">
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

        <div style="text-align: center">
          <Space>
            <Button v-if="aiStep > 0 && !generateSuccess" @click="handleAiPrev">上一步</Button>
            <Button 
              v-if="aiStep < 2" 
              type="primary" 
              :disabled="!aiStatus.available"
              @click="handleAiNext"
            >
              下一步
            </Button>
            <Button 
              v-if="aiStep === 2 && !generateSuccess && aiGeneratedContent" 
              type="primary" 
              :loading="aiSaving"
              @click="handleAiSave"
            >
              💾 保存文书
            </Button>
          </Space>
        </div>
      </template>
    </Card>
  </Page>
</template>

<style scoped>
.selected-card {
  border-color: #1890ff;
  box-shadow: 0 0 8px rgba(24, 144, 255, 0.3);
}

:deep(.ant-list-item) {
  padding: 0;
}

:deep(.ant-card-body) {
  padding: 16px;
}

:deep(.ant-radio-button-wrapper) {
  padding: 0 24px;
}
</style>
