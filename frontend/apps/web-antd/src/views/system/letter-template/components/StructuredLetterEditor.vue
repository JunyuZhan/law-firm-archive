<script setup lang="ts">
/**
 * 结构化出函模板编辑器
 * 将函件模板分为多个区块：标题、编号、收件单位、正文、落款
 * 用户只需关注内容，打印时系统自动排版
 */
import { reactive, ref, watch, computed } from 'vue';

import {
  Alert,
  Collapse,
  CollapsePanel,
  Dropdown,
  Button,
  Input,
  Menu,
  MenuItem,
  Tag,
} from 'ant-design-vue';

import { decodeHtmlEntities } from '../../contract-template/utils/print-formatter';

const { TextArea: Textarea } = Input;

// Props
const props = defineProps<{
  modelValue: string;
  variables?: Array<{ label: string; value: string; description?: string }>;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 函件模板分块数据结构
interface TemplateBlocks {
  title: {
    letterTitle: string; // 函件标题
    letterNo: string; // 函件编号（可选）
  };
  recipient: string; // 收件单位
  body: string; // 正文内容
  signature: {
    firmName: string; // 律所名称
    lawyerNames: string; // 承办律师
    date: string; // 日期
    contactInfo: string; // 联系方式（可选）
  };
}

const blocks = reactive<TemplateBlocks>({
  title: {
    letterTitle: '介 绍 信',
    letterNo: '编号：${letterNo}',
  },
  recipient: '${targetUnit}：',
  body: `兹介绍本所律师\${lawyerNames}（执业证号：\${lawyerLicenseNo}）前往贵处，就\${clientName}与\${opposingParty}\${causeOfAction}一案，进行会见、调查取证、阅卷等相关工作，请予接洽为盼。`,
  signature: {
    firmName: '${firmName}',
    lawyerNames: '承办律师：${lawyerNames}',
    date: '${date}',
    contactInfo: '联系电话：${firmPhone}',
  },
});

// 默认展开的面板
const activeKeys = ref(['title', 'recipient', 'body', 'signature']);

// 记录每个输入框的光标位置
const cursorPositions = ref<Record<string, number>>({});

// 处理输入框失去焦点时保存光标位置
function handleBlur(name: string, event: FocusEvent) {
  const target = event.target as HTMLTextAreaElement | HTMLInputElement;
  if (target) {
    cursorPositions.value[name] = target.selectionStart ?? 0;
  }
}

// 处理点击和选择变化时更新光标位置
function handleSelect(name: string, event: Event) {
  const target = event.target as HTMLTextAreaElement | HTMLInputElement;
  if (target) {
    cursorPositions.value[name] = target.selectionStart ?? 0;
  }
}

// 解析已有内容到区块
function parseContent(content: string) {
  if (!content) return;

  // 先解码 HTML 实体
  const decoded = decodeHtmlEntities(content);

  // 尝试解析 JSON 格式（结构化存储）
  try {
    const parsed = JSON.parse(decoded);
    if (parsed._structured && parsed.blocks) {
      // 新格式：{ _structured: true, blocks: { ... } }
      Object.assign(blocks, parsed.blocks);
      return;
    } else if (
      parsed.title ||
      parsed.recipient ||
      parsed.body ||
      parsed.signature
    ) {
      // 兼容旧格式：直接是 blocks 对象
      Object.assign(blocks, parsed);
      return;
    }
  } catch {
    // 不是 JSON，保持默认值
  }
}

// 将区块转换为存储格式
function blocksToContent(): string {
  return JSON.stringify({
    _structured: true,
    _version: 1,
    blocks: {
      title: blocks.title,
      recipient: blocks.recipient,
      body: blocks.body,
      signature: blocks.signature,
    },
  });
}

// 监听 modelValue 变化
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      parseContent(newVal);
    }
  },
  { immediate: true },
);

// 监听区块变化，输出内容
watch(
  blocks,
  () => {
    emit('update:modelValue', blocksToContent());
  },
  { deep: true },
);

