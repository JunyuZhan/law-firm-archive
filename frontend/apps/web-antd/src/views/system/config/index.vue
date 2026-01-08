<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Form,
  FormItem,
  Textarea,
  Tabs,
  TabPane,
  Tag,
  Alert,
  Select,
  InputNumber,
  Divider,
  Row,
  Col,
  Tooltip,
} from 'ant-design-vue';
import { Copy, Eye, CircleHelp } from '@vben/icons';
import { 
  getSysConfigList, 
  updateConfig, 
  previewContractNumber,
  getContractNumberVariables,
  getRecommendedPatterns,
  getCaseTypeOptions,
} from '#/api/system';
import type { 
  SysConfigDTO, 
  ContractNumberPreview, 
  ContractNumberVariable, 
  ContractNumberPattern,
  CaseTypeOption,
} from '#/api/system/types';
import ConfigModal from './components/ConfigModal.vue';

defineOptions({ name: 'SysConfig' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<SysConfigDTO[]>([]);
const activeTab = ref('general');
const configModalRef = ref<InstanceType<typeof ConfigModal>>();

// 合同编号配置相关
const contractNumberConfig = reactive({
  prefix: 'HT',
  pattern: '{PREFIX}{DATE}{RANDOM}',
  sequenceLength: 4,
});
const previewLoading = ref(false);
const previewResults = ref<ContractNumberPreview[]>([]);
const variables = ref<ContractNumberVariable[]>([]);
const recommendedPatterns = ref<ContractNumberPattern[]>([]);
const caseTypeOptions = ref<CaseTypeOption[]>([]);
const selectedCaseType = ref('');

// 表格列
const columns = [
  { title: '配置名称', dataIndex: 'configName', key: 'configName', width: 200 },
  { title: '配置键', dataIndex: 'configKey', key: 'configKey', width: 250 },
  { title: '配置值', dataIndex: 'configValue', key: 'configValue', ellipsis: true },
  { title: '备注', dataIndex: 'description', key: 'description', ellipsis: true, width: 200 },
  { title: '操作', key: 'action', width: 100 },
];

// 过滤出非合同编号的配置
const generalConfigs = computed(() => {
  return dataSource.value.filter(item => !item.configKey?.startsWith('contract.number.'));
});

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true;
  try {
    dataSource.value = await getSysConfigList();
    initContractNumberConfig();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载配置列表失败');
  } finally {
    loading.value = false;
  }
}

// 初始化合同编号配置
function initContractNumberConfig() {
  const prefixConfig = dataSource.value.find(c => c.configKey === 'contract.number.prefix');
  const patternConfig = dataSource.value.find(c => c.configKey === 'contract.number.pattern');
  const lengthConfig = dataSource.value.find(c => c.configKey === 'contract.number.sequence.length');
  
  if (prefixConfig) contractNumberConfig.prefix = prefixConfig.configValue;
  if (patternConfig) contractNumberConfig.pattern = patternConfig.configValue;
  if (lengthConfig) contractNumberConfig.sequenceLength = parseInt(lengthConfig.configValue) || 4;
}

// 加载合同编号相关数据
async function loadContractNumberData() {
  try {
    const [vars, patterns, caseTypes] = await Promise.all([
      getContractNumberVariables(),
      getRecommendedPatterns(),
      getCaseTypeOptions(),
    ]);
    variables.value = vars;
    recommendedPatterns.value = patterns;
    caseTypeOptions.value = caseTypes;
  } catch (error: unknown) {
    console.error('加载合同编号配置数据失败', error);
  }
}

// ==================== 合同编号操作 ====================

async function handlePreview() {
  previewLoading.value = true;
  try {
    previewResults.value = await previewContractNumber({
      pattern: contractNumberConfig.pattern,
      prefix: contractNumberConfig.prefix,
      sequenceLength: contractNumberConfig.sequenceLength,
      caseType: selectedCaseType.value || undefined,
    });
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '预览失败');
  } finally {
    previewLoading.value = false;
  }
}

