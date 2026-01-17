<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CommissionRule } from '#/api/finance/commission-rule';

import { ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  Collapse,
  CollapsePanel,
  message,
  Modal,
  Space,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { commissionRuleApi } from '#/api/finance/commission-rule';

import CommissionRuleModal from './components/CommissionRuleModal.vue';

defineOptions({ name: 'SystemCommissionConfig' });

// ==================== 状态定义 ====================

const ruleModalRef = ref<InstanceType<typeof CommissionRuleModal>>();

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['columns'] = [
  { title: '方案名称', field: 'ruleName', minWidth: 120 },
  {
    title: '律所',
    field: 'firmRate',
    width: 80,
    slots: { default: 'firmRate' },
  },
  {
    title: '主办律师',
    field: 'leadLawyerRate',
    width: 90,
    slots: { default: 'leadLawyerRate' },
  },
  {
    title: '协办律师',
    field: 'assistLawyerRate',
    width: 90,
    slots: { default: 'assistLawyerRate' },
  },
  {
    title: '辅助人员',
    field: 'supportStaffRate',
    width: 90,
    slots: { default: 'supportStaffRate' },
  },
  {
    title: '案源人',
    field: 'originatorRate',
    width: 80,
    slots: { default: 'originatorRate' },
  },
  {
    title: '可修改',
    field: 'allowModify',
    width: 70,
    slots: { default: 'allowModify' },
  },
  {
    title: '默认',
    field: 'isDefault',
    width: 60,
    slots: { default: 'isDefault' },
  },
  { title: '状态', field: 'active', width: 60, slots: { default: 'active' } },
  { title: '说明', field: 'description', minWidth: 180 },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载数据
async function loadData() {
  const list = await commissionRuleApi.getList();
  return {
    items: list || [],
    total: (list || []).length,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    // 移除高度限制，让表格完整显示所有数据
    height: '',
    minHeight: 200,
    proxyConfig: {
      ajax: {
        query: async () => {
          return await loadData();
        },
      },
    },
    pagerConfig: {
      enabled: false,
    },
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// ==================== 操作方法 ====================

// 格式化比例显示
function formatRate(rate: number): string {
  if (rate === 0) return '-';
  return `${rate.toFixed(1)}%`;
}

function handleAdd() {
  ruleModalRef.value?.openCreate();
}

function handleEdit(row: CommissionRule) {
  ruleModalRef.value?.openEdit(row);
}

async function handleSetDefault(row: CommissionRule) {
  try {
    await commissionRuleApi.setDefault(row.id!);
    message.success('已设为默认方案');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

async function handleToggle(row: CommissionRule) {
  try {
    await commissionRuleApi.toggle(row.id!);
    message.success(row.active ? '已停用' : '已启用');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

function handleDelete(row: CommissionRule) {
  if (row.isDefault) {
    message.error('默认方案不能删除');
    return;
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除"${row.ruleName}"吗？`,
    okText: '确认',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await commissionRuleApi.delete(row.id!);
        message.success('删除成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

function handleModalSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page
    title="提成规则配置"
    description="设置案件收入分配预设方案，律师创建合同时可选择并自定义"
    auto-content-height
  >
    <template #extra>
      <Button type="primary" @click="handleAdd">
        <Plus class="mr-1 size-4" /> 新增方案
      </Button>
    </template>

    <!-- 帮助说明（可折叠） -->
    <Collapse
      :bordered="false"
      class="mb-4 bg-transparent"
      style="background: transparent"
    >
      <CollapsePanel key="help" header="📖 使用说明与业务流程">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
          <!-- 使用说明 -->
          <div class="rounded-lg bg-blue-50 p-4">
            <div class="mb-2 font-medium text-blue-700">💡 使用说明</div>
            <ul class="m-0 list-none space-y-1 p-0 text-sm text-gray-600">
              <li>• <strong>比例可以为0</strong>：0%表示该角色不参与分配</li>
              <li>
                • <strong>比例之和不强制=100%</strong>：根据实际情况灵活设置
              </li>
              <li>
                •
                <strong>允许修改</strong
                >：律师创建合同时可在预设基础上自定义比例
              </li>
            </ul>
          </div>

          <!-- 业务流程 -->
          <div class="rounded-lg bg-green-50 p-4">
            <div class="mb-2 font-medium text-green-700">📋 业务流程</div>
            <div
              class="flex flex-wrap items-center gap-1 text-sm text-gray-600"
            >
              <Tag color="blue" class="m-0">1.创建合同</Tag>
              <span class="mx-1">→</span>
              <Tag color="blue" class="m-0">2.收款确认</Tag>
              <span class="mx-1">→</span>
              <Tag color="orange" class="m-0">3.财务结算</Tag>
              <span class="mx-1">→</span>
              <Tag color="green" class="m-0">4.发放提成</Tag>
            </div>
          </div>

          <!-- 常见场景 -->
          <div class="rounded-lg bg-purple-50 p-4">
            <div class="mb-2 font-medium text-purple-700">🎯 常见场景</div>
            <div class="space-y-1 text-sm text-gray-600">
              <div><strong>独立办案</strong>：律所30% + 主办70%</div>
              <div>
                <strong>团队协作</strong>：律所20% + 主办40% + 协办30% + 辅助10%
              </div>
              <div><strong>律所不提成</strong>：律所0% + 主办70% + 协办30%</div>
            </div>
          </div>
        </div>
      </CollapsePanel>
    </Collapse>

    <!-- 表格区域 -->
    <Card :bordered="false">
      <Grid>
        <!-- 工具栏按钮（隐藏，使用页面顶部按钮） -->
        <template #toolbar-buttons>
          <span></span>
        </template>

        <!-- 比例列 -->
        <template #firmRate="{ row }">
          <span
            :class="
              row.firmRate > 0 ? 'font-medium text-blue-500' : 'text-gray-400'
            "
          >
            {{ formatRate(row.firmRate) }}
          </span>
        </template>
        <template #leadLawyerRate="{ row }">
          <span
            :class="
              row.leadLawyerRate > 0
                ? 'font-medium text-green-500'
                : 'text-gray-400'
            "
          >
            {{ formatRate(row.leadLawyerRate) }}
          </span>
        </template>
        <template #assistLawyerRate="{ row }">
          <span
            :class="
              row.assistLawyerRate > 0
                ? 'font-medium text-orange-500'
                : 'text-gray-400'
            "
          >
            {{ formatRate(row.assistLawyerRate) }}
          </span>
        </template>
        <template #supportStaffRate="{ row }">
          <span
            :class="
              row.supportStaffRate > 0
                ? 'font-medium text-purple-500'
                : 'text-gray-400'
            "
          >
            {{ formatRate(row.supportStaffRate) }}
          </span>
        </template>
        <template #originatorRate="{ row }">
          <span
            :class="
              row.originatorRate > 0
                ? 'font-medium text-pink-500'
                : 'text-gray-400'
            "
          >
            {{ formatRate(row.originatorRate) }}
          </span>
        </template>

        <!-- 状态列 -->
        <template #allowModify="{ row }">
          <Tag :color="row.allowModify ? 'green' : 'default'" class="m-0">
            {{ row.allowModify ? '是' : '否' }}
          </Tag>
        </template>
        <template #isDefault="{ row }">
          <Tag v-if="row.isDefault" color="gold" class="m-0">默认</Tag>
          <span v-else class="text-gray-400">-</span>
        </template>
        <template #active="{ row }">
          <Tag :color="row.active ? 'success' : 'default'" class="m-0">
            {{ row.active ? '启用' : '停用' }}
          </Tag>
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <Space :size="4">
            <a
              class="text-blue-500 hover:text-blue-600"
              @click="handleEdit(row)"
              >编辑</a
            >
            <a
              v-if="!row.isDefault"
              class="text-blue-500 hover:text-blue-600"
              @click="handleSetDefault(row)"
            >
              设为默认
            </a>
            <a
              class="text-blue-500 hover:text-blue-600"
              @click="handleToggle(row)"
            >
              {{ row.active ? '停用' : '启用' }}
            </a>
            <a
              v-if="!row.isDefault"
              class="text-red-500 hover:text-red-600"
              @click="handleDelete(row)"
            >
              删除
            </a>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 编辑弹窗 -->
    <CommissionRuleModal ref="ruleModalRef" @success="handleModalSuccess" />
  </Page>
</template>

<style scoped>
:deep(.ant-collapse-header) {
  padding: 8px 16px !important;
  background: #fafafa;
  border-radius: 8px !important;
}

:deep(.ant-collapse-content-box) {
  padding: 16px !important;
}

:deep(.ant-collapse > .ant-collapse-item) {
  border: none;
}

/* 强制表格显示全部内容，不限制高度 */
:deep(.vxe-grid),
:deep(.vxe-table),
:deep(.vxe-table--body-wrapper) {
  height: auto !important;
  max-height: none !important;
}
</style>
