/**
 * 共享类型定义
 * 避免在多个模块中重复定义相同的类型
 */

/**
 * 分页结果通用类型
 */
export interface PageResult<T> {
  total: number;
  list: T[];
  pageNum: number;
  pageSize: number;
  pages: number;
}

/**
 * 分页查询参数
 */
export interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

/**
 * 通用ID类型
 */
export type ID = number | string;
