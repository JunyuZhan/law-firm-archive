package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.CauseOfAction;
import com.lawfirm.infrastructure.persistence.mapper.CauseOfActionMapper;
import org.springframework.stereotype.Repository;

/**
 * 案由/罪名仓储
 */
@Repository
public class CauseOfActionRepository extends AbstractRepository<CauseOfActionMapper, CauseOfAction> {
}
