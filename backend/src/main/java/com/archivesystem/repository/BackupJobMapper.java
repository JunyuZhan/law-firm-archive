package com.archivesystem.repository;

import com.archivesystem.entity.BackupJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备份任务 Mapper.
 * @author junyuzhan
 */
@Mapper
public interface BackupJobMapper extends BaseMapper<BackupJob> {
}
