<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import { Modal, Button, Space, Tag } from 'ant-design-vue';
import { sampleData } from './contract-templates';

interface ContractTemplateDTO {
  id: number;
  name: string;
  content: string;
}

// 状态
const visible = ref(false);
const previewContent = ref('');
const previewTitle = ref('');

// 打开预览
function open(record: ContractTemplateDTO) {
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
  visible.value = true;
}

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
        h2 { font-size: 18pt; }
        h3 { font-size: 14pt; text-align: left; margin: 1.5em 0 0.5em; }
        p { text-indent: 2em; margin: 0.5em 0; }
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

defineExpose({
  open,
});
</script>

<template>
  <Modal
    v-model:open="visible"
    :title="`预览 - ${previewTitle}`"
    width="900px"
    :footer="null"
    :body-style="{ padding: '0' }"
  >
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
      <Tag color="blue">蓝色文字</Tag> 表示已替换的变量值（实际合同中为正常黑色）
    </div>
  </Modal>
</template>

<style scoped>
.preview-toolbar {
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: flex-end;
}

.preview-container {
  padding: 24px;
  background: #e8e8e8;
  max-height: 70vh;
  overflow-y: auto;
}

.preview-paper {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  padding: 60px 80px;
  min-height: 800px;
  margin: 0 auto;
  max-width: 800px;
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
  margin: 1em 0;
}

.preview-content :deep(h2) {
  font-size: 22px;
  font-weight: bold;
}

.preview-content :deep(h3) {
  font-size: 16px;
  font-weight: bold;
  text-align: left;
  margin: 1.5em 0 0.5em;
}

.preview-content :deep(p) {
  text-indent: 2em;
  margin: 0.5em 0;
}

.preview-content :deep(.preview-var) {
  color: #1890ff;
  font-weight: 500;
}

.preview-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}

.preview-content :deep(td),
.preview-content :deep(th) {
  border: 1px solid #000;
  padding: 8px 12px;
}

.preview-footer {
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  color: #666;
  font-size: 12px;
}
</style>

