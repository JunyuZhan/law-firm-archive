<script setup lang="ts">
import type { LetterTemplateDTO } from '#/api/admin';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, message, Space, Tag } from 'ant-design-vue';

import { getConfigValue } from '#/api/system';

import { createLetterSampleData } from '../constants/sample-data';

const previewContent = ref('');
const previewTitle = ref('');

// 预览示例数据（动态加载律所信息）
const sampleData = ref<Record<string, string>>(createLetterSampleData());

// 加载律所信息（从系统配置获取）
async function loadFirmInfo() {
  try {
    const [
      firmNameConfig,
      firmAddressConfig,
      firmPhoneConfig,
      firmLicenseConfig,
    ] = await Promise.all([
      getConfigValue('firm.name').catch(() => null),
      getConfigValue('firm.address').catch(() => null),
      getConfigValue('firm.phone').catch(() => null),
      getConfigValue('firm.license').catch(() => null),
    ]);

    if (firmNameConfig?.configValue) {
      sampleData.value.firmName = firmNameConfig.configValue;
    }
    if (firmAddressConfig?.configValue) {
      sampleData.value.firmAddress = firmAddressConfig.configValue;
    }
    if (firmPhoneConfig?.configValue) {
      sampleData.value.firmPhone = firmPhoneConfig.configValue;
    }
    if (firmLicenseConfig?.configValue) {
      sampleData.value.firmLicense = firmLicenseConfig.configValue;
    }
  } catch (error) {
    console.warn('加载律所信息失败，使用默认值', error);
  }
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
});

// 打开预览
async function open(record: LetterTemplateDTO) {
  previewTitle.value = record.name;
  let content = record.content || '';

  // 确保律所信息已加载
  if (!sampleData.value.firmAddress && !sampleData.value.firmPhone) {
    await loadFirmInfo();
  }

  // 替换变量为示例值
  Object.entries(sampleData.value).forEach(([key, value]) => {
    const displayValue = value || `[${key}]`;
    content = content.replaceAll(
      new RegExp(String.raw`\$\{${key}\}`, 'g'),
      `<span class="preview-var">${displayValue}</span>`,
    );
    content = content.replaceAll(
      new RegExp(`<span[^>]*data-variable="${key}"[^>]*>[^<]*</span>`, 'g'),
      `<span class="preview-var">${displayValue}</span>`,
    );
  });

  previewContent.value = content;
  modalApi.setState({ title: `预览 - ${record.name}` });
  modalApi.open();
}

// 组件挂载时加载律所信息
onMounted(() => {
  loadFirmInfo();
});

// 打印预览
function handlePrint() {
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器弹窗设置');
    return;
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${previewTitle.value}</title>
      <style>
        @page { 
          size: A4;
          margin-top: 3.7cm;
          margin-bottom: 3.5cm;
          margin-left: 2.8cm;
          margin-right: 2.6cm;
        }
        body { 
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
          font-size: 16pt; 
          line-height: 28pt;
          color: #000;
          padding: 0;
          margin: 0;
        }
        * {
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
        }
        .preview-var { color: #000; font-weight: 500; }
        h1, h2, h3 { 
          text-align: center; 
          font-family: "FZXiaoBiaoSong-B05S", "方正小标宋", "FZXBS", serif;
          font-size: 22pt;
          font-weight: normal;
          letter-spacing: 2pt;
          margin: 20pt 0 10pt;
        }
        p { 
          text-indent: 2em; 
          margin: 0; 
          padding: 0;
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
          font-size: 16pt;
          line-height: 28pt;
        }
        .signature { 
          text-align: right; 
          margin-top: 40pt; 
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
          font-size: 16pt;
          line-height: 28pt;
        }
      </style>
    </head>
    <body>
      ${previewContent.value}
    </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.print();
}

defineExpose({ open });
</script>

<template>
  <Modal class="w-[800px]">
    <div class="preview-toolbar">
      <Space>
        <Button type="primary" @click="handlePrint">打印 / 导出PDF</Button>
      </Space>
    </div>

    <div class="preview-container">
      <div class="preview-paper">
        <div v-html="previewContent" class="preview-content"></div>
      </div>
    </div>

    <div class="preview-footer">
      <Tag color="blue">蓝色文字</Tag>
      表示已替换的变量值（实际函件中为正常黑色）
    </div>
  </Modal>
</template>

<style scoped>
.preview-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-container {
  max-height: 55vh;
  padding: 24px;
  overflow-y: auto;
  background: #e8e8e8;
  border-radius: 4px;
}

.preview-paper {
  max-width: 700px;
  min-height: 700px;
  padding: 50px 70px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 2px 12px rgb(0 0 0 / 15%);
}

.preview-content {
  font-family: SimSun, '宋体', serif;
  font-size: 14px;
  line-height: 2;
  color: #000;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  margin: 0.8em 0;
  text-align: center;
}

.preview-content :deep(h2) {
  font-size: 20px;
  font-weight: bold;
  letter-spacing: 0.5em;
}

.preview-content :deep(p) {
  margin: 0.5em 0;
  text-indent: 2em;
}

.preview-content :deep(.preview-var) {
  font-weight: 500;
  color: #1890ff;
}

.preview-content :deep(hr) {
  margin: 2em 0;
  border: none;
  border-top: 1px dashed #ccc;
}

.preview-footer {
  padding: 12px 0;
  margin-top: 16px;
  font-size: 12px;
  color: #666;
  border-top: 1px solid #e8e8e8;
}
</style>
