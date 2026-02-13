package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.QualityCheck;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckMapper;
import org.springframework.stereotype.Repository;

/** 质量检查仓储（M10-031） */
@Repository
public class QualityCheckRepository extends AbstractRepository<QualityCheckMapper, QualityCheck> {}
