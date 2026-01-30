package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.CaseLibrary;
import com.lawfirm.infrastructure.persistence.mapper.CaseLibraryMapper;
import org.springframework.stereotype.Repository;

/** 案例库Repository */
@Repository
public class CaseLibraryRepository extends AbstractRepository<CaseLibraryMapper, CaseLibrary> {}
