/**
 * 案由数据类型定义
 */

export interface CauseItem {
  code: string;
  name: string;
  children?: CauseItem[];
}

export interface CauseCategory {
  code: string;
  name: string;
  causes: CauseItem[];
}

export type CauseType = 'CIVIL' | 'CRIMINAL' | 'ADMINISTRATIVE' | 'LABOR_ARBITRATION';
