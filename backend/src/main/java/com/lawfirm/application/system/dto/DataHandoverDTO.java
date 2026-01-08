package com.lawfirm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据交接记录DTO
 */
@Data
public class DataHandoverDTO {

    private Long id;

    /**
     * 交接单号
     */
    private String handoverNo;

    /**
     * 移交人ID
     */
    private Long fromUserId;

    /**
     * 移交人姓名
     */
    private String fromUsername;

    /**
     * 接收人ID
     */
    private Long toUserId;

    /**
     * 接收人姓名
     */
    private String toUsername;

    /**
     * 交接类型
     */
    private String handoverType;

    /**
     * 交接类型名称
     */
    private String handoverTypeName;

    /**
     * 交接原因
     */
    private String handoverReason;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 移交项目数
     */
    private Integer matterCount;

    /**
     * 移交客户数
     */
    private Integer clientCount;

    /**
     * 移交案源数
     */
    private Integer leadCount;

    /**
     * 移交任务数
     */
    private Integer taskCount;

    /**
     * 提交人ID
     */
    private Long submittedBy;

    /**
     * 提交人姓名
     */
    private String submittedByName;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 确认人ID
     */
    private Long confirmedBy;

    /**
     * 确认人姓名
     */
    private String confirmedByName;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 交接明细列表
     */
    private List<DataHandoverDetailDTO> details;
}

