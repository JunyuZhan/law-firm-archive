package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.PerformanceTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 考核任务Mapper
 */
@Mapper
public interface PerformanceTaskMapper extends BaseMapper<PerformanceTask> {

    @Select("<script>" +
            "SELECT * FROM hr_performance_task WHERE deleted = false " +
            "<if test='year != null'>AND year = #{year} </if>" +
            "<if test='periodType != null and periodType != \"\"'>AND period_type = #{periodType} </if>" +
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>" +
            "ORDER BY year DESC, period DESC" +
            "</script>")
    IPage<PerformanceTask> findPage(Page<PerformanceTask> page,
                                    @Param("year") Integer year,
                                    @Param("periodType") String periodType,
                                    @Param("status") String status);

    @Select("SELECT * FROM hr_performance_task WHERE deleted = false AND status = 'IN_PROGRESS' ORDER BY created_at DESC")
    List<PerformanceTask> findInProgress();
}
