package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.admin.entity.MeetingBooking;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 会议预约Mapper */
@Mapper
public interface MeetingBookingMapper extends BaseMapper<MeetingBooking> {

  /**
   * 分页查询会议预约.
   *
   * @param page 分页参数
   * @param roomId 会议室ID
   * @param organizerId 组织者ID
   * @param status 状态
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 会议预约分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM meeting_booking WHERE deleted = false "
          + "<if test='roomId != null'> AND room_id = #{roomId} </if>"
          + "<if test='organizerId != null'> AND organizer_id = #{organizerId} </if>"
          + "<if test='status != null'> AND status = #{status} </if>"
          + "<if test='startTime != null'> AND start_time &gt;= #{startTime} </if>"
          + "<if test='endTime != null'> AND end_time &lt;= #{endTime} </if>"
          + "ORDER BY start_time DESC"
          + "</script>")
  IPage<MeetingBooking> selectBookingPage(
      Page<MeetingBooking> page,
      @Param("roomId") Long roomId,
      @Param("organizerId") Long organizerId,
      @Param("status") String status,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 查询会议室某时间段的预约.
   *
   * @param roomId 会议室ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 会议预约列表
   */
  @Select(
      "SELECT * FROM meeting_booking WHERE room_id = #{roomId} "
          + "AND start_time >= #{startTime} AND start_time < #{endTime} "
          + "AND status IN ('BOOKED', 'IN_PROGRESS') AND deleted = false "
          + "ORDER BY start_time")
  List<MeetingBooking> selectByRoomAndTimeRange(
      @Param("roomId") Long roomId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 检查时间段是否有冲突.
   *
   * @param roomId 会议室ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param excludeId 排除的预约ID
   * @return 冲突数量
   */
  @Select(
      "<script>"
          + "SELECT COUNT(*) FROM meeting_booking "
          + "WHERE room_id = #{roomId} AND status IN ('BOOKED', 'IN_PROGRESS') AND deleted = false "
          + "AND ((start_time &lt;= #{startTime} AND end_time &gt; #{startTime}) "
          + "OR (start_time &lt; #{endTime} AND end_time &gt;= #{endTime}) "
          + "OR (start_time &gt;= #{startTime} AND end_time &lt;= #{endTime}))"
          + "<if test='excludeId != null'> AND id != #{excludeId} </if>"
          + "</script>")
  int countConflicting(
      @Param("roomId") Long roomId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime,
      @Param("excludeId") Long excludeId);

  /**
   * 查询用户的会议预约.
   *
   * @param userId 用户ID
   * @return 会议预约列表
   */
  @Select(
      "SELECT * FROM meeting_booking WHERE organizer_id = #{userId} "
          + "AND status IN ('BOOKED', 'IN_PROGRESS') AND deleted = false "
          + "ORDER BY start_time")
  List<MeetingBooking> selectByOrganizer(@Param("userId") Long userId);

  /**
   * 查询即将开始的会议（用于发送通知）（M8-022）.
   *
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 即将开始的会议列表
   */
  @Select(
      "SELECT * FROM meeting_booking "
          + "WHERE start_time >= #{startTime} AND start_time <= #{endTime} "
          + "AND status = 'BOOKED' AND (reminder_sent IS NULL OR reminder_sent = false) "
          + "AND deleted = false "
          + "ORDER BY start_time")
  List<MeetingBooking> selectUpcomingMeetings(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
