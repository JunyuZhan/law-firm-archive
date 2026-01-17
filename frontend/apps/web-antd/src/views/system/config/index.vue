<script setup lang="ts">
import type {
  CaseTypeOption,
  ContractNumberPattern,
  ContractNumberPreview,
  ContractNumberVariable,
  SysConfigDTO,
} from '#/api/system/types';

import {
  computed,
  defineAsyncComponent,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';

import { Page } from '@vben/common-ui';
import { CircleHelp, Copy, Eye } from '@vben/icons';

import {
  Alert,
  Modal as AModal,
  Button,
  Card,
  Col,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Row,
  Select,
  Space,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
  Tooltip,
  Popconfirm,
} from 'ant-design-vue';

import {
  createConfig,
  deleteConfig,
  disableMaintenanceMode,
  enableMaintenanceMode,
  getCaseTypeOptions,
  getConfigValue,
  getContractNumberVariables,
  getEmailStatus,
  getMaintenanceStatus,
  getRecommendedPatterns,
  getSysConfigList,
  getVersionInfo,
  previewContractNumber,
  previewSystemReport,
  sendSystemReport,
  sendTestAlert,
  testEmailConfig,
  updateConfig,
} from '#/api/system';
import { getWecomStatus, testWecomBot } from '#/api/system/wecom';

import ConfigModal from './components/ConfigModal.vue';

// 懒加载组件，提高首次加载速度
const CauseOfActionTab = defineAsyncComponent(
  () => import('./components/CauseOfActionTab.vue'),
);
const AiBillingTab = defineAsyncComponent(
  () => import('./components/AiBillingTab.vue'),
);

defineOptions({ name: 'SysConfig' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<SysConfigDTO[]>([]);
const activeTab = ref('general');
const configModalRef = ref<InstanceType<typeof ConfigModal>>();
const tableKey = ref(0); // 用于强制 Table 重新渲染

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

// 维护模式相关
const maintenanceStatus = ref({ enabled: false, message: '' });
const maintenanceLoading = ref(false);
const maintenanceMessage = ref('');

// 版本信息相关
const versionInfo = ref<{
  buildTime: string;
  buildVersion?: string; // 构建版本号（如 1.0.0-SNAPSHOT）
  gitCommit: string;
  javaVendor: string;
  javaVersion: string;
  osName: string;
  osVersion: string;
  profile: string;
  serverTime: string;
  version: string; // 显示版本号（优先数据库配置，支持简单格式如 0.4）
} | null>(null);
const versionLoading = ref(false);

// 邮件通知相关
const emailStatus = ref({ enabled: false });
const emailTestLoading = ref(false);
const testEmailAddress = ref('');
const reportPreviewLoading = ref(false);
const reportPreviewHtml = ref('');
const reportPreviewVisible = ref(false);

// 企业微信机器人相关
const wecomStatus = ref({ enabled: false });
const wecomTestLoading = ref(false);
const wecomWebhookUrl = ref('');

// 表格列 - 移除固定宽度，让列自适应
const columns = [
  {
    title: '配置名称',
    dataIndex: 'configName',
    key: 'configName',
    ellipsis: true,
  },
  { title: '配置键', dataIndex: 'configKey', key: 'configKey', ellipsis: true },
  {
    title: '配置值',
    dataIndex: 'configValue',
    key: 'configValue',
    ellipsis: true,
  },
  {
    title: '备注',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true,
  },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
];

// 配置分组优先级（数字越小越靠前）
const configGroupOrder: Record<string, number> = {
  // 1. 系统基础信息
  'sys.name': 1,
  'sys.version': 2,
  'sys.copyright': 3,

  // 2. 登录安全配置
  'sys.login.captcha': 10,
  'sys.login.maxAttempts': 11,
  'sys.login.lockDuration': 12,

  // 3. 密码配置
  'sys.password.minLength': 20,
  'sys.password.complexity': 21,

  // 4. 会话配置
  'sys.session.timeout': 30,

  // 5. 维护模式
  'sys.maintenance.enabled': 40,
  'sys.maintenance.message': 41,

  // 6. 上传配置
  'sys.upload.maxSize': 50,
  'sys.upload.allowTypes': 51,

  // 7. 邮件通知配置
  'notification.email.enabled': 60,
  'notification.email.smtp.host': 61,
  'notification.email.smtp.port': 62,
  'notification.email.smtp.username': 63,
  'notification.email.smtp.password': 64,
  'notification.email.admin.recipients': 65,

  // 8. 告警配置
  'notification.alert.login.failure': 70,
  'notification.alert.account.locked': 71,
  'notification.alert.system.error': 72,
  'notification.alert.disk.space': 73,
  'notification.alert.backup.failure': 74,

  // 9. 定时报告配置
  'notification.report.daily.enabled': 80,
  'notification.report.weekly.enabled': 81,
};

// 获取配置项的排序权重
function getConfigSortWeight(key: string): number {
  // 精确匹配
  if (configGroupOrder[key] !== undefined) {
    return configGroupOrder[key];
  }
  // 按前缀分组
  if (key.startsWith('sys.')) return 100;
  if (key.startsWith('notification.')) return 150;
  if (key.startsWith('ai.')) return 200;
  if (key.startsWith('ocr.')) return 300;
  if (key.startsWith('backup.')) return 400;
  if (key.startsWith('work.')) return 500;
  if (key.startsWith('approval.')) return 600;
  if (key.startsWith('finance.')) return 700;
  // 其他配置
  return 1000;
}

// 过滤出非合同编号的配置，并按功能模块分组排序
const generalConfigs = computed(() => {
  const filtered = dataSource.value.filter(
    (item) => !item.configKey?.startsWith('contract.number.'),
  );

  // 按分组排序
  return filtered.sort((a, b) => {
    const keyA = a.configKey || '';
    const keyB = b.configKey || '';

    const weightA = getConfigSortWeight(keyA);
    const weightB = getConfigSortWeight(keyB);

    // 先按权重排序
    if (weightA !== weightB) {
      return weightA - weightB;
    }

    // 同一组内按字母顺序
    return keyA.localeCompare(keyB);
  });
});

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true;
  try {
    const newData = await getSysConfigList();
    // 直接替换数组引用，确保响应式更新
    dataSource.value = [...newData];
    // 更新 tableKey 强制 Table 重新渲染
    await nextTick();
    tableKey.value = Date.now();
    await nextTick();
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
  const prefixConfig = dataSource.value.find(
    (c) => c.configKey === 'contract.number.prefix',
  );
  const patternConfig = dataSource.value.find(
    (c) => c.configKey === 'contract.number.pattern',
  );
  const lengthConfig = dataSource.value.find(
    (c) => c.configKey === 'contract.number.sequence.length',
  );

  if (prefixConfig) contractNumberConfig.prefix = prefixConfig.configValue;
  if (patternConfig) contractNumberConfig.pattern = patternConfig.configValue;
  if (lengthConfig)
    contractNumberConfig.sequenceLength =
      Number.parseInt(lengthConfig.configValue) || 4;
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

// 加载版本信息
async function loadVersionInfo() {
  versionLoading.value = true;
  try {
    versionInfo.value = await getVersionInfo();
  } catch (error: unknown) {
    console.error('加载版本信息失败', error);
    message.error('加载版本信息失败');
  } finally {
    versionLoading.value = false;
  }
}

// 加载邮件服务状态
async function loadEmailStatus() {
  try {
    emailStatus.value = await getEmailStatus();
  } catch (error: unknown) {
    console.error('加载邮件服务状态失败', error);
  }
}

// 测试邮件配置
async function handleTestEmail() {
  if (!testEmailAddress.value) {
    message.warning('请输入测试邮箱地址');
    return;
  }
  emailTestLoading.value = true;
  try {
    const result = await testEmailConfig(testEmailAddress.value);
    message.success(result || '测试邮件已发送');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '发送测试邮件失败');
  } finally {
    emailTestLoading.value = false;
  }
}

// 发送测试告警
async function handleSendTestAlert(type: string) {
  try {
    await sendTestAlert(type);
    message.success('测试告警已发送');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '发送测试告警失败');
  }
}

// 预览系统报告
async function handlePreviewReport(type: 'daily' | 'weekly') {
  reportPreviewLoading.value = true;
  try {
    reportPreviewHtml.value = await previewSystemReport(type);
    reportPreviewVisible.value = true;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '生成报告预览失败');
  } finally {
    reportPreviewLoading.value = false;
  }
}

// 立即发送系统报告
async function handleSendReport(type: 'daily' | 'weekly') {
  try {
    await sendSystemReport(type);
    message.success('系统报告已发送');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '发送系统报告失败');
  }
}

