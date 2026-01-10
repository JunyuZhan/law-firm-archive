<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Tag,
  Button,
  Space,
  Card,
  Statistic,
  Row,
  Col,
  Popconfirm,
  Form,
  FormItem,
  Select,
  Textarea,
  Progress,
  Upload,
} from 'ant-design-vue';
import { Plus, DownloadOutlined, DeleteOutlined, UploadOutlined } from '@vben/icons';
import type { VbenFormSchema } from '#/adapter/form';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getBackupList,
  getBackupDetail,
  createBackup,
  restoreBackup,
  deleteBackup,
  downloadBackup,
  importBackup,
} from '#/api/system';
import type { BackupDTO, CreateBackupCommand, RestoreBackupCommand } from '#/api/system/types';
import dayjs from 'dayjs';

defineOptions({ name: 'SystemBackup' });

// ==================== 状态定义 ====================

const createModalVisible = ref(false);
const restoreModalVisible = ref(false);
const createFormRef = ref();
const restoreFormRef = ref();
const createLoading = ref(false);
const restoreLoading = ref(false);
const currentBackup = ref<BackupDTO | null>(null);

// 下载进度相关
const downloadingId = ref<number | null>(null);
const downloadProgress = ref(0);
const downloadModalVisible = ref(false);
const downloadFileName = ref('');

// 恢复进度相关
const restoringId = ref<number | null>(null);
const restoreProgressModalVisible = ref(false);
const restoreProgressInterval = ref<NodeJS.Timeout | null>(null);

// 导入相关
const importModalVisible = ref(false);
const importLoading = ref(false);
const importProgress = ref(0);
const importFormRef = ref();
const importFormData = ref({
  file: null as File | null,
  backupType: 'DATABASE',
  description: '',
});

const createFormData = ref<CreateBackupCommand>({
  backupType: '',
  description: '',
});

const restoreFormData = ref<RestoreBackupCommand>({
  backupId: 0,
  description: '',
});

// 备份类型选项
const backupTypeOptions = [
  { label: '全量备份', value: 'FULL' },
  { label: '增量备份', value: 'INCREMENTAL' },
  { label: '数据库备份', value: 'DATABASE' },
  { label: '文件备份', value: 'FILE' },
];

// 备份状态选项
const statusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '进行中', value: 'IN_PROGRESS' },
];

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'backupType',
    label: '备份类型',
    component: 'Select',
    componentProps: {
      placeholder: '请选择备份类型',
      allowClear: true,
      options: backupTypeOptions,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: statusOptions,
    },
  },
  {
    fieldName: 'startTime',
    label: '开始时间',
    component: 'DatePicker',
    componentProps: {
      placeholder: '开始时间',
      showTime: true,
      format: 'YYYY-MM-DD HH:mm:ss',
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      style: { width: '100%' },
    },
  },
  {
    fieldName: 'endTime',
    label: '结束时间',
    component: 'DatePicker',
    componentProps: {
      placeholder: '结束时间',
      showTime: true,
      format: 'YYYY-MM-DD HH:mm:ss',
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      style: { width: '100%' },
    },
  },
];

// ==================== 表格配置 ====================

const gridColumns: any[] = [
  { title: '备份编号', field: 'backupNo', width: 180 },
  { title: '备份名称', field: 'backupName', width: 200 },
  { title: '备份类型', field: 'backupType', width: 120, slots: { default: 'backupType' } },
  { title: '文件大小', field: 'fileSize', width: 120, slots: { default: 'fileSize' } },
  { title: '状态', field: 'status', width: 100, slots: { default: 'status' } },
  { title: '备份时间', field: 'backupTime', width: 180 },
  { title: '创建人', field: 'createdByName', width: 100 },
  { title: '描述', field: 'description', width: 200, ellipsis: true },
  { title: '操作', field: 'action_btn', width: 200, fixed: 'right', slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: { page: number; pageSize: number } & Record<string, any>) {
  const res = await getBackupList({
    pageNum: params.page,
    pageSize: params.pageSize,
    backupType: params.backupType,
    status: params.status,
    startTime: params.startTime,
    endTime: params.endTime,
  });
  return {
    items: res.list,
    total: res.total,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    showCollapseButton: true,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }: { page: any; form: any }) => {
          return await loadData({
            page: page.currentPage,
            pageSize: page.pageSize,
            ...form,
          });
        },
      },
    },
    pagerConfig: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
    },
  },
});

// ==================== 操作方法 ====================

// 打开创建备份弹窗
function handleCreate() {
  createModalVisible.value = true;
}