function selectRecommendedPattern(pattern: ContractNumberPattern) {
  contractNumberConfig.pattern = pattern.pattern;
  handlePreview();
}

function insertVariable(variable: ContractNumberVariable) {
  contractNumberConfig.pattern += variable.name;
  handlePreview();
}

function copyVariable(variable: ContractNumberVariable) {
  navigator.clipboard.writeText(variable.name);
  message.success(`已复制: ${variable.name}`);
}

async function saveContractNumberConfig() {
  try {
    const prefixConfig = dataSource.value.find(c => c.configKey === 'contract.number.prefix');
    const patternConfig = dataSource.value.find(c => c.configKey === 'contract.number.pattern');
    const lengthConfig = dataSource.value.find(c => c.configKey === 'contract.number.sequence.length');
    
    const promises = [];
    if (prefixConfig) {
      promises.push(updateConfig(prefixConfig.id, { configValue: contractNumberConfig.prefix }));
    }
    if (patternConfig) {
      promises.push(updateConfig(patternConfig.id, { configValue: contractNumberConfig.pattern }));
    }
    if (lengthConfig) {
      promises.push(updateConfig(lengthConfig.id, { configValue: String(contractNumberConfig.sequenceLength) }));
    }
    
    await Promise.all(promises);
    message.success('合同编号配置保存成功');
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  }
}

// ==================== 通用配置操作 ====================

function handleEdit(record: SysConfigDTO) {
  configModalRef.value?.openEdit(record);
}

function handleModalSuccess() {
  fetchData();
}

// ==================== 生命周期 ====================

watch(() => contractNumberConfig.pattern, () => {
  if (contractNumberConfig.pattern) {
    handlePreview();
  }
}, { debounce: 500 } as any);

onMounted(async () => {
  await fetchData();
  await loadContractNumberData();
  handlePreview();
});
</script>

