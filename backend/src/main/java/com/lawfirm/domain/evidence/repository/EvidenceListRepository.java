package com.lawfirm.domain.evidence.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceListMapper;
import org.springframework.stereotype.Repository;

/** 证据清单Repository */
@Repository
public class EvidenceListRepository extends AbstractRepository<EvidenceListMapper, EvidenceList> {}
