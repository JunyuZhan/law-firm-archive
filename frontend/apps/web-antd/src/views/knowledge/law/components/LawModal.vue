<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { LawRegulationDTO } from '#/api/knowledge/types';
import type { OcrResultDTO } from '#/api/ocr';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Alert, message, Spin, Tooltip, Upload } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createLawRegulation, updateLawRegulation } from '#/api/knowledge';
import { recognizeGeneral } from '#/api/ocr';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();
const ocrLoading = ref(false);

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
      await formApi.validate();
      const values = await formApi.getValues();

      const data = {
        name: values.name,
        lawType: values.lawType,
        categoryId: values.categoryId,
        issuingAuthority: values.issuingAuthority,
        documentNumber: values.documentNumber,
        effectiveDate: values.effectiveDate,
        status: values.status,
        summary: values.summary,
      };

      if (isEdit.value && editId.value) {
        await updateLawRegulation(editId.value, data);
        message.success('更新成功');
      } else {
        await createLawRegulation(data);
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

// OCR识别法规截图
async function handleOcrLaw(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      const text = result.rawText;

      // 尝试智能解析法规信息
      const parsed: Record<string, any> = {};

      // 提取法规名称（通常在开头）
      const nameMatch = text.match(
        /^(.{2,50}(?:法|条例|规定|办法|细则|解释|通知|意见))/m,
      );
      if (nameMatch?.[1]) {
        parsed.name = nameMatch[1].trim();
      }

      // 提取发布机关
      const issuerMatch = text.match(
        /([\u4E00-\u9FA5]+(?:人大|国务院|最高人民法院|最高人民检察院|公安部|司法部|[\u4E00-\u9FA5]委员会))/,
      );
      if (issuerMatch) {
        parsed.issuer = issuerMatch[1];
      }

      // 设置内容
      parsed.content = text;

      if (Object.keys(parsed).length > 0) {
        formApi.setValues(parsed);
        message.success(
          `法规识别成功！已自动填充 ${Object.keys(parsed).length} 个字段`,
        );
      } else {
        formApi.setValues({ content: text });
        message.success('已将识别内容填充到法规内容中');
      }
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

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[700px]">
    <Spin :spinning="ocrLoading" tip="正在识别法规内容...">
      <!-- OCR识别区域 -->
      <Alert type="info" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-blue-700">智能识别法规</span>
          <span class="ml-2 text-xs text-gray-500">上传法规截图自动填充</span>
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="handleOcrLaw"
              accept="image/*"
            >
              <Tooltip title="上传法规截图，自动识别法规名称、发布机关、内容等">
                <a class="font-medium text-blue-600 hover:text-blue-800">
                  <IconifyIcon
                    icon="ant-design:scan-outlined"
                    class="mr-1"
                  />识别法规截图
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
