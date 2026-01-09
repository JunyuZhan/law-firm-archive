<script setup lang="ts">
/**
 * 证据表单组件（新增/编辑）
 * 支持直接上传文件或从卷宗管理中选择文件
 */
import { ref, watch, computed } from 'vue';
import { Modal, Form, FormItem, Input, Select, Textarea, InputNumber, message, Radio, Space, Tag, Button, Upload, Tooltip, Spin, Alert } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import EvidenceUploader from './EvidenceUploader.vue';
import DossierFileSelector from './DossierFileSelector.vue';
import type { UploadResult } from './EvidenceUploader.vue';
import type { EvidenceItem } from './types';
import { EVIDENCE_TYPE_OPTIONS } from './types';
import { createEvidence, updateEvidence, type CreateEvidenceCommand, type UpdateEvidenceCommand } from '#/api/evidence';
import { recognizeGeneral, type OcrResultDTO } from '#/api/ocr';
import type { DocumentDTO } from '#/api/document';

const props = defineProps<{
  open: boolean;
  evidence?: EvidenceItem | null;
  matterId: number;
  groups?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref();
const loading = ref(false);
const ocrLoading = ref(false);
const ocrResult = ref('');

const isEdit = computed(() => !!props.evidence?.id);
const title = computed(() => isEdit.value ? '编辑证据' : '添加证据');

interface FormData {
  name: string;
  evidenceType: string;
  source: string;
  groupName: string;
  provePurpose: string;
  description: string;
  isOriginal: 'original' | 'copy';
  originalCount: number;
  copyCount: number;
  pageStart?: number;
  pageEnd?: number;
  fileInfo: UploadResult | null;
  // 卷宗引用相关
  fileSource: 'upload' | 'dossier';
  documentId?: number;
  documentInfo?: { title?: string; fileName?: string; fileSize?: number; fileType?: string } | null;
}

const formData = ref<FormData>({
  name: '',
  evidenceType: 'DOCUMENTARY',
  source: '',
  groupName: '',
  provePurpose: '',
  description: '',
  isOriginal: 'original',
  originalCount: 1,
  copyCount: 0,
  pageStart: undefined,
  pageEnd: undefined,
  fileInfo: null,
  fileSource: 'dossier',  // 默认从卷宗选择
  documentId: undefined,
  documentInfo: null,
});

// 卷宗文件选择器
const dossierSelectorVisible = ref(false);

const rules = {
  name: [{ required: true, message: '请输入证据名称' }],
  evidenceType: [{ required: true, message: '请选择证据类型' }],
};

// 监听 evidence 变化，填充表单
watch(() => props.evidence, (val) => {
  if (val) {
    formData.value = {
      name: val.name || '',
      evidenceType: val.evidenceType || 'DOCUMENTARY',
      source: val.source || '',
      groupName: val.groupName || '',
      provePurpose: val.provePurpose || '',
      description: val.description || '',
      isOriginal: val.isOriginal === false ? 'copy' : 'original',
      originalCount: val.originalCount ?? 1,
      copyCount: val.copyCount ?? 0,
      pageStart: val.pageStart,
      pageEnd: val.pageEnd,
      fileInfo: val.fileUrl ? {
        fileUrl: val.fileUrl,
        fileName: val.fileName || '',
        fileSize: val.fileSize || 0,
        fileType: val.fileType || 'other',
        thumbnailUrl: val.thumbnailUrl || null,
      } : null,
    };
  } else {
    resetForm();
  }
}, { immediate: true });

function resetForm() {
  formData.value = {
    name: '',
    evidenceType: 'DOCUMENTARY',
    source: '',
    groupName: '',
    provePurpose: '',
    description: '',
    isOriginal: 'original',
    originalCount: 1,
    copyCount: 0,
    pageStart: undefined,
    pageEnd: undefined,
    fileInfo: null,
    fileSource: 'dossier',
    documentId: undefined,
    documentInfo: null,
  };
}

// 从卷宗选择文件
function handleDossierSelect(doc: DocumentDTO) {
  formData.value.documentId = doc.id;
  formData.value.documentInfo = {
    title: doc.title || doc.name,
    fileName: doc.fileName,
    fileSize: doc.fileSize,
    fileType: doc.fileType,
  };
  // 如果名称为空，自动填充文件名
  if (!formData.value.name && (doc.title || doc.fileName)) {
    const name = doc.title || doc.fileName || '';
    formData.value.name = name.replace(/\.[^/.]+$/, '');
  }
}

// 清除卷宗文件选择
function clearDossierFile() {
  formData.value.documentId = undefined;
  formData.value.documentInfo = null;
}

// 格式化文件大小
function formatFileSize(bytes?: number): string {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function handleUploadSuccess(result: UploadResult) {
  // 如果名称为空，自动填充文件名
  if (!formData.value.name && result.fileName) {
    const nameWithoutExt = result.fileName.replace(/\.[^/.]+$/, '');
    formData.value.name = nameWithoutExt;
  }
}

// OCR识别证据图片内容
async function handleOcrEvidence(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      ocrResult.value = result.rawText;
      // 如果描述为空，自动填充OCR结果的前300字
      if (!formData.value.description) {
        formData.value.description = result.rawText.substring(0, 300);
      }
      // 如果证明目的为空，尝试提取
      if (!formData.value.provePurpose && result.rawText.length > 50) {
        formData.value.provePurpose = result.rawText.substring(0, 100);
      }
      message.success('OCR识别成功，已自动填充描述');
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

async function handleSubmit() {
  try {
    await formRef.value?.validate();
    loading.value = true;

    const data: CreateEvidenceCommand | UpdateEvidenceCommand = {
      name: formData.value.name,
      evidenceType: formData.value.evidenceType,
      source: formData.value.source || undefined,
      groupName: formData.value.groupName || undefined,
      provePurpose: formData.value.provePurpose || undefined,
      description: formData.value.description || undefined,
      isOriginal: formData.value.isOriginal === 'original',
      originalCount: formData.value.originalCount,
      copyCount: formData.value.copyCount,
      pageStart: formData.value.pageStart,
      pageEnd: formData.value.pageEnd,
    };

    if (isEdit.value && props.evidence?.id) {
      await updateEvidence(props.evidence.id, data as UpdateEvidenceCommand);
      message.success('更新成功');
    } else {
      const createData: CreateEvidenceCommand = {
        ...data,
        matterId: props.matterId,
      };

      // 根据文件来源设置不同的字段
      if (formData.value.fileSource === 'dossier' && formData.value.documentId) {
        // 从卷宗引用
        createData.documentId = formData.value.documentId;
      } else if (formData.value.fileSource === 'upload' && formData.value.fileInfo) {
        // 直接上传
        createData.fileUrl = formData.value.fileInfo.fileUrl;
        createData.fileName = formData.value.fileInfo.fileName;
        createData.fileSize = formData.value.fileInfo.fileSize;
        createData.fileType = formData.value.fileInfo.fileType;
        createData.thumbnailUrl = formData.value.fileInfo.thumbnailUrl || undefined;
      }

      await createEvidence(createData);
      message.success('创建成功');
    }

    emit('update:open', false);
    emit('success');
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '操作失败');
  } finally {
    loading.value = false;
  }
}

function handleCancel() {
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :open="open"
    :title="title"
    width="600px"
    :confirm-loading="loading"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <Form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
    >
      <FormItem label="证据名称" name="name">
        <Input v-model:value="formData.name" placeholder="请输入证据名称" />
      </FormItem>

      <FormItem label="证据类型" name="evidenceType">
        <Select v-model:value="formData.evidenceType" :options="EVIDENCE_TYPE_OPTIONS" />
      </FormItem>

      <FormItem label="分组" name="groupName">
        <Select
          v-model:value="formData.groupName"
          placeholder="选择或输入分组名称"
          allow-clear
          :options="(groups || []).map(g => ({ label: g, value: g }))"
          mode="combobox"
        />
      </FormItem>

      <FormItem label="证据来源" name="source">
        <Input v-model:value="formData.source" placeholder="请输入证据来源" />
      </FormItem>

      <FormItem label="证明目的" name="provePurpose">
        <Textarea v-model:value="formData.provePurpose" :rows="2" placeholder="请输入证明目的" />
      </FormItem>

      <FormItem label="原件/复印件">
        <Select v-model:value="formData.isOriginal" style="width: 120px">
          <Select.Option value="original">原件</Select.Option>
          <Select.Option value="copy">复印件</Select.Option>
        </Select>
      </FormItem>

      <FormItem label="份数">
        <div style="display: flex; gap: 16px">
          <div>
            <span style="margin-right: 8px">原件</span>
            <InputNumber v-model:value="formData.originalCount" :min="0" :max="99" style="width: 80px" />
          </div>
          <div>
            <span style="margin-right: 8px">复印件</span>
            <InputNumber v-model:value="formData.copyCount" :min="0" :max="99" style="width: 80px" />
          </div>
        </div>
      </FormItem>

      <FormItem label="页码范围">
        <div style="display: flex; align-items: center; gap: 8px">
          <InputNumber v-model:value="formData.pageStart" :min="1" placeholder="起始" style="width: 100px" />
          <span>-</span>
          <InputNumber v-model:value="formData.pageEnd" :min="formData.pageStart || 1" placeholder="结束" style="width: 100px" />
        </div>
      </FormItem>

      <FormItem v-if="!isEdit" label="文件来源">
        <Radio.Group v-model:value="formData.fileSource">
          <Radio value="dossier">从卷宗选择</Radio>
          <Radio value="upload">直接上传</Radio>
        </Radio.Group>
      </FormItem>

      <!-- 从卷宗选择 -->
      <FormItem v-if="!isEdit && formData.fileSource === 'dossier'" label="选择文件">
        <div v-if="formData.documentInfo" style="display: flex; align-items: center; gap: 8px;">
          <Tag color="blue">{{ formData.documentInfo.title || formData.documentInfo.fileName }}</Tag>
          <span v-if="formData.documentInfo.fileSize" style="color: #999; font-size: 12px;">
            {{ formatFileSize(formData.documentInfo.fileSize) }}
          </span>
          <Button type="link" size="small" @click="clearDossierFile">移除</Button>
        </div>
        <Button v-else type="dashed" @click="dossierSelectorVisible = true">
          从卷宗选择文件
        </Button>
      </FormItem>

      <!-- 直接上传 -->
      <FormItem v-if="!isEdit && formData.fileSource === 'upload'" label="上传文件">
        <EvidenceUploader v-model="formData.fileInfo" @success="handleUploadSuccess" />
      </FormItem>

      <!-- OCR识别区域 -->
      <FormItem label="智能识别">
        <Space>
          <Upload
            :show-upload-list="false"
            :before-upload="handleOcrEvidence"
            accept="image/*"
            :disabled="ocrLoading"
          >
            <Tooltip title="上传证据图片，自动识别内容填充到描述中">
              <Button size="small" :loading="ocrLoading">
                <IconifyIcon icon="ant-design:scan-outlined" />
                OCR识别图片内容
              </Button>
            </Tooltip>
          </Upload>
          <span style="color: #999; font-size: 12px;">上传图片自动提取文字到描述</span>
        </Space>
      </FormItem>

      <FormItem label="描述" name="description">
        <Textarea v-model:value="formData.description" :rows="3" placeholder="请输入描述信息（可通过OCR自动提取）" />
      </FormItem>
    </Form>

    <!-- 卷宗文件选择器 -->
    <DossierFileSelector
      v-model:open="dossierSelectorVisible"
      :matter-id="matterId"
      @select="handleDossierSelect"
    />
  </Modal>
</template>
