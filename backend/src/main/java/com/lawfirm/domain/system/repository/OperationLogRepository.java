package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.OperationLog;
import com.lawfirm.infrastructure.persistence.mapper.OperationLogMapper;
import org.springframework.stereotype.Repository;

/**
 * 操作日志仓储。
 *
 * <p>提供操作日志的持久化操作。
 */
@Repository
public class OperationLogRepository extends AbstractRepository<OperationLogMapper, OperationLog> {}