// 创建备份
async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate();
    createLoading.value = true;
    await createBackup({
      backupType: createFormData.value.backupType,
      description: createFormData.value.description,
    });
    message.success('备份任务已创建');
    createModalVisible.value = false;
    createFormData.value = { backupType: '', description: '' };
    createFormRef.value?.resetFields();
    gridApi.reload();
  } catch (err: any) {
    if (err?.errorFields) {
      return; // 表单验证错误
    }
    message.error(err.message || '创建备份失败');
  } finally {
    createLoading.value = false;
  }
}

// 打开恢复备份弹窗
function handleRestore(row: BackupDTO) {
  currentBackup.value = row;
  restoreFormData.value = {
    backupId: row.id,
    description: '',
  };
  restoreModalVisible.value = true;
}

// 恢复备份
async function handleRestoreSubmit() {
  if (!currentBackup.value) return;
  
  try {
    await restoreFormRef.value?.validate();
    restoreLoading.value = true;
    
    Modal.confirm({
      title: '确认恢复备份',
      content: `确定要恢复备份 "${currentBackup.value.backupName}" 吗？此操作将覆盖当前数据库数据，请谨慎操作！`,
      okText: '确定',
      cancelText: '取消',
      okType: 'danger',
      async onOk() {
        try {
          await restoreBackup({
            backupId: currentBackup.value!.id,
            description: restoreFormData.value.description,
          });
          
          // 关闭确认弹窗
          restoreModalVisible.value = false;
          restoreFormData.value = { backupId: 0, description: '' };
          restoreFormRef.value?.resetFields();
          restoreLoading.value = false;
          
          // 开始轮询恢复进度
          restoringId.value = currentBackup.value!.id;
          restoreProgressModalVisible.value = true;
          startRestoreProgressPolling(currentBackup.value!.id);
          
          currentBackup.value = null;
        } catch (err: any) {
          message.error(err.message || '恢复备份失败');
          restoreLoading.value = false;
        }
      },
      onCancel() {
        restoreLoading.value = false;
      },
    });
  } catch (err: any) {
    if (err?.errorFields) {
      return; // 表单验证错误
    }
    message.error(err.message || '恢复备份失败');
    restoreLoading.value = false;
  }
}

// 开始轮询恢复进度
function startRestoreProgressPolling(backupId: number) {
  // 清除之前的轮询
  if (restoreProgressInterval.value) {
    clearInterval(restoreProgressInterval.value);
  }
  
  // 立即查询一次
  checkRestoreStatus(backupId);
  
  // 每2秒轮询一次
  restoreProgressInterval.value = setInterval(() => {
    checkRestoreStatus(backupId);
  }, 2000);
}

// 检查恢复状态
async function checkRestoreStatus(backupId: number) {
  try {
    const result = await getBackupDetail(backupId);
    const backup = result.data || result;
    
    if (backup.status === 'SUCCESS') {
      // 恢复成功
      if (restoreProgressInterval.value) {
        clearInterval(restoreProgressInterval.value);
        restoreProgressInterval.value = null;
      }
      restoreProgressModalVisible.value = false;
      restoringId.value = null;
      message.success('备份恢复成功');
      gridApi.reload();
    } else if (backup.status === 'FAILED') {
      // 恢复失败
      if (restoreProgressInterval.value) {
        clearInterval(restoreProgressInterval.value);
        restoreProgressInterval.value = null;
      }
      restoreProgressModalVisible.value = false;
      restoringId.value = null;
      message.error('备份恢复失败：' + (backup.description || '未知错误'));
      gridApi.reload();
    }
    // IN_PROGRESS 状态继续轮询
  } catch (err: any) {
    console.error('查询恢复状态失败:', err);
    // 查询失败时继续轮询，不中断
  }
}

// 取消恢复进度监控
function cancelRestoreProgress() {
  if (restoreProgressInterval.value) {
    clearInterval(restoreProgressInterval.value);
    restoreProgressInterval.value = null;
  }
  restoreProgressModalVisible.value = false;
  restoringId.value = null;
  gridApi.reload();
}

// 下载备份
async function handleDownload(row: BackupDTO) {
  try {
    downloadingId.value = row.id;
    downloadProgress.value = 0;
    downloadFileName.value = row.backupName || `backup_${row.backupNo}.sql`;
    downloadModalVisible.value = true;

    const response = await downloadBackup(row.id, (progress) => {
      downloadProgress.value = progress.percent;
    });
    
    // 检查返回的数据是否是 Blob
    let blob: Blob;
    if (response instanceof Blob) {
      blob = response;
    } else if (response?.data instanceof Blob) {
      blob = response.data;
    } else {
      // 如果返回的是 JSON 错误信息，尝试解析
      if (response?.data && typeof response.data === 'object') {
        const errorData = response.data as any;
        throw new Error(errorData.message || errorData.msg || '下载失败');
      }
      throw new Error('返回的数据格式不正确');
    }
    
    downloadProgress.value = 100;
    
    // 延迟一下让用户看到100%的进度
    await new Promise((resolve) => setTimeout(resolve, 300));
    
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = downloadFileName.value;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    message.success('下载成功');
    downloadModalVisible.value = false;
    downloadingId.value = null;
    downloadProgress.value = 0;
  } catch (err: any) {
    console.error('下载备份失败:', err);
    downloadModalVisible.value = false;
    downloadingId.value = null;
    downloadProgress.value = 0;
    
    // 如果是 Blob 类型的错误响应，尝试解析错误信息
    if (err?.response?.data instanceof Blob) {
      try {
        const text = await err.response.data.text();
        const errorData = JSON.parse(text);
        message.error(errorData.message || errorData.msg || '下载失败');
      } catch {
        message.error('下载失败');
      }
    } else {
      message.error(err.message || err.msg || '下载失败');
    }
  }
}

