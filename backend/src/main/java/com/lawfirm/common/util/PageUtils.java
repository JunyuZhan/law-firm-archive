package com.lawfirm.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * 分页工具类
 *
 * <p>功能： 1. 游标分页：避免深度分页性能问题 2. 分页参数校验：限制最大页数和每页大小 3. 分页结果转换：DTO 转换
 *
 * @author junyuzhan
 */
@Slf4j
public final class PageUtils {

  /** 默认每页大小 */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /** 最大每页大小 */
  public static final int MAX_PAGE_SIZE = 100;

  /** 最大页数（防止深度分页） */
  public static final int MAX_PAGE_NUM = 500;

  /** 深度分页阈值（超过此值建议使用游标分页） */
  public static final int DEEP_PAGE_THRESHOLD = 100;

  private PageUtils() {
    // 私有构造函数
  }

  /**
   * 创建安全的分页对象.
   *
   * @param pageNum 页码（从1开始）
   * @param pageSize 每页大小
   * @param <T> 数据类型
   * @return 分页对象
   */
  public static <T> Page<T> createPage(final int pageNum, final int pageSize) {
    // 页码校验
    int finalPageNum = pageNum;
    if (finalPageNum < 1) {
      finalPageNum = 1;
    }
    if (finalPageNum > MAX_PAGE_NUM) {
      log.warn("请求页码 {} 超过最大限制 {}，已自动调整", finalPageNum, MAX_PAGE_NUM);
      finalPageNum = MAX_PAGE_NUM;
    }

    // 每页大小校验
    int finalPageSize = pageSize;
    if (finalPageSize < 1) {
      finalPageSize = DEFAULT_PAGE_SIZE;
    }
    if (finalPageSize > MAX_PAGE_SIZE) {
      log.warn("每页大小 {} 超过最大限制 {}，已自动调整", finalPageSize, MAX_PAGE_SIZE);
      finalPageSize = MAX_PAGE_SIZE;
    }

    // 深度分页警告
    if (finalPageNum > DEEP_PAGE_THRESHOLD) {
      log.warn("检测到深度分页请求: pageNum={}，建议使用游标分页", finalPageNum);
    }

    return new Page<>(finalPageNum, finalPageSize);
  }

  /**
   * 创建安全的分页对象（使用默认每页大小）
   *
   * @param pageNum 页码
   * @param <T> 数据类型
   * @return 分页对象
   */
  public static <T> Page<T> createPage(final int pageNum) {
    return createPage(pageNum, DEFAULT_PAGE_SIZE);
  }

  /**
   * 转换分页结果（Entity -> DTO）.
   *
   * @param page 原始分页结果
   * @param converter 转换函数
   * @param <T> 源数据类型
   * @param <R> 目标数据类型
   * @return 转换后的分页结果
   */
  public static <T, R> IPage<R> convert(final IPage<T> page, final Function<T, R> converter) {
    Page<R> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    result.setRecords(page.getRecords().stream().map(converter).toList());
    return result;
  }

  /**
   * 检查是否为深度分页
   *
   * @param pageNum 页码
   * @return 是否为深度分页
   */
  public static boolean isDeepPage(final int pageNum) {
    return pageNum > DEEP_PAGE_THRESHOLD;
  }

  /**
   * 计算偏移量
   *
   * @param pageNum 页码（从1开始）
   * @param pageSize 每页大小
   * @return 偏移量
   */
  public static long calculateOffset(final int pageNum, final int pageSize) {
    return (long) (pageNum - 1) * pageSize;
  }

  /**
   * 计算总页数
   *
   * @param total 总记录数
   * @param pageSize 每页大小
   * @return 总页数
   */
  public static int calculateTotalPages(final long total, final int pageSize) {
    if (pageSize <= 0) {
      return 0;
    }
    return (int) Math.ceil((double) total / pageSize);
  }

  // ==================== 游标分页支持 ====================

  /**
   * 游标分页结果
   *
   * @param <T> 数据类型
   */
  public static class CursorPage<T> {
    /** 记录列表. */
    private List<T> records;

    /** 下一个游标. */
    private Long nextCursor;

    /** 是否有更多. */
    private boolean hasMore;

    /** 大小. */
    private int size;

    /**
     * 构造函数
     *
     * @param records 记录列表
     * @param nextCursor 下一个游标
     * @param hasMore 是否有更多
     */
    public CursorPage(final List<T> records, final Long nextCursor, final boolean hasMore) {
      this.records = records;
      this.nextCursor = nextCursor;
      this.hasMore = hasMore;
      this.size = records != null ? records.size() : 0;
    }

    public List<T> getRecords() {
      return records;
    }

    public Long getNextCursor() {
      return nextCursor;
    }

    public boolean isHasMore() {
      return hasMore;
    }

    public int getSize() {
      return size;
    }
  }

  /**
   * 创建游标分页结果.
   *
   * @param records 查询结果（查询时多查一条用于判断是否有下一页）
   * @param pageSize 每页大小
   * @param idExtractor ID 提取函数
   * @param <T> 数据类型
   * @return 游标分页结果
   */
  public static <T> CursorPage<T> createCursorPage(
      final List<T> records, final int pageSize, final Function<T, Long> idExtractor) {
    if (records == null || records.isEmpty()) {
      return new CursorPage<>(List.of(), null, false);
    }

    boolean hasMore = records.size() > pageSize;

    // 如果有下一页，移除多查的那条记录
    List<T> finalRecords = records;
    if (hasMore) {
      finalRecords = records.subList(0, pageSize);
    }

    // 获取下一页游标（最后一条记录的 ID）
    Long nextCursor = null;
    if (hasMore && !finalRecords.isEmpty()) {
      nextCursor = idExtractor.apply(finalRecords.get(finalRecords.size() - 1));
    }

    return new CursorPage<>(finalRecords, nextCursor, hasMore);
  }

  /** 允许用于游标分页的列名白名单 */
  private static final java.util.Set<String> ALLOWED_CURSOR_COLUMNS =
      java.util.Set.of("id", "created_at", "updated_at", "sort_order", "order_num");

  /**
   * 游标分页 SQL 片段生成
   *
   * <p>使用示例： SELECT * FROM matter WHERE deleted = false {cursorCondition} ORDER BY id LIMIT
   * {pageSize + 1}
   *
   * @param cursor 游标值（上一页最后一条记录的 ID）
   * @param column 游标列名（必须在白名单中）
   * @param ascending 是否升序
   * @return SQL 条件片段
   * @throws IllegalArgumentException 如果列名不在白名单中
   */
  public static String buildCursorCondition(
      final Long cursor, final String column, final boolean ascending) {
    if (cursor == null) {
      return "";
    }
    // SQL 注入防护：校验列名白名单
    if (column == null || !ALLOWED_CURSOR_COLUMNS.contains(column.toLowerCase())) {
      throw new IllegalArgumentException("不允许的游标列名: " + column);
    }
    String operator = ascending ? ">" : "<";
    return String.format("AND %s %s %d", column, operator, cursor);
  }
}
