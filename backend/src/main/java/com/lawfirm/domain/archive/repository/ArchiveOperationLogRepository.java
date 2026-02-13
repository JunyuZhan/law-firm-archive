package com.lawfirm.domain.archive.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.archive.entity.ArchiveOperationLog;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveOperationLogMapper;
import org.springframework.stereotype.Repository;

/** 档案操作日志仓储 */
@Repository
public class ArchiveOperationLogRepository
    extends AbstractRepository<ArchiveOperationLogMapper, ArchiveOperationLog> {}
