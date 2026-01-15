<script setup lang="ts">
import type {
  ArchiveBorrowDTO,
  CreateBorrowCommand,
  ReturnArchiveCommand,
} from '#/api/archive/borrow';
import type { ArchiveDTO } from '#/api/archive/types';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { useResponsive } from '#/hooks/useResponsive';

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
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';

import { getArchiveList } from '#/api/archive';
import {
  approveBorrow,
  confirmBorrow,
  createBorrow,
  getBorrowList,
  getOverdueBorrows,
  rejectBorrow,
  returnArchive,
} from '#/api/archive/borrow';

defineOptions({ name: 'ArchiveBorrow' });

// 响应式布局
const { isMobile } = useResponsive();

// 状态
const loading = ref(false);
const dataSource = ref<ArchiveBorrowDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const returnModalVisible = ref(false);
const formRef = ref();
const activeTab = ref('borrowing');
const archives = ref<ArchiveDTO[]>([]);
const currentBorrow = ref<ArchiveBorrowDTO | null>(null);

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  archiveId: undefined as number | undefined,
  status: undefined as string | undefined,
});

// 表单数据
const formData = reactive<CreateBorrowCommand>({
  archiveId: 0,
  expectedReturnDate: '',
  reason: '',
});

// 归还表单数据
const returnFormData = reactive<ReturnArchiveCommand & { returnDate?: string }>(
  {
    borrowId: 0,
    returnDate: '',
    condition: '',
  },
);

