package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.QualityCheckStandard;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckStandardMapper;
import org.springframework.stereotype.Repository;

/** 质量检查标准仓储（M10-030） */
@Repository
public class QualityCheckStandardRepository
    extends AbstractRepository<QualityCheckStandardMapper, QualityCheckStandard> {}
