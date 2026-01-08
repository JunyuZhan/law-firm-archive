<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Button, Space, Tag } from 'ant-design-vue';
import { message } from 'ant-design-vue';
import type { LetterTemplateDTO } from '#/api/admin';

const previewContent = ref('');
const previewTitle = ref('');

// 预览示例数据
const sampleData: Record<string, string> = {
  matterName: '张三与李四民间借贷纠纷案',
  causeOfAction: '民间借贷纠纷',
  lawyerNames: '王律师、刘律师',
  lawyerLicenseNo: '15200201912345678',
  clientName: '张三',
  clientIdNumber: '520000199001011234',
  opposingParty: '李四',
  targetUnit: '贵阳市中级人民法院',
  targetAddress: '贵州省贵阳市云岩区XX路XX号',
  firmName: '贵州威迪律师事务所',
  firmAddress: '贵州省毕节市七星关区开行路联邦金座10楼',
  firmPhone: '0857-8228444',
  letterNo: 'WD-HJ-2026-001',
  date: '2026年1月5日',
  currentYear: '2026',
};

const [Modal, modalApi] = useVbenModal({
  footer: false,
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
        @page { margin: 2cm; size: A4; }
        body { 
          font-family: "SimSun", "宋体", serif; 
          font-size: 14pt; 
          line-height: 2;
          color: #000;
        }
        .preview-var { color: #000; font-weight: 500; }
        h1, h2, h3 { text-align: center; }
        p { text-indent: 2em; margin: 0.5em 0; }
        .signature { text-align: right; margin-top: 2em; }
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

// 打开预览弹窗
function open(record: LetterTemplateDTO) {
  previewTitle.value = record.name;
  let content = record.content || '';
  
  // 替换变量为示例值
  Object.entries(sampleData).forEach(([key, value]) => {
    content = content.replace(
      new RegExp(`\\$\\{${key}\\}`, 'g'), 
      `<span class="preview-var">${value}</span>`
    );
    // 替换带data-variable属性的标签内容
    content = content.replace(
      new RegExp(`<span[^>]*data-variable="${key}"[^>]*>[^<]*</span>`, 'g'),
      `<span class="preview-var">${value}</span>`
    );
  });
  
  previewContent.value = content;
  modalApi.setState({ title: `预览 - ${record.name}` });
  modalApi.open();
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
      <Tag color="blue">蓝色文字</Tag> 表示已替换的变量值（实际函件中为正常黑色）
    </div>
  </Modal>
</template>

<style scoped>
.preview-toolbar {
  padding: 12px 0;
  display: flex;
  justify-content: flex-end;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 16px;
}

.preview-container {
  padding: 24px;
  background: #e8e8e8;
  max-height: 55vh;
  overflow-y: auto;
  border-radius: 4px;
}

.preview-paper {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 50px 70px;
  min-height: 600px;
  margin: 0 auto;
  max-width: 650px;
}

.preview-content {
  font-family: "SimSun", "宋体", serif;
  font-size: 14px;
  line-height: 2;
  color: #000;
}

.preview-content :deep(h1),
.preview-content :deep(h2),
.preview-content :deep(h3) {
  text-align: center;
  margin: 0.8em 0;
}

.preview-content :deep(h2) {
  font-size: 20px;
  font-weight: bold;
  letter-spacing: 0.5em;
}

.preview-content :deep(p) {
  text-indent: 2em;
  margin: 0.5em 0;
}

.preview-content :deep(.preview-var) {
  color: #1890ff;
  font-weight: 500;
}

.preview-content :deep(hr) {
  border: none;
  border-top: 1px dashed #ccc;
  margin: 2em 0;
}

.preview-footer {
  padding: 12px 0;
  border-top: 1px solid #e8e8e8;
  margin-top: 16px;
  color: #666;
  font-size: 12px;
}
</style>
