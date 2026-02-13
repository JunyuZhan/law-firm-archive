package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import org.springframework.stereotype.Repository;

/**
 * 外部系统集成仓储。
 *
 * <p>提供外部系统集成数据的持久化操作。
 */
@Repository
public class ExternalIntegrationRepository
    extends AbstractRepository<ExternalIntegrationMapper, ExternalIntegration> {}
