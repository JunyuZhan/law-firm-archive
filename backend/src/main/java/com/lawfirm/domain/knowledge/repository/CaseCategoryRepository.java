package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.CaseCategory;
import com.lawfirm.infrastructure.persistence.mapper.CaseCategoryMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CaseCategoryRepository extends AbstractRepository<CaseCategoryMapper, CaseCategory> {
}
