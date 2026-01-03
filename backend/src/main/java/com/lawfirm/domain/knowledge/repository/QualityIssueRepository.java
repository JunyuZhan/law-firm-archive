package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.QualityIssue;
import com.lawfirm.infrastructure.persistence.mapper.QualityIssueMapper;
import org.springframework.stereotype.Repository;

/**
 * 问题整改仓储（M10-032）
 */
@Repository
public class QualityIssueRepository extends AbstractRepository<QualityIssueMapper, QualityIssue> {
}

