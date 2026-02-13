package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.domain.matter.entity.Matter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 案件 Mapper */
@Mapper
public interface MatterMapper extends BaseMapper<Matter> {

  /**
   * 根据案件编号查询.
   *
   * @param matterNo 案件编号
   * @return 案件信息
   */
  @Select("SELECT * FROM matter WHERE matter_no = #{matterNo} AND deleted = false")
  Matter selectByMatterNo(@Param("matterNo") String matterNo);

  /**
   * 分页查询案件.
   *
   * @param page 分页对象
   * @param query 查询条件
   * @return 案件分页结果
   */
  @Select(
      """
        <script>
        SELECT m.*
        FROM matter m
        WHERE m.deleted = false
        <if test="query.name != null and query.name != ''">
            AND m.name LIKE CONCAT('%', #{query.name}, '%')
        </if>
        <if test="query.matterNo != null and query.matterNo != ''">
            AND m.matter_no LIKE CONCAT('%', #{query.matterNo}, '%')
        </if>
        <if test="query.clientId != null">
            AND m.client_id = #{query.clientId}
        </if>
        <if test="query.leadLawyerId != null">
            AND m.lead_lawyer_id = #{query.leadLawyerId}
        </if>
        <if test="query.departmentId != null">
            AND m.department_id = #{query.departmentId}
        </if>
        <if test="query.matterType != null and query.matterType != ''">
            AND m.matter_type = #{query.matterType}
        </if>
        <if test="query.status != null and query.status != ''">
            AND m.status = #{query.status}
        </if>
        <if test="query.matterIds != null and query.matterIds.size() > 0">
            AND m.id IN
            <foreach collection="query.matterIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
        <if test="query.createdAtFrom != null">
            AND m.created_at &gt;= #{query.createdAtFrom}
        </if>
        <if test="query.createdAtTo != null">
            AND m.created_at &lt;= #{query.createdAtTo}
        </if>
        ORDER BY m.created_at DESC
        </script>
        """)
  IPage<Matter> selectMatterPage(Page<Matter> page, @Param("query") MatterQueryDTO query);

  /**
   * 查询律师参与的案件（支持时间筛选）.
   *
   * @param page 分页对象
   * @param userId 用户ID
   * @param name 案件名称
   * @param status 状态
   * @param createdAtFrom 创建时间起始
   * @param createdAtTo 创建时间结束
   * @return 案件分页结果
   */
  @Select(
      """
        <script>
        SELECT m.* FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND mp.status = 'ACTIVE' AND m.deleted = false
        <if test="name != null and name != ''">
            AND m.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="status != null and status != ''">
            AND m.status = #{status}
        </if>
        <if test="createdAtFrom != null">
            AND m.created_at &gt;= #{createdAtFrom}
        </if>
        <if test="createdAtTo != null">
            AND m.created_at &lt;= #{createdAtTo}
        </if>
        ORDER BY m.created_at DESC
        </script>
        """)
  IPage<Matter> selectByParticipantUserId(
      Page<Matter> page,
      @Param("userId") Long userId,
      @Param("name") String name,
      @Param("status") String status,
      @Param("createdAtFrom") java.time.LocalDateTime createdAtFrom,
      @Param("createdAtTo") java.time.LocalDateTime createdAtTo);

  /**
   * 统计用户参与的项目数量（按状态）.
   *
   * @param userId 用户ID
   * @param status 状态
   * @return 项目数量
   */
  @Select(
      """
        SELECT COUNT(DISTINCT m.id) FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND m.status = #{status} AND m.deleted = false
        """)
  int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

  /**
   * 查询用户最近参与的项目.
   *
   * @param userId 用户ID
   * @param limit 限制数量
   * @return 项目列表
   */
  @Select(
      """
        SELECT m.* FROM matter m
        INNER JOIN matter_participant mp ON m.id = mp.matter_id
        WHERE mp.user_id = #{userId} AND m.deleted = false
        ORDER BY m.updated_at DESC
        LIMIT #{limit}
        """)
  java.util.List<Matter> selectRecentByUserId(
      @Param("userId") Long userId, @Param("limit") int limit);

  /**
   * 统计客户关联的案件数量.
   *
   * @param clientId 客户ID
   * @return 案件数量
   */
  @Select("SELECT COUNT(*) FROM matter WHERE client_id = #{clientId} AND deleted = false")
  int countByClientId(@Param("clientId") Long clientId);

  /**
   * 查询指定前缀的最大编号（用于生成新编号） 例如：前缀为 "2026张三MS-"，查询所有以该前缀开头的编号，返回最大的序号部分 编号格式：前缀 + 4位序号，如
   * "2026张三MS-0001".
   *
   * @param prefix 前缀
   * @param prefixLength 前缀长度
   * @return 最大序号
   */
  @Select(
      """
        SELECT MAX(CAST(SUBSTRING(matter_no, #{prefixLength} + 1, 4) AS INTEGER)) as maxSeq
        FROM matter
        WHERE matter_no LIKE CONCAT(#{prefix}, '%')
        AND LENGTH(matter_no) = #{prefixLength} + 4
        AND deleted = false
        """)
  Integer selectMaxSequenceByPrefix(
      @Param("prefix") String prefix, @Param("prefixLength") int prefixLength);
}
