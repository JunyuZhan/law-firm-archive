package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.admin.entity.MeetingRoom;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 会议室Mapper */
@Mapper
public interface MeetingRoomMapper extends BaseMapper<MeetingRoom> {

  /**
   * 查询所有启用的会议室.
   *
   * @return 启用的会议室列表
   */
  @Select("SELECT * FROM meeting_room WHERE enabled = true AND deleted = false ORDER BY sort_order")
  List<MeetingRoom> selectEnabledRooms();

  /**
   * 根据编码查询.
   *
   * @param code 会议室编码
   * @return 会议室信息
   */
  @Select("SELECT * FROM meeting_room WHERE code = #{code} AND deleted = false")
  MeetingRoom selectByCode(String code);

  /**
   * 查询可用的会议室.
   *
   * @return 可用的会议室列表
   */
  @Select(
      "SELECT * FROM meeting_room WHERE enabled = true "
          + "AND status = 'AVAILABLE' AND deleted = false ORDER BY sort_order")
  List<MeetingRoom> selectAvailableRooms();
}
