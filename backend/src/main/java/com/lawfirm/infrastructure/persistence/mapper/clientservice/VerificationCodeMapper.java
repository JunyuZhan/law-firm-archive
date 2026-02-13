package com.lawfirm.infrastructure.persistence.mapper.clientservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.clientservice.entity.VerificationCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 验证码 Mapper */
@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {

  /**
   * 根据验证码查询.
   *
   * @param code 验证码
   * @return 验证码信息
   */
  @Select(
      "SELECT * FROM openapi_verification_code WHERE verification_code = #{code} AND deleted = false")
  VerificationCode selectByCode(@Param("code") String code);

  /**
   * 根据业务ID和类型查询.
   *
   * @param businessId 业务ID
   * @param type 验证类型
   * @return 验证码信息
   */
  @Select(
      "SELECT * FROM openapi_verification_code WHERE business_id = #{businessId} "
          + "AND verification_type = #{type} AND status = 'ACTIVE' "
          + "AND deleted = false")
  VerificationCode selectByBusinessIdAndType(
      @Param("businessId") Long businessId, @Param("type") String type);

  /**
   * 增加验证次数.
   *
   * @param id 验证码ID
   * @return 更新数量
   */
  @Update("UPDATE openapi_verification_code SET verify_count = verify_count + 1 WHERE id = #{id}")
  int incrementVerifyCount(@Param("id") Long id);

  /**
   * 撤销验证码.
   *
   * @param id 验证码ID
   * @return 更新数量
   */
  @Update("UPDATE openapi_verification_code SET status = 'REVOKED' WHERE id = #{id}")
  int revokeCode(@Param("id") Long id);

  /**
   * 批量更新过期验证码状态.
   *
   * @return 更新数量
   */
  @Update(
      "UPDATE openapi_verification_code SET status = 'EXPIRED' "
          + "WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP "
          + "AND deleted = false")
  int updateExpiredCodes();
}
