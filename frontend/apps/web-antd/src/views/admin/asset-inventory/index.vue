<script setup lang="ts">
import type {
  AssetInventoryDetailDTO,
  AssetInventoryDTO,
  CreateAssetInventoryCommand,
  UpdateInventoryDetailRequest,
} from '#/api/admin/asset-inventory';
import type { DepartmentDTO } from '#/api/system/types';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Textarea,
  TreeSelect,
} from 'ant-design-vue';

import { getAssetList, getIdleAssets } from '#/api/admin/asset';
import {
  completeAssetInventory,
  createAssetInventory,
  getAssetInventoryDetail,
  getInProgressInventories,
  updateInventoryDetail,
} from '#/api/admin/asset-inventory';
import { getDepartmentTreePublic } from '#/api/system';

defineOptions({ name: 'AssetInventoryManagement' });

// 盘点类型选项
const inventoryTypeOptions = [
  { label: '全盘', value: 'FULL' },
  { label: '抽盘', value: 'PARTIAL' },
];

// 资产分类选项
const assetCategoryOptions = [
  { label: '全部分类', value: '' },
  { label: '办公设备', value: 'OFFICE' },
  { label: 'IT设备', value: 'IT' },
  { label: '家具', value: 'FURNITURE' },
  { label: '车辆', value: 'VEHICLE' },
  { label: '其他', value: 'OTHER' },
];

// 状态标签颜色
const statusColorMap: Record<string, string> = {
  IN_PROGRESS: 'orange',
  COMPLETED: 'green',
};

// 状态文本
const statusTextMap: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
};

// 部门树数据
const departmentTree = ref<DepartmentDTO[]>([]);

// 转换部门树为 TreeSelect 需要的格式
function convertToTreeData(departments: DepartmentDTO[]): any[] {
  return departments.map((dept) => ({
    value: dept.id,
    title: dept.name,
    children: dept.children ? convertToTreeData(dept.children) : [],
  }));
}

const departmentTreeData = computed(() =>
  convertToTreeData(departmentTree.value),
);

// 筛选条件
const filterCategory = ref<string>('');

// 表格列
const columns = [
  {
    title: '盘点编号',
    dataIndex: 'inventoryNo',
    key: 'inventoryNo',
    width: 120,
  },
  {
    title: '盘点日期',
    dataIndex: 'inventoryDate',
    key: 'inventoryDate',
    width: 120,
  },
  {
    title: '盘点类型',
    dataIndex: 'inventoryTypeName',
    key: 'inventoryType',
    width: 100,
  },
  {
    title: '部门',
    dataIndex: 'departmentName',
    key: 'departmentName',
    width: 120,
  },
  { title: '位置', dataIndex: 'location', key: 'location', width: 120 },
  { title: '总数', dataIndex: 'totalCount', key: 'totalCount', width: 80 },
  { title: '实盘数', dataIndex: 'actualCount', key: 'actualCount', width: 80 },
  { title: '盘盈', dataIndex: 'surplusCount', key: 'surplusCount', width: 80 },
  {
    title: '盘亏',
    dataIndex: 'shortageCount',
    key: 'shortageCount',
    width: 80,
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 明细表格列
const detailColumns = [
  { title: '资产编号', dataIndex: 'assetNo', key: 'assetNo', width: 120 },
  { title: '资产名称', dataIndex: 'assetName', key: 'assetName', width: 150 },
  {
    title: '预期状态',
    dataIndex: 'expectedStatus',
    key: 'expectedStatus',
    width: 100,
  },
  {
    title: '实际状态',
    dataIndex: 'actualStatus',
    key: 'actualStatus',
    width: 100,
  },
  {
    title: '预期位置',
    dataIndex: 'expectedLocation',
    key: 'expectedLocation',
    width: 120,
  },
  {
    title: '实际位置',
    dataIndex: 'actualLocation',
    key: 'actualLocation',
    width: 120,
  },
  {
    title: '预期使用人',
    dataIndex: 'expectedUserName',
    key: 'expectedUserName',
    width: 100,
  },
  {
    title: '实际使用人',
    dataIndex: 'actualUserName',
    key: 'actualUserName',
    width: 100,
  },
  {
    title: '差异类型',
    dataIndex: 'discrepancyTypeName',
    key: 'discrepancyType',
    width: 100,
  },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
];

// 表格数据
const tableData = ref<AssetInventoryDTO[]>([]);
const loading = ref(false);

// 新增弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const inventoryForm = reactive<CreateAssetInventoryCommand>({
  inventoryDate: '',
  inventoryType: '',
  departmentId: undefined,
  location: '',
  remark: '',
  assetIds: [],
});

