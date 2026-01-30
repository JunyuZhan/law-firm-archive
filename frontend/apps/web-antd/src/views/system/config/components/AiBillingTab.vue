<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';

import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
} from 'ant-design-vue';

import { requestClient } from '#/api/request';

// ===== 系统配置 =====
interface SysConfig {
  id: number;
  configKey: string;
  configValue: string;
  description?: string;
}

const configLoading = ref(false);
const configs = ref<SysConfig[]>([]);

const configKeyLabels: Record<string, string> = {
  'ai.billing.enabled': 'AI计费开关',
  'ai.billing.charge_ratio': '用户承担比例 (%)',
  'ai.billing.salary_deduction': '从工资扣减',
  'ai.billing.free_tokens': '月度免费Token',
  'ai.billing.free_amount': '月度免费金额',
  'ai.usage.max_tokens': 'Token 上限 (0=无限)',
  'ai.usage.max_amount': '费用上限 (0=无限)',
};

const configValueType: Record<string, 'boolean' | 'number' | 'string'> = {
  'ai.billing.enabled': 'boolean',
  'ai.billing.charge_ratio': 'number',
  'ai.billing.salary_deduction': 'boolean',
  'ai.billing.free_tokens': 'number',
  'ai.billing.free_amount': 'number',
  'ai.usage.max_tokens': 'number',
  'ai.usage.max_amount': 'number',
};

async function loadConfigs() {
  configLoading.value = true;
  try {
    const res = await requestClient.get<SysConfig[]>('/system/config', {
      params: { keyPrefix: 'ai.' },
    });
    configs.value = res || [];
  } catch (error: any) {
    message.error(error.message || '加载配置失败');
  } finally {
    configLoading.value = false;
  }
}

async function updateConfig(key: string, value: string) {
  try {
    // 使用 /key/{key} 端点更新配置值
    await requestClient.put(`/system/config/key/${key}`, { value });
    message.success('配置已更新');
    await loadConfigs();
  } catch (error: any) {
    message.error(error.message || '更新失败');
  }
}

// ===== 定价配置 =====
interface AiPricingConfig {
  id: number;
  integrationCode: string;
  modelName: string;
  promptPrice: number;
  completionPrice: number;
  perCallPrice: number;
  pricingMode: string;
  enabled: boolean;
}

const pricingLoading = ref(false);
const pricingList = ref<AiPricingConfig[]>([]);
const pricingModalVisible = ref(false);
const editingPricing = ref<AiPricingConfig | null>(null);

const pricingForm = reactive({
  integrationCode: '',
  modelName: '',
  promptPrice: 0,
  completionPrice: 0,
  perCallPrice: 0,
  pricingMode: 'PER_TOKEN',
  enabled: true,
});

const pricingColumns = [
  {
    title: '集成代码',
    dataIndex: 'integrationCode',
    key: 'integrationCode',
    width: 140,
  },
  { title: '模型名称', dataIndex: 'modelName', key: 'modelName', width: 180 },
  {
    title: '输入价格 (元/千Token)',
    dataIndex: 'promptPrice',
    key: 'promptPrice',
    width: 160,
  },
  {
    title: '输出价格 (元/千Token)',
    dataIndex: 'completionPrice',
    key: 'completionPrice',
    width: 160,
  },
  {
    title: '单次调用费',
    dataIndex: 'perCallPrice',
    key: 'perCallPrice',
    width: 120,
  },
  {
    title: '计费模式',
    dataIndex: 'pricingMode',
    key: 'pricingMode',
    width: 100,
  },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
];

const integrationOptions = [
  { label: 'DeepSeek', value: 'AI_DEEPSEEK' },
  { label: 'DeepSeek R1', value: 'AI_DEEPSEEK_R1' },
  { label: '通义千问', value: 'AI_QWEN' },
  { label: '智谱AI', value: 'AI_ZHIPU' },
  { label: '文心一言', value: 'AI_WENXIN' },
  { label: 'Moonshot', value: 'AI_MOONSHOT' },
  { label: 'MiniMax', value: 'AI_MINIMAX' },
  { label: '百川', value: 'AI_BAICHUAN' },
  { label: 'OpenAI', value: 'AI_OPENAI' },
  { label: 'Claude', value: 'AI_CLAUDE' },
];

const pricingModeOptions = [
  { label: '按Token计费', value: 'PER_TOKEN' },
  { label: '按次计费', value: 'PER_CALL' },
  { label: '混合计费', value: 'HYBRID' },
];

async function loadPricing() {
  pricingLoading.value = true;
  try {
    const res = await requestClient.get<AiPricingConfig[]>('/ai/pricing');
    pricingList.value = res || [];
  } catch (error: any) {
    message.error(error.message || '加载定价失败');
  } finally {
    pricingLoading.value = false;
  }
}

function openPricingModal(record?: AiPricingConfig) {
  if (record) {
    editingPricing.value = record;
    Object.assign(pricingForm, record);
  } else {
    editingPricing.value = null;
    Object.assign(pricingForm, {
      integrationCode: '',
      modelName: '',
      promptPrice: 0,
      completionPrice: 0,
      perCallPrice: 0,
      pricingMode: 'PER_TOKEN',
      enabled: true,
    });
  }
  pricingModalVisible.value = true;
}

