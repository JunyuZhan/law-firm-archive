package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.Attendance;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 考勤记录Mapper */
@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {

  /**
   * 分页查询考勤记录.
   *
   * @param page 分页对象
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param status 状态
   * @return 考勤记录分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM attendance WHERE deleted = false "
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='startDate != null'> AND attendance_date &gt;= #{startDate} </if>"
          + "<if test='endDate != null'> AND attendance_date &lt;= #{endDate} </if>"
          + "<if test='status != null'> AND status = #{status} </if>"
          + "ORDER BY attendance_date DESC, id DESC"
          + "</script>")
  IPage<Attendance> selectAttendancePage(
      Page<Attendance> page,
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("status") String status);

  /**
   * 查询用户某日考勤.
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 考勤记录
   */
  @Select(
      "SELECT * FROM attendance WHERE user_id = #{userId} AND attendance_date = #{date} AND deleted = false")
  Attendance selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

  /**
   * 统计用户月度考勤.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 统计结果
   */
  @Select(
      "SELECT status, COUNT(*) as count FROM attendance "
          + "WHERE user_id = #{userId} AND attendance_date >= #{startDate} "
          + "AND attendance_date <= #{endDate} AND deleted = false "
          + "GROUP BY status")
  List<Object[]> countMonthlyAttendance(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
