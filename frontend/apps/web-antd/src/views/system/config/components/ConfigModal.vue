<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { updateConfig } from '#/api/system';
import type { SysConfigDTO } from '#/api/system/types';

const emit = defineEmits<{
  success: [];
}>();

const editId = ref<number>();

// 表单 Schema
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'configName',
    label: '配置名称',
    component: 'Input',
    componentProps: {
      disabled: true,
    },
  },
  {
    fieldName: 'configKey',
    label: '配置键',
    component: 'Input',
    componentProps: {
      disabled: true,
    },
  },
  {
    fieldName: 'configValue',
    label: '配置值',
    component: 'Textarea',
    rules: 'required',
    componentProps: {
      placeholder: '请输入配置值',
      rows: 4,
    },
  },
  {
    fieldName: 'description',
    label: '备注',
    component: 'Textarea',
    componentProps: {
      placeholder: '请输入备注',
      rows: 3,
    },
  },
];

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 80,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      const values = await formApi.validate();
      
      if (editId.value) {
        await updateConfig(editId.value, {
          configValue: values.configValue,
          description: values.description,
        });
        message.success('更新成功');
        modalApi.close();
        emit('success');
      }
    } catch (error: unknown) {
      const err = error as { errorFields?: unknown; message?: string };
      if (err?.errorFields) return;
      message.error(err.message || '操作失败');
    }
  },
});

// 打开编辑弹窗
function openEdit(record: SysConfigDTO) {
  editId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    configName: record.configName,
    configKey: record.configKey,
    configValue: record.configValue,
    description: record.description || '',
  });
  modalApi.setState({ title: '编辑配置' });
  modalApi.open();
}

defineExpose({ openEdit });
</script>

<template>
  <Modal class="w-[550px]">
    <Form />
  </Modal>
</template>
