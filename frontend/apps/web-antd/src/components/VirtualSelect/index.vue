<script setup lang="ts">
/**
 * 虚拟滚动远程搜索选择器
 * 支持大数据量下拉选择，使用虚拟滚动 + 远程搜索优化性能
 */
import type { SelectProps } from 'ant-design-vue';

import { computed, ref, watch } from 'vue';

import { Select, Spin } from 'ant-design-vue';
import { useDebounceFn } from '@vueuse/core';

export interface VirtualSelectOption {
  label: string;
  value: number | string;
  disabled?: boolean;
  [key: string]: any;
}

const props = withDefaults(
  defineProps<{
    /** 选中的值 */
    value?: null | number | number[] | string | string[];
    /** 占位文本 */
    placeholder?: string;
    /** 是否允许清除 */
    allowClear?: boolean;
    /** 是否禁用 */
    disabled?: boolean;
    /** 是否多选 */
    multiple?: boolean;
    /** 搜索防抖延迟(ms) */
    debounceTime?: number;
    /** 初始选项（用于回显） */
    initialOptions?: VirtualSelectOption[];
    /** 远程搜索函数 */
    fetchOptions?: (keyword: string) => Promise<VirtualSelectOption[]>;
    /** 静态选项（不需要远程搜索时使用） */
    options?: VirtualSelectOption[];
    /** 虚拟滚动列表高度 */
    listHeight?: number;
    /** 最大标签数量（多选时） */
    maxTagCount?: number | 'responsive';
    /** 样式 */
    style?: Record<string, any> | string;
    /** 是否显示搜索框 */
    showSearch?: boolean;
    /** 下拉菜单样式 */
    dropdownStyle?: Record<string, any>;
    /** 选项过滤函数（静态选项时使用） */
    filterOption?: SelectProps['filterOption'];
    /** 虚拟滚动阈值 */
    virtualThreshold?: number;
  }>(),
  {
    placeholder: '请选择',
    allowClear: true,
    disabled: false,
    multiple: false,
    debounceTime: 300,
    listHeight: 256,
    maxTagCount: 'responsive',
    showSearch: true,
    virtualThreshold: 50,
  },
);

const emit = defineEmits<{
  (e: 'update:value', value: any): void;
  (e: 'change', value: any, option: any): void;
  (e: 'search', keyword: string): void;
}>();

// 状态
const loading = ref(false);
const searchKeyword = ref('');
const remoteOptions = ref<VirtualSelectOption[]>([]);

// 计算选中值
const selectedValue = computed({
  get: () => props.value,
  set: (val) => {
    emit('update:value', val ?? null);
  },
});

// 是否使用远程搜索
const useRemoteSearch = computed(() => !!props.fetchOptions);

// 合并选项：远程选项 + 初始选项（用于回显已选中的值）
const mergedOptions = computed(() => {
  if (!useRemoteSearch.value) {
    return props.options || [];
  }

  // 合并远程搜索结果和初始选项（去重）
  const optionsMap = new Map<string | number, VirtualSelectOption>();

  // 先添加初始选项（用于回显）
  props.initialOptions?.forEach((opt) => {
    optionsMap.set(opt.value, opt);
  });

  // 再添加远程选项（覆盖同value的初始选项）
  remoteOptions.value.forEach((opt) => {
    optionsMap.set(opt.value, opt);
  });

  return [...optionsMap.values()];
});

// 是否启用虚拟滚动
const enableVirtual = computed(() => {
  const totalOptions = useRemoteSearch.value
    ? mergedOptions.value.length
    : (props.options?.length || 0);
  return totalOptions >= props.virtualThreshold;
});

// 远程搜索
const handleSearch = useDebounceFn(async (keyword: string) => {
  searchKeyword.value = keyword;
  emit('search', keyword);

  if (!props.fetchOptions) return;

  loading.value = true;
  try {
    const results = await props.fetchOptions(keyword);
    remoteOptions.value = results;
  } catch (error) {
    console.error('远程搜索失败:', error);
    remoteOptions.value = [];
  } finally {
    loading.value = false;
  }
}, props.debounceTime);

// 选择变化
function handleChange(value: any, option: any) {
  emit('change', value, option);
}

// 下拉框展开时，如果是远程搜索且没有数据，触发一次空搜索
function handleDropdownVisibleChange(open: boolean) {
  if (open && useRemoteSearch.value && remoteOptions.value.length === 0) {
    handleSearch('');
  }
}

// 监听初始选项变化，更新远程选项
watch(
  () => props.initialOptions,
  (newOptions) => {
    if (newOptions && newOptions.length > 0 && remoteOptions.value.length === 0) {
      // 如果有初始选项且远程选项为空，使用初始选项
      remoteOptions.value = [...newOptions];
    }
  },
  { immediate: true },
);

// 暴露方法
defineExpose({
  refresh: () => handleSearch(searchKeyword.value),
  clear: () => {
    remoteOptions.value = [];
    searchKeyword.value = '';
  },
});
</script>

<template>
  <Select
    v-model:value="selectedValue"
    :placeholder="placeholder"
    :allow-clear="allowClear"
    :disabled="disabled"
    :mode="multiple ? 'multiple' : undefined"
    :show-search="showSearch"
    :loading="loading"
    :options="mergedOptions"
    :virtual="enableVirtual"
    :list-height="listHeight"
    :max-tag-count="maxTagCount"
    :style="{ width: '100%', ...(typeof style === 'object' ? style : {}) }"
    :dropdown-style="dropdownStyle"
    :filter-option="useRemoteSearch ? false : filterOption"
    :not-found-content="loading ? undefined : '暂无数据'"
    @search="useRemoteSearch ? handleSearch : undefined"
    @change="handleChange"
    @dropdownVisibleChange="handleDropdownVisibleChange"
  >
    <template v-if="loading" #notFoundContent>
      <div style="text-align: center; padding: 12px">
        <Spin size="small" />
        <span style="margin-left: 8px">搜索中...</span>
      </div>
    </template>
  </Select>
</template>
