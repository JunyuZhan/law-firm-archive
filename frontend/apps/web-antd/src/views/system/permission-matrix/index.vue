<script setup lang="ts">
import type {
  MenuDTO,
  PermissionCompareDTO,
  PermissionMatrixDTO,
  RoleDTO,
} from '#/api/system/types';

import { computed, h, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tree,
} from 'ant-design-vue';

import {
  assignRoleMenus,
  comparePermissions,
  getAllRoles,
  getMenuTree,
  getPermissionMatrix,
  getRoleMenuIds,
} from '#/api/system';

defineOptions({ name: 'PermissionMatrix' });

// ==================== 状态定义 ====================

const loading = ref(false);
const matrixData = ref<null | PermissionMatrixDTO>(null);
const roles = ref<RoleDTO[]>([]);
const menuTree = ref<MenuDTO[]>([]);
const isEditMode = ref(false);

// 对比相关
const selectedRoleIds = ref<number[]>([]);
const compareModalVisible = ref(false);
const compareLoading = ref(false);
const compareResult = ref<null | PermissionCompareDTO>(null);

// 编辑权限相关
const editModalVisible = ref(false);
const editLoading = ref(false);
const currentEditRole = ref<null | RoleDTO>(null);
const checkedMenuKeys = ref<number[]>([]);

// 筛选参数
const filterParams = reactive({
  module: undefined as string | undefined,
  permissionType: undefined as string | undefined,
});

// ==================== 常量配置 ====================

const moduleOptions = [
  { label: '全部', value: undefined },
  // 业务模块
  { label: '客户管理', value: 'client' },
  { label: '案源管理', value: 'lead' },
  { label: '利冲检查', value: 'conflict' },
  { label: '项目管理', value: 'matter' },
  { label: '合同管理', value: 'contract' },
  { label: '任务管理', value: 'task' },
  { label: '期限管理', value: 'deadline' },
  { label: '工时管理', value: 'timesheet' },
  { label: '日程管理', value: 'schedule' },
  // 财务模块
  { label: '财务管理', value: 'finance' },
  { label: '收费管理', value: 'fee' },
  { label: '工资管理', value: 'payroll' },
  // 文档档案
  { label: '文档管理', value: 'doc' },
  { label: '档案管理', value: 'archive' },
  { label: '知识库', value: 'knowledge' },
  // 审批报表
  { label: '审批管理', value: 'approval' },
  { label: '报表统计', value: 'report' },
  // 人事行政
  { label: '人力资源', value: 'hr' },
  { label: '行政管理', value: 'admin' },
  // 系统管理
  { label: '系统管理', value: 'sys' },
  { label: '系统配置', value: 'system' },
];

const permissionTypeOptions = [
  { label: '全部', value: undefined },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
];

// ==================== 计算属性 ====================

// 表格列配置（权限作为行，角色作为列）
const columns = computed(() => {
  const cols: any[] = [
    {
      title: '权限',
      dataIndex: 'permission',
      key: 'permission',
      width: 220,
      fixed: 'left',
      customRender: ({ record }: any) => {
        return h('div', { style: 'line-height: 1.3' }, [
          h(
            'div',
            { style: 'font-weight: 500; font-size: 12px' },
            record.permissionName,
          ),
          h(
            'div',
            { style: 'font-size: 10px; color: #999; margin-top: 1px' },
            record.permissionCode,
          ),
          h(
            Tag,
            {
              size: 'small',
              style: 'margin-top: 2px; font-size: 10px',
              color: record.menuType === 'BUTTON' ? 'blue' : 'default',
            },
            () => (record.menuType === 'BUTTON' ? '按钮' : '菜单'),
          ),
        ]);
      },
    },
  ];

  if (matrixData.value && matrixData.value.roles.length > 0) {
    matrixData.value.roles.forEach((role) => {
      cols.push({
        title: role.roleName,
        dataIndex: `role_${role.id}`,
        key: `role_${role.id}`,
        width: 80,
        align: 'center',
        customRender: ({ record }: any) => {
          const cellData = record[`role_${role.id}`];
          if (!cellData) return null;

          return h(
            'div',
            {
              class: [
                'permission-cell',
                {
                  editable: isEditMode.value,
                  'has-permission': cellData.hasPermission,
                },
              ],
              style: {
                cursor: isEditMode.value ? 'pointer' : 'default',
                padding: '2px',
                transition: 'background-color 0.2s',
              },
              title: isEditMode.value ? '点击切换权限' : '',
              onClick: isEditMode.value
                ? () =>
                    handleTogglePermission(
                      cellData.roleId,
                      cellData.permissionIndex,
                    )
                : undefined,
            },
            [
              h(
                Tag,
                {
                  color: cellData.hasPermission ? 'success' : 'default',
                  size: 'small',
                  style: 'font-size: 11px',
                },
                () => (cellData.hasPermission ? '✓' : '✗'),
              ),
            ],
          );
        },
      });
    });
  }

  return cols;
});

