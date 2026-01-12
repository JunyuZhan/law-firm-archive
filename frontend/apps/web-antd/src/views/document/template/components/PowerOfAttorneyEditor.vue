<script setup lang="ts">
/**
 * 授权委托书模板编辑器
 * 采用分块编辑方式，简化模板管理
 * 排版格式在PDF生成时固定：标题二号宋体，正文三号仿宋，备注五号宋体
 */
import { computed, reactive, ref, watch } from 'vue';

import {
  Alert,
  Button,
  Card,
  Col,
  Collapse,
  CollapsePanel,
  Divider,
  Input,
  Row,
  Space,
  Tag,
  Tooltip,
} from 'ant-design-vue';

// 示例数据，用于预览效果
const sampleData: Record<string, string> = {
  'client.name': '张三',
  'client.idLabel': '身份证号',
  'client.idNumber': '110101199001011234',
  'client.phone': '13800138000',
  'client.address': '北京市朝阳区XX路XX号',
  'client.legalPerson': '李总',
  'firm.name': '北京XX律师事务所',
  'firm.address': '北京市海淀区中关村大街1号',
  'firm.phone': '010-88888888',
  'lawyer.name': '王律师',
  'lawyer.licenseNo': '11101202011234567',
  'lawyer.phone': '13900139000',
  'matter.name': '张三诉李四合同纠纷',
  'matter.caseTypeName': '民事',
  'matter.causeOfAction': '合同纠纷',
  'matter.opposingParty': '李四',
  'matter.litigationStageName': '一审',
  'authorizationType': '特别授权',
  'authorizationScope': '特别授权包括：代为承认、放弃、变更诉讼请求，进行和解，提起反诉或上诉，代为签收法律文书等。',
  'date.today': '2026年1月12日',
  'date.year': '2026',
  'date.month': '01',
  'date.day': '12',
};

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 模板分块数据结构
interface TemplateBlocks {
  title: string; // 标题
  clientInfo: string; // 委托人信息
  agentInfo: string; // 受托人信息
  matterInfo: string; // 委托事项
  authorization: string; // 代理权限
  duration: string; // 委托期限
  signature: string; // 签字区域
  remarks: string; // 备注
}

const blocks = reactive<TemplateBlocks>({
  title: '授 权 委 托 书',
  clientInfo: `委托人：\${client.name}
\${client.idLabel}：\${client.idNumber}
联系电话：\${client.phone}
住所地址：\${client.address}`,
  agentInfo: `受托人：\${firm.name}
承办律师：\${lawyer.name}
执业证号：\${lawyer.licenseNo}
律所地址：\${firm.address}`,
  matterInfo: `本人因 \${matter.name}（\${matter.caseTypeName}）一案，特委托上述受托人作为本人的诉讼代理人。

代理阶段：\${matter.litigationStageName}`,
  authorization: `代理权限类型：\${authorizationType}

\${authorizationScope}`,
  duration: `本委托书自签署之日起至本案\${matter.litigationStageName}结案止。`,
  signature: `委托人（签章）：________________

日    期：    年  月  日`,
  remarks: `生成日期：\${date.today}
【本授权委托书由系统自动生成，以签字盖章版本为准】`,
});

