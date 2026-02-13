package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DictType;
import com.lawfirm.infrastructure.persistence.mapper.DictTypeMapper;
import org.springframework.stereotype.Repository;

/**
 * 字典类型仓储。
 *
 * <p>提供字典类型数据的持久化操作。
 */
@Repository
public class DictTypeRepository extends AbstractRepository<DictTypeMapper, DictType> {}
