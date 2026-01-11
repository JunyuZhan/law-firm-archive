<script setup lang="ts">
import type { AssetDTO, AssetQuery, AssetRecordDTO } from '#/api/admin/asset';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';

import {
  Button,
  Card,
  Col,
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
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Textarea,
  Timeline,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createAsset,
  getAssetDetail,
  getAssetList,
  getAssetRecords,
  getAssetStatistics,
  receiveAsset,
  returnAsset,
  scrapAsset,
  updateAsset,
} from '#/api/admin/asset';
import { UserTreeSelect } from '#/components/UserTreeSelect';

defineOptions({ name: 'AdminAsset' });

const loading = ref(false);
const dataSource = ref<AssetDTO[]>([]);
const total = ref(0);
const statistics = ref<Record<string, any>>({});

const queryParams = ref<AssetQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: undefined,
  status: undefined,
});

const modalVisible = ref(false);
const detailVisible = ref(false);
const receiveVisible = ref(false);
const editingId = ref<null | number>(null);
const currentAsset = ref<AssetDTO | null>(null);
const assetRecords = ref<AssetRecordDTO[]>([]);

const formData = ref({
  name: '',
  category: 'COMPUTER',
  brand: '',
  model: '',
  specification: '',
  serialNumber: '',
  purchaseDate: undefined as any,
  purchasePrice: undefined as number | undefined,
  supplier: '',
  warrantyExpireDate: undefined as any,
  usefulLife: undefined as number | undefined,
  location: '',
  remarks: '',
});

const receiveForm = ref({
  assetId: 0,
  userId: undefined as number | undefined,
  expectedReturnDate: undefined as any,
  reason: '',
});

