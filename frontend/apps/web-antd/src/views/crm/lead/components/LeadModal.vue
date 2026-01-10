<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message, Upload, Spin, Tooltip, Alert, Button, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { createLead, updateLead } from '#/api/client';
import { recognizeBusinessCard, OCR_DISABLED, OCR_DISABLED_MESSAGE } from '#/api/ocr';
import type { LeadDTO, CreateLeadCommand } from '#/api/client/types';

const emit = defineEmits<{
  success: [];
}>();

const editingId = ref<number>();
const ocrLoading = ref(false);

// 来源选项
const sourceOptions = [
  { label: '转介绍', value: 'REFERRAL' },
  { label: '网络', value: 'ONLINE' },
  { label: '电话咨询', value: 'PHONE' },
  { label: '上门咨询', value: 'WALK_IN' },
  { label: '其他', value: 'OTHER' },
];

// 表单 Schema
const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'clientName',
    label: '客户名称',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入客户名称',
    },
  },
  {
    fieldName: 'contactPerson',
    label: '联系人',
    component: 'Input',
    componentProps: {
      placeholder: '请输入联系人',
    },
  },
  {
    fieldName: 'contactPhone',
    label: '联系电话',
    component: 'Input',
    componentProps: {
      placeholder: '请输入联系电话',
    },
  },
  {
    fieldName: 'source',
    label: '来源',
    component: 'Select',
    rules: 'required',
    defaultValue: 'REFERRAL',
    componentProps: {
      options: sourceOptions,
      placeholder: '请选择来源',
    },
  },
  {
    fieldName: 'matterType',
    label: '案件类型',
    component: 'Input',
    componentProps: {
      placeholder: '请输入案件类型',
    },
  },
  {
    fieldName: 'estimatedAmount',
    label: '预估金额',
    component: 'InputNumber',
    componentProps: {
      min: 0,
      precision: 2,
      style: { width: '100%' },
      formatter: (value: any) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ','),
      parser: (value: any) => value.replace(/¥\s?|(,*)/g, ''),
      placeholder: '请输入预估金额',
    },
  },
  {
    fieldName: 'followUpUserId',
    label: '跟进人',
    component: 'UserTreeSelect',
    componentProps: {
      placeholder: '选择跟进人（按部门筛选）',
      allowClear: true,
    },
  },
  {
    fieldName: 'remark',
    label: '备注',
    component: 'Textarea',
    componentProps: {
      rows: 3,
      placeholder: '请输入备注',
    },
  },
];

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 80,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      const values = await formApi.validate();
      
      if (editingId.value) {
        await updateLead(editingId.value, values as unknown as Partial<CreateLeadCommand>);
        message.success('更新成功');
      } else {
        await createLead(values as unknown as CreateLeadCommand);
        message.success('创建成功');
      }
      
      modalApi.close();
      emit('success');
    } catch (error: any) {
      if (error?.errorFields) return;
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

// OCR名片识别
async function handleBusinessCardOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;
  
  ocrLoading.value = true;
  try {
    const result = await recognizeBusinessCard(file);
    if (result.success) {
      const values: Record<string, any> = {};
      // 公司名称填充到客户名称
      if (result.cardCompany) {
        values.clientName = result.cardCompany;
      }
      // 联系人姓名
      if (result.name) {
        values.contactPerson = result.name;
      }
      // 联系电话（优先使用手机号）
      if (result.mobile) {
        values.contactPhone = result.mobile;
      } else if (result.phone) {
        values.contactPhone = result.phone;
      }
      
      formApi.setValues(values);
      message.success(`名片识别成功！已自动填充联系人信息`);
    } else {
      message.error(result.errorMessage || '名片识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '名片识别失败');
  } finally {
    ocrLoading.value = false;
  }
}

// 打开新增弹窗
function openCreate() {
  editingId.value = undefined;
  formApi.resetForm();
  formApi.setValues({ source: 'REFERRAL' });
  modalApi.setState({ title: '新增案源' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: LeadDTO) {
  editingId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    clientName: record.clientName,
    contactPerson: record.contactPerson,
    contactPhone: record.contactPhone,
    source: record.source,
    matterType: record.matterType,
    estimatedAmount: record.estimatedAmount,
    followUpUserId: record.followUpUserId,
    remark: record.remark,
  });
  modalApi.setState({ title: '编辑案源' });
  modalApi.open();
}

// 兼容旧的 open 方法
function open(record?: LeadDTO) {
  if (record) {
    openEdit(record);
  } else {
    openCreate();
  }
}

defineExpose({ open, openCreate, openEdit });
</script>

<template>
  <Modal class="w-[600px]">
    <Spin :spinning="ocrLoading" tip="正在识别名片...">
      <!-- OCR智能识别区域 - 仅新增时显示 -->
      <Alert v-if="!editingId && !OCR_DISABLED" type="info" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-blue-700">智能填充</span>
          <span class="text-gray-500 text-xs ml-2">上传名片自动识别</span>
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="() => false"
              accept="image/*"
              @change="handleBusinessCardOcr"
            >
              <Tooltip title="拍照或上传名片，自动识别姓名、电话等信息">
                <Button :loading="ocrLoading" :disabled="ocrLoading" size="small">
                  <template #icon><IconifyIcon icon="ant-design:idcard-outlined" /></template>
                  名片识别
                </Button>
              </Tooltip>
            </Upload>
          </div>
        </template>
      </Alert>
      
      <!-- OCR禁用提示 -->
      <Alert v-else-if="!editingId && OCR_DISABLED" type="warning" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-gray-500">智能填充</span>
          <Tag color="default" class="ml-2">暂不可用</Tag>
        </template>
        <template #description>
          <div class="text-gray-400 text-xs">{{ OCR_DISABLED_MESSAGE }}</div>
        </template>
      </Alert>

      <Form />
    </Spin>
  </Modal>
</template>
