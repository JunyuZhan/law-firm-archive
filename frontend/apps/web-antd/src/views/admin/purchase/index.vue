<script setup lang="ts">
import type {
  PurchaseItemCommand,
  PurchaseQuery,
  PurchaseRequestDTO,
} from '#/api/admin/purchase';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  approvePurchaseRequest,
  cancelPurchaseRequest,
  createPurchaseRequest,
  getPurchaseDetail,
  getPurchaseList,
  getPurchaseStatistics,
  submitPurchaseRequest,
} from '#/api/admin/purchase';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'AdminPurchase' });

// 响应式布局
const { isMobile } = useResponsive();

const loading = ref(false);
const dataSource = ref<PurchaseRequestDTO[]>([]);
const total = ref(0);
const statistics = ref<Record<string, any>>({});

const queryParams = ref<PurchaseQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  purchaseType: undefined,
});

const activeTab = ref('all');
const modalVisible = ref(false);
const detailVisible = ref(false);
const approveVisible = ref(false);
const currentPurchase = ref<null | PurchaseRequestDTO>(null);

const formData = ref({
  title: '',
  purchaseType: 'OFFICE',
  expectedDate: undefined as any,
  reason: '',
  remarks: '',
  items: [
    {
      itemName: '',
      specification: '',
      unit: '个',
      quantity: 1,
      estimatedPrice: undefined as number | undefined,
      remarks: '',
    },
  ] as PurchaseItemCommand[],
});

const approveForm = ref({
  id: 0,
  approved: 1 as number,
  comment: '',
});

const columns = [
  { title: '申请编号', dataIndex: 'requestNo', key: 'requestNo', width: 140 },
  { title: '申请标题', dataIndex: 'title', key: 'title', width: 180 },
  {
    title: '采购类型',
    dataIndex: 'purchaseTypeName',
    key: 'purchaseTypeName',
    width: 100,
  },
  {
    title: '预算金额',
    dataIndex: 'estimatedAmount',
    key: 'estimatedAmount',
    width: 110,
  },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
    width: 100,
  },
  { title: '申请日期', dataIndex: 'createdAt', key: 'createdAt', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
];

const purchaseTypeOptions = [
  { label: '办公用品', value: 'OFFICE' },
  { label: '电脑设备', value: 'COMPUTER' },
  { label: '办公家具', value: 'FURNITURE' },
  { label: '其他', value: 'OTHER' },
];

const statusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  PENDING: { color: 'processing', text: '待审批' },
  APPROVED: { color: 'success', text: '已批准' },
  REJECTED: { color: 'error', text: '已拒绝' },
  PURCHASING: { color: 'warning', text: '采购中' },
  COMPLETED: { color: 'success', text: '已完成' },
  CANCELLED: { color: 'default', text: '已取消' },
};

const tabStatusMap: Record<string, string | undefined> = {
  all: undefined,
  pending: 'PENDING',
  approved: 'APPROVED',
  completed: 'COMPLETED',
};

