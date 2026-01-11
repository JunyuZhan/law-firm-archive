<script setup lang="ts">
import type { CaseLibraryDTO } from '#/api/knowledge';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { Descriptions, DescriptionsItem, message, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { getCaseDetail } from '#/api/knowledge';

const currentCase = ref<CaseLibraryDTO | null>(null);

const referenceValueMap: Record<string, { color: string; text: string }> = {
  HIGH: { color: 'red', text: '高' },
  MEDIUM: { color: 'orange', text: '中' },
  LOW: { color: 'blue', text: '低' },
};

const [Drawer, drawerApi] = useVbenDrawer({
  overlayBlur: 4,
  placement: 'left', // 右侧按钮触发，从左侧滑入
  onOpenChange(isOpen) {
    if (!isOpen) {
      currentCase.value = null;
    }
  },
});

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

async function open(record: CaseLibraryDTO) {
  try {
    const detail = await getCaseDetail(record.id);
    currentCase.value = detail;
    drawerApi.open();
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

defineExpose({ open });
</script>

<template>
  <Drawer title="案例详情" class="w-[520px]" :show-footer="false">
    <Descriptions v-if="currentCase" :column="2" bordered size="small">
      <DescriptionsItem label="案例名称" :span="2">
        {{ currentCase.name }}
      </DescriptionsItem>
      <DescriptionsItem label="案由类型">
        {{ currentCase.caseTypeName }}
      </DescriptionsItem>
      <DescriptionsItem label="案例分类">
        {{ currentCase.categoryName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="审理法院">
        {{ currentCase.court || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="判决日期">
        {{ formatDate(currentCase.judgmentDate) }}
      </DescriptionsItem>
      <DescriptionsItem label="案件结果">
        {{ currentCase.resultName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="经办律师">
        {{ currentCase.lawyerName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="参考价值">
        <Tag
          :color="referenceValueMap[currentCase.referenceValue || '']?.color"
        >
          {{
            currentCase.referenceValueName ||
            referenceValueMap[currentCase.referenceValue || '']?.text
          }}
        </Tag>
      </DescriptionsItem>
      <DescriptionsItem label="创建时间">
        {{ formatDate(currentCase.createdAt) }}
      </DescriptionsItem>
      <DescriptionsItem label="案例摘要" :span="2">
        <div style="white-space: pre-wrap">
          {{ currentCase.summary || '-' }}
        </div>
      </DescriptionsItem>
    </Descriptions>
  </Drawer>
</template>
