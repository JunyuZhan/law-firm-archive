package com.lawfirm.domain.archive.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.archive.entity.Archive;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveMapper;
import org.springframework.stereotype.Repository;

/**
 * 档案仓储
 */
@Repository
public class ArchiveRepository extends AbstractRepository<ArchiveMapper, Archive> {
}

