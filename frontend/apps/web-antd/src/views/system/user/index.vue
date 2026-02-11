<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DepartmentDTO, UserDTO } from '#/api/system/types';

import { ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { DownloadOutlined, DownOutlined, UploadOutlined } from '@vben/icons';

import {
  Button,
  Dropdown,
  Menu,
  MenuItem,
  message,
  Modal,
  Space,
  Switch,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  changeUserStatus,
  deleteUser,
  exportUsers,
  getDepartmentTree,
  getUserList,
  resetPassword,
} from '#/api/system';
import { useResponsive } from '#/hooks/useResponsive';

import UserDrawer from './components/UserDrawer.vue';
import UserImportModal from './components/UserImportModal.vue';
import UserModal from './components/UserModal.vue';

defineOptions({ name: 'SystemUser' });

// 响应式布局
const { isMobile } = useResponsive();

// ==================== 状态定义 ====================

const userModalRef = ref<InstanceType<typeof UserModal>>();
const userDrawerRef = ref<InstanceType<typeof UserDrawer>>();
const userImportModalRef = ref<InstanceType<typeof UserImportModal>>();
const useDrawer = ref(true); // 默认使用抽屉模式体验
const departments = ref<DepartmentDTO[]>([]);
const exporting = ref(false);

// 加载部门树
async function loadDepartments() {
  try {
    departments.value = await getDepartmentTree();
  } catch (error) {
    console.error('加载部门失败:', error);
  }
}
loadDepartments();

// ==================== 搜索表单配置 ====================

const formSchema: VbenFormSchema[] = [
  {
    fieldName: 'username',
    label: '用户名',
    component: 'Input',
    componentProps: {
      placeholder: '请输入用户名',
      allowClear: true,
    },
  },
  {
    fieldName: 'realName',
    label: '姓名',
    component: 'Input',
    componentProps: {
      placeholder: '请输入姓名',
      allowClear: true,
    },
  },
  {
    fieldName: 'departmentId',
    label: '部门',
    component: 'TreeSelect',
    componentProps: {
      placeholder: '请选择部门',
      treeData: departments.value,
      fieldNames: { label: 'name', value: 'id', children: 'children' },
      allowClear: true,
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'Select',
    componentProps: {
      placeholder: '请选择状态',
      allowClear: true,
      options: [
        { label: '正常', value: 'ACTIVE' },
        { label: '禁用', value: 'DISABLED' },
      ],
    },
  },
];

// ==================== 表格配置 ====================

// 响应式列配置
function getGridColumns(): VxeGridProps['columns'] {
  const baseColumns = [
    { type: 'checkbox' as const, width: 50 },
    { title: '用户名', field: 'username', width: 120, mobileShow: true },
    { title: '姓名', field: 'realName', width: 100, mobileShow: true },
    { title: '部门', field: 'departmentName', width: 120 },
    { title: '职位', field: 'position', width: 100 },
    { title: '手机号', field: 'phone', width: 130 },
    { title: '邮箱', field: 'email', minWidth: 180 },
    {
      title: '状态',
      field: 'status',
      width: 80,
      slots: { default: 'status' },
      mobileShow: true,
    },
    { title: '创建时间', field: 'createdAt', width: 160 },
    {
      title: '操作',
      field: 'action',
      width: isMobile.value ? 120 : 220,
      fixed: 'right' as const,
      slots: { default: 'action' },
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return baseColumns.filter(
      (col) => col.type === 'checkbox' || col.mobileShow === true,
    );
  }
  return baseColumns;
}

// 加载数据
async function loadData(
  params: Record<string, any> & { page: number; pageSize: number },
) {
  const res = await getUserList({
    pageNum: params.page,
    pageSize: params.pageSize,
    username: params.username,
    realName: params.realName,
    departmentId: params.departmentId,
    status: params.status,
  });
  return {
    items: res?.list ?? [],
    total: res?.total ?? 0,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: formSchema,
    showCollapseButton: false,
    submitButtonOptions: { content: '查询' },
    resetButtonOptions: { content: '重置' },
  },
  gridOptions: {
    columns: getGridColumns(),
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({
          page,
          form,
        }: {
          form: Record<string, any>;
          page: { currentPage: number; pageSize: number };
        }) => {
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
    toolbarConfig: {
      slots: { buttons: 'toolbar-buttons' },
    },
  },
});

// 监听响应式变化，更新列配置
watch(isMobile, () => {
  gridApi.setGridOptions({ columns: getGridColumns() });
});

// ==================== 操作方法 ====================

// 新增用户（左侧按钮 → 右侧抽屉）
function handleAdd() {
  if (useDrawer.value) {
    userDrawerRef.value?.openCreate('right');
  } else {
    userModalRef.value?.openCreate();
  }
}

// 编辑用户（右侧按钮 → 左侧抽屉）
function handleEdit(row: UserDTO) {
  if (useDrawer.value) {
    userDrawerRef.value?.openEdit(row, 'left');
  } else {
    userModalRef.value?.openEdit(row);
  }
}

// 删除用户
function handleDelete(row: UserDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户 "${row.realName}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteUser(row.id);
        message.success('删除成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

// 重置密码
function handleResetPassword(row: UserDTO) {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置用户 "${row.realName}" 的密码吗？默认密码为：123456`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await resetPassword(row.id, '123456');
        message.success('密码已重置为 123456');
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '重置密码失败');
      }
    },
  });
}

// 修改用户状态
function handleStatusChange(row: UserDTO) {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const statusText = newStatus === 'ACTIVE' ? '启用' : '禁用';
  Modal.confirm({
    title: `确认${statusText}`,
    content: `确定要${statusText}用户 "${row.realName}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await changeUserStatus(row.id, newStatus);
        message.success(`${statusText}成功`);
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || `${statusText}失败`);
      }
    },
  });
}

