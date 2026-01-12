<script setup lang="ts">
import { ref, computed } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { DownloadOutlined, InboxOutlined } from '@vben/icons';
import {
  Alert,
  Button,
  message,
  Progress,
  Upload,
  type UploadFile,
} from 'ant-design-vue';

import { downloadUserImportTemplate, importUsers } from '#/api/system';

const emit = defineEmits<{
  success: [];
}>();

// 状态
const fileList = ref<UploadFile[]>([]);
const uploading = ref(false);
const uploadProgress = ref(0);
const importResult = ref<{
  errorMessages: string[];
  failCount: number;
  successCount: number;
  total: number;
} | null>(null);

// 是否有导入结果
const hasResult = computed(() => importResult.value !== null);

// 重置状态
function resetState() {
  fileList.value = [];
  uploading.value = false;
  uploadProgress.value = 0;
  importResult.value = null;
}

// 弹窗配置
const [Modal, modalApi] = useVbenModal({
  title: '批量导入用户',
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      resetState();
    }
  },
  footer: false,
});

// 打开弹窗
function open() {
  modalApi.open();
}

// 下载模板
async function handleDownloadTemplate() {
  try {
    const blob = await downloadUserImportTemplate();
    const url = window.URL.createObjectURL(blob as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = '用户导入模板.xlsx';
    link.click();
    window.URL.revokeObjectURL(url);
    message.success('模板下载成功');
  } catch (error: any) {
    message.error(error.message || '模板下载失败');
  }
}

// 上传前检查
function beforeUpload(file: File) {
  const isExcel =
    file.type ===
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'application/vnd.ms-excel' ||
    file.name.endsWith('.xlsx') ||
    file.name.endsWith('.xls');

  if (!isExcel) {
    message.error('只能上传 Excel 文件（.xlsx 或 .xls）');
    return false;
  }

  const isLt10M = file.size / 1024 / 1024 < 10;
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB');
    return false;
  }

  return false; // 阻止自动上传
}

// 处理文件变化
function handleChange(info: { fileList: UploadFile[] }) {
  fileList.value = info.fileList.slice(-1); // 只保留最后一个文件
  importResult.value = null; // 清除之前的导入结果
}

// 开始导入
async function handleImport() {
  if (fileList.value.length === 0) {
    message.warning('请先选择要导入的文件');
    return;
  }

  const file = fileList.value[0]?.originFileObj;
  if (!file) {
    message.error('文件读取失败，请重新选择');
    return;
  }

  uploading.value = true;
  uploadProgress.value = 0;
  importResult.value = null;

  try {
    const result = await importUsers(file, (progress) => {
      uploadProgress.value = progress.percent;
    });

    importResult.value = result;

    if (result.failCount === 0) {
      message.success(`导入成功！共导入 ${result.successCount} 个用户`);
      emit('success');
    } else if (result.successCount > 0) {
      message.warning(
        `部分导入成功：成功 ${result.successCount} 个，失败 ${result.failCount} 个`,
      );
      emit('success');
    } else {
      message.error(`导入失败：共 ${result.failCount} 条数据导入失败`);
    }
  } catch (error: any) {
    message.error(error.message || '导入失败');
  } finally {
    uploading.value = false;
  }
}

// 关闭弹窗
function handleClose() {
  modalApi.close();
}

defineExpose({ open });
</script>

<template>
  <Modal class="w-[600px]">
    <div class="space-y-4">
      <!-- 操作说明 -->
      <Alert
        message="导入说明"
        type="info"
        show-icon
      >
        <template #description>
          <div class="text-sm">
            <p>1. 请先下载导入模板，按模板格式填写用户信息</p>
            <p>2. 必填字段：用户名、姓名；密码为空时默认为 LawFirm@2026</p>
            <p>3. 用户名不能重复，重复的用户名将导入失败</p>
            <p>4. 支持 .xlsx 和 .xls 格式，文件大小不超过 10MB</p>
          </div>
        </template>
      </Alert>

      <!-- 下载模板按钮 -->
      <div class="flex justify-center">
        <Button type="link" @click="handleDownloadTemplate">
          <template #icon><DownloadOutlined /></template>
          下载导入模板
        </Button>
      </div>

      <!-- 文件上传区域 -->
      <Upload.Dragger
        v-model:file-list="fileList"
        :before-upload="beforeUpload"
        :disabled="uploading"
        accept=".xlsx,.xls"
        :max-count="1"
        @change="handleChange"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
        <p class="ant-upload-hint">支持 .xlsx 和 .xls 格式的 Excel 文件</p>
      </Upload.Dragger>

      <!-- 上传进度 -->
      <div v-if="uploading" class="mt-4">
        <Progress :percent="uploadProgress" status="active" />
        <p class="text-center text-gray-500 mt-2">正在导入，请稍候...</p>
      </div>

      <!-- 导入结果 -->
      <div v-if="hasResult && importResult" class="mt-4">
        <Alert
          :type="importResult.failCount === 0 ? 'success' : 'warning'"
          show-icon
        >
          <template #message>
            <span>
              导入完成：总计 {{ importResult.total }} 条，成功
              {{ importResult.successCount }} 条，失败
              {{ importResult.failCount }} 条
            </span>
          </template>
        </Alert>

        <!-- 错误详情 -->
        <div
          v-if="importResult.errorMessages && importResult.errorMessages.length > 0"
          class="mt-3 max-h-[200px] overflow-auto bg-red-50 rounded p-3"
        >
          <p class="font-medium text-red-600 mb-2">错误详情：</p>
          <ul class="list-disc list-inside text-sm text-red-500 space-y-1">
            <li v-for="(err, idx) in importResult.errorMessages" :key="idx">
              {{ err }}
            </li>
          </ul>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="flex justify-end gap-3 mt-6 pt-4 border-t">
        <Button @click="handleClose">关闭</Button>
        <Button
          type="primary"
          :loading="uploading"
          :disabled="fileList.length === 0"
          @click="handleImport"
        >
          {{ uploading ? '导入中...' : '开始导入' }}
        </Button>
      </div>
    </div>
  </Modal>
</template>
