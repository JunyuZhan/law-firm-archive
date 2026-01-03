package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.RiskWarning;
import com.lawfirm.infrastructure.persistence.mapper.RiskWarningMapper;
import org.springframework.stereotype.Repository;

/**
 * 风险预警仓储（M10-033）
 */
@Repository
public class RiskWarningRepository extends AbstractRepository<RiskWarningMapper, RiskWarning> {
}

