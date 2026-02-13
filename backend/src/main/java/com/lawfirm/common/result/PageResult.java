package com.lawfirm.common.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应结果
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

  /** 总记录数 */
  private long total;

  /** 当前页数据 */
  private List<T> list;

  /** 当前页码 */
  private int pageNum;

  /** 每页条数 */
  private int pageSize;

  /** 总页数 */
  private int pages;

  /**
   * 获取记录列表（兼容方法）
   *
   * @return 数据列表
   */
  public List<T> getRecords() {
    return list;
  }

  /**
   * 创建分页结果
   *
   * @param <T> 数据类型
   * @param list 数据列表
   * @param total 总记录数
   * @param pageNum 当前页码
   * @param pageSize 每页条数
   * @return 分页结果
   */
  public static <T> PageResult<T> of(
      final List<T> list, final long total, final int pageNum, final int pageSize) {
    PageResult<T> result = new PageResult<>();
    result.list = list;
    result.total = total;
    result.pageNum = pageNum;
    result.pageSize = pageSize;
    // 防止除零错误（pageSize <= 0 时返回 0 页或 1 页）
    result.pages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : (total > 0 ? 1 : 0);
    return result;
  }

  /**
   * 返回空的分页结果
   *
   * @param <T> 数据类型
   * @return 空分页结果
   */
  public static <T> PageResult<T> empty() {
    return of(java.util.Collections.emptyList(), 0, 1, 10);
  }
}
