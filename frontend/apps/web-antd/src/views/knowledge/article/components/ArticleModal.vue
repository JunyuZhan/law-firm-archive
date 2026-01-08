<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createArticle, updateArticle } from '#/api/knowledge';
import type { KnowledgeArticleDTO } from '#/api/knowledge/types';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();

const categoryOptions = [
  { label: '法律法规', value: 'LAW' },
  { label: '实务经验', value: 'PRACTICE' },
  { label: '文书模板', value: 'TEMPLATE' },
  { label: '行业动态', value: 'NEWS' },
];

const [Form, formApi] = useVbenForm({
  schema: [
    {
      fieldName: 'title',
      label: '文章标题',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入文章标题' },
    },
    {
      fieldName: 'category',
      label: '文章分类',
      component: 'Select',
      componentProps: { options: categoryOptions, placeholder: '请选择分类' },
    },
    {
      fieldName: 'content',
      label: '文章内容',
      component: 'Textarea',
      rules: 'required',
      componentProps: { rows: 10, placeholder: '请输入文章内容' },
    },
  ],
  showDefaultActions: false,
  commonConfig: { componentProps: { class: 'w-full' } },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const values = await formApi.validate();
    try {
      if (editingId.value) {
        await updateArticle(editingId.value, values);
        message.success('更新成功');
      } else {
        await createArticle(values);
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

function open(record?: KnowledgeArticleDTO) {
  if (record) {
    editingId.value = record.id;
    formApi.setValues({
      title: record.title,
      category: record.category || '',
      content: record.content || '',
    });
  }
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑文章' : '新增文章'" class="w-[800px]">
    <Form />
  </Modal>
</template>
