<script setup lang="ts">
import type { DocumentPrintData } from '@vben/utils';

import type { AiStatus } from '../types';

import type {
  AiModelInfo,
  MaskingMappingDTO,
  MatterContextDTO,
} from '#/api/document/ai';
import type { OcrResultDTO } from '#/api/ocr';

/**
 * AI 智能生成模式组件
 * 使用 AI 生成文书：描述需求 → 选择项目 → AI 生成 → 保存
 */
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { IconifyIcon } from '@vben/icons';
import { printDocument } from '@vben/utils';

import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Form,
  FormItem,
  Input,
  message,
  Progress,
  Result,
  Row,
  Select,
  Space,
  Step,
  Steps,
  Tag,
  Textarea,
  Tooltip,
  Upload,
} from 'ant-design-vue';

import {
  aiGenerateDocument,
  checkAiStatus,
  restoreMaskedText,
} from '#/api/document/ai';
import { recognizeGeneral } from '#/api/ocr';

import { DOCUMENT_TYPE_OPTIONS, TONE_OPTIONS } from '../types';
import ContextCollector from './ContextCollector.vue';
import MatterSelector from './MatterSelector.vue';

defineOptions({ name: 'AiMode' });

const emit = defineEmits<{
  (e: 'success'): void;
  (e: 'reset'): void;
}>();

const router = useRouter();

// AI 状态
const aiStatus = ref<AiStatus>({ available: false });
const availableModels = ref<AiModelInfo[]>([]);
const selectedModelId = ref<number | undefined>(undefined);

// 步骤状态
const aiStep = ref(0);
const aiGenerating = ref(false);
const aiSaving = ref(false);
const ocrLoading = ref(false);
const generateSuccess = ref(false);

// 项目选择状态
const selectedMatterId = ref<number | undefined>(undefined);
const selectedDossierId = ref<number | undefined>(undefined);
const isPersonalDoc = ref(false);

// 上下文状态
const matterContext = ref<MatterContextDTO | null>(null);
const matterContextMasked = ref<MatterContextDTO | null>(null);
const maskingMapping = ref<MaskingMappingDTO | null>(null);
const useMatterContext = ref(false);
const enableMasking = ref(true);

// AI 生成结果
const aiGeneratedContent = ref('');
const aiContentRestored = ref(false);
const restoredContent = ref('');
const restoring = ref(false);

// 生成进度
const generateProgress = ref(0);
const generateStage = ref('');

// 迭代优化
const showRevisionPanel = ref(false);
const revisionRequest = ref('');
const revisionMode = ref<'refine' | 'regenerate'>('refine'); // regenerate: 重新生成, refine: 在此基础上修改
const generateStages = [
  { percent: 10, text: '📤 发送请求到 AI 服务...' },
  { percent: 25, text: '🧠 AI 正在分析需求...' },
  { percent: 45, text: '📝 AI 正在构思文书结构...' },
  { percent: 65, text: '✍️ AI 正在撰写内容...' },
  { percent: 85, text: '🔍 AI 正在润色文稿...' },
  { percent: 95, text: '📦 正在处理响应...' },
];
let progressTimer: null | ReturnType<typeof setInterval> = null;

// 组件引用
const matterSelectorRef = ref<InstanceType<typeof MatterSelector>>();
// contextCollectorRef 保留用于后续功能扩展

// 表单数据
const aiFormData = reactive({
  documentType: '',
  requirement: '',
  fileName: '',
  additionalContext: '',
  tone: '',
});

// 自定义语气风格
const customTones = ref<string[]>([]);
const customToneInput = ref('');

// 语气风格选项（预设 + 自定义）
const toneOptions = computed(() => {
  const baseOptions = TONE_OPTIONS.map((opt) => ({ ...opt }));
  const customOptions = customTones.value.map((tone) => ({
    label: `⭐ ${tone}`,
    value: tone,
  }));
  return [...baseOptions, ...customOptions];
});

// 语气风格过滤函数
function filterToneOption(inputValue: string, option: any) {
  customToneInput.value = inputValue;
  if (!inputValue) return true;
  const label = option.label || option.value || '';
  return label.toLowerCase().includes(inputValue.toLowerCase());
}

// 添加自定义语气
function handleAddCustomTone() {
  if (
    customToneInput.value &&
    !customTones.value.includes(customToneInput.value)
  ) {
    customTones.value.push(customToneInput.value);
    aiFormData.tone = customToneInput.value;
    customToneInput.value = '';
  }
}

