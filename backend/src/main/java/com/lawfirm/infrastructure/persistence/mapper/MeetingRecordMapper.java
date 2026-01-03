package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.MeetingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 会议记录Mapper（M8-023）
 */
@Mapper
public interface MeetingRecordMapper extends BaseMapper<MeetingRecord> {

    /**
     * 根据预约ID查询
     */
    @Select("SELECT * FROM meeting_record WHERE booking_id = #{bookingId} AND deleted = false")
    MeetingRecord selectByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 查询会议室的会议记录
     */
    @Select("SELECT * FROM meeting_record WHERE room_id = #{roomId} AND deleted = false ORDER BY meeting_date DESC, start_time DESC")
    List<MeetingRecord> selectByRoomId(@Param("roomId") Long roomId);

    /**
     * 查询指定日期范围的会议记录
     */
    @Select("SELECT * FROM meeting_record " +
            "WHERE meeting_date >= #{startDate} " +
            "AND meeting_date <= #{endDate} " +
            "AND deleted = false " +
            "ORDER BY meeting_date DESC, start_time DESC")
    List<MeetingRecord> selectByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}

