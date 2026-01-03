package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 会议室Mapper
 */
@Mapper
public interface MeetingRoomMapper extends BaseMapper<MeetingRoom> {

    /**
     * 查询所有启用的会议室
     */
    @Select("SELECT * FROM meeting_room WHERE enabled = true AND deleted = false ORDER BY sort_order")
    List<MeetingRoom> selectEnabledRooms();

    /**
     * 根据编码查询
     */
    @Select("SELECT * FROM meeting_room WHERE code = #{code} AND deleted = false")
    MeetingRoom selectByCode(String code);

    /**
     * 查询可用的会议室
     */
    @Select("SELECT * FROM meeting_room WHERE enabled = true AND status = 'AVAILABLE' AND deleted = false ORDER BY sort_order")
    List<MeetingRoom> selectAvailableRooms();
}
