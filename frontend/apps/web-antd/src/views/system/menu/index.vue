<script setup lang="ts">
import type { MenuDTO } from '#/api/system/types';

import { onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { deleteMenu, getMenuTree } from '#/api/system';

import MenuModal from './components/MenuModal.vue';

defineOptions({ name: 'SystemMenu' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<MenuDTO[]>([]);
const menuModalRef = ref<InstanceType<typeof MenuModal>>();

// 菜单类型选项
const menuTypeOptions = [
  { label: '目录', value: 'DIRECTORY' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
];

// 表格列
const columns = [
  { title: '菜单名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '图标', dataIndex: 'icon', key: 'icon', width: 100 },
  { title: '类型', dataIndex: 'menuType', key: 'menuType', width: 100 },
  { title: '路由路径', dataIndex: 'path', key: 'path', width: 200 },
  { title: '组件路径', dataIndex: 'component', key: 'component', width: 200 },
  { title: '权限标识', dataIndex: 'permission', key: 'permission', width: 150 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '可见', dataIndex: 'visible', key: 'visible', width: 80 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' as const },
];

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true;
  try {
    dataSource.value = await getMenuTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载菜单列表失败');
  } finally {
    loading.value = false;
  }
}

// ==================== 操作方法 ====================

// 新增菜单
function handleAdd(parentId?: number) {
  menuModalRef.value?.openCreate(parentId);
}

// 编辑菜单
function handleEdit(record: MenuDTO) {
  menuModalRef.value?.openEdit(record);
}

// 删除菜单
function handleDelete(record: MenuDTO) {
  if (record.children && record.children.length > 0) {
    message.warning('请先删除子菜单');
    return;
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除菜单 "${record.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteMenu(record.id);
        message.success('删除成功');
        fetchData();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

// 弹窗成功回调
function handleModalSuccess() {
  fetchData();
}

// 获取菜单类型名称
function getMenuTypeName(type: string) {
  return menuTypeOptions.find((o) => o.value === type)?.label || type;
}

// 获取菜单类型颜色
function getMenuTypeColor(type: string) {
  const colorMap: Record<string, string> = {
    DIRECTORY: 'blue',
    MENU: 'green',
    BUTTON: 'orange',
  };
  return colorMap[type] || 'default';
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="菜单管理" description="管理系统菜单和权限">
    <Card :bordered="false">
      <!-- 操作按钮 -->
      <div style="margin-bottom: 16px">
        <Button type="primary" @click="handleAdd()">新增菜单</Button>
      </div>

      <!-- 数据表格（树形） -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :default-expand-all-rows="true"
        row-key="id"
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record: rawRecord }">
          <template v-if="column.key === 'menuType'">
            <Tag :color="getMenuTypeColor((rawRecord as MenuDTO).menuType)">
              {{ getMenuTypeName((rawRecord as MenuDTO).menuType) }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'visible'">
            <Tag :color="(rawRecord as MenuDTO).visible ? 'green' : 'red'">
              {{ (rawRecord as MenuDTO).visible ? '是' : '否' }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleAdd((rawRecord as MenuDTO).id)">新增子菜单</a>
              <a @click="handleEdit(rawRecord as MenuDTO)">编辑</a>
              <Popconfirm
                title="确定删除该菜单？"
                @confirm="handleDelete(rawRecord as MenuDTO)"
              >
                <a style="color: #ff4d4f">删除</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 菜单弹窗 -->
    <MenuModal
      ref="menuModalRef"
      :menu-tree="dataSource"
      @success="handleModalSuccess"
    />
  </Page>
</template>
