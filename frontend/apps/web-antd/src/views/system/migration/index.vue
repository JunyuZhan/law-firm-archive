<script setup lang="ts">
import type { ColumnType } from 'ant-design-vue/es/table';

import type { MigrationDTO } from '#/api/system/types';

import { h, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Eye, PlayCircleOutlined, RotateCw } from '@vben/icons';

import {
  Alert,
  Button,
  Card,
  Descriptions,
  DescriptionsItem,
  message,
  Modal,
  Space,
  Table,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';

import {
  executeMigration,
  getMigrationDetail,
  getMigrationList,
  scanMigrationScripts,
} from '#/api/system';

defineOptions({ name: 'Migration' });

// ==================== 状态定义 ====================

const loading = ref(false);
const scripts = ref<MigrationDTO[]>([]);
const records = ref<MigrationDTO[]>([]);
const detailVisible = ref(false);
const currentDetail = ref<MigrationDTO | null>(null);
const activeTab = ref('scripts');

// ==================== 表格列 ====================

const scriptColumns: ColumnType[] = [
  { title: '版本号', dataIndex: 'version', key: 'version', width: 120 },
  {
    title: '脚本名称',
    dataIndex: 'scriptName',
    key: 'scriptName',
    ellipsis: true,
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt', width: 180 },
  {
    title: '耗时(ms)',
    dataIndex: 'executionTimeMs',
    key: 'executionTimeMs',
    width: 100,
  },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

const recordColumns: ColumnType[] = [
  {
    title: '迁移编号',
    dataIndex: 'migrationNo',
    key: 'migrationNo',
    width: 180,
  },
  { title: '版本号', dataIndex: 'version', key: 'version', width: 120 },
  {
    title: '脚本名称',
    dataIndex: 'scriptName',
    key: 'scriptName',
    ellipsis: true,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
  },
  { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt', width: 180 },
  {
    title: '耗时(ms)',
    dataIndex: 'executionTimeMs',
    key: 'executionTimeMs',
    width: 100,
  },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
];

// ==================== 方法 ====================

// 扫描迁移脚本
async function handleScan() {
  loading.value = true;
  try {
    const res = await scanMigrationScripts();
    scripts.value = res;
    message.success(`扫描完成，共发现 ${res.length} 个迁移脚本`);
  } catch (error: any) {
    message.error(error.message || '扫描失败');
  } finally {
    loading.value = false;
  }
}

// 加载迁移记录
async function loadRecords() {
  loading.value = true;
  try {
    const res = await getMigrationList({ pageNum: 1, pageSize: 100 });
    records.value = res.list || [];
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 执行迁移
async function handleExecute(version: string) {
  Modal.confirm({
    title: '确认执行迁移',
    content: h('div', [
      h(
        'p',
        { style: 'margin-bottom: 12px;' },
        `确定要执行版本 ${version} 的迁移脚本吗？`,
      ),
      h('Alert', {
        type: 'warning',
        showIcon: true,
        style: 'margin-top: 12px;',
        message: '重要提示',
        description:
          '执行迁移前建议先开启维护模式，以避免用户在升级过程中访问系统。迁移完成后可关闭维护模式。',
      }),
      h(
        'p',
        { style: 'margin-top: 12px; color: #666;' },
        '执行前请确保已备份数据库。',
      ),
    ]),
    okText: '确认执行',
    cancelText: '取消',
    width: 500,
    onOk: async () => {
      loading.value = true;
      try {
        await executeMigration(version);
        message.success(
          '迁移执行成功！请检查系统是否正常运行，确认无误后可关闭维护模式。',
        );
        await handleScan();
        await loadRecords();
      } catch (error: any) {
        message.error(error.message || '执行失败');
      } finally {
        loading.value = false;
      }
    },
  });
}

// 查看详情
async function handleDetail(record: MigrationDTO) {
  try {
    if (record.id) {
      const res = await getMigrationDetail(record.id);
      currentDetail.value = res;
    } else {
      currentDetail.value = record;
    }
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 获取状态标签颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'default',
    SUCCESS: 'green',
    FAILED: 'red',
    ROLLED_BACK: 'orange',
  };
  return colorMap[status] || 'default';
}

// 获取状态文本
function getStatusText(status: string) {
  const textMap: Record<string, string> = {
    PENDING: '待执行',
    SUCCESS: '成功',
    FAILED: '失败',
    ROLLED_BACK: '已回滚',
  };
  return textMap[status] || status;
}

onMounted(() => {
  handleScan();
  loadRecords();
});
</script>

<template>
  <Page title="数据库迁移管理" description="管理数据库迁移脚本的执行和记录">
    <Tabs v-model:active-key="activeTab">
      <!-- 迁移脚本 -->
      <TabPane key="scripts" tab="迁移脚本">
        <Card :bordered="false">
          <template #extra>
            <Button :loading="loading" @click="handleScan">
              <RotateCw class="mr-1 size-4" /> 重新扫描
            </Button>
          </template>

          <Alert
            v-if="scripts.length === 0"
            type="info"
            message="未发现迁移脚本"
            description="请将迁移脚本文件（.sql）放置在 scripts/migration 目录下"
            class="mb-4"
          />

          <Table
            :columns="scriptColumns"
            :data-source="scripts"
            :loading="loading"
            row-key="version"
            :pagination="{ pageSize: 20 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button
                    v-if="
                      record.status === 'PENDING' || record.status === 'FAILED'
                    "
                    type="primary"
                    size="small"
                    @click="handleExecute(record.version)"
                  >
                    <PlayCircleOutlined class="mr-1 size-4" /> 执行
                  </Button>
                  <Button
                    size="small"
                    @click="handleDetail(record as MigrationDTO)"
                  >
                    <Eye class="mr-1 size-4" /> 详情
                  </Button>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </TabPane>

      <!-- 执行记录 -->
      <TabPane key="records" tab="执行记录">
        <Card :bordered="false">
          <Table
            :columns="recordColumns"
            :data-source="records"
            :loading="loading"
            row-key="id"
            :pagination="{ pageSize: 20 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <Tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Button
                  size="small"
                  @click="handleDetail(record as MigrationDTO)"
                >
                  <Eye class="mr-1 size-4" /> 详情
                </Button>
              </template>
            </template>
          </Table>
        </Card>
      </TabPane>
    </Tabs>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="迁移详情"
      width="800px"
      :footer="null"
    >
      <Descriptions v-if="currentDetail" :column="2" bordered>
        <DescriptionsItem label="迁移编号">
          {{ currentDetail.migrationNo }}
        </DescriptionsItem>
        <DescriptionsItem label="版本号">
          {{ currentDetail.version }}
        </DescriptionsItem>
        <DescriptionsItem label="脚本名称" :span="2">
          {{ currentDetail.scriptName }}
        </DescriptionsItem>
        <DescriptionsItem label="脚本路径" :span="2">
          {{ currentDetail.scriptPath }}
        </DescriptionsItem>
        <DescriptionsItem label="描述" :span="2">
          {{ currentDetail.description || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="getStatusColor(currentDetail.status)">
            {{ getStatusText(currentDetail.status) }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="执行时间">
          {{ currentDetail.executedAt || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="执行耗时">
          {{
            currentDetail.executionTimeMs
              ? `${currentDetail.executionTimeMs}ms`
              : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="错误信息" :span="2">
          <span v-if="currentDetail.errorMessage" style="color: red">
            {{ currentDetail.errorMessage }}
          </span>
          <span v-else>-</span>
        </DescriptionsItem>
      </Descriptions>
    </Modal>
  </Page>
</template>
