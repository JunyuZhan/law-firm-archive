<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreateCauseCommand,
  UpdateCauseCommand,
} from '#/api/system/cause-of-action';
import type { CauseOfActionDTO } from '#/api/system/types';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createCause,
  getCauseById,
  updateCause,
  type CauseType,
} from '#/api/system/cause-of-action';

const emit = defineEmits<{
  success: [];
}>();

const props = defineProps<{
  causeType: CauseType;
  parentCode?: string;
  level?: number;
}>();

const isEdit = ref(false);
const editId = ref<number>();

// 获取代码占位符
function getCodePlaceholder(): string {
  const placeholders = {
    CIVIL: '如：1 或 8.1',
    CRIMINAL: '如：C101',
    ADMIN: '如：A1 或 A1.1',
  };
  return placeholders[props.causeType];
}

// 表单 Schema - 使用 computed 确保响应式更新（符合指南规范）
const formSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'code',
    label: '案由代码',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: getCodePlaceholder(),
      maxlength: 20,
      disabled: isEdit.value, // 编辑时不允许修改代码
    },
  },
  {
    fieldName: 'name',
    label: '案由名称',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入案由名称',
      maxlength: 200,
    },
  },
  {
    fieldName: 'categoryCode',
    label: '分类代码',
    component: 'Input',
    componentProps: {
      placeholder: '请输入分类代码（如：P1, C1, A1）',
      maxlength: 20,
    },
  },
  {
    fieldName: 'categoryName',
    label: '分类名称',
    component: 'Input',
    componentProps: {
      placeholder: '请输入分类名称',
      maxlength: 100,
    },
  },
  {
    fieldName: 'parentCode',
    label: '父级代码',
    component: 'Input',
    componentProps: {
      placeholder: '二级案由需填写父级代码',
      maxlength: 20,
      disabled: false, // 通过表单值动态控制
    },
  },
  {
    fieldName: 'level',
    label: '层级',
    component: 'InputNumber',
    rules: 'required',
    defaultValue: props.level || 1,
    componentProps: {
      min: 1,
      max: 2,
      style: { width: '100%' },
      disabled: true, // 层级由父级决定
    },
  },
  {
    fieldName: 'sortOrder',
    label: '排序号',
    component: 'InputNumber',
    componentProps: {
      placeholder: '自动生成或手动输入',
      min: 0,
      style: { width: '100%' },
    },
  },
  {
    fieldName: 'isActive',
    label: '是否启用',
    component: 'Switch',
    defaultValue: true,
  },
]);

const [Form, formApi] = useVbenForm({
  schema: formSchema as any, // computed 类型需要类型转换
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 100,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    await handleSubmit();
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
async function openCreate(
  parentCode?: string,
  level?: number,
  categoryCode?: string,
  categoryName?: string,
) {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  const currentLevel = level || 1;
  formApi.setValues({
    causeType: props.causeType,
    level: currentLevel,
    parentCode: currentLevel === 1 ? undefined : parentCode,
    categoryCode: categoryCode,
    categoryName: categoryName,
    isActive: true,
  });
  // 更新 schema 以反映 disabled 状态
  formApi.setState({ schema: formSchema.value });
  modalApi.setState({ title: '新增案由' });
  modalApi.open();
}

// 打开编辑弹窗
async function openEdit(id: number) {
  isEdit.value = true;
  editId.value = id;
  formApi.resetForm();

  try {
    const data = await getCauseById(id);
    formApi.setValues({
      code: data.code,
      name: data.name,
      categoryCode: data.categoryCode,
      categoryName: data.categoryName,
      parentCode: data.parentCode,
      level: data.level,
      sortOrder: data.sortOrder,
      isActive: data.isActive,
    });
    // 更新 schema 以反映 disabled 状态
    formApi.setState({ schema: formSchema.value });
    modalApi.setState({ title: '编辑案由' });
    modalApi.open();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载案由详情失败');
  }
}

// 提交表单
async function handleSubmit() {
  try {
    // 先验证表单
    await formApi.validate();
    // 再获取表单值
    const formValues = (await formApi.getValues()) as any;

    // 层级为1时，自动清空父级代码
    if (formValues.level === 1) {
      formValues.parentCode = undefined;
    }

    const command = {
      ...formValues,
      causeType: props.causeType,
    };

    if (isEdit.value && editId.value) {
      await updateCause(editId.value, command as UpdateCauseCommand);
      message.success('更新成功');
    } else {
      await createCause(command as CreateCauseCommand);
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
    message.error(err.message || (isEdit.value ? '更新失败' : '创建失败'));
  }
}

// 监听 isEdit 变化，更新表单 schema（确保 disabled 状态正确）
watch(isEdit, () => {
  formApi.setState({ schema: formSchema.value });
});

defineExpose({
  openCreate,
  openEdit,
});
</script>

<template>
  <Modal class="w-[600px]">
    <Form />
  </Modal>
</template>
