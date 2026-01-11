<script setup lang="ts">
import type { DocumentTemplateDTO } from '#/api/document/template-types';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Alert,
  Descriptions,
  DescriptionsItem,
  message,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';

import { getTemplateDetail, previewTemplate } from '#/api/document/template';

const previewContent = ref('');
const currentTemplate = ref<DocumentTemplateDTO | null>(null);
const loading = ref(false);
const activeTab = ref('preview');

const [Modal, modalApi] = useVbenModal({
  onOpenChange(isOpen) {
    if (!isOpen) {
      previewContent.value = '';
      currentTemplate.value = null;
      activeTab.value = 'preview';
    }
  },
});

// 提取模板中的变量
const extractedVariables = computed(() => {
  if (!currentTemplate.value?.content) return [];
  const regex = /\$\{([^}]+)\}/g;
  const matches = currentTemplate.value.content.match(regex) || [];
  return [...new Set(matches.map((m) => m.replaceAll(/\$\{|\}/g, '')))];
});

function getTemplateTypeColor(type?: string) {
  const colors: Record<string, string> = {
    WORD: 'blue',
    EXCEL: 'green',
    PDF: 'red',
    HTML: 'purple',
  };
  return colors[type || ''] || 'default';
}

async function open(record: DocumentTemplateDTO) {
  loading.value = true;
  try {
    currentTemplate.value = record;
    // 获取完整详情
    const detail = await getTemplateDetail(record.id);
    currentTemplate.value = detail;

    // 获取预览内容
    const data = await previewTemplate({
      templateId: record.id,
      variables: {},
    });
    previewContent.value =
      data.content || data.preview || detail.content || '暂无内容';
    modalApi.open();
  } catch (error: any) {
    message.error(error.message || '预览失败');
  } finally {
    loading.value = false;
  }
}

defineExpose({ open });
</script>

<template>
  <Modal
    :title="`模板预览 - ${currentTemplate?.name || ''}`"
    class="w-[900px]"
    :show-footer="false"
    :loading="loading"
  >
    <Tabs v-model:active-key="activeTab">
      <TabPane key="preview" tab="预览效果">
        <Alert
          message="以下为模板预览效果，变量将显示为占位符格式"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        />
        <div class="preview-container">
          <div
            v-if="currentTemplate?.templateType === 'HTML'"
            v-html="previewContent"
            class="html-preview"
          ></div>
          <pre v-else class="text-preview">{{ previewContent }}</pre>
        </div>
      </TabPane>

      <TabPane key="info" tab="模板信息">
        <Descriptions :column="2" bordered size="small">
          <DescriptionsItem label="模板名称">
            {{ currentTemplate?.name }}
          </DescriptionsItem>
          <DescriptionsItem label="模板类型">
            <Tag :color="getTemplateTypeColor(currentTemplate?.templateType)">
              {{
                currentTemplate?.templateTypeName ||
                currentTemplate?.templateType
              }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="适用业务">
            {{ currentTemplate?.businessTypeName || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="状态">
            <Tag
              :color="
                currentTemplate?.status === 'ACTIVE' ? 'green' : 'default'
              "
            >
              {{ currentTemplate?.statusName }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="创建人">
            {{ currentTemplate?.creatorName || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="创建时间">
            {{ currentTemplate?.createdAt || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="使用次数" :span="2">
            <span style="font-weight: 500; color: #1890ff"
              >{{ currentTemplate?.useCount || 0 }} 次</span
            >
          </DescriptionsItem>
          <DescriptionsItem label="描述说明" :span="2">
            {{ currentTemplate?.description || '-' }}
          </DescriptionsItem>
        </Descriptions>
      </TabPane>

      <TabPane key="variables" tab="变量列表">
        <Alert
          message="以下是模板中使用的变量，生成文书时需要提供对应的数据"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        />
        <div v-if="extractedVariables.length > 0" class="variables-list">
          <Tag
            v-for="v in extractedVariables"
            :key="v"
            color="blue"
            style="margin: 4px"
          >
            ${ {{ v }} }
          </Tag>
        </div>
        <div v-else style="padding: 20px; color: #999; text-align: center">
          该模板未使用任何变量
        </div>
      </TabPane>
    </Tabs>
  </Modal>
</template>

<style scoped>
.preview-container {
  max-height: 500px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
}

.html-preview {
  padding: 24px;
  line-height: 1.8;
}

.html-preview :deep(h1),
.html-preview :deep(h2),
.html-preview :deep(h3) {
  margin-top: 0;
}

.html-preview :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.html-preview :deep(td),
.html-preview :deep(th) {
  padding: 8px;
  border: 1px solid #e8e8e8;
}

.text-preview {
  padding: 16px;
  margin: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.variables-list {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
</style>