<template>
  <Page title="系统配置" description="管理系统配置参数">
    <Tabs v-model:activeKey="activeTab">
      <!-- 合同编号配置 -->
      <TabPane key="contract" tab="合同编号配置">
        <Card title="编号规则设置" :bordered="false">
          <Row :gutter="24">
            <Col :span="16">
              <Form layout="vertical">
                <FormItem label="编号前缀">
                  <Input 
                    v-model:value="contractNumberConfig.prefix" 
                    placeholder="如：HT、CONTRACT" 
                    style="width: 200px"
                    @change="handlePreview"
                  />
                  <span style="margin-left: 12px; color: #999;">用于 {PREFIX} 变量</span>
                </FormItem>
                
                <FormItem label="序号长度">
                  <InputNumber 
                    v-model:value="contractNumberConfig.sequenceLength" 
                    :min="1" 
                    :max="10"
                    style="width: 120px"
                    @change="handlePreview"
                  />
                  <span style="margin-left: 12px; color: #999;">序号不足位数前面补0，如：0001</span>
                </FormItem>
                
                <FormItem>
                  <template #label>
                    <span>编号规则</span>
                    <Tooltip title="使用变量组合定义编号格式，变量会在生成时被替换为实际值">
                      <CircleHelp class="size-4 ml-1 text-gray-400 inline-block" />
                    </Tooltip>
                  </template>
                  <Textarea 
                    v-model:value="contractNumberConfig.pattern" 
                    :rows="2" 
                    placeholder="如：{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号"
                    style="font-family: monospace; font-size: 14px;"
                  />
                </FormItem>
                
                <FormItem>
                  <Space>
                    <Button type="primary" @click="saveContractNumberConfig">保存配置</Button>
                    <Button @click="handlePreview" :loading="previewLoading">
                      <template #icon><Eye class="size-4" /></template>
                      刷新预览
                    </Button>
                  </Space>
                </FormItem>
              </Form>
            </Col>
            
            <Col :span="8">
              <Card title="编号预览" size="small" :loading="previewLoading">
                <div v-if="previewResults.length">
                  <div v-for="item in previewResults" :key="item.caseType" style="margin-bottom: 12px;">
                    <Tag v-if="item.caseTypeName !== '通用'" color="blue">{{ item.caseTypeName }}</Tag>
                    <div style="font-size: 16px; font-weight: 500; color: #1890ff; margin-top: 4px; font-family: monospace;">
                      {{ item.preview }}
                    </div>
                  </div>
                </div>
                <div v-else style="color: #999;">暂无预览</div>
                
                <Divider style="margin: 16px 0 12px 0;" />
                <div style="color: #999; font-size: 12px;">
                  选择案件类型查看不同类型的编号效果
                </div>
                <Select 
                  v-model:value="selectedCaseType" 
                  placeholder="选择案件类型预览" 
                  style="width: 100%; margin-top: 8px;"
                  allowClear
                  @change="handlePreview"
                >
                  <Select.Option v-for="opt in caseTypeOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }} ({{ opt.shortName }}/{{ opt.code }})
                  </Select.Option>
                </Select>
              </Card>
            </Col>
          </Row>
        </Card>
        
        <Row :gutter="16" style="margin-top: 16px;">
          <Col :span="12">
            <Card title="推荐规则模板" size="small" :bordered="false">
              <div style="max-height: 360px; overflow-y: auto;">
                <div 
                  v-for="pattern in recommendedPatterns" 
                  :key="pattern.name"
                  style="padding: 12px; margin-bottom: 8px; background: #fafafa; border-radius: 6px; cursor: pointer; transition: all 0.2s;"
                  :style="{ background: contractNumberConfig.pattern === pattern.pattern ? '#e6f7ff' : '#fafafa' }"
                  @click="selectRecommendedPattern(pattern)"
                >
                  <div style="display: flex; justify-content: space-between; align-items: center;">
                    <span style="font-weight: 500;">{{ pattern.name }}</span>
                    <Tag color="green">{{ pattern.example }}</Tag>
                  </div>
                  <div style="color: #666; font-size: 12px; margin-top: 4px; font-family: monospace;">
                    {{ pattern.pattern }}
                  </div>
                  <div style="color: #999; font-size: 12px; margin-top: 4px;">
                    {{ pattern.description }}
                  </div>
                </div>
              </div>
            </Card>
          </Col>
          
          <Col :span="12">
            <Card title="支持的变量" size="small" :bordered="false">
              <div style="max-height: 360px; overflow-y: auto;">
                <div 
                  v-for="variable in variables" 
                  :key="variable.name"
                  style="display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0;"
                >
                  <Tag color="blue" style="font-family: monospace; cursor: pointer;" @click="insertVariable(variable)">
                    {{ variable.name }}
                  </Tag>
                  <span style="flex: 1; margin-left: 8px;">
                    <span style="font-weight: 500;">{{ variable.label }}</span>
                    <span style="color: #999; font-size: 12px; margin-left: 8px;">{{ variable.description }}</span>
                  </span>
                  <Tooltip title="复制变量">
                    <Button type="text" size="small" @click="copyVariable(variable)">
                      <template #icon><Copy class="size-4" /></template>
                    </Button>
                  </Tooltip>
                </div>
              </div>
              <Alert 
                type="info" 
                :show-icon="false"
                style="margin-top: 12px;"
                message="点击变量标签可直接插入到编号规则中"
              />
            </Card>
          </Col>
        </Row>
      </TabPane>
      
      <!-- 通用配置 -->
      <TabPane key="general" tab="通用配置">
        <Card :bordered="false">
          <Table
            :columns="columns"
            :data-source="generalConfigs"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record: rawRecord }">
              <template v-if="column.key === 'action'">
                <a @click="handleEdit(rawRecord as SysConfigDTO)">编辑</a>
              </template>
            </template>
          </Table>
        </Card>
      </TabPane>
    </Tabs>

    <!-- 配置弹窗 -->
    <ConfigModal ref="configModalRef" @success="handleModalSuccess" />
  </Page>
</template>

<style scoped>
:deep(.ant-card-head-title) {
  font-size: 14px;
}
</style>
