<script setup lang="ts">
import type {
  CollectOptions,
  DocumentInfo,
  MaskingMappingDTO,
  MatterContextDTO,
} from '#/api/document/ai';

/**
 * 项目上下文收集器组件
 * 用于收集项目信息、脱敏处理，供 AI 生成文书使用
 */
import { computed, reactive, ref, watch } from 'vue';

import {
  Alert,
  Button,
  Card,
  Checkbox,
  Collapse,
  CollapsePanel,
  Divider,
  message,
  Space,
  Switch,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  collectSelectiveContext,
  getAvailableDocuments,
  maskWithMapping,
} from '#/api/document/ai';

defineOptions({ name: 'ContextCollector' });

const props = defineProps<{
  /** 是否启用脱敏 */
  enableMasking?: boolean;
  /** 项目 ID */
  matterId: number;
}>();

const emit = defineEmits<{
  (e: 'update:enableMasking', value: boolean): void;
  (
    e: 'contextCollected',
    context: MatterContextDTO,
    masked: MatterContextDTO | null,
    mapping: MaskingMappingDTO | null,
  ): void;
  (e: 'contextCleared'): void;
}>();

// 状态
const matterContext = ref<MatterContextDTO | null>(null);
const matterContextMasked = ref<MatterContextDTO | null>(null);
const maskingMapping = ref<MaskingMappingDTO | null>(null);
const contextCollecting = ref(false);
const contextMasking = ref(false);
const showContextPreview = ref(false);
const showMaskingDetails = ref(false);

// 可选文档
const availableDocuments = ref<DocumentInfo[]>([]);
const selectedDocumentIds = ref<number[]>([]);

// 收集选项
const collectOptions = reactive<CollectOptions>({
  includeMatterInfo: true,
  includeClients: true,
  includeParticipants: true,
  includeDocuments: true,
  extractDocumentContent: false,
  selectedDocumentIds: [],
});

// 计算属性
const hasContext = computed(() => !!matterContext.value);

const currentContext = computed(() => {
  if (!matterContext.value) return null;
  return props.enableMasking && matterContextMasked.value
    ? matterContextMasked.value
    : matterContext.value;
});

// 脱敏统计
const maskingStats = computed(() => {
  if (!maskingMapping.value?.mappings) return {};
  const stats: Record<string, number> = {};
  maskingMapping.value.mappings.forEach((m) => {
    const field = m.fieldName || '其他';
    stats[field] = (stats[field] || 0) + 1;
  });
  return stats;
});

// 辅助方法：根据字段名获取颜色
function getFieldColor(fieldName: string | undefined): string {
  if (!fieldName) return 'default';
  if (fieldName.includes('身份证')) return 'red';
  if (fieldName.includes('手机') || fieldName.includes('电话')) return 'orange';
  if (fieldName.includes('邮箱')) return 'blue';
  if (fieldName.includes('银行') || fieldName.includes('账号')) return 'gold';
  if (fieldName.includes('姓名') || fieldName.includes('名称')) return 'green';
  if (fieldName.includes('文档内容')) return 'purple';
  if (fieldName.includes('地址')) return 'cyan';
  return 'default';
}

// 辅助方法：截断长文本
function truncateText(text: string | undefined, maxLen: number): string {
  if (!text) return '-';
  if (text.length <= maxLen) return text;
  return `${text.slice(0, maxLen)}...`;
}

// 方法
async function loadAvailableDocuments() {
  if (!props.matterId) return;
  try {
    const docs = await getAvailableDocuments(props.matterId);
    availableDocuments.value = docs || [];
    selectedDocumentIds.value = docs?.map((d) => d.id!).filter(Boolean) || [];
  } catch (error: any) {
    console.error('加载项目文档列表失败', error);
    availableDocuments.value = [];
  }
}