// 步骤定义
const steps = [
  { title: '描述需求', description: '输入文书需求' },
  { title: '关联项目', description: '选择保存位置' },
  { title: '生成预览', description: 'AI 生成并预览' },
];

// 计算属性
const selectedModel = computed(() => {
  if (!selectedModelId.value || availableModels.value.length === 0) {
    return null;
  }
  return (
    availableModels.value.find((m) => m.id === selectedModelId.value) || null
  );
});

const modelOptions = computed(() => {
  return availableModels.value.map((m) => {
    const icon = getModelIcon(m.code);
    return {
      label: m.modelName
        ? `${icon} ${m.name} (${m.modelName})`
        : `${icon} ${m.name}`,
      value: m.id,
    };
  });
});

const currentContext = computed(() => {
  if (!useMatterContext.value || !matterContext.value) {
    return null;
  }
  return enableMasking.value && matterContextMasked.value
    ? matterContextMasked.value
    : matterContext.value;
});

const displayedContent = computed(() => {
  if (aiContentRestored.value && restoredContent.value) {
    return restoredContent.value;
  }
  return aiGeneratedContent.value;
});

const successSubTitle = computed(() => {
  return isPersonalDoc.value
    ? '文书已保存到"我的文书"中'
    : '文书已保存到项目卷宗中';
});

const viewButtonText = computed(() => {
  return isPersonalDoc.value ? '查看我的文书' : '查看项目卷宗';
});

// 方法
async function loadAiStatus() {
  try {
    const status = await checkAiStatus();
    aiStatus.value = status;

    if (status.models && status.models.length > 0) {
      availableModels.value = status.models;
      const firstModel = status.models[0];
      selectedModelId.value =
        status.defaultId || (firstModel ? firstModel.id : undefined);
    }
  } catch {
    aiStatus.value = { available: false, message: '检查 AI 状态失败' };
  }
}

function getModelIcon(code: string): string {
  if (!code) return '◆';
  if (code.includes('OPENAI')) return '●'; // OpenAI - 绿点
  if (code.includes('CLAUDE')) return '◆'; // Claude - 紫菱形
  if (code.includes('DEEPSEEK_R1')) return '◈'; // DeepSeek R1 - 特殊
  if (code.includes('DEEPSEEK')) return '◇'; // DeepSeek - 蓝菱形
  if (code.includes('QWEN')) return '◎'; // 通义千问 - 同心圆
  if (code.includes('ZHIPU')) return '◉'; // 智谱 - 实心圆
  if (code.includes('MOONSHOT')) return '☽'; // Moonshot - 月亮
  if (code.includes('YI')) return '◐'; // 零一万物 - 半圆
  if (code.includes('MINIMAX')) return '▣'; // MiniMax
  if (code.includes('DIFY')) return '⌂'; // Dify - 房子
  if (code.includes('OLLAMA')) return '◭'; // Ollama
  if (code.includes('LOCALAI')) return '⊞'; // LocalAI - 本地
  if (code.includes('VLLM')) return '⚡'; // vLLM - 闪电
  if (code.includes('XINFERENCE')) return '▸'; // Xinference
  if (code.includes('ONEAPI')) return '⊕'; // OneAPI
  if (code.includes('CUSTOM')) return '⚙'; // 自定义
  return '◆';
}

function getModelTag(code: string | undefined): string {
  if (!code) return '';
  const localModels = [
    'DIFY',
    'OLLAMA',
    'LOCALAI',
    'VLLM',
    'XINFERENCE',
    'ONEAPI',
    'COMPATIBLE',
    'CUSTOM',
  ];
  return localModels.some((m) => code.includes(m)) ? '本地' : '云端';
}

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

