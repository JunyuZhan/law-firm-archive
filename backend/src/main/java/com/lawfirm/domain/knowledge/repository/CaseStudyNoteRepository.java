package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.CaseStudyNote;
import com.lawfirm.infrastructure.persistence.mapper.CaseStudyNoteMapper;
import org.springframework.stereotype.Repository;

/** 案例学习笔记仓储（M10-013） */
@Repository
public class CaseStudyNoteRepository
    extends AbstractRepository<CaseStudyNoteMapper, CaseStudyNote> {}
