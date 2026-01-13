<script setup lang="ts">
import type { DepartmentDTO, UserDTO } from '#/api/system/types';

/**
 * 用户选择器组件
 * 支持按部门分组展示用户，支持搜索和虚拟滚动
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { TreeSelect } from 'ant-design-vue';

import { getDepartmentTreePublic, getUserSelectOptions } from '#/api/system';

defineOptions({ name: 'UserTreeSelect' });

const props = withDefaults(
  defineProps<{
    /** 是否允许清除 */
    allowClear?: boolean;
    /** 是否禁用 */
    disabled?: boolean;
    /** 排除的用户ID列表 */
    excludeUserIds?: number[];
    /** 最大可选数量（多选时） */
    maxCount?: number;
    /** 是否多选 */
    multiple?: boolean;
    /** 占位文本 */
    placeholder?: string;
    /** 样式 */
    style?: Record<string, any> | string;
    /** 只显示指定状态的用户 */
    userStatus?: string;
    /** 选中的用户ID（单选） */
    value?: null | number | number[];
    /** 虚拟滚动列表高度 */
    listHeight?: number;
    /** 启用虚拟滚动的阈值 */
    virtualThreshold?: number;
  }>(),
  {
    placeholder: '请选择用户',
    allowClear: true,
    disabled: false,
    excludeUserIds: () => [],
    userStatus: 'ACTIVE',
    multiple: false,
    listHeight: 300,
    virtualThreshold: 30,
  },
);

const emit = defineEmits<{
  (e: 'update:value', value: null | number | number[]): void;
  (
    e: 'change',
    value: null | number | number[],
    users: null | UserDTO | UserDTO[],
  ): void;
}>();

// 数据
const loading = ref(false);
const users = ref<UserDTO[]>([]);
const departments = ref<DepartmentDTO[]>([]);

// 当前选中值
const selectedValue = computed({
  get: () => props.value,
  set: (val) => {
    emit('update:value', val ?? null);
    if (props.multiple && Array.isArray(val)) {
      const selectedUsers = users.value.filter((u) => val.includes(u.id));
      emit('change', val, selectedUsers);
    } else {
      const user = users.value.find((u) => u.id === val) || null;
      emit('change', val ?? null, user);
    }
  },
});

// 加载数据
async function loadData() {
  loading.value = true;
  try {
    const [userRes, deptRes] = await Promise.all([
      getUserSelectOptions({
        pageNum: 1,
        pageSize: 500,
        status: props.userStatus,
      }),
      getDepartmentTreePublic(),
    ]);
    users.value = userRes.list || [];
    departments.value = deptRes || [];
  } catch (error) {
    console.error('加载用户数据失败:', error);
  } finally {
    loading.value = false;
  }
}

// 按部门分组的用户树形数据
const treeData = computed(() => {
  if (departments.value.length === 0 || users.value.length === 0) {
    return [];
  }

  // 过滤排除的用户
  const filteredUsers = users.value.filter(
    (u) => !props.excludeUserIds?.includes(u.id),
  );

  // 构建部门ID到用户的映射
  const deptUserMap = new Map<number, UserDTO[]>();
  filteredUsers.forEach((user) => {
    const deptId = user.departmentId || 0;
    if (!deptUserMap.has(deptId)) {
      deptUserMap.set(deptId, []);
    }
    deptUserMap.get(deptId)!.push(user);
  });

  // 递归构建树形结构
  function buildTree(depts: DepartmentDTO[]): any[] {
    const result: any[] = [];

    for (const dept of depts) {
      const deptUsers = deptUserMap.get(dept.id) || [];
      const children: any[] = [];

      // 添加子部门
      if (dept.children && dept.children.length > 0) {
        children.push(...buildTree(dept.children));
      }

      // 添加该部门的用户
      deptUsers.forEach((user) => {
        const displayName = `${user.realName}${user.position ? ` (${user.position})` : ''}`;
        children.push({
          title: displayName,
          value: user.id,
          key: user.id, // key 和 value 保持一致，避免 Ant Design Vue 警告
          label: displayName, // 添加 label 字段，用于多选时显示
          isLeaf: true,
        });
      });

      // 只添加有子项的部门
      if (children.length > 0) {
        result.push({
          title: dept.name,
          value: `dept-${dept.id}`,
          key: `dept-${dept.id}`,
          selectable: false,
          children,
        });
      }
    }

    return result;
  }

  return buildTree(departments.value);
});

// 搜索过滤
function filterTreeNode(inputValue: string, treeNode: any): boolean {
  if (!inputValue) return true;
  const title = treeNode.title?.toLowerCase() || '';
  return title.includes(inputValue.toLowerCase());
}

// 数据是否已加载完成
const dataReady = computed(
  () => users.value.length > 0 && departments.value.length > 0,
);

// 是否启用虚拟滚动（用户数量超过阈值时启用）
const enableVirtual = computed(() => {
  return users.value.length >= props.virtualThreshold;
});

// 标记是否正在更新值，防止无限循环
let isUpdatingValue = false;

// 监听数据加载完成，确保 TreeSelect 能正确显示标签
watch(
  [dataReady, () => props.value],
  ([ready, value]) => {
    // 防止无限循环
    if (isUpdatingValue) return;

    if (ready && props.multiple && Array.isArray(value) && value.length > 0) {
      // 数据加载完成且有多选值，等待下一个 tick 让 TreeSelect 更新显示
      nextTick(() => {
        // 通过重新设置值来触发 TreeSelect 更新显示
        if (value && value.length > 0) {
          isUpdatingValue = true;
          emit('update:value', [...value]);
          // 下一个 tick 后重置标记
          nextTick(() => {
            isUpdatingValue = false;
          });
        }
      });
    }
  },
  { immediate: false },
);

onMounted(() => {
  loadData();
});

// 暴露刷新方法
defineExpose({
  refresh: loadData,
});
</script>

<template>
  <TreeSelect
    v-model:value="selectedValue"
    :tree-data="treeData"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :disabled="disabled"
    :loading="loading"
    :style="{ width: '100%', ...(typeof style === 'object' ? style : {}) }"
    show-search
    :tree-default-expand-all="!enableVirtual"
    :filter-tree-node="filterTreeNode"
    :dropdown-style="{ maxHeight: `${listHeight}px`, overflow: 'auto' }"
    :multiple="multiple"
    :tree-checkable="multiple"
    :max-tag-count="multiple ? 3 : undefined"
    tree-node-label-prop="title"
    :virtual="enableVirtual"
    :list-height="listHeight"
  />
</template>
