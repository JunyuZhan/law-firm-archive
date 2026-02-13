package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Timesheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 工时记录Mapper */
@Mapper
public interface TimesheetMapper extends BaseMapper<Timesheet> {

  /**
   * 分页查询工时.
   *
   * @param page 分页对象
   * @param matterId 项目ID
   * @param userId 用户ID
   * @param workType 工作类型
   * @param status 状态
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param billable 是否可计费
   * @param matterIds 项目ID列表
   * @return 工时分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM timesheet WHERE deleted = false "
          + "<if test='matterId != null'> AND matter_id = #{matterId} </if>"
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='workType != null and workType != \"\"'> AND work_type = #{workType} </if>"
          + "<if test='status != null and status != \"\"'> AND status = #{status} </if>"
          + "<if test='startDate != null'> AND work_date &gt;= #{startDate} </if>"
          + "<if test='endDate != null'> AND work_date &lt;= #{endDate} </if>"
          + "<if test='billable != null'> AND billable = #{billable} </if>"
          + "<if test='matterIds != null and matterIds.size() > 0'> AND matter_id IN "
          + "<foreach collection='matterIds' item='id' open='(' separator=',' close=')'>"
          + "#{id}"
          + "</foreach>"
          + "</if>"
          + "ORDER BY work_date DESC, created_at DESC"
          + "</script>")
  IPage<Timesheet> selectTimesheetPage(
      Page<Timesheet> page,
      @Param("matterId") Long matterId,
      @Param("userId") Long userId,
      @Param("workType") String workType,
      @Param("status") String status,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("billable") Boolean billable,
      @Param("matterIds") java.util.List<Long> matterIds);

  /**
   * 按用户和日期范围查询.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工时记录列表
   */
  @Select(
      "SELECT * FROM timesheet WHERE user_id = #{userId} "
          + "AND work_date BETWEEN #{startDate} AND #{endDate} "
          + "AND deleted = false ORDER BY work_date")
  List<Timesheet> selectByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 统计用户某月总工时.
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 总工时
   */
  @Select(
      "SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} "
          + "AND EXTRACT(YEAR FROM work_date) = #{year} "
          + "AND EXTRACT(MONTH FROM work_date) = #{month} "
          + "AND status = 'APPROVED' AND deleted = false")
  BigDecimal sumHoursByUserAndMonth(
      @Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

  /**
   * 统计案件总工时.
   *
   * @param matterId 项目ID
   * @return 总工时
   */
  @Select(
      "SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE matter_id = #{matterId} "
          + "AND status = 'APPROVED' AND deleted = false")
  BigDecimal sumHoursByMatter(@Param("matterId") Long matterId);

  /**
   * 查询待审批工时.
   *
   * @return 待审批工时列表
   */
  @Select(
      "SELECT * FROM timesheet WHERE status = 'SUBMITTED' AND deleted = false ORDER BY submitted_at ASC")
  List<Timesheet> selectPendingApproval();

  /**
   * 统计用户日期范围内总工时.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 总工时
   */
  @Select(
      "SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} "
          + "AND work_date BETWEEN #{startDate} AND #{endDate} "
          + "AND deleted = false")
  BigDecimal sumHoursByUserIdAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 统计用户日期范围内可计费工时.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 可计费工时
   */
  @Select(
      "SELECT COALESCE(SUM(hours), 0) FROM timesheet WHERE user_id = #{userId} "
          + "AND work_date BETWEEN #{startDate} AND #{endDate} "
          + "AND billable = true AND deleted = false")
  BigDecimal sumBillableHoursByUserIdAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 统计用户日期范围内记录数.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 记录数量
   */
  @Select(
      "SELECT COUNT(*) FROM timesheet WHERE user_id = #{userId} "
          + "AND work_date BETWEEN #{startDate} AND #{endDate} "
          + "AND deleted = false")
  int countByUserIdAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