// 详情弹窗
const detailModalVisible = ref(false);
const detailData = ref<AssetInventoryDTO | null>(null);
const detailTableData = ref<AssetInventoryDetailDTO[]>([]);

// 更新明细弹窗
const updateDetailModalVisible = ref(false);
const updateDetailLoading = ref(false);
const currentDetail = ref<AssetInventoryDetailDTO | null>(null);
const detailUpdateForm = reactive<UpdateInventoryDetailRequest>({
  actualStatus: '',
  actualLocation: '',
  actualUserId: undefined,
  discrepancyDesc: '',
});

// 资产列表（用于选择）
const assetList = ref<any[]>([]);
const allAssets = ref<any[]>([]); // 全部资产（用于全盘时按条件筛选）

// 根据筛选条件过滤的资产列表
const filteredAssets = computed(() => {
  let list =
    inventoryForm.inventoryType === 'PARTIAL'
      ? assetList.value
      : allAssets.value;

  // 按分类筛选
  if (filterCategory.value) {
    list = list.filter((a) => a.category === filterCategory.value);
  }

  // 按部门筛选
  if (inventoryForm.departmentId) {
    list = list.filter((a) => a.departmentId === inventoryForm.departmentId);
  }

  // 按位置筛选
  if (inventoryForm.location) {
    list = list.filter((a) => a.location?.includes(inventoryForm.location));
  }

  return list;
});

// 预计盘点资产数量
const expectedAssetCount = computed(() => {
  if (inventoryForm.inventoryType === 'PARTIAL') {
    return inventoryForm.assetIds?.length || 0;
  }
  return filteredAssets.value.length;
});

// 获取盘点列表
async function fetchData() {
  loading.value = true;
  try {
    const data = await getInProgressInventories();
    tableData.value = data || [];
  } catch (error) {
    console.error('获取盘点列表失败:', error);
    message.error('获取盘点列表失败');
  } finally {
    loading.value = false;
  }
}

// 获取闲置资产列表
async function fetchAssets() {
  try {
    const res = await getIdleAssets();
    assetList.value = res || [];
  } catch (error) {
    console.error('获取资产列表失败:', error);
    assetList.value = [];
  }
}

// 获取全部资产列表
async function fetchAllAssets() {
  try {
    const res = await getAssetList({ pageNum: 1, pageSize: 1000 });
    allAssets.value = res?.list || res || [];
  } catch (error) {
    console.error('获取全部资产列表失败:', error);
    allAssets.value = [];
  }
}

// 获取部门树（使用公共接口）
async function fetchDepartmentTree() {
  try {
    const res = await getDepartmentTreePublic();
    departmentTree.value = res || [];
  } catch (error) {
    console.error('获取部门树失败:', error);
    departmentTree.value = [];
  }
}

// 新增盘点
function handleAdd() {
  Object.assign(inventoryForm, {
    inventoryDate: '',
    inventoryType: '',
    departmentId: undefined,
    location: '',
    remark: '',
    assetIds: [],
  });
  filterCategory.value = '';
  modalVisible.value = true;
  fetchAssets();
  fetchAllAssets();
  fetchDepartmentTree();
}

