package com.lawfirm.domain.workbench.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.workbench.entity.Approval;
import com.lawfirm.infrastructure.persistence.mapper.ApprovalMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 审批记录 Repository */
@Repository
public class ApprovalRepository extends AbstractRepository<ApprovalMapper, Approval> {

  /**
   * 统计用户待审批数量。
   *
   * @param approverId 审批人ID
   * @return 待审批数量
   */
  public int countPendingByApproverId(final Long approverId) {
    return baseMapper.countPendingByApproverId(approverId);
  }

  /**
   * 查询用户待审批列表（限制数量）。
   *
   * @param approverId 审批人ID
   * @param limit 限制数量
   * @return 待审批列表
   */
  public List<Approval> findPendingByApproverId(final Long approverId, final int limit) {
    return baseMapper.selectPendingByApproverId(approverId, limit);
  }
}
