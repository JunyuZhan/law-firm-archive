package com.archivesystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 总页数 */
    private Integer pages;

    /**
     * 创建分页结果.
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages((int) Math.ceil((double) total / pageSize));
        return result;
    }
}
