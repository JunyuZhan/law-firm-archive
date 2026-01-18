<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import {
  Button,
  Form,
  FormItem,
  Input,
  message,
  Select,
  Space,
  Textarea,
} from 'ant-design-vue';
import { useResponsive } from '#/hooks/useResponsive';
import {
  createDossierTemplate,
  updateDossierTemplate,
  CASE_TYPE_OPTIONS,
  type DossierTemplate,
} from '#/api/document/dossier';

const emit = defineEmits<{ success: [] }>();

// 响应式布局
const { isMobile, formLayout } = useResponsive();

const editingId = ref<number>();
const loading = ref(false);

const formData = reactive({
  name: '',
  caseType: 'CIVIL',
  description: '',
});

const [Modal, modalApi] = useVbenModal({
  footer: false,
  onOpenChange(isOpen) {
    if (!isOpen) {
      resetForm();
    }
  },
});

// 重置表单
function resetForm() {
  editingId.value = undefined;
  Object.assign(formData, {
    name: '',
    caseType: 'CIVIL',
    description: '',
  });
}

// 打开新增弹窗
function openCreate() {
  resetForm();
  modalApi.setState({ title: '新增卷宗模板' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: DossierTemplate) {
  editingId.value = record.id;
  Object.assign(formData, {
    name: record.name,
    caseType: record.caseType,
    description: record.description || '',
  });
  modalApi.setState({ title: '编辑卷宗模板' });
  modalApi.open();
}

// 保存
async function handleSave() {
  if (!formData.name?.trim()) {
    message.warning('请输入模板名称');
    return;
  }

  if (!formData.caseType) {
    message.warning('请选择案件类型');
    return;
  }

  try {
    loading.value = true;
    if (editingId.value) {
      await updateDossierTemplate(editingId.value, formData);
      message.success('更新成功');
    } else {
      await createDossierTemplate(formData);
      message.success('创建成功');
    }
    modalApi.close();
    emit('success');
  } catch (error: any) {
    message.error(error?.message || '操作失败');
  } finally {
    loading.value = false;
  }
}

defineExpose({
  openCreate,
  openEdit,
});
</script>

<template>
  <Modal 
    :loading="loading"
    :class="isMobile ? 'w-full' : 'w-[600px]'"
    :centered="isMobile"
  >
    <Form 
      :layout="formLayout"
      :label-col="isMobile ? undefined : { span: 6 }" 
      :wrapper-col="isMobile ? undefined : { span: 18 }"
    >
      <FormItem label="模板名称" required>
        <Input
          v-model:value="formData.name"
          placeholder="请输入模板名称，如：刑事案件卷宗模板"
        />
      </FormItem>

      <FormItem label="案件类型" required>
        <Select
          v-model:value="formData.caseType"
          :options="CASE_TYPE_OPTIONS"
          placeholder="请选择案件类型"
        />
      </FormItem>

      <FormItem label="描述">
        <Textarea
          v-model:value="formData.description"
          :rows="3"
          placeholder="请输入模板描述（可选）"
        />
      </FormItem>
    </Form>

    <div
      :style="{
        paddingTop: '16px',
        marginTop: '16px',
        textAlign: isMobile ? 'center' : 'right',
        borderTop: '1px solid #e8e8e8',
      }"
    >
      <Space :size="isMobile ? 8 : 8" :direction="isMobile ? 'vertical' : 'horizontal'" :style="{ width: isMobile ? '100%' : 'auto' }">
        <Button :block="isMobile" @click="modalApi.close()">取消</Button>
        <Button :block="isMobile" type="primary" :loading="loading" @click="handleSave">
          保存
        </Button>
      </Space>
    </div>
  </Modal>
</template>
