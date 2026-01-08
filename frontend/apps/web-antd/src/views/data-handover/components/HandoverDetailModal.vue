<script setup lang="ts">
import { ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message, Modal } from 'ant-design-vue';
import {
  Descriptions,
  DescriptionsItem,
  Tag,
  Divider,
  Row,
  Col,
  Statistic,
  Table,
  Space,
  Button,
  Alert,
} from 'ant-design-vue';
import { getHandoverDetail, confirmHandover, cancelHandover } from '#/api/system';
import type { DataHandoverDTO } from '#/api/system/types';

const emit = defineEmits<{
  success: [];
}>();

const detailData = ref<DataHandoverDTO | null>(null);
const loading = ref(false);

// 状态颜色
function getStatusColor(status: string) {
  switch (status) {
    case 'PENDING_APPROVAL': return 'processing';
    case 'APPROVED': return 'orange';
    case 'REJECTED': return 'error';
    case 'CONFIRMED': return 'success';
    case 'CANCELLED': return 'default';
    default: return 'default';
  }
}

const [VbenModal, modalApi] = useVbenModal({
  footer: false,
});

// 确认交接
function handleConfirm() {
  if (!detailData.value) return;
  const record = detailData.value;
  Modal.confirm({
    title: '确认交接',
    content: `确定要执行交接单「${record.handoverNo}」吗？确认后将立即执行数据迁移。`,
    okText: '确认执行',
    cancelText: '取消',
    okType: 'primary',
    onOk: async () => {
      try {
        await confirmHandover(record.id);
        message.success('交接已完成');
        detailData.value = await getHandoverDetail(record.id);
        emit('success');
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '确认失败');
      }
    },
  });
}

// 取消交接
function handleCancel() {
  if (!detailData.value) return;
  const record = detailData.value;
  Modal.confirm({
    title: '取消交接',
    content: `确定要取消交接单「${record.handoverNo}」吗？`,
    okText: '确认取消',
    cancelText: '返回',
    okType: 'danger',
    onOk: async () => {
      try {
        await cancelHandover(record.id, '用户取消');
        message.success('已取消');
        modalApi.close();
        emit('success');
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '取消失败');
      }
    },
  });
}

// 打开详情弹窗
async function open(record: DataHandoverDTO) {
  loading.value = true;
  modalApi.setState({ title: '交接单详情' });
  modalApi.open();
  try {
    detailData.value = await getHandoverDetail(record.id);
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载详情失败');
    modalApi.close();
  } finally {
    loading.value = false;
  }
}

defineExpose({ open });
</script>

<template>
  <VbenModal class="w-[900px]">
    <div v-if="detailData" :loading="loading">
      <Descriptions :column="3" bordered size="small">
        <DescriptionsItem label="交接单号">{{ detailData.handoverNo }}</DescriptionsItem>
        <DescriptionsItem label="交接类型">{{ detailData.handoverTypeName }}</DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="getStatusColor(detailData.status)">{{ detailData.statusName }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem label="移交人">{{ detailData.fromUsername }}</DescriptionsItem>
        <DescriptionsItem label="接收人">{{ detailData.toUsername }}</DescriptionsItem>
        <DescriptionsItem label="提交人">{{ detailData.submittedByName }}</DescriptionsItem>
        <DescriptionsItem label="交接原因" :span="3">{{ detailData.handoverReason || '-' }}</DescriptionsItem>
        <DescriptionsItem label="提交时间">{{ detailData.submittedAt || '-' }}</DescriptionsItem>
        <DescriptionsItem label="确认人">{{ detailData.confirmedByName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="确认时间">{{ detailData.confirmedAt || '-' }}</DescriptionsItem>
      </Descriptions>

      <Divider>交接明细</Divider>

      <Row :gutter="16" style="margin-bottom: 16px">
        <Col :span="6"><Statistic title="项目" :value="detailData.matterCount" /></Col>
        <Col :span="6"><Statistic title="客户" :value="detailData.clientCount" /></Col>
        <Col :span="6"><Statistic title="案源" :value="detailData.leadCount" /></Col>
        <Col :span="6"><Statistic title="任务" :value="detailData.taskCount" /></Col>
      </Row>

      <Table
        v-if="detailData.details && detailData.details.length > 0"
        :columns="[
          { title: '数据类型', dataIndex: 'dataTypeName', width: 80 },
          { title: '数据编号', dataIndex: 'dataNo', width: 120 },
          { title: '数据名称', dataIndex: 'dataName', ellipsis: true },
          { title: '变更字段', dataIndex: 'fieldDisplayName', width: 100 },
          { title: '原值', dataIndex: 'oldUserName', width: 80 },
          { title: '新值', dataIndex: 'newUserName', width: 80 },
          { title: '状态', dataIndex: 'statusName', width: 80 },
        ]"
        :data-source="detailData.details"
        :pagination="false"
        size="small"
        row-key="id"
        :scroll="{ y: 300 }"
      />

      <div v-if="detailData.status === 'PENDING_APPROVAL'" style="text-align: right; margin-top: 24px">
        <Space>
          <Alert message="交接单正在等待审批，审批通过后可确认执行" type="info" show-icon style="margin-right: 16px" />
          <Button danger @click="handleCancel">取消交接</Button>
        </Space>
      </div>
      <div v-else-if="detailData.status === 'APPROVED'" style="text-align: right; margin-top: 24px">
        <Space>
          <Alert message="审批已通过，请确认执行数据交接" type="success" show-icon style="margin-right: 16px" />
          <Button danger @click="handleCancel">取消交接</Button>
          <Button type="primary" @click="handleConfirm">确认执行</Button>
        </Space>
      </div>
    </div>
  </VbenModal>
</template>