// 删除备份
async function handleDelete(row: BackupDTO) {
  try {
    await deleteBackup(row.id);
    message.success('删除成功');
    gridApi.reload();
  } catch (err: any) {
    message.error(err.message || '删除失败');
  }
}

// 打开导入弹窗
function handleOpenImport() {
  importFormData.value = {
    file: null,
    backupType: 'DATABASE',
    description: '',
  };
  importProgress.value = 0;
  importModalVisible.value = true;
}

// 处理文件选择
function handleFileChange(info: any) {
  if (info.file) {
    importFormData.value.file = info.file;
  }
}

// 阻止默认上传行为
function beforeUpload(file: File) {
  importFormData.value.file = file;
  return false; // 返回 false 阻止自动上传
}

// 导入备份
async function handleImportSubmit() {
  if (!importFormData.value.file) {
    message.error('请选择要导入的备份文件');
    return;
  }
  
  try {
    importLoading.value = true;
    importProgress.value = 0;
    
    await importBackup(
      importFormData.value.file,
      importFormData.value.backupType,
      importFormData.value.description,
      (progress) => {
        importProgress.value = progress.percent;
      }
    );
    
    importProgress.value = 100;
    message.success('备份文件导入成功');
    importModalVisible.value = false;
    importFormData.value = { file: null, backupType: 'DATABASE', description: '' };
    gridApi.reload();
  } catch (err: any) {
    console.error('导入备份失败:', err);
    message.error(err.message || '导入备份失败');
  } finally {
    importLoading.value = false;
    importProgress.value = 0;
  }
}

// 格式化文件大小
function formatFileSize(bytes: number): string {
  if (!bytes) return '-';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

// 获取备份类型名称
function getBackupTypeName(type: string): string {
  const option = backupTypeOptions.find((opt) => opt.value === type);
  return option?.label || type;
}

// 获取状态颜色
function getStatusColor(status: string): string {
  const colorMap: Record<string, string> = {
    SUCCESS: 'green',
    FAILED: 'red',
    IN_PROGRESS: 'blue',
  };
  return colorMap[status] || 'default';
}

// 获取状态名称
function getStatusName(status: string): string {
  const nameMap: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    IN_PROGRESS: '进行中',
  };
  return nameMap[status] || status;
}

onMounted(() => {
  gridApi.reload();
});

// 组件卸载时清理轮询
onUnmounted(() => {
  if (restoreProgressInterval.value) {
    clearInterval(restoreProgressInterval.value);
    restoreProgressInterval.value = null;
  }
});
</script>

