<script setup lang="ts">
import type {
  CareerLevelDTO,
  PromotionApplicationDTO,
  PromotionApplicationQuery,
} from '#/api/hr/promotion';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import { useResponsive } from '#/hooks/useResponsive';

import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
  Timeline,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  approvePromotionApplication,
  cancelPromotionApplication,
  getCareerLevelList,
  getPromotionApplicationDetail,
  getPromotionApplicationList,
  rejectPromotionApplication,
  submitPromotionApplication,
} from '#/api/hr/promotion';
import UserTreeSelect from '#/components/UserTreeSelect';

defineOptions({ name: 'HrPromotion' });

// 响应式布局
const { isMobile } = useResponsive();

// 状态
const loading = ref(false);
const dataSource = ref<PromotionApplicationDTO[]>([]);
const total = ref(0);
const activeTab = ref('all');
const careerLevels = ref<CareerLevelDTO[]>([]);

// 查询参数
const queryParams = ref<PromotionApplicationQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
});

// 弹窗
const modalVisible = ref(false);
const detailVisible = ref(false);
const approveVisible = ref(false);
const currentRecord = ref<null | PromotionApplicationDTO>(null);

// 表单
const formData = ref({
  targetLevelId: undefined as number | undefined,
  applyReason: '',
  achievements: '',
  selfEvaluation: '',
  directManagerId: undefined as number | undefined, // 直属上级
  hrReviewerId: undefined as number | undefined, // HR评审人
});

const approveForm = ref({
  approved: 'approve' as string, // 'approve' | 'reject'
  comment: '',
  effectiveDate: undefined as any,
});

// 表格列
const columns = [
  {
    title: '申请编号',
    dataIndex: 'applicationNo',
    key: 'applicationNo',
    width: 140,
  },
  {
    title: '员工姓名',
    dataIndex: 'employeeName',
    key: 'employeeName',
    width: 100,
  },
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 120,
  },
  {
    title: '当前职级',
    dataIndex: 'currentLevelName',
    key: 'currentLevelName',
    width: 100,
  },
  {
    title: '申请职级',
    dataIndex: 'targetLevelName',
    key: 'targetLevelName',
    width: 100,
  },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 状态映射
const statusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  PENDING: { color: 'processing', text: '待审批' },
  REVIEWING: { color: 'warning', text: '评审中' },
  APPROVED: { color: 'success', text: '已通过' },
  REJECTED: { color: 'error', text: '已拒绝' },
  CANCELLED: { color: 'default', text: '已取消' },
};