// 弹窗成功回调
function handleModalSuccess() {
  gridApi.reload();
}

// 打开批量导入弹窗
function handleImport() {
  userImportModalRef.value?.open();
}

// 导出用户列表
async function handleExport() {
  try {
    exporting.value = true;
    const blob = await exportUsers();
    const url = window.URL.createObjectURL(blob as Blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `用户列表_${new Date().toISOString().split('T')[0]}.xlsx`;
    link.click();
    window.URL.revokeObjectURL(url);
    message.success('导出成功');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '导出失败');
  } finally {
    exporting.value = false;
  }
}

// 状态颜色
function getStatusColor(status: string) {
  return status === 'ACTIVE' ? 'green' : 'red';
}
</script>

<template>
  <Page title="用户管理" description="管理系统用户">
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Space>
          <Button
            v-access:code="'sys:user:create'"
            type="primary"
            @click="handleAdd"
          >
            新增用户
          </Button>
          <span v-access:code="'sys:user:create'">
            <Dropdown>
              <Button>
                更多操作
                <DownOutlined />
              </Button>
              <template #overlay>
                <Menu>
                  <MenuItem key="import" @click="handleImport">
                    <UploadOutlined />
                    批量导入
                  </MenuItem>
                  <MenuItem key="export" @click="handleExport">
                    <DownloadOutlined />
                    {{ exporting ? '导出中...' : '导出列表' }}
                  </MenuItem>
                </Menu>
              </template>
            </Dropdown>
          </span>
          <Tooltip title="切换弹窗/抽屉模式">
            <Space>
              <span class="text-sm text-gray-500">弹窗</span>
              <Switch v-model:checked="useDrawer" />
              <span class="text-sm text-gray-500">抽屉</span>
            </Space>
          </Tooltip>
        </Space>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <Tag :color="getStatusColor(row.status)">
          {{ row.statusName || (row.status === 'ACTIVE' ? '正常' : '禁用') }}
        </Tag>
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <Space>
          <a v-access:code="'sys:user:update'" @click="handleEdit(row)">编辑</a>
          <a v-access:code="'sys:user:update'" @click="handleResetPassword(row)"
            >重置密码</a
          >
          <a v-access:code="'sys:user:update'" @click="handleStatusChange(row)">
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </a>
          <a
            v-access:code="'sys:user:delete'"
            style="color: #ff4d4f"
            @click="handleDelete(row)"
            >删除</a
          >
        </Space>
      </template>
    </Grid>

    <!-- 用户弹窗 -->
    <UserModal ref="userModalRef" @success="handleModalSuccess" />
    <!-- 用户抽屉 -->
    <UserDrawer ref="userDrawerRef" @success="handleModalSuccess" />
    <!-- 用户导入弹窗 -->
    <UserImportModal ref="userImportModalRef" @success="handleModalSuccess" />
  </Page>
</template>
