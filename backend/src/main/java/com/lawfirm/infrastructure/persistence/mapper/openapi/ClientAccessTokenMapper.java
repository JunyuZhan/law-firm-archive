package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.openapi.entity.ClientAccessToken;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户访问令牌 Mapper
 */
@Mapper
public interface ClientAccessTokenMapper extends BaseMapper<ClientAccessToken> {

    /**
     * 根据令牌查询
     */
    @Select("SELECT * FROM openapi_client_token WHERE token = #{token} AND deleted = false")
    ClientAccessToken selectByToken(@Param("token") String token);

    /**
     * 根据客户ID查询有效令牌列表
     */
    @Select("SELECT * FROM openapi_client_token WHERE client_id = #{clientId} AND status = 'ACTIVE' AND deleted = false ORDER BY created_at DESC")
    List<ClientAccessToken> selectActiveByClientId(@Param("clientId") Long clientId);

    /**
     * 根据项目ID查询有效令牌列表
     */
    @Select("SELECT * FROM openapi_client_token WHERE matter_id = #{matterId} AND status = 'ACTIVE' AND deleted = false ORDER BY created_at DESC")
    List<ClientAccessToken> selectActiveByMatterId(@Param("matterId") Long matterId);

    /**
     * 分页查询令牌
     */
    @Select("<script>" +
            "SELECT * FROM openapi_client_token WHERE deleted = false " +
            "<if test='clientId != null'> AND client_id = #{clientId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<ClientAccessToken> selectPage(Page<ClientAccessToken> page,
                                         @Param("clientId") Long clientId,
                                         @Param("matterId") Long matterId,
                                         @Param("status") String status);

    /**
     * 更新访问计数和最后访问信息
     */
    @Update("UPDATE openapi_client_token SET access_count = access_count + 1, last_access_ip = #{ip}, last_access_at = #{accessTime}, updated_at = #{accessTime} WHERE id = #{id}")
    int updateAccessInfo(@Param("id") Long id, @Param("ip") String ip, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 撤销令牌
     */
    @Update("UPDATE openapi_client_token SET status = 'REVOKED', revoked_at = #{revokedAt}, revoked_by = #{revokedBy}, revoke_reason = #{reason}, updated_at = #{revokedAt} WHERE id = #{id}")
    int revokeToken(@Param("id") Long id, @Param("revokedBy") Long revokedBy, @Param("revokedAt") LocalDateTime revokedAt, @Param("reason") String reason);

    /**
     * 批量更新过期令牌状态
     */
    @Update("UPDATE openapi_client_token SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP AND deleted = false")
    int updateExpiredTokens();
}

