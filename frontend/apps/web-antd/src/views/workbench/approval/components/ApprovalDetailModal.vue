<script setup lang="ts">
import type { ApprovalDTO } from '#/api/workbench';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  Button,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  message,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { approveApproval, getApprovalDetail } from '#/api/workbench';

const emit = defineEmits<{
  success: [];
}>();

const currentApproval = ref<ApprovalDTO | null>(null);
const approvalComment = ref('');
const approvalLoading = ref(false);
const isPending = ref(false);

// 状态颜色映射
const statusColorMap: Record<string, string> = {
  PENDING: 'orange',
  APPROVED: 'green',
  REJECTED: 'red',
  CANCELLED: 'default',
};

// 优先级颜色映射
const priorityColorMap: Record<string, string> = {
  HIGH: 'red',
  MEDIUM: 'orange',
  LOW: 'default',
};

const [VbenDrawer, drawerApi] = useVbenDrawer({
  footer: false,
  overlayBlur: 4,
  placement: 'left', // 右侧按钮触发，从左侧滑入
});

// 格式化时间
function formatTime(time: string | undefined) {
  if (!time) return '-';
  return time.replace('T', ' ').slice(0, 19);
}

// 跳转到业务详情
function goToBusinessDetail() {
  if (!currentApproval.value) return;
  const record = currentApproval.value;
  const urlMap: Record<string, string> = {
    CONTRACT: `/matter/contract?id=${record.businessId}`,
    SEAL_APPLICATION: `/document/seal-apply?id=${record.businessId}`,
    CONFLICT_CHECK: `/client/conflict?id=${record.businessId}`,
    EXPENSE: `/finance/expense?id=${record.businessId}`,
    PAYMENT_AMENDMENT: `/finance/payment-amendment?id=${record.businessId}`,
    MATTER_CLOSE: `/matter/list?id=${record.businessId}`,
    REGULARIZATION: `/hr/regularization?id=${record.businessId}`,
    RESIGNATION: `/hr/resignation?id=${record.businessId}`,
  };

  const url = urlMap[record.businessType];
  if (url) {
    window.open(url, '_blank');
  } else {
    message.info('暂不支持查看该类型业务详情');
  }
}

// 审批通过
async function handleApprove() {
  if (!currentApproval.value) return;

  approvalLoading.value = true;
  try {
    await approveApproval({
      approvalId: currentApproval.value.id,
      result: 'APPROVED',
      comment: approvalComment.value.trim() || undefined,
    });
    message.success('审批通过');
    drawerApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '审批失败');
  } finally {
    approvalLoading.value = false;
  }
}

// 审批拒绝
async function handleReject() {
  if (!currentApproval.value) return;

  if (!approvalComment.value.trim()) {
    message.warning('拒绝时必须填写审批意见');
    return;
  }

  approvalLoading.value = true;
  try {
    await approveApproval({
      approvalId: currentApproval.value.id,
      result: 'REJECTED',
      comment: approvalComment.value.trim(),
    });
    message.success('已拒绝');
    drawerApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  } finally {
    approvalLoading.value = false;
  }
}

// 打开抽屉
async function open(record: ApprovalDTO, pending: boolean = false) {
  isPending.value = pending;
  approvalComment.value = '';
  drawerApi.setState({ title: pending ? '审批详情' : '查看审批' });
  drawerApi.open();

  try {
    currentApproval.value = await getApprovalDetail(record.id);
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '获取详情失败');
    drawerApi.close();
  }
}

defineExpose({ open });
</script>

<template>
  <VbenDrawer class="w-[520px]">
    <template v-if="currentApproval">
      <Descriptions
        :column="2"
        bordered
        size="small"
        style="margin-bottom: 16px"
      >
        <DescriptionsItem label="审批编号" :span="2">
          {{ currentApproval.approvalNo }}
        </DescriptionsItem>
        <DescriptionsItem label="业务类型">
          {{ currentApproval.businessTypeName }}
        </DescriptionsItem>
        <DescriptionsItem label="业务编号">
          {{ currentApproval.businessNo || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="业务标题" :span="2">
          {{ currentApproval.businessTitle }}
        </DescriptionsItem>
        <DescriptionsItem label="申请人">
          {{ currentApproval.applicantName }}
        </DescriptionsItem>
        <DescriptionsItem label="申请时间">
          {{ formatTime(currentApproval.createdAt) }}
        </DescriptionsItem>
        <DescriptionsItem label="审批人">
          {{ currentApproval.approverName }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusColorMap[currentApproval.status] || 'default'">
            {{ currentApproval.statusName || currentApproval.status }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="优先级">
          <Tag :color="priorityColorMap[currentApproval.priority || 'MEDIUM']">
            {{ currentApproval.priorityName || '中' }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="紧急程度">
          <Tag v-if="currentApproval.urgency === 'URGENT'" color="red">
            紧急
          </Tag>
          <span v-else>普通</span>
        </DescriptionsItem>
        <DescriptionsItem
          v-if="currentApproval.approvedAt"
          label="审批时间"
          :span="2"
        >
          {{ formatTime(currentApproval.approvedAt) }}
        </DescriptionsItem>
        <DescriptionsItem
          v-if="currentApproval.comment"
          label="审批意见"
          :span="2"
        >
          {{ currentApproval.comment }}
        </DescriptionsItem>
      </Descriptions>

      <div style="margin-bottom: 16px">
        <Button type="link" @click="goToBusinessDetail" style="padding: 0">
          📄 查看业务详情 →
        </Button>
      </div>

      <!-- 待审批时显示审批操作区域 -->
      <template v-if="isPending && currentApproval.status === 'PENDING'">
        <Form layout="vertical">
          <FormItem label="审批意见">
            <Textarea
              v-model:value="approvalComment"
              placeholder="请输入审批意见（拒绝时必填）"
              :rows="3"
              :maxlength="500"
              show-count
            />
          </FormItem>
        </Form>
        <div style="display: flex; gap: 12px; justify-content: flex-end">
          <Button @click="drawerApi.close()">取消</Button>
          <Button danger :loading="approvalLoading" @click="handleReject">
            拒绝
          </Button>
          <Button
            type="primary"
            :loading="approvalLoading"
            @click="handleApprove"
          >
            通过
          </Button>
        </div>
      </template>

      <!-- 非待审批状态显示关闭按钮 -->
      <template v-else>
        <div style="display: flex; justify-content: flex-end">
          <Button @click="drawerApi.close()">关闭</Button>
        </div>
      </template>
    </template>
  </VbenDrawer>
</template>
