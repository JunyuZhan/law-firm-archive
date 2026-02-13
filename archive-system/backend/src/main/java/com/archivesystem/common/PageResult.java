package com.archivesystem.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页码 */
    private Long current;

    /** 每页大小 */
    private Long size;

    /** 总记录数 */
    private Long total;

    /** 总页数 */
    private Long pages;

    /** 数据列表 */
    private List<T> records;

    /**
     * 从MyBatis-Plus分页对象转换.
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.<T>builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .records(page.getRecords())
                .build();
    }

    /**
     * 创建空分页.
     */
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .current(1L)
                .size(10L)
                .total(0L)
                .pages(0L)
                .records(Collections.emptyList())
                .build();
    }

    /**
     * 创建分页结果.
     */
    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return PageResult.<T>builder()
                .current(current)
                .size(size)
                .total(total)
                .pages(pages)
                .records(records)
                .build();
    }
}