async function loadData() {
  loading.value = true;
  try {
    const params = { ...queryParams.value };
    if (activeTab.value !== 'all') {
      params.status = tabStatusMap[activeTab.value];
    }
    const res = await getPurchaseList(params);
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadStatistics() {
  try {
    const res = await getPurchaseStatistics();
    statistics.value = res || {};
  } catch (error) {
    console.error('加载统计失败', error);
  }
}

function handleSearch() {
  queryParams.value.pageNum = 1;
  loadData();
}

function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  queryParams.value.pageNum = 1;
  loadData();
}

function handleTableChange(pagination: any) {
  queryParams.value.pageNum = pagination.current;
  queryParams.value.pageSize = pagination.pageSize;
  loadData();
}

function handleAdd() {
  formData.value = {
    title: '',
    purchaseType: 'OFFICE',
    expectedDate: undefined,
    reason: '',
    remarks: '',
    items: [
      {
        itemName: '',
        specification: '',
        unit: '个',
        quantity: 1,
        estimatedPrice: undefined,
        remarks: '',
      },
    ],
  };
  modalVisible.value = true;
}

function addItem() {
  formData.value.items.push({
    itemName: '',
    specification: '',
    unit: '个',
    quantity: 1,
    estimatedPrice: undefined,
    remarks: '',
  });
}

function removeItem(index: number) {
  if (formData.value.items.length > 1) {
    formData.value.items.splice(index, 1);
  }
}

const totalEstimatedAmount = computed(() => {
  return formData.value.items.reduce((sum, item) => {
    return sum + (item.quantity || 0) * (item.estimatedPrice || 0);
  }, 0);
});

async function handleSave() {
  if (!formData.value.title) {
    message.error('请填写申请标题');
    return;
  }
  if (!formData.value.items.some((i) => i.itemName)) {
    message.error('请至少添加一个采购物品');
    return;
  }
  try {
    const data = {
      ...formData.value,
      expectedDate: formData.value.expectedDate?.format('YYYY-MM-DD'),
      items: formData.value.items.filter((i) => i.itemName),
    };
    await createPurchaseRequest(data);
    message.success('创建成功');
    modalVisible.value = false;
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  }
}

async function handleView(record: Record<string, any>) {
  try {
    const detail = await getPurchaseDetail(record.id);
    currentPurchase.value = detail;
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

async function handleSubmit(id: number) {
  try {
    await submitPurchaseRequest(id);
    message.success('提交成功');
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '提交失败');
  }
}

function openApprove(record: Record<string, any>) {
  approveForm.value = { id: record.id, approved: 1, comment: '' };
  approveVisible.value = true;
}

async function handleApprove() {
  try {
    const isApproved = approveForm.value.approved === 1;
    await approvePurchaseRequest(
      approveForm.value.id,
      isApproved,
      approveForm.value.comment,
    );
    message.success(isApproved ? '审批通过' : '已拒绝');
    approveVisible.value = false;
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '审批失败');
  }
}

async function handleCancel(id: number) {
  try {
    await cancelPurchaseRequest(id);
    message.success('已取消');
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '取消失败');
  }
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

function formatMoney(amount?: number) {
  return amount === undefined ? '-' : `¥${amount.toLocaleString()}`;
}

onMounted(() => {
  loadData();
  loadStatistics();
});
</script>

<template>
  <Page title="采购管理" description="管理采购申请">
    <Card>
      <Tabs :active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="all" tab="全部" />
        <Tabs.TabPane key="pending" tab="待审批" />
        <Tabs.TabPane key="approved" tab="已批准" />
        <Tabs.TabPane key="completed" tab="已完成" />
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
          <Select
            v-model:value="queryParams.purchaseType"
            placeholder="采购类型"
            :style="{ width: isMobile ? '100%' : '120px', minWidth: '120px' }"
            allow-clear
            :options="purchaseTypeOptions"
            @change="handleSearch"
          />
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索采购"
            :style="{ width: isMobile ? '100%' : '200px', minWidth: '150px' }"
            allow-clear
            @press-enter="handleSearch"
          />
          <Button @click="handleSearch">查询</Button>
        </Space>
        <Button type="primary" :block="isMobile" @click="handleAdd">
          <Plus />新建采购申请
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
        }"
        :scroll="{ x: isMobile ? 800 : 1100 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'estimatedAmount'">
            {{ formatMoney(record.estimatedAmount) }}
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ record.statusName || statusMap[record.status]?.text }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <!-- eslint-disable-next-line prettier/prettier -->
              <a
                v-if="record.status === 'DRAFT'"
                @click="handleSubmit(record.id)"
                >提交</a
              >
              <!-- eslint-disable-next-line prettier/prettier -->
              <a v-if="record.status === 'PENDING'" @click="openApprove(record)"
                >审批</a
              >
              <Popconfirm
                v-if="['DRAFT', 'PENDING'].includes(record.status)"
                title="确定取消此申请？"
                @confirm="handleCancel(record.id)"
              >
                <a style="color: #ff4d4f">取消</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新建弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建采购申请"
      :width="isMobile ? '100%' : '700px'"
      :centered="isMobile"
      @ok="handleSave"
    >
      <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <FormItem label="申请标题" required>
          <Input
            v-model:value="formData.title"
            placeholder="如：办公用品采购"
          />
        </FormItem>
        <FormItem label="采购类型">
          <Select
            v-model:value="formData.purchaseType"
            :options="purchaseTypeOptions"
          />
        </FormItem>
        <FormItem label="期望到货">
          <DatePicker
            v-model:value="formData.expectedDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="采购原因">
          <Textarea
            v-model:value="formData.reason"
            :rows="2"
            placeholder="采购原因说明"
          />
        </FormItem>
        <FormItem label="采购物品">
          <div
            v-for="(item, index) in formData.items"
            :key="index"
            style="
              display: flex;
              gap: 8px;
              align-items: center;
              margin-bottom: 8px;
            "
          >
            <Input
              v-model:value="item.itemName"
              placeholder="物品名称"
              style="width: 150px"
            />
            <Input
              v-model:value="item.specification"
              placeholder="规格"
              style="width: 100px"
            />
            <InputNumber
              v-model:value="item.quantity"
              :min="1"
              placeholder="数量"
              style="width: 80px"
            />
            <InputNumber
              v-model:value="item.estimatedPrice"
              :min="0"
              placeholder="单价"
              prefix="¥"
              style="width: 100px"
            />
            <Button
              v-if="formData.items.length > 1"
              type="text"
              danger
              @click="removeItem(index)"
            >
              删除
            </Button>
          </div>
          <Button type="dashed" block @click="addItem">+ 添加物品</Button>
          <div style="margin-top: 8px; color: #666; text-align: right">
            预算总额: {{ formatMoney(totalEstimatedAmount) }}
          </div>
        </FormItem>
        <FormItem label="备注">
          <Textarea v-model:value="formData.remarks" :rows="2" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="采购申请详情"
      :width="isMobile ? '100%' : '700px'"
      :centered="isMobile"
      :footer="null"
    >
      <Descriptions v-if="currentPurchase" :column="2" bordered size="small">
        <DescriptionsItem label="申请编号">
          {{ currentPurchase.requestNo }}
        </DescriptionsItem>
        <DescriptionsItem label="申请标题">
          {{ currentPurchase.title }}
        </DescriptionsItem>
        <DescriptionsItem label="采购类型">
          {{ currentPurchase.purchaseTypeName }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentPurchase.status]?.color">
            {{ currentPurchase.statusName }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="申请人">
          {{ currentPurchase.applicantName }}
        </DescriptionsItem>
        <DescriptionsItem label="申请日期">
          {{ formatDate(currentPurchase.createdAt) }}
        </DescriptionsItem>
        <DescriptionsItem label="期望到货">
          {{ formatDate(currentPurchase.expectedDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="预算金额">
          {{ formatMoney(currentPurchase.estimatedAmount) }}
        </DescriptionsItem>
        <DescriptionsItem label="采购原因" :span="2">
          {{ currentPurchase.reason || '-' }}
        </DescriptionsItem>
        <DescriptionsItem v-if="currentPurchase.approverName" label="审批人">
          {{ currentPurchase.approverName }}
        </DescriptionsItem>
        <DescriptionsItem v-if="currentPurchase.approvalDate" label="审批日期">
          {{ formatDate(currentPurchase.approvalDate) }}
        </DescriptionsItem>
        <DescriptionsItem
          v-if="currentPurchase.approvalComment"
          label="审批意见"
          :span="2"
        >
          {{ currentPurchase.approvalComment }}
        </DescriptionsItem>
      </Descriptions>
      <div v-if="currentPurchase?.items?.length" style="margin-top: 16px">
        <h4>采购物品</h4>
        <Table
          :data-source="currentPurchase.items"
          :pagination="false"
          size="small"
          row-key="id"
          :columns="[
            { title: '物品名称', dataIndex: 'itemName' },
            { title: '规格', dataIndex: 'specification' },
            { title: '数量', dataIndex: 'quantity' },
            {
              title: '预估单价',
              dataIndex: 'estimatedPrice',
              customRender: ({ text }: any) => formatMoney(text),
            },
            {
              title: '预估金额',
              dataIndex: 'estimatedAmount',
              customRender: ({ text }: any) => formatMoney(text),
            },
          ]"
        />
      </div>
    </Modal>

    <!-- 审批弹窗 -->
    <Modal
      v-model:open="approveVisible"
      title="审批采购申请"
      :width="isMobile ? '100%' : '520px'"
      :centered="isMobile"
      @ok="handleApprove"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="审批结果">
          <Select
            v-model:value="approveForm.approved"
            :options="[
              { label: '通过', value: 1 },
              { label: '拒绝', value: 0 },
            ]"
          />
        </FormItem>
        <FormItem label="审批意见">
          <Textarea
            v-model:value="approveForm.comment"
            :rows="3"
            placeholder="审批意见"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
