package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DictType;
import com.lawfirm.infrastructure.persistence.mapper.DictTypeMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DictTypeRepository extends AbstractRepository<DictTypeMapper, DictType> {
}
