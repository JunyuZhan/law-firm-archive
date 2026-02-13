package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.PerformanceTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 考核任务Mapper */
@Mapper
public interface PerformanceTaskMapper extends BaseMapper<PerformanceTask> {

  /**
   * 分页查询考核任务.
   *
   * @param page 分页参数
   * @param year 年份
   * @param periodType 周期类型
   * @param status 状态
   * @return 考核任务分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM hr_performance_task WHERE deleted = false "
          + "<if test='year != null'>AND year = #{year} </if>"
          + "<if test='periodType != null and periodType != \"\"'>AND period_type = #{periodType} </if>"
          + "<if test='status != null and status != \"\"'>AND status = #{status} </if>"
          + "ORDER BY year DESC, period DESC"
          + "</script>")
  IPage<PerformanceTask> findPage(
      Page<PerformanceTask> page,
      @Param("year") Integer year,
      @Param("periodType") String periodType,
      @Param("status") String status);

  /**
   * 查询进行中的考核任务.
   *
   * @return 进行中的考核任务列表
   */
  @Select(
      "SELECT * FROM hr_performance_task WHERE deleted = false AND status = 'IN_PROGRESS' ORDER BY created_at DESC")
  List<PerformanceTask> findInProgress();
}
