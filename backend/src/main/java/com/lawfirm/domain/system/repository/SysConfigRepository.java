package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.SysConfig;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SysConfigRepository extends AbstractRepository<SysConfigMapper, SysConfig> {
}
