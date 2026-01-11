<script setup lang="ts">
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CommissionRule } from '#/api/finance/commission-rule';

import { h, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Col,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Space,
  Switch,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { commissionRuleApi } from '#/api/finance/commission-rule';
import { usePermission } from '#/hooks/usePermission';

const Textarea = Input.TextArea;

const { hasPermission } = usePermission();

// 格式化百分比
const formatPercent = (value: number) => `${value.toFixed(1)}%`;

// 表格列定义
const gridColumns: VxeGridProps['columns'] = [
  {
    title: '规则编码',
    field: 'ruleCode',
    minWidth: 120,
  },
  {
    title: '规则名称',
    field: 'ruleName',
    minWidth: 150,
  },
  {
    title: '律所比例',
    field: 'firmRate',
    minWidth: 100,
    slots: {
      default: ({ row }: { row: CommissionRule }) =>
        h('span', formatPercent(row.firmRate)),
    },
  },
  {
    title: '主办律师比例',
    field: 'leadLawyerRate',
    minWidth: 120,
    slots: {
      default: ({ row }: { row: CommissionRule }) =>
        h('span', formatPercent(row.leadLawyerRate)),
    },
  },
  {
    title: '案源人比例',
    field: 'originatorRate',
    minWidth: 100,
    slots: {
      default: ({ row }: { row: CommissionRule }) =>
        h('span', formatPercent(row.originatorRate)),
    },
  },
  {
    title: '状态',
    field: 'active',
    minWidth: 80,
    slots: {
      default: ({ row }: { row: CommissionRule }) =>
        h(Tag, { color: row.active ? 'green' : 'red' }, () =>
          row.active ? '启用' : '禁用',
        ),
    },
  },
  {
    title: '默认规则',
    field: 'isDefault',
    minWidth: 100,
    slots: {
      default: ({ row }: { row: CommissionRule }) =>
        h(Tag, { color: row.isDefault ? 'blue' : 'default' }, () =>
          row.isDefault ? '是' : '否',
        ),
    },
  },
  {
    title: '操作',
    field: 'action',
    width: 200,
    fixed: 'right' as const,
    slots: { default: 'action' },
  },
];

// 使用 useVbenVxeGrid
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    proxyConfig: {
      ajax: {
        query: async () => {
          const response = await commissionRuleApi.getList({});
          return {
            items: response || [],
            total: (response || []).length,
          };
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
    toolbarConfig: {
      refresh: true,
    },
  },
});

// 弹窗状态
const modalVisible = ref(false);
const modalTitle = ref('');
const isEdit = ref(false);
const formRef = ref();

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  ruleCode: '',
  ruleName: '',
  firmRate: 30,
  leadLawyerRate: 50,
  assistLawyerRate: 0,
  supportStaffRate: 0,
  originatorRate: 20,
  allowModify: true,
  isDefault: false,
  active: true,
  description: '',
});

// 表单验证规则
const rules = {
  ruleCode: [{ required: true, message: '请输入规则编码' }],
  ruleName: [{ required: true, message: '请输入规则名称' }],
  firmRate: [{ required: true, message: '请输入律所比例' }],
  leadLawyerRate: [{ required: true, message: '请输入主办律师比例' }],
};

// 新建规则
const handleCreate = () => {
  modalTitle.value = '新建提成规则';
  isEdit.value = false;
  resetForm();
  modalVisible.value = true;
};

// 编辑规则
const handleEdit = (record: CommissionRule) => {
  modalTitle.value = '编辑提成规则';
  isEdit.value = true;
  Object.assign(formData, record);
  modalVisible.value = true;
};

// 查看规则
const handleView = (record: CommissionRule) => {
  modalTitle.value = '查看提成规则';
  isEdit.value = false;
  Object.assign(formData, record);
  modalVisible.value = true;
};

