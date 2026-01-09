<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Row,
  Col,
  Tag,
  Tabs,
  Textarea,
  Alert,
  Checkbox,
} from 'ant-design-vue';
import {
  getArchiveList,
  applyMigrateArchive,
  approveMigrateArchive,
} from '#/api/archive';
import type { ArchiveDTO, ArchiveQuery } from '#/api/archive/types';

defineOptions({ name: 'ArchiveMigrate' });

// 状态
const loading = ref(false);
const dataSource = ref<ArchiveDTO[]>([]);
const total = ref(0);
const applyModalVisible = ref(false);
const approveModalVisible = ref(false);
const activeTab = ref('stored');
const currentArchive = ref<ArchiveDTO | null>(null);

// 申请迁移表单
const migrateFormData = reactive({
  reason: '',
  migrateTarget: '',
});

// 审批表单
const approveFormData = reactive({
  approved: true,
  comment: '',
  deleteFiles: false,
});

// 查询参数
const queryParams = reactive<ArchiveQuery>({
  pageNum: 1,
  pageSize: 10,
  archiveName: undefined,
  status: undefined,
});

// 表格列
const columns = [
  { title: '档案编号', dataIndex: 'archiveNo', key: 'archiveNo', width: 130 },
  { title: '档案名称', dataIndex: 'archiveName', key: 'archiveName', width: 200 },
  { title: '项目名称', dataIndex: 'matterName', key: 'matterName', width: 150 },
  { title: '保管期限', dataIndex: 'retentionPeriodName', key: 'retentionPeriodName', width: 100 },
  { title: '到期日期', dataIndex: 'retentionExpireDate', key: 'retentionExpireDate', width: 110 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '迁移目标', dataIndex: 'migrateTarget', key: 'migrateTarget', width: 150 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 迁移目标选项
const migrateTargetOptions = [
  { label: '市档案馆', value: '市档案馆' },
  { label: '区档案馆', value: '区档案馆' },
  { label: '第三方档案管理系统', value: '第三方档案管理系统' },
  { label: '其他', value: '其他' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    // 根据tab设置状态筛选
    if (activeTab.value === 'stored') {
      queryParams.status = 'STORED';
    } else if (activeTab.value === 'pending') {
      queryParams.status = 'PENDING_MIGRATE';
    } else if (activeTab.value === 'migrated') {
      queryParams.status = 'MIGRATED';
    } else {
      queryParams.status = undefined;
    }
    
    const res = await getArchiveList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    message.error(error.message || '加载档案列表失败');
  } finally {
    loading.value = false;
  }
}

// Tab切换
function handleTabChange(key: string | number) {
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
  queryParams.archiveName = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// 申请迁移
function handleApplyMigrate(record: ArchiveDTO) {
  currentArchive.value = record;
  migrateFormData.reason = '';
  migrateFormData.migrateTarget = '';
  applyModalVisible.value = true;
}

// 保存迁移申请
async function handleApplySave() {
  try {
    if (!migrateFormData.reason) {
      message.error('请输入迁移原因');
      return;
    }
    if (!migrateFormData.migrateTarget) {
      message.error('请选择迁移目标');
      return;
    }
    if (currentArchive.value) {
      await applyMigrateArchive(currentArchive.value.id, {
        reason: migrateFormData.reason,
        migrateTarget: migrateFormData.migrateTarget,
      });
      message.success('迁移申请已提交');
      applyModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '申请失败');
  }
}

// 审批迁移
function handleApproveMigrate(record: ArchiveDTO) {
  currentArchive.value = record;
  approveFormData.approved = true;
  approveFormData.comment = '';
  approveFormData.deleteFiles = false;
  approveModalVisible.value = true;
}

// 保存审批
async function handleApproveSave() {
  try {
    if (currentArchive.value) {
      await approveMigrateArchive(currentArchive.value.id, {
        approved: approveFormData.approved,
        comment: approveFormData.comment,
        deleteFiles: approveFormData.deleteFiles,
      });
      message.success(approveFormData.approved ? '迁移审批通过' : '已拒绝迁移');
      approveModalVisible.value = false;
      fetchData();
    }
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    STORED: 'green',
    BORROWED: 'gold',
    PENDING_MIGRATE: 'purple',
    MIGRATED: 'default',
  };
  return colorMap[status] || 'default';
}

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="档案迁移" description="将档案迁移到外部档案管理系统">
    <Alert
      message="档案迁移须知"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #description>
        <p>档案迁移是将本系统中的档案转移到外部档案管理系统（如市档案馆、区档案馆等）的过程。</p>
        <p>迁移后，可选择删除本系统中的实体文件以节省存储空间，但档案信息将保留用于查询。</p>
      </template>
    </Alert>
    
    <Card>
      <!-- Tab切换 -->
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <Tabs.TabPane key="stored" tab="可迁移档案" />
        <Tabs.TabPane key="pending" tab="待审批" />
        <Tabs.TabPane key="migrated" tab="已迁移" />
        <Tabs.TabPane key="all" tab="全部" />
      </Tabs>

      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="8" :lg="6">
            <Input
              v-model:value="queryParams.archiveName"
              placeholder="档案名称"
              allowClear
            />
          </Col>
          <Col :xs="24" :sm="12" :md="16" :lg="18">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
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
          total: total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, size) => {
            queryParams.pageNum = page;
            queryParams.pageSize = size;
            fetchData();
          },
        }"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag :color="getStatusColor((record as ArchiveDTO).status || '')">
              {{ (record as ArchiveDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'migrateTarget'">
            <span v-if="(record as ArchiveDTO).migrateTarget">
              {{ (record as ArchiveDTO).migrateTarget }}
              <Tag v-if="(record as ArchiveDTO).filesDeleted" color="orange" size="small">文件已删除</Tag>
            </span>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <!-- 已入库：可申请迁移 -->
              <template v-if="(record as ArchiveDTO).status === 'STORED'">
                <a @click="handleApplyMigrate(record as ArchiveDTO)">申请迁移</a>
              </template>
              <!-- 待迁移审批：可审批 -->
              <template v-if="(record as ArchiveDTO).status === 'PENDING_MIGRATE'">
                <a @click="handleApproveMigrate(record as ArchiveDTO)">审批</a>
              </template>
              <!-- 已迁移：查看详情 -->
              <template v-if="(record as ArchiveDTO).status === 'MIGRATED'">
                <span style="color: #999">已迁移至 {{ (record as ArchiveDTO).migrateTarget }}</span>
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 申请迁移弹窗 -->
    <Modal
      v-model:open="applyModalVisible"
      title="申请迁移档案"
      width="600px"
      @ok="handleApplySave"
    >
      <div style="margin-bottom: 16px">
        <p><strong>档案名称：</strong>{{ currentArchive?.archiveName }}</p>
        <p><strong>档案编号：</strong>{{ currentArchive?.archiveNo }}</p>
        <p><strong>项目名称：</strong>{{ currentArchive?.matterName }}</p>
      </div>
      <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <FormItem label="迁移目标" :rules="[{ required: true, message: '请选择迁移目标' }]">
          <Select 
            v-model:value="migrateFormData.migrateTarget" 
            placeholder="请选择迁移目标"
            :options="migrateTargetOptions"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="迁移原因" :rules="[{ required: true, message: '请输入迁移原因' }]">
          <Textarea v-model:value="migrateFormData.reason" :rows="4" placeholder="请输入迁移原因（如：保管期限到期、存储空间不足等）" />
      </FormItem>
      </Form>
    </Modal>

    <!-- 审批迁移弹窗 -->
    <Modal
      v-model:open="approveModalVisible"
      title="审批迁移申请"
      width="600px"
      @ok="handleApproveSave"
    >
      <div style="margin-bottom: 16px">
        <p><strong>档案名称：</strong>{{ currentArchive?.archiveName }}</p>
        <p><strong>档案编号：</strong>{{ currentArchive?.archiveNo }}</p>
        <p><strong>迁移目标：</strong>{{ currentArchive?.migrateTarget }}</p>
        <p><strong>迁移原因：</strong>{{ currentArchive?.migrateReason }}</p>
      </div>
      <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
      <FormItem label="审批结果">
          <Select :value="approveFormData.approved ? 'true' : 'false'" @change="(v: any) => approveFormData.approved = v === 'true'" style="width: 100%">
            <Select.Option value="true">通过</Select.Option>
            <Select.Option value="false">拒绝</Select.Option>
        </Select>
      </FormItem>
      <FormItem label="审批意见">
          <Textarea v-model:value="approveFormData.comment" :rows="3" placeholder="请输入审批意见" />
        </FormItem>
        <FormItem v-if="approveFormData.approved" label="删除文件">
          <Checkbox v-model:checked="approveFormData.deleteFiles">
            迁移后删除本系统中的实体文件（保留档案信息）
          </Checkbox>
      </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
