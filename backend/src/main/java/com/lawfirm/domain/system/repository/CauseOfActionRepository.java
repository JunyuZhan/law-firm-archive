package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.CauseOfAction;
import com.lawfirm.infrastructure.persistence.mapper.CauseOfActionMapper;
import org.springframework.stereotype.Repository;

/**
 * 案由/罪名仓储。
 *
 * <p>提供案由/罪名数据的持久化操作。
 */
@Repository
public class CauseOfActionRepository
    extends AbstractRepository<CauseOfActionMapper, CauseOfAction> {}
