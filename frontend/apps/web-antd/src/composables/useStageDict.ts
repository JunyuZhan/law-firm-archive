/**
 * 代理阶段字典映射 Composable
 * 统一管理案件类型与代理阶段字典的映射关系
 */
import { ref } from 'vue';

import { getDictDataByCode } from '#/api/system';

/**
 * 案件类型到代理阶段字典的映射
 * 统一维护，避免多处重复定义
 */
export const CASE_TYPE_TO_STAGE_DICT: Record<string, string> = {
  CIVIL: 'litigation_stage_civil',
  CRIMINAL: 'litigation_stage_criminal',
  ADMINISTRATIVE: 'litigation_stage_administrative',
  LABOR_ARBITRATION: 'litigation_stage_labor_arbitration',
  COMMERCIAL_ARBITRATION: 'litigation_stage_commercial_arbitration',
  ENFORCEMENT: 'litigation_stage_enforcement',
  STATE_COMP_ADMIN: 'litigation_stage_state_comp_admin',
  STATE_COMP_CRIMINAL: 'litigation_stage_state_comp_criminal',
};

/**
 * 根据案件类型获取对应的字典编码
 */
export function getStageDictCode(caseType: string): string {
  return CASE_TYPE_TO_STAGE_DICT[caseType] || 'litigation_stage_default';
}

/**
 * 代理阶段选项接口
 */
export interface StageOption {
  value: string;
  label: string;
}

/**
 * 使用代理阶段字典
 */
export function useStageDict() {
  const loading = ref(false);
  const stageOptions = ref<StageOption[]>([]);

  /**
   * 根据案件类型加载代理阶段选项
   */
  async function loadStageOptions(caseType: string): Promise<void> {
    if (!caseType) {
      stageOptions.value = [];
      return;
    }

    const dictCode = getStageDictCode(caseType);
    loading.value = true;

    try {
      const items = await getDictDataByCode(dictCode);
      stageOptions.value = items.map((item) => ({
        value: item.value,
        label: item.label,
      }));
    } catch (error) {
      console.error('加载代理阶段字典失败:', error);
      stageOptions.value = [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * 清空代理阶段选项
   */
  function clearStageOptions(): void {
    stageOptions.value = [];
  }

  return {
    loading,
    stageOptions,
    loadStageOptions,
    clearStageOptions,
    getStageDictCode,
  };
}
