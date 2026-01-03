package com.lawfirm.domain.archive.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.archive.entity.ArchiveBorrow;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveBorrowMapper;
import org.springframework.stereotype.Repository;

/**
 * 档案借阅仓储
 */
@Repository
public class ArchiveBorrowRepository extends AbstractRepository<ArchiveBorrowMapper, ArchiveBorrow> {
}

