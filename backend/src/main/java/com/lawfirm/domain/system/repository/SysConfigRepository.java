package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.SysConfig;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import org.springframework.stereotype.Repository;

/**
 * 系统配置仓储。
 *
 * <p>提供系统配置数据的持久化操作。
 */
@Repository
public class SysConfigRepository extends AbstractRepository<SysConfigMapper, SysConfig> {}
