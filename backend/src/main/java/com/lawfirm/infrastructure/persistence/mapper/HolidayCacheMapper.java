package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.HolidayCache;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 节假日缓存Mapper */
@Mapper
public interface HolidayCacheMapper extends BaseMapper<HolidayCache> {

  /**
   * 根据日期查询.
   *
   * @param date 日期
   * @return 节假日缓存
   */
  @Select("SELECT * FROM sys_holiday_cache WHERE date = #{date}")
  HolidayCache selectByDate(@Param("date") LocalDate date);

  /**
   * 查询某年某月的节假日.
   *
   * @param year 年份
   * @param month 月份
   * @return 节假日列表
   */
  @Select("SELECT * FROM sys_holiday_cache WHERE year = #{year} AND month = #{month} ORDER BY date")
  List<HolidayCache> selectByYearMonth(@Param("year") int year, @Param("month") int month);

  /**
   * 查询某年的所有节假日.
   *
   * @param year 年份
   * @return 节假日列表
   */
  @Select("SELECT * FROM sys_holiday_cache WHERE year = #{year} ORDER BY date")
  List<HolidayCache> selectByYear(@Param("year") int year);

  /**
   * 查询日期范围内的休息日.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 休息日列表
   */
  @Select(
      "SELECT * FROM sys_holiday_cache WHERE date BETWEEN #{startDate} AND #{endDate} AND is_off = true ORDER BY date")
  List<HolidayCache> selectOffDaysInRange(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  /**
   * 插入或更新（根据日期）.
   *
   * @param holidayCache 节假日缓存
   * @return 影响行数
   */
  @Insert(
      "INSERT INTO sys_holiday_cache "
          + "(date, year, month, day_type, day_type_name, holiday_name, is_off, "
          + "data_source, fetched_at, created_at) "
          + "VALUES (#{date}, #{year}, #{month}, #{dayType}, #{dayTypeName}, "
          + "#{holidayName}, #{isOff}, #{dataSource}, #{fetchedAt}, NOW()) "
          + "ON CONFLICT (date) DO UPDATE SET "
          + "day_type = EXCLUDED.day_type, "
          + "day_type_name = EXCLUDED.day_type_name, "
          + "holiday_name = EXCLUDED.holiday_name, "
          + "is_off = EXCLUDED.is_off, "
          + "data_source = EXCLUDED.data_source, "
          + "fetched_at = EXCLUDED.fetched_at")
  int insertOrUpdate(HolidayCache holidayCache);

  /**
   * 批量插入或更新.
   *
   * @param list 节假日缓存列表
   * @return 影响行数
   */
  @Insert(
      "<script>"
          + "INSERT INTO sys_holiday_cache "
          + "(date, year, month, day_type, day_type_name, holiday_name, is_off, "
          + "data_source, fetched_at, created_at) VALUES "
          + "<foreach collection='list' item='item' separator=','>"
          + "(#{item.date}, #{item.year}, #{item.month}, #{item.dayType}, "
          + "#{item.dayTypeName}, #{item.holidayName}, #{item.isOff}, "
          + "#{item.dataSource}, #{item.fetchedAt}, NOW())"
          + "</foreach>"
          + "ON CONFLICT (date) DO UPDATE SET "
          + "day_type = EXCLUDED.day_type, "
          + "day_type_name = EXCLUDED.day_type_name, "
          + "holiday_name = EXCLUDED.holiday_name, "
          + "is_off = EXCLUDED.is_off, "
          + "data_source = EXCLUDED.data_source, "
          + "fetched_at = EXCLUDED.fetched_at"
          + "</script>")
  int batchInsertOrUpdate(@Param("list") List<HolidayCache> list);

  /**
   * 统计某年数据条数.
   *
   * @param year 年份
   * @return 数据条数
   */
  @Select("SELECT COUNT(*) FROM sys_holiday_cache WHERE year = #{year}")
  int countByYear(@Param("year") int year);
}
