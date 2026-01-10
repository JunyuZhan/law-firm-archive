package com.lawfirm.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * 分页工具类
 * 
 * 功能：
 * 1. 游标分页：避免深度分页性能问题
 * 2. 分页参数校验：限制最大页数和每页大小
 * 3. 分页结果转换：DTO 转换
 * 
 * @author Kiro-1
 */
@Slf4j
public final class PageUtils {

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 最大页数（防止深度分页）
     */
    public static final int MAX_PAGE_NUM = 500;

    /**
     * 深度分页阈值（超过此值建议使用游标分页）
     */
    public static final int DEEP_PAGE_THRESHOLD = 100;

    private PageUtils() {
        // 私有构造函数
    }

    /**
     * 创建安全的分页对象
     * 
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页对象
     */
    public static <T> Page<T> createPage(int pageNum, int pageSize) {
        // 页码校验
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageNum > MAX_PAGE_NUM) {
            log.warn("请求页码 {} 超过最大限制 {}，已自动调整", pageNum, MAX_PAGE_NUM);
            pageNum = MAX_PAGE_NUM;
        }
        
        // 每页大小校验
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            log.warn("每页大小 {} 超过最大限制 {}，已自动调整", pageSize, MAX_PAGE_SIZE);
            pageSize = MAX_PAGE_SIZE;
        }
        
        // 深度分页警告
        if (pageNum > DEEP_PAGE_THRESHOLD) {
            log.warn("检测到深度分页请求: pageNum={}，建议使用游标分页", pageNum);
        }
        
        return new Page<>(pageNum, pageSize);
    }

    /**
     * 创建安全的分页对象（使用默认每页大小）
     */
    public static <T> Page<T> createPage(int pageNum) {
        return createPage(pageNum, DEFAULT_PAGE_SIZE);
    }

    /**
     * 转换分页结果（Entity -> DTO）
     * 
     * @param page      原始分页结果
     * @param converter 转换函数
     * @return 转换后的分页结果
     */
    public static <T, R> IPage<R> convert(IPage<T> page, Function<T, R> converter) {
        Page<R> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(converter)
                .toList());
        return result;
    }

    /**
     * 检查是否为深度分页
     * 
     * @param pageNum 页码
     * @return 是否为深度分页
     */
    public static boolean isDeepPage(int pageNum) {
        return pageNum > DEEP_PAGE_THRESHOLD;
    }

    /**
     * 计算偏移量
     * 
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页大小
     * @return 偏移量
     */
    public static long calculateOffset(int pageNum, int pageSize) {
        return (long) (pageNum - 1) * pageSize;
    }

    /**
     * 计算总页数
     * 
     * @param total    总记录数
     * @param pageSize 每页大小
     * @return 总页数
     */
    public static int calculateTotalPages(long total, int pageSize) {
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
        private List<T> records;
        private Long nextCursor;
        private boolean hasMore;
        private int size;

        public CursorPage(List<T> records, Long nextCursor, boolean hasMore) {
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
     * 创建游标分页结果
     * 
     * @param records    查询结果（查询时多查一条用于判断是否有下一页）
     * @param pageSize   每页大小
     * @param idExtractor ID 提取函数
     * @return 游标分页结果
     */
    public static <T> CursorPage<T> createCursorPage(List<T> records, int pageSize, 
                                                      Function<T, Long> idExtractor) {
        if (records == null || records.isEmpty()) {
            return new CursorPage<>(List.of(), null, false);
        }
        
        boolean hasMore = records.size() > pageSize;
        
        // 如果有下一页，移除多查的那条记录
        if (hasMore) {
            records = records.subList(0, pageSize);
        }
        
        // 获取下一页游标（最后一条记录的 ID）
        Long nextCursor = null;
        if (hasMore && !records.isEmpty()) {
            nextCursor = idExtractor.apply(records.get(records.size() - 1));
        }
        
        return new CursorPage<>(records, nextCursor, hasMore);
    }

    /**
     * 游标分页 SQL 片段生成
     * 
     * 使用示例：
     * SELECT * FROM matter 
     * WHERE deleted = false {cursorCondition} 
     * ORDER BY id 
     * LIMIT {pageSize + 1}
     * 
     * @param cursor   游标值（上一页最后一条记录的 ID）
     * @param column   游标列名
     * @param ascending 是否升序
     * @return SQL 条件片段
     */
    public static String buildCursorCondition(Long cursor, String column, boolean ascending) {
        if (cursor == null) {
            return "";
        }
        String operator = ascending ? ">" : "<";
        return String.format("AND %s %s %d", column, operator, cursor);
    }
}

