package com.archivesystem.repository;

import com.archivesystem.entity.BackupTarget;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备份目标 Mapper.
 * @author junyuzhan
 */
@Mapper
public interface BackupTargetMapper extends BaseMapper<BackupTarget> {
}
