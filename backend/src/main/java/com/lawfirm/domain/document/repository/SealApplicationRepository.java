package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.SealApplication;
import com.lawfirm.infrastructure.persistence.mapper.SealApplicationMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 用印申请仓储。
 *
 * <p>提供用印申请数据的持久化操作。
 */
@Repository
public class SealApplicationRepository
    extends AbstractRepository<SealApplicationMapper, SealApplication> {

  /**
   * 查询待审批的申请。
   *
   * @return 待审批申请列表
   */
  public List<SealApplication> findPendingApplications() {
    return baseMapper.selectPendingApplications();
  }

  /**
   * 统计印章使用次数。
   *
   * @param sealId 印章ID
   * @return 使用次数
   */
  public int countUsageBySealId(final Long sealId) {
    return baseMapper.countUsageBySealId(sealId);
  }

  /**
   * 统计印章待处理的申请数量。
   *
   * @param sealId 印章ID
   * @return 待处理申请数量
   */
  public int countPendingBySealId(final Long sealId) {
    return baseMapper.countPendingBySealId(sealId);
  }
}
