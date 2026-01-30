<script setup lang="ts">
import type { KnowledgeArticleDTO } from '#/api/knowledge/types';
import type { OcrResultDTO } from '#/api/ocr';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Alert, message, Spin, Tooltip, Upload } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createArticle, updateArticle } from '#/api/knowledge';
import { recognizeGeneral } from '#/api/ocr';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();
const ocrLoading = ref(false);

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
    await formApi.validate();
    const values = await formApi.getValues();
    try {
      const data = {
        title: values.title,
        content: values.content,
        categoryId: values.categoryId,
        keywords: values.keywords,
        summary: values.summary,
      };
      if (editingId.value) {
        await updateArticle(editingId.value, data);
        message.success('更新成功');
      } else {
        await createArticle(data);
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

// OCR识别文章图片
async function handleOcrArticle(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      // 获取当前内容并追加
      const formValues = await formApi.getValues();
      const currentContent = formValues.content || '';
      const separator = currentContent ? '\n\n--- OCR识别内容 ---\n' : '';
      formApi.setValues({
        content: currentContent + separator + result.rawText,
      });
      message.success('OCR识别成功，内容已添加');
    } else {
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (error: any) {
    message.error(error?.message || 'OCR识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false;
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑文章' : '新增文章'" class="w-[800px]">
    <Spin :spinning="ocrLoading" tip="正在识别图片内容...">
      <!-- OCR识别区域 -->
      <Alert type="info" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-blue-700">智能识别图片内容</span>
          <span class="ml-2 text-xs text-gray-500"
            >上传文章截图自动提取文字</span
          >
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="handleOcrArticle"
              accept="image/*"
            >
              <Tooltip title="上传文章截图，自动识别文字内容">
                <a class="font-medium text-blue-600 hover:text-blue-800">
                  <IconifyIcon
                    icon="ant-design:scan-outlined"
                    class="mr-1"
                  />识别图片内容
                </a>
              </Tooltip>
            </Upload>
          </div>
        </template>
      </Alert>

      <Form />
    </Spin>
  </Modal>
</template>
