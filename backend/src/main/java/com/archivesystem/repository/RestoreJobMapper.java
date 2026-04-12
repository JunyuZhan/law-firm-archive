package com.archivesystem.repository;

import com.archivesystem.entity.RestoreJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 恢复任务 Mapper.
 * @author junyuzhan
 */
@Mapper
public interface RestoreJobMapper extends BaseMapper<RestoreJob> {
}
