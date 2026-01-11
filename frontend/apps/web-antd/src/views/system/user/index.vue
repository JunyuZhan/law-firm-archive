<script setup lang="ts">
import { ref } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { Button, Space, Tag } from 'ant-design-vue';
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getUserList,
  deleteUser,
  resetPassword,
  changeUserStatus,
  getDepartmentTree,
} from '#/api/system';
import type { UserDTO, DepartmentDTO } from '#/api/system/types';
import UserModal from './components/UserModal.vue';

defineOptions({ name: 'SystemUser' });

// ==================== 状态定义 ====================

const userModalRef = ref<InstanceType<typeof UserModal>>();
const departments = ref<DepartmentDTO[]>([]);

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

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { type: 'checkbox', width: 50 },
  { title: '用户名', field: 'username', width: 120 },
  { title: '姓名', field: 'realName', width: 100 },
  { title: '部门', field: 'departmentName', width: 120 },
  { title: '职位', field: 'position', width: 100 },
  { title: '手机号', field: 'phone', width: 130 },
  { title: '邮箱', field: 'email', minWidth: 180 },
  { title: '状态', field: 'status', width: 80, slots: { default: 'status' } },
  { title: '创建时间', field: 'createdAt', width: 160 },
  { title: '操作', field: 'action', width: 220, fixed: 'right', slots: { default: 'action' } },
];

// 加载数据
async function loadData(params: { page: number; pageSize: number } & Record<string, any>) {
  const res = await getUserList({
    pageNum: params.page,
    pageSize: params.pageSize,
    username: params.username,
    realName: params.realName,
    departmentId: params.departmentId,
    status: params.status,
  });
  return {
    items: res.list,
    total: res.total,
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
    columns: gridColumns,
    height: 'auto',
    proxyConfig: {
      ajax: {
        query: async ({ page, form }) => {
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

// ==================== 操作方法 ====================

// 新增用户
function handleAdd() {
  userModalRef.value?.openCreate();
}

// 编辑用户
function handleEdit(row: UserDTO) {
  userModalRef.value?.openEdit(row);
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

// 状态颜色
function getStatusColor(status: string) {
  return status === 'ACTIVE' ? 'green' : 'red';
}
</script>

<template>
  <Page title="用户管理" description="管理系统用户" auto-content-height>
    <Grid>
      <!-- 工具栏按钮 -->
      <template #toolbar-buttons>
        <Button v-access:code="'user:create'" type="primary" @click="handleAdd">新增用户</Button>
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
          <a v-access:code="'user:edit'" @click="handleEdit(row)">编辑</a>
          <a v-access:code="'user:reset-password'" @click="handleResetPassword(row)">重置密码</a>
          <a v-access:code="'user:edit'" @click="handleStatusChange(row)">
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </a>
          <a v-access:code="'user:delete'" style="color: #ff4d4f" @click="handleDelete(row)">删除</a>
        </Space>
      </template>
    </Grid>

    <!-- 用户弹窗 -->
    <UserModal ref="userModalRef" @success="handleModalSuccess" />
  </Page>
</template>
