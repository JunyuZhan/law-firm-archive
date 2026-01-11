<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { message } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Modal,
  Textarea,
  Tag,
  Tabs,
  Tooltip,
  Alert,
  Popconfirm,
  Descriptions,
  DescriptionsItem,
} from 'ant-design-vue';
import {
  getExternalIntegrationList,
  updateExternalIntegration,
  enableExternalIntegration,
  disableExternalIntegration,
  testExternalIntegration,
} from '#/api/system';
import type { ExternalIntegrationDTO } from '#/api/system/types';

defineOptions({ name: 'ExternalIntegration' });

// 状态
const loading = ref(false);
const dataSource = ref<ExternalIntegrationDTO[]>([]);
const modalVisible = ref(false);
const detailVisible = ref(false);
const formRef = ref();
const testLoading = ref<number | null>(null);
const activeTab = ref('AI');
const currentDetail = ref<ExternalIntegrationDTO | null>(null);

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  integrationCode: '',
  integrationName: '',
  integrationType: '',
  apiUrl: '',
  apiKey: '',
  apiSecret: '',
  authType: 'API_KEY',
  extraConfig: {} as Record<string, unknown>,
  description: '',
});

// 集成类型选项
const integrationTypeOptions = [
  { value: 'ARCHIVE', label: '档案系统' },
  { value: 'AI', label: 'AI大模型' },
  { value: 'OTHER', label: '其他' },
];

// 认证方式选项
const authTypeOptions = [
  { value: 'API_KEY', label: 'API Key' },
  { value: 'BEARER_TOKEN', label: 'Bearer Token' },
  { value: 'BASIC', label: 'Basic Auth' },
  { value: 'OAUTH2', label: 'OAuth 2.0' },
];

// 按类型分组的数据
const archiveIntegrations = computed(() => 
  dataSource.value.filter(item => item.integrationType === 'ARCHIVE')
);

const aiIntegrations = computed(() => 
  dataSource.value.filter(item => item.integrationType === 'AI')
);

const otherIntegrations = computed(() => 
  dataSource.value.filter(item => item.integrationType === 'OTHER')
);