// 表格数据
const tableData = computed(() => {
  if (!matrixData.value?.permissions?.length) return [];

  return matrixData.value.permissions.map((perm, permIndex) => {
    const rowData: any = {
      key: perm.permissionCode,
      permissionCode: perm.permissionCode,
      permissionName: perm.permissionName,
      menuType: perm.menuType,
    };

    matrixData.value?.matrix?.forEach((matrixRow) => {
      const cell = matrixRow.permissions[permIndex];
      rowData[`role_${matrixRow.roleId}`] = {
        hasPermission: cell?.hasPermission || false,
        roleId: matrixRow.roleId,
        permissionIndex: permIndex,
      };
    });

    return rowData;
  });
});

// ==================== 数据加载 ====================

async function fetchMatrix() {
  loading.value = true;
  try {
    const params: any = {};
    if (filterParams.module) params.module = filterParams.module;
    if (filterParams.permissionType)
      params.permissionType = filterParams.permissionType;

    matrixData.value = await getPermissionMatrix(
      Object.keys(params).length > 0 ? params : undefined,
    );
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(`加载权限矩阵失败: ${err.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  try {
    roles.value = (await getAllRoles()) || [];
  } catch (error) {
    console.warn('加载角色列表失败', error);
  }
}

// ==================== 搜索操作 ====================

function handleSearch() {
  fetchMatrix();
}

function handleReset() {
  filterParams.module = undefined;
  filterParams.permissionType = undefined;
  fetchMatrix();
}

// ==================== 编辑模式 ====================

function toggleEditMode() {
  isEditMode.value = !isEditMode.value;
  if (isEditMode.value) {
    message.info('已进入编辑模式，点击单元格可快速切换权限');
  }
}

// 查找菜单ID
function findMenuIdByPermission(
  menus: MenuDTO[],
  permissionCode: string,
): null | number {
  for (const menu of menus) {
    if (menu.permission === permissionCode) return menu.id;
    if (menu.children?.length) {
      const found = findMenuIdByPermission(menu.children, permissionCode);
      if (found) return found;
    }
  }
  return null;
}

// 快速切换权限
function handleTogglePermission(roleId: number, permissionIndex: number) {
  if (!isEditMode.value || !matrixData.value) return;

  const row = matrixData.value.matrix.find((r) => r.roleId === roleId);
  const cell = row?.permissions[permissionIndex];
  const permission = matrixData.value.permissions[permissionIndex];
  if (!cell || !permission) return;

  const role = roles.value.find((r) => r.id === roleId);
  const action = cell.hasPermission ? '移除' : '添加';

  Modal.confirm({
    title: '确认修改权限',
    content: `确定要${action}角色"${role?.roleName || roleId}"的权限"${permission.permissionName}"吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: () => executeTogglePermission(roleId, permissionIndex),
  });
}

async function executeTogglePermission(
  roleId: number,
  permissionIndex: number,
) {
  if (!matrixData.value) return;

  const row = matrixData.value.matrix.find((r) => r.roleId === roleId);
  const cell = row?.permissions[permissionIndex];
  const permission = matrixData.value.permissions[permissionIndex];
  if (!cell || !permission) return;

  try {
    const currentMenuIds = await getRoleMenuIds(roleId);

    if (menuTree.value.length === 0) {
      menuTree.value = await getMenuTree();
    }

    const menuId = findMenuIdByPermission(
      menuTree.value,
      permission.permissionCode,
    );
    if (!menuId) {
      message.warning(`未找到权限 "${permission.permissionName}" 对应的菜单`);
      return;
    }

    const newMenuIds = cell.hasPermission
      ? currentMenuIds.filter((id) => id !== menuId)
      : [...currentMenuIds, menuId];

    await assignRoleMenus(roleId, newMenuIds);
    message.success(
      `权限已${cell.hasPermission ? '移除' : '添加'}: ${permission.permissionName}`,
    );
    await fetchMatrix();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(`更新权限失败: ${err.message || '未知错误'}`);
  }
}

// ==================== 对比权限 ====================

function handleCompare() {
  selectedRoleIds.value = [];
  compareResult.value = null;
  compareModalVisible.value = true;
}

async function executeCompare(roleIds: number[]) {
  if (roleIds.length < 2) {
    message.warning('请至少选择2个角色进行对比');
    return;
  }

  compareLoading.value = true;
  try {
    compareResult.value = await comparePermissions(roleIds);
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(`对比权限失败: ${err.message || '未知错误'}`);
  } finally {
    compareLoading.value = false;
  }
}

// ==================== 编辑角色权限 ====================

