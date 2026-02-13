package com.lawfirm.application.system.dto;

import lombok.Data;

/** 数据交接查询DTO */
@Data
public class DataHandoverQueryDTO {

  /** 默认页码 */
  private static final int DEFAULT_PAGE_NUM = 1;

  /** 默认每页大小 */
  private static final int DEFAULT_PAGE_SIZE = 10;

  /** 最大每页大小 */
  private static final int MAX_PAGE_SIZE = 100;

  /** 页码 */
  private Integer pageNum = DEFAULT_PAGE_NUM;

  /** 每页大小 */
  private Integer pageSize = DEFAULT_PAGE_SIZE;

  /** 移交人ID */
  private Long fromUserId;

  /** 接收人ID */
  private Long toUserId;

  /** 交接类型 */
  private String handoverType;

  /** 状态 */
  private String status;

  /**
   * 获取安全的页码（防止 NPE）
   *
   * @return 页码，至少为 1
   */
  public int getSafePageNum() {
    return pageNum != null && pageNum > 0 ? pageNum : DEFAULT_PAGE_NUM;
  }

  /**
   * 获取安全的每页大小（防止 NPE 和超大值）
   *
   * @return 每页大小，范围 1-100
   */
  public int getSafePageSize() {
    if (pageSize == null || pageSize <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(pageSize, MAX_PAGE_SIZE);
  }

  /**
   * 获取偏移量
   *
   * @return 分页偏移量
   */
  public int getOffset() {
    return (getSafePageNum() - 1) * getSafePageSize();
  }
}