// ==================== 企业微信机器人操作 ====================

// 加载企业微信状态
async function loadWecomStatus() {
  try {
    wecomStatus.value = await getWecomStatus();
    // 从配置中读取 webhook URL
    const webhookConfig = dataSource.value.find(
      (c) => c.configKey === 'wecom.bot.webhook',
    );
    if (webhookConfig) {
      wecomWebhookUrl.value = webhookConfig.configValue || '';
    }
  } catch (error: unknown) {
    console.error('加载企业微信状态失败', error);
  }
}

// 保存企业微信 Webhook 配置
async function handleSaveWecomWebhook() {
  if (!wecomWebhookUrl.value) {
    message.warning('请输入 Webhook 地址');
    return;
  }
  try {
    // 先检查配置是否存在
    let existingConfig = null;
    try {
      existingConfig = await getConfigValue('wecom.bot.webhook');
    } catch {
      // 配置不存在，忽略错误
    }

    if (existingConfig?.id) {
      // 更新现有配置
      await updateConfig(existingConfig.id, {
        configValue: wecomWebhookUrl.value,
        description: '企业微信群机器人的 Webhook 地址',
      });
    } else {
      // 创建新配置
      await createConfig({
        configKey: 'wecom.bot.webhook',
        configName: '企业微信机器人Webhook',
        configValue: wecomWebhookUrl.value,
        description: '企业微信群机器人的 Webhook 地址',
      });
    }
    message.success('Webhook 配置已保存');
    await fetchData();
    await loadWecomStatus();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  }
}

