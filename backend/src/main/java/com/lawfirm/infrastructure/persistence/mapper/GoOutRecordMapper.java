package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.GoOutRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 外出登记Mapper（M8-005） */
@Mapper
public interface GoOutRecordMapper extends BaseMapper<GoOutRecord> {

  /**
   * 根据登记编号查询.
   *
   * @param recordNo 登记编号
   * @return 外出记录
   */
  @Select("SELECT * FROM go_out_record WHERE record_no = #{recordNo} AND deleted = false")
  GoOutRecord selectByRecordNo(@Param("recordNo") String recordNo);

  /**
   * 查询用户的外出记录.
   *
   * @param userId 用户ID
   * @return 外出记录列表
   */
  @Select(
      "SELECT * FROM go_out_record WHERE user_id = #{userId} AND deleted = false ORDER BY out_time DESC")
  List<GoOutRecord> selectByUserId(@Param("userId") Long userId);

  /**
   * 查询指定日期范围的外出记录.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 外出记录列表
   */
  @Select(
      "SELECT * FROM go_out_record "
          + "WHERE user_id = #{userId} "
          + "AND DATE(out_time) >= #{startDate} "
          + "AND DATE(out_time) <= #{endDate} "
          + "AND deleted = false "
          + "ORDER BY out_time DESC")
  List<GoOutRecord> selectByDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 查询当前外出的记录.
   *
   * @param userId 用户ID
   * @return 外出记录列表
   */
  @Select(
      "SELECT * FROM go_out_record WHERE user_id = #{userId} "
          + "AND status = 'OUT' AND deleted = false ORDER BY out_time DESC")
  List<GoOutRecord> selectCurrentOut(@Param("userId") Long userId);
}
