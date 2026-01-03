package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.Backup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备份Mapper
 */
@Mapper
public interface BackupMapper extends BaseMapper<Backup> {
}

