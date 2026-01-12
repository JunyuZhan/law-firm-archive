package com.lawfirm.infrastructure.persistence.mapper.openapi;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.openapi.entity.ClientFile;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 客户上传文件 Mapper
 */
@Mapper
public interface ClientFileMapper extends BaseMapper<ClientFile> {

    /**
     * 分页查询项目的客户文件
     */
    @Select("<script>" +
            "SELECT * FROM openapi_client_file " +
            "WHERE deleted = false AND matter_id = #{matterId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    Page<ClientFile> selectPage(Page<ClientFile> page, 
                                 @Param("matterId") Long matterId, 
                                 @Param("status") String status);

    /**
     * 查询项目待同步的文件列表
     */
    @Select("SELECT * FROM openapi_client_file " +
            "WHERE deleted = false AND matter_id = #{matterId} AND status = 'PENDING' " +
            "ORDER BY created_at DESC")
    List<ClientFile> selectPendingByMatterId(@Param("matterId") Long matterId);

    /**
     * 统计项目待同步文件数量
     */
    @Select("SELECT COUNT(*) FROM openapi_client_file " +
            "WHERE deleted = false AND matter_id = #{matterId} AND status = 'PENDING'")
    int countPendingByMatterId(@Param("matterId") Long matterId);

    /**
     * 根据外部文件ID查询
     */
    @Select("SELECT * FROM openapi_client_file " +
            "WHERE deleted = false AND external_file_id = #{externalFileId}")
    ClientFile selectByExternalFileId(@Param("externalFileId") String externalFileId);

    /**
     * 更新同步状态
     */
    @Update("UPDATE openapi_client_file SET " +
            "status = #{status}, " +
            "local_document_id = #{localDocumentId}, " +
            "target_dossier_id = #{targetDossierId}, " +
            "synced_at = NOW(), " +
            "synced_by = #{syncedBy}, " +
            "error_message = #{errorMessage}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateSyncStatus(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("localDocumentId") Long localDocumentId,
                         @Param("targetDossierId") Long targetDossierId,
                         @Param("syncedBy") Long syncedBy,
                         @Param("errorMessage") String errorMessage);

    /**
     * 标记为已删除（客服系统删除后回调）
     */
    @Update("UPDATE openapi_client_file SET status = 'DELETED', updated_at = NOW() " +
            "WHERE external_file_id = #{externalFileId}")
    int markAsDeleted(@Param("externalFileId") String externalFileId);
}
