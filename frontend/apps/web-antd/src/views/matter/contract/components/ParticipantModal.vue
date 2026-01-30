<script setup lang="ts">
import type { ContractParticipantDTO } from '#/api/finance/types';

import { ref, watch } from 'vue';

import {
  Form,
  FormItem,
  InputNumber,
  Modal,
  Select,
  Textarea,
} from 'ant-design-vue';

import { UserTreeSelect } from '#/components/UserTreeSelect';

interface Props {
  open?: boolean;
  participant?: ContractParticipantDTO | null;
  roleOptions?: { label: string; value: string }[];
}

interface Emits {
  (e: 'update:open', value: boolean): void;
  (e: 'save', data: CreateParticipantData): void;
}

export interface CreateParticipantData {
  userId?: number;
  role?: string;
  commissionRate?: number;
  remark?: string;
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  participant: null,
  roleOptions: () => [],
});

const emit = defineEmits<Emits>();

const formRef = ref();
const formData = ref<CreateParticipantData>({
  userId: undefined,
  role: undefined,
  commissionRate: undefined,
  remark: '',
});

// 监听 participant 变化，更新表单数据
watch(
  () => props.participant,
  (val) => {
    if (val) {
      formData.value = {
        userId: val.userId,
        role: val.role,
        commissionRate: val.commissionRate,
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
    userId: undefined,
    role: undefined,
    commissionRate: undefined,
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
    :title="participant?.id ? '编辑参与人' : '添加参与人'"
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
        label="人员"
        name="userId"
        :rules="[{ required: true, message: '请选择人员' }]"
      >
        <UserTreeSelect
          v-model:value="formData.userId"
          placeholder="选择人员（按部门筛选）"
          :disabled="!!participant?.id"
        />
      </FormItem>
      <FormItem
        label="角色"
        name="role"
        :rules="[{ required: true, message: '请选择角色' }]"
      >
        <Select
          v-model:value="formData.role"
          :options="roleOptions"
          placeholder="请选择角色"
        />
      </FormItem>
      <FormItem label="提成比例(%)" name="commissionRate">
        <InputNumber
          v-model:value="formData.commissionRate"
          :min="0"
          :max="100"
          :precision="1"
          style="width: 100%"
        />
      </FormItem>
      <FormItem label="备注" name="remark">
        <Textarea v-model:value="formData.remark" :rows="2" />
      </FormItem>
    </Form>
  </Modal>
</template>
