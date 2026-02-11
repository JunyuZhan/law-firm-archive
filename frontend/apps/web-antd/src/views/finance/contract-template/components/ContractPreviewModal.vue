<script setup lang="ts">
import { onMounted, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Button, message, Space, Tag } from 'ant-design-vue';

import { getConfigValue } from '#/api/system';
import { sanitizeHtml } from '#/utils/sanitize';

import { createContractSampleData } from '../constants/sample-data';
import {
  decodeHtmlEntities,
  formatPlainTextForPrint,
  formatStructuredForPrint,
  isStructuredContent,
} from '../utils/print-formatter';

interface ContractTemplateDTO {
  id: number;
  name: string;
  content: string;
}

const previewContent = ref('');
const previewTitle = ref('');

// 预览示例数据（动态加载律所信息）
const sampleData = ref<Record<string, string>>(createContractSampleData());

// 系统配置与变量名的映射关系
const firmConfigMapping: Record<string, string> = {
  'firm.name': 'firmName',
  'firm.address': 'firmAddress',
  'firm.phone': 'firmPhone',
  'firm.postcode': 'firmPostcode',
  'firm.legal.rep': 'firmLegalPerson',
  'firm.bank.name': 'firmBankName',
  'firm.bank.account': 'firmBankAccount',
  'firm.license': 'firmLicense',
  'firm.fax': 'firmFax',
  'firm.email': 'firmEmail',
  'firm.website': 'firmWebsite',
};

// 加载律所信息（从系统配置获取）
async function loadFirmInfo() {
  try {
    const configKeys = Object.keys(firmConfigMapping);
    const results = await Promise.all(
      configKeys.map((key) => getConfigValue(key).catch(() => null)),
    );

    // 将系统配置值映射到 sampleData
    configKeys.forEach((configKey, index) => {
      const variableName = firmConfigMapping[configKey];
      const configResult = results[index];
      if (variableName && configResult?.configValue) {
        sampleData.value[variableName] = configResult.configValue;
      }
      // 注意：如果系统未配置，保持空值，后续会显示 [变量名] 提示
    });
  } catch (error) {
    console.warn('加载律所信息失败', error);
  }
}

const [Modal, modalApi] = useVbenModal({
  footer: false,
});