// 各分块可用的变量提示
const blockVariables: Record<keyof TemplateBlocks, Array<{ label: string; value: string; desc: string }>> = {
  title: [],
  clientInfo: [
    { label: '客户名称', value: 'client.name', desc: '委托人姓名' },
    { label: '身份标识', value: 'client.idLabel', desc: '身份证号/统一社会信用代码' },
    { label: '身份号码', value: 'client.idNumber', desc: '身份证号或信用代码' },
    { label: '客户电话', value: 'client.phone', desc: '联系电话' },
    { label: '客户地址', value: 'client.address', desc: '联系地址' },
    { label: '法定代表人', value: 'client.legalPerson', desc: '企业法定代表人' },
  ],
  agentInfo: [
    { label: '律所名称', value: 'firm.name', desc: '律师事务所全称' },
    { label: '律所地址', value: 'firm.address', desc: '律师事务所地址' },
    { label: '律所电话', value: 'firm.phone', desc: '律师事务所电话' },
    { label: '承办律师', value: 'lawyer.name', desc: '承办律师姓名' },
    { label: '执业证号', value: 'lawyer.licenseNo', desc: '承办律师执业证号' },
    { label: '律师电话', value: 'lawyer.phone', desc: '律师联系电话' },
  ],
  matterInfo: [
    { label: '项目名称', value: 'matter.name', desc: '委托项目/案件名称' },
    { label: '案件类型', value: 'matter.caseTypeName', desc: '民事/刑事/行政等' },
    { label: '案由', value: 'matter.causeOfAction', desc: '案件案由' },
    { label: '对方当事人', value: 'matter.opposingParty', desc: '对方当事人姓名' },
    { label: '代理阶段', value: 'matter.litigationStageName', desc: '一审/二审/再审等' },
  ],
  authorization: [
    { label: '代理权限类型', value: 'authorizationType', desc: '一般代理/特别代理' },
    { label: '代理权限范围', value: 'authorizationScope', desc: '代理权限详细描述' },
  ],
  duration: [
    { label: '代理阶段', value: 'matter.litigationStageName', desc: '一审/二审/再审等' },
  ],
  signature: [],
  remarks: [
    { label: '当前日期', value: 'date.today', desc: '当前完整日期' },
    { label: '当前年份', value: 'date.year', desc: '当前年份' },
    { label: '当前月份', value: 'date.month', desc: '当前月份' },
    { label: '当前日', value: 'date.day', desc: '当前日' },
  ],
};

// 分块配置
const blockConfigs = [
  { key: 'title', label: '标题', format: '二号宋体，居中', rows: 1 },
  { key: 'clientInfo', label: '委托人信息', format: '三号仿宋', rows: 4 },
  { key: 'agentInfo', label: '受托人信息', format: '三号仿宋', rows: 4 },
  { key: 'matterInfo', label: '委托事项', format: '三号仿宋', rows: 4 },
  { key: 'authorization', label: '代理权限', format: '三号仿宋', rows: 4 },
  { key: 'duration', label: '委托期限', format: '三号仿宋', rows: 2 },
  { key: 'signature', label: '签字区域', format: '三号仿宋', rows: 3 },
  { key: 'remarks', label: '备注说明', format: '五号宋体', rows: 2 },
] as const;

// 解析传入的内容到分块
function parseContentToBlocks(content: string): void {
  if (!content) return;
  
  try {
    // 尝试解析JSON格式的分块内容
    const parsed = JSON.parse(content);
    if (parsed && typeof parsed === 'object') {
      Object.keys(blocks).forEach((key) => {
        if (parsed[key] !== undefined) {
          blocks[key as keyof TemplateBlocks] = parsed[key];
        }
      });
      return;
    }
  } catch {
    // 非JSON格式，尝试解析旧的纯文本格式
    // 保持默认值
  }
}

// 将分块组合成JSON字符串输出
function blocksToContent(): string {
  return JSON.stringify(blocks);
}

// 初始化：解析传入的内容
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      parseContentToBlocks(val);
    }
  },
  { immediate: true }
);

// 监听分块变化，输出内容
watch(
  blocks,
  () => {
    emit('update:modelValue', blocksToContent());
  },
  { deep: true }
);

// 插入变量到指定分块
function insertVariable(blockKey: keyof TemplateBlocks, variable: string) {
  blocks[blockKey] += `\${${variable}}`;
}

// 默认展开的面板
const activeKeys = ref(['title', 'clientInfo', 'agentInfo', 'matterInfo', 'authorization']);

// 预览模式
const showPreview = ref(false);

// 替换变量生成预览内容
function replaceVariables(text: string): string {
  if (!text) return '';
  return text.replace(/\$\{([^}]+)\}/g, (match, key) => {
    return sampleData[key] || `【${key}】`;
  });
}

// 预览内容
const previewContent = computed(() => {
  const sections = [
    { label: '', content: blocks.title, isTitle: true },
    { label: '【委托人信息】', content: blocks.clientInfo },
    { label: '【受托人信息】', content: blocks.agentInfo },
    { label: '【委托事项】', content: blocks.matterInfo },
    { label: '【代理权限】', content: blocks.authorization },
    { label: '【委托期限】', content: blocks.duration },
    { label: '【签字确认】', content: blocks.signature },
    { label: '━'.repeat(30), content: '' },
    { label: '', content: blocks.remarks, isRemarks: true },
  ];
  
  return sections.map(s => ({
    ...s,
    content: replaceVariables(s.content),
  }));
});
</script>

