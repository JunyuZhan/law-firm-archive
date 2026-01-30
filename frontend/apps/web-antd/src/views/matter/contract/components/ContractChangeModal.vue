<script setup lang="ts">
import type { ContractDTO } from '#/api/finance/types';

import { reactive, ref, watch } from 'vue';

import {
  Col,
  DatePicker,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Row,
  Select,
  Textarea,
} from 'ant-design-vue';

interface Props {
  open?: boolean;
  contract?: ContractDTO | null;
  isMobile?: boolean;
  feeTypeOptions?: { label: string; value: string }[];
}

interface Emits {
  (e: 'update:open', value: boolean): void;
  (e: 'submit', data: ContractChangeData): void;
}

export interface ContractChangeData {
  changeReason: string;
  changeDescription?: string;
  name?: string;
  totalAmount?: number;
  feeType?: string;
  expiryDate?: string;
  paymentTerms?: string;
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  contract: null,
  isMobile: false,
  feeTypeOptions: () => [],
});

const emit = defineEmits<Emits>();

const formRef = ref();
const formData = reactive<ContractChangeData>({
  changeReason: '',
  changeDescription: '',
  name: undefined,
  totalAmount: undefined,
  feeType: undefined,
  expiryDate: undefined,
  paymentTerms: undefined,
});

// 监听 contract 变化，初始化表单数据
watch(
  () => props.contract,
  (val) => {
    if (val) {
      formData.name = val.name;
      formData.totalAmount = val.totalAmount;
      formData.feeType = val.feeType;
      formData.expiryDate = val.expiryDate;
      formData.paymentTerms = val.paymentTerms;
      formData.changeReason = '';
      formData.changeDescription = '';
    }
  },
  { immediate: true },
);

function resetForm() {
  formData.changeReason = '';
  formData.changeDescription = '';
  formData.name = props.contract?.name;
  formData.totalAmount = props.contract?.totalAmount;
  formData.feeType = props.contract?.feeType;
  formData.expiryDate = props.contract?.expiryDate;
  formData.paymentTerms = props.contract?.paymentTerms;
  formRef.value?.clearValidate();
}

async function handleOk() {
  try {
    await formRef.value?.validate();
    emit('submit', { ...formData });
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
    title="合同变更申请"
    :width="isMobile ? '100%' : '800px'"
    :centered="isMobile"
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
        label="变更原因"
        name="changeReason"
        :rules="[{ required: true, message: '请输入变更原因' }]"
      >
        <Input
          v-model:value="formData.changeReason"
          placeholder="请输入变更原因"
        />
      </FormItem>
      <FormItem label="变更说明" name="changeDescription">
        <Textarea
          v-model:value="formData.changeDescription"
          :rows="2"
          placeholder="请详细说明变更内容"
        />
      </FormItem>
      <Divider>变更内容</Divider>
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="合同名称" name="name">
            <Input v-model:value="formData.name" />
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="合同金额" name="totalAmount">
            <InputNumber
              v-model:value="formData.totalAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
              prefix="¥"
            />
          </FormItem>
        </Col>
      </Row>
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="收费方式" name="feeType">
            <Select
              v-model:value="formData.feeType"
              :options="feeTypeOptions"
            />
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="到期日期" name="expiryDate">
            <DatePicker
              v-model:value="formData.expiryDate"
              style="width: 100%"
              format="YYYY-MM-DD"
            />
          </FormItem>
        </Col>
      </Row>
      <FormItem
        label="付款条款"
        name="paymentTerms"
        :label-col="{ span: 3 }"
        :wrapper-col="{ span: 21 }"
      >
        <Textarea v-model:value="formData.paymentTerms" :rows="2" />
      </FormItem>
    </Form>
  </Modal>
</template>
