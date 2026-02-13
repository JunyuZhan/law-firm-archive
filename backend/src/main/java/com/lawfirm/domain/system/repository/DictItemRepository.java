package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DictItem;
import com.lawfirm.infrastructure.persistence.mapper.DictItemMapper;
import org.springframework.stereotype.Repository;

/**
 * 字典项仓储。
 *
 * <p>提供字典项数据的持久化操作。
 */
@Repository
public class DictItemRepository extends AbstractRepository<DictItemMapper, DictItem> {}