async function handleCollectContext() {
  if (!props.matterId) {
    message.warning('请先选择一个项目');
    return;
  }

  contextCollecting.value = true;
  try {
    collectOptions.selectedDocumentIds = selectedDocumentIds.value;

    const context = await collectSelectiveContext(
      props.matterId,
      collectOptions,
    );
    matterContext.value = context;

    const docsCount = context.documents?.length || 0;
    const hasContent = context.documents?.some((d) => d.content);
    message.success(
      `项目信息收集成功！${docsCount > 0 ? `包含 ${docsCount} 个文档` : ''}${hasContent ? '（已提取内容）' : ''}`,
    );

    // 如果开启了脱敏，自动脱敏
    if (props.enableMasking) {
      await handleMaskContext();
    } else {
      emit('contextCollected', context, null, null);
    }
  } catch (error: any) {
    message.error(error.message || '项目信息收集失败');
  } finally {
    contextCollecting.value = false;
  }
}

async function handleMaskContext() {
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
    message.success(
      `数据脱敏完成！${mappingCount > 0 ? `（${mappingCount} 项敏感信息已脱敏）` : ''}`,
    );

    emit(
      'contextCollected',
      matterContext.value,
      result.maskedContext,
      result.mapping,
    );
  } catch (error: any) {
    message.error(error.message || '数据脱敏失败');
  } finally {
    contextMasking.value = false;
  }
}

function handleMaskingChange(checked: boolean | number | string) {
  const value = Boolean(checked);
  emit('update:enableMasking', value);

  // 如果已收集上下文且开启脱敏，执行脱敏
  if (value && matterContext.value && !matterContextMasked.value) {
    handleMaskContext();
  }
}

function handleExtractContentChange(checked: boolean | number | string) {
  collectOptions.extractDocumentContent = Boolean(checked);
}

function handleDocumentSelect(docId: number, checked: boolean) {
  if (checked) {
    if (!selectedDocumentIds.value.includes(docId)) {
      selectedDocumentIds.value.push(docId);
    }
  } else {
    selectedDocumentIds.value = selectedDocumentIds.value.filter(
      (id) => id !== docId,
    );
  }
}

function handleSelectAllDocuments(checked: boolean) {
  selectedDocumentIds.value = checked
    ? availableDocuments.value.map((d) => d.id!).filter(Boolean)
    : [];
}

function clearContext() {
  matterContext.value = null;
  matterContextMasked.value = null;
  maskingMapping.value = null;
  showContextPreview.value = false;
  emit('contextCleared');
}

function formatContextForPreview(context: MatterContextDTO | null): string {
  if (!context) return '';

  const lines: string[] = [];

  if (context.matter) {
    lines.push(
      '【项目信息】',
      `项目名称：${context.matter.name || '-'}`,
      `项目编号：${context.matter.matterNo || '-'}`,
    );
    if (context.matter.matterType)
      lines.push(`项目类型：${context.matter.matterType}`);
    if (context.matter.caseType)
      lines.push(`案件类型：${context.matter.caseType}`);
    if (context.matter.description)
      lines.push(`案情概述：${context.matter.description}`);
    if (context.matter.opposingParty)
      lines.push(`对方当事人：${context.matter.opposingParty}`);
    lines.push('');
  }

  if (context.clients && context.clients.length > 0) {
    lines.push('【当事人信息】');
    context.clients.forEach((client) => {
      const primaryLabel = client.isPrimary ? '（主要）' : '';
      lines.push(
        `${client.role || '委托人'}${primaryLabel}：${client.name || '-'}`,
      );
      if (client.idCard) lines.push(`  身份证号：${client.idCard}`);
      if (client.creditCode)
        lines.push(`  统一社会信用代码：${client.creditCode}`);
      if (client.contactPhone) lines.push(`  联系电话：${client.contactPhone}`);
      if (client.registeredAddress)
        lines.push(`  地址：${client.registeredAddress}`);
    });
    lines.push('');
  }

  if (context.participants && context.participants.length > 0) {
    lines.push('【代理律师信息】');
    context.participants.forEach((p) => {
      lines.push(`${p.role || '律师'}：${p.name || '-'}`);
      if (p.lawyerLicenseNo) lines.push(`  执业证号：${p.lawyerLicenseNo}`);
    });
    lines.push('');
  }

  if (context.documents && context.documents.length > 0) {
    lines.push('【相关文档】');
    context.documents.forEach((doc) => {
      lines.push(`- ${doc.title || doc.fileName} (${doc.fileType})`);
    });
  }

  if (context.masked) {
    lines.push('', '⚠️ 以上信息已进行脱敏处理，带 * 的部分为脱敏内容');
  }

  return lines.join('\n');
}

