package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.LetterTemplate;
import com.lawfirm.infrastructure.persistence.mapper.LetterTemplateMapper;
import org.springframework.stereotype.Repository;

/** 出函模板仓储 */
@Repository
public class LetterTemplateRepository
    extends AbstractRepository<LetterTemplateMapper, LetterTemplate> {}
