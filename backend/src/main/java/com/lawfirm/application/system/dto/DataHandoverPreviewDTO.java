package com.lawfirm.application.system.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据交接预览DTO
 */
@Data
public class DataHandoverPreviewDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    // ========== 项目相关 ==========

    /**
     * 主办项目数量
     */
    private Integer leadMatterCount = 0;

    /**
     * 主办项目列表
     */
    private List<Map<String, Object>> leadMatters;

    /**
     * 参与项目数量
     */
    private Integer participantMatterCount = 0;

    /**
     * 参与项目列表
     */
    private List<Map<String, Object>> participantMatters;

    /**
     * 案源人项目数量
     */
    private Integer originatorMatterCount = 0;

    // ========== 客户相关 ==========

    /**
     * 负责客户数量
     */
    private Integer clientCount = 0;

    /**
     * 负责客户列表
     */
    private List<Map<String, Object>> clients;

    // ========== 案源相关 ==========

    /**
     * 负责案源数量
     */
    private Integer leadCount = 0;

    /**
     * 负责案源列表
     */
    private List<Map<String, Object>> leads;

    // ========== 任务相关 ==========

    /**
     * 待办任务数量
     */
    private Integer taskCount = 0;

    /**
     * 待办任务列表
     */
    private List<Map<String, Object>> tasks;

    // ========== 合同参与相关 ==========

    /**
     * 合同参与数量
     */
    private Integer contractParticipantCount = 0;

    // ========== 统计 ==========

    /**
     * 总数据量
     */
    public Integer getTotalCount() {
        return leadMatterCount + participantMatterCount + clientCount + leadCount + taskCount;
    }
}

