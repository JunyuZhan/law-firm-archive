package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.openapi.entity.OpenApiAccessLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OpenAPI 访问日志 Mapper
 */
@Mapper
public interface OpenApiAccessLogMapper extends BaseMapper<OpenApiAccessLog> {

    /**
     * 分页查询访问日志
     */
    @Select("<script>" +
            "SELECT * FROM openapi_access_log WHERE 1=1 " +
            "<if test='tokenId != null'> AND token_id = #{tokenId} </if>" +
            "<if test='clientId != null'> AND client_id = #{clientId} </if>" +
            "<if test='matterId != null'> AND matter_id = #{matterId} </if>" +
            "<if test='accessResult != null'> AND access_result = #{accessResult} </if>" +
            "<if test='startTime != null'> AND access_at >= #{startTime} </if>" +
            "<if test='endTime != null'> AND access_at &lt;= #{endTime} </if>" +
            "ORDER BY access_at DESC" +
            "</script>")
    IPage<OpenApiAccessLog> selectPage(Page<OpenApiAccessLog> page,
                                        @Param("tokenId") Long tokenId,
                                        @Param("clientId") Long clientId,
                                        @Param("matterId") Long matterId,
                                        @Param("accessResult") String accessResult,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定令牌的访问次数
     */
    @Select("SELECT COUNT(*) FROM openapi_access_log WHERE token_id = #{tokenId}")
    int countByTokenId(@Param("tokenId") Long tokenId);

    /**
     * 统计指定客户的访问次数
     */
    @Select("SELECT COUNT(*) FROM openapi_access_log WHERE client_id = #{clientId} AND access_at >= #{startTime}")
    int countByClientIdSince(@Param("clientId") Long clientId, @Param("startTime") LocalDateTime startTime);

    /**
     * 查询最近的访问记录
     */
    @Select("SELECT * FROM openapi_access_log WHERE token_id = #{tokenId} ORDER BY access_at DESC LIMIT #{limit}")
    List<OpenApiAccessLog> selectRecentByTokenId(@Param("tokenId") Long tokenId, @Param("limit") int limit);
}

