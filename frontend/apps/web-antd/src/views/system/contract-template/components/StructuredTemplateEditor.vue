<script setup lang="ts">
/**
 * 结构化合同模板编辑器
 * 将合同模板分为四个区块：标题、主体、条款、签署
 * 用户只需关注内容，打印时系统自动排版
 */
import { reactive, watch, computed } from 'vue';

import {
  Alert,
  Collapse,
  CollapsePanel,
  Divider,
  Dropdown,
  Button,
  Input,
  Menu,
  MenuItem,
  Tag,
} from 'ant-design-vue';

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
    contractNo: string; // 合同编号格式
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
    contractNo: '',
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
const activeKeys = ['title', 'parties', 'clauses', 'signature'];

// 解析已有内容到区块
function parseContent(content: string) {
  if (!content) return;

  // 尝试解析 JSON 格式（结构化存储）
  try {
    const parsed = JSON.parse(content);
    if (parsed._structured) {
      Object.assign(blocks, parsed.blocks);
      return;
    }
  } catch {
    // 不是 JSON，尝试解析旧格式或设为条款内容
  }

  // 旧格式内容直接放入条款区
  blocks.clauses = content;
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
  { immediate: true }
);

// 监听区块变化，输出内容
watch(
  blocks,
  () => {
    emit('update:modelValue', blocksToContent());
  },
  { deep: true }
);

// 插入变量
function insertVariable(
  target: 'contractName' | 'contractNo' | 'partyA' | 'partyB' | 'clauses' | 'partyASign' | 'partyBSign' | 'signInfo',
  variable: string
) {
  const varStr = `\${${variable}}`;
  
  if (target === 'contractName') {
    blocks.title.contractName += varStr;
  } else if (target === 'contractNo') {
    blocks.title.contractNo += varStr;
  } else if (target === 'partyA') {
    blocks.parties.partyA += varStr;
  } else if (target === 'partyB') {
    blocks.parties.partyB += varStr;
  } else if (target === 'clauses') {
    blocks.clauses += varStr;
  } else if (target === 'partyASign') {
    blocks.signature.partyASign += varStr;
  } else if (target === 'partyBSign') {
    blocks.signature.partyBSign += varStr;
  } else if (target === 'signInfo') {
    blocks.signature.signInfo += varStr;
  }
}

// 变量分组
const variableGroups = computed(() => {
  const groups: Record<string, Array<{ label: string; value: string }>> = {
    '合同信息': [],
    '委托人/甲方': [],
    '律所/乙方': [],
    '项目/案件': [],
    '其他': [],
  };

  props.variables?.forEach((v) => {
    if (['contractNo', 'contractName', 'signDate', 'effectiveDate', 'expiryDate'].includes(v.value)) {
      groups['合同信息'].push(v);
    } else if (v.value.startsWith('client') || v.value.includes('委托')) {
      groups['委托人/甲方'].push(v);
    } else if (v.value.startsWith('firm') || v.value.includes('律所') || v.value.includes('lawyer')) {
      groups['律所/乙方'].push(v);
    } else if (['matterName', 'matterNo', 'causeOfAction', 'caseType'].includes(v.value)) {
      groups['项目/案件'].push(v);
    } else {
      groups['其他'].push(v);
    }
  });

  return groups;
});
</script>

<template>
  <div class="structured-editor">
    <Alert
      message="📝 结构化模板编辑"
      description="将合同分为四个区块填写，系统会在打印时自动排版。点击变量标签可插入到对应位置。"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <Collapse v-model:activeKey="activeKeys" :bordered="false">
      <!-- 区块1：标题区 -->
      <CollapsePanel key="title" header="📌 区块一：标题区">
        <template #extra>
          <Tag color="blue">合同名称、编号</Tag>
        </template>
        
        <div class="block-content">
          <div class="field-group">
            <label>合同名称（将居中显示为大标题）</label>
            <Textarea
              v-model:value="blocks.title.contractName"
              :rows="2"
              placeholder="例如：委托代理合同、法律顾问合同"
            />
            <div class="variable-hint">
              常用变量：
              <Tag 
                v-for="v in variableGroups['合同信息']?.slice(0, 3)" 
                :key="v.value"
                color="cyan"
                class="var-tag"
                @click="insertVariable('contractName', v.value)"
              >
                {{ v.label }}
              </Tag>
            </div>
          </div>

          <div class="field-group">
            <label>合同编号格式</label>
            <Input
              v-model:value="blocks.title.contractNo"
              placeholder="例如：（${contractYear}）贵威律代字第${contractSeq}号"
            />
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
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <label>合同条款内容（每个条款用"一、""二、"等编号）</label>
              <Dropdown>
                <Button size="small">+ 插入变量</Button>
                <template #overlay>
                  <Menu>
                    <template v-for="(vars, group) in variableGroups" :key="group">
                      <MenuItem v-if="vars.length > 0" disabled style="font-weight: 600; color: #1890ff;">
                        {{ group }}
                      </MenuItem>
                      <MenuItem 
                        v-for="v in vars" 
                        :key="v.value"
                        @click="insertVariable('clauses', v.value)"
                        style="padding-left: 24px;"
                      >
                        {{ v.label }}
                      </MenuItem>
                      <Divider v-if="vars.length > 0" style="margin: 4px 0;" />
                    </template>
                  </Menu>
                </template>
              </Dropdown>
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
              />
            </div>

            <div class="party-section">
              <label>乙方签署</label>
              <Textarea
                v-model:value="blocks.signature.partyBSign"
                :rows="4"
                placeholder="乙方（签章）：${firmName}

负责人：${firmLegalPerson}"
              />
            </div>
          </div>

          <div class="field-group" style="margin-top: 16px;">
            <label>签订日期/地点</label>
            <Input
              v-model:value="blocks.signature.signInfo"
              placeholder="签订日期：${signDate}　　签订地点：${signPlace}"
            />
          </div>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 预览提示 -->
    <div class="preview-hint">
      <Alert
        message="💡 打印时，系统将自动：标题居中大号显示、主体信息表格化、条款段落缩进、签署区左右分栏"
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
