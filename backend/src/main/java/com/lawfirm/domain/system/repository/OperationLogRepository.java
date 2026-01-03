package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.OperationLog;
import com.lawfirm.infrastructure.persistence.mapper.OperationLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OperationLogRepository extends AbstractRepository<OperationLogMapper, OperationLog> {
}