const columns = [
  { title: '资产编号', dataIndex: 'assetNo', key: 'assetNo', width: 130 },
  { title: '资产名称', dataIndex: 'name', key: 'name', width: 150 },
  {
    title: '资产类型',
    dataIndex: 'categoryName',
    key: 'categoryName',
    width: 100,
  },
  { title: '品牌/型号', dataIndex: 'brand', key: 'brand', width: 120 },
  {
    title: '使用人',
    dataIndex: 'currentUserName',
    key: 'currentUserName',
    width: 100,
  },
  { title: '存放位置', dataIndex: 'location', key: 'location', width: 120 },
  {
    title: '购入日期',
    dataIndex: 'purchaseDate',
    key: 'purchaseDate',
    width: 110,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
];

const categoryOptions = [
  { label: '电脑设备', value: 'COMPUTER' },
  { label: '办公家具', value: 'FURNITURE' },
  { label: '办公设备', value: 'OFFICE_EQUIPMENT' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '其他', value: 'OTHER' },
];

const statusMap: Record<string, { color: string; text: string }> = {
  IDLE: { color: 'default', text: '闲置' },
  IN_USE: { color: 'success', text: '在用' },
  MAINTENANCE: { color: 'warning', text: '维修中' },
  SCRAPPED: { color: 'error', text: '已报废' },
};

async function loadData() {
  loading.value = true;
  try {
    const res = await getAssetList(queryParams.value);
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
    const res = await getAssetStatistics();
    statistics.value = res || {};
  } catch (error) {
    console.error('加载统计失败', error);
  }
}

function handleSearch() {
  queryParams.value.pageNum = 1;
  loadData();
}

function handleTableChange(pagination: any) {
  queryParams.value.pageNum = pagination.current;
  queryParams.value.pageSize = pagination.pageSize;
  loadData();
}

function handleAdd() {
  editingId.value = null;
  formData.value = {
    name: '',
    category: 'COMPUTER',
    brand: '',
    model: '',
    specification: '',
    serialNumber: '',
    purchaseDate: undefined,
    purchasePrice: undefined,
    supplier: '',
    warrantyExpireDate: undefined,
    usefulLife: undefined,
    location: '',
    remarks: '',
  };
  modalVisible.value = true;
}

async function handleEdit(record: Record<string, any>) {
  try {
    const detail = await getAssetDetail(record.id);
    editingId.value = record.id;
    formData.value = {
      name: detail.name,
      category: detail.category,
      brand: detail.brand || '',
      model: detail.model || '',
      specification: detail.specification || '',
      serialNumber: detail.serialNumber || '',
      purchaseDate: detail.purchaseDate
        ? dayjs(detail.purchaseDate)
        : undefined,
      purchasePrice: detail.purchasePrice,
      supplier: detail.supplier || '',
      warrantyExpireDate: detail.warrantyExpireDate
        ? dayjs(detail.warrantyExpireDate)
        : undefined,
      usefulLife: detail.usefulLife,
      location: detail.location || '',
      remarks: detail.remarks || '',
    };
    modalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

async function handleSave() {
  if (!formData.value.name || !formData.value.category) {
    message.error('请填写必填项');
    return;
  }
  try {
    const data = {
      ...formData.value,
      purchaseDate: formData.value.purchaseDate?.format('YYYY-MM-DD'),
      warrantyExpireDate:
        formData.value.warrantyExpireDate?.format('YYYY-MM-DD'),
    };
    if (editingId.value) {
      await updateAsset(editingId.value, data);
      message.success('更新成功');
    } else {
      await createAsset(data);
      message.success('创建成功');
    }
    modalVisible.value = false;
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  }
}

async function handleView(record: Record<string, any>) {
  try {
    const [detail, records] = await Promise.all([
      getAssetDetail(record.id),
      getAssetRecords(record.id),
    ]);
    currentAsset.value = detail;
    assetRecords.value = records || [];
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

function openReceive(record: Record<string, any>) {
  receiveForm.value = {
    assetId: record.id,
    userId: undefined,
    expectedReturnDate: undefined,
    reason: '',
  };
  receiveVisible.value = true;
}

async function handleReceive() {
  if (!receiveForm.value.userId) {
    message.error('请选择领用人');
    return;
  }
  try {
    await receiveAsset({
      assetId: receiveForm.value.assetId,
      userId: receiveForm.value.userId,
      expectedReturnDate:
        receiveForm.value.expectedReturnDate?.format('YYYY-MM-DD'),
      reason: receiveForm.value.reason,
    });
    message.success('领用成功');
    receiveVisible.value = false;
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '领用失败');
  }
}

async function handleReturn(id: number) {
  try {
    await returnAsset(id);
    message.success('归还成功');
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '归还失败');
  }
}

async function handleScrap(id: number) {
  try {
    await scrapAsset(id, '资产报废');
    message.success('报废成功');
    loadData();
    loadStatistics();
  } catch (error: any) {
    message.error(error.message || '报废失败');
  }
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

function formatMoney(amount?: number) {
  return amount ? `¥${amount.toLocaleString()}` : '-';
}

onMounted(() => {
  loadData();
  loadStatistics();
});
</script>

<template>
  <Page title="资产管理" description="管理律所固定资产">
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :span="6">
        <Card>
          <Statistic
            title="资产总数"
            :value="statistics.totalCount || 0"
            suffix="件"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="在用资产"
            :value="statistics.inUseCount || 0"
            suffix="件"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="闲置资产"
            :value="statistics.idleCount || 0"
            suffix="件"
          />
        </Card>
      </Col>
      <Col :span="6">
        <Card>
          <Statistic
            title="资产总值"
            :value="statistics.totalValue || 0"
            prefix="¥"
            :precision="0"
          />
        </Card>
      </Col>
    </Row>

    <Card>
      <div
        style="
          display: flex;
          justify-content: space-between;
          margin-bottom: 16px;
        "
      >
        <Space>
          <Select
            v-model:value="queryParams.category"
            placeholder="资产类型"
            style="width: 120px"
            allow-clear
            :options="categoryOptions"
            @change="handleSearch"
          />
          <Select
            v-model:value="queryParams.status"
            placeholder="状态"
            style="width: 100px"
            allow-clear
            :options="
              Object.entries(statusMap).map(([k, v]) => ({
                label: v.text,
                value: k,
              }))
            "
            @change="handleSearch"
          />
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索资产"
            style="width: 200px"
            allow-clear
            @press-enter="handleSearch"
          />
          <Button @click="handleSearch">查询</Button>
        </Space>
        <Button type="primary" @click="handleAdd"><Plus />添加资产</Button>
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
        :scroll="{ x: 1200 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'brand'">
            {{ record.brand }}{{ record.model ? ` / ${record.model}` : '' }}
          </template>
          <template v-else-if="column.key === 'purchaseDate'">
            {{ formatDate(record.purchaseDate) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ record.statusName || statusMap[record.status]?.text }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 'IDLE'" @click="openReceive(record)"
                >领用</a
              >
              <Popconfirm
                v-if="record.status === 'IN_USE'"
                title="确定归还此资产？"
                @confirm="handleReturn(record.id)"
              >
                <a>归还</a>
              </Popconfirm>
              <Popconfirm
                v-if="record.status === 'IDLE'"
                title="确定报废此资产？"
                @confirm="handleScrap(record.id)"
              >
                <a style="color: #ff4d4f">报废</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新建/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑资产' : '添加资产'"
      width="600px"
      @ok="handleSave"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="资产名称" required>
          <Input
            v-model:value="formData.name"
            placeholder="如：ThinkPad笔记本"
          />
        </FormItem>
        <FormItem label="资产类型" required>
          <Select
            v-model:value="formData.category"
            :options="categoryOptions"
          />
        </FormItem>
        <FormItem label="品牌">
          <Input v-model:value="formData.brand" placeholder="品牌" />
        </FormItem>
        <FormItem label="型号">
          <Input v-model:value="formData.model" placeholder="型号" />
        </FormItem>
        <FormItem label="规格">
          <Input
            v-model:value="formData.specification"
            placeholder="规格参数"
          />
        </FormItem>
        <FormItem label="序列号">
          <Input
            v-model:value="formData.serialNumber"
            placeholder="序列号/SN"
          />
        </FormItem>
        <FormItem label="购入日期">
          <DatePicker
            v-model:value="formData.purchaseDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="购入价格">
          <InputNumber
            v-model:value="formData.purchasePrice"
            :min="0"
            prefix="¥"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="供应商">
          <Input v-model:value="formData.supplier" placeholder="供应商名称" />
        </FormItem>
        <FormItem label="保修到期">
          <DatePicker
            v-model:value="formData.warrantyExpireDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="存放位置">
          <Input v-model:value="formData.location" placeholder="存放位置" />
        </FormItem>
        <FormItem label="备注">
          <Textarea v-model:value="formData.remarks" :rows="2" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="资产详情"
      width="700px"
      :footer="null"
    >
      <Descriptions v-if="currentAsset" :column="2" bordered size="small">
        <DescriptionsItem label="资产编号">
          {{ currentAsset.assetNo }}
        </DescriptionsItem>
        <DescriptionsItem label="资产名称">
          {{ currentAsset.name }}
        </DescriptionsItem>
        <DescriptionsItem label="资产类型">
          {{ currentAsset.categoryName }}
        </DescriptionsItem>
        <DescriptionsItem label="品牌/型号">
          {{ currentAsset.brand }} {{ currentAsset.model }}
        </DescriptionsItem>
        <DescriptionsItem label="序列号">
          {{ currentAsset.serialNumber || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentAsset.status]?.color">
            {{ statusMap[currentAsset.status]?.text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="购入日期">
          {{ formatDate(currentAsset.purchaseDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="购入价格">
          {{ formatMoney(currentAsset.purchasePrice) }}
        </DescriptionsItem>
        <DescriptionsItem label="使用人">
          {{ currentAsset.currentUserName || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="存放位置">
          {{ currentAsset.location || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="保修到期">
          {{ formatDate(currentAsset.warrantyExpireDate) }}
        </DescriptionsItem>
        <DescriptionsItem label="保修状态">
          <Tag :color="currentAsset.inWarranty ? 'success' : 'default'">
            {{ currentAsset.inWarranty ? '保修中' : '已过保' }}
          </Tag>
        </DescriptionsItem>
      </Descriptions>
      <div v-if="assetRecords.length > 0" style="margin-top: 16px">
        <h4>操作记录</h4>
        <Timeline>
          <Timeline.Item v-for="r in assetRecords" :key="r.id">
            <p>
              <strong>{{ r.recordTypeName }}</strong> - {{ r.operatorName }}
            </p>
            <p v-if="r.toUserName">领用人: {{ r.toUserName }}</p>
            <p v-if="r.reason">原因: {{ r.reason }}</p>
            <p style="font-size: 12px; color: #999">
              {{ formatDate(r.operateDate) }}
            </p>
          </Timeline.Item>
        </Timeline>
      </div>
    </Modal>

    <!-- 领用弹窗 -->
    <Modal v-model:open="receiveVisible" title="资产领用" @ok="handleReceive">
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="领用人" required>
          <UserTreeSelect
            v-model:value="receiveForm.userId"
            placeholder="选择领用人（按部门筛选）"
          />
        </FormItem>
        <FormItem label="预计归还">
          <DatePicker
            v-model:value="receiveForm.expectedReturnDate"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="领用原因">
          <Textarea
            v-model:value="receiveForm.reason"
            :rows="2"
            placeholder="领用原因"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