async function handleOcrReference(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      const existingText = aiFormData.additionalContext || '';
      const separator = existingText ? '\n\n---OCR识别内容---\n' : '';
      aiFormData.additionalContext = existingText + separator + result.rawText;
      message.success('OCR识别成功，已添加到补充信息中');
    } else {
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (error: any) {
    message.error(error?.message || 'OCR识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false;
}

function handleNext() {
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
    handleGenerate();
  }
  aiStep.value++;
}

function handlePrev() {
  aiStep.value--;
}

// 开始模拟进度
function startProgress() {
  generateProgress.value = 0;
  generateStage.value = generateStages[0]?.text || '准备中...';
  let stageIndex = 0;

  progressTimer = setInterval(() => {
    if (stageIndex < generateStages.length - 1) {
      stageIndex++;
      const stage = generateStages[stageIndex];
      if (stage) {
        generateProgress.value = stage.percent;
        generateStage.value = stage.text;
      }
    }
  }, 2000); // 每 2 秒更新一个阶段
}

// 停止进度
function stopProgress(success: boolean) {
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
  if (success) {
    generateProgress.value = 100;
    generateStage.value = '✅ 生成完成！';
  } else {
    generateStage.value = '❌ 生成失败';
  }
}

async function handleGenerate() {
  if (!aiStatus.value.available) {
    message.error('AI 服务不可用');
    return;
  }

  aiGenerating.value = true;
  aiGeneratedContent.value = '';
  aiContentRestored.value = false;
  restoredContent.value = '';

  // 开始进度动画
  startProgress();

  try {
    const result = await aiGenerateDocument({
      documentType: aiFormData.documentType,
      requirement: aiFormData.requirement,
      matterId: selectedMatterId.value,
      additionalContext: aiFormData.additionalContext,
      tone: aiFormData.tone,
      previewOnly: true,
      aiIntegrationId: selectedModelId.value,
      useMatterContext: useMatterContext.value && !!currentContext.value,
      matterContext: currentContext.value || undefined,
      enableMasking: enableMasking.value,
    });
    stopProgress(true);
    aiGeneratedContent.value = result.content;
  } catch (error: any) {
    stopProgress(false);
    message.error(error.message || 'AI 生成失败');
    aiGeneratedContent.value = '生成失败，请重试';
  } finally {
    aiGenerating.value = false;
  }
}

// 显示修改要求面板
function showRevision() {
  showRevisionPanel.value = true;
  revisionRequest.value = '';
}

// 取消修改
function cancelRevision() {
  showRevisionPanel.value = false;
  revisionRequest.value = '';
}

// 执行迭代优化
async function handleRevision() {
  if (!revisionRequest.value.trim()) {
    message.warning('请输入修改要求');
    return;
  }

  if (!aiStatus.value.available) {
    message.error('AI 服务不可用');
    return;
  }

  showRevisionPanel.value = false;
  aiGenerating.value = true;
  aiContentRestored.value = false;
  restoredContent.value = '';

  // 保存当前内容用于迭代
  const previousContent = aiGeneratedContent.value;
  aiGeneratedContent.value = '';

  // 开始进度动画
  startProgress();

  try {
    // 构建请求
    let finalRequirement = '';
    let finalAdditionalContext = aiFormData.additionalContext || '';

    if (revisionMode.value === 'refine') {
      // 在此基础上修改：将现有内容和修改要求一起发给 AI
      finalRequirement = `请基于以下已有文书内容进行修改：\n\n【原有要求】${aiFormData.requirement}\n\n【修改要求】${revisionRequest.value}`;
      finalAdditionalContext = `【已生成的文书内容】\n${previousContent}\n\n${finalAdditionalContext}`;
    } else {
      // 完全重新生成：使用新要求替换原要求
      finalRequirement = revisionRequest.value;
    }

    const result = await aiGenerateDocument({
      documentType: aiFormData.documentType,
      requirement: finalRequirement,
      matterId: selectedMatterId.value,
      additionalContext: finalAdditionalContext,
      tone: aiFormData.tone,
      previewOnly: true,
      aiIntegrationId: selectedModelId.value,
      useMatterContext: useMatterContext.value && !!currentContext.value,
      matterContext: currentContext.value || undefined,
      enableMasking: enableMasking.value,
    });
    stopProgress(true);
    aiGeneratedContent.value = result.content;
    revisionRequest.value = '';
    message.success(
      revisionMode.value === 'refine'
        ? '文书已根据您的要求修改！'
        : '文书已重新生成！',
    );
  } catch (error: any) {
    stopProgress(false);
    message.error(error.message || 'AI 生成失败');
    // 恢复之前的内容
    aiGeneratedContent.value = previousContent;
  } finally {
    aiGenerating.value = false;
  }
}

async function handleSave() {
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
      aiIntegrationId: selectedModelId.value,
      useMatterContext: useMatterContext.value && !!currentContext.value,
      matterContext: currentContext.value || undefined,
      enableMasking: enableMasking.value,
    });

    if (result.document) {
      generateSuccess.value = true;
      message.success('AI 文书保存成功！');
      emit('success');
    }
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    aiSaving.value = false;
  }
}

