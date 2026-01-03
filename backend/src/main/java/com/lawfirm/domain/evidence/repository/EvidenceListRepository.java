package com.lawfirm.domain.evidence.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.evidence.entity.EvidenceList;
import com.lawfirm.infrastructure.persistence.mapper.EvidenceListMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EvidenceListRepository extends AbstractRepository<EvidenceListMapper, EvidenceList> {
}
