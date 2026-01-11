<script setup lang="ts">
import type {
  ClientAccessTokenDTO,
  CreateTokenRequest,
  ScopeOption,
} from '#/api/system/openapi';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Alert,
  Button,
  Card,
  Checkbox,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createAccessToken,
  getClientTokens,
  getScopeOptions,
  getTokenList,
  revokeToken,
} from '#/api/system/openapi';
import { getClientList } from '#/api/client';
import { getMatterList } from '#/api/matter';

defineOptions({ name: 'OpenApiManagement' });

// 状态
const loading = ref(false);
const dataSource = ref<ClientAccessTokenDTO[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);
const createModalVisible = ref(false);
const detailModalVisible = ref(false);
const currentDetail = ref<ClientAccessTokenDTO | null>(null);
const newTokenResult = ref<ClientAccessTokenDTO | null>(null);
const showNewTokenModal = ref(false);

// 授权范围选项
const scopeOptions = ref<ScopeOption[]>([]);

// 客户和项目选项
const clientOptions = ref<Array<{ value: number; label: string }>>([]);
const matterOptions = ref<Array<{ value: number; label: string }>>([]);

// 查询条件
const queryForm = reactive({
  clientId: undefined as number | undefined,
  matterId: undefined as number | undefined,
  status: undefined as string | undefined,
});

// 创建表单
const createForm = reactive<CreateTokenRequest>({
  clientId: 0,
  matterId: undefined,
  scopes: [],
  validDays: 30,
  maxAccessCount: undefined,
  ipWhitelist: undefined,
  remark: undefined,
});
const createFormRef = ref();

