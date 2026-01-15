<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, message, Space, Tag } from 'ant-design-vue';

import { getConfigValue } from '#/api/system';

import { createContractSampleData } from '../constants/sample-data';

interface ContractTemplateDTO {
  id: number;
  name: string;
  content: string;
}

const previewContent = ref('');
const previewTitle = ref('');

// 预览示例数据（动态加载律所信息）
const sampleData = ref<Record<string, string>>(createContractSampleData());

// 加载律所信息（从系统配置获取）
async function loadFirmInfo() {
  try {
    const [
      firmNameConfig,
      firmAddressConfig,
      firmPhoneConfig,
      firmPostcodeConfig,
      firmLegalRepConfig,
    ] = await Promise.all([
      getConfigValue('firm.name').catch(() => null),
      getConfigValue('firm.address').catch(() => null),
      getConfigValue('firm.phone').catch(() => null),
      getConfigValue('firm.postcode').catch(() => null),
      getConfigValue('firm.legal.rep').catch(() => null),
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
    if (firmPostcodeConfig?.configValue) {
      sampleData.value.firmPostcode = firmPostcodeConfig.configValue;
    }
    if (firmLegalRepConfig?.configValue) {
      sampleData.value.firmLegalPerson = firmLegalRepConfig.configValue;
    }
  } catch (error) {
    console.warn('加载律所信息失败，使用默认值', error);
  }
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
});

// 打开预览
async function open(record: ContractTemplateDTO) {
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

  // 重要：不要为所有 p 标签添加默认的 text-indent
  // wangeditor 编辑器使用内联样式保存格式，需要保留这些样式
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${previewTitle.value}</title>
      <style>
        @page { margin: 2cm; size: A4; }
        body { 
          font-family: "SimSun", "宋体", serif; 
          font-size: 14pt; 
          line-height: 2;
          color: #000;
        }
        * {
          font-family: "SimSun", "宋体", serif;
        }
        .preview-var { color: #000; font-weight: 500; }
        h1, h2, h3 { text-align: center; }
        h2 { font-size: 18pt; }
        h3 { font-size: 14pt; text-align: left; margin: 1.5em 0 0.5em; }
        /* 不设置默认 text-indent，保留编辑器的内联样式设置 */
        p { margin: 0.5em 0; }
        /* 支持 wangeditor 的对齐和缩进格式 - 内联样式会覆盖默认样式 */
        ul, ol { padding-left: 2em; margin: 0.5em 0; }
        li { margin: 0.3em 0; }
        strong, b { font-weight: bold; }
        em, i { font-style: italic; }
        u { text-decoration: underline; }
        table { width: 100%; border-collapse: collapse; margin: 1em 0; }
        td, th { border: 1px solid #000; padding: 8px; }
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
  <Modal class="w-[900px]">
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
      表示已替换的变量值（实际合同中为正常黑色）
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
  max-height: 60vh;
  padding: 24px;
  overflow-y: auto;
  background: #e8e8e8;
  border-radius: 4px;
}

.preview-paper {
  max-width: 800px;
  min-height: 800px;
  padding: 60px 80px;
  margin: 0 auto;
  background: #fff;
  box-shadow: 0 2px 12px rgb(0 0 0 / 15%);
}

.preview-content {
  font-family: SimSun, '宋体', serif;
  font-size: 14pt;
  line-height: 2;
  color: #000;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  margin: 1em 0;
  text-align: center;
}

.preview-content :deep(h2) {
  font-size: 22px;
  font-weight: bold;
}

.preview-content :deep(h3) {
  margin: 1.5em 0 0.5em;
  font-size: 16px;
  font-weight: bold;
  text-align: left;
}

.preview-content :deep(p) {
  margin: 0.5em 0;
  /* 不设置默认 text-indent，保留编辑器的内联样式设置 */
}

.preview-content :deep(.preview-var) {
  font-weight: 500;
  color: #1890ff;
}

.preview-content :deep(table) {
  width: 100%;
  margin: 1em 0;
  border-collapse: collapse;
}

.preview-content :deep(td),
.preview-content :deep(th) {
  padding: 8px 12px;
  border: 1px solid #000;
}

.preview-footer {
  padding: 12px 0;
  margin-top: 16px;
  font-size: 12px;
  color: #666;
  border-top: 1px solid #e8e8e8;
}
</style>