// 监听项目变化
watch(
  () => props.matterId,
  (newVal, oldVal) => {
    if (newVal !== oldVal) {
      clearContext();
      if (newVal) {
        loadAvailableDocuments();
      } else {
        availableDocuments.value = [];
        selectedDocumentIds.value = [];
      }
    }
  },
  { immediate: true },
);

// 暴露
defineExpose({
  matterContext,
  matterContextMasked,
  maskingMapping,
  hasContext,
  currentContext,
  clearContext,
});
</script>

<template>
  <div class="context-collector">
    <Divider style="margin: 16px 0" />
    <h4 class="section-title">📋 收集项目信息</h4>
    <p class="hint-text">
      选择要收集的信息类型和文档，提供给 AI 生成更精准的文书
    </p>

    <!-- 选择性收集选项 -->
    <Collapse ghost class="options-collapse">
      <CollapsePanel key="options" header="📌 选择收集内容">
        <div class="options-content">
          <p class="options-label">信息类型：</p>
          <Space direction="vertical" style="width: 100%">
            <Checkbox v-model:checked="collectOptions.includeMatterInfo">
              项目基本信息
            </Checkbox>
            <Checkbox v-model:checked="collectOptions.includeClients">
              客户/当事人信息
            </Checkbox>
            <Checkbox v-model:checked="collectOptions.includeParticipants">
              参与律师信息
            </Checkbox>
            <Checkbox v-model:checked="collectOptions.includeDocuments">
              相关文档
            </Checkbox>
          </Space>

          <!-- 文档内容提取开关 -->
          <div
            v-if="collectOptions.includeDocuments"
            class="extract-content-box"
          >
            <Space align="center">
              <span class="options-label">📄 提取文档内容：</span>
              <Switch
                :checked="collectOptions.extractDocumentContent"
                @change="handleExtractContentChange"
              />
              <Tooltip title="提取 Word、PDF、图片等文档的文本内容，供 AI 参考">
                <Tag color="blue">?</Tag>
              </Tooltip>
            </Space>
            <p class="extract-hint">
              {{
                collectOptions.extractDocumentContent
                  ? '将提取 Word、PDF、图片（OCR）的文本内容'
                  : '仅收集文档元信息（标题、类型）'
              }}
            </p>

            <!-- 文档内容安全提示 -->
            <Alert
              v-if="collectOptions.extractDocumentContent"
              type="info"
              class="extract-security-alert"
              show-icon
            >
              <template #message>
                <strong>🔐 文档内容安全说明</strong>
              </template>
              <template #description>
                <p style="margin: 4px 0">
                  提取的文档内容将发送给 AI 大模型进行分析。
                </p>
                <ul style="padding-left: 18px; margin: 4px 0 0">
                  <li>
                    <strong>启用脱敏</strong
                    >：身份证号、手机号、银行卡号、邮箱等敏感信息将被替换为占位符
                  </li>
                  <li>
                    <strong>未启用脱敏</strong
                    >：文档原文将直接发送（仅建议本地部署的模型）
                  </li>
                </ul>
                <p style="margin: 8px 0 0; color: #fa8c16">
                  ⚠️ 请在下方开启"数据脱敏保护"来保护文档中的敏感信息！
                </p>
              </template>
            </Alert>
          </div>

          <!-- 文档选择列表 -->
          <div
            v-if="
              collectOptions.includeDocuments && availableDocuments.length > 0
            "
            class="document-list"
          >
            <div class="document-list-header">
              <span class="options-label"
                >选择文档（{{ selectedDocumentIds.length }}/{{
                  availableDocuments.length
                }}）：</span
              >
              <Space>
                <Button size="small" @click="handleSelectAllDocuments(true)">
                  全选
                </Button>
                <Button size="small" @click="handleSelectAllDocuments(false)">
                  取消全选
                </Button>
              </Space>
            </div>
            <div class="document-list-content">
              <div
                v-for="doc in availableDocuments"
                :key="doc.id"
                class="document-item"
              >
                <Checkbox
                  :checked="selectedDocumentIds.includes(doc.id!)"
                  @change="
                    (e: any) => handleDocumentSelect(doc.id!, e.target.checked)
                  "
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
            class="no-docs-alert"
          />
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 收集按钮 -->
    <Space>
      <Button
        type="primary"
        :loading="contextCollecting"
        @click="handleCollectContext"
      >
        🔍 收集选中信息
      </Button>
      <Tag v-if="hasContext" color="success"> ✓ 已收集 </Tag>
    </Space>

    <!-- 脱敏开关 - 重要安全控制（始终显示，提前告知用户） -->
    <div
      class="masking-control-box"
      :class="{
        'masking-enabled': enableMasking,
        'masking-disabled': !enableMasking,
      }"
    >
      <div class="masking-header">
        <Space align="center" :size="12">
          <span class="masking-title">🔒 数据脱敏保护</span>
          <Switch
            :checked="enableMasking"
            @change="handleMaskingChange"
            :loading="contextMasking"
          />
          <Tag v-if="enableMasking" color="green" class="status-tag">
            ✓ 已开启保护
          </Tag>
          <Tag v-else color="red" class="status-tag">⚠️ 未开启保护</Tag>
        </Space>
      </div>

      <p class="masking-subtitle">
        控制发送给 AI 大模型的敏感信息是否进行脱敏处理
      </p>
    </div>

    <!-- 收集后的脱敏详情 -->
    <div v-if="hasContext" class="masking-details-box">
      <!-- 脱敏范围说明 -->
      <Alert v-if="enableMasking" type="success" class="masking-scope-alert">
        <template #message>
          <strong>✅ 脱敏范围（发送给 AI 前自动处理）：</strong>
        </template>
        <template #description>
          <ul class="masking-scope-list">
            <li>✓ 项目信息：案件编号等</li>
            <li>✓ 客户信息：姓名、身份证号、联系电话、邮箱、地址</li>
            <li>✓ 对方当事人信息：姓名、联系方式</li>
            <li>✓ 企业信息：统一社会信用代码、银行卡号、账号</li>
            <li
              v-if="collectOptions.extractDocumentContent"
              class="doc-content-item"
            >
              <strong>✓ 文档内容中的敏感信息</strong>
              <ul class="doc-content-sublist">
                <li>身份证号码</li>
                <li>手机号码</li>
                <li>邮箱地址</li>
                <li>银行卡号</li>
                <li>其他可识别的敏感模式</li>
              </ul>
            </li>
          </ul>
          <p class="masking-note">
            💡 <strong>还原机制</strong>：系统记录脱敏映射表，AI
            生成文档后可一键还原真实信息
          </p>
        </template>
      </Alert>

      <Alert v-else type="error" class="masking-warning-alert">
        <template #message>
          <strong>⚠️ 安全警告：未启用脱敏保护</strong>
        </template>
        <template #description>
          <p style="margin-bottom: 8px">
            以下原始敏感信息将<strong>直接发送</strong>给 AI 大模型：
          </p>
          <ul class="masking-warning-list">
            <li>客户姓名、身份证号、手机号、邮箱</li>
            <li v-if="collectOptions.extractDocumentContent">
              <strong>文档内容中的所有敏感信息</strong>
            </li>
          </ul>
          <Divider style="margin: 12px 0" />
          <p style="margin-bottom: 8px"><strong>潜在风险：</strong></p>
          <ul class="risk-list">
            <li>🔴 云端大模型可能记录或泄露敏感数据</li>
            <li>🔴 客户隐私信息可能被第三方获取</li>
            <li>🔴 可能不符合《个人信息保护法》等合规要求</li>
          </ul>
          <p class="final-warning">
            <strong>⚡ 仅建议在使用本地部署的大模型时关闭脱敏！</strong>
          </p>
        </template>
      </Alert>

      <!-- 脱敏详情展示 -->
      <div
        v-if="maskingMapping && maskingMapping.mappings?.length"
        class="masking-details"
      >
        <div class="masking-details-header">
          <Space>
            <span class="mapping-hint">
              🔐 已记录
              {{ maskingMapping.mappings.length }} 项脱敏映射，生成后可自动还原
            </span>
            <Button
              type="link"
              size="small"
              @click="showMaskingDetails = !showMaskingDetails"
            >
              {{ showMaskingDetails ? '收起详情' : '查看详情' }}
            </Button>
          </Space>
        </div>

        <!-- 脱敏映射详情表格 -->
        <Card
          v-show="showMaskingDetails"
          size="small"
          class="masking-details-card"
        >
          <template #title>
            <Space>
              <span>🔒 脱敏映射详情</span>
              <Tag color="blue">{{ maskingMapping.mappings.length }} 项</Tag>
            </Space>
          </template>
          <div class="masking-table">
            <div class="masking-table-header">
              <span class="col-field">字段</span>
              <span class="col-original">原始值</span>
              <span class="col-arrow"></span>
              <span class="col-masked">脱敏后</span>
            </div>
            <div class="masking-table-body">
              <div
                v-for="(m, i) in maskingMapping.mappings"
                :key="i"
                class="masking-table-row"
                :class="{ highlight: m.fieldName?.includes('文档内容') }"
              >
                <span class="col-field">
                  <Tag :color="getFieldColor(m.fieldName)" size="small">{{
                    m.fieldName || '未知'
                  }}</Tag>
                </span>
                <span class="col-original" :title="m.originalValue">
                  {{ truncateText(m.originalValue, 20) }}
                </span>
                <span class="col-arrow">→</span>
                <span class="col-masked" :title="m.maskedValue">
                  {{ m.maskedValue }}
                </span>
              </div>
            </div>
          </div>
          <div class="masking-stats">
            <p>📊 脱敏统计：</p>
            <Space wrap>
              <Tag
                v-for="(count, type) in maskingStats"
                :key="type"
                :color="getFieldColor(type)"
              >
                {{ type }}: {{ count }}
              </Tag>
            </Space>
          </div>
        </Card>
      </div>
    </div>

    <!-- 收集的信息预览 -->
    <div v-if="hasContext && currentContext" class="context-preview">
      <div class="preview-header">
        <h4 style="margin: 0">👁️ 收集的信息预览</h4>
        <Button
          type="link"
          size="small"
          @click="showContextPreview = !showContextPreview"
        >
          {{ showContextPreview ? '收起' : '展开' }}
        </Button>
      </div>
      <Card v-show="showContextPreview" size="small" class="preview-card">
        <pre class="preview-content">{{
          formatContextForPreview(currentContext)
        }}</pre>
      </Card>
      <Alert
        v-if="enableMasking && matterContextMasked?.masked"
        type="success"
        class="masked-alert"
      >
        <template #message>
          <span>✓ 以上信息已脱敏，带 * 的内容为脱敏后的数据</span>
        </template>
      </Alert>
    </div>
  </div>
