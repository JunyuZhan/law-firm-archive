package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.AppraisalRecord;

import java.util.List;

/**
 * 鉴定管理服务接口.
 */
public interface AppraisalService {

    /**
     * 发起鉴定.
     *
     * @param archiveId       档案ID
     * @param appraisalType   鉴定类型
     * @param originalValue   原值
     * @param newValue        新值
     * @param appraisalReason 鉴定原因
     * @return 鉴定记录
     */
    AppraisalRecord create(Long archiveId, String appraisalType, String originalValue,
                           String newValue, String appraisalReason);

    /**
     * 获取鉴定详情.
     */
    AppraisalRecord getById(Long id);

    /**
     * 获取鉴定列表（分页）.
     *
     * @param type     鉴定类型（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<AppraisalRecord> getList(String type, String status, Integer pageNum, Integer pageSize);

    /**
     * 获取待审批列表.
     */
    PageResult<AppraisalRecord> getPendingList(Integer pageNum, Integer pageSize);

    /**
     * 获取档案的鉴定历史.
     */
    List<AppraisalRecord> getByArchiveId(Long archiveId);

    /**
     * 审批通过.
     *
     * @param id      鉴定记录ID
     * @param comment 审批意见
     */
    void approve(Long id, String comment);

    /**
     * 审批拒绝.
     *
     * @param id      鉴定记录ID
     * @param comment 拒绝原因
     */
    void reject(Long id, String comment);
}
