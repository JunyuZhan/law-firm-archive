<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message, Form, FormItem, Input, InputNumber, Button, Space, Divider, Textarea, Row, Col, Tooltip, Switch } from 'ant-design-vue';
import { commissionRuleApi, type CommissionRule } from '#/api/finance/commission-rule';

const emit = defineEmits<{
  success: [];
}>();

const editingId = ref<number>();
const saving = ref(false);

const formData = reactive<CommissionRule>({
  ruleCode: '',
  ruleName: '',
  firmRate: 0,
  leadLawyerRate: 0,
  assistLawyerRate: 0,
  supportStaffRate: 0,
  originatorRate: 0,
  allowModify: true,
  isDefault: false,
  active: true,
  description: '',
});

// 计算总比例
const totalRate = computed(() => {
  return formData.firmRate + formData.leadLawyerRate + formData.assistLawyerRate + formData.supportStaffRate + formData.originatorRate;
});

const [Modal, modalApi] = useVbenModal({
  footer: false,
  onOpenChange(isOpen) {
    if (!isOpen) {
      resetForm();
    }
  },
});

// 重置表单
function resetForm() {
  editingId.value = undefined;
  Object.assign(formData, {
    ruleCode: '',
    ruleName: '',
    firmRate: 0,
    leadLawyerRate: 0,
    assistLawyerRate: 0,
    supportStaffRate: 0,
    originatorRate: 0,
    allowModify: true,
    isDefault: false,
    active: true,
    description: '',
  });
}

// 打开新增弹窗
function openCreate() {
  resetForm();
  modalApi.setState({ title: '新增提成方案' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: CommissionRule) {
  editingId.value = record.id;
  Object.assign(formData, record);
  modalApi.setState({ title: '编辑提成方案' });
  modalApi.open();
}

// 保存
async function handleSave() {
  if (!formData.ruleCode) {
    message.error('请输入方案编码');
    return;
  }
  if (!formData.ruleName) {
    message.error('请输入方案名称');
    return;
  }

  saving.value = true;
  try {
    if (editingId.value) {
      await commissionRuleApi.update(editingId.value, formData);
      message.success('更新成功');
    } else {
      await commissionRuleApi.create(formData as CommissionRule);
      message.success('创建成功');
    }
    modalApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

defineExpose({
  openCreate,
  openEdit,
});
</script>

<template>
  <Modal class="w-[600px]">
    <Form layout="vertical">
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="方案编码" required>
            <Input v-model:value="formData.ruleCode" placeholder="如：SOLO" :disabled="!!editingId" />
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="方案名称" required>
            <Input v-model:value="formData.ruleName" placeholder="如：独立办案" />
          </FormItem>
        </Col>
      </Row>

      <Divider orientation="left" style="margin: 12px 0;">
        分配比例（0%表示不参与分配）
      </Divider>

      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="律所比例">
            <Tooltip title="0表示律所不参与分配">
              <InputNumber
                v-model:value="formData.firmRate"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                style="width: 100%"
              />
            </Tooltip>
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="主办律师比例">
            <Tooltip title="0表示主办律师不参与分配">
              <InputNumber
                v-model:value="formData.leadLawyerRate"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                style="width: 100%"
              />
            </Tooltip>
          </FormItem>
        </Col>
      </Row>

      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="协办律师比例">
            <Tooltip title="0表示无协办律师或不参与分配">
              <InputNumber
                v-model:value="formData.assistLawyerRate"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                style="width: 100%"
              />
            </Tooltip>
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="辅助人员比例">
            <Tooltip title="0表示无辅助人员或不参与分配">
              <InputNumber
                v-model:value="formData.supportStaffRate"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                style="width: 100%"
              />
            </Tooltip>
          </FormItem>
        </Col>
      </Row>
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="案源人比例">
            <Tooltip title="0表示无案源人或不参与分配">
              <InputNumber
                v-model:value="formData.originatorRate"
                :min="0"
                :max="100"
                :precision="2"
                addon-after="%"
                style="width: 100%"
              />
            </Tooltip>
          </FormItem>
        </Col>
      </Row>

      <div style=" padding: 8px; margin-bottom: 16px;text-align: center; background: #f5f5f5; border-radius: 4px;">
        <span style="color: #666;">当前合计：</span>
        <strong :style="{ color: totalRate === 100 ? '#52c41a' : '#1890ff' }">{{ totalRate.toFixed(2) }}%</strong>
        <span v-if="totalRate !== 100" style=" margin-left: 8px; font-size: 12px;color: #999;">
          （比例之和不强制=100%）
        </span>
      </div>

      <FormItem label="方案说明">
        <Textarea v-model:value="formData.description" :rows="2" placeholder="描述此方案的适用场景" />
      </FormItem>

      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="允许律师修改">
            <Switch v-model:checked="formData.allowModify" />
            <span style="margin-left: 8px; font-size: 12px; color: #666;">创建合同时可自定义比例</span>
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="启用状态">
            <Switch v-model:checked="formData.active" />
            <span style="margin-left: 8px; font-size: 12px; color: #666;">停用后不可选择</span>
          </FormItem>
        </Col>
      </Row>

      <Divider style="margin: 12px 0" />

      <div style="text-align: right;">
        <Space>
          <Button @click="modalApi.close()">取消</Button>
          <Button type="primary" :loading="saving" @click="handleSave">
            保存
          </Button>
        </Space>
      </div>
    </Form>
  </Modal>
</template>
