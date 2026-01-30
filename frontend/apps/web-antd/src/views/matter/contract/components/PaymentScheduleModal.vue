<script setup lang="ts">
import type { ContractPaymentScheduleDTO } from '#/api/finance/types';

import { ref, watch } from 'vue';

import {
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Textarea,
} from 'ant-design-vue';

interface Props {
  open?: boolean;
  schedule?: ContractPaymentScheduleDTO | null;
}

interface Emits {
  (e: 'update:open', value: boolean): void;
  (e: 'save', data: CreatePaymentScheduleData): void;
}

export interface CreatePaymentScheduleData {
  phaseName?: string;
  amount?: number;
  percentage?: number;
  plannedDate?: string;
  remark?: string;
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  schedule: null,
});

const emit = defineEmits<Emits>();

const formRef = ref();
const formData = ref<CreatePaymentScheduleData>({
  phaseName: '',
  amount: undefined,
  percentage: undefined,
  plannedDate: undefined,
  remark: '',
});

// 监听 schedule 变化，更新表单数据
watch(
  () => props.schedule,
  (val) => {
    if (val) {
      formData.value = {
        phaseName: val.phaseName || '',
        amount: val.amount,
        percentage: val.percentage,
        plannedDate: val.plannedDate,
        remark: val.remark,
      };
    } else {
      resetForm();
    }
  },
  { immediate: true },
);

function resetForm() {
  formData.value = {
    phaseName: '',
    amount: undefined,
    percentage: undefined,
    plannedDate: undefined,
    remark: '',
  };
  formRef.value?.clearValidate();
}

async function handleOk() {
  try {
    await formRef.value?.validate();
    emit('save', { ...formData.value });
  } catch {
    // 验证失败，不关闭弹窗
  }
}

function handleCancel() {
  emit('update:open', false);
}

// 暴露重置方法
defineExpose({
  resetForm,
});
</script>

<template>
  <Modal
    :open="open"
    :title="schedule?.id ? '编辑付款计划' : '添加付款计划'"
    width="500px"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <Form
      ref="formRef"
      :model="formData"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <FormItem
        label="阶段名称"
        name="phaseName"
        :rules="[{ required: true, message: '请输入阶段名称' }]"
      >
        <Input
          v-model:value="formData.phaseName"
          placeholder="如：首付款、尾款"
        />
      </FormItem>
      <FormItem label="金额" name="amount">
        <InputNumber
          v-model:value="formData.amount"
          :min="0"
          :precision="2"
          style="width: 100%"
          prefix="¥"
        />
      </FormItem>
      <FormItem label="比例(%)" name="percentage">
        <InputNumber
          v-model:value="formData.percentage"
          :min="0"
          :max="100"
          :precision="1"
          style="width: 100%"
        />
      </FormItem>
      <FormItem label="计划日期" name="plannedDate">
        <DatePicker
          v-model:value="formData.plannedDate"
          style="width: 100%"
          format="YYYY-MM-DD"
        />
      </FormItem>
      <FormItem label="备注" name="remark">
        <Textarea v-model:value="formData.remark" :rows="2" />
      </FormItem>
    </Form>
  </Modal>
</template>
