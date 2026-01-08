package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.matter.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程Mapper
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 查询用户日程
     */
    @Select("<script>" +
            "SELECT * FROM schedule WHERE deleted = false " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='scheduleType != null and scheduleType != \"\"'> AND schedule_type = #{scheduleType} </if>" +
            "<if test='startTime != null'> AND start_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND end_time &lt;= #{endTime} </if>" +
            "ORDER BY start_time ASC" +
            "</script>")
    IPage<Schedule> selectSchedulePage(Page<Schedule> page,
                                       @Param("userId") Long userId,
                                       @Param("matterId") Long matterId,
                                       @Param("scheduleType") String scheduleType,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户某天的日程
     */
    @Select("SELECT * FROM schedule WHERE user_id = #{userId} AND DATE(start_time) = #{date} AND deleted = false ORDER BY start_time")
    List<Schedule> selectByUserAndDate(@Param("userId") Long userId, @Param("date") java.time.LocalDate date);

    /**
     * 查询需要提醒的日程
     */
    @Select("SELECT * FROM schedule WHERE reminder_sent = false AND reminder_minutes IS NOT NULL " +
            "AND start_time - (reminder_minutes || ' minutes')::interval <= NOW() " +
            "AND start_time > NOW() AND deleted = false")
    List<Schedule> selectNeedReminder();

    /**
     * 查询用户未来几天的日程
     */
    @Select("SELECT * FROM schedule WHERE user_id = #{userId} " +
            "AND start_time >= NOW() AND start_time <= #{endTime} " +
            "AND status != 'CANCELLED' AND deleted = false " +
            "ORDER BY start_time ASC LIMIT #{limit}")
    List<Schedule> selectUpcomingSchedules(@Param("userId") Long userId, 
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("limit") int limit);
}
