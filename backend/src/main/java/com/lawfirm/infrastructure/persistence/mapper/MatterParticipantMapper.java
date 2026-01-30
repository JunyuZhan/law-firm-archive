package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 案件参与人 Mapper */
@Mapper
public interface MatterParticipantMapper extends BaseMapper<MatterParticipant> {

  /**
   * 查询案件的所有参与人.
   *
   * @param matterId 案件ID
   * @return 参与人列表
   */
  @Select(
      """
        SELECT mp.*, u.real_name as user_name, u.position, u.compensation_type
        FROM matter_participant mp
        INNER JOIN sys_user u ON mp.user_id = u.id
        WHERE mp.matter_id = #{matterId} AND mp.deleted = false
        ORDER BY mp.role, mp.id
        """)
  List<MatterParticipant> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 查询案件的主办律师.
   *
   * @param matterId 案件ID
   * @return 主办律师信息
   */
  @Select(
      """
        SELECT mp.* FROM matter_participant mp
        WHERE mp.matter_id = #{matterId} AND mp.role = 'LEAD' AND mp.status = 'ACTIVE' AND mp.deleted = false
        """)
  MatterParticipant selectLeadLawyer(@Param("matterId") Long matterId);

  /**
   * 删除案件的所有参与人.
   *
   * @param matterId 案件ID
   */
  @Delete("DELETE FROM matter_participant WHERE matter_id = #{matterId}")
  void deleteByMatterId(@Param("matterId") Long matterId);

  /**
   * 检查用户是否已在案件团队中.
   *
   * @param matterId 案件ID
   * @param userId 用户ID
   * @return 数量
   */
  @Select(
      """
        SELECT COUNT(*) FROM matter_participant
        WHERE matter_id = #{matterId} AND user_id = #{userId} AND status = 'ACTIVE' AND deleted = false
        """)
  int countByMatterIdAndUserId(@Param("matterId") Long matterId, @Param("userId") Long userId);

  /**
   * 判断用户是否是案件参与者.
   *
   * @param matterId 案件ID
   * @param userId 用户ID
   * @return 是否参与
   */
  default boolean existsByMatterIdAndUserId(Long matterId, Long userId) {
    return countByMatterIdAndUserId(matterId, userId) > 0;
  }
}
