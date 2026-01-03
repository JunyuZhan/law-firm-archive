package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审批记录 Repository
 */
@Repository
public class ApprovalRepository extends AbstractRepository<ApprovalMapper, Approval> {

    /**
     * 统计用户待审批数量
     */
    public int countPendingByApproverId(Long approverId) {
        return baseMapper.countPendingByApproverId(approverId);
    }

    /**
     * 查询用户待审批列表（限制数量）
     */
    public List<Approval> findPendingByApproverId(Long approverId, int limit) {
        return baseMapper.selectPendingByApproverId(approverId, limit);
    }
}

