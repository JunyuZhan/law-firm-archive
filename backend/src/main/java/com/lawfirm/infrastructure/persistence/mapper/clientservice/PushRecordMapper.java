package com.lawfirm.infrastructure.persistence.mapper.clientservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.clientservice.entity.PushRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 推送记录 Mapper */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {

  /**
   * 分页查询推送记录.
   *
   * @param page 分页对象
   * @param matterId 项目ID
   * @param clientId 客户ID
   * @param status 状态
   * @return 推送记录分页结果
   */
  @Select(
      """
        <script>
        SELECT * FROM openapi_push_record
        WHERE deleted = false
        <if test="matterId != null">
            AND matter_id = #{matterId}
        </if>
        <if test="clientId != null">
            AND client_id = #{clientId}
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        ORDER BY created_at DESC
        </script>
        """)
  IPage<PushRecord> selectPage(
      Page<PushRecord> page,
      @Param("matterId") Long matterId,
      @Param("clientId") Long clientId,
      @Param("status") String status);

  /**
   * 根据项目ID查询最近的成功推送记录.
   *
   * @param matterId 项目ID
   * @return 最近成功推送记录
   */
  @Select(
      """
        SELECT * FROM openapi_push_record
        WHERE matter_id = #{matterId} AND status = 'SUCCESS' AND deleted = false
        ORDER BY created_at DESC LIMIT 1
        """)
  PushRecord selectLatestSuccessByMatterId(@Param("matterId") Long matterId);

  /**
   * 根据项目ID查询所有记录.
   *
   * @param matterId 项目ID
   * @return 推送记录列表
   */
  @Select(
      "SELECT * FROM openapi_push_record WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
  List<PushRecord> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 更新推送状态.
   *
   * @param id 记录ID
   * @param status 状态
   * @param externalId 外部ID
   * @param externalUrl 外部URL
   * @param errorMessage 错误信息
   * @return 更新数量
   */
  @Update(
      """
        UPDATE openapi_push_record
        SET status = #{status},
            external_id = #{externalId},
            external_url = #{externalUrl},
            error_message = #{errorMessage},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
  int updatePushResult(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("externalId") String externalId,
      @Param("externalUrl") String externalUrl,
      @Param("errorMessage") String errorMessage);

  /**
   * 统计项目的推送次数.
   *
   * @param matterId 项目ID
   * @return 推送次数
   */
  @Select(
      "SELECT COUNT(*) FROM openapi_push_record WHERE matter_id = #{matterId} "
          + "AND status = 'SUCCESS' AND deleted = false")
  int countSuccessByMatterId(@Param("matterId") Long matterId);

  /**
   * 更新同一项目所有历史成功记录的访问链接（二次推送时同步更新历史记录）.
   *
   * @param matterId 项目ID
   * @param externalUrl 新的外部访问链接
   * @return 更新数量
   */
  @Update(
      """
        UPDATE openapi_push_record
        SET external_url = #{externalUrl},
            updated_at = CURRENT_TIMESTAMP
        WHERE matter_id = #{matterId}
          AND status = 'SUCCESS'
          AND deleted = false
        """)
  int updateHistoricalExternalUrl(
      @Param("matterId") Long matterId, @Param("externalUrl") String externalUrl);
}
