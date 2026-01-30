package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.CostSplit;
import com.lawfirm.infrastructure.persistence.mapper.CostSplitMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 成本分摊仓储 */
@Repository
public class CostSplitRepository extends AbstractRepository<CostSplitMapper, CostSplit> {

  /**
   * 根据费用ID查询所有分摊记录。
   *
   * @param expenseId 费用ID
   * @return 分摊记录列表
   */
  public List<CostSplit> findByExpenseId(final Long expenseId) {
    return baseMapper.selectByExpenseId(expenseId);
  }

  /**
   * 根据项目ID查询所有分摊记录。
   *
   * @param matterId 项目ID
   * @return 分摊记录列表
   */
  public List<CostSplit> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }
}