// 测试企业微信机器人
async function handleTestWecomBot() {
  if (!wecomWebhookUrl.value) {
    message.warning('请先配置 Webhook 地址');
    return;
  }
  wecomTestLoading.value = true;
  try {
    const result = await testWecomBot();
    if (result.success) {
      message.success('测试消息已发送，请在企业微信群中查看');
    } else {
      message.error(result.message || '测试失败');
    }
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '测试失败');
  } finally {
    wecomTestLoading.value = false;
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
    const prefixConfig = dataSource.value.find(
      (c) => c.configKey === 'contract.number.prefix',
    );
    const patternConfig = dataSource.value.find(
      (c) => c.configKey === 'contract.number.pattern',
    );
    const lengthConfig = dataSource.value.find(
      (c) => c.configKey === 'contract.number.sequence.length',
    );

    const promises = [];
    if (prefixConfig) {
      promises.push(
        updateConfig(prefixConfig.id, {
          configValue: contractNumberConfig.prefix,
        }),
      );
    }
    if (patternConfig) {
      promises.push(
        updateConfig(patternConfig.id, {
          configValue: contractNumberConfig.pattern,
        }),
      );
    }
    if (lengthConfig) {
      promises.push(
        updateConfig(lengthConfig.id, {
          configValue: String(contractNumberConfig.sequenceLength),
        }),
      );
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

function handleCreate() {
  configModalRef.value?.openCreate();
}

function handleEdit(record: SysConfigDTO) {
  configModalRef.value?.openEdit(record);
}

async function handleDelete(record: SysConfigDTO) {
  try {
    await deleteConfig(record.id);
    message.success('删除成功');
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

async function handleModalSuccess() {
  // 强制刷新列表数据
  await fetchData();
  // 等待 DOM 更新完成
  await nextTick();
  // 再次更新 tableKey 确保表格重新渲染
  tableKey.value = Date.now();
  await nextTick();
}

// ==================== 生命周期 ====================

// 监听 dataSource 变化，强制更新表格
watch(
  () => dataSource.value,
  () => {
    nextTick(() => {
      tableKey.value = Date.now();
    });
  },
  { deep: true, flush: 'post' },
);

watch(
  () => contractNumberConfig.pattern,
  () => {
    if (contractNumberConfig.pattern) {
      handlePreview();
    }
  },
  { debounce: 500 } as any,
);

// 加载维护模式状态
async function loadMaintenanceStatus() {
  try {
    const res = await getMaintenanceStatus();
    maintenanceStatus.value = res;
    maintenanceMessage.value = res.message || '系统正在维护中，请稍后再试';
  } catch (error: any) {
    console.error('加载维护模式状态失败', error);
  }
}

// 开启维护模式
async function handleEnableMaintenance() {
  if (!maintenanceMessage.value.trim()) {
    message.warning('请输入维护提示信息');
    return;
  }
  maintenanceLoading.value = true;
  try {
    await enableMaintenanceMode(maintenanceMessage.value);
    message.success('维护模式已开启');
    await loadMaintenanceStatus();
  } catch (error: any) {
    message.error(error.message || '开启维护模式失败');
  } finally {
    maintenanceLoading.value = false;
  }
}

// 关闭维护模式
async function handleDisableMaintenance() {
  maintenanceLoading.value = true;
  try {
    await disableMaintenanceMode();
    message.success('维护模式已关闭');
    await loadMaintenanceStatus();
  } catch (error: any) {
    message.error(error.message || '关闭维护模式失败');
  } finally {
    maintenanceLoading.value = false;
  }
}

onMounted(async () => {
  await fetchData();
  await loadContractNumberData();
  handlePreview();
  await loadMaintenanceStatus();
  await loadVersionInfo();
  await loadEmailStatus();
  await loadWecomStatus();
});

// 监听标签页切换，切换到版本信息时刷新
watch(activeTab, (newTab) => {
  if (newTab === 'version') {
    loadVersionInfo();
  }
  if (newTab === 'notification') {
    loadEmailStatus();
  }
});
</script>

<template>
  <Page title="系统配置" description="管理系统配置参数">
    <Tabs v-model:active-key="activeTab">
      <!-- 合同编号配置 -->
      <TabPane key="contract" tab="合同编号配置">
        <Card title="编号规则设置" :bordered="false">
          <Row :gutter="[24, 16]">
            <Col :xs="24" :lg="16">
              <Form layout="vertical">
                <FormItem label="编号前缀">
                  <Input
                    v-model:value="contractNumberConfig.prefix"
                    placeholder="如：HT、CONTRACT"
                    style="width: 200px"
                    @change="handlePreview"
                  />
                  <span style="margin-left: 12px; color: #999"
                    >用于 {PREFIX} 变量</span
                  >
                </FormItem>

                <FormItem label="序号长度">
                  <InputNumber
                    v-model:value="contractNumberConfig.sequenceLength"
                    :min="1"
                    :max="10"
                    style="width: 120px"
                    @change="handlePreview"
                  />
                  <span style="margin-left: 12px; color: #999"
                    >序号不足位数前面补0，如：0001</span
                  >
                </FormItem>

                <FormItem>
                  <template #label>
                    <span>编号规则</span>
                    <Tooltip
                      title="使用变量组合定义编号格式，变量会在生成时被替换为实际值"
                    >
                      <CircleHelp
                        class="ml-1 inline-block size-4 text-gray-400"
                      />
                    </Tooltip>
                  </template>
                  <Textarea
                    v-model:value="contractNumberConfig.pattern"
                    :rows="2"
                    placeholder="如：{YEAR}{CASE_TYPE}代字第{SEQUENCE_YEAR}号"
                    style="font-family: monospace; font-size: 14px"
                  />
                </FormItem>

                <FormItem>
                  <Space>
                    <Button type="primary" @click="saveContractNumberConfig">
                      保存配置
                    </Button>
                    <Button @click="handlePreview" :loading="previewLoading">
                      <template #icon><Eye class="size-4" /></template>
                      刷新预览
                    </Button>
                  </Space>
                </FormItem>
              </Form>
            </Col>

            <Col :xs="24" :lg="8">
              <Card title="编号预览" size="small" :loading="previewLoading">
                <div v-if="previewResults.length > 0">
                  <div
                    v-for="item in previewResults"
                    :key="item.caseType"
                    style="margin-bottom: 12px"
                  >
                    <Tag v-if="item.caseTypeName !== '通用'" color="blue">
                      {{ item.caseTypeName }}
                    </Tag>
                    <div
                      style="
                        margin-top: 4px;
                        font-family: monospace;
                        font-size: 16px;
                        font-weight: 500;
                        color: #1890ff;
                      "
                    >
                      {{ item.preview }}
                    </div>
                  </div>
                </div>
                <div v-else style="color: #999">暂无预览</div>

                <Divider style="margin: 16px 0 12px" />
                <div style="font-size: 12px; color: #999">
                  选择案件类型查看不同类型的编号效果
                </div>
                <Select
                  v-model:value="selectedCaseType"
                  placeholder="选择案件类型预览"
                  style="width: 100%; margin-top: 8px"
                  allow-clear
                  @change="handlePreview"
                >
                  <Select.Option
                    v-for="opt in caseTypeOptions"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }} ({{ opt.shortName }}/{{ opt.code }})
                  </Select.Option>
                </Select>
              </Card>
            </Col>
          </Row>
        </Card>

        <Row :gutter="[16, 16]" style="margin-top: 16px">
          <Col :xs="24" :lg="12">
            <Card title="推荐规则模板" size="small" :bordered="false">
              <div style="max-height: 360px; overflow-y: auto">
                <div
                  v-for="pattern in recommendedPatterns"
                  :key="pattern.name"
                  style="
                    padding: 12px;
                    margin-bottom: 8px;
                    cursor: pointer;
                    background: #fafafa;
                    border-radius: 6px;
                    transition: all 0.2s;
                  "
                  :style="{
                    background:
                      contractNumberConfig.pattern === pattern.pattern
                        ? '#e6f7ff'
                        : '#fafafa',
                  }"
                  @click="selectRecommendedPattern(pattern)"
                >
                  <div
                    style="
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                    "
                  >
                    <span style="font-weight: 500">{{ pattern.name }}</span>
                    <Tag color="green">{{ pattern.example }}</Tag>
                  </div>
                  <div
                    style="
                      margin-top: 4px;
                      font-family: monospace;
                      font-size: 12px;
                      color: #666;
                    "
                  >
                    {{ pattern.pattern }}
                  </div>
                  <div style="margin-top: 4px; font-size: 12px; color: #999">
                    {{ pattern.description }}
                  </div>
                </div>
              </div>
            </Card>
          </Col>

          <Col :xs="24" :lg="12">
            <Card title="支持的变量" size="small" :bordered="false">
              <div style="max-height: 360px; overflow-y: auto">
                <div
                  v-for="variable in variables"
                  :key="variable.name"
                  style="
                    display: flex;
                    align-items: center;
                    padding: 8px 0;
                    border-bottom: 1px solid #f0f0f0;
                  "
                >
                  <Tag
                    color="blue"
                    style="font-family: monospace; cursor: pointer"
                    @click="insertVariable(variable)"
                  >
                    {{ variable.name }}
                  </Tag>
                  <span style="flex: 1; margin-left: 8px">
                    <span style="font-weight: 500">{{ variable.label }}</span>
                    <span
                      style="margin-left: 8px; font-size: 12px; color: #999"
                      >{{ variable.description }}</span
                    >
                  </span>
                  <Tooltip title="复制变量">
                    <Button
                      type="text"
                      size="small"
                      @click="copyVariable(variable)"
                    >
                      <template #icon><Copy class="size-4" /></template>
                    </Button>
                  </Tooltip>
                </div>
              </div>
              <Alert
                type="info"
                :show-icon="false"
                style="margin-top: 12px"
                message="点击变量标签可直接插入到编号规则中"
              />
            </Card>
          </Col>
        </Row>
      </TabPane>

      <!-- 维护模式配置 -->
      <TabPane key="maintenance" tab="维护模式">
        <Card title="系统维护模式" :bordered="false">
          <Alert
            v-if="maintenanceStatus.enabled"
            type="warning"
            show-icon
            message="系统当前处于维护模式"
            :description="maintenanceStatus.message"
            style="margin-bottom: 24px"
          />
          <Alert
            v-else
            type="info"
            show-icon
            message="系统正常运行"
            description="维护模式关闭时，所有用户均可正常访问系统"
            style="margin-bottom: 24px"
          />

          <Form layout="vertical" style="max-width: 600px">
            <FormItem label="维护提示信息">
              <Textarea
                v-model:value="maintenanceMessage"
                :rows="4"
                placeholder="请输入维护提示信息，例如：系统正在维护中，预计维护时间：30分钟，请稍后再试"
                :disabled="maintenanceStatus.enabled"
              />
            </FormItem>

            <FormItem>
              <Space>
                <Button
                  v-if="!maintenanceStatus.enabled"
                  type="primary"
                  danger
                  :loading="maintenanceLoading"
                  @click="handleEnableMaintenance"
                >
                  开启维护模式
                </Button>
                <Button
                  v-else
                  type="primary"
                  :loading="maintenanceLoading"
                  @click="handleDisableMaintenance"
                >
                  关闭维护模式
                </Button>
              </Space>
            </FormItem>
          </Form>

          <Divider />

          <Alert type="warning" :show-icon="false" style="margin-top: 16px">
            <div style="margin-bottom: 8px; font-weight: 500">注意事项：</div>
            <ul style="padding-left: 20px; margin: 0; color: #666">
              <li>开启维护模式后，只有管理员账户可以访问系统</li>
              <li>普通用户访问时将看到维护提示信息</li>
              <li>维护模式下，登录接口仍然可用，允许管理员登录</li>
              <li>请确保在维护完成后及时关闭维护模式</li>
            </ul>
          </Alert>
        </Card>
      </TabPane>

      <!-- 版本信息 -->
      <TabPane key="version" tab="版本信息">
        <Card title="系统版本信息" :bordered="false">
          <div v-if="versionLoading" style="padding: 40px; text-align: center">
            <span>加载中...</span>
          </div>
          <div v-else-if="versionInfo" style="max-width: 800px">
            <Row :gutter="[16, 16]">
              <Col :span="12">
                <Card size="small" title="应用版本">
                  <div
                    style="font-size: 24px; font-weight: bold; color: #1890ff"
                  >
                    {{ versionInfo.version }}
                  </div>
                  <div
                    v-if="
                      versionInfo.buildVersion &&
                      versionInfo.buildVersion !== versionInfo.version
                    "
                    style="margin-top: 4px; font-size: 12px; color: #999"
                  >
                    构建版本: {{ versionInfo.buildVersion }}
                  </div>
                </Card>
              </Col>
              <Col :span="12">
                <Card size="small" title="构建时间">
                  <div style="font-size: 14px; color: #666">
                    {{ versionInfo.buildTime }}
                  </div>
                </Card>
              </Col>
              <Col :span="12">
                <Card size="small" title="Git 提交">
                  <div
                    style="font-family: monospace; font-size: 14px; color: #666"
                  >
                    {{ versionInfo.gitCommit }}
                  </div>
                </Card>
              </Col>
              <Col :span="12">
                <Card size="small" title="运行环境">
                  <Tag color="blue">{{ versionInfo.profile }}</Tag>
                </Card>
              </Col>
              <Col :span="24">
                <Card size="small" title="系统信息">
                  <Row :gutter="[16, 8]">
                    <Col :span="8">
                      <div>
                        <strong>Java 版本:</strong>
                        {{ versionInfo.javaVersion }}
                      </div>
                    </Col>
                    <Col :span="8">
                      <div>
                        <strong>Java 供应商:</strong>
                        {{ versionInfo.javaVendor }}
                      </div>
                    </Col>
                    <Col :span="8">
                      <div>
                        <strong>操作系统:</strong> {{ versionInfo.osName }}
                        {{ versionInfo.osVersion }}
                      </div>
                    </Col>
                    <Col :span="24">
                      <div>
                        <strong>服务器时间:</strong>
                        {{ versionInfo.serverTime }}
                      </div>
                    </Col>
                  </Row>
                </Card>
              </Col>
            </Row>
            <Divider />
            <Alert type="info" :show-icon="false" style="margin-top: 16px">
              <div style="margin-bottom: 8px; font-weight: 500">
                版本信息说明：
              </div>
              <ul style="padding-left: 20px; margin: 0; color: #666">
                <li>
                  <strong>版本号</strong
                  >：当前系统版本，优先从数据库配置（sys.version）读取，支持简单格式如
                  0.4、1.2 等
                </li>
                <li>
                  <strong>构建版本</strong>：Maven 构建时的版本号（如
                  1.0.0-SNAPSHOT），仅在版本号与构建版本不同时显示
                </li>
                <li><strong>构建时间</strong>：应用打包构建的时间</li>
                <li>
                  <strong>Git 提交</strong>：构建时对应的 Git 提交 ID（短格式）
                </li>
                <li>
                  <strong>运行环境</strong>：当前 Spring
                  Profile（dev/test/prod）
                </li>
                <li style="margin-top: 8px; color: #1890ff">
                  💡 <strong>提示</strong>：可以在"通用配置"中修改 sys.version
                  来设置简单的版本号格式（如 0.4）
                </li>
              </ul>
            </Alert>
          </div>
          <div v-else style="padding: 40px; color: #999; text-align: center">
            无法加载版本信息
          </div>
        </Card>
      </TabPane>

      <!-- 邮件通知配置 -->
      <TabPane key="notification" tab="邮件通知">
        <Card title="邮件服务配置" :bordered="false">
          <Alert
            v-if="emailStatus.enabled"
            type="success"
            show-icon
            message="邮件服务已启用"
            description="系统将通过邮件发送告警通知和运行报告"
            style="margin-bottom: 24px"
          />
          <Alert
            v-else
            type="warning"
            show-icon
            message="邮件服务未启用"
            description="请在下方的通用配置中配置 SMTP 信息并启用邮件通知"
            style="margin-bottom: 24px"
          />

          <Divider orientation="left">测试邮件配置</Divider>
          <Form layout="inline" style="margin-bottom: 24px">
            <FormItem label="测试邮箱">
              <Input
                v-model:value="testEmailAddress"
                placeholder="请输入接收测试邮件的邮箱地址"
                style="width: 300px"
              />
            </FormItem>
            <FormItem>
              <Button
                type="primary"
                :loading="emailTestLoading"
                :disabled="!emailStatus.enabled"
                @click="handleTestEmail"
              >
                发送测试邮件
              </Button>
            </FormItem>
          </Form>

          <Divider orientation="left">测试告警功能</Divider>
          <Space style="margin-bottom: 24px">
            <Button @click="handleSendTestAlert('login')">
              测试登录失败告警
            </Button>
            <Button @click="handleSendTestAlert('locked')">
              测试账户锁定告警
            </Button>
            <Button @click="handleSendTestAlert('disk')">
              测试磁盘空间告警
            </Button>
            <Button @click="handleSendTestAlert('backup')">
              测试备份失败告警
            </Button>
          </Space>

          <Divider orientation="left">系统运行报告</Divider>
          <Space style="margin-bottom: 24px">
            <Button
              :loading="reportPreviewLoading"
              @click="handlePreviewReport('daily')"
            >
              预览日报
            </Button>
            <Button
              :loading="reportPreviewLoading"
              @click="handlePreviewReport('weekly')"
            >
              预览周报
            </Button>
            <Button
              type="primary"
              :disabled="!emailStatus.enabled"
              @click="handleSendReport('daily')"
            >
              立即发送日报
            </Button>
            <Button
              type="primary"
              :disabled="!emailStatus.enabled"
              @click="handleSendReport('weekly')"
            >
              立即发送周报
            </Button>
          </Space>

          <Divider />
          <Alert type="info" :show-icon="false" style="margin-top: 16px">
            <div style="margin-bottom: 8px; font-weight: 500">配置说明：</div>
            <ul style="padding-left: 20px; margin: 0; color: #666">
              <li>
                <strong>邮件通知开关</strong>：在"通用配置"中设置
                notification.email.enabled 为 true 启用
              </li>
              <li>
                <strong>SMTP 配置</strong>：需配置 SMTP
                服务器、端口、用户名（发件邮箱）、密码（授权码）
              </li>
              <li>
                <strong>告警接收邮箱</strong>：设置
                notification.email.admin.recipients，多个邮箱用逗号分隔
              </li>
              <li>
                <strong>定时报告</strong>：启用
                notification.report.daily.enabled 每天早上8点发送日报
              </li>
              <li>
                <strong>每周报告</strong>：启用
                notification.report.weekly.enabled 每周一早上9点发送周报
              </li>
              <li style="margin-top: 8px">
                💡 <strong>常用 SMTP 配置</strong>：
                <ul style="margin-top: 4px">
                  <li>QQ邮箱：smtp.qq.com，端口 465 (SSL)，密码使用授权码</li>
                  <li>163邮箱：smtp.163.com，端口 465 (SSL)，密码使用授权码</li>
                  <li>Gmail：smtp.gmail.com，端口 587 (TLS)，需开启两步验证</li>
                </ul>
              </li>
            </ul>
          </Alert>
        </Card>
      </TabPane>

      <!-- 企业微信机器人配置 -->
      <TabPane key="wecom" tab="企业微信">
        <Card title="企业微信群机器人" :bordered="false">
          <Alert
            :message="
              wecomStatus.enabled ? '企业微信通知已启用' : '企业微信通知未配置'
            "
            :type="wecomStatus.enabled ? 'success' : 'warning'"
            show-icon
            style="margin-bottom: 24px"
          />

          <Form layout="vertical">
            <FormItem label="Webhook 地址">
              <Input
                v-model:value="wecomWebhookUrl"
                placeholder="请输入企业微信群机器人的 Webhook 地址"
                style="max-width: 600px"
              />
              <div style="margin-top: 4px; font-size: 12px; color: #888">
                格式：https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx
              </div>
            </FormItem>
            <FormItem>
              <Space>
                <Button type="primary" @click="handleSaveWecomWebhook">
                  保存配置
                </Button>
                <Button :loading="wecomTestLoading" @click="handleTestWecomBot">
                  发送测试消息
                </Button>
              </Space>
            </FormItem>
          </Form>

          <Divider />

          <Alert type="info" show-icon>
            <template #message>配置说明</template>
            <template #description>
              <ul style="padding-left: 20px; margin: 8px 0 0">
                <li>
                  <strong>如何获取 Webhook</strong>：在企业微信群中 → 群设置 →
                  群机器人 → 添加机器人 → 复制 Webhook 地址
                </li>
                <li style="margin-top: 8px">
                  <strong>消息类型</strong>：系统会自动发送以下通知到群聊：
                  <ul style="margin-top: 4px">
                    <li>任务到期提醒</li>
                    <li>日程开始提醒</li>
                    <li>合同到期预警</li>
                    <li>审批待办通知</li>
                  </ul>
                </li>
                <li style="margin-top: 8px">
                  💡 <strong>提示</strong>：企业微信机器人是免费的，无需额外付费
                </li>
              </ul>
            </template>
          </Alert>
        </Card>
      </TabPane>

      <!-- 案由罪名配置 -->
      <TabPane key="cause" tab="案由罪名配置">
        <CauseOfActionTab />
      </TabPane>

      <!-- AI计费配置 -->
      <TabPane key="ai-billing" tab="AI计费配置">
        <AiBillingTab />
      </TabPane>

      <!-- 通用配置 -->
      <TabPane key="general" tab="通用配置">
        <Card :bordered="false">
          <template #extra>
            <Button type="primary" @click="handleCreate">新增配置</Button>
          </template>
          <Table
            :key="tableKey"
            :columns="columns"
            :data-source="generalConfigs"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record: rawRecord }">
              <template v-if="column.key === 'configValue'">
                <span>{{ rawRecord.configValue || '-' }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <a @click="handleEdit(rawRecord as SysConfigDTO)">编辑</a>
                  <Popconfirm
                    title="确定要删除该配置吗？"
                    ok-text="确定"
                    cancel-text="取消"
                    @confirm="handleDelete(rawRecord as SysConfigDTO)"
                  >
                    <a style="color: #ff4d4f">删除</a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </TabPane>
    </Tabs>

    <!-- 报告预览弹窗 -->
    <AModal
      v-model:open="reportPreviewVisible"
      title="系统运行报告预览"
      :width="800"
      :footer="null"
    >
      <div
        v-html="reportPreviewHtml"
        style="max-height: 600px; overflow: auto"
      ></div>
    </AModal>

    <!-- 配置弹窗 -->
    <ConfigModal ref="configModalRef" @success="handleModalSuccess" />
  </Page>
</template>

<style scoped>
:deep(.ant-card-head-title) {
  font-size: 14px;
}
</style>
