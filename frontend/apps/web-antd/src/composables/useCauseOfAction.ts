/**
 * 案由数据 Composable
 * 从后端 API 获取案由数据，提供缓存和转换功能
 */
import { computed, ref, shallowRef } from 'vue';

import { requestClient } from '#/api/request';

/** 案由类型 */
export type CauseType =
  | 'ADMINISTRATIVE'
  | 'CIVIL'
  | 'CRIMINAL'
  | 'LABOR_ARBITRATION';

/** 案由树节点 */
export interface CauseTreeNode {
  code: string;
  name: string;
  level: number;
  children?: CauseTreeNode[];
}

/** 级联选择器选项 */
export interface CascaderOption {
  value: string;
  label: string;
  children?: CascaderOption[];
}

// 缓存
const civilCausesCache = shallowRef<CauseTreeNode[]>([]);
const criminalChargesCache = shallowRef<CauseTreeNode[]>([]);
const adminCausesCache = shallowRef<CauseTreeNode[]>([]);
// 正在进行的请求 Promise 缓存（用于防止并发重复请求）
const pendingRequests: Record<string, Promise<CauseTreeNode[]> | null> = {
  admin: null,
  civil: null,
  criminal: null,
};

/**
 * 从后端获取案由树
 */
async function fetchCauseTree(
  type: 'admin' | 'civil' | 'criminal',
): Promise<CauseTreeNode[]> {
  const response = await requestClient.get<CauseTreeNode[]>(
    `/causes/${type}/tree`,
  );
  return response || [];
}

/**
 * 获取民事案由
 */
export async function getCivilCauses(): Promise<CauseTreeNode[]> {
  if (civilCausesCache.value.length > 0) {
    return civilCausesCache.value;
  }
  // 如果有正在进行的请求，复用它（避免并发重复请求）
  if (pendingRequests.civil) {
    return pendingRequests.civil;
  }
  // 创建新请求并缓存 Promise
  pendingRequests.civil = fetchCauseTree('civil').then((data) => {
    civilCausesCache.value = data;
    return data;
  }).finally(() => {
    pendingRequests.civil = null;
  });
  return pendingRequests.civil;
}

/**
 * 获取刑事罪名
 */
export async function getCriminalCharges(): Promise<CauseTreeNode[]> {
  if (criminalChargesCache.value.length > 0) {
    return criminalChargesCache.value;
  }
  // 如果有正在进行的请求，复用它（避免并发重复请求）
  if (pendingRequests.criminal) {
    return pendingRequests.criminal;
  }
  // 创建新请求并缓存 Promise
  pendingRequests.criminal = fetchCauseTree('criminal').then((data) => {
    criminalChargesCache.value = data;
    return data;
  }).finally(() => {
    pendingRequests.criminal = null;
  });
  return pendingRequests.criminal;
}

/**
 * 获取行政案由
 */
export async function getAdminCauses(): Promise<CauseTreeNode[]> {
  if (adminCausesCache.value.length > 0) {
    return adminCausesCache.value;
  }
  // 如果有正在进行的请求，复用它（避免并发重复请求）
  if (pendingRequests.admin) {
    return pendingRequests.admin;
  }
  // 创建新请求并缓存 Promise
  pendingRequests.admin = fetchCauseTree('admin').then((data) => {
    adminCausesCache.value = data;
    return data;
  }).finally(() => {
    pendingRequests.admin = null;
  });
  return pendingRequests.admin;
}

/**
 * 根据案件类型获取案由数据
 */
export async function getCausesByType(
  type: CauseType,
): Promise<CauseTreeNode[]> {
  switch (type) {
    case 'ADMINISTRATIVE': {
      return getAdminCauses();
    }
    case 'CIVIL': {
      return getCivilCauses();
    }
    case 'CRIMINAL': {
      return getCriminalCharges();
    }
    case 'LABOR_ARBITRATION': {
      // 劳动仲裁使用民事案由中的劳动争议部分
      const civil = await getCivilCauses();
      return civil.filter((c) => c.code === 'P19'); // P19 是劳动争议
    }
    default: {
      return [];
    }
  }
}

/**
 * 将案由数据转换为级联选择器选项
 */
export function causesToCascaderOptions(
  categories: CauseTreeNode[],
): CascaderOption[] {
  const convertNode = (node: CauseTreeNode): CascaderOption => ({
    value: node.code,
    label: node.name,
    children: node.children?.map((n) => convertNode(n)),
  });

  return categories.map((c) => convertNode(c));
}

/**
 * 将案由数据转换为树形选择器选项
 */
export function causesToTreeSelectOptions(categories: CauseTreeNode[]) {
  const convertNode = (node: CauseTreeNode): any => ({
    key: node.code,
    value: node.code,
    title: node.name,
    children: node.children?.map((n) => convertNode(n)),
  });

  return categories.map((c) => convertNode(c));
}

