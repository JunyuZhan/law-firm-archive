package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PromotionApplication;
import com.lawfirm.infrastructure.persistence.mapper.PromotionApplicationMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 晋升申请 Repository. */
@Repository
public class PromotionApplicationRepository
    extends AbstractRepository<PromotionApplicationMapper, PromotionApplication> {

  /**
   * 根据申请编号查询.
   *
   * @param applicationNo 申请编号
   * @return 晋升申请
   */
  public Optional<PromotionApplication> findByApplicationNo(final String applicationNo) {
    return Optional.ofNullable(baseMapper.selectByApplicationNo(applicationNo));
  }

  /**
   * 统计待审批数量.
   *
   * @return 待审批数量
   */
  public int countPending() {
    return baseMapper.countPending();
  }
}