async function handleRestoreContent() {
  if (!aiGeneratedContent.value || !maskingMapping.value) {
    message.warning('没有可还原的内容');
    return;
  }

  restoring.value = true;
  try {
    const restored = await restoreMaskedText(
      aiGeneratedContent.value,
      maskingMapping.value,
    );
    restoredContent.value = restored;
    aiContentRestored.value = true;
    message.success('脱敏还原完成！');
  } catch (error: any) {
    message.error(error.message || '脱敏还原失败');
  } finally {
    restoring.value = false;
  }
}

function toggleRestoredContent() {
  aiContentRestored.value = !aiContentRestored.value;
}

function handlePrint() {
  if (!displayedContent.value) {
    message.warning('暂无内容可打印');
    return;
  }

  try {
    const printData: DocumentPrintData = {
      title: aiFormData.documentType || 'AI生成文书',
      content: displayedContent.value,
      documentType: aiFormData.documentType,
      preserveFormat: true,
    };
    printDocument(printData);
  } catch (error: any) {
    message.error(error.message || '打印失败');
  }
}

function handleContextCollected(
  context: MatterContextDTO,
  masked: MatterContextDTO | null,
  mapping: MaskingMappingDTO | null,
) {
  matterContext.value = context;
  matterContextMasked.value = masked;
  maskingMapping.value = mapping;
  useMatterContext.value = true;
}

function handleContextCleared() {
  matterContext.value = null;
  matterContextMasked.value = null;
  maskingMapping.value = null;
  useMatterContext.value = false;
}

function handleMatterChange() {
  // 清空上下文
  handleContextCleared();
  aiContentRestored.value = false;
  restoredContent.value = '';
}

function handleReset() {
  aiStep.value = 0;
  selectedMatterId.value = undefined;
  selectedDossierId.value = undefined;
  isPersonalDoc.value = false;
  aiFormData.documentType = '';
  aiFormData.requirement = '';
  aiFormData.fileName = '';
  aiFormData.additionalContext = '';
  aiFormData.tone = '';
  aiGeneratedContent.value = '';
  generateSuccess.value = false;
  handleContextCleared();

  if (aiStatus.value.defaultId) {
    selectedModelId.value = aiStatus.value.defaultId;
  }

  emit('reset');
}

function goToDocuments() {
  if (selectedMatterId.value) {
    router.push(`/matter/detail/${selectedMatterId.value}?tab=dossier`);
  } else {
    router.push('/document/my');
  }
}

onMounted(() => {
  loadAiStatus();
});

onUnmounted(() => {
  // 清理进度定时器
  if (progressTimer) {
    clearInterval(progressTimer);
    progressTimer = null;
  }
});

// 暴露
defineExpose({
  handleReset,
  loadAiStatus,
});
</script>

