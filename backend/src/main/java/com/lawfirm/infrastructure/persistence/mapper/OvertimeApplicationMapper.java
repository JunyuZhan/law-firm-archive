package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.OvertimeApplication;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 加班申请Mapper（M8-004） */
@Mapper
public interface OvertimeApplicationMapper extends BaseMapper<OvertimeApplication> {

  /**
   * 根据申请编号查询.
   *
   * @param applicationNo 申请编号
   * @return 加班申请
   */
  @Select(
      "SELECT * FROM overtime_application WHERE application_no = #{applicationNo} AND deleted = false")
  OvertimeApplication selectByApplicationNo(@Param("applicationNo") String applicationNo);

  /**
   * 查询用户的加班申请.
   *
   * @param userId 用户ID
   * @return 加班申请列表
   */
  @Select(
      "SELECT * FROM overtime_application WHERE user_id = #{userId} "
          + "AND deleted = false ORDER BY overtime_date DESC, created_at DESC")
  List<OvertimeApplication> selectByUserId(@Param("userId") Long userId);

  /**
   * 查询指定日期范围的加班申请.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 加班申请列表
   */
  @Select(
      "SELECT * FROM overtime_application "
          + "WHERE user_id = #{userId} "
          + "AND overtime_date >= #{startDate} "
          + "AND overtime_date <= #{endDate} "
          + "AND deleted = false "
          + "ORDER BY overtime_date DESC")
  List<OvertimeApplication> selectByDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
