package com.lawfirm.infrastructure.persistence.mapper.clientservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.clientservice.entity.ClientFile;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 客户上传文件 Mapper */
@Mapper
public interface ClientFileMapper extends BaseMapper<ClientFile> {

  /**
   * 分页查询项目的客户文件.
   *
   * @param page 分页对象
   * @param matterId 项目ID
   * @param status 状态
   * @return 客户文件分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM openapi_client_file "
          + "WHERE deleted = false AND matter_id = #{matterId} "
          + "<if test='status != null'> AND status = #{status} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  Page<ClientFile> selectPage(
      Page<ClientFile> page, @Param("matterId") Long matterId, @Param("status") String status);

  /**
   * 查询项目待同步的文件列表.
   *
   * @param matterId 项目ID
   * @return 待同步文件列表
   */
  @Select(
      "SELECT * FROM openapi_client_file "
          + "WHERE deleted = false AND matter_id = #{matterId} AND status = 'PENDING' "
          + "ORDER BY created_at DESC")
  List<ClientFile> selectPendingByMatterId(@Param("matterId") Long matterId);

  /**
   * 统计项目待同步文件数量.
   *
   * @param matterId 项目ID
   * @return 待同步文件数量
   */
  @Select(
      "SELECT COUNT(*) FROM openapi_client_file "
          + "WHERE deleted = false AND matter_id = #{matterId} AND status = 'PENDING'")
  int countPendingByMatterId(@Param("matterId") Long matterId);

  /**
   * 根据外部文件ID查询.
   *
   * @param externalFileId 外部文件ID
   * @return 客户文件
   */
  @Select(
      "SELECT * FROM openapi_client_file "
          + "WHERE deleted = false AND external_file_id = #{externalFileId}")
  ClientFile selectByExternalFileId(@Param("externalFileId") String externalFileId);

  /**
   * 更新同步状态.
   *
   * @param id 文件ID
   * @param status 状态
   * @param localDocumentId 本地文档ID
   * @param targetDossierId 目标卷宗ID
   * @param syncedBy 同步人ID
   * @param errorMessage 错误信息
   * @return 更新数量
   */
  @Update(
      "UPDATE openapi_client_file SET "
          + "status = #{status}, "
          + "local_document_id = #{localDocumentId}, "
          + "target_dossier_id = #{targetDossierId}, "
          + "synced_at = NOW(), "
          + "synced_by = #{syncedBy}, "
          + "error_message = #{errorMessage}, "
          + "updated_at = NOW() "
          + "WHERE id = #{id}")
  int updateSyncStatus(
      @Param("id") Long id,
      @Param("status") String status,
      @Param("localDocumentId") Long localDocumentId,
      @Param("targetDossierId") Long targetDossierId,
      @Param("syncedBy") Long syncedBy,
      @Param("errorMessage") String errorMessage);

  /**
   * 标记为已删除（客服系统删除后回调）.
   *
   * @param externalFileId 外部文件ID
   * @return 更新数量
   */
  @Update(
      "UPDATE openapi_client_file SET status = 'DELETED', updated_at = NOW() "
          + "WHERE external_file_id = #{externalFileId}")
  int markAsDeleted(@Param("externalFileId") String externalFileId);
}
