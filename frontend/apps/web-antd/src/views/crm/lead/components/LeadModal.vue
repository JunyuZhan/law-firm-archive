<script setup lang="ts">
import { ref, reactive } from 'vue';
import { message, Upload, Spin, Tooltip } from 'ant-design-vue';
import {
  Modal,
  Form,
  FormItem,
  Input,
  Select,
  InputNumber,
  Textarea,
  Space,
  Button,
  Divider,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { createLead, updateLead } from '#/api/client';
import { recognizeBusinessCard, type OcrResultDTO } from '#/api/ocr';
import type { LeadDTO, CreateLeadCommand } from '#/api/client/types';
import { UserTreeSelect } from '#/components/UserTreeSelect';

const emit = defineEmits<{
  success: [];
}>();

const visible = ref(false);
const modalTitle = ref('新增案源');
const formRef = ref();
const loading = ref(false);
const ocrLoading = ref(false);

const formData = reactive<Partial<CreateLeadCommand> & { id?: number }>({
  id: undefined,
  clientName: '',
  contactPerson: '',
  contactPhone: '',
  source: 'REFERRAL',
  matterType: '',
  estimatedAmount: undefined,
  followUpUserId: undefined,
  remark: '',
});

const sourceOptions = [
  { label: '转介绍', value: 'REFERRAL' },
  { label: '网络', value: 'ONLINE' },
  { label: '电话咨询', value: 'PHONE' },
  { label: '上门咨询', value: 'WALK_IN' },
  { label: '其他', value: 'OTHER' },
];

function open(record?: LeadDTO) {
  if (record) {
    modalTitle.value = '编辑案源';
    Object.assign(formData, {
      id: record.id,
      clientName: record.clientName,
      contactPerson: record.contactPerson,
      contactPhone: record.contactPhone,
      source: record.source,
      matterType: record.matterType,
      estimatedAmount: record.estimatedAmount,
      followUpUserId: record.followUpUserId,
      remark: record.remark,
    });
  } else {
    modalTitle.value = '新增案源';
    Object.assign(formData, {
      id: undefined,
      clientName: '',
      contactPerson: '',
      contactPhone: '',
      source: 'REFERRAL',
      matterType: '',
      estimatedAmount: undefined,
      followUpUserId: undefined,
      remark: '',
    });
  }
  visible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();
    loading.value = true;
    
    if (formData.id) {
      await updateLead(formData.id, formData as Partial<CreateLeadCommand>);
      message.success('更新成功');
    } else {
      await createLead(formData as CreateLeadCommand);
      message.success('创建成功');
    }
    visible.value = false;
    emit('success');
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '操作失败');
  } finally {
    loading.value = false;
  }
}

// OCR名片识别
async function handleBusinessCardOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;
  
  ocrLoading.value = true;
  try {
    const result = await recognizeBusinessCard(file);
    if (result.success) {
      // 自动填充表单
      // 公司名称填充到客户名称
      if (result.cardCompany) {
        formData.clientName = result.cardCompany;
      }
      // 联系人姓名
      if (result.name) {
        formData.contactPerson = result.name;
      }
      // 联系电话（优先使用手机号）
      if (result.mobile) {
        formData.contactPhone = result.mobile;
      } else if (result.phone) {
        formData.contactPhone = result.phone;
      }
      
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

defineExpose({ open });
</script>

<template>
  <Modal
    v-model:open="visible"
    :title="modalTitle"
    width="600px"
    :confirm-loading="loading"
    @ok="handleSave"
  >
    <Form
      ref="formRef"
      :model="formData"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <!-- OCR智能识别区域 -->
      <div v-if="!formData.id" class="mb-4 p-3 bg-blue-50 rounded border border-blue-200">
        <div class="flex items-center mb-2">
          <IconifyIcon icon="ant-design:scan-outlined" class="text-blue-500 mr-2" />
          <span class="font-medium text-blue-700">智能填充</span>
          <span class="text-gray-500 text-xs ml-2">上传名片自动识别</span>
        </div>
        <Spin :spinning="ocrLoading" size="small">
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
        </Spin>
      </div>

      <FormItem label="客户名称" name="clientName" :rules="[{ required: true, message: '请输入客户名称' }]">
        <Input v-model:value="formData.clientName" placeholder="请输入客户名称" />
      </FormItem>
      <FormItem label="联系人" name="contactPerson">
        <Input v-model:value="formData.contactPerson" placeholder="请输入联系人" />
      </FormItem>
      <FormItem label="联系电话" name="contactPhone">
        <Input v-model:value="formData.contactPhone" placeholder="请输入联系电话" />
      </FormItem>
      <FormItem label="来源" name="source" :rules="[{ required: true, message: '请选择来源' }]">
        <Select v-model:value="formData.source" :options="sourceOptions" />
      </FormItem>
      <FormItem label="预估金额" name="estimatedAmount">
        <InputNumber
          v-model:value="formData.estimatedAmount"
          :min="0"
          :precision="2"
          style="width: 100%"
          :formatter="(value: any) => `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
          :parser="(value: any) => value.replace(/¥\s?|(,*)/g, '')"
        />
      </FormItem>
      <FormItem label="跟进人" name="followUpUserId">
        <UserTreeSelect
          v-model:value="formData.followUpUserId"
          placeholder="选择跟进人（按部门筛选）"
        />
      </FormItem>
      <FormItem label="备注" name="remark">
        <Textarea v-model:value="formData.remark" :rows="3" placeholder="请输入备注" />
      </FormItem>
    </Form>
  </Modal>
</template>

