package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Migration;
import com.lawfirm.infrastructure.persistence.mapper.MigrationMapper;
import org.springframework.stereotype.Repository;

/**
 * 数据库迁移仓储
 */
@Repository
public class MigrationRepository extends AbstractRepository<MigrationMapper, Migration> {
}