<template>
  <div class="poa-editor">
    <!-- 工具栏 -->
    <div class="toolbar">
      <Button 
        :type="showPreview ? 'default' : 'primary'" 
        @click="showPreview = false"
      >
        📝 编辑模式
      </Button>
      <Button 
        :type="showPreview ? 'primary' : 'default'" 
        @click="showPreview = true"
        style="margin-left: 8px"
      >
        👁️ 预览效果
      </Button>
      <span style="margin-left: 16px; color: #999; font-size: 12px">
        {{ showPreview ? '预览使用示例数据填充变量' : '编辑各信息块内容' }}
      </span>
    </div>

    <!-- 预览模式 -->
    <div v-if="showPreview" class="preview-mode">
      <Alert
        message="以下为模板预览效果（使用示例数据）"
        type="success"
        show-icon
        style="margin-bottom: 16px"
      />
      <div class="preview-paper">
        <template v-for="(section, idx) in previewContent" :key="idx">
          <h1 v-if="section.isTitle" class="preview-title">{{ section.content }}</h1>
          <template v-else>
            <div v-if="section.label && !section.label.startsWith('━')" class="preview-section-title">{{ section.label }}</div>
            <div v-if="section.label.startsWith('━')" class="preview-divider">{{ section.label }}</div>
            <div 
              v-if="section.content" 
              :class="['preview-content', { 'preview-remarks': section.isRemarks }]"
            >
              <p v-for="(line, lineIdx) in section.content.split('\n')" :key="lineIdx">{{ line }}</p>
            </div>
          </template>
        </template>
      </div>
    </div>

    <!-- 编辑模式 -->
    <div v-else>
      <!-- 编辑说明 -->
      <Alert
        type="info"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #message>
          <div style="font-weight: 500">授权委托书模板编辑说明</div>
        </template>
        <template #description>
          <div style="font-size: 13px; line-height: 1.8">
            <p style="margin: 4px 0">• 模板按信息块编辑，<strong>只需填写内容</strong>，排版格式在PDF生成时自动应用</p>
            <p style="margin: 4px 0">• 使用 <Tag color="blue" style="margin: 0 2px">${变量名}</Tag> 格式插入变量，生成时自动替换为项目实际数据</p>
            <p style="margin: 4px 0">• 若变量对应的数据为空，生成时会显示为下划线（如：________________）</p>
          </div>
        </template>
      </Alert>

    <!-- 格式预览提示 -->
    <Card size="small" style="margin-bottom: 16px; background: #fafafa">
      <template #title>
        <span style="font-size: 13px; color: #666">📄 PDF生成格式说明</span>
      </template>
      <Row :gutter="[16, 8]">
        <Col :span="8">
          <Tag color="orange">标题</Tag> 二号宋体，居中
        </Col>
        <Col :span="8">
          <Tag color="blue">正文</Tag> 三号仿宋
        </Col>
        <Col :span="8">
          <Tag color="default">备注</Tag> 五号宋体
        </Col>
      </Row>
    </Card>

    <!-- 分块编辑区域 -->
    <Collapse v-model:activeKey="activeKeys">
      <CollapsePanel
        v-for="config in blockConfigs"
        :key="config.key"
        :header="`${config.label}`"
      >
        <template #extra>
          <Tag :color="config.key === 'title' ? 'orange' : config.key === 'remarks' ? 'default' : 'blue'" style="margin-right: 0">
            {{ config.format }}
          </Tag>
        </template>

        <!-- 可用变量 -->
        <div v-if="blockVariables[config.key].length > 0" class="variable-hints">
          <span class="hint-label">可用变量：</span>
          <Space wrap :size="4">
            <Tooltip v-for="v in blockVariables[config.key]" :key="v.value" :title="v.desc">
              <Tag
                color="processing"
                class="variable-tag"
                @click="insertVariable(config.key, v.value)"
              >
                {{ v.label }}
              </Tag>
            </Tooltip>
          </Space>
        </div>

        <!-- 编辑区域 -->
        <Input.TextArea
          v-model:value="blocks[config.key]"
          :rows="config.rows"
          :placeholder="`请输入${config.label}内容...`"
          style="font-family: 'Microsoft YaHei', monospace"
        />

        <!-- 示例提示 -->
        <div v-if="config.key === 'clientInfo'" class="example-hint">
          <strong>示例：</strong>
          <pre>委托人：张三
