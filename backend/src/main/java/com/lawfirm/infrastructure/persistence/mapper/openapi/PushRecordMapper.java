package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.openapi.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 推送记录 Mapper
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {

    /**
     * 分页查询推送记录
     */
    @Select("""
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
    IPage<PushRecord> selectPage(Page<PushRecord> page,
                                  @Param("matterId") Long matterId,
                                  @Param("clientId") Long clientId,
                                  @Param("status") String status);

    /**
     * 根据项目ID查询最近的成功推送记录
     */
    @Select("""
        SELECT * FROM openapi_push_record 
        WHERE matter_id = #{matterId} AND status = 'SUCCESS' AND deleted = false
        ORDER BY created_at DESC LIMIT 1
        """)
    PushRecord selectLatestSuccessByMatterId(@Param("matterId") Long matterId);

    /**
     * 根据项目ID查询所有记录
     */
    @Select("SELECT * FROM openapi_push_record WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
    List<PushRecord> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 更新推送状态
     */
    @Update("""
        UPDATE openapi_push_record 
        SET status = #{status}, 
            external_id = #{externalId}, 
            external_url = #{externalUrl},
            error_message = #{errorMessage},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int updatePushResult(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("externalId") String externalId,
                         @Param("externalUrl") String externalUrl,
                         @Param("errorMessage") String errorMessage);

    /**
     * 统计项目的推送次数
     */
    @Select("SELECT COUNT(*) FROM openapi_push_record WHERE matter_id = #{matterId} AND status = 'SUCCESS' AND deleted = false")
    int countSuccessByMatterId(@Param("matterId") Long matterId);
}

