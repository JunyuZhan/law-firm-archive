<script setup lang="ts">
/**
 * 结构化合同模板编辑器
 * 将合同模板分为四个区块：标题、主体、条款、签署
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

import { decodeHtmlEntities } from '../utils/print-formatter';

const { TextArea: Textarea } = Input;

// Props
const props = defineProps<{
  modelValue: string;
  variables?: Array<{ label: string; value: string; description?: string }>;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 四个区块的数据结构
interface TemplateBlocks {
  title: {
    contractName: string; // 合同名称
  };
  parties: {
    partyA: string; // 甲方信息
    partyB: string; // 乙方信息
  };
  clauses: string; // 合同条款内容
  signature: {
    partyASign: string; // 甲方签署
    partyBSign: string; // 乙方签署
    signInfo: string; // 签订日期地点
  };
}

const blocks = reactive<TemplateBlocks>({
  title: {
    contractName: '',
  },
  parties: {
    partyA: '',
    partyB: '',
  },
  clauses: '',
  signature: {
    partyASign: '',
    partyBSign: '',
    signInfo: '',
  },
});

// 默认展开的面板
const activeKeys = ref(['title', 'parties', 'clauses', 'signature']);

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
    if (parsed._structured) {
      Object.assign(blocks, parsed.blocks);
      return;
    }
  } catch {
    // 不是 JSON，尝试解析旧格式或设为条款内容
  }

  // 旧格式内容直接放入条款区（使用解码后的内容）
  blocks.clauses = decoded;
}

// 将区块转换为存储格式
function blocksToContent(): string {
  return JSON.stringify({
    _structured: true,
    _version: 1,
    blocks: {
      title: blocks.title,
      parties: blocks.parties,
      clauses: blocks.clauses,
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
    | 'contractName'
    | 'partyA'
    | 'partyB'
    | 'clauses'
    | 'partyASign'
    | 'partyBSign'
    | 'signInfo',
  variable: string,
) {
  const varStr = `\${${variable}}`;

  // 获取当前值
  let currentValue = '';
  if (target === 'contractName') {
    currentValue = blocks.title.contractName;
  } else if (target === 'partyA') {
    currentValue = blocks.parties.partyA;
  } else if (target === 'partyB') {
    currentValue = blocks.parties.partyB;
  } else if (target === 'clauses') {
    currentValue = blocks.clauses;
  } else if (target === 'partyASign') {
    currentValue = blocks.signature.partyASign;
  } else if (target === 'partyBSign') {
    currentValue = blocks.signature.partyBSign;
  } else if (target === 'signInfo') {
    currentValue = blocks.signature.signInfo;
  }

  // 获取记录的光标位置，如果没有记录则追加到末尾
  const cursorPos = cursorPositions.value[target] ?? currentValue.length;

  // 在光标位置插入变量
  const newValue =
    currentValue.slice(0, cursorPos) + varStr + currentValue.slice(cursorPos);

  // 更新对应区块的值
  if (target === 'contractName') {
    blocks.title.contractName = newValue;
  } else if (target === 'partyA') {
    blocks.parties.partyA = newValue;
  } else if (target === 'partyB') {
    blocks.parties.partyB = newValue;
  } else if (target === 'clauses') {
    blocks.clauses = newValue;
  } else if (target === 'partyASign') {
    blocks.signature.partyASign = newValue;
  } else if (target === 'partyBSign') {
    blocks.signature.partyBSign = newValue;
  } else if (target === 'signInfo') {
    blocks.signature.signInfo = newValue;
  }

  // 更新光标位置（移到插入变量之后）
  cursorPositions.value[target] = cursorPos + varStr.length;
}

// 变量分组 - 按类别明确分类
const variableGroups = computed(() => {
  const groups: Record<string, Array<{ label: string; value: string }>> = {
    合同信息: [],
    '委托人/甲方': [],
    '律所/乙方': [],
    '项目/案件': [],
    '金额/收费': [],
    日期: [],
  };

  // 明确的分组映射
  const groupMapping: Record<string, string> = {
    // 合同信息
    contractNo: '合同信息',
    contractName: '合同信息',
    signDate: '合同信息',
    effectiveDate: '合同信息',
    expiryDate: '合同信息',

    // 委托人/甲方
    clientName: '委托人/甲方',
    clientIdNumber: '委托人/甲方',
    clientAddress: '委托人/甲方',
    clientPhone: '委托人/甲方',
    clientEmail: '委托人/甲方',
    legalRepresentative: '委托人/甲方',
    creditCode: '委托人/甲方',

    // 律所/乙方
    lawyerName: '律所/乙方',
    lawyerLicenseNo: '律所/乙方',
    firmName: '律所/乙方',
    firmAddress: '律所/乙方',
    firmPhone: '律所/乙方',
    firmLegalPerson: '律所/乙方',

    // 项目/案件
    matterName: '项目/案件',
    matterNo: '项目/案件',
    matterDescription: '项目/案件',
    causeOfAction: '项目/案件',
    caseType: '项目/案件',
    trialStage: '项目/案件',
    opposingParty: '项目/案件',
    jurisdictionCourt: '项目/案件',
    caseSummary: '项目/案件',
    procedureStage: '项目/案件',
    authorityScope: '项目/案件',

    // 金额/收费
    totalAmount: '金额/收费',
    totalAmountChinese: '金额/收费',
    totalAmountFormatted: '金额/收费',
    claimAmount: '金额/收费',
    claimAmountChinese: '金额/收费',
    feeType: '金额/收费',
    paymentTerms: '金额/收费',
    riskRatio: '金额/收费',
    advanceTravelFee: '金额/收费',

    // 日期
    currentYear: '日期',
    currentDate: '日期',
  };

  props.variables?.forEach((v) => {
    const group = groupMapping[v.value];
    if (group && groups[group]) {
      groups[group].push(v);
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
      <CollapsePanel key="title" header="📌 区块一：标题区">
        <template #extra>
          <Tag color="blue">合同名称</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>合同名称（将居中显示为大标题）</label>
            <Textarea
              v-model:value="blocks.title.contractName"
              :rows="2"
              placeholder="例如：委托代理合同、法律顾问合同"
              @blur="(e: FocusEvent) => handleBlur('contractName', e)"
              @click="(e: Event) => handleSelect('contractName', e)"
              @keyup="(e: Event) => handleSelect('contractName', e)"
            />
            <div class="variable-hint">
              <span style="font-size: 12px; color: #999">
                💡 合同编号由系统自动生成，打印时自动载入
              </span>
            </div>
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块2：主体区 -->
      <CollapsePanel key="parties" header="👥 区块二：合同主体">
        <template #extra>
          <Tag color="green">甲方乙方信息</Tag>
        </template>

        <div class="block-content">
          <div class="parties-row">
            <div class="party-section">
              <div class="party-header">
                <span class="party-title">甲方（委托人）</span>
                <Dropdown>
                  <Button size="small" type="link">+ 插入变量</Button>
                  <template #overlay>
                    <Menu>
                      <MenuItem
                        v-for="v in variableGroups['委托人/甲方']"
                        :key="v.value"
                        @click="insertVariable('partyA', v.value)"
                      >
                        {{ v.label }}
                      </MenuItem>
                    </Menu>
                  </template>
                </Dropdown>
              </div>
              <Textarea
                v-model:value="blocks.parties.partyA"
                :rows="6"
                placeholder="委托人：${clientName}
住所地：${clientAddress}
身份证号：${clientIdNumber}
联系电话：${clientPhone}"
                @blur="(e: FocusEvent) => handleBlur('partyA', e)"
                @click="(e: Event) => handleSelect('partyA', e)"
                @keyup="(e: Event) => handleSelect('partyA', e)"
              />
            </div>

            <div class="party-section">
              <div class="party-header">
                <span class="party-title">乙方（受托人）</span>
                <Dropdown>
                  <Button size="small" type="link">+ 插入变量</Button>
                  <template #overlay>
                    <Menu>
                      <MenuItem
                        v-for="v in variableGroups['律所/乙方']"
                        :key="v.value"
                        @click="insertVariable('partyB', v.value)"
                      >
                        {{ v.label }}
                      </MenuItem>
                    </Menu>
                  </template>
                </Dropdown>
              </div>
              <Textarea
                v-model:value="blocks.parties.partyB"
                :rows="6"
                placeholder="受托人：${firmName}
负责人：${firmLegalPerson}
地址：${firmAddress}
电话：${firmPhone}"
                @blur="(e: FocusEvent) => handleBlur('partyB', e)"
                @click="(e: Event) => handleSelect('partyB', e)"
                @keyup="(e: Event) => handleSelect('partyB', e)"
              />
            </div>
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块3：条款区 -->
      <CollapsePanel key="clauses" header="📜 区块三：合同条款">
        <template #extra>
          <Tag color="orange">约定内容</Tag>
        </template>

        <div class="block-content">
          <div class="field-group">
            <label>合同条款内容（每个条款用"一、""二、"等编号）</label>

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
                    @click="insertVariable('clauses', v.value)"
                  >
                    {{ v.label }}
                  </Tag>
                </div>
              </template>
            </div>

            <Textarea
              v-model:value="blocks.clauses"
              :rows="15"
              placeholder="一、委托事项
甲方因${matterName}一案，委托乙方提供法律服务。

二、代理权限
乙方指派律师${lawyerName}担任甲方的诉讼代理人，代理权限为：${authorityScope}。

三、律师服务费
甲方应向乙方支付律师服务费人民币${totalAmount}元（大写：${totalAmountChinese}）。

四、付款方式
${paymentTerms}

五、双方权利义务
...

六、违约责任
...

七、争议解决
..."
              @blur="(e: FocusEvent) => handleBlur('clauses', e)"
              @click="(e: Event) => handleSelect('clauses', e)"
              @keyup="(e: Event) => handleSelect('clauses', e)"
            />
          </div>
        </div>
      </CollapsePanel>

      <!-- 区块4：签署区 -->
      <CollapsePanel key="signature" header="✍️ 区块四：签署落款">
        <template #extra>
          <Tag color="purple">签字盖章</Tag>
        </template>

        <div class="block-content">
          <div class="parties-row">
            <div class="party-section">
              <label>甲方签署</label>
              <Textarea
                v-model:value="blocks.signature.partyASign"
                :rows="4"
                placeholder="甲方（签章）：

法定代表人/委托人："
                @blur="(e: FocusEvent) => handleBlur('partyASign', e)"
                @click="(e: Event) => handleSelect('partyASign', e)"
                @keyup="(e: Event) => handleSelect('partyASign', e)"
              />
            </div>

            <div class="party-section">
              <label>乙方签署</label>
              <Textarea
                v-model:value="blocks.signature.partyBSign"
                :rows="4"
                placeholder="乙方（签章）：${firmName}

负责人：${firmLegalPerson}"
                @blur="(e: FocusEvent) => handleBlur('partyBSign', e)"
                @click="(e: Event) => handleSelect('partyBSign', e)"
                @keyup="(e: Event) => handleSelect('partyBSign', e)"
              />
            </div>
          </div>

          <div class="field-group" style="margin-top: 16px">
            <label>签订日期/地点</label>
            <Input
              v-model:value="blocks.signature.signInfo"
              placeholder="签订日期：${signDate}　　签订地点：${signPlace}"
              @blur="(e: FocusEvent) => handleBlur('signInfo', e)"
              @click="(e: Event) => handleSelect('signInfo', e)"
              @keyup="(e: Event) => handleSelect('signInfo', e)"
            />
          </div>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 预览提示 -->
    <div class="preview-hint">
      <Alert
        message="💡 格式说明：模板只存储内容和换行，格式在预览和打印时自动应用。系统将自动：标题居中大号显示、主体信息表格化、条款段落缩进、签署区左右分栏"
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

.field-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
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

.parties-row {
  display: flex;
  gap: 24px;
}

.party-section {
  flex: 1;
}

.party-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.party-title {
  font-weight: 600;
  color: #333;
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
