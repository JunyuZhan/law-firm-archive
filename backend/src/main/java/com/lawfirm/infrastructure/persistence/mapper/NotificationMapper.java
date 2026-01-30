package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 系统通知Mapper */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

  /**
   * 分页查询用户通知.
   *
   * @param page 分页对象
   * @param receiverId 接收人ID
   * @param type 通知类型
   * @param isRead 是否已读
   * @return 通知分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM sys_notification WHERE receiver_id = #{receiverId} AND deleted = false "
          + "<if test='type != null'> AND type = #{type} </if>"
          + "<if test='isRead != null'> AND is_read = #{isRead} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<Notification> selectByReceiver(
      Page<Notification> page,
      @Param("receiverId") Long receiverId,
      @Param("type") String type,
      @Param("isRead") Boolean isRead);

  /**
   * 统计未读数量.
   *
   * @param receiverId 接收人ID
   * @return 未读数量
   */
  @Select(
      "SELECT COUNT(*) FROM sys_notification WHERE receiver_id = #{receiverId} AND is_read = false AND deleted = false")
  int countUnread(@Param("receiverId") Long receiverId);

  /**
   * 标记为已读.
   *
   * @param id 通知ID
   * @return 更新数量
   */
  @Update("UPDATE sys_notification SET is_read = true, read_at = NOW() WHERE id = #{id}")
  int markAsRead(@Param("id") Long id);

  /**
   * 全部标记为已读.
   *
   * @param receiverId 接收人ID
   * @return 更新数量
   */
  @Update(
      "UPDATE sys_notification SET is_read = true, read_at = NOW() "
          + "WHERE receiver_id = #{receiverId} AND is_read = false")
  int markAllAsRead(@Param("receiverId") Long receiverId);

  /**
   * 批量删除已读通知.
   *
   * @param receiverId 接收人ID
   * @return 删除数量
   */
  @Update(
      "UPDATE sys_notification SET deleted = true WHERE receiver_id = #{receiverId} "
          + "AND is_read = true AND deleted = false")
  int deleteReadNotifications(@Param("receiverId") Long receiverId);
}
