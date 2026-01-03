package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.CostAllocation;
import com.lawfirm.infrastructure.persistence.mapper.CostAllocationMapper;
import org.springframework.stereotype.Repository;

/**
 * 成本归集 Repository
 */
@Repository
public class CostAllocationRepository extends AbstractRepository<CostAllocationMapper, CostAllocation> {
}

