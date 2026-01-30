<script setup lang="ts">
import type {
  CreateRoleCommand,
  MenuDTO,
  RoleDTO,
  RoleQuery,
  UpdateRoleCommand,
} from '#/api/system/types';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
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
  Table,
  Textarea,
  Tree,
} from 'ant-design-vue';

import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getMenuTree,
  getRoleList,
  getRoleMenuIds,
  updateRole,
} from '#/api/system';
import { useResponsive } from '#/hooks/useResponsive';

defineOptions({ name: 'SystemRole' });

// 响应式布局
const { isMobile } = useResponsive();

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<RoleDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const modalTitle = ref('新增角色');
const formRef = ref();
const menuModalVisible = ref(false);
const currentRole = ref<null | RoleDTO>(null);
const menuTree = ref<MenuDTO[]>([]);
const checkedMenuKeys = ref<number[]>([]);

// 查询参数
const queryParams = reactive<RoleQuery>({
  pageNum: 1,
  pageSize: 10,
  roleCode: undefined,
  roleName: undefined,
});

// 表单数据
const formData = reactive<Partial<CreateRoleCommand> & { id?: number }>({
  id: undefined,
  roleCode: '',
  roleName: '',
  description: '',
  dataScope: 'SELF',
  sortOrder: 0,
});

// ==================== 常量配置 ====================

// 表格列（响应式）
const columns = computed(() => {
  const baseColumns = [
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', width: 150 },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'roleName',
      width: 150,
      mobileShow: true,
    },
    {
      title: '数据范围',
      dataIndex: 'dataScopeName',
      key: 'dataScopeName',
      width: 120,
      mobileShow: true,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
    {
      title: '操作',
      key: 'action',
      width: isMobile.value ? 120 : 200,
      fixed: 'right' as const,
      mobileShow: true,
    },
  ];

  if (isMobile.value) {
    return baseColumns.filter((col) => col.mobileShow === true);
  }
  return baseColumns;
});