/**
 * 在案由树中查找名称
 */
function findNameInTree(
  nodes: CauseTreeNode[],
  code: string,
): string | undefined {
  for (const node of nodes) {
    if (node.code === code) return node.name;
    if (node.children) {
      const found = findNameInTree(node.children, code);
      if (found) return found;
    }
  }
  return undefined;
}

/**
 * 根据代码查找案由名称（在所有类型中查找）
 */
export async function findCauseNameByCode(
  code: string,
): Promise<string | undefined> {
  // 依次在缓存中查找
  const allCauses = [
    ...civilCausesCache.value,
    ...criminalChargesCache.value,
    ...adminCausesCache.value,
  ];

  let found = findNameInTree(allCauses, code);
  if (found) return found;

  // 如果缓存为空，尝试加载
  if (civilCausesCache.value.length === 0) {
    await getCivilCauses();
    found = findNameInTree(civilCausesCache.value, code);
    if (found) return found;
  }
  if (criminalChargesCache.value.length === 0) {
    await getCriminalCharges();
    found = findNameInTree(criminalChargesCache.value, code);
    if (found) return found;
  }
  if (adminCausesCache.value.length === 0) {
    await getAdminCauses();
    found = findNameInTree(adminCausesCache.value, code);
    if (found) return found;
  }

  return undefined;
}

/**
 * 同步版本：在已加载的缓存中查找案由名称
 * 如果缓存为空，返回 undefined
 */
export function findCauseNameInAll(code: string): string | undefined {
  if (!code) return undefined;

  // 如果已经是中文名称，直接返回
  if (/[\u4E00-\u9FA5]/.test(code)) {
    return code;
  }

  const allCauses = [
    ...civilCausesCache.value,
    ...criminalChargesCache.value,
    ...adminCausesCache.value,
  ];

  return findNameInTree(allCauses, code);
}

/** 案件类型选项 */
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
  {
    value: 'CIVIL',
    label: '民事案件',
    matterType: 'LITIGATION',
    hasCause: true,
    causeType: 'CIVIL' as CauseType,
  },
  {
    value: 'CRIMINAL',
    label: '刑事案件',
    matterType: 'LITIGATION',
    hasCause: true,
    causeType: 'CRIMINAL' as CauseType,
  },
  {
    value: 'ADMINISTRATIVE',
    label: '行政案件',
    matterType: 'LITIGATION',
    hasCause: true,
    causeType: 'ADMINISTRATIVE' as CauseType,
  },
  {
    value: 'BANKRUPTCY',
    label: '破产案件',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'IP',
    label: '知识产权案件',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'COMMERCIAL_ARBITRATION',
    label: '商事仲裁',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'LABOR_ARBITRATION',
    label: '劳动仲裁',
    matterType: 'LITIGATION',
    hasCause: true,
    causeType: 'LABOR_ARBITRATION' as CauseType,
  },
  {
    value: 'ENFORCEMENT',
    label: '执行案件',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  // 国家赔偿类
  {
    value: 'STATE_COMP_ADMIN',
    label: '行政国家赔偿',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'STATE_COMP_CRIMINAL',
    label: '刑事国家赔偿',
    matterType: 'LITIGATION',
    hasCause: false,
    causeType: null,
  },
  // 非诉类
  {
    value: 'LEGAL_COUNSEL',
    label: '法律顾问',
    matterType: 'NON_LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'SPECIAL_SERVICE',
    label: '专项服务',
    matterType: 'NON_LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'DUE_DILIGENCE',
    label: '尽职调查',
    matterType: 'NON_LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'CONTRACT_REVIEW',
    label: '合同审查',
    matterType: 'NON_LITIGATION',
    hasCause: false,
    causeType: null,
  },
  {
    value: 'LEGAL_OPINION',
    label: '法律意见',
    matterType: 'NON_LITIGATION',
    hasCause: false,
    causeType: null,
  },
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

/**
 * 预加载所有案由数据
 */
export async function preloadAllCauses(): Promise<void> {
  await Promise.all([getCivilCauses(), getCriminalCharges(), getAdminCauses()]);
}

/**
 * Composable: 使用案由数据
 */
export function useCauseOfAction() {
  const loading = ref(false);
  const causes = shallowRef<CauseTreeNode[]>([]);

  const cascaderOptions = computed(() => causesToCascaderOptions(causes.value));
  const treeSelectOptions = computed(() =>
    causesToTreeSelectOptions(causes.value),
  );

  async function loadCauses(type: CauseType) {
    loading.value = true;
    try {
      causes.value = await getCausesByType(type);
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    causes,
    cascaderOptions,
    treeSelectOptions,
    loadCauses,
    findCauseNameInAll,
    getCausesByType,
    causesToCascaderOptions,
    needsCauseOfAction,
    getCauseTypeByCase,
  };
}
