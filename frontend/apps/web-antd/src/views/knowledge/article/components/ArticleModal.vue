<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message, Upload, Spin, Alert, Tooltip, Space } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { createArticle, updateArticle } from '#/api/knowledge';
import { recognizeGeneral, type OcrResultDTO } from '#/api/ocr';
import type { KnowledgeArticleDTO } from '#/api/knowledge/types';

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

// OCR识别文章图片
async function handleOcrArticle(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      // 获取当前内容并追加
      const currentContent = (await formApi.getValues()).content || '';
      const separator = currentContent ? '\n\n--- OCR识别内容 ---\n' : '';
      formApi.setValues({ content: currentContent + separator + result.rawText });
      message.success('OCR识别成功，内容已添加');
    } else {
      message.error(result.errorMessage || 'OCR识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || 'OCR识别失败');
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
          <span class="text-gray-500 text-xs ml-2">上传文章截图自动提取文字</span>
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="handleOcrArticle"
              accept="image/*"
            >
              <Tooltip title="上传文章截图，自动识别文字内容">
                <a class="text-blue-600 hover:text-blue-800 font-medium">
                  <IconifyIcon icon="ant-design:scan-outlined" class="mr-1" />识别图片内容
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
