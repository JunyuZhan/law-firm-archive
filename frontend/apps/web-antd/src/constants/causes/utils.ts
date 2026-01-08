/**
 * 案由数据工具函数
 */
import type { CauseCategory, CauseItem, CauseType } from './types';
import { CIVIL_CAUSES, CIVIL_PART6_LABOR } from './civil-causes';
import { CRIMINAL_CHARGES } from './criminal-charges';
import { ADMIN_CAUSES } from './admin-causes';

/** 根据案件类型获取案由数据 */
export function getCausesByType(type: CauseType): CauseCategory[] {
  switch (type) {
    case 'CIVIL':
      return CIVIL_CAUSES;
    case 'CRIMINAL':
      return CRIMINAL_CHARGES;
    case 'ADMINISTRATIVE':
      return ADMIN_CAUSES;
    case 'LABOR_ARBITRATION':
      // 劳动仲裁使用民事案由中的劳动争议
      return [CIVIL_PART6_LABOR];
    default:
      return [];
  }
}

/** 将案由数据转换为级联选择器选项 (Cascader) */
export function causesToCascaderOptions(categories: CauseCategory[]) {
  const convertItem = (item: CauseItem): any => ({
    value: item.code,
    label: item.name,
    children: item.children?.map(convertItem),
  });

  return categories.map((cat) => ({
    value: cat.code,
    label: cat.name,
    children: cat.causes.map(convertItem),
  }));
}

/** 将案由数据转换为树形选择器选项 (TreeSelect) */
export function causesToTreeSelectOptions(categories: CauseCategory[]) {
  const convertItem = (item: CauseItem): any => ({
    key: item.code,
    value: item.code,
    title: item.name,
    children: item.children?.map(convertItem),
  });

  return categories.map((cat) => ({
    key: cat.code,
    value: cat.code,
    title: cat.name,
    children: cat.causes.map(convertItem),
  }));
}

/** 将案由数据扁平化为下拉选项 (Select) - 只取最末级 */
export function causesToSelectOptions(categories: CauseCategory[]) {
  const options: { value: string; label: string; category: string }[] = [];

  const flattenItem = (item: CauseItem, categoryName: string) => {
    if (item.children && item.children.length > 0) {
      item.children.forEach((child) => flattenItem(child, categoryName));
    } else {
      options.push({
        value: item.code,
        label: item.name,
        category: categoryName,
      });
    }
  };

  categories.forEach((cat) => {
    cat.causes.forEach((cause) => flattenItem(cause, cat.name));
  });

  return options;
}

/** 根据代码查找案由名称 */
export function findCauseNameByCode(
  categories: CauseCategory[],
  code: string,
): string | undefined {
  const findInItem = (item: CauseItem): string | undefined => {
    if (item.code === code) return item.name;
    if (item.children) {
      for (const child of item.children) {
        const found = findInItem(child);
        if (found) return found;
      }
    }
    return undefined;
  };

  for (const cat of categories) {
    if (cat.code === code) return cat.name;
    for (const cause of cat.causes) {
      const found = findInItem(cause);
      if (found) return found;
    }
  }
  return undefined;
}

/** 在所有案由中查找名称（自动判断类型） */
export function findCauseNameInAll(code: string): string | undefined {
  // 依次在民事、刑事、行政案由中查找
  const allCategories = [...CIVIL_CAUSES, ...CRIMINAL_CHARGES, ...ADMIN_CAUSES];
  return findCauseNameByCode(allCategories, code);
}

/** 案件类型选项（用于案由选择） */
export const CASE_TYPE_OPTIONS = [
  { value: 'CIVIL', label: '民事案件' },
  { value: 'CRIMINAL', label: '刑事案件' },
  { value: 'ADMINISTRATIVE', label: '行政案件' },
];

/** 项目大类选项 */
export const MATTER_TYPE_OPTIONS = [
  { value: 'LITIGATION', label: '诉讼案件' },
  { value: 'NON_LITIGATION', label: '非诉项目' },
];

/** 案件细分类型选项 */
export const CASE_CATEGORY_OPTIONS = [
  // 诉讼类
  { value: 'CIVIL', label: '民事案件', matterType: 'LITIGATION', hasCause: true, causeType: 'CIVIL' as CauseType },
  { value: 'CRIMINAL', label: '刑事案件', matterType: 'LITIGATION', hasCause: true, causeType: 'CRIMINAL' as CauseType },
  { value: 'ADMINISTRATIVE', label: '行政案件', matterType: 'LITIGATION', hasCause: true, causeType: 'ADMINISTRATIVE' as CauseType },
  { value: 'BANKRUPTCY', label: '破产案件', matterType: 'LITIGATION', hasCause: false, causeType: null },
  { value: 'IP', label: '知识产权案件', matterType: 'LITIGATION', hasCause: false, causeType: null },
  { value: 'COMMERCIAL_ARBITRATION', label: '商事仲裁', matterType: 'LITIGATION', hasCause: false, causeType: null },
  { value: 'LABOR_ARBITRATION', label: '劳动仲裁', matterType: 'LITIGATION', hasCause: true, causeType: 'LABOR_ARBITRATION' as CauseType },
  { value: 'ENFORCEMENT', label: '执行案件', matterType: 'LITIGATION', hasCause: false, causeType: null },
  // 非诉类
  { value: 'LEGAL_COUNSEL', label: '法律顾问', matterType: 'NON_LITIGATION', hasCause: false, causeType: null },
  { value: 'SPECIAL_SERVICE', label: '专项服务', matterType: 'NON_LITIGATION', hasCause: false, causeType: null },
  { value: 'DUE_DILIGENCE', label: '尽职调查', matterType: 'NON_LITIGATION', hasCause: false, causeType: null },
  { value: 'CONTRACT_REVIEW', label: '合同审查', matterType: 'NON_LITIGATION', hasCause: false, causeType: null },
  { value: 'LEGAL_OPINION', label: '法律意见', matterType: 'NON_LITIGATION', hasCause: false, causeType: null },
];

/** 根据项目大类筛选案件类型 */
export function getCaseCategoryByMatterType(matterType: string) {
  return CASE_CATEGORY_OPTIONS.filter((opt) => opt.matterType === matterType);
}

/** 判断案件类型是否需要选择案由 */
export function needsCauseOfAction(caseType: string): boolean {
  const category = CASE_CATEGORY_OPTIONS.find((opt) => opt.value === caseType);
  return category?.hasCause ?? false;
}

/** 根据案件类型获取对应的案由类型 */
export function getCauseTypeByCase(caseType: string): CauseType | null {
  const category = CASE_CATEGORY_OPTIONS.find((opt) => opt.value === caseType);
  return category?.causeType ?? null;
}
