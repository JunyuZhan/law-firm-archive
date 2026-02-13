package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.infrastructure.persistence.mapper.SealMapper;
import org.springframework.stereotype.Repository;

/**
 * 印章仓储。
 *
 * <p>提供印章数据的持久化操作。
 */
@Repository
public class SealRepository extends AbstractRepository<SealMapper, Seal> {}
