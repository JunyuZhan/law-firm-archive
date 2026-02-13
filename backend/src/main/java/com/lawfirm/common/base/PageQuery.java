package com.lawfirm.common.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/** 分页查询基类 */
@Data
public class PageQuery {

  /** 当前页码，从1开始 */
  private Integer pageNum = 1;

  /** 每页条数 */
  private Integer pageSize = 10;

  /** 排序字段 */
  private String orderBy;

  /** 排序方向：asc/desc */
  private String orderDirection = "desc";

  /**
   * 获取偏移量
   *
   * @return 分页偏移量
   */
  public int getOffset() {
    return (pageNum != null && pageSize != null) ? (pageNum - 1) * pageSize : 0;
  }

  /**
   * 获取MyBatis-Plus分页对象
   *
   * @param <T> 分页数据类型
   * @return MyBatis-Plus分页对象
   */
  public <T> Page<T> getPage() {
    return new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
  }
}