// 在光标位置插入变量
function insertVariable(
  target:
    | 'letterTitle'
    | 'letterNo'
    | 'recipient'
    | 'body'
    | 'firmName'
    | 'lawyerNames'
    | 'date'
    | 'contactInfo',
  variable: string,
) {
  const varStr = `\${${variable}}`;

  // 获取当前值
  let currentValue = '';
  if (target === 'letterTitle') {
    currentValue = blocks.title.letterTitle;
  } else if (target === 'letterNo') {
    currentValue = blocks.title.letterNo;
  } else if (target === 'recipient') {
    currentValue = blocks.recipient;
  } else if (target === 'body') {
    currentValue = blocks.body;
  } else if (target === 'firmName') {
    currentValue = blocks.signature.firmName;
  } else if (target === 'lawyerNames') {
    currentValue = blocks.signature.lawyerNames;
  } else if (target === 'date') {
    currentValue = blocks.signature.date;
  } else if (target === 'contactInfo') {
    currentValue = blocks.signature.contactInfo;
  }

  // 获取记录的光标位置，如果没有记录则追加到末尾
  const cursorPos = cursorPositions.value[target] ?? currentValue.length;

  // 在光标位置插入变量
  const newValue =
    currentValue.slice(0, cursorPos) + varStr + currentValue.slice(cursorPos);

  // 更新对应区块的值
  if (target === 'letterTitle') {
    blocks.title.letterTitle = newValue;
  } else if (target === 'letterNo') {
    blocks.title.letterNo = newValue;
  } else if (target === 'recipient') {
    blocks.recipient = newValue;
  } else if (target === 'body') {
    blocks.body = newValue;
  } else if (target === 'firmName') {
    blocks.signature.firmName = newValue;
  } else if (target === 'lawyerNames') {
    blocks.signature.lawyerNames = newValue;
  } else if (target === 'date') {
    blocks.signature.date = newValue;
  } else if (target === 'contactInfo') {
    blocks.signature.contactInfo = newValue;
  }

  // 更新光标位置（移到插入变量之后）
  cursorPositions.value[target] = cursorPos + varStr.length;
}