<template>
  <div class="ai-mode">
    <!-- AI 状态提示 -->
    <Alert
      v-if="!aiStatus.available"
      type="warning"
      show-icon
      class="status-alert"
    >
      <template #message>AI 服务未配置</template>
      <template #description>
        {{
          aiStatus.message ||
          '请在「系统管理 → 外部集成」中配置并启用 AI 大模型'
        }}
      </template>
    </Alert>

    <Alert v-else type="success" show-icon class="status-alert">
      <template #message>
        <Space>
          <IconifyIcon icon="carbon:flash" class="ai-icon-flash" />
          AI 服务已就绪
          <Tag color="blue">{{ aiStatus.model }}</Tag>
        </Space>
      </template>
    </Alert>

    <Steps :current="aiStep" class="steps-bar">
      <Step
        v-for="step in steps"
        :key="step.title"
        :title="step.title"
        :description="step.description"
      />
    </Steps>

    <div class="step-content">
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
                  option-filter-prop="label"
                  show-search
                >
                  <template #suffixIcon>
                    <IconifyIcon icon="carbon:machine-learning-model" />
                  </template>
                </Select>
              </Col>
              <Col :span="8">
                <Space>
                  <Tag
                    v-if="selectedModel"
                    :color="getModelTagColor(selectedModel.code)"
                  >
                    {{ selectedModel.modelName || selectedModel.code }}
                  </Tag>
                  <Tag
                    v-if="selectedModel"
                    :color="
                      getModelTag(selectedModel.code) === '本地'
                        ? 'green'
                        : 'blue'
                    "
                  >
                    {{ getModelTag(selectedModel.code) }}
                  </Tag>
                </Space>
              </Col>
            </Row>
            <div v-if="availableModels.length > 1" class="model-hint">
              <Space>
                <Tag color="cyan">{{ availableModels.length }} 个可用</Tag>
                <span class="hint-text">💡 可选择云端或本地部署的 AI 模型</span>
              </Space>
            </div>
            <Alert
              v-if="selectedModel && getModelTag(selectedModel.code) === '本地'"
              type="info"
              class="local-model-alert"
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
                  :options="DOCUMENT_TYPE_OPTIONS"
                  style="width: 100%"
                />
              </FormItem>
            </Col>
            <Col :span="12">
              <FormItem label="语气风格">
                <Select
                  v-model:value="aiFormData.tone"
                  placeholder="选择或输入自定义语气风格"
                  :options="toneOptions"
                  allow-clear
                  show-search
                  :filter-option="filterToneOption"
                  style="width: 100%"
                >
                  <template #dropdownRender="{ menuNode }">
                    <div>
                      <component :is="menuNode" />
                      <div v-if="customToneInput" class="custom-tone-add">
                        <Divider style="margin: 4px 0" />
                        <div
                          class="add-custom-tone"
                          @click="handleAddCustomTone"
                        >
                          <span>➕ 添加自定义：</span>
                          <strong>{{ customToneInput }}</strong>
                        </div>
                      </div>
                    </div>
                  </template>
                </Select>
                <p class="tone-hint">
                  💡 可从预设选项中选择，或输入自定义语气风格
                </p>
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
            <div class="ocr-upload">
              <Space>
                <Upload
                  :show-upload-list="false"
                  :before-upload="handleOcrReference"
                  accept="image/*"
                  :disabled="ocrLoading"
                >
                  <Tooltip
                    title="上传参考材料图片，自动识别文字内容添加到补充信息中"
                  >
                    <Button size="small" :loading="ocrLoading">
                      <IconifyIcon icon="ant-design:scan-outlined" />
                      OCR识别材料
                    </Button>
                  </Tooltip>
                </Upload>
                <span class="hint-text"
                  >支持上传图片，自动提取文字作为参考</span
                >
              </Space>
            </div>
            <Textarea
              v-model:value="aiFormData.additionalContext"
              placeholder="提供更多背景信息，如：&#10;- 相关法律条文&#10;- 证据情况&#10;- 对方情况等&#10;&#10;可点击上方按钮上传图片自动识别文字"
              :rows="4"
              :maxlength="2000"
            />
          </FormItem>

          <FormItem label="文件名称">
            <Input
              v-model:value="aiFormData.fileName"
              placeholder="留空将自动生成"
              style="max-width: 400px"
            />
          </FormItem>
        </Form>
      </div>

      <!-- 步骤2：选择项目 & 收集信息 -->
      <div v-show="aiStep === 1">
        <Row :gutter="24">
          <Col :span="12">
            <MatterSelector
              ref="matterSelectorRef"
              v-model="selectedMatterId"
              v-model:is-personal-doc="isPersonalDoc"
              v-model:dossier-id="selectedDossierId"
              :show-dossier="true"
              :show-matter-card="true"
              @matter-change="handleMatterChange"
            />

            <!-- 上下文收集器 -->
            <ContextCollector
              v-if="selectedMatterId && !isPersonalDoc"
              ref="contextCollectorRef"
              :matter-id="selectedMatterId"
              :enable-masking="enableMasking"
              @update:enable-masking="enableMasking = $event"
              @context-collected="handleContextCollected"
              @context-cleared="handleContextCleared"
            />
          </Col>
          <Col :span="12">
            <!-- 文书信息摘要 -->
            <div class="doc-summary">
              <p class="summary-label">📋 文书信息摘要</p>
              <p style="margin: 0">
                <strong>类型：</strong>{{ aiFormData.documentType || '未选择' }}
              </p>
              <p style="margin: 8px 0 0">
                <strong>风格：</strong>{{ aiFormData.tone || '默认' }}
              </p>
              <Divider style="margin: 12px 0" />
              <p class="requirement-preview">
                <strong>需求描述：</strong><br />
                {{
                  aiFormData.requirement
                    ? aiFormData.requirement.substring(0, 100) +
                      (aiFormData.requirement.length > 100 ? '...' : '')
                    : '未填写'
                }}
              </p>
              <template v-if="useMatterContext">
                <Divider style="margin: 12px 0" />
                <p style="margin: 0">
                  <Tag color="blue">📋 包含项目信息</Tag>
                  <Tag v-if="enableMasking" color="green">🔒 已脱敏</Tag>
                </p>
              </template>
            </div>

            <!-- 保存位置提示 -->
            <Alert
              v-if="selectedMatterId && !isPersonalDoc"
              type="info"
              class="save-location-alert"
            >
              <template #message>
                <Space>
                  <span>📂 保存位置：</span>
                  <strong>{{
                    matterSelectorRef?.selectedMatter?.name || '项目卷宗'
                  }}</strong>
                </Space>
              </template>
            </Alert>
            <Alert
              v-else-if="isPersonalDoc"
              type="info"
              class="save-location-alert"
            >
              <template #message>
                <Space>
                  <span>📂 保存位置：</span>
                  <strong>我的文书</strong>
                </Space>
              </template>
            </Alert>
          </Col>
        </Row>
      </div>

      <!-- 步骤3：AI 生成预览 -->
      <div v-show="aiStep === 2">
        <template v-if="!generateSuccess">
          <Row :gutter="24">
            <Col :span="16">
              <div class="preview-header">
                <h4 style="margin: 0">
                  <IconifyIcon
                    icon="carbon:machine-learning-model"
                    class="ai-icon"
                  />
                  AI 生成内容
                  <Tag
                    v-if="aiContentRestored"
                    color="green"
                    style="margin-left: 8px"
                  >
                    已还原
                  </Tag>
                  <Tag
                    v-else-if="
                      enableMasking && maskingMapping?.mappings?.length
                    "
                    color="blue"
                    style="margin-left: 8px"
                  >
                    脱敏中
                  </Tag>
                </h4>
                <Space>
                  <Button
                    v-if="
                      enableMasking &&
                      maskingMapping?.mappings?.length &&
                      aiGeneratedContent &&
                      !aiContentRestored
                    "
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
                  <Button
                    type="link"
                    :loading="aiGenerating"
                    @click="handleGenerate"
                  >
                    <IconifyIcon icon="carbon:renew" /> 重新生成
                  </Button>
                  <Button
                    type="link"
                    @click="showRevision"
                    :disabled="aiGenerating"
                  >
                    <IconifyIcon icon="carbon:edit" /> 修改要求
                  </Button>
                  <Button
                    v-if="displayedContent"
                    type="primary"
                    ghost
                    @click="handlePrint"
                  >
                    🖨️ 打印
                  </Button>
                </Space>
              </div>

              <Alert
                v-if="
                  enableMasking &&
                  maskingMapping?.mappings?.length &&
                  aiGeneratedContent &&
                  !aiContentRestored
                "
                type="info"
                class="restore-hint"
              >
                <template #message>
                  <Space>
                    <span
                      >💡
                      当前显示的是脱敏后的内容，点击"脱敏还原"可恢复真实信息</span
                    >
                    <Tag color="blue">
                      {{ maskingMapping.mappings.length }} 处待还原
                    </Tag>
                  </Space>
                </template>
              </Alert>

              <!-- 修改要求面板 -->
              <Card v-if="showRevisionPanel" class="revision-panel">
                <template #title>
                  <Space>
                    <IconifyIcon icon="carbon:edit" />
                    <span>修改要求</span>
                    <Tag v-if="revisionMode === 'refine'" color="blue">
                      在此基础上修改
                    </Tag>
                    <Tag v-else color="orange">完全重新生成</Tag>
                  </Space>
                </template>
                <template #extra>
                  <Button type="text" size="small" @click="cancelRevision">
                    取消
                  </Button>
                </template>

                <div class="revision-mode">
                  <span class="mode-label">修改方式：</span>
                  <Space>
                    <Button
                      :type="revisionMode === 'refine' ? 'primary' : 'default'"
                      size="small"
                      @click="revisionMode = 'refine'"
                    >
                      <IconifyIcon icon="carbon:settings-adjust" />
                      在此基础上修改
                    </Button>
                    <Button
                      :type="
                        revisionMode === 'regenerate' ? 'primary' : 'default'
                      "
                      size="small"
                      @click="revisionMode = 'regenerate'"
                    >
                      <IconifyIcon icon="carbon:renew" /> 完全重新生成
                    </Button>
                  </Space>
                </div>

                <p class="revision-hint">
                  {{
                    revisionMode === 'refine'
                      ? '💡 AI 将在现有文书基础上，根据您的要求进行修改和优化'
                      : '💡 AI 将使用新的要求重新生成文书，不参考现有内容'
                  }}
                </p>

                <Textarea
                  v-model:value="revisionRequest"
                  :placeholder="
                    revisionMode === 'refine'
                      ? '请输入修改要求，例如：\n- 第一段增加更多法律依据\n- 结尾部分语气更加正式\n- 补充关于时效的说明'
                      : '请输入新的文书要求...'
                  "
                  :rows="4"
                  show-count
                  :maxlength="1000"
                  class="revision-input"
                />

                <div class="revision-actions">
                  <Space>
                    <Button @click="cancelRevision">取消</Button>
                    <Button
                      type="primary"
                      :loading="aiGenerating"
                      :disabled="!revisionRequest.trim()"
                      @click="handleRevision"
                    >
                      <IconifyIcon
                        :icon="
                          revisionMode === 'refine'
                            ? 'carbon:settings-adjust'
                            : 'carbon:renew'
                        "
                      />
                      {{ revisionMode === 'refine' ? '应用修改' : '重新生成' }}
                    </Button>
                  </Space>
                </div>
              </Card>

              <!-- 生成进度条 -->
              <div v-if="aiGenerating" class="generate-progress">
                <Card class="progress-card">
                  <div class="progress-header">
                    <IconifyIcon
                      icon="carbon:machine-learning-model"
                      class="progress-icon"
                    />
                    <span class="progress-title">AI 正在生成文书</span>
                  </div>
                  <Progress
                    :percent="generateProgress"
                    :status="generateProgress === 100 ? 'success' : 'active'"
                    :stroke-color="{
                      '0%': '#108ee9',
                      '100%': '#87d068',
                    }"
                  />
                  <div class="progress-stage">
                    <span class="stage-text">{{ generateStage }}</span>
                  </div>
                  <div class="progress-tips">
                    <p>💡 提示：AI 生成通常需要 10-30 秒，请耐心等待</p>
                    <p v-if="useMatterContext">📋 正在使用项目信息辅助生成</p>
                    <p v-if="enableMasking">🔒 敏感信息已脱敏处理</p>
                  </div>
                </Card>
              </div>

              <!-- 生成结果预览 -->
              <Card v-else class="preview-card">
                <pre class="preview-content">{{
                  displayedContent || '点击"生成预览"开始生成文书'
                }}</pre>
              </Card>
            </Col>
            <Col :span="8">
              <h4 class="section-title">📋 保存信息</h4>
              <Card>
                <p><strong>文书类型：</strong>{{ aiFormData.documentType }}</p>
                <p>
                  <strong>语气风格：</strong>{{ aiFormData.tone || '默认' }}
                </p>
                <p>
                  <strong>保存位置：</strong>
                  <span v-if="isPersonalDoc">我的文书</span>
                  <span v-else-if="selectedMatterId">项目卷宗</span>
                  <span v-else>-</span>
                </p>
                <p>
                  <strong>文件名：</strong
                  >{{ aiFormData.fileName || '自动生成' }}
                </p>
                <Divider />
                <p class="model-info">
                  <IconifyIcon
                    icon="carbon:machine-learning-model"
                    class="ai-icon"
                  />
                  使用 {{ selectedModel?.name || aiStatus.model }}
                  <Tag v-if="selectedModel?.modelName" size="small">
                    {{ selectedModel.modelName }}
                  </Tag>
                </p>

                <div v-if="useMatterContext" class="context-status">
                  <Tag color="blue">📋 使用项目信息</Tag>
                  <Tag
                    v-if="enableMasking"
                    :color="aiContentRestored ? 'green' : 'orange'"
                  >
                    {{ aiContentRestored ? '🔓 已还原' : '🔒 脱敏中' }}
                  </Tag>
                </div>
              </Card>

              <!-- 脱敏映射详情 -->
              <Card
                v-if="maskingMapping?.mappings?.length"
                class="mapping-card"
                size="small"
              >
                <template #title>
                  <span class="mapping-title"
                    >🔐 脱敏映射（{{
                      maskingMapping.mappings.length
                    }}
                    项）</span
                  >
                </template>
                <div class="mapping-content">
                  <div
                    v-for="(m, i) in maskingMapping.mappings.slice(0, 10)"
                    :key="i"
                    class="mapping-item"
                  >
                    <span class="field-name">{{ m.fieldName }}：</span>
                    <span class="masked-value">{{ m.maskedValue }}</span>
                    <span class="arrow">→</span>
                    <span class="original-value">{{ m.originalValue }}</span>
                  </div>
                  <p
                    v-if="maskingMapping.mappings.length > 10"
                    class="more-hint"
                  >
                    ...还有 {{ maskingMapping.mappings.length - 10 }} 项
                  </p>
                </div>
              </Card>
            </Col>
          </Row>
        </template>

        <Result
          v-else
          status="success"
          title="AI 文书保存成功！"
          :sub-title="successSubTitle"
        >
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
        <Button v-if="aiStep > 0 && !generateSuccess" @click="handlePrev">
          上一步
        </Button>
        <Button
          v-if="aiStep < 2"
          type="primary"
          :disabled="!aiStatus.available"
          @click="handleNext"
        >
          下一步
        </Button>
        <Button
          v-if="aiStep === 2 && !generateSuccess && aiGeneratedContent"
          type="primary"
          :loading="aiSaving"
          @click="handleSave"
        >
          💾 保存文书
        </Button>
      </Space>
    </div>
  </div>
