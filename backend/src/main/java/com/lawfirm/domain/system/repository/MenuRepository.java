package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import org.springframework.stereotype.Repository;

/**
 * 菜单仓储。
 *
 * <p>提供菜单数据的持久化操作。
 */
@Repository
public class MenuRepository extends AbstractRepository<MenuMapper, Menu> {}