// 数据范围选项
const dataScopeOptions = [
  { label: '全部数据', value: 'ALL' },
  { label: '本部门及下级', value: 'DEPT_AND_CHILD' },
  { label: '本部门', value: 'DEPT' },
  { label: '仅本人', value: 'SELF' },
];

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true;
  try {
    const res = await getRoleList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载角色列表失败');
  } finally {
    loading.value = false;
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

function handleReset() {
  queryParams.roleCode = undefined;
  queryParams.roleName = undefined;
  queryParams.pageNum = 1;
  fetchData();
}

// ==================== CRUD 操作 ====================

// 新增角色
function handleAdd() {
  modalTitle.value = '新增角色';
  Object.assign(formData, {
    id: undefined,
    roleCode: '',
    roleName: '',
    description: '',
    dataScope: 'SELF',
    sortOrder: 0,
  });
  modalVisible.value = true;
}

// 编辑角色
function handleEdit(record: RoleDTO) {
  modalTitle.value = '编辑角色';
  Object.assign(formData, {
    id: record.id,
    roleCode: record.roleCode,
    roleName: record.roleName,
    description: record.description,
    dataScope: record.dataScope || 'SELF',
    sortOrder: record.sortOrder || 0,
  });
  modalVisible.value = true;
}

// 保存角色
async function handleSave() {
  try {
    await formRef.value?.validate();

    if (formData.id) {
      const updateData: UpdateRoleCommand = {
        id: formData.id,
        ...formData,
      } as UpdateRoleCommand;
      await updateRole(updateData);
      message.success('更新成功');
    } else {
      const createData: CreateRoleCommand = {
        ...formData,
      } as CreateRoleCommand;
      await createRole(createData);
      message.success('创建成功');
    }
    modalVisible.value = false;
    fetchData();
  } catch (error: unknown) {
    const err = error as { errorFields?: unknown; message?: string };
    if (err?.errorFields) return;
    message.error(err.message || '操作失败');
  }
}

// 删除角色
async function handleDelete(record: RoleDTO) {
  try {
    await deleteRole(record.id);
    message.success('删除成功');
    fetchData();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

// ==================== 权限分配 ====================

// 打开分配权限弹窗
async function handleAssignMenu(record: RoleDTO) {
  currentRole.value = record;
  try {
    // 加载菜单树
    if (menuTree.value.length === 0) {
      menuTree.value = await getMenuTree();
    }
    // 加载角色已有权限
    checkedMenuKeys.value = await getRoleMenuIds(record.id);
    menuModalVisible.value = true;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载权限失败');
  }
}

// 保存权限分配
async function handleSaveMenu() {
  if (!currentRole.value) return;
  try {
    await assignRoleMenus(currentRole.value.id, checkedMenuKeys.value);
    message.success('权限分配成功');
    menuModalVisible.value = false;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '权限分配失败');
  }
}

// ==================== 辅助方法 ====================

// 分页变化
function handlePageChange(page: number, size: number) {
  queryParams.pageNum = page;
  queryParams.pageSize = size;
  fetchData();
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="角色管理" description="管理系统角色和权限">
    <Card :bordered="false">
      <!-- 搜索栏 - 响应式布局 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Input
              v-model:value="queryParams.roleCode"
              placeholder="角色编码"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Input
              v-model:value="queryParams.roleName"
              placeholder="角色名称"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12" :lg="14">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">新增角色</Button>
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
          showQuickJumper: true,
          showTotal: (t: number) => `共 ${t} 条`,
          onChange: handlePageChange,
        }"
        row-key="id"
        :scroll="{ x: 1000 }"
      >
        <template #bodyCell="{ column, record: rawRecord }">
          <template v-if="column.key === 'dataScopeName'">
            {{
              dataScopeOptions.find(
                (o) => o.value === (rawRecord as RoleDTO).dataScope,
              )?.label || (rawRecord as RoleDTO).dataScope
            }}
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleEdit(rawRecord as RoleDTO)">编辑</a>
              <a @click="handleAssignMenu(rawRecord as RoleDTO)">分配权限</a>
              <Popconfirm
                title="确定删除该角色？"
                @confirm="handleDelete(rawRecord as RoleDTO)"
              >
                <a style="color: #ff4d4f">删除</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="isMobile ? '100%' : '600px'"
      :centered="isMobile"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        style="margin-top: 16px"
      >
        <FormItem
          label="角色编码"
          name="roleCode"
          :rules="[{ required: true, message: '请输入角色编码' }]"
        >
          <Input
            v-model:value="formData.roleCode"
            :disabled="!!formData.id"
            placeholder="请输入角色编码"
          />
        </FormItem>

        <FormItem
          label="角色名称"
          name="roleName"
          :rules="[{ required: true, message: '请输入角色名称' }]"
        >
          <Input
            v-model:value="formData.roleName"
            placeholder="请输入角色名称"
          />
        </FormItem>

        <FormItem label="数据范围" name="dataScope">
          <Select
            v-model:value="formData.dataScope"
            :options="dataScopeOptions"
            style="width: 100%"
          />
        </FormItem>

        <FormItem label="排序" name="sortOrder">
          <InputNumber
            v-model:value="formData.sortOrder"
            :min="0"
            style="width: 100%"
          />
        </FormItem>

        <FormItem label="描述" name="description">
          <Textarea
            v-model:value="formData.description"
            :rows="3"
            placeholder="请输入描述"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 分配权限弹窗 -->
    <Modal
      v-model:open="menuModalVisible"
      :title="`分配权限 - ${currentRole?.roleName || ''}`"
      :width="isMobile ? '100%' : '600px'"
      :centered="isMobile"
      @ok="handleSaveMenu"
    >
      <Tree
        v-model:checked-keys="checkedMenuKeys"
        :tree-data="menuTree as any"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        checkable
        :default-expand-all="true"
        :height="400"
      />
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-table-cell) {
  vertical-align: middle;
}
</style>