</template>

<style scoped>
@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.1);
  }
}

@keyframes glow {
  0%,
  100% {
    filter: drop-shadow(0 0 2px #722ed1);
  }

  50% {
    filter: drop-shadow(0 0 8px #722ed1) drop-shadow(0 0 12px #b37feb);
  }
}

.ai-mode {
  width: 100%;
}

.status-alert {
  margin-bottom: 16px;
}

.steps-bar {
  margin-bottom: 24px;
}

.step-content {
  min-height: 400px;
}

.section-title {
  margin-bottom: 16px;
}

.hint-text {
  font-size: 12px;
  color: #666;
}

.model-hint {
  margin-top: 8px;
}

.local-model-alert {
  margin-top: 8px;
}

.ocr-upload {
  margin-bottom: 8px;
}

.doc-summary {
  padding: 16px;
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
  border: 1px solid #b7eb8f;
  border-radius: 8px;
}

.summary-label {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: #52c41a;
}

.requirement-preview {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
}

.save-location-alert {
  margin-top: 16px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.restore-hint {
  margin-bottom: 16px;
}

.preview-card {
  min-height: 400px;
  background: #fafafa;
}

.preview-content {
  font-family: inherit;
  line-height: 1.8;
  white-space: pre-wrap;
}

.model-info {
  font-size: 12px;
  color: #999;
}

.context-status {
  padding: 8px;
  margin-top: 12px;
  background: #f6ffed;
  border-radius: 4px;
}

.mapping-card {
  margin-top: 16px;
}

.mapping-title {
  font-size: 12px;
}

.mapping-content {
  max-height: 150px;
  overflow-y: auto;
  font-size: 12px;
}

.mapping-item {
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}

.field-name {
  color: #999;
}

.masked-value {
  color: #ff4d4f;
  text-decoration: line-through;
}

.arrow {
  margin: 0 4px;
}

.original-value {
  color: #52c41a;
}

.more-hint {
  margin: 8px 0 0;
  color: #999;
}

.action-bar {
  text-align: center;
}

.tone-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #666;
}

.custom-tone-add {
  padding: 0 8px 8px;
}

.add-custom-tone {
  padding: 6px 12px;
  color: #1890ff;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.add-custom-tone:hover {
  background: #e6f7ff;
}

.add-custom-tone strong {
  margin-left: 4px;
  color: #52c41a;
}

/* 生成进度 */
.generate-progress {
  margin-bottom: 16px;
}

.progress-card {
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 100%);
  border: 1px solid #91d5ff;
}

.progress-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.progress-title {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.progress-stage {
  padding: 8px 12px;
  margin-top: 12px;
  text-align: center;
  background: #fff;
  border-radius: 4px;
}

.stage-text {
  font-size: 14px;
  font-weight: 500;
  color: #1890ff;
}

.progress-tips {
  padding: 12px;
  margin-top: 16px;
  background: rgb(255 255 255 / 70%);
  border-radius: 4px;
}

.progress-tips p {
  margin: 4px 0;
  font-size: 12px;
  color: #666;
}

/* 修改要求面板 */
.revision-panel {
  margin-bottom: 16px;
  border: 2px solid #1890ff;
  border-radius: 8px;
}

.revision-mode {
  margin-bottom: 12px;
}

.mode-label {
  margin-right: 8px;
  font-weight: 500;
}

.revision-hint {
  padding: 8px 12px;
  margin: 8px 0 12px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
}

.revision-input {
  margin-bottom: 12px;
}

.revision-actions {
  text-align: right;
}

/* AI 图标样式 */
.ai-icon {
  margin-right: 4px;
  font-size: 18px;
  vertical-align: middle;
  color: #722ed1;
}

.ai-icon-flash {
  margin-right: 4px;
  font-size: 16px;
  vertical-align: middle;
  color: #faad14;
}

/* 进度图标动画 */
.progress-icon {
  margin-right: 12px;
  font-size: 32px;
  color: #722ed1;
  animation:
    pulse 1.5s ease-in-out infinite,
    glow 2s ease-in-out infinite;
}
</style>
