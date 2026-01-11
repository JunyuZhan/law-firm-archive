package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.openapi.entity.VerificationCode;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

/**
 * 验证码 Mapper
 */
@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {

    /**
     * 根据验证码查询
     */
    @Select("SELECT * FROM openapi_verification_code WHERE verification_code = #{code} AND deleted = false")
    VerificationCode selectByCode(@Param("code") String code);

    /**
     * 根据业务ID和类型查询
     */
    @Select("SELECT * FROM openapi_verification_code WHERE business_id = #{businessId} AND verification_type = #{type} AND status = 'ACTIVE' AND deleted = false")
    VerificationCode selectByBusinessIdAndType(@Param("businessId") Long businessId, @Param("type") String type);

    /**
     * 增加验证次数
     */
    @Update("UPDATE openapi_verification_code SET verify_count = verify_count + 1 WHERE id = #{id}")
    int incrementVerifyCount(@Param("id") Long id);

    /**
     * 撤销验证码
     */
    @Update("UPDATE openapi_verification_code SET status = 'REVOKED' WHERE id = #{id}")
    int revokeCode(@Param("id") Long id);

    /**
     * 批量更新过期验证码状态
     */
    @Update("UPDATE openapi_verification_code SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP AND deleted = false")
    int updateExpiredCodes();
}

