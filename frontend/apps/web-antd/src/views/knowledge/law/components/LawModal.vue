<script setup lang="ts">
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createLawRegulation, updateLawRegulation } from '#/api/knowledge';
import type { LawRegulationDTO, CreateLawRegulationCommand } from '#/api/knowledge/types';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();

// 法规类型选项
const lawTypeOptions = [
  { label: '法律', value: 'LAW' },
  { label: '行政法规', value: 'REGULATION' },
  { label: '部门规章', value: 'RULE' },
  { label: '司法解释', value: 'INTERPRETATION' },
  { label: '地方性法规', value: 'LOCAL' },
];

// 表单 Schema
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'name',
    label: '法规名称',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入法规名称',
    },
  },
  {
    fieldName: 'lawType',
    label: '法规类型',
    component: 'Select',
    rules: 'required',
    defaultValue: 'LAW',
    componentProps: {
      options: lawTypeOptions,
    },
  },
  {
    fieldName: 'issuer',
    label: '发布机关',
    component: 'Input',
    componentProps: {
      placeholder: '请输入发布机关',
    },
  },
  {
    fieldName: 'issueDate',
    label: '发布日期',
    component: 'DatePicker',
    componentProps: {
      placeholder: '请选择发布日期',
      style: { width: '100%' },
      valueFormat: 'YYYY-MM-DD',
    },
  },
  {
    fieldName: 'effectiveDate',
    label: '实施日期',
    component: 'DatePicker',
    componentProps: {
      placeholder: '请选择实施日期',
      style: { width: '100%' },
      valueFormat: 'YYYY-MM-DD',
    },
  },
  {
    fieldName: 'content',
    label: '法规内容',
    component: 'Textarea',
    componentProps: {
      placeholder: '请输入法规内容',
      rows: 6,
    },
  },
];

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 100,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      const values = await formApi.validate();
      
      if (isEdit.value && editId.value) {
        await updateLawRegulation(editId.value, values);
        message.success('更新成功');
      } else {
        await createLawRegulation(values as CreateLawRegulationCommand);
        message.success('创建成功');
      }
      
      modalApi.close();
      emit('success');
    } catch (error: unknown) {
      const err = error as { errorFields?: unknown; message?: string };
      if (err?.errorFields) return;
      message.error(err.message || '操作失败');
    }
  },
});

// 打开新增弹窗
function openCreate() {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  modalApi.setState({ title: '新增法规' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: LawRegulationDTO) {
  isEdit.value = true;
  editId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    name: record.name,
    lawType: record.lawType,
    issuer: record.issuer || '',
    issueDate: record.issueDate || '',
    effectiveDate: record.effectiveDate || '',
    content: record.content || '',
  });
  modalApi.setState({ title: '编辑法规' });
  modalApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[650px]">
    <Form />
  </Modal>
</template>
