package com.lawfirm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据交接明细DTO
 */
@Data
public class DataHandoverDetailDTO {

    private Long id;

    /**
     * 交接单ID
     */
    private Long handoverId;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 数据类型名称
     */
    private String dataTypeName;

    /**
     * 数据ID
     */
    private Long dataId;

    /**
     * 数据编号
     */
    private String dataNo;

    /**
     * 数据名称
     */
    private String dataName;

    /**
     * 变更字段名
     */
    private String fieldName;

    /**
     * 变更字段名称
     */
    private String fieldDisplayName;

    /**
     * 原值（用户ID）
     */
    private String oldValue;

    /**
     * 原用户姓名
     */
    private String oldUserName;

    /**
     * 新值（用户ID）
     */
    private String newValue;

    /**
     * 新用户姓名
     */
    private String newUserName;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private LocalDateTime executedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

