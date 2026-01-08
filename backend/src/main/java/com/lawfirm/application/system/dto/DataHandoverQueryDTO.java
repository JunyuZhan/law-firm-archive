package com.lawfirm.application.system.dto;

import lombok.Data;

/**
 * 数据交接查询DTO
 */
@Data
public class DataHandoverQueryDTO {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 移交人ID
     */
    private Long fromUserId;

    /**
     * 接收人ID
     */
    private Long toUserId;

    /**
     * 交接类型
     */
    private String handoverType;

    /**
     * 状态
     */
    private String status;

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}

