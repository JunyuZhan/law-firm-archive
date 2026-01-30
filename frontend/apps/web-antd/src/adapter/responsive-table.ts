import type { Ref } from 'vue';

import type { VxeGridProps } from '#/adapter/vxe-table';

import { computed } from 'vue';

import { useResponsive } from '#/hooks/useResponsive';

/**
 * 表格列配置扩展类型
 */
export interface ResponsiveColumnConfig {
  /** 列字段 */
  field?: string;
  /** 列标题 */
  title?: string;
  /** 列宽度 */
  width?: number | string;
  /** 最小宽度 */
  minWidth?: number | string;
  /** 移动端是否显示 */
  mobileShow?: boolean;
  /** 移动端优先级（数字越小越优先显示） */
  mobilePriority?: number;
  /** 列类型 (seq, checkbox, radio 等) */
  type?: string;
  /** 插槽配置 */
  slots?: { [key: string]: string | undefined; default?: string };
  /** 其他属性 */
  [key: string]: any;
}

/**
 * 响应式表格配置
 */
export interface ResponsiveGridOptions {
  /** 移动端最大显示列数 */
  mobileMaxColumns?: number;
  /** 平板端最大显示列数 */
  tabletMaxColumns?: number;
  /** 是否在移动端启用卡片视图 */
  enableMobileCardView?: boolean;
}

/**
 * 响应式表格适配器
 * 根据屏幕尺寸自动调整表格列的显示
 *
 * @param baseColumns 基础列配置
 * @param options 响应式配置选项
 */
export function useResponsiveGrid(
  baseColumns: Ref<ResponsiveColumnConfig[]> | ResponsiveColumnConfig[],
  options: ResponsiveGridOptions = {},
) {
  const { isMobile, isTablet } = useResponsive();
  const {
    mobileMaxColumns = 4,
    tabletMaxColumns = 6,
    enableMobileCardView = false,
  } = options;

  // 获取原始列配置
  const getBaseColumns = () => {
    if ('value' in baseColumns) {
      return baseColumns.value;
    }
    return baseColumns;
  };

  // 响应式列配置
  const responsiveColumns = computed(() => {
    const columns = getBaseColumns();
    if (!columns) return [];

    // 桌面端显示所有列
    if (!isMobile.value && !isTablet.value) {
      return columns;
    }

    // 移动端/平板端筛选列
    const maxColumns = isMobile.value ? mobileMaxColumns : tabletMaxColumns;

    // 必须保留的列类型
    const mustShowTypes = new Set(['checkbox', 'radio', 'seq']);

    // 筛选和排序列
    const filteredColumns = columns
      .map((col, index) => ({
        ...col,
        _originalIndex: index,
        _priority: getMobilePriority(col),
      }))
      .filter((col) => {
        // 必须显示的列类型
        if (mustShowTypes.has(col.type as string)) return true;
        // 操作列必须显示
        if (col.field === 'action' || col.slots?.default === 'action')
          return true;
        // 明确标记移动端显示
        if (col.mobileShow === true) return true;
        // 明确标记不显示
        if (col.mobileShow === false) return false;
        // 根据优先级决定
        return col._priority !== undefined;
      })
      .toSorted((a, b) => (a._priority || 999) - (b._priority || 999))
      .slice(0, maxColumns);

    // 按原始顺序排序
    return filteredColumns
      .toSorted((a, b) => a._originalIndex - b._originalIndex)
      .map(({ _originalIndex, _priority, ...col }) => ({
        ...col,
        // 移动端调整列宽
        width: isMobile.value ? adjustMobileWidth(col.width) : col.width,
      }));
  });

  // 移动端表格配置
  const mobileGridProps = computed<Partial<VxeGridProps>>(() => {
    if (!isMobile.value) return {};

    return {
      height: 'auto',
      showOverflow: true,
      columnConfig: {
        resizable: false,
      },
      rowConfig: {
        height: 48,
      },
    };
  });

  // 是否应该使用卡片视图
  const shouldUseCardView = computed(() => {
    return enableMobileCardView && isMobile.value;
  });

  return {
    responsiveColumns,
    mobileGridProps,
    shouldUseCardView,
    isMobile,
    isTablet,
  };
}

/**
 * 获取列的移动端优先级
 * 数字越小优先级越高
 */
function getMobilePriority(col: ResponsiveColumnConfig): number | undefined {
  // 明确设置的优先级
  if (col.mobilePriority !== undefined) return col.mobilePriority;

  // 根据字段名推断优先级
  const field = col.field?.toLowerCase() || '';
  const title = col.title?.toLowerCase() || '';

  // 高优先级字段
  if (
    field.includes('name') ||
    field.includes('title') ||
    title.includes('名称')
  ) {
    return 1;
  }
  if (
    field.includes('status') ||
    field.includes('state') ||
    title.includes('状态')
  ) {
    return 2;
  }

  // 中优先级字段
  if (field.includes('type') || title.includes('类型')) return 3;
  if (field.includes('amount') || title.includes('金额')) return 4;

  // 低优先级字段（通常隐藏）
  if (field.includes('created') || field.includes('updated')) return 10;
  if (field.includes('remark') || field.includes('description')) return 11;

  return 5; // 默认优先级
}

/**
 * 调整移动端列宽
 */
function adjustMobileWidth(
  width: number | string | undefined,
): number | string | undefined {
  if (width === undefined) return undefined;
  if (typeof width === 'string') return width;

  // 移动端列宽适当缩小
  if (width > 150) return Math.floor(width * 0.7);
  if (width > 100) return Math.floor(width * 0.8);
  return width;
}

/**
 * 创建响应式列配置
 * 便捷方法，用于快速标记列的移动端显示属性
 *
 * @param columns 基础列配置
 * @param mobileFields 移动端要显示的字段名数组
 */
export function createResponsiveColumns(
  columns: ResponsiveColumnConfig[],
  mobileFields: string[],
): ResponsiveColumnConfig[] {
  return columns.map((col) => ({
    ...col,
    mobileShow: mobileFields.includes(col.field || ''),
  }));
}