<template>
  <Page title="数据库备份" description="管理系统数据库备份和恢复" auto-content-height>
    <!-- 操作栏 -->
    <Card class="mb-4" :bordered="false">
      <Space>
        <Button type="primary" @click="handleCreate">
          <Plus class="size-4 mr-1" /> 创建备份
        </Button>
        <Button @click="handleOpenImport">
          <UploadOutlined class="size-4 mr-1" /> 导入备份
        </Button>
        <Button @click="gridApi.reload()">刷新</Button>
      </Space>
    </Card>

    <Grid>
      <!-- 备份类型列 -->
      <template #backupType="{ row }">
        <Tag>{{ getBackupTypeName(row.backupType) }}</Tag>
      </template>

      <!-- 文件大小列 -->
      <template #fileSize="{ row }">
        {{ formatFileSize(row.fileSize) }}
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ getStatusName(row.status) }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a @click="handleDownload(row)">下载</a>
          <a 
            v-if="row.status === 'SUCCESS' || row.status === 'IN_PROGRESS'" 
            :class="{ 'opacity-50 cursor-not-allowed': row.status === 'IN_PROGRESS' }"
            @click="row.status === 'IN_PROGRESS' ? null : handleRestore(row)"
          >
            {{ row.status === 'IN_PROGRESS' ? '恢复中...' : '恢复' }}
          </a>
          <Popconfirm
            title="确定要删除该备份吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete(row)"
          >
            <a style="color: #ff4d4f">删除</a>
          </Popconfirm>
        </Space>
      </template>
    </Grid>

    <!-- 创建备份弹窗 -->
    <Modal
      v-model:open="createModalVisible"
      title="创建备份"
      :confirm-loading="createLoading"
      @ok="handleCreateSubmit"
      @cancel="() => { createFormRef?.resetFields(); createFormData = { backupType: '', description: '' }; }"
    >
      <Form ref="createFormRef" :model="createFormData" layout="vertical">
        <FormItem
          name="backupType"
          label="备份类型"
          :rules="[{ required: true, message: '请选择备份类型' }]"
        >
          <Select v-model:value="createFormData.backupType" :options="backupTypeOptions" placeholder="请选择备份类型" />
        </FormItem>
        <FormItem name="description" label="备份说明">
          <Textarea
            v-model:value="createFormData.description"
            :rows="4"
            placeholder="请输入备份说明（可选）"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 恢复备份弹窗 -->
    <Modal
      v-model:open="restoreModalVisible"
      title="恢复备份"
      :confirm-loading="restoreLoading"
      ok-type="danger"
      ok-text="确认恢复"
      @ok="handleRestoreSubmit"
      @cancel="() => { restoreFormRef?.resetFields(); restoreFormData = { backupId: 0, description: '' }; currentBackup = null; }"
    >
      <div v-if="currentBackup" class="mb-4">
        <p><strong>备份名称：</strong>{{ currentBackup.backupName }}</p>
        <p><strong>备份类型：</strong>{{ getBackupTypeName(currentBackup.backupType) }}</p>
        <p><strong>备份时间：</strong>{{ currentBackup.backupTime }}</p>
        <p class="text-red-500 mt-2">警告：恢复备份将覆盖当前数据库数据，请谨慎操作！</p>
      </div>
      <Form ref="restoreFormRef" :model="restoreFormData" layout="vertical">
        <FormItem name="description" label="恢复说明">
          <Textarea
            v-model:value="restoreFormData.description"
            :rows="4"
            placeholder="请输入恢复说明（可选）"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 下载进度弹窗 -->
    <Modal
      v-model:open="downloadModalVisible"
      title="下载备份"
      :footer="null"
      :closable="false"
      :mask-closable="false"
    >
      <div class="text-center py-4">
        <p class="mb-4">正在下载备份文件：{{ downloadFileName }}</p>
        <Progress :percent="downloadProgress" :status="downloadProgress === 100 ? 'success' : 'active'" />
        <p v-if="downloadProgress === 100" class="mt-4 text-green-500">下载完成！</p>
      </div>
    </Modal>

    <!-- 恢复进度弹窗 -->
    <Modal
      v-model:open="restoreProgressModalVisible"
      title="恢复备份"
      :footer="null"
      :closable="false"
      :mask-closable="false"
    >
      <div class="text-center py-4">
        <p class="mb-4">正在恢复备份，请稍候...</p>
        <Progress :percent="100" status="active" :show-info="false" />
        <p class="mt-4 text-gray-500">恢复操作可能需要几分钟时间，请勿关闭此窗口</p>
        <Button class="mt-4" @click="cancelRestoreProgress">取消监控</Button>
      </div>
    </Modal>

    <!-- 导入备份弹窗 -->
    <Modal
      v-model:open="importModalVisible"
      title="导入外部备份"
      :confirm-loading="importLoading"
      ok-text="导入"
      @ok="handleImportSubmit"
      @cancel="() => { importFormData = { file: null, backupType: 'DATABASE', description: '' }; }"
    >
      <Form ref="importFormRef" :model="importFormData" layout="vertical">
        <FormItem
          name="file"
          label="备份文件"
          :rules="[{ required: true, message: '请选择备份文件' }]"
        >
          <Upload
            :before-upload="beforeUpload"
            :max-count="1"
            :file-list="importFormData.file ? [{ uid: '-1', name: importFormData.file.name, status: 'done' }] : []"
            @remove="() => importFormData.file = null"
          >
            <Button>
              <UploadOutlined class="size-4 mr-1" /> 选择文件
            </Button>
          </Upload>
          <div class="text-gray-400 text-xs mt-1">
            支持 .sql 或 pg_dump 格式的备份文件
          </div>
        </FormItem>
        <FormItem
          name="backupType"
          label="备份类型"
          :rules="[{ required: true, message: '请选择备份类型' }]"
        >
          <Select v-model:value="importFormData.backupType" :options="backupTypeOptions" placeholder="请选择备份类型" />
        </FormItem>
        <FormItem name="description" label="备份说明">
          <Textarea
            v-model:value="importFormData.description"
            :rows="3"
            placeholder="请输入备份说明（可选）"
          />
        </FormItem>
        <FormItem v-if="importLoading">
          <Progress :percent="importProgress" :status="importProgress === 100 ? 'success' : 'active'" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

