package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.UserWecom;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 用户企业微信绑定Mapper */
@Mapper
public interface UserWecomMapper extends BaseMapper<UserWecom> {

  /**
   * 根据用户ID查询.
   *
   * @param userId 用户ID
   * @return 用户企业微信绑定信息
   */
  @Select("SELECT * FROM sys_user_wecom WHERE user_id = #{userId}")
  UserWecom selectByUserId(@Param("userId") Long userId);

  /**
   * 根据企业微信UserId查询.
   *
   * @param wecomUserid 企业微信UserId
   * @return 用户企业微信绑定信息
   */
  @Select("SELECT * FROM sys_user_wecom WHERE wecom_userid = #{wecomUserid} AND enabled = true")
  UserWecom selectByWecomUserid(@Param("wecomUserid") String wecomUserid);

  /**
   * 查询所有启用的绑定.
   *
   * @return 启用的绑定列表
   */
  @Select("SELECT * FROM sys_user_wecom WHERE enabled = true")
  List<UserWecom> selectAllEnabled();

  /**
   * 批量查询用户的企业微信ID.
   *
   * @param userIds 用户ID列表
   * @return 用户企业微信绑定列表
   */
  @Select(
      "<script>"
          + "SELECT * FROM sys_user_wecom WHERE user_id IN "
          + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>"
          + "#{id}"
          + "</foreach>"
          + " AND enabled = true"
          + "</script>")
  List<UserWecom> selectByUserIds(@Param("userIds") List<Long> userIds);

  /**
   * 插入或更新.
   *
   * @param userWecom 用户企业微信绑定信息
   * @return 影响行数
   */
  @Insert(
      "INSERT INTO sys_user_wecom (user_id, wecom_userid, wecom_mobile, enabled, created_at, updated_at) "
          + "VALUES (#{userId}, #{wecomUserid}, #{wecomMobile}, #{enabled}, NOW(), NOW()) "
          + "ON CONFLICT (user_id) DO UPDATE SET "
          + "wecom_userid = EXCLUDED.wecom_userid, "
          + "wecom_mobile = EXCLUDED.wecom_mobile, "
          + "enabled = EXCLUDED.enabled, "
          + "updated_at = NOW()")
  int insertOrUpdate(UserWecom userWecom);
}
