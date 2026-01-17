<script setup lang="ts">
/**
 * 结构化授权委托书模板编辑器
 * 将委托书模板分为多个区块：标题、委托人、受托人、委托事项、签字落款
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

import { decodeHtmlEntities } from '../../../system/contract-template/utils/print-formatter';

const { TextArea: Textarea } = Input;

// Props
const props = defineProps<{
  modelValue: string;
  variables?: Array<{ label: string; value: string; description?: string }>;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 委托书模板分块数据结构
interface TemplateBlocks {
  title: {
    documentTitle: string; // 委托书标题
  };
  client: string; // 委托人信息
  agent: string; // 受托人信息
  matter: string; // 委托事项内容
  signature: {
    clientSign: string; // 委托人签署
    signDate: string; // 签署日期
    remarks: string; // 备注说明
  };
}

const blocks = reactive<TemplateBlocks>({
  title: {
    documentTitle: '授 权 委 托 书',
  },
  client: `\${clients.allInfo}`,
  agent: `\${lawyers.allInfo}`,
  matter: `本人因 \${matter.name}（\${matter.caseTypeName}）一案，特委托上述受托人作为本人的诉讼代理人。

代理阶段：\${matter.litigationStageName}

代理权限类型：\${authorizationType}

\${authorizationScope}

本委托书自签署之日起至本案\${matter.litigationStageName}结案止。`,
  signature: {
    clientSign: '委托人（签章）：________________',
    signDate: '日    期：    年  月  日',
    remarks: `生成日期：\${date.today}
【本授权委托书由系统自动生成，以签字盖章版本为准】`,
  },
});

// 默认展开的面板
const activeKeys = ref(['title', 'client', 'agent', 'matter', 'signature']);

// 记录每个输入框的光标位置（在失去焦点或选择变化时更新）
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

  // 先解码 HTML 实体（处理可能被编码的内容）
  const decoded = decodeHtmlEntities(content);

  // 尝试解析 JSON 格式（结构化存储）
  try {
    const parsed = JSON.parse(decoded);
    // 检查是否是结构化格式
    if (parsed._structured && parsed.blocks) {
      // 新格式：{ _structured: true, blocks: { ... } }
      Object.assign(blocks, parsed.blocks);
      return;
    } else if (
      parsed.title ||
      parsed.client ||
      parsed.agent ||
      parsed.matter ||
      parsed.signature
    ) {
      // 兼容旧格式：直接是 blocks 对象
      Object.assign(blocks, parsed);
      return;
    }
  } catch {
    // 不是 JSON，尝试解析旧格式或设为默认值
  }
}

// 将区块转换为存储格式
function blocksToContent(): string {
  return JSON.stringify({
    _structured: true,
    _version: 1,
    blocks: {
      title: blocks.title,
      client: blocks.client,
      agent: blocks.agent,
      matter: blocks.matter,
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
    | 'documentTitle'
    | 'client'
    | 'agent'
    | 'matter'
    | 'clientSign'
    | 'signDate'
    | 'remarks',
  variable: string,
) {
  const varStr = `\${${variable}}`;

  // 获取当前值
  let currentValue = '';
  if (target === 'documentTitle') {
    currentValue = blocks.title.documentTitle;
  } else if (target === 'client') {
    currentValue = blocks.client;
  } else if (target === 'agent') {
    currentValue = blocks.agent;
  } else if (target === 'matter') {
    currentValue = blocks.matter;
  } else if (target === 'clientSign') {
    currentValue = blocks.signature.clientSign;
  } else if (target === 'signDate') {
    currentValue = blocks.signature.signDate;
  } else if (target === 'remarks') {
    currentValue = blocks.signature.remarks;
  }

  // 获取记录的光标位置，如果没有记录则追加到末尾
  const cursorPos = cursorPositions.value[target] ?? currentValue.length;

  // 在光标位置插入变量
  const newValue =
    currentValue.slice(0, cursorPos) + varStr + currentValue.slice(cursorPos);

  // 更新对应区块的值
  if (target === 'documentTitle') {
    blocks.title.documentTitle = newValue;
  } else if (target === 'client') {
    blocks.client = newValue;
  } else if (target === 'agent') {
    blocks.agent = newValue;
  } else if (target === 'matter') {
    blocks.matter = newValue;
  } else if (target === 'clientSign') {
    blocks.signature.clientSign = newValue;
  } else if (target === 'signDate') {
    blocks.signature.signDate = newValue;
  } else if (target === 'remarks') {
    blocks.signature.remarks = newValue;
  }

  // 更新光标位置（移到插入变量之后）
  cursorPositions.value[target] = cursorPos + varStr.length;
}

// 变量分组 - 按类别明确分类
const variableGroups = computed(() => {
  const groups: Record<string, Array<{ label: string; value: string }>> = {
    委托人信息: [],
    受托人信息: [],
    '项目/案件': [],
    代理权限: [],
    日期: [],
  };

  // 明确的分组映射
  const groupMapping: Record<string, string> = {
    // 委托人信息
    'client.name': '委托人信息',
    'client.idLabel': '委托人信息',
    'client.idNumber': '委托人信息',
    'client.phone': '委托人信息',
    'client.address': '委托人信息',
    'client.legalPerson': '委托人信息',
    'client.typeName': '委托人信息',

    // 受托人信息
    'lawyer.name': '受托人信息',
    'lawyer.licenseNo': '受托人信息',
    'lawyer.phone': '受托人信息',
    'firm.name': '受托人信息',
    'firm.address': '受托人信息',
    'firm.phone': '受托人信息',

    // 项目/案件
    'matter.name': '项目/案件',
    'matter.no': '项目/案件',
    'matter.caseTypeName': '项目/案件',
    'matter.matterTypeName': '项目/案件',
    'matter.causeOfAction': '项目/案件',
    'matter.opposingParty': '项目/案件',
    'matter.description': '项目/案件',
    'matter.litigationStageName': '项目/案件',
    'matter.filingDate': '项目/案件',
    trialStage: '项目/案件',

    // 代理权限
    authorizationType: '代理权限',
    authorizationScope: '代理权限',

    // 多个委托人/受托人
    'clients.allInfo': '委托人信息',
    'clients.allNames': '委托人信息',
    'lawyers.allInfo': '受托人信息',
    'lawyers.allNames': '受托人信息',

    // 日期
    'date.today': '日期',
    'date.year': '日期',
    'date.month': '日期',
    'date.day': '日期',
  };

  props.variables?.forEach((v) => {
    const group = groupMapping[v.value];
    if (group && groups[group]) {
      groups[group].push(v);
    } else {
      // 未映射的变量，根据名称推测分组
      if (v.value.startsWith('client.')) {
        const targetGroup = groups['委托人信息'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.startsWith('lawyer.') || v.value.startsWith('firm.')) {
        const targetGroup = groups['受托人信息'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.startsWith('matter.')) {
        const targetGroup = groups['项目/案件'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (
        v.value.includes('authorization') ||
        v.value.includes('代理')
      ) {
        const targetGroup = groups['代理权限'];
        if (targetGroup) {
          targetGroup.push(v);
        }
      } else if (v.value.startsWith('date.')) {
        const targetGroup = groups['日期'];
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
      <CollapsePanel key="title" header="📌 区块一：标题块">
        <template #extra>
          <Tag color="blue">委托书标题</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>委托书标题（将居中显示为大标题）</label>
            <Textarea
              v-model:value="blocks.title.documentTitle"
              :rows="2"
              placeholder="例如：授权委托书"
              @blur="(e: FocusEvent) => handleBlur('documentTitle', e)"
              @click="(e: Event) => handleSelect('documentTitle', e)"
              @keyup="(e: Event) => handleSelect('documentTitle', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 标题将在打印时居中显示为大标题
              </span>
            </div>
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块2：委托人区 -->
      <CollapsePanel key="client" header="👤 区块二：委托人">
        <template #extra>
          <Tag color="green">委托人信息</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <div class="field-header">
              <label>委托人信息</label>
              <Dropdown>
                <Button size="small" type="link">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <MenuItem
                      v-for="v in variableGroups['委托人信息']"
                      :key="v.value"
                      @click="insertVariable('client', v.value)"
                    >
                      {{ v.label }}
                    </MenuItem>
                  </Menu>
                </template>
              </Dropdown>
            </div>
            <Textarea
              v-model:value="blocks.client"
              :rows="6"
              placeholder="委托人：${client.name}
${client.idLabel}：${client.idNumber}
联系电话：${client.phone}
住所地址：${client.address}"
              @blur="(e: FocusEvent) => handleBlur('client', e)"
              @click="(e: Event) => handleSelect('client', e)"
              @keyup="(e: Event) => handleSelect('client', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块3：受托人区 -->
      <CollapsePanel key="agent" header="⚖️ 区块三：受托人">
        <template #extra>
          <Tag color="orange">受托人信息</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <div class="field-header">
              <label>受托人信息</label>
              <Dropdown>
                <Button size="small" type="link">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <MenuItem
                      v-for="v in variableGroups['受托人信息']"
                      :key="v.value"
                      @click="insertVariable('agent', v.value)"
                    >
                      {{ v.label }}
                    </MenuItem>
                  </Menu>
                </template>
              </Dropdown>
            </div>
            <Textarea
              v-model:value="blocks.agent"
              :rows="6"
              placeholder="受托人：${firm.name}
承办律师：${lawyer.name}
执业证号：${lawyer.licenseNo}
律所地址：${firm.address}"
              @blur="(e: FocusEvent) => handleBlur('agent', e)"
              @click="(e: Event) => handleSelect('agent', e)"
              @keyup="(e: Event) => handleSelect('agent', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块4：委托事项区 -->
      <CollapsePanel key="matter" header="📜 区块四：委托事项">
        <template #extra>
          <Tag color="purple">委托事项</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>委托事项内容（包含案件信息、代理权限、委托期限等）</label>

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
                    @click="insertVariable('matter', v.value)"
                  >
                    {{ v.label }}
                  </Tag>
                </div>
              </template>
            </div>

            <Textarea
              v-model:value="blocks.matter"
              :rows="15"
              placeholder="本人因 ${matter.name}（${matter.caseTypeName}）一案，特委托上述受托人作为本人的诉讼代理人。

代理阶段：${matter.litigationStageName}

代理权限类型：${authorizationType}

${authorizationScope}

本委托书自签署之日起至本案${matter.litigationStageName}结案止。"
              @blur="(e: FocusEvent) => handleBlur('matter', e)"
              @click="(e: Event) => handleSelect('matter', e)"
              @keyup="(e: Event) => handleSelect('matter', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块5：签字落款区 -->
      <CollapsePanel key="signature" header="✍️ 区块五：签字落款">
        <template #extra>
          <Tag color="red">签字盖章</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>委托人签署</label>
            <Textarea
              v-model:value="blocks.signature.clientSign"
              :rows="3"
              placeholder="委托人（签章）：________________"
              @blur="(e: FocusEvent) => handleBlur('clientSign', e)"
              @click="(e: Event) => handleSelect('clientSign', e)"
              @keyup="(e: Event) => handleSelect('clientSign', e)"
            />
          </div>

          <div class="field-group">
            <label>签署日期</label>
            <Input
              v-model:value="blocks.signature.signDate"
              placeholder="日    期：    年  月  日"
              @blur="(e: FocusEvent) => handleBlur('signDate', e)"
              @click="(e: Event) => handleSelect('signDate', e)"
              @keyup="(e: Event) => handleSelect('signDate', e)"
            />
          </div>

          <div class="field-group">
            <label>备注说明</label>
            <Textarea
              v-model:value="blocks.signature.remarks"
              :rows="3"
              placeholder="生成日期：${date.today}
【本授权委托书由系统自动生成，以签字盖章版本为准】"
              @blur="(e: FocusEvent) => handleBlur('remarks', e)"
              @click="(e: Event) => handleSelect('remarks', e)"
              @keyup="(e: Event) => handleSelect('remarks', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 备注信息将以较小字体显示在文档底部
              </span>
            </div>
          </div>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 预览提示 -->
    <div class="preview-hint">
      <Alert
        message="💡 格式说明：模板只存储内容和换行，格式在预览和打印时自动应用。系统将自动：标题居中大号显示、委托人/受托人信息段落化、委托事项段落缩进、签字落款居右对齐"
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