async function savePricing() {
  try {
    if (editingPricing.value) {
      await requestClient.put(
        `/ai/pricing/${editingPricing.value.id}`,
        pricingForm,
      );
      message.success('定价已更新');
    } else {
      await requestClient.post('/ai/pricing', pricingForm);
      message.success('定价已创建');
    }
    pricingModalVisible.value = false;
    await loadPricing();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  }
}

async function deletePricing(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该定价配置吗？',
    onOk: async () => {
      try {
        await requestClient.delete(`/ai/pricing/${id}`);
        message.success('已删除');
        await loadPricing();
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

// 加载数据
function loadData() {
  loadConfigs();
  loadPricing();
}

onMounted(() => {
  loadData();
});

// 暴露刷新方法供父组件调用
defineExpose({ loadData });
</script>

<template>
  <div class="ai-billing-tab">
    <!-- 系统配置 -->
    <Card
      title="计费全局配置"
      :loading="configLoading"
      style="margin-bottom: 16px"
    >
      <Row :gutter="[24, 16]">
        <Col
          v-for="cfg in configs"
          :key="cfg.configKey"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="config-item">
            <div class="config-label">
              {{ configKeyLabels[cfg.configKey] || cfg.configKey }}
            </div>
            <div class="config-value">
              <template v-if="configValueType[cfg.configKey] === 'boolean'">
                <Switch
                  :checked="cfg.configValue === 'true'"
                  @change="
                    (checked) => updateConfig(cfg.configKey, String(checked))
                  "
                />
              </template>
              <template v-else-if="configValueType[cfg.configKey] === 'number'">
                <InputNumber
                  :value="Number(cfg.configValue)"
                  style="width: 120px"
                  @change="
                    (val) => updateConfig(cfg.configKey, String(val ?? 0))
                  "
                />
              </template>
              <template v-else>
                <Input
                  :value="cfg.configValue"
                  style="width: 120px"
                  @change="
                    (e: Event) =>
                      updateConfig(
                        cfg.configKey,
                        (e.target as HTMLInputElement).value,
                      )
                  "
                />
              </template>
            </div>
          </div>
        </Col>
      </Row>
    </Card>

    <!-- 定价配置 -->
    <Card title="AI模型定价配置" :loading="pricingLoading">
      <template #extra>
        <Button type="primary" @click="openPricingModal()"> 新增定价 </Button>
      </template>

      <Table
        :columns="pricingColumns"
        :data-source="pricingList"
        row-key="id"
        :pagination="false"
        size="small"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'promptPrice'">
            ¥{{ Number(record.promptPrice).toFixed(6) }}
          </template>
          <template v-else-if="column.key === 'completionPrice'">
            ¥{{ Number(record.completionPrice).toFixed(6) }}
          </template>
          <template v-else-if="column.key === 'perCallPrice'">
            ¥{{ Number(record.perCallPrice).toFixed(4) }}
          </template>
          <template v-else-if="column.key === 'enabled'">
            <Tag :color="record.enabled ? 'green' : 'red'">
              {{ record.enabled ? '启用' : '停用' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <Button
                type="link"
                size="small"
                @click="openPricingModal(record as AiPricingConfig)"
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                @click="deletePricing(record.id)"
              >
                删除
              </Button>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 定价编辑弹窗 -->
    <Modal
      v-model:open="pricingModalVisible"
      :title="editingPricing ? '编辑定价' : '新增定价'"
      @ok="savePricing"
      :width="500"
      class="pricing-modal"
    >
      <Form layout="vertical">
        <Form.Item label="集成代码" required>
          <Select
            v-model:value="pricingForm.integrationCode"
            :options="integrationOptions"
            placeholder="选择AI服务商"
          />
        </Form.Item>
        <Form.Item label="模型名称" required>
          <Input
            v-model:value="pricingForm.modelName"
            placeholder="如: gpt-4, deepseek-chat"
          />
        </Form.Item>
        <Form.Item label="计费模式">
          <Select
            v-model:value="pricingForm.pricingMode"
            :options="pricingModeOptions"
          />
        </Form.Item>
        <Row :gutter="16">
          <Col :span="12">
            <Form.Item label="输入价格 (元/千Token)">
              <InputNumber
                v-model:value="pricingForm.promptPrice"
                :min="0"
                :step="0.001"
                :precision="6"
                style="width: 100%"
              />
            </Form.Item>
          </Col>
          <Col :span="12">
            <Form.Item label="输出价格 (元/千Token)">
              <InputNumber
                v-model:value="pricingForm.completionPrice"
                :min="0"
                :step="0.001"
                :precision="6"
                style="width: 100%"
              />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="单次调用费 (元)">
          <InputNumber
            v-model:value="pricingForm.perCallPrice"
            :min="0"
            :step="0.01"
            :precision="4"
            style="width: 100%"
          />
        </Form.Item>
        <Form.Item label="状态">
          <Switch
            v-model:checked="pricingForm.enabled"
            checked-children="启用"
            un-checked-children="停用"
          />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
/* 移动端适配 */
@media (max-width: 576px) {
  .config-label {
    font-size: 13px;
  }

  .config-value :deep(.ant-input-number) {
    width: 100% !important;
  }
}

.ai-billing-tab {
  padding: 0;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-label {
  font-weight: 500;
  color: #333;
}

.config-value {
  display: flex;
  align-items: center;
}
</style>
