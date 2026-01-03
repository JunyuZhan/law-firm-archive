package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.CostSplit;
import com.lawfirm.infrastructure.persistence.mapper.CostSplitMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 成本分摊仓储
 */
@Repository
public class CostSplitRepository extends AbstractRepository<CostSplitMapper, CostSplit> {

    /**
     * 根据费用ID查询所有分摊记录
     */
    public List<CostSplit> findByExpenseId(Long expenseId) {
        return baseMapper.selectByExpenseId(expenseId);
    }

    /**
     * 根据项目ID查询所有分摊记录
     */
    public List<CostSplit> findByMatterId(Long matterId) {
        return baseMapper.selectByMatterId(matterId);
    }
}