</template>

<style scoped>
.context-collector {
  width: 100%;
}

.section-title {
  margin-bottom: 12px;
}

.hint-text {
  margin-bottom: 12px;
  font-size: 12px;
  color: #666;
}

.options-collapse {
  margin-bottom: 16px;
}

.options-content {
  padding: 8px 0;
}

.options-label {
  margin: 0 0 12px;
  font-weight: 500;
}

.extract-content-box {
  padding: 12px;
  margin-top: 16px;
  background: #e6f7ff;
  border-radius: 8px;
}

.extract-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #096dd9;
}

.document-list {
  margin-top: 16px;
}

.document-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.document-list-content {
  max-height: 200px;
  padding: 8px;
  overflow-y: auto;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
}

.document-item {
  padding: 4px 0;
}

.no-docs-alert {
  margin-top: 12px;
}

.extract-security-alert {
  margin-top: 12px;
  border: 1px solid #91d5ff;
}

/* 脱敏控制区域 - 始终显示 */
.masking-control-box {
  padding: 16px;
  margin: 16px 0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.masking-control-box.masking-enabled {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 2px solid #52c41a;
}

.masking-control-box.masking-disabled {
  background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%);
  border: 2px solid #ff4d4f;
}

.masking-header {
  margin-bottom: 8px;
}

