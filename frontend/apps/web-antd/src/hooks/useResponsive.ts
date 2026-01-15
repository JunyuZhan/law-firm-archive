import { breakpointsTailwind, useBreakpoints } from '@vueuse/core';
import { computed } from 'vue';

/**
 * 响应式布局工具函数
 * 提供移动端适配所需的断点检测和布局建议
 */
export function useResponsive() {
  const breakpoints = useBreakpoints(breakpointsTailwind);
  
  // 移动端检测 (< 768px)
  const isMobile = breakpoints.smaller('md');

  // 断点状态
  const isXs = breakpoints.smaller('sm'); // < 640px
  const isSm = breakpoints.between('sm', 'md'); // 640-768px
  const isMd = breakpoints.between('md', 'lg'); // 768-1024px
  const isLg = breakpoints.between('lg', 'xl'); // 1024-1280px
  const isXl = breakpoints.greaterOrEqual('xl'); // >= 1280px

  // 设备类型
  const isTablet = breakpoints.between('md', 'lg');
  const isDesktop = breakpoints.greaterOrEqual('lg');

  // 表格列数建议
  const suggestedColumns = computed(() => {
    if (isXs.value) return 1;
    if (isSm.value) return 2;
    if (isMd.value) return 3;
    return 4;
  });

  // Modal 宽度建议
  const modalWidth = computed(() => {
    if (isMobile.value) return '100%';
    if (isTablet.value) return '80%';
    return 720;
  });

  // Drawer 宽度建议
  const drawerWidth = computed(() => {
    if (isMobile.value) return '100%';
    if (isTablet.value) return '70%';
    return 600;
  });

  // 表单布局建议
  const formLayout = computed(() => {
    return isMobile.value ? 'vertical' : 'horizontal';
  });

  // 表单栅格配置
  const formColSpan = computed(() => ({
    xs: 24,
    sm: 24,
    md: 12,
    lg: 8,
    xl: 8,
  }));

  // 统计卡片栅格配置
  const statCardColSpan = computed(() => ({
    xs: 12,
    sm: 12,
    md: 6,
    lg: 6,
    xl: 6,
  }));

  // 列表卡片栅格配置
  const listCardColSpan = computed(() => ({
    xs: 24,
    sm: 24,
    md: 12,
    lg: 8,
    xl: 6,
  }));

  // 按钮尺寸建议
  const buttonSize = computed(() => {
    return isMobile.value ? 'middle' : 'middle';
  });

  // 表格行高建议
  const tableRowHeight = computed(() => {
    return isMobile.value ? 'small' : 'default';
  });

  return {
    // 断点状态
    isMobile,
    isXs,
    isSm,
    isMd,
    isLg,
    isXl,
    isTablet,
    isDesktop,
    // 布局建议
    suggestedColumns,
    modalWidth,
    drawerWidth,
    formLayout,
    formColSpan,
    statCardColSpan,
    listCardColSpan,
    buttonSize,
    tableRowHeight,
  };
}

/**
 * 获取响应式类名
 * @param baseClass 基础类名
 * @param mobileClass 移动端类名
 * @param isMobile 是否移动端
 */
export function getResponsiveClass(
  baseClass: string,
  mobileClass: string,
  isMobile: boolean,
): string {
  return isMobile ? `${baseClass} ${mobileClass}` : baseClass;
}
