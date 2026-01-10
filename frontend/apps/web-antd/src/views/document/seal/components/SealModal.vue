<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createSeal, updateSeal } from '#/api/document/seal';
import type { SealDTO } from '#/api/document/seal-types';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();

const sealTypeOptions = [
  { label: '公章', value: 'OFFICIAL' },
  { label: '合同章', value: 'CONTRACT' },
  { label: '财务章', value: 'FINANCE' },
  { label: '法人章', value: 'LEGAL' },
];

const [Form, formApi] = useVbenForm({
  schema: [
    { fieldName: 'name', label: '印章名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入印章名称' } },
    { fieldName: 'sealType', label: '印章类型', component: 'Select', rules: 'required', componentProps: { options: sealTypeOptions } },
    { fieldName: 'keeperId', label: '保管人', component: 'UserTreeSelect', componentProps: { placeholder: '选择保管人', allowClear: true } },
    { fieldName: 'imageUrl', label: '印章图片URL', component: 'Input', componentProps: { placeholder: '请输入印章图片URL' } },
    { fieldName: 'description', label: '描述', component: 'Textarea', componentProps: { rows: 3, placeholder: '请输入描述' } },
  ],
  showDefaultActions: false,
  commonConfig: { componentProps: { class: 'w-full' } },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const values = await formApi.validate();
    try {
      if (editingId.value) {
        await updateSeal(editingId.value, values);
        message.success('更新成功');
      } else {
        await createSeal(values);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      formApi.resetForm();
      editingId.value = undefined;
    }
  },
});

function open(record?: SealDTO) {
  if (record) {
    editingId.value = record.id;
    formApi.setValues({
      name: record.name,
      sealType: record.sealType,
      keeperId: record.keeperId,
      imageUrl: record.imageUrl || '',
      description: record.description || '',
    });
  }
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑印章' : '添加印章'" class="w-[600px]">
    <Form />
  </Modal>
</template>
