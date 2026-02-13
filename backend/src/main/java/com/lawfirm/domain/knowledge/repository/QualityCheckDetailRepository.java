package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.QualityCheckDetail;
import com.lawfirm.infrastructure.persistence.mapper.QualityCheckDetailMapper;
import org.springframework.stereotype.Repository;

/** 质量检查明细仓储（M10-031） */
@Repository
public class QualityCheckDetailRepository
    extends AbstractRepository<QualityCheckDetailMapper, QualityCheckDetail> {}
