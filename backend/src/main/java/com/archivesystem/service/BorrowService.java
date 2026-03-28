package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.BorrowApplication;

import java.time.LocalDate;
import java.util.List;

/**
 * 借阅服务接口.
 */
public interface BorrowService {

    /**
     * 提交借阅申请.
     */
    BorrowApplication apply(Long archiveId, String borrowPurpose, LocalDate expectedReturnDate, String remarks);

    /**
     * 获取借阅申请详情.
     */
    BorrowApplication getById(Long id);

    /**
     * 获取我的申请列表.
     */
    PageResult<BorrowApplication> getMyApplications(Long userId, String status, Integer pageNum, Integer pageSize);

    /**
     * 取消申请.
     */
    void cancel(Long id);

    /**
     * 获取待审批列表.
     */
    PageResult<BorrowApplication> getPendingList(Integer pageNum, Integer pageSize);

    /**
     * 获取待借出列表（已审批通过）.
     */
    PageResult<BorrowApplication> getApprovedList(Integer pageNum, Integer pageSize);

    /**
     * 审批通过.
     */
    void approve(Long id, String remarks);

    /**
     * 审批拒绝.
     */
    void reject(Long id, String rejectReason);

    /**
     * 借出档案.
     */
    void lend(Long id);

    /**
     * 归还档案.
     */
    void returnArchive(Long id, String remarks);

    /**
     * 续借.
     */
    void renew(Long id, LocalDate newReturnDate);

    /**
     * 获取逾期未还列表.
     */
    List<BorrowApplication> getOverdueList();

    /**
     * 根据档案ID获取当前借阅记录.
     */
    BorrowApplication getCurrentByArchiveId(Long archiveId);
}
