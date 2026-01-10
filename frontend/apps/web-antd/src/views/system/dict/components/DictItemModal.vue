<script setup lang="ts">
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createDictItem, updateDictItem } from '#/api/system';
import type {
  DictDataDTO,
  CreateDictItemCommand,
} from '#/api/system/types';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();
const dictTypeId = ref<number>();

// 表单 Schema
const formSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'label',
    label: '标签',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入标签名称',
      maxlength: 50,
    },
  },
  {
    fieldName: 'value',
    label: '值',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入字典值',
      maxlength: 100,
    },
  },
  {
    fieldName: 'sortOrder',
    label: '排序',
    component: 'InputNumber',
    defaultValue: 0,
    componentProps: {
      placeholder: '请输入排序号',
      min: 0,
      style: { width: '100%' },
    },
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
  {
    fieldName: 'cssClass',
    label: 'CSS类名',
    component: 'Input',
    componentProps: {
      placeholder: '请输入CSS类名（可选）',
      maxlength: 50,
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
      const values = await formApi.validate<CreateDictItemCommand>();
      const submitData: CreateDictItemCommand = {
        ...values,
        dictTypeId: dictTypeId.value!,
      };
      if (isEdit.value && editId.value) {
        await updateDictItem(editId.value, submitData);
        message.success('更新成功');
      } else {
        await createDictItem(submitData);
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
      dictTypeId.value = undefined;
    }
  },
});

// 打开创建弹窗
function openCreate(typeId: number) {
  isEdit.value = false;
  editId.value = undefined;
  dictTypeId.value = typeId;
  formApi.resetForm();
  modalApi.setState({ title: '新增字典项' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: DictDataDTO) {
  isEdit.value = true;
  editId.value = record.id;
  dictTypeId.value = record.dictTypeId;
  formApi.resetForm();
  formApi.setValues({
    label: record.label,
    value: record.value,
    sortOrder: record.sortOrder || 0,
    description: record.description || '',
    cssClass: record.cssClass || '',
  });
  modalApi.setState({ title: '编辑字典项' });
  modalApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[600px]">
    <Form />
  </Modal>
</template>