// 表格列
const columns = [
  { title: '客户', dataIndex: 'clientName', key: 'clientName', width: 150 },
  { title: '项目', dataIndex: 'matterName', key: 'matterName', width: 180, ellipsis: true },
  { title: '授权范围', dataIndex: 'scopes', key: 'scopes', width: 200 },
  { title: '过期时间', dataIndex: 'expiresAt', key: 'expiresAt', width: 170 },
  { title: '访问次数', key: 'accessInfo', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 状态映射
const statusMap = {
  ACTIVE: { text: '有效', color: 'success' },
  REVOKED: { text: '已撤销', color: 'error' },
  EXPIRED: { text: '已过期', color: 'default' },
};

// 授权范围标签
const scopeLabelMap: Record<string, string> = {
  MATTER_INFO: '基本信息',
  MATTER_PROGRESS: '进度',
  LAWYER_INFO: '律师',
  TASK_LIST: '任务',
  DEADLINE_INFO: '期限',
  DOCUMENT_LIST: '文档',
  FEE_INFO: '费用',
};

// 计算当前客户的项目
const filteredMatterOptions = computed(() => {
  if (!createForm.clientId) return [];
  return matterOptions.value;
});

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getTokenList({
      clientId: queryForm.clientId,
      matterId: queryForm.matterId,
      status: queryForm.status,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 加载授权范围选项
async function loadScopeOptions() {
  try {
    scopeOptions.value = await getScopeOptions();
  } catch {
    // 使用默认值
    scopeOptions.value = [
      { value: 'MATTER_INFO', label: '项目基本信息', description: '项目名称、类型、状态等' },
      { value: 'MATTER_PROGRESS', label: '项目进度', description: '当前阶段、整体进度' },
      { value: 'LAWYER_INFO', label: '律师信息', description: '团队成员姓名、联系方式（脱敏）' },
      { value: 'TASK_LIST', label: '任务列表', description: '任务标题、状态、进度' },
      { value: 'DEADLINE_INFO', label: '关键期限', description: '重要日期和期限' },
      { value: 'DOCUMENT_LIST', label: '文档列表', description: '文档名称列表' },
      { value: 'FEE_INFO', label: '费用信息', description: '合同金额、已收款、待收款' },
    ];
  }
}

// 加载客户选项
async function loadClientOptions() {
  try {
    const res = await getClientList({ pageNum: 1, pageSize: 1000 });
    clientOptions.value = (res.list || []).map((c: { id: number; name: string }) => ({
      value: c.id,
      label: c.name,
    }));
  } catch {
    message.error('加载客户列表失败');
  }
}

// 加载项目选项
async function loadMatterOptions(clientId?: number) {
  try {
    const res = await getMatterList({
      pageNum: 1,
      pageSize: 1000,
      clientId,
    });
    matterOptions.value = (res.list || []).map((m: { id: number; name: string }) => ({
      value: m.id,
      label: m.name,
    }));
  } catch {
    message.error('加载项目列表失败');
  }
}

// 当客户选择变化时
function handleClientChange(clientId: number) {
  createForm.matterId = undefined;
  if (clientId) {
    loadMatterOptions(clientId);
  } else {
    matterOptions.value = [];
  }
}

// 打开创建弹窗
function handleCreate() {
  Object.assign(createForm, {
    clientId: 0,
    matterId: undefined,
    scopes: ['MATTER_INFO', 'MATTER_PROGRESS', 'LAWYER_INFO'],
    validDays: 30,
    maxAccessCount: undefined,
    ipWhitelist: undefined,
    remark: undefined,
  });
  matterOptions.value = [];
  createModalVisible.value = true;
}

// 提交创建
async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate();

    if (createForm.scopes.length === 0) {
      message.error('请选择至少一个授权范围');
      return;
    }

    const result = await createAccessToken(createForm);
    message.success('创建成功');
    createModalVisible.value = false;
    
    // 显示新令牌
    newTokenResult.value = result;
    showNewTokenModal.value = true;
    
    fetchData();
  } catch (error: unknown) {
    const err = error as { errorFields?: unknown; message?: string };
    if (err?.errorFields) return;
    message.error(err.message || '创建失败');
  }
}

// 查看详情
function handleView(record: ClientAccessTokenDTO) {
  currentDetail.value = record;
  detailModalVisible.value = true;
}

// 撤销令牌
async function handleRevoke(record: ClientAccessTokenDTO) {
  try {
    await revokeToken(record.id, '管理员手动撤销');
    message.success('已撤销');
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '撤销失败');
  }
}

// 复制令牌
function copyToken(token: string) {
  navigator.clipboard.writeText(token).then(() => {
    message.success('令牌已复制到剪贴板');
  }).catch(() => {
    message.error('复制失败，请手动复制');
  });
}

// 复制链接
function copyPortalUrl(url: string) {
  navigator.clipboard.writeText(url).then(() => {
    message.success('门户链接已复制到剪贴板');
  }).catch(() => {
    message.error('复制失败，请手动复制');
  });
}

// 格式化时间
function formatTime(time: string | undefined) {
  if (!time) return '-';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
}

// 分页变化
function handlePageChange(page: number, size: number) {
  pageNum.value = page;
  pageSize.value = size;
  fetchData();
}

// 查询
function handleSearch() {
  pageNum.value = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryForm.clientId = undefined;
  queryForm.matterId = undefined;
  queryForm.status = undefined;
  pageNum.value = 1;
  fetchData();
}

onMounted(() => {
  fetchData();
  loadScopeOptions();
  loadClientOptions();
});
</script>

<template>
  <Page
    title="开放接口管理"
    description="管理客户门户访问令牌，为客户提供安全的项目信息查看通道"
  >
    <Alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #message>
        <span>客户门户访问令牌用于向客户提供项目进度、团队信息等数据的安全访问。令牌具有独立的访问控制，与系统内部认证完全隔离。</span>
      </template>
    </Alert>

    <Card :bordered="false">
      <!-- 查询条件 -->
      <Form layout="inline" style="margin-bottom: 16px">
        <FormItem label="客户">
          <Select
            v-model:value="queryForm.clientId"
            placeholder="选择客户"
            style="width: 180px"
            allow-clear
            show-search
            :filter-option="(input: string, option: { label: string }) => option.label.toLowerCase().includes(input.toLowerCase())"
            :options="clientOptions"
          />
        </FormItem>
        <FormItem label="状态">
          <Select
            v-model:value="queryForm.status"
            placeholder="选择状态"
            style="width: 120px"
            allow-clear
            :options="[
              { value: 'ACTIVE', label: '有效' },
              { value: 'REVOKED', label: '已撤销' },
              { value: 'EXPIRED', label: '已过期' },
            ]"
          />
        </FormItem>
        <FormItem>
          <Space>
            <Button type="primary" @click="handleSearch">查询</Button>
            <Button @click="handleReset">重置</Button>
            <Button type="primary" @click="handleCreate">创建令牌</Button>
          </Space>
        </FormItem>
      </Form>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: pageNum,
          pageSize: pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handlePageChange,
        }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record: rawRecord }">
          <template v-if="column.key === 'scopes'">
            <Space size="small" wrap>
              <Tag
                v-for="scope in (rawRecord as ClientAccessTokenDTO).scopes.slice(0, 3)"
                :key="scope"
                size="small"
              >
                {{ scopeLabelMap[scope] || scope }}
              </Tag>
              <Tooltip
                v-if="(rawRecord as ClientAccessTokenDTO).scopes.length > 3"
                :title="(rawRecord as ClientAccessTokenDTO).scopes.map(s => scopeLabelMap[s] || s).join(', ')"
              >
                <Tag size="small">+{{ (rawRecord as ClientAccessTokenDTO).scopes.length - 3 }}</Tag>
              </Tooltip>
            </Space>
          </template>
          <template v-else-if="column.key === 'expiresAt'">
            {{ formatTime((rawRecord as ClientAccessTokenDTO).expiresAt) }}
          </template>
          <template v-else-if="column.key === 'accessInfo'">
            <span>
              {{ (rawRecord as ClientAccessTokenDTO).accessCount }}
              <template v-if="(rawRecord as ClientAccessTokenDTO).maxAccessCount">
                / {{ (rawRecord as ClientAccessTokenDTO).maxAccessCount }}
              </template>
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[(rawRecord as ClientAccessTokenDTO).status]?.color">
              {{ statusMap[(rawRecord as ClientAccessTokenDTO).status]?.text }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime((rawRecord as ClientAccessTokenDTO).createdAt) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(rawRecord as ClientAccessTokenDTO)">详情</a>
              <Popconfirm
                v-if="(rawRecord as ClientAccessTokenDTO).status === 'ACTIVE'"
                title="确定要撤销此令牌吗？撤销后客户将无法继续访问。"
                @confirm="handleRevoke(rawRecord as ClientAccessTokenDTO)"
              >
                <a style="color: #ff4d4f">撤销</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 创建令牌弹窗 -->
    <Modal
      v-model:open="createModalVisible"
      title="创建客户访问令牌"
      width="640px"
      @ok="handleCreateSubmit"
    >
      <Form
        ref="createFormRef"
        :model="createForm"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        style="margin-top: 20px"
      >
        <FormItem
          label="选择客户"
          name="clientId"
          :rules="[{ required: true, message: '请选择客户' }]"
        >
          <Select
            v-model:value="createForm.clientId"
            placeholder="选择客户"
            show-search
            :filter-option="(input: string, option: { label: string }) => option.label.toLowerCase().includes(input.toLowerCase())"
            :options="clientOptions"
            @change="handleClientChange"
          />
        </FormItem>

        <FormItem label="绑定项目" name="matterId">
          <Select
            v-model:value="createForm.matterId"
            placeholder="选择项目（可选）"
            allow-clear
            show-search
            :filter-option="(input: string, option: { label: string }) => option.label.toLowerCase().includes(input.toLowerCase())"
            :options="filteredMatterOptions"
            :disabled="!createForm.clientId"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            绑定项目后，客户只能查看该项目的信息
          </div>
        </FormItem>

        <FormItem
          label="授权范围"
          name="scopes"
          :rules="[{ required: true, message: '请选择授权范围' }]"
        >
          <Checkbox.Group v-model:value="createForm.scopes" style="width: 100%">
            <div
              v-for="opt in scopeOptions"
              :key="opt.value"
              style="margin-bottom: 8px"
            >
              <Checkbox :value="opt.value">
                <span>{{ opt.label }}</span>
                <span style="color: #999; font-size: 12px; margin-left: 8px">
                  {{ opt.description }}
                </span>
              </Checkbox>
            </div>
          </Checkbox.Group>
        </FormItem>

        <FormItem
          label="有效期"
          name="validDays"
          :rules="[{ required: true, message: '请输入有效期' }]"
        >
          <InputNumber
            v-model:value="createForm.validDays"
            :min="1"
            :max="365"
            style="width: 150px"
          />
          <span style="margin-left: 8px">天</span>
        </FormItem>

        <FormItem label="访问次数限制" name="maxAccessCount">
          <InputNumber
            v-model:value="createForm.maxAccessCount"
            :min="1"
            placeholder="不限制"
            style="width: 150px"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            留空表示不限制访问次数
          </div>
        </FormItem>

        <FormItem label="IP白名单" name="ipWhitelist">
          <Input
            v-model:value="createForm.ipWhitelist"
            placeholder="多个IP用逗号分隔，如：192.168.1.1,10.0.0.1"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            留空表示不限制访问IP
          </div>
        </FormItem>

        <FormItem label="备注" name="remark">
          <Input.TextArea
            v-model:value="createForm.remark"
            :rows="2"
            placeholder="备注说明"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 新令牌显示弹窗 -->
    <Modal
      v-model:open="showNewTokenModal"
      title="令牌创建成功"
      width="600px"
      :footer="null"
    >
      <Alert
        type="warning"
        show-icon
        style="margin-bottom: 16px"
        message="请妥善保存令牌"
        description="令牌仅显示一次，关闭此窗口后将无法再次查看完整令牌。请复制并安全地发送给客户。"
      />
      
      <Descriptions :column="1" bordered size="small" v-if="newTokenResult">
        <DescriptionsItem label="客户">{{ newTokenResult.clientName }}</DescriptionsItem>
        <DescriptionsItem label="项目">{{ newTokenResult.matterName || '全部项目' }}</DescriptionsItem>
        <DescriptionsItem label="访问令牌">
          <div style="display: flex; align-items: center; gap: 8px">
            <code style="background: #f5f5f5; padding: 4px 8px; border-radius: 4px; word-break: break-all">
              {{ newTokenResult.token }}
            </code>
            <Button size="small" @click="copyToken(newTokenResult!.token)">复制</Button>
          </div>
        </DescriptionsItem>
        <DescriptionsItem v-if="newTokenResult.portalUrl" label="门户链接">
          <div style="display: flex; align-items: center; gap: 8px">
            <a :href="newTokenResult.portalUrl" target="_blank" style="word-break: break-all">
              {{ newTokenResult.portalUrl }}
            </a>
            <Button size="small" @click="copyPortalUrl(newTokenResult!.portalUrl!)">复制</Button>
          </div>
        </DescriptionsItem>
        <DescriptionsItem label="有效期至">
          {{ formatTime(newTokenResult.expiresAt) }}
        </DescriptionsItem>
        <DescriptionsItem label="授权范围">
          <Space size="small" wrap>
            <Tag v-for="scope in newTokenResult.scopes" :key="scope">
              {{ scopeLabelMap[scope] || scope }}
            </Tag>
          </Space>
        </DescriptionsItem>
      </Descriptions>

      <div style="text-align: center; margin-top: 24px">
        <Button type="primary" @click="showNewTokenModal = false">我已保存，关闭</Button>
      </div>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      :title="`令牌详情 - ${currentDetail?.clientName}`"
      width="640px"
      :footer="null"
    >
      <Descriptions v-if="currentDetail" :column="1" bordered size="small">
        <DescriptionsItem label="令牌ID">{{ currentDetail.id }}</DescriptionsItem>
        <DescriptionsItem label="令牌（脱敏）">
          <code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px">
            {{ currentDetail.token }}
          </code>
        </DescriptionsItem>
        <DescriptionsItem label="客户">{{ currentDetail.clientName }}</DescriptionsItem>
        <DescriptionsItem label="项目">{{ currentDetail.matterName || '全部项目' }}</DescriptionsItem>
        <DescriptionsItem label="授权范围">
          <Space size="small" wrap>
            <Tag v-for="scope in currentDetail.scopes" :key="scope">
              {{ scopeLabelMap[scope] || scope }}
            </Tag>
          </Space>
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentDetail.status]?.color">
            {{ statusMap[currentDetail.status]?.text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="有效期至">{{ formatTime(currentDetail.expiresAt) }}</DescriptionsItem>
        <DescriptionsItem label="访问次数">
          {{ currentDetail.accessCount }}
          <template v-if="currentDetail.maxAccessCount">
            / {{ currentDetail.maxAccessCount }}
          </template>
        </DescriptionsItem>
        <DescriptionsItem label="IP白名单">{{ currentDetail.ipWhitelist || '不限制' }}</DescriptionsItem>
        <DescriptionsItem label="最后访问IP">{{ currentDetail.lastAccessIp || '-' }}</DescriptionsItem>
        <DescriptionsItem label="最后访问时间">{{ formatTime(currentDetail.lastAccessAt) }}</DescriptionsItem>
        <DescriptionsItem label="创建人">{{ currentDetail.creatorName }}</DescriptionsItem>
        <DescriptionsItem label="创建时间">{{ formatTime(currentDetail.createdAt) }}</DescriptionsItem>
        <DescriptionsItem v-if="currentDetail.remark" label="备注">{{ currentDetail.remark }}</DescriptionsItem>
      </Descriptions>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-descriptions-item-label) {
  width: 120px;
}
</style>