// 表格列
const columns = [
  { title: '名称', dataIndex: 'integrationName', key: 'integrationName', width: 150 },
  { title: '编码', dataIndex: 'integrationCode', key: 'integrationCode', width: 140 },
  { title: 'API地址', dataIndex: 'apiUrl', key: 'apiUrl', ellipsis: true },
  { title: '认证方式', dataIndex: 'authType', key: 'authType', width: 100 },
  { title: '状态', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '测试结果', dataIndex: 'lastTestResult', key: 'lastTestResult', width: 120 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getExternalIntegrationList({
      pageNum: 1,
      pageSize: 100,
    });
    dataSource.value = res.list || [];
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 编辑
function handleEdit(record: ExternalIntegrationDTO) {
  Object.assign(formData, {
    id: record.id,
    integrationCode: record.integrationCode,
    integrationName: record.integrationName,
    integrationType: record.integrationType,
    apiUrl: record.apiUrl || '',
    apiKey: '',
    apiSecret: '',
    authType: record.authType || 'API_KEY',
    extraConfig: record.extraConfig || {},
    description: record.description || '',
  });
  modalVisible.value = true;
}

// 查看详情
function handleView(record: ExternalIntegrationDTO) {
  currentDetail.value = record;
  detailVisible.value = true;
}

// 保存
async function handleSave() {
  try {
    await formRef.value?.validate();
    
    const updateData: Record<string, unknown> = {
      id: formData.id,
      apiUrl: formData.apiUrl || undefined,
      authType: formData.authType,
      description: formData.description || undefined,
    };
    
    if (formData.apiKey) {
      updateData.apiKey = formData.apiKey;
    }
    if (formData.apiSecret) {
      updateData.apiSecret = formData.apiSecret;
    }
    if (Object.keys(formData.extraConfig).length > 0) {
      updateData.extraConfig = formData.extraConfig;
    }
    
    await updateExternalIntegration(updateData as unknown as Parameters<typeof updateExternalIntegration>[0]);
    message.success('保存成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: unknown) {
    const err = error as { errorFields?: unknown; message?: string };
    if (err?.errorFields) return;
    message.error(err.message || '保存失败');
  }
}

// 启用/禁用
async function handleToggleEnabled(record: ExternalIntegrationDTO) {
  try {
    if (record.enabled) {
      await disableExternalIntegration(record.id);
      message.success('已禁用');
    } else {
      await enableExternalIntegration(record.id);
      message.success('已启用');
    }
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

// 测试连接
async function handleTest(record: ExternalIntegrationDTO) {
  testLoading.value = record.id;
  try {
    const result = await testExternalIntegration(record.id);
    if (result.lastTestResult === 'SUCCESS') {
      message.success('连接测试成功');
    } else {
      message.error(result.lastTestMessage || '连接测试失败');
    }
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '测试失败');
  } finally {
    testLoading.value = null;
  }
}

// 格式化测试结果
function formatTestResult(result: string | undefined) {
  if (!result) return { text: '未测试', color: 'default' };
  if (result === 'SUCCESS') return { text: '成功', color: 'success' };
  return { text: '失败', color: 'error' };
}

// 格式化认证方式
function formatAuthType(type: string) {
  const item = authTypeOptions.find(opt => opt.value === type);
  return item?.label || type;
}

// 格式化时间
function formatTime(time: string | undefined) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="外部系统集成" description="管理档案馆API、AI大模型等外部系统的连接配置">
    <Alert 
      type="info" 
      show-icon 
      style="margin-bottom: 16px;"
      message="配置说明"
      description="此处配置外部系统的连接信息，包括档案馆对接和AI大模型服务。配置后可在相关功能中使用。API密钥会加密存储，请确保配置信息的安全性。"
    />

    <Card :bordered="false">
      <Tabs v-model:activeKey="activeTab">
        <!-- AI大模型 -->
        <Tabs.TabPane key="AI" tab="AI大模型">
          <Alert 
            type="warning" 
            style="margin-bottom: 16px;"
            message="AI大模型配置用于系统中的智能辅助功能，如合同审查、法律文书生成等。同一时间只能启用一个AI服务。"
          />
          <Table
            :columns="columns"
            :data-source="aiIntegrations"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record: rawRecord }">
              <template v-if="column.key === 'authType'">
                <Tag>{{ formatAuthType((rawRecord as ExternalIntegrationDTO).authType) }}</Tag>
              </template>
              <template v-else-if="column.key === 'enabled'">
                <Tag :color="(rawRecord as ExternalIntegrationDTO).enabled ? 'success' : 'default'">
                  {{ (rawRecord as ExternalIntegrationDTO).enabled ? '已启用' : '未启用' }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'lastTestResult'">
                <Space>
                  <Tag :color="formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).color">
                    {{ formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).text }}
                  </Tag>
                  <Tooltip v-if="(rawRecord as ExternalIntegrationDTO).lastTestMessage" :title="(rawRecord as ExternalIntegrationDTO).lastTestMessage">
                    <span style=" font-size: 12px;color: #999; cursor: help;">详情</span>
                  </Tooltip>
                </Space>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <a @click="handleView(rawRecord as ExternalIntegrationDTO)">详情</a>
                  <a @click="handleEdit(rawRecord as ExternalIntegrationDTO)">配置</a>
                  <a 
                    :class="{ 'opacity-50': testLoading === (rawRecord as ExternalIntegrationDTO).id }"
                    @click="handleTest(rawRecord as ExternalIntegrationDTO)"
                  >测试</a>
                  <Popconfirm 
                    :title="(rawRecord as ExternalIntegrationDTO).enabled ? '确定要禁用此集成？' : '确定要启用此集成？'"
                    @confirm="handleToggleEnabled(rawRecord as ExternalIntegrationDTO)"
                  >
                    <a :style="{ color: (rawRecord as ExternalIntegrationDTO).enabled ? '#ff4d4f' : '#52c41a' }">
                      {{ (rawRecord as ExternalIntegrationDTO).enabled ? '禁用' : '启用' }}
                    </a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <!-- 档案系统 -->
        <Tabs.TabPane key="ARCHIVE" tab="档案系统">
          <Alert 
            type="info" 
            style="margin-bottom: 16px;"
            message="档案系统配置用于档案迁移功能，将律所档案同步到外部档案管理系统（如市档案馆、区档案馆）。"
          />
          <Table
            :columns="columns"
            :data-source="archiveIntegrations"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record: rawRecord }">
              <template v-if="column.key === 'authType'">
                <Tag>{{ formatAuthType((rawRecord as ExternalIntegrationDTO).authType) }}</Tag>
              </template>
              <template v-else-if="column.key === 'enabled'">
                <Tag :color="(rawRecord as ExternalIntegrationDTO).enabled ? 'success' : 'default'">
                  {{ (rawRecord as ExternalIntegrationDTO).enabled ? '已启用' : '未启用' }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'lastTestResult'">
                <Space>
                  <Tag :color="formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).color">
                    {{ formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).text }}
                  </Tag>
                  <Tooltip v-if="(rawRecord as ExternalIntegrationDTO).lastTestMessage" :title="(rawRecord as ExternalIntegrationDTO).lastTestMessage">
                    <span style=" font-size: 12px;color: #999; cursor: help;">详情</span>
                  </Tooltip>
                </Space>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <a @click="handleView(rawRecord as ExternalIntegrationDTO)">详情</a>
                  <a @click="handleEdit(rawRecord as ExternalIntegrationDTO)">配置</a>
                  <a 
                    :class="{ 'opacity-50': testLoading === (rawRecord as ExternalIntegrationDTO).id }"
                    @click="handleTest(rawRecord as ExternalIntegrationDTO)"
                  >测试</a>
                  <Popconfirm 
                    :title="(rawRecord as ExternalIntegrationDTO).enabled ? '确定要禁用此集成？' : '确定要启用此集成？'"
                    @confirm="handleToggleEnabled(rawRecord as ExternalIntegrationDTO)"
                  >
                    <a :style="{ color: (rawRecord as ExternalIntegrationDTO).enabled ? '#ff4d4f' : '#52c41a' }">
                      {{ (rawRecord as ExternalIntegrationDTO).enabled ? '禁用' : '启用' }}
                    </a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <!-- 其他系统 -->
        <Tabs.TabPane key="OTHER" tab="其他系统">
          <Table
            :columns="columns"
            :data-source="otherIntegrations"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :scroll="{ x: 900 }"
          >
            <template #bodyCell="{ column, record: rawRecord }">
              <template v-if="column.key === 'authType'">
                <Tag>{{ formatAuthType((rawRecord as ExternalIntegrationDTO).authType) }}</Tag>
              </template>
              <template v-else-if="column.key === 'enabled'">
                <Tag :color="(rawRecord as ExternalIntegrationDTO).enabled ? 'success' : 'default'">
                  {{ (rawRecord as ExternalIntegrationDTO).enabled ? '已启用' : '未启用' }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'lastTestResult'">
                <Space>
                  <Tag :color="formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).color">
                    {{ formatTestResult((rawRecord as ExternalIntegrationDTO).lastTestResult).text }}
                  </Tag>
                </Space>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <a @click="handleView(rawRecord as ExternalIntegrationDTO)">详情</a>
                  <a @click="handleEdit(rawRecord as ExternalIntegrationDTO)">配置</a>
                  <a @click="handleTest(rawRecord as ExternalIntegrationDTO)">测试</a>
                  <Popconfirm 
                    :title="(rawRecord as ExternalIntegrationDTO).enabled ? '确定要禁用此集成？' : '确定要启用此集成？'"
                    @confirm="handleToggleEnabled(rawRecord as ExternalIntegrationDTO)"
                  >
                    <a :style="{ color: (rawRecord as ExternalIntegrationDTO).enabled ? '#ff4d4f' : '#52c41a' }">
                      {{ (rawRecord as ExternalIntegrationDTO).enabled ? '禁用' : '启用' }}
                    </a>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
          <div v-if="otherIntegrations.length === 0" style=" padding: 40px; color: #999;text-align: center;">
            暂无其他系统集成配置
          </div>
        </Tabs.TabPane>
      </Tabs>
    </Card>

    <!-- 编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="`配置 - ${formData.integrationName}`"
      width="640px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        style="margin-top: 20px;"
      >
        <FormItem label="集成编码">
          <Input v-model:value="formData.integrationCode" disabled />
        </FormItem>
        
        <FormItem 
          label="API地址" 
          name="apiUrl"
          :rules="[{ required: true, message: '请输入API地址' }]"
        >
          <Input v-model:value="formData.apiUrl" placeholder="如：https://api.openai.com/v1" />
        </FormItem>
        
        <FormItem label="认证方式" name="authType">
          <Select v-model:value="formData.authType" :options="authTypeOptions" style="width: 200px" />
        </FormItem>
        
        <FormItem label="API Key" name="apiKey">
          <Input.Password 
            v-model:value="formData.apiKey" 
            placeholder="留空表示不修改，输入新值则更新"
            autocomplete="off"
          />
          <div style=" margin-top: 4px; font-size: 12px;color: #999;">
            用于 API_KEY 或 BEARER_TOKEN 认证方式
          </div>
        </FormItem>
        
        <FormItem label="API Secret" name="apiSecret">
          <Input.Password 
            v-model:value="formData.apiSecret" 
            placeholder="留空表示不修改，部分API需要"
            autocomplete="off"
          />
        </FormItem>
        
        <FormItem label="描述" name="description">
          <Textarea 
            v-model:value="formData.description" 
            :rows="3" 
            placeholder="配置说明"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      :title="`${currentDetail?.integrationName} - 详情`"
      width="640px"
      :footer="null"
    >
      <Descriptions v-if="currentDetail" :column="1" bordered size="small">
        <DescriptionsItem label="集成编码">{{ currentDetail.integrationCode }}</DescriptionsItem>
        <DescriptionsItem label="集成名称">{{ currentDetail.integrationName }}</DescriptionsItem>
        <DescriptionsItem label="集成类型">
          <Tag>{{ integrationTypeOptions.find(opt => opt.value === currentDetail?.integrationType)?.label }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="API地址">{{ currentDetail.apiUrl || '-' }}</DescriptionsItem>
        <DescriptionsItem label="API密钥">
          {{ currentDetail.hasApiSecret ? '******（已配置）' : '未配置' }}
        </DescriptionsItem>
        <DescriptionsItem label="认证方式">
          <Tag>{{ formatAuthType(currentDetail.authType) }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="currentDetail.enabled ? 'success' : 'default'">
            {{ currentDetail.enabled ? '已启用' : '未启用' }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="最后测试时间">{{ formatTime(currentDetail.lastTestTime) }}</DescriptionsItem>
        <DescriptionsItem label="最后测试结果">
          <Tag :color="formatTestResult(currentDetail.lastTestResult).color">
            {{ formatTestResult(currentDetail.lastTestResult).text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem v-if="currentDetail.lastTestMessage" label="测试消息">
          {{ currentDetail.lastTestMessage }}
        </DescriptionsItem>
        <DescriptionsItem label="描述">{{ currentDetail.description || '-' }}</DescriptionsItem>
        <DescriptionsItem v-if="currentDetail.extraConfig && Object.keys(currentDetail.extraConfig).length > 0" label="额外配置">
          <pre style=" padding: 8px;margin: 0; font-size: 12px; background: #f5f5f5; border-radius: 4px;">{{ JSON.stringify(currentDetail.extraConfig, null, 2) }}</pre>
        </DescriptionsItem>
        <DescriptionsItem label="更新时间">{{ formatTime(currentDetail.updatedAt) }}</DescriptionsItem>
      </Descriptions>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-table-cell) {
  vertical-align: middle;
}

:deep(.ant-descriptions-item-content pre) {
  max-height: 200px;
  overflow: auto;
}

.opacity-50 {
  pointer-events: none;
  opacity: 0.5;
}
</style>