.masking-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.masking-subtitle {
  margin: 0;
  font-size: 13px;
  color: #595959;
}

.status-tag {
  font-size: 12px;
}

/* 脱敏详情区域 */
.masking-details-box {
  margin-top: 12px;
}

.masking-scope-list {
  padding-left: 18px;
  margin: 8px 0;
}

.masking-scope-list li {
  margin: 4px 0;
}

.doc-content-item {
  margin-top: 8px !important;
  color: #1890ff;
}

.doc-content-sublist {
  padding-left: 16px;
  margin: 4px 0 0;
  font-size: 12px;
  color: #595959;
}

.masking-note {
  padding: 8px 12px;
  margin: 12px 0 0;
  font-size: 13px;
  background: #e6f7ff;
  border-radius: 4px;
}

.masking-warning-list {
  padding-left: 18px;
  margin: 0;
}

.risk-list {
  padding-left: 18px;
  margin: 0;
}

.risk-list li {
  margin: 4px 0;
}

.final-warning {
  padding: 8px 12px;
  margin: 12px 0 0;
  color: #cf1322;
  background: #fff1f0;
  border-radius: 4px;
}

.masking-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #ad6800;
}

.mapping-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #52c41a;
}

.context-preview {
  margin-top: 24px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.preview-card {
  max-height: 350px;
  overflow-y: auto;
  background: #fafafa;
}

.preview-content {
  margin: 0;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.masked-alert {
  margin-top: 12px;
}

/* 脱敏详情 */
.masking-details {
  margin-top: 12px;
}

.masking-details-header {
  margin-bottom: 8px;
}

.masking-details-card {
  max-height: 400px;
  margin-top: 8px;
  overflow-y: auto;
}

.masking-table {
  font-size: 12px;
}

.masking-table-header {
  display: flex;
  padding: 8px 0;
  padding: 12px;
  margin: -12px -12px 8px;
  font-weight: 600;
  color: #262626;
  background: #fafafa;
  border-bottom: 2px solid #f0f0f0;
}

.masking-table-body {
  max-height: 250px;
  overflow-y: auto;
}

.masking-table-row {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}

.masking-table-row:hover {
  background: #f0f5ff;
}

.masking-table-row.highlight {
  background: #f9f0ff;
}

.col-field {
  flex: 0 0 120px;
}

.col-original {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: monospace;
  color: #ff4d4f;
  white-space: nowrap;
}

.col-arrow {
  flex: 0 0 30px;
  color: #1890ff;
  text-align: center;
}

.col-masked {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: monospace;
  color: #52c41a;
  white-space: nowrap;
}

.masking-stats {
  padding-top: 12px;
  margin-top: 16px;
  border-top: 1px dashed #d9d9d9;
}

.masking-stats p {
  margin: 0 0 8px;
  font-weight: 500;
}
</style>
