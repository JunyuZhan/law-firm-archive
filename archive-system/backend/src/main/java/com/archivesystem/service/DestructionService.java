package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.DestructionRecord;

import java.util.List;

/**
 * 销毁管理服务接口.
 */
public interface DestructionService {

    /**
     * 申请销毁.
     *
     * @param archiveId         档案ID
     * @param destructionReason 销毁原因
     * @param destructionMethod 销毁方式
     * @return 销毁记录
     */
    DestructionRecord apply(Long archiveId, String destructionReason, String destructionMethod);

    /**
     * 批量申请销毁.
     *
     * @param archiveIds        档案ID列表
     * @param destructionReason 销毁原因
     * @param destructionMethod 销毁方式
     * @return 销毁记录列表
     */
    List<DestructionRecord> batchApply(List<Long> archiveIds, String destructionReason, String destructionMethod);

    /**
     * 获取销毁记录详情.
     */
    DestructionRecord getById(Long id);

    /**
     * 获取销毁记录列表（分页）.
     */
    PageResult<DestructionRecord> getList(String status, Integer pageNum, Integer pageSize);

    /**
     * 获取待审批列表.
     */
    PageResult<DestructionRecord> getPendingList(Integer pageNum, Integer pageSize);

    /**
     * 获取待执行列表（已审批）.
     */
    PageResult<DestructionRecord> getApprovedList(Integer pageNum, Integer pageSize);

    /**
     * 获取档案的销毁记录.
     */
    List<DestructionRecord> getByArchiveId(Long archiveId);

    /**
     * 审批通过.
     */
    void approve(Long id, String comment);

    /**
     * 审批拒绝.
     */
    void reject(Long id, String comment);

    /**
     * 执行销毁.
     */
    void execute(Long id, String remarks);

    /**
     * 批量执行销毁.
     */
    void batchExecute(List<Long> ids, String remarks);
}