// 删除规则
const handleDelete = async (record: CommissionRule) => {
  try {
    if (record.id) {
      await commissionRuleApi.delete(record.id);
      message.success('删除成功');
      gridApi.reload();
    }
  } catch {
    message.error('删除失败');
  }
};

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate();

    const { id, ...submitData } = formData;
    if (isEdit.value && id) {
      await commissionRuleApi.update(id, submitData);
      message.success('更新成功');
    } else {
      await commissionRuleApi.create(
        submitData as Omit<CommissionRule, 'createdAt' | 'id' | 'updatedAt'>,
      );
      message.success('创建成功');
    }

    modalVisible.value = false;
    gridApi.reload();
  } catch {
    message.error(isEdit.value ? '更新失败' : '创建失败');
  }
};

// 取消弹窗
const handleCancel = () => {
  modalVisible.value = false;
  resetForm();
};

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    ruleCode: '',
    ruleName: '',
    firmRate: 30,
    leadLawyerRate: 50,
    assistLawyerRate: 0,
    supportStaffRate: 0,
    originatorRate: 20,
    allowModify: true,
    isDefault: false,
    active: true,
    description: '',
  });
  formRef.value?.resetFields();
};
</script>

<template>
  <Page title="提成规则管理" content-class="p-4">
    <template #extra>
      <Button
        v-if="hasPermission('finance:commission:rule:create')"
        type="primary"
        @click="handleCreate"
      >
        <Plus class="mr-1 size-4" />
        新建规则
      </Button>
    </template>

    <Grid>
      <template #action="{ row }">
        <Space>
          <Button
            v-if="hasPermission('finance:commission:rule:view')"
            type="link"
            size="small"
            @click="handleView(row)"
          >
            查看
          </Button>
          <Button
            v-if="hasPermission('finance:commission:rule:update')"
            type="link"
            size="small"
            @click="handleEdit(row)"
          >
            编辑
          </Button>
          <Popconfirm
            v-if="hasPermission('finance:commission:rule:delete')"
            title="确定要删除这个提成规则吗？"
            @confirm="handleDelete(row)"
          >
            <Button type="link" size="small" danger> 删除 </Button>
          </Popconfirm>
        </Space>
      </template>
    </Grid>

    <!-- 新建/编辑规则弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <Form ref="formRef" :model="formData" :rules="rules" layout="vertical">
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="规则编码" name="ruleCode">
              <Input
                v-model:value="formData.ruleCode"
                placeholder="请输入规则编码"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="规则名称" name="ruleName">
              <Input
                v-model:value="formData.ruleName"
                placeholder="请输入规则名称"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="8">
            <FormItem label="律所比例 (%)" name="firmRate">
              <InputNumber
                v-model:value="formData.firmRate"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
                placeholder="请输入律所比例"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="主办律师比例 (%)" name="leadLawyerRate">
              <InputNumber
                v-model:value="formData.leadLawyerRate"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
                placeholder="请输入主办律师比例"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="协办律师比例 (%)" name="assistLawyerRate">
              <InputNumber
                v-model:value="formData.assistLawyerRate"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
                placeholder="请输入协办律师比例"
              />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="8">
            <FormItem label="辅助人员比例 (%)" name="supportStaffRate">
              <InputNumber
                v-model:value="formData.supportStaffRate"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
                placeholder="请输入辅助人员比例"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="案源人比例 (%)" name="originatorRate">
              <InputNumber
                v-model:value="formData.originatorRate"
                :min="0"
                :max="100"
                :precision="1"
                style="width: 100%"
                placeholder="请输入案源人比例"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="允许修改比例" name="allowModify">
              <Switch v-model:checked="formData.allowModify" />
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="是否默认规则" name="isDefault">
              <Switch v-model:checked="formData.isDefault" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="启用状态" name="active">
              <Switch v-model:checked="formData.active" />
            </FormItem>
          </Col>
        </Row>

        <FormItem label="规则描述" name="description">
          <Textarea
            v-model:value="formData.description"
            :rows="3"
            placeholder="请输入规则描述"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
