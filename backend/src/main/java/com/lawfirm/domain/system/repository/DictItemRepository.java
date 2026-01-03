package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DictItem;
import com.lawfirm.infrastructure.persistence.mapper.DictItemMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DictItemRepository extends AbstractRepository<DictItemMapper, DictItem> {
}