// 表格列（响应式）
const columns = computed(() => {
  const baseColumns = [
    { title: '借阅编号', dataIndex: 'borrowNo', key: 'borrowNo', width: 130 },
    {
      title: '档案名称',
      dataIndex: 'archiveName',
      key: 'archiveName',
      width: isMobile.value ? 120 : 200,
      mobileShow: true,
    },
    { title: '档案编号', dataIndex: 'archiveNo', key: 'archiveNo', width: 130 },
    {
      title: '借阅人',
      dataIndex: 'borrowerName',
      key: 'borrowerName',
      width: 100,
      mobileShow: true,
    },
    { title: '借阅日期', dataIndex: 'borrowDate', key: 'borrowDate', width: 120 },
    {
      title: '预计归还',
      dataIndex: 'expectedReturnDate',
      key: 'expectedReturnDate',
      width: 120,
    },
    {
      title: '实际归还',
      dataIndex: 'actualReturnDate',
      key: 'actualReturnDate',
      width: 120,
    },
    { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100, mobileShow: true },
    { title: '操作', key: 'action', width: isMobile.value ? 100 : 200, fixed: 'right' as const, mobileShow: true },
  ];
  
  if (isMobile.value) {
    return baseColumns.filter(col => col.mobileShow === true);
  }
  return baseColumns;
});

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    // 根据tab设置状态筛选
    switch (activeTab.value) {
      case 'borrowing': {
        queryParams.status = 'BORROWING';

        break;
      }
      case 'overdue': {
        // 逾期未还 - 使用特殊接口
        const data = await getOverdueBorrows();
        dataSource.value = data;
        total.value = data.length;
        loading.value = false;
        return;
      }
      case 'returned': {
        queryParams.status = 'RETURNED';

        break;
      }
      default: {
        queryParams.status = undefined;
      }
    }

    const res = await getBorrowList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    message.error(error.message || '加载借阅列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载档案列表
async function loadArchives() {
  try {
    const res = await getArchiveList({
      pageNum: 1,
      pageSize: 1000,
      status: 'ARCHIVED',
    });
    archives.value = res.list;
  } catch (error: any) {
    console.error('加载档案列表失败:', error);
  }
}

// Tab切换
function handleTabChange(key: number | string) {
  activeTab.value = String(key);
  queryParams.pageNum = 1;
  fetchData();
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.archiveId = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// 新增申请
function handleAdd() {
  Object.assign(formData, {
    archiveId: 0,
    expectedReturnDate: '',
    reason: '',
  });
  modalVisible.value = true;
}

// 保存申请
async function handleSave() {
  try {
    await formRef.value?.validate();

    if (!formData.archiveId) {
      message.error('请选择档案');
      return;
    }

    await createBorrow(formData);
    message.success('申请提交成功');
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    if (error?.errorFields) {
      return;
    }
    message.error(error.message || '提交失败');
  }
}

// 审批通过
async function handleApprove(record: ArchiveBorrowDTO) {
  try {
    await approveBorrow(record.id);
    message.success('审批通过');
    fetchData();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 审批拒绝
function handleReject(record: ArchiveBorrowDTO) {
  Modal.confirm({
    title: '确认拒绝',
    content: '请输入拒绝原因',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const reason = prompt('请输入拒绝原因:');
      if (reason) {
        try {
          await rejectBorrow(record.id, reason);
          message.success('已拒绝');
          fetchData();
        } catch (error: any) {
          message.error(error.message || '操作失败');
        }
      }
    },
  });
}

// 确认借出
async function handleConfirm(record: ArchiveBorrowDTO) {
  try {
    await confirmBorrow(record.id);
    message.success('确认借出成功');
    fetchData();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 归还档案
function handleReturn(record: ArchiveBorrowDTO) {
  currentBorrow.value = record;
  returnFormData.borrowId = record.id;
  returnFormData.returnDate = '';
  returnFormData.condition = '';
  returnModalVisible.value = true;
}

// 保存归还
async function handleReturnSave() {
  try {
    if (!returnFormData.returnDate) {
      message.error('请选择归还日期');
      return;
    }
    await returnArchive(returnFormData as ReturnArchiveCommand);
    message.success('归还成功');
    returnModalVisible.value = false;
    fetchData();
  } catch (error: any) {
    message.error(error.message || '归还失败');
  }
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'blue',
    BORROWING: 'green',
    RETURNED: 'default',
    REJECTED: 'red',
    OVERDUE: 'red',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  fetchData();
  loadArchives();
});
</script>

<template>
  <Page title="档案借阅" description="管理档案借阅记录">
    <Card>
      <!-- Tab切换 -->
      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="borrowing" tab="借阅中" />
        <Tabs.TabPane key="returned" tab="已归还" />
        <Tabs.TabPane key="overdue" tab="逾期未还" />
        <Tabs.TabPane key="all" tab="全部" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <Select
              v-model:value="queryParams.archiveId"
              placeholder="选择档案"
              allow-clear
              show-search
              style="width: 100%"
              :filter-option="
                (input, option) =>
                  (option?.label || '')
                    .toLowerCase()
                    .includes(input.toLowerCase())
              "
              :options="archives.map((a) => ({ label: a.name, value: a.id }))"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="16" :lg="18">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">申请借阅</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 表格 -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => {
            queryParams.pageNum = page;
            queryParams.pageSize = size;
            fetchData();
          },
        }"
        row-key="id"
        :scroll="{ x: 1300 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as ArchiveBorrowDTO).status)">
              {{ (record as ArchiveBorrowDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <template
                v-if="(record as ArchiveBorrowDTO).status === 'PENDING'"
              >
                <a @click="handleApprove(record as ArchiveBorrowDTO)">通过</a>
                <a
                  @click="handleReject(record as ArchiveBorrowDTO)"
                  style="color: red"
                  >拒绝</a
                >
              </template>
              <template
                v-if="(record as ArchiveBorrowDTO).status === 'APPROVED'"
              >
                <a @click="handleConfirm(record as ArchiveBorrowDTO)"
                  >确认借出</a
                >
              </template>
              <template
                v-if="(record as ArchiveBorrowDTO).status === 'BORROWING'"
              >
                <a @click="handleReturn(record as ArchiveBorrowDTO)">归还</a>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 申请借阅弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="申请借阅"
      width="600px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem
          label="选择档案"
          name="archiveId"
          :rules="[{ required: true, message: '请选择档案' }]"
        >
          <Select
            v-model:value="formData.archiveId"
            placeholder="请选择档案"
            show-search
            style="width: 100%"
            :filter-option="
              (input, option) =>
                (option?.label || '')
                  .toLowerCase()
                  .includes(input.toLowerCase())
            "
            :options="archives.map((a) => ({ label: a.name, value: a.id }))"
          />
        </FormItem>
        <FormItem
          label="预计归还日期"
          name="expectedReturnDate"
          :rules="[{ required: true, message: '请选择预计归还日期' }]"
        >
          <DatePicker
            v-model:value="formData.expectedReturnDate"
            placeholder="请选择预计归还日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="借阅事由" name="reason">
          <Textarea
            v-model:value="formData.reason"
            :rows="3"
            placeholder="请输入借阅事由"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 归还档案弹窗 -->
    <Modal
      v-model:open="returnModalVisible"
      title="归还档案"
      width="500px"
      @ok="handleReturnSave"
    >
      <Form
        :model="returnFormData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="档案名称">
          <Input :value="currentBorrow?.archiveName" disabled />
        </FormItem>
        <FormItem
          label="归还日期"
          :rules="[{ required: true, message: '请选择归还日期' }]"
        >
          <DatePicker
            v-model:value="returnFormData.returnDate"
            placeholder="请选择归还日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="档案状况">
          <Textarea
            v-model:value="returnFormData.condition"
            :rows="3"
            placeholder="请输入档案状况（可选）"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