// 变量分组 - 按类别明确分类
const variableGroups = computed(() => {
  const groups: Record<string, Array<{ label: string; value: string }>> = {
    函件信息: [],
    '项目/案件': [],
    委托人信息: [],
    '律师/律所': [],
    收件单位: [],
    日期: [],
  };

  // 明确的分组映射
  const groupMapping: Record<string, string> = {
    // 函件信息
    letterNo: '函件信息',
    targetUnit: '收件单位',
    targetAddress: '收件单位',

    // 项目/案件
    matterName: '项目/案件',
    matterNo: '项目/案件',
    caseType: '项目/案件',
    causeOfAction: '项目/案件',
    trialStage: '项目/案件',
    procedureStage: '项目/案件',
    opposingParty: '项目/案件',
    opposingPartyRole: '项目/案件',
    opposingPartyRoleName: '项目/案件',
    opposingLawyerName: '项目/案件',
    opposingLawyerFirm: '项目/案件',
    claimAmount: '项目/案件',
    jurisdictionCourt: '项目/案件',

    // 委托人信息
    clientName: '委托人信息',
    clientIdNumber: '委托人信息',
    clientAddress: '委托人信息',
    clientPhone: '委托人信息',
    clientEmail: '委托人信息',
    clientRole: '委托人信息',
    clientRoleName: '委托人信息',
    legalRepresentative: '委托人信息',
    creditCode: '委托人信息',

    // 律师/律所
    lawyerNames: '律师/律所',
    lawyerLicenseNo: '律师/律所',
    firmName: '律师/律所',
    firmAddress: '律师/律所',
    firmPhone: '律师/律所',
    firmLicense: '律师/律所',
    firmLegalPerson: '律师/律所',

    // 日期
    date: '日期',
    currentDate: '日期',
    currentYear: '日期',
  };

  props.variables?.forEach((v) => {
    const group = groupMapping[v.value];
    if (group && groups[group]) {
      groups[group].push(v);
    } else {
      // 未映射的变量，根据名称推测分组
      if (v.value.startsWith('client')) {
        const targetGroup = groups['委托人信息'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.startsWith('lawyer') || v.value.startsWith('firm')) {
        const targetGroup = groups['律师/律所'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (
        v.value.startsWith('matter') ||
        v.value.includes('case') ||
        v.value.includes('opposing')
      ) {
        const targetGroup = groups['项目/案件'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.startsWith('target')) {
        const targetGroup = groups['收件单位'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.includes('date') || v.value.includes('year')) {
        const targetGroup = groups['日期'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.includes('letter')) {
        const targetGroup = groups['函件信息'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      }
    }
  });

  return groups;
});
</script>

<template>
  <div class="structured-editor">
    <Alert
      message="📝 结构化模板编辑"
      description="模板只负责内容和换行，不存储格式信息。只需填写文本内容，系统会在预览和打印时自动应用格式（字体、大小、对齐等）。点击变量标签可插入到对应位置。"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <Collapse v-model:activeKey="activeKeys" :bordered="false">
      <!-- 区块1：标题区 -->
      <CollapsePanel key="title" header="📌 区块一：标题和编号">
        <template #extra>
          <Tag color="blue">函件标题</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <div class="field-header">
              <label>函件标题（将居中显示为大标题，支持变量）</label>
              <Dropdown>
                <Button size="small" type="link">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <MenuItem
                      v-for="v in (variableGroups['律师/律所'] || []).filter(
                        (v) => ['firmName', 'lawyerNames'].includes(v.value),
                      )"
                      :key="v.value"
                      @click="insertVariable('letterTitle', v.value)"
                    >
                      {{ v.label }}
                    </MenuItem>
                  </Menu>
                </template>
              </Dropdown>
            </div>
            <Textarea
              v-model:value="blocks.title.letterTitle"
              :rows="2"
              placeholder="例如：介绍信、${firmName}会见函、调查函"
              @blur="(e: FocusEvent) => handleBlur('letterTitle', e)"
              @click="(e: Event) => handleSelect('letterTitle', e)"
              @keyup="(e: Event) => handleSelect('letterTitle', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 可以在标题中使用变量，如：${firmName}、${lawyerNames} 等
              </span>
            </div>
          </div>

          <div class="field-group">
            <div class="field-header">
              <label>函件编号（可选，将显示在标题下方居中）</label>
              <Dropdown>
                <Button size="small" type="link">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <MenuItem
                      v-for="v in variableGroups['函件信息']"
                      :key="v.value"
                      @click="insertVariable('letterNo', v.value)"
                    >
                      {{ v.label }}
                    </MenuItem>
                  </Menu>
                </template>
              </Dropdown>
            </div>
            <Input
              v-model:value="blocks.title.letterNo"
              placeholder="编号：${letterNo}"
              @blur="(e: FocusEvent) => handleBlur('letterNo', e)"
              @click="(e: Event) => handleSelect('letterNo', e)"
              @keyup="(e: Event) => handleSelect('letterNo', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 函件编号由系统自动生成，打印时自动载入。常用变量：${letterNo}
              </span>
            </div>
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块2：收件单位区 -->
      <CollapsePanel key="recipient" header="📮 区块二：收件单位">
        <template #extra>
          <Tag color="green">收件单位</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <div class="field-header">
              <label>收件单位</label>
              <Dropdown>
                <Button size="small" type="link">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <MenuItem
                      v-for="v in variableGroups['收件单位']"
                      :key="v.value"
                      @click="insertVariable('recipient', v.value)"
                    >
                      {{ v.label }}
                    </MenuItem>
                  </Menu>
                </template>
              </Dropdown>
            </div>
            <Textarea
              v-model:value="blocks.recipient"
              :rows="3"
              placeholder="${targetUnit}："
              @blur="(e: FocusEvent) => handleBlur('recipient', e)"
              @click="(e: Event) => handleSelect('recipient', e)"
              @keyup="(e: Event) => handleSelect('recipient', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块3：正文区 -->
      <CollapsePanel key="body" header="📜 区块三：正文内容">
        <template #extra>
          <Tag color="orange">正文</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>正文内容（函件的主要内容）</label>

            <!-- 变量标签横向排列 -->
            <div class="variables-panel">
              <template v-for="(vars, group) in variableGroups" :key="group">
                <div v-if="vars.length > 0" class="variable-group">
                  <span class="group-label">{{ group }}：</span>
                  <Tag
                    v-for="v in vars"
                    :key="v.value"
                    color="cyan"
                    class="var-tag"
                    @click="insertVariable('body', v.value)"
                  >
                    {{ v.label }}
                  </Tag>
                </div>
              </template>
            </div>

            <Textarea
              v-model:value="blocks.body"
              :rows="15"
              placeholder="兹介绍本所律师${lawyerNames}（执业证号：${lawyerLicenseNo}）前往贵处，就${clientName}与${opposingParty}${causeOfAction}一案，进行会见、调查取证、阅卷等相关工作，请予接洽为盼。"
              @blur="(e: FocusEvent) => handleBlur('body', e)"
              @click="(e: Event) => handleSelect('body', e)"
              @keyup="(e: Event) => handleSelect('body', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块4：落款区 -->
      <CollapsePanel key="signature" header="✍️ 区块四：落款">
        <template #extra>
          <Tag color="purple">落款</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>律所名称</label>
            <Input
              v-model:value="blocks.signature.firmName"
              placeholder="${firmName}"
              @blur="(e: FocusEvent) => handleBlur('firmName', e)"
              @click="(e: Event) => handleSelect('firmName', e)"
              @keyup="(e: Event) => handleSelect('firmName', e)"
            />
          </div>

          <div class="field-group">
            <label>承办律师</label>
            <Input
              v-model:value="blocks.signature.lawyerNames"
              placeholder="承办律师：${lawyerNames}"
              @blur="(e: FocusEvent) => handleBlur('lawyerNames', e)"
              @click="(e: Event) => handleSelect('lawyerNames', e)"
              @keyup="(e: Event) => handleSelect('lawyerNames', e)"
            />
          </div>

          <div class="field-group">
            <label>日期</label>
            <Input
              v-model:value="blocks.signature.date"
              placeholder="${date}"
              @blur="(e: FocusEvent) => handleBlur('date', e)"
              @click="(e: Event) => handleSelect('date', e)"
              @keyup="(e: Event) => handleSelect('date', e)"
            />
          </div>

          <div class="field-group">
            <label>联系方式（可选）</label>
            <Input
              v-model:value="blocks.signature.contactInfo"
              placeholder="联系电话：${firmPhone}"
              @blur="(e: FocusEvent) => handleBlur('contactInfo', e)"
              @click="(e: Event) => handleSelect('contactInfo', e)"
              @keyup="(e: Event) => handleSelect('contactInfo', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 联系方式将显示在落款区域
              </span>
            </div>
          </div>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 预览提示 -->
    <div class="preview-hint">
      <Alert
        message="💡 格式说明：模板只存储内容和换行，格式在预览和打印时自动应用。系统将自动：标题居中大号显示、编号居中显示、收件单位左对齐、正文段落缩进、落款右对齐"
        type="success"
        show-icon
      />
    </div>
  </div>
</template>

<style scoped>
.structured-editor {
  padding: 8px 0;
}

.block-content {
  padding: 12px 0;
}

.field-group {
  margin-bottom: 16px;
}

.field-group:last-child {
  margin-bottom: 0;
}

.field-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.field-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.field-header label {
  margin-bottom: 0;
}

.variable-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.variables-panel {
  padding: 12px;
  margin-bottom: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
}

.variable-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 8px;
}

.variable-group:last-child {
  margin-bottom: 0;
}

.group-label {
  min-width: 80px;
  font-size: 12px;
  font-weight: 500;
  color: #52c41a;
}

.var-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.var-tag:hover {
  transform: scale(1.05);
}

.preview-hint {
  margin-top: 16px;
}

:deep(.ant-collapse-header) {
  font-weight: 500 !important;
}

:deep(.ant-collapse-content-box) {
  padding: 12px 16px !important;
  background: #fafafa;
}
</style>