async function handleSavePermissions() {
  if (!currentEditRole.value) return;
  editLoading.value = true;
  try {
    await assignRoleMenus(currentEditRole.value.id, checkedMenuKeys.value);
    message.success(
      `权限分配成功，已分配 ${checkedMenuKeys.value.length} 个权限`,
    );
    editModalVisible.value = false;
    await fetchMatrix();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(`权限分配失败: ${err.message || '未知错误'}`);
  } finally {
    editLoading.value = false;
  }
}

// ==================== 导出 ====================

function handleExport() {
  message.info('导出功能开发中...');
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchMatrix();
  loadRoles();
});
</script>

<template>
  <Page title="权限矩阵管理" description="查看和管理所有角色的权限配置">
    <Card>
      <!-- 筛选栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Select
              v-model:value="filterParams.module"
              placeholder="模块筛选"
              allow-clear
              style="width: 100%"
              :options="moduleOptions"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6" :lg="5">
            <Select
              v-model:value="filterParams.permissionType"
              placeholder="权限类型"
              allow-clear
              style="width: 100%"
              :options="permissionTypeOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12" :lg="14">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button
                :type="isEditMode ? 'default' : 'primary'"
                @click="toggleEditMode"
              >
                {{ isEditMode ? '退出编辑' : '编辑权限' }}
              </Button>
              <Button @click="handleCompare">对比权限</Button>
              <Button @click="handleExport">导出Excel</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 权限矩阵表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="{
          pageSize: 50,
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: ['20', '50', '100', '200'],
        }"
        :scroll="{ x: 'max-content', y: 500 }"
        bordered
        size="small"
        :row-class-name="() => 'permission-matrix-row'"
      />

      <!-- 说明 -->
      <Alert
        :message="isEditMode ? '编辑模式已开启' : '权限矩阵说明'"
        :description="
          isEditMode
            ? '点击单元格切换权限时会弹出确认对话框，确认后立即生效。'
            : '✓ 表示拥有该权限，✗ 表示没有该权限。点击「编辑权限」可进入编辑模式。'
        "
        :type="isEditMode ? 'warning' : 'info'"
        show-icon
        style="margin-top: 16px"
      />
    </Card>

    <!-- 权限对比弹窗 -->
    <Modal
      v-model:open="compareModalVisible"
      title="权限对比"
      width="800px"
      :footer="null"
    >
      <div v-if="!compareResult">
        <p>请选择要对比的角色：</p>
        <Checkbox.Group v-model:value="selectedRoleIds" style="width: 100%">
          <Row>
            <Col :span="8" v-for="role in roles" :key="role.id">
              <Checkbox :value="role.id">{{ role.roleName }}</Checkbox>
            </Col>
          </Row>
        </Checkbox.Group>
        <div style="margin-top: 16px; text-align: right">
          <Button
            type="primary"
            @click="executeCompare(selectedRoleIds)"
            :loading="compareLoading"
          >
            开始对比
          </Button>
        </div>
      </div>
      <div v-else>
        <Alert
          message="对比结果"
          description="显示选中角色的权限差异"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <div v-if="compareResult.permissions?.length">
          <Table
            :columns="[
              {
                title: '权限名称',
                key: 'permissionName',
                dataIndex: 'permissionName',
                width: 200,
              },
              ...(compareResult.roles || []).map((r) => ({
                title: r.roleName,
                key: `role_${r.id}`,
                dataIndex: `role_${r.id}`,
                width: 100,
                align: 'center' as const,
              })),
            ]"
            :data-source="
              compareResult?.permissions?.map((p, idx) => ({
                key: idx,
                permissionName: p.permissionName,
                ...(compareResult?.roles || []).reduce((acc, r) => {
                  acc[`role_${r.id}`] = p.roleHasPermission?.[r.id] || false;
                  return acc;
                }, {} as any),
              })) || []
            "
            :pagination="false"
            bordered
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key?.toString().startsWith('role_')">
                <Tag
                  :color="record[column.key] ? 'success' : 'default'"
                  size="small"
                >
                  {{ record[column.key] ? '✓' : '✗' }}
                </Tag>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </Modal>

    <!-- 编辑权限弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      :title="`编辑权限 - ${currentEditRole?.roleName || ''}`"
      width="600px"
      @ok="handleSavePermissions"
      :confirm-loading="editLoading"
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
.permission-matrix-row :deep(.ant-table-cell) {
  padding: 4px 6px !important;
}

:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}

:deep(.ant-table-tbody > tr > td) {
  padding: 4px 6px !important;
}

.permission-cell {
  display: inline-block;
}

.permission-cell.editable {
  border-radius: 4px;
  transition: background-color 0.2s;
}

.permission-cell.editable:hover {
  background-color: #f5f5f5;
}

.permission-cell.has-permission.editable:hover {
  background-color: #f6ffed;
}
</style>