// 提交盘点
async function handleSubmit() {
  if (!inventoryForm.inventoryDate) {
    message.warning('请选择盘点日期');
    return;
  }
  if (!inventoryForm.inventoryType) {
    message.warning('请选择盘点类型');
    return;
  }

  modalLoading.value = true;
  try {
    await createAssetInventory(inventoryForm);
    message.success('创建成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error?.message || '创建失败');
  } finally {
    modalLoading.value = false;
  }
}

// 查看详情
async function handleView(record: Record<string, any>) {
  try {
    const detail = await getAssetInventoryDetail(record.id);
    detailData.value = detail;
    detailTableData.value = detail.details || [];
    detailModalVisible.value = true;
  } catch (error: any) {
    message.error(error?.message || '获取详情失败');
  }
}

// 完成盘点
function handleComplete(record: Record<string, any>) {
  Modal.confirm({
    title: '确认完成',
    content: `确定要完成盘点"${record.inventoryNo || record.id}"吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await completeAssetInventory(record.id);
        message.success('完成成功');
        fetchData();
      } catch (error: any) {
        message.error(error?.message || '操作失败');
      }
    },
  });
}

// 更新明细
function handleUpdateDetail(record: Record<string, any>) {
  currentDetail.value = record as AssetInventoryDetailDTO;
  Object.assign(detailUpdateForm, {
    actualStatus: record.actualStatus || '',
    actualLocation: record.actualLocation || '',
    actualUserId: record.actualUserId,
    discrepancyDesc: record.discrepancyDesc || '',
  });
  updateDetailModalVisible.value = true;
}

// 提交更新明细
async function handleUpdateDetailSubmit() {
  if (!currentDetail.value) return;

  updateDetailLoading.value = true;
  try {
    await updateInventoryDetail(currentDetail.value.id, detailUpdateForm);
    message.success('更新成功');
    updateDetailModalVisible.value = false;
    if (detailData.value) {
      const detail = await getAssetInventoryDetail(detailData.value.id);
      detailData.value = detail;
      detailTableData.value = detail.details || [];
    }
  } catch (error: any) {
    message.error(error?.message || '更新失败');
  } finally {
    updateDetailLoading.value = false;
  }
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="资产盘点" description="资产盘点管理">
    <Card>
      <template #extra>
        <Button type="primary" @click="handleAdd">新建盘点</Button>
      </template>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <Tag :color="statusColorMap[record.status || '']">
              {{ statusTextMap[record.status || ''] || record.status }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <a
                v-if="record.status === 'IN_PROGRESS'"
                @click="handleComplete(record)"
                >完成盘点</a
              >
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增盘点弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="新建资产盘点"
      :confirm-loading="modalLoading"
      width="720px"
      @ok="handleSubmit"
    >
      <Form :model="inventoryForm" layout="vertical">
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="盘点日期" required>
              <DatePicker
                v-model:value="inventoryForm.inventoryDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                placeholder="请选择盘点日期"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="盘点类型" required>
              <Select
                v-model:value="inventoryForm.inventoryType"
                placeholder="请选择盘点类型"
              >
                <Select.Option
                  v-for="item in inventoryTypeOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="盘点部门">
              <TreeSelect
                v-model:value="inventoryForm.departmentId"
                :tree-data="departmentTreeData"
                placeholder="请选择部门（可选）"
                allow-clear
                tree-default-expand-all
                style="width: 100%"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="资产分类">
              <Select
                v-model:value="filterCategory"
                placeholder="请选择资产分类"
                allow-clear
              >
                <Select.Option
                  v-for="item in assetCategoryOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>

        <FormItem label="盘点位置">
          <Input
            v-model:value="inventoryForm.location"
            placeholder="请输入位置（如：3楼办公区）"
          />
        </FormItem>

        <FormItem
          v-if="inventoryForm.inventoryType === 'PARTIAL'"
          label="选择资产"
        >
          <Select
            v-model:value="inventoryForm.assetIds"
            mode="multiple"
            placeholder="请选择要盘点的资产"
            style="width: 100%"
            :max-tag-count="5"
            show-search
            :filter-option="
              (input: string, option: any) =>
                option.label?.toLowerCase().includes(input.toLowerCase())
            "
          >
            <Select.Option
              v-for="asset in filteredAssets"
              :key="asset.id"
              :value="asset.id"
              :label="`${asset.assetNo} - ${asset.name}`"
            >
              <div style="display: flex; justify-content: space-between">
                <span>{{ asset.assetNo }} - {{ asset.name }}</span>
                <span style="font-size: 12px; color: #999">{{
                  asset.location || '未知位置'
                }}</span>
              </div>
            </Select.Option>
          </Select>
        </FormItem>

        <FormItem label="备注">
          <Textarea
            v-model:value="inventoryForm.remark"
            placeholder="请输入盘点说明或备注"
            :rows="3"
          />
        </FormItem>

        <!-- 预计盘点信息 -->
        <div
          style="
            padding: 12px;
            margin-top: 8px;
            background: #f5f5f5;
            border-radius: 4px;
          "
        >
          <div style="font-size: 14px; color: #666">
            <span>预计盘点资产数量：</span>
            <span style="font-weight: 500; color: #1890ff">{{
              expectedAssetCount
            }}</span>
            <span> 件</span>
            <span
              v-if="
                inventoryForm.inventoryType === 'FULL' &&
                (inventoryForm.departmentId ||
                  filterCategory ||
                  inventoryForm.location)
              "
              style="margin-left: 12px; color: #999"
            >
              （已按条件筛选）
            </span>
          </div>
        </div>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailModalVisible"
      title="盘点详情"
      width="1200px"
      :footer="null"
    >
      <div v-if="detailData" style="margin-bottom: 16px; line-height: 2">
        <p><strong>盘点编号:</strong> {{ detailData.inventoryNo || '-' }}</p>
        <p><strong>盘点日期:</strong> {{ detailData.inventoryDate || '-' }}</p>
        <p>
          <strong>盘点类型:</strong> {{ detailData.inventoryTypeName || '-' }}
        </p>
        <p><strong>部门:</strong> {{ detailData.departmentName || '-' }}</p>
        <p><strong>位置:</strong> {{ detailData.location || '-' }}</p>
        <p><strong>总数:</strong> {{ detailData.totalCount || 0 }}</p>
        <p><strong>实盘数:</strong> {{ detailData.actualCount || 0 }}</p>
        <p><strong>盘盈:</strong> {{ detailData.surplusCount || 0 }}</p>
        <p><strong>盘亏:</strong> {{ detailData.shortageCount || 0 }}</p>
        <p><strong>状态:</strong> {{ detailData.statusName || '-' }}</p>
      </div>
      <Table
        :columns="detailColumns"
        :data-source="detailTableData"
        row-key="id"
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a
              v-if="detailData?.status === 'IN_PROGRESS'"
              @click="handleUpdateDetail(record)"
              >更新</a
            >
          </template>
        </template>
      </Table>
    </Modal>

    <!-- 更新明细弹窗 -->
    <Modal
      v-model:open="updateDetailModalVisible"
      title="更新盘点明细"
      :confirm-loading="updateDetailLoading"
      width="600px"
      @ok="handleUpdateDetailSubmit"
    >
      <Form :model="detailUpdateForm" layout="vertical">
        <FormItem label="实际状态">
          <Input
            v-model:value="detailUpdateForm.actualStatus"
            placeholder="请输入实际状态"
          />
        </FormItem>
        <FormItem label="实际位置">
          <Input
            v-model:value="detailUpdateForm.actualLocation"
            placeholder="请输入实际位置"
          />
        </FormItem>
        <FormItem label="差异描述">
          <Textarea
            v-model:value="detailUpdateForm.discrepancyDesc"
            placeholder="请输入差异描述"
            :rows="4"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