身份证号：110101199001011234
联系电话：13800138000
住所地址：北京市朝阳区XX路XX号</pre>
        </div>
        <div v-if="config.key === 'agentInfo'" class="example-hint">
          <strong>示例：</strong>
          <pre>受托人：北京XX律师事务所
承办律师：李四
执业证号：11101202011234567
律所地址：北京市海淀区XX路XX号</pre>
        </div>
        <div v-if="config.key === 'matterInfo'" class="example-hint">
          <strong>示例：</strong>
          <pre>本人因 张三诉李四合同纠纷（民事）一案，特委托上述受托人作为本人的诉讼代理人。

代理阶段：一审</pre>
        </div>
        <div v-if="config.key === 'authorization'" class="example-hint">
          <strong>示例：</strong>
          <pre>代理权限类型：特别授权

特别授权包括：代为承认、放弃、变更诉讼请求，进行和解，提起反诉或上诉，代为签收法律文书等。</pre>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 变量使用说明 -->
    <Divider style="margin: 16px 0 12px" />
    <div class="usage-tips">
      <div class="tip-title">💡 变量使用提示</div>
      <ul class="tip-list">
        <li>点击分块区域上方的变量标签，可快速插入变量</li>
        <li>变量格式：<code>${client.name}</code>、<code>${lawyer.name}</code> 等</li>
        <li>生成PDF时，系统会自动从项目、客户、律师等数据中获取实际值</li>
        <li>数据为空时，变量会被替换为下划线，方便手工填写</li>
      </ul>
    </div>
    </div><!-- 编辑模式结束 -->
  </div>
</template>

<style scoped>
.poa-editor {
  padding: 8px 0;
}

.toolbar {
  padding: 12px 16px;
  margin-bottom: 16px;
  background: linear-gradient(to right, #f0f5ff, #e6f0ff);
  border-radius: 8px;
}

/* 预览模式样式 */
.preview-mode {
  padding: 0;
}

.preview-paper {
  max-height: 500px;
  padding: 40px 60px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgb(0 0 0 / 10%);
}

.preview-title {
  margin-bottom: 30px;
  font-family: SimSun, serif;
  font-size: 22px;
  font-weight: bold;
  text-align: center;
}

.preview-section-title {
  margin-top: 20px;
  margin-bottom: 10px;
  font-family: SimSun, serif;
  font-size: 14px;
  font-weight: bold;
}

.preview-content {
  font-family: FangSong, STFangsong, serif;
  font-size: 16px;
  line-height: 2;
}

.preview-content p {
  margin: 0;
}

.preview-divider {
  margin: 20px 0;
  font-size: 10px;
  color: #999;
  text-align: center;
}

.preview-remarks {
  font-family: SimSun, serif;
  font-size: 10.5px;
  color: #666;
}

.variable-hints {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: linear-gradient(to right, #e6f4ff, #f0f7ff);
  border-radius: 4px;
}

.hint-label {
  margin-right: 8px;
  font-size: 12px;
  color: #666;
}

.variable-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.variable-tag:hover {
  box-shadow: 0 2px 4px rgb(24 144 255 / 30%);
  transform: scale(1.05);
}

.example-hint {
  padding: 8px 12px;
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  background: #fafafa;
  border-left: 3px solid #1890ff;
  border-radius: 0 4px 4px 0;
}

.example-hint pre {
  margin: 4px 0 0;
  font-family: 'Microsoft YaHei', monospace;
  font-size: 12px;
  white-space: pre-wrap;
}

.usage-tips {
  padding: 12px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
}

.tip-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #d48806;
}

.tip-list {
  padding-left: 20px;
  margin: 0;
  font-size: 12px;
  line-height: 1.8;
  color: #666;
}

.tip-list code {
  padding: 1px 4px;
  font-family: monospace;
  background: #f5f5f5;
  border-radius: 2px;
}
</style>
