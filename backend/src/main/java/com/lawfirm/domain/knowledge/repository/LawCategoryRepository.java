package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.LawCategory;
import com.lawfirm.infrastructure.persistence.mapper.LawCategoryMapper;
import org.springframework.stereotype.Repository;

/** 法规分类Repository */
@Repository
public class LawCategoryRepository extends AbstractRepository<LawCategoryMapper, LawCategory> {}
