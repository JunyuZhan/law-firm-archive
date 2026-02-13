package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.client.entity.ConflictCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 利益冲突检查 Mapper */
@Mapper
public interface ConflictCheckMapper extends BaseMapper<ConflictCheck> {

  /**
   * 根据检查编号查询.
   *
   * @param checkNo 检查编号
   * @return 利冲检查记录
   */
  @Select("SELECT * FROM crm_conflict_check WHERE check_no = #{checkNo} AND deleted = false")
  ConflictCheck selectByCheckNo(@Param("checkNo") String checkNo);

  /**
   * 分页查询利冲记录.
   *
   * @param page 分页对象
   * @param checkType 检查类型
   * @param status 状态
   * @param clientName 客户名称
   * @param applicantId 申请人ID
   * @return 利冲检查分页结果
   */
  @Select(
      """
        <script>
        SELECT cc.*, u.real_name as applicant_name, r.real_name as reviewer_name
        FROM crm_conflict_check cc
        LEFT JOIN sys_user u ON cc.applicant_id = u.id
        LEFT JOIN sys_user r ON cc.reviewer_id = r.id
        WHERE cc.deleted = false
        <if test="checkType != null and checkType != ''">
            AND cc.check_type = #{checkType}
        </if>
        <if test="status != null and status != ''">
            AND cc.status = #{status}
        </if>
        <if test="clientName != null and clientName != ''">
            AND cc.client_name LIKE CONCAT('%', #{clientName}, '%')
        </if>
        <if test="applicantId != null">
            AND cc.applicant_id = #{applicantId}
        </if>
        ORDER BY cc.id DESC
        </script>
        """)
  IPage<ConflictCheck> selectConflictCheckPage(
      Page<ConflictCheck> page,
      @Param("checkType") String checkType,
      @Param("status") String status,
      @Param("clientName") String clientName,
      @Param("applicantId") Long applicantId);

  /**
   * 查询客户的利冲记录.
   *
   * @param page 分页对象
   * @param clientId 客户ID
   * @return 利冲检查分页结果
   */
  @Select(
      "SELECT * FROM crm_conflict_check WHERE client_id = #{clientId} AND deleted = false ORDER BY id DESC")
  IPage<ConflictCheck> selectByClientId(Page<ConflictCheck> page, @Param("clientId") Long clientId);
}
