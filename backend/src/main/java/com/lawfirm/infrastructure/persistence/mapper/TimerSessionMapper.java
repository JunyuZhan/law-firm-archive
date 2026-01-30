package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.TimerSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 计时器会话 Mapper */
@Mapper
public interface TimerSessionMapper extends BaseMapper<TimerSession> {

  /**
   * 查询用户当前正在运行的计时器会话.
   *
   * @param userId 用户ID
   * @return 计时器会话
   */
  @Select(
      "SELECT * FROM timer_session WHERE user_id = #{userId} AND status = 'RUNNING' ORDER BY start_time DESC LIMIT 1")
  TimerSession selectRunningByUserId(@Param("userId") Long userId);

  /**
   * 查询用户当前已暂停的计时器会话.
   *
   * @param userId 用户ID
   * @return 计时器会话
   */
  @Select(
      "SELECT * FROM timer_session WHERE user_id = #{userId} AND status = 'PAUSED' ORDER BY pause_time DESC LIMIT 1")
  TimerSession selectPausedByUserId(@Param("userId") Long userId);
}
