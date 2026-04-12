package com.archivesystem.repository;

import com.archivesystem.entity.RetentionPeriod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 保管期限Mapper接口.
 * @author junyuzhan
 */
@Mapper
public interface RetentionPeriodMapper extends BaseMapper<RetentionPeriod> {

    /**
     * 根据期限代码查询.
     */
    @Select("SELECT * FROM arc_retention_period WHERE period_code = #{periodCode}")
    RetentionPeriod selectByPeriodCode(@Param("periodCode") String periodCode);
}
