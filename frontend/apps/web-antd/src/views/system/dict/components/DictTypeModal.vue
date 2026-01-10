<script setup lang="ts">
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createDictType, updateDictType } from '#/api/system';
import type {
  DictTypeDTO,
  CreateDictTypeCommand,
} from '#/api/system/types';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();

// 表单 Schema
const formSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'name',
    label: '字典名称',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入字典名称',
      maxlength: 50,
    },
  },
  {
    fieldName: 'code',
    label: '字典编码',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入字典编码（字母开头，只能包含字母、数字和下划线）',
      maxlength: 50,
    },
    helpMessage: '只能包含字母、数字和下划线，以字母开头，长度1-50',
  },
  {
    fieldName: 'description',
    label: '描述',
    component: 'InputTextarea',
    componentProps: {
      placeholder: '请输入描述信息',
      rows: 3,
      maxlength: 200,
    },
  },
]);

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
      const values = await formApi.validate<CreateDictTypeCommand>();
      if (isEdit.value && editId.value) {
        await updateDictType(editId.value, values);
        message.success('更新成功');
      } else {
        await createDictType(values);
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
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formApi.resetForm();
      isEdit.value = false;
      editId.value = undefined;
    }
  },
});

// 打开创建弹窗
function openCreate() {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  modalApi.setState({ title: '新增字典类型' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: DictTypeDTO) {
  isEdit.value = true;
  editId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    name: record.name,
    code: record.code,
    description: record.description || '',
  });
  modalApi.setState({ title: '编辑字典类型' });
  modalApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[600px]">
    <Form />
  </Modal>
</template>

