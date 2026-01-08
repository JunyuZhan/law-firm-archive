<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { Descriptions, DescriptionsItem, Tag } from 'ant-design-vue';
import type { OperationLogDTO } from '#/api/system/types';

const currentLog = ref<OperationLogDTO | null>(null);

const [Modal, modalApi] = useVbenModal({
  footer: false,
});

// 打开详情弹窗
function open(record: OperationLogDTO) {
  currentLog.value = record;
  modalApi.setState({ title: '日志详情' });
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal class="w-[700px]">
    <Descriptions v-if="currentLog" :column="2" bordered>
      <DescriptionsItem label="模块">{{ currentLog.module }}</DescriptionsItem>
      <DescriptionsItem label="操作">{{ currentLog.action }}</DescriptionsItem>
      <DescriptionsItem label="操作人">{{ currentLog.operatorName }}</DescriptionsItem>
      <DescriptionsItem label="操作IP">{{ currentLog.operatorIp }}</DescriptionsItem>
      <DescriptionsItem label="请求方法">{{ currentLog.requestMethod }}</DescriptionsItem>
      <DescriptionsItem label="耗时">{{ currentLog.duration }}ms</DescriptionsItem>
      <DescriptionsItem label="状态">
        <Tag :color="currentLog.status === 'SUCCESS' ? 'green' : 'red'">
          {{ currentLog.status === 'SUCCESS' ? '成功' : '失败' }}
        </Tag>
      </DescriptionsItem>
      <DescriptionsItem label="操作时间">{{ currentLog.operationTime }}</DescriptionsItem>
      <DescriptionsItem label="请求URL" :span="2">{{ currentLog.requestUrl }}</DescriptionsItem>
      <DescriptionsItem label="请求参数" :span="2">
        <pre style="max-height: 200px; overflow: auto; margin: 0; white-space: pre-wrap; background: #f5f5f5; padding: 8px; border-radius: 4px;">{{ currentLog.requestParams }}</pre>
      </DescriptionsItem>
      <DescriptionsItem v-if="currentLog.status === 'FAILED'" label="错误信息" :span="2">
        <span style="color: #ff4d4f">{{ currentLog.errorMsg }}</span>
      </DescriptionsItem>
    </Descriptions>
  </Modal>
</template>
