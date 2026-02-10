<script setup lang="ts">
import type { LetterTemplateDTO } from '#/api/admin';

import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { OFFICIAL_DOCUMENT_STYLES } from '@vben/utils';

import { Button, message, Space, Tag } from 'ant-design-vue';

import { sanitizeHtml } from '#/utils/sanitize';

import { getProfileInfo } from '#/api/core/profile';
import { getConfigValue } from '#/api/system';

import { decodeHtmlEntities } from '../../../finance/contract-template/utils/print-formatter';
import { createLetterSampleData } from '../constants/sample-data';
import {
  formatHtmlLetterForPreview,
  formatPlainTextLetterForPreview,
  formatStructuredLetterForPreview,
  isHtmlContent,
  isStructuredLetterContent,
} from '../utils/letter-formatter';

const previewContent = ref('');
const previewTitle = ref('');

// 预览示例数据（动态加载律所信息）
const sampleData = ref<Record<string, string>>(createLetterSampleData());

// 加载律所信息（从系统配置获取）和当前用户信息（作为律师示例数据）
async function loadFirmInfo() {
  try {
    // 并行加载系统配置和当前用户信息
    const [
      firmNameConfig,
      firmAddressConfig,
      firmPhoneConfig,
      firmLicenseConfig,
      firmEmailConfig,
      firmLegalRepConfig,
      firmFaxConfig,
      firmWebsiteConfig,
      firmPostcodeConfig,
      currentUser,
    ] = await Promise.all([
      getConfigValue('firm.name').catch(() => null),
      getConfigValue('firm.address').catch(() => null),
      getConfigValue('firm.phone').catch(() => null),
      getConfigValue('firm.license').catch(() => null),
      getConfigValue('firm.email').catch(() => null),
      getConfigValue('firm.legal.rep').catch(() => null),
      getConfigValue('firm.fax').catch(() => null),
      getConfigValue('firm.website').catch(() => null),
      getConfigValue('firm.postcode').catch(() => null),
      getProfileInfo().catch(() => null), // 获取当前登录用户信息
    ]);

    // 律所信息（从系统配置获取）
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
    if (firmEmailConfig?.configValue) {
      sampleData.value.firmEmail = firmEmailConfig.configValue;
    }
    if (firmLegalRepConfig?.configValue) {
      sampleData.value.firmLegalPerson = firmLegalRepConfig.configValue;
    }
    if (firmFaxConfig?.configValue) {
      sampleData.value.firmFax = firmFaxConfig.configValue;
    }
    if (firmWebsiteConfig?.configValue) {
      sampleData.value.firmWebsite = firmWebsiteConfig.configValue;
    }
    if (firmPostcodeConfig?.configValue) {
      sampleData.value.firmPostcode = firmPostcodeConfig.configValue;
    }

    // 律师信息（从当前登录用户获取，作为示例数据）
    if (currentUser) {
      if (currentUser.realName) {
        sampleData.value.lawyerNames = `${currentUser.realName}律师`;
      }
      if (currentUser.lawyerLicenseNo) {
        sampleData.value.lawyerLicenseNo = currentUser.lawyerLicenseNo;
      }
      if (currentUser.phone) {
        // 可选：如果当前用户有电话，也可以作为律师联系电话的示例
        // sampleData.value.lawyerPhone = currentUser.phone;
      }
    }
  } catch (error) {
    console.warn('加载律所信息失败，使用默认值', error);
  }
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
});

// 打印预览 - 使用共享的公文格式样式
function handlePrint() {
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器弹窗设置');
    return;
  }

  // 使用共享的公文格式样式，添加预览变量高亮样式
  const customStyles = `
    .preview-var { color: #000; font-weight: 500; }
  `;

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${previewTitle.value}</title>
      <style>
        ${OFFICIAL_DOCUMENT_STYLES}
        ${customStyles}
      </style>
    </head>
    <body>
      ${sanitizeHtml(previewContent.value)}
    </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.print();
}

// 打开预览弹窗
async function open(record: LetterTemplateDTO) {
  previewTitle.value = record.name;
  // 解码可能被 HTML 编码的内容
  const content = decodeHtmlEntities(record.content || '');

  // 确保律所信息已加载
  if (!sampleData.value.firmAddress && !sampleData.value.firmPhone) {
    await loadFirmInfo();
  }

  // 检查内容格式并使用对应的预览函数
  if (isStructuredLetterContent(content)) {
    // 结构化格式（JSON）：使用预览格式化函数
    previewContent.value = formatStructuredLetterForPreview(
      content,
      sampleData.value,
    );
  } else if (isHtmlContent(content)) {
    // HTML 格式：保持 HTML 结构，只替换变量
    previewContent.value = formatHtmlLetterForPreview(
      content,
      sampleData.value,
    );
  } else {
    // 纯文本格式：使用公文格式化函数
    previewContent.value = formatPlainTextLetterForPreview(
      content,
      sampleData.value,
    );
  }

  modalApi.setState({ title: `预览 - ${record.name}` });
  modalApi.open();
}

// 组件挂载时加载律所信息
onMounted(() => {
  loadFirmInfo();
});

defineExpose({ open });
</script>

<template>
  <Modal title="函件预览" class="w-[800px]">
    <div class="preview-toolbar">
      <Space>
        <Button type="primary" @click="handlePrint">打印 / 导出PDF</Button>
      </Space>
    </div>

    <div class="preview-container">
      <div class="preview-paper">
        <!-- 使用 sanitizeHtml 防止 XSS 攻击 -->
        <div v-html="sanitizeHtml(previewContent)" class="preview-content"></div>
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
  max-width: 650px;
  min-height: 600px;
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
