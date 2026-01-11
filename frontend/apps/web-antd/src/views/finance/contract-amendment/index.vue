<script setup lang="ts">
import { onMounted, ref } from 'vue';

import {
  Button,
  Card,
  Input,
  message,
  Modal,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { requestClient } from '#/api/request';

interface Amendment {
  id: number;
  amendmentNo: string;
  contractId: number;
  amendmentType: string;
  amendmentReason: string;
  lawyerAmendedAt: string;
  status: string;
  affectsPayments: boolean;
  financeRemark: string;
}

const loading = ref(false);
const amendments = ref<Amendment[]>([]);
const remarkInput = ref('');

const columns = [
  {
    title: '变更编号',
    dataIndex: 'amendmentNo',
    key: 'amendmentNo',
    width: 160,
  },
  { title: '合同ID', dataIndex: 'contractId', key: 'contractId', width: 100 },
  {
    title: '变更类型',
    dataIndex: 'amendmentType',
    key: 'amendmentType',
    width: 120,
  },
  {
    title: '变更说明',
    dataIndex: 'amendmentReason',
    key: 'amendmentReason',
    ellipsis: true,
  },
  {
    title: '变更时间',
    dataIndex: 'lawyerAmendedAt',
    key: 'lawyerAmendedAt',
    width: 180,
  },
  {
    title: '影响收款',
    dataIndex: 'affectsPayments',
    key: 'affectsPayments',
    width: 100,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

const amendmentTypeMap: Record<string, string> = {
  AMOUNT: '金额变更',
  PARTICIPANT: '参与人变更',
  SCHEDULE: '付款计划变更',
  OTHER: '其他变更',
};

const statusMap: Record<string, { color: string; text: string }> = {
  PENDING: { text: '待处理', color: 'orange' },
  SYNCED: { text: '已同步', color: 'green' },
  IGNORED: { text: '已忽略', color: 'default' },
  PARTIAL: { text: '部分同步', color: 'blue' },
};

async function fetchPendingAmendments() {
  loading.value = true;
  try {
    const res = await requestClient.get<Amendment[]>(
      '/finance/contract-amendments/pending',
    );
    amendments.value = res || [];
  } catch {
    message.error('获取待处理变更失败');
  } finally {
    loading.value = false;
  }
}

async function handleSync(record: Record<string, any>) {
  Modal.confirm({
    title: '确认同步',
    content: `确定要同步变更 ${record.amendmentNo} 到财务数据吗？${record.affectsPayments ? '注意：此变更会影响已有收款记录！' : ''}`,
    okText: '确认同步',
    cancelText: '取消',
    onOk: async () => {
      try {
        await requestClient.post(
          `/finance/contract-amendments/${record.id}/sync`,
          null,
          {
            params: { remark: remarkInput.value || '财务确认同步' },
          },
        );
        message.success('同步成功');
        fetchPendingAmendments();
      } catch {
        message.error('同步失败');
      }
    },
  });
}

const ignoreModalVisible = ref(false);
const currentIgnoreRecord = ref<Amendment | null>(null);
const ignoreReason = ref('');

function showIgnoreModal(record: Record<string, any>) {
  currentIgnoreRecord.value = record as Amendment;
  ignoreReason.value = '';
  ignoreModalVisible.value = true;
}

async function confirmIgnore() {
  if (!ignoreReason.value) {
    message.warning('请输入忽略原因');
    return;
  }
  if (!currentIgnoreRecord.value) return;

  try {
    await requestClient.post(
      `/finance/contract-amendments/${currentIgnoreRecord.value.id}/ignore`,
      null,
      {
        params: { remark: ignoreReason.value },
      },
    );
    message.success('已忽略');
    ignoreModalVisible.value = false;
    ignoreReason.value = '';
    currentIgnoreRecord.value = null;
    fetchPendingAmendments();
  } catch {
    message.error('操作失败');
  }
}

onMounted(() => {
  fetchPendingAmendments();
});
</script>

<template>
  <div class="contract-amendment-page">
    <Card title="合同变更处理" :bordered="false">
      <template #extra>
        <Button
          type="primary"
          @click="fetchPendingAmendments"
          :loading="loading"
        >
          刷新
        </Button>
      </template>

      <Table
        :columns="columns"
        :data-source="amendments"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amendmentType'">
            {{ amendmentTypeMap[record.amendmentType] || record.amendmentType }}
          </template>
          <template v-else-if="column.key === 'affectsPayments'">
            <Tag :color="record.affectsPayments ? 'red' : 'green'">
              {{ record.affectsPayments ? '是' : '否' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ statusMap[record.status]?.text || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space v-if="record.status === 'PENDING'">
              <Button type="primary" size="small" @click="handleSync(record)">
                同步
              </Button>
              <Button size="small" danger @click="showIgnoreModal(record)">
                忽略
              </Button>
            </Space>
            <span v-else class="text-gray-400">已处理</span>
          </template>
        </template>
      </Table>

      <!-- 忽略确认弹窗 -->
      <Modal
        v-model:open="ignoreModalVisible"
        title="确认忽略"
        @ok="confirmIgnore"
        ok-text="确认忽略"
        cancel-text="取消"
      >
        <p v-if="currentIgnoreRecord">
          确定要忽略变更 {{ currentIgnoreRecord.amendmentNo }} 吗？
        </p>
        <Input.TextArea
          v-model:value="ignoreReason"
          placeholder="请输入忽略原因"
          :rows="3"
        />
      </Modal>
    </Card>
  </div>
</template>

<style scoped>
.contract-amendment-page {
  padding: 16px;
}
</style>