// 打开预览
async function open(record: ContractTemplateDTO) {
  previewTitle.value = record.name;
  // 先解码 HTML 实体（处理可能被编码的内容）
  const rawContent = record.content || '';
  const content = decodeHtmlEntities(rawContent);

  // 每次打开都重新加载系统配置（确保获取最新值）
  await loadFirmInfo();

  // 创建用于替换的数据，空值显示 [变量名] 提示
  const displayData: Record<string, string> = {};
  Object.entries(sampleData.value).forEach(([key, value]) => {
    // 如果值为空，显示 [变量名] 提示管理员配置
    displayData[key] = value || `[${key}]`;
  });

  // 检查是否为结构化内容
  try {
    // 调试：记录内容类型和内容预览
    const isStructured = isStructuredContent(content);
    // 调试信息已移除

    // 如果识别为纯文本但看起来像 JSON，尝试强制解析
    if (
      !isStructured &&
      content.trim().startsWith('{') &&
      content.trim().endsWith('}')
    ) {
      try {
        const parsed = JSON.parse(content.trim());
        if (
          parsed &&
          typeof parsed === 'object' &&
          parsed._structured === true
        ) {
          // 检测到未被识别的结构化内容，强制使用结构化格式化
          // 强制使用结构化格式化
          let formatted = formatStructuredForPrint(content.trim(), displayData);

          if (!formatted || formatted.trim() === '') {
            console.error('强制格式化结构化内容失败，内容为空');
            previewContent.value =
              '<p style="color: red;">⚠️ 模板内容格式错误，无法预览。请检查模板内容是否正确。</p>';
            modalApi.setState({ title: `预览 - ${record.name}` });
            modalApi.open();
            return;
          }

          // 高亮变量值（包括未配置的提示）
          Object.entries(displayData).forEach(([_key, value]) => {
            if (value && value.startsWith('[') && value.endsWith(']')) {
              formatted = formatted.replaceAll(
                value,
                `<span class="preview-var-missing">${value}</span>`,
              );
            } else if (value) {
              const escapedValue = value.replaceAll(
                /[.*+?^${}()|[\]\\]/g,
                String.raw`\$&`,
              );
              formatted = formatted.replaceAll(
                new RegExp(escapedValue, 'g'),
                `<span class="preview-var">${value}</span>`,
              );
            }
          });
          previewContent.value = formatted;
          modalApi.setState({ title: `预览 - ${record.name}` });
          modalApi.open();
          return;
        }
      } catch (error) {
        console.warn('强制解析 JSON 失败:', error);
      }
    }

    if (isStructured) {
      // 使用新的格式化函数处理结构化内容
      let formatted = formatStructuredForPrint(content, displayData);

      // 如果格式化结果为空，说明解析失败
      if (!formatted || formatted.trim() === '') {
        console.error('格式化结构化内容失败，内容为空');
        previewContent.value =
          '<p style="color: red;">⚠️ 模板内容格式错误，无法预览。请检查模板内容是否正确。</p>';
        modalApi.setState({ title: `预览 - ${record.name}` });
        modalApi.open();
        return;
      }

      // 高亮变量值（包括未配置的提示）
      Object.entries(displayData).forEach(([_key, value]) => {
        if (value && value.startsWith('[') && value.endsWith(']')) {
          // 未配置的变量，用红色警告样式
          formatted = formatted.replaceAll(
            value,
            `<span class="preview-var-missing">${value}</span>`,
          );
        } else if (value) {
          // 转义特殊字符，避免在 replaceAll 中出错
          const escapedValue = value.replaceAll(
            /[.*+?^${}()|[\]\\]/g,
            String.raw`\$&`,
          );
          formatted = formatted.replaceAll(
            new RegExp(escapedValue, 'g'),
            `<span class="preview-var">${value}</span>`,
          );
        }
      });
      previewContent.value = formatted;
    } else {
      // 旧格式：使用智能格式化或直接显示
      let formatted = formatPlainTextForPrint(content, displayData);

      // 如果格式化结果看起来像JSON（可能是未被识别的结构化内容），给出提示
      if (formatted.trim().startsWith('{') && formatted.trim().endsWith('}')) {
        console.warn('检测到可能是JSON格式的内容，但未被识别为结构化内容');
        previewContent.value =
          '<p style="color: red;">⚠️ 模板内容格式错误：检测到JSON格式但无法解析。请检查模板内容是否正确保存。</p>';
        modalApi.setState({ title: `预览 - ${record.name}` });
        modalApi.open();
        return;
      }

      // 替换变量为示例值并高亮
      Object.entries(displayData).forEach(([key, value]) => {
        const isMissing = value && value.startsWith('[') && value.endsWith(']');
        const cssClass = isMissing ? 'preview-var-missing' : 'preview-var';
        // 转义特殊字符
        const escapedKey = key.replaceAll(
          /[.*+?^${}()|[\]\\]/g,
          String.raw`\$&`,
        );
        formatted = formatted.replaceAll(
          new RegExp(String.raw`\$\{${escapedKey}\}`, 'g'),
          `<span class="${cssClass}">${value}</span>`,
        );
        formatted = formatted.replaceAll(
          new RegExp(
            `<span[^>]*data-variable="${escapedKey}"[^>]*>[^<]*</span>`,
            'g',
          ),
          `<span class="${cssClass}">${value}</span>`,
        );
      });
      previewContent.value = formatted;
    }
  } catch (error) {
    console.error('预览格式化失败:', error);
    previewContent.value = `<p style="color: red;">⚠️ 预览失败：${error instanceof Error ? error.message : '未知错误'}</p>`;
  }

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
        @page { margin: 2.5cm; size: A4; }
        body { 
          font-family: "SimSun", "宋体", serif; 
          font-size: 12pt; /* 小四号 */
          line-height: 1.8;
          color: #000;
        }
        * {
          font-family: "SimSun", "宋体", serif;
        }
        .preview-var { color: #000; font-weight: 500; }
        .preview-var-missing { color: #f00; font-weight: 500; }
        h1, h2, h3 { text-align: center; }
        h2 { font-size: 16pt; font-weight: bold; } /* 三号偏小 */
        h3 { font-size: 12pt; font-weight: bold; text-align: left; margin: 1.2em 0 0.5em; }
        p { margin: 0.4em 0; text-align: justify; }
        ul, ol { padding-left: 2em; margin: 0.4em 0; }
        li { margin: 0.2em 0; }
        strong, b { font-weight: bold; }
        em, i { font-style: italic; }
        u { text-decoration: underline; }
        table { width: 100%; border-collapse: collapse; margin: 0.8em 0; }
        td, th { border: 1px solid #000; padding: 6px 8px; }
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

defineExpose({ open });
</script>

<template>
  <Modal title="合同预览" class="w-[900px]">
    <div class="preview-toolbar">
      <Space>
        <Button type="primary" @click="handlePrint">打印 / 导出PDF</Button>
      </Space>
    </div>

    <div class="preview-container">
      <div class="preview-paper">
        <!-- 使用 sanitizeHtml 防止 XSS 攻击 -->
        <div
          v-html="sanitizeHtml(previewContent)"
          class="preview-content"
        ></div>
      </div>
    </div>

    <div class="preview-footer">
      <Space>
        <span><Tag color="blue">蓝色文字</Tag> 已替换的变量值</span>
        <span
          ><Tag color="red">红色文字</Tag> 系统未配置，请前往「系统管理 →
          基础配置」设置</span
        >
      </Space>
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
  font-size: 12pt; /* 小四号 */
  line-height: 1.8;
  color: #000;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  margin: 0.8em 0;
  text-align: center;
}

.preview-content :deep(h2) {
  font-size: 16pt;
  font-weight: bold;
}

.preview-content :deep(h3) {
  margin: 1.2em 0 0.5em;
  font-size: 12pt;
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

.preview-content :deep(.preview-var-missing) {
  padding: 0 4px;
  font-weight: 500;
  color: #ff4d4f;
  background: #fff2f0;
  border-radius: 2px;
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