// 加载数据
async function loadData() {
  loading.value = true;
  try {
    const res = await getPromotionApplicationList(queryParams.value);
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 加载职级列表
async function loadCareerLevels() {
  try {
    const res = await getCareerLevelList({
      pageNum: 1,
      pageSize: 100,
      status: 'ACTIVE',
    });
    careerLevels.value = res.list || [];
  } catch (error) {
    console.error('加载职级失败', error);
  }
}

// Tab切换
function handleTabChange(key: number | string) {
  const keyStr = String(key);
  activeTab.value = keyStr;
  queryParams.value.status = keyStr === 'all' ? undefined : keyStr;
  queryParams.value.pageNum = 1;
  loadData();
}

// 搜索
function handleSearch() {
  queryParams.value.pageNum = 1;
  loadData();
}

// 分页
function handleTableChange(pagination: any) {
  queryParams.value.pageNum = pagination.current;
  queryParams.value.pageSize = pagination.pageSize;
  loadData();
}

// 打开申请弹窗
function handleAdd() {
  formData.value = {
    targetLevelId: undefined,
    applyReason: '',
    achievements: '',
    selfEvaluation: '',
    directManagerId: undefined,
    hrReviewerId: undefined,
  };
  modalVisible.value = true;
}

// 提交申请
async function handleSubmit() {
  if (!formData.value.targetLevelId) {
    message.error('请选择目标职级');
    return;
  }
  if (!formData.value.directManagerId) {
    message.error('请选择直属上级（审批人）');
    return;
  }
  if (!formData.value.applyReason) {
    message.error('请填写申请理由');
    return;
  }
  try {
    await submitPromotionApplication({
      targetLevelId: formData.value.targetLevelId,
      applyReason: formData.value.applyReason,
      achievements: formData.value.achievements,
      selfEvaluation: formData.value.selfEvaluation,
    });
    message.success('申请提交成功');
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '提交失败');
  }
}

// 查看详情
async function handleView(record: any) {
  try {
    const detail = await getPromotionApplicationDetail(record.id);
    currentRecord.value = detail;
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 取消申请
async function handleCancel(id: number) {
  try {
    await cancelPromotionApplication(id);
    message.success('已取消申请');
    loadData();
  } catch (error: any) {
    message.error(error.message || '取消失败');
  }
}

// 打开审批弹窗
function handleApprove(record: any) {
  currentRecord.value = record;
  approveForm.value = {
    approved: 'approve',
    comment: '',
    effectiveDate: dayjs().add(1, 'month').startOf('month'),
  };
  approveVisible.value = true;
}

// 提交审批
async function submitApproval() {
  if (!currentRecord.value) return;
  try {
    if (approveForm.value.approved === 'approve') {
      await approvePromotionApplication(
        currentRecord.value.id,
        approveForm.value.comment,
        approveForm.value.effectiveDate?.format('YYYY-MM-DD'),
      );
      message.success('已通过晋升申请');
    } else {
      await rejectPromotionApplication(
        currentRecord.value.id,
        approveForm.value.comment,
      );
      message.success('已拒绝晋升申请');
    }
    approveVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '审批失败');
  }
}

// 格式化日期
function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

onMounted(() => {
  loadData();
  loadCareerLevels();
});
</script>

<template>
  <Page title="晋升管理" description="管理员工晋升申请">
    <Card>
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="all" tab="全部" />
        <Tabs.TabPane key="PENDING" tab="待审批" />
        <Tabs.TabPane key="APPROVED" tab="已通过" />
        <Tabs.TabPane key="REJECTED" tab="已拒绝" />
      </Tabs>

      <div
        :style="{
          display: 'flex',
          flexDirection: isMobile ? 'column' : 'row',
          justifyContent: 'space-between',
          gap: isMobile ? '12px' : '0',
          marginBottom: '16px',
        }"
      >
        <Space :wrap="isMobile">
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索员工姓名"
            :style="{ width: isMobile ? '100%' : '200px', minWidth: '150px' }"
            allow-clear
            @press-enter="handleSearch"
          />
          <Button @click="handleSearch">查询</Button>
        </Space>
        <Button type="primary" :block="isMobile" @click="handleAdd">
          <Plus />
          申请晋升
        </Button>
      </div>

      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showQuickJumper: !isMobile,
        }"
        :scroll="{ x: isMobile ? 800 : 1000 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'applyDate'">
            {{ formatDate(record.applyDate) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{
                record.statusName ||
                statusMap[record.status]?.text ||
                record.status
              }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <Popconfirm
                v-if="record.status === 'DRAFT' || record.status === 'PENDING'"
                title="确定要取消此申请吗？"
                @confirm="handleCancel(record.id)"
              >
                <a style="color: #ff4d4f">取消</a>
              </Popconfirm>
              <a
                v-if="
                  record.status === 'PENDING' || record.status === 'REVIEWING'
                "
                @click="handleApprove(record)"
                >审批</a
              >
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="申请晋升"
      :width="isMobile ? '100%' : '650px'"
      :centered="isMobile"
      @ok="handleSubmit"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="目标职级" required>
          <Select
            v-model:value="formData.targetLevelId"
            placeholder="请选择目标职级"
            :options="
              careerLevels.map((l) => ({
                label: `${l.levelName} (${l.categoryName || l.category})`,
                value: l.id,
              }))
            "
          />
        </FormItem>
        <FormItem label="直属上级" required>
          <UserTreeSelect
            v-model:value="formData.directManagerId"
            placeholder="请从组织架构中选择直属上级（审批人）"
          />
        </FormItem>
        <FormItem label="HR评审人">
          <UserTreeSelect
            v-model:value="formData.hrReviewerId"
            placeholder="请从组织架构中选择HR评审人（可选）"
          />
        </FormItem>
        <FormItem label="申请理由" required>
          <Textarea
            v-model:value="formData.applyReason"
            :rows="3"
            placeholder="请说明申请晋升的理由"
          />
        </FormItem>
        <FormItem label="工作业绩">
          <Textarea
            v-model:value="formData.achievements"
            :rows="3"
            placeholder="请描述您的主要工作业绩"
          />
        </FormItem>
        <FormItem label="自我评价">
          <Textarea
            v-model:value="formData.selfEvaluation"
            :rows="3"
            placeholder="请进行自我评价"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="晋升申请详情"
      :width="isMobile ? '100%' : '700px'"
      :centered="isMobile"
      :footer="null"
    >
      <Descriptions v-if="currentRecord" :column="2" bordered size="small">
        <DescriptionsItem label="申请编号">
          {{ currentRecord.applicationNo }}
        </DescriptionsItem>
        <DescriptionsItem label="员工姓名">
          {{ currentRecord.employeeName }}
        </DescriptionsItem>
        <DescriptionsItem label="部门">
          {{ currentRecord.departmentName }}
        </DescriptionsItem>
        <DescriptionsItem label="当前职级">
          {{ currentRecord.currentLevelName }}
        </DescriptionsItem>
        <DescriptionsItem label="目标职级">
          {{ currentRecord.targetLevelName }}
        </DescriptionsItem>
        <DescriptionsItem label="申请日期">
          {{ formatDate(currentRecord.applyDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag
            :color="
              currentRecord.status
                ? statusMap[currentRecord.status]?.color
                : 'default'
            "
          >
            {{
              currentRecord.statusName ||
              (currentRecord.status
                ? statusMap[currentRecord.status]?.text
                : '-')
            }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="生效日期">
          {{ formatDate(currentRecord.effectiveDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="申请理由" :span="2">
          {{ currentRecord.applyReason || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="工作业绩" :span="2">
          {{ currentRecord.achievements || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="自我评价" :span="2">
          {{ currentRecord.selfEvaluation || '-' }}
        </DescriptionsItem>
        <DescriptionsItem
          v-if="currentRecord.approvalComment"
          label="审批意见"
          :span="2"
        >
          {{ currentRecord.approvalComment }}
        </DescriptionsItem>
      </Descriptions>

      <div v-if="currentRecord?.reviews?.length" style="margin-top: 16px">
        <h4>评审记录</h4>
        <Timeline>
          <Timeline.Item
            v-for="review in currentRecord.reviews"
            :key="review.id"
          >
            <p>
              <strong>{{ review.reviewerName }}</strong> ({{
                review.reviewerRoleName
              }})
            </p>
            <p>
              评审意见: {{ review.reviewOpinionName }} | 评分:
              {{ review.totalScore }}
            </p>
            <p v-if="review.reviewComment">评语: {{ review.reviewComment }}</p>
            <p style="font-size: 12px; color: #999">
              {{ formatDate(review.reviewTime) }}
            </p>
          </Timeline.Item>
        </Timeline>
      </div>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveVisible"
      title="审批晋升申请"
      :width="isMobile ? '100%' : '520px'"
      :centered="isMobile"
      @ok="submitApproval"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="审批结果">
          <Select
            v-model:value="approveForm.approved"
            :options="[
              { label: '通过', value: 'approve' },
              { label: '拒绝', value: 'reject' },
            ]"
          />
        </FormItem>
        <FormItem v-if="approveForm.approved === 'approve'" label="生效日期">
          <DatePicker
            v-model:value="approveForm.effectiveDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="审批意见">
          <Textarea
            v-model:value="approveForm.comment"
            :rows="3"
            placeholder="请输入审批意见"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
