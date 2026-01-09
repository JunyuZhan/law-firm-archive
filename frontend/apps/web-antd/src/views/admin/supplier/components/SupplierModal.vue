<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message, Upload, Spin, Tooltip, Alert } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { createSupplier, updateSupplier, getSupplierDetail } from '#/api/admin/supplier';
import type { SupplierDTO } from '#/api/admin/supplier';
import { recognizeBusinessLicense, type OcrResultDTO } from '#/api/ocr';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();
const ocrLoading = ref(false);

const supplierTypeOptions = [
  { label: '服务商', value: 'SERVICE' },
  { label: '供应商', value: 'SUPPLIER' },
  { label: '合作伙伴', value: 'PARTNER' },
];

const ratingOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];

const [Form, formApi] = useVbenForm({
  schema: [
    { fieldName: 'name', label: '供应商名称', component: 'Input', rules: 'required', componentProps: { placeholder: '请输入供应商名称' } },
    { fieldName: 'supplierType', label: '供应商类型', component: 'Select', componentProps: { options: supplierTypeOptions, placeholder: '请选择', allowClear: true } },
    { fieldName: 'contactPerson', label: '联系人', component: 'Input', componentProps: { placeholder: '请输入联系人' } },
    { fieldName: 'contactPhone', label: '联系电话', component: 'Input', componentProps: { placeholder: '请输入联系电话' } },
    { fieldName: 'contactEmail', label: '联系邮箱', component: 'Input', componentProps: { placeholder: '请输入联系邮箱' } },
    { fieldName: 'address', label: '地址', component: 'Input', componentProps: { placeholder: '请输入地址' } },
    { fieldName: 'creditCode', label: '统一社会信用代码', component: 'Input', componentProps: { placeholder: '请输入统一社会信用代码' } },
    { fieldName: 'bankName', label: '开户银行', component: 'Input', componentProps: { placeholder: '请输入开户银行' } },
    { fieldName: 'bankAccount', label: '银行账号', component: 'Input', componentProps: { placeholder: '请输入银行账号' } },
    { fieldName: 'supplyScope', label: '供应范围', component: 'Textarea', componentProps: { rows: 2, placeholder: '请输入供应范围' } },
    { fieldName: 'rating', label: '评级', component: 'Select', componentProps: { options: ratingOptions, placeholder: '请选择', allowClear: true } },
    { fieldName: 'remarks', label: '备注', component: 'Textarea', componentProps: { rows: 2, placeholder: '请输入备注' } },
  ],
  showDefaultActions: false,
  commonConfig: { componentProps: { class: 'w-full' } },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const values = await formApi.validate();
    try {
      if (editingId.value) {
        await updateSupplier(editingId.value, values);
        message.success('更新成功');
      } else {
        await createSupplier(values);
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

async function open(record?: SupplierDTO) {
  if (record) {
    try {
      const detail = await getSupplierDetail(record.id);
      editingId.value = record.id;
      formApi.setValues({
        name: detail.name || '',
        supplierType: detail.supplierType,
        contactPerson: detail.contactPerson || '',
        contactPhone: detail.contactPhone || '',
        contactEmail: detail.contactEmail || '',
        address: detail.address || '',
        creditCode: detail.creditCode || '',
        bankName: detail.bankName || '',
        bankAccount: detail.bankAccount || '',
        supplyScope: detail.supplyScope || '',
        rating: detail.rating,
        remarks: detail.remarks || '',
      });
    } catch (error: any) {
      message.error(error.message || '获取详情失败');
      return;
    }
  }
  modalApi.open();
}

// OCR营业执照识别
async function handleOcrBusinessLicense(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeBusinessLicense(file);
    if (result.success) {
      // 自动填充识别结果
      const values: Record<string, string> = {};
      
      if (result.companyName) values.name = result.companyName;
      if (result.creditCode) values.creditCode = result.creditCode;
      if (result.address) values.address = result.address;
      if (result.legalPerson) values.contactPerson = result.legalPerson;
      
      formApi.setValues(values);
      message.success(`营业执照识别成功！置信度: ${Math.round((result.confidence || 0) * 100)}%`);
    } else {
      message.error(result.errorMessage || '营业执照识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '营业执照识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false; // 阻止自动上传
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑供应商' : '新增供应商'" class="w-[700px]">
    <Spin :spinning="ocrLoading" tip="正在识别营业执照...">
      <!-- OCR识别区域 -->
      <Alert type="info" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-blue-700">营业执照智能识别</span>
          <span class="text-gray-500 text-xs ml-2">上传营业执照自动填充</span>
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="handleOcrBusinessLicense"
              accept="image/*"
            >
              <Tooltip title="上传营业执照图片，自动识别企业名称、统一社会信用代码、地址等">
                <a class="text-blue-600 hover:text-blue-800 font-medium">
                  <IconifyIcon icon="ant-design:scan-outlined" class="mr-1" />识别营业执照
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
