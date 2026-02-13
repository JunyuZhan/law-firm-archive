package com.lawfirm.domain.knowledge.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.knowledge.entity.LawRegulation;
import com.lawfirm.infrastructure.persistence.mapper.LawRegulationMapper;
import org.springframework.stereotype.Repository;

/** 法规Repository */
@Repository
public class LawRegulationRepository
    extends AbstractRepository<LawRegulationMapper, LawRegulation> {}
