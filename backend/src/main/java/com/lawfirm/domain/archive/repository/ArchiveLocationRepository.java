package com.lawfirm.domain.archive.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.archive.entity.ArchiveLocation;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveLocationMapper;
import org.springframework.stereotype.Repository;

/** 档案库位仓储 */
@Repository
public class ArchiveLocationRepository
    extends AbstractRepository<ArchiveLocationMapper, ArchiveLocation> {}
