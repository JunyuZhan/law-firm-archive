<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { SysConfigDTO } from '#/api/system/types';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createConfig, updateConfig } from '#/api/system';

const emit = defineEmits<{
  success: [];
}>();

const editId = ref<number>();
const isEdit = ref(false);

// 表单 Schema - 使用 computed 确保响应式更新（符合指南规范）
const formSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'configName',
    label: '配置名称',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入配置名称',
      disabled: isEdit.value,
    },
  },
  {
    fieldName: 'configKey',
    label: '配置键',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '如：firm.name',
      disabled: isEdit.value,
    },
  },
  {
    fieldName: 'configType',
    label: '配置类型',
    component: 'Select',
    rules: 'required',
    defaultValue: 'STRING',
    componentProps: {
      disabled: isEdit.value,
      placeholder: '请选择配置类型',
      options: [
        { label: '字符串', value: 'STRING' },
        { label: '数字', value: 'NUMBER' },
        { label: '布尔值', value: 'BOOLEAN' },
        { label: 'JSON', value: 'JSON' },
      ],
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
      placeholder: '请输入备注（可选）',
      rows: 3,
    },
  },
]);

const [Form, formApi] = useVbenForm({
  schema: formSchema as any, // computed 类型需要类型转换
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 80,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      // 使用 getValues 获取表单值，更可靠
      const formValues = (await formApi.getValues()) as any;

      if (isEdit.value && editId.value) {
        // 编辑模式 - 只更新 configValue 和 description
        const updateData: { configValue: string; description?: string } = {
          configValue: String(formValues?.configValue ?? ''), // 确保是字符串
        };
        if (formValues?.description !== undefined) {
          updateData.description = String(formValues.description ?? '');
        }

        // 先验证表单
        await formApi.validate();

        await updateConfig(editId.value, updateData);
        message.success('更新成功');
      } else {
        // 新增模式 - 先验证表单
        await formApi.validate();

        await createConfig({
          configKey: String(formValues?.configKey ?? ''),
          configValue: String(formValues?.configValue ?? ''),
          configName: String(formValues?.configName ?? ''),
          configType: String(formValues?.configType || 'STRING'),
          description: String(formValues?.description ?? ''),
        });
        message.success('创建成功');
      }
      modalApi.close();
      // 延迟一下再触发 success 事件，确保弹窗关闭后再刷新列表
      setTimeout(() => {
        emit('success');
      }, 300);
    } catch (error: unknown) {
      const err = error as { errorFields?: unknown; message?: string };
      if (err?.errorFields) return;
      message.error(err.message || '操作失败');
    }
  },
});

// 打开编辑弹窗
function openEdit(record: SysConfigDTO) {
  isEdit.value = true;
  editId.value = record.id;

  // 先打开弹窗，再设置表单值，确保表单已渲染
  modalApi.setState({ title: '编辑配置' });
  modalApi.open();

  // 使用 setTimeout 确保弹窗和表单都已渲染完成
  setTimeout(() => {
    formApi.resetForm();
    formApi.setValues({
      configName: record.configName || '',
      configKey: record.configKey || '',
      configType: record.configType || 'STRING',
      configValue: record.configValue ?? '', // 使用 ?? 确保 null/undefined 转为空字符串
      description: record.description ?? '',
    });
  }, 100);
}

// 打开新增弹窗
function openCreate() {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  formApi.setValues({
    configName: '',
    configKey: '',
    configType: 'STRING',
    configValue: '',
    description: '',
  });
  modalApi.setState({ title: '新增配置' });
  modalApi.open();
}

defineExpose({ openEdit, openCreate });
</script>

<template>
  <Modal class="w-[550px]">
    <Form />
  </Modal>
</template>
