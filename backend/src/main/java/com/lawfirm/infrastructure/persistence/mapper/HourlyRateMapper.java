package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.HourlyRate;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 小时费率Mapper */
@Mapper
public interface HourlyRateMapper extends BaseMapper<HourlyRate> {

  /**
   * 获取用户当前有效费率.
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 小时费率
   */
  @Select(
      "SELECT * FROM hourly_rate WHERE user_id = #{userId} AND status = 'ACTIVE' "
          + "AND effective_date <= #{date} AND (expiry_date IS NULL OR expiry_date >= #{date}) "
          + "AND deleted = false ORDER BY effective_date DESC LIMIT 1")
  HourlyRate selectCurrentRate(@Param("userId") Long userId, @Param("date") LocalDate date);

  /**
   * 获取用户最新费率.
   *
   * @param userId 用户ID
   * @return 小时费率
   */
  @Select(
      "SELECT * FROM hourly_rate WHERE user_id = #{userId} "
          + "AND status = 'ACTIVE' AND deleted = false "
          + "ORDER BY effective_date DESC LIMIT 1")
  HourlyRate selectLatestRate(@Param("userId") Long userId);
}
