<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreateMenuCommand,
  MenuDTO,
  UpdateMenuCommand,
} from '#/api/system/types';

import { computed, ref, watch } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createMenu, updateMenu } from '#/api/system';

const props = defineProps<{
  menuTree: MenuDTO[];
}>();

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();
const currentMenuType = ref<string>('MENU');

// 菜单类型选项
const menuTypeOptions = [
  { label: '目录', value: 'DIRECTORY' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
];

// 构建菜单树数据
const menuTreeData = computed(() => {
  const buildTree = (menus: MenuDTO[], parentId: number = 0): any[] => {
    return menus
      .filter((menu) => menu.parentId === parentId)
      .map((menu) => ({
        id: menu.id,
        name: menu.name,
        children: buildTree(menus, menu.id),
      }));
  };
  // 扁平化处理
  const flattenMenus = (menus: MenuDTO[]): MenuDTO[] => {
    const result: MenuDTO[] = [];
    const flatten = (items: MenuDTO[]) => {
      items.forEach((item) => {
        result.push(item);
        if (item.children?.length) {
          flatten(item.children);
        }
      });
    };
    flatten(menus);
    return result;
  };
  return [
    {
      id: 0,
      name: '顶级菜单',
      children: buildTree(flattenMenus(props.menuTree)),
    },
  ];
});

// 构建表单 Schema
function getFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'parentId',
      label: '上级菜单',
      component: 'TreeSelect',
      defaultValue: 0,
      componentProps: {
        placeholder: '请选择上级菜单',
        treeData: menuTreeData.value,
        fieldNames: { label: 'name', value: 'id', children: 'children' },
        allowClear: true,
        treeDefaultExpandAll: true,
      },
    },
    {
      fieldName: 'menuType',
      label: '菜单类型',
      component: 'Select',
      rules: 'required',
      defaultValue: 'MENU',
      componentProps: {
        options: menuTypeOptions,
        onChange: (val: string) => {
          currentMenuType.value = val;
          updateFormSchema();
        },
      },
    },
    {
      fieldName: 'name',
      label: '菜单名称',
      component: 'Input',
      rules: 'required',
      componentProps: {
        placeholder: '请输入菜单名称',
      },
    },
    {
      fieldName: 'path',
      label: '路由路径',
      component: 'Input',
      componentProps: {
        placeholder: '请输入路由路径',
      },
    },
    {
      fieldName: 'component',
      label: '组件路径',
      component: 'Input',
      hide: currentMenuType.value !== 'MENU',
      componentProps: {
        placeholder: '请输入组件路径',
      },
    },
    {
      fieldName: 'icon',
      label: '图标',
      component: 'Input',
      componentProps: {
        placeholder: '请输入图标名称',
      },
    },
    {
      fieldName: 'permission',
      label: '权限标识',
      component: 'Input',
      componentProps: {
        placeholder: '请输入权限标识',
      },
    },
    {
      fieldName: 'sortOrder',
      label: '排序',
      component: 'InputNumber',
      defaultValue: 0,
      componentProps: {
        min: 0,
        style: { width: '100%' },
      },
    },
    {
      fieldName: 'visible',
      label: '是否可见',
      component: 'Switch',
      defaultValue: true,
    },
    {
      fieldName: 'isCache',
      label: '是否缓存',
      component: 'Switch',
      defaultValue: true,
    },
    {
      fieldName: 'isExternal',
      label: '是否外链',
      component: 'Switch',
      defaultValue: false,
    },
  ];
}

const [Form, formApi] = useVbenForm({
  schema: getFormSchema(),
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 100,
  },
});

// 更新表单schema
function updateFormSchema() {
  formApi.setState({ schema: getFormSchema() });
}

// 监听菜单树变化更新表单
watch(
  () => props.menuTree,
  () => {
    updateFormSchema();
  },
  { deep: true },
);

const [Drawer, drawerApi] = useVbenDrawer({
  overlayBlur: 4,
  placement: 'right', // 默认从右侧滑入
  async onConfirm() {
    try {
      await formApi.validate();
      const values = await formApi.getValues();

      if (isEdit.value && editId.value) {
        const updateData: UpdateMenuCommand = {
          id: editId.value,
          parentId: values.parentId,
          name: values.name,
          path: values.path,
          component: values.component,
          icon: values.icon,
          menuType: values.menuType,
          permission: values.permission,
          sortOrder: values.sortOrder,
          visible: values.visible,
          isExternal: values.isExternal,
          isCache: values.isCache,
        };
        await updateMenu(updateData);
        message.success('更新成功');
      } else {
        const createData: CreateMenuCommand = {
          parentId: values.parentId,
          name: values.name,
          path: values.path,
          component: values.component,
          icon: values.icon,
          menuType: values.menuType,
          permission: values.permission,
          sortOrder: values.sortOrder,
          visible: values.visible,
          isExternal: values.isExternal,
          isCache: values.isCache,
        };
        await createMenu(createData);
        message.success('创建成功');
      }

      drawerApi.close();
      emit('success');
    } catch (error: unknown) {
      const err = error as { errorFields?: unknown; message?: string };
      if (err?.errorFields) return;
      message.error(err.message || '操作失败');
    }
  },
});

// 打开新增抽屉（左侧按钮 → 右侧抽屉）
function openCreate(parentId?: number) {
  isEdit.value = false;
  editId.value = undefined;
  currentMenuType.value = 'MENU';
  formApi.resetForm();
  if (parentId !== undefined) {
    formApi.setValues({ parentId });
  }
  drawerApi.setState({ title: '新增菜单', placement: 'right' });
  drawerApi.open();
}

// 打开编辑抽屉（右侧按钮 → 左侧抽屉）
function openEdit(record: MenuDTO) {
  isEdit.value = true;
  editId.value = record.id;
  currentMenuType.value = record.menuType || 'MENU';
  formApi.resetForm();
  formApi.setValues({
    parentId: record.parentId || 0,
    name: record.name,
    path: record.path,
    component: record.component,
    redirect: record.redirect,
    icon: record.icon,
    menuType: record.menuType,
    permission: record.permission,
    sortOrder: record.sortOrder || 0,
    visible: record.visible !== false,
    isExternal: record.isExternal || false,
    isCache: record.isCache !== false,
  });
  drawerApi.setState({ title: '编辑菜单', placement: 'left' });
  drawerApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Drawer class="w-[520px]">
    <Form />
  </Drawer>
</template>
