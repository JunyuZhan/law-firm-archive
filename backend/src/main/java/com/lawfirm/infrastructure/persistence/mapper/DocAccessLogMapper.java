package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.document.entity.DocAccessLog;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 文档访问日志Mapper */
@Mapper
public interface DocAccessLogMapper extends BaseMapper<DocAccessLog> {

  /**
   * 分页查询访问日志.
   *
   * @param page 分页对象
   * @param documentId 文档ID
   * @param userId 用户ID
   * @param actionType 操作类型
   * @return 访问日志分页结果
   */
  @Select(
      "<script>"
          + "SELECT * FROM doc_access_log WHERE 1=1 "
          + "<if test='documentId != null'> AND document_id = #{documentId} </if>"
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='actionType != null'> AND action_type = #{actionType} </if>"
          + "ORDER BY created_at DESC"
          + "</script>")
  IPage<DocAccessLog> selectLogPage(
      Page<DocAccessLog> page,
      @Param("documentId") Long documentId,
      @Param("userId") Long userId,
      @Param("actionType") String actionType);

  /**
   * 统计文档访问次数.
   *
   * @param documentId 文档ID
   * @return 访问次数
   */
  @Select("SELECT COUNT(*) FROM doc_access_log WHERE document_id = #{documentId}")
  int countByDocumentId(@Param("documentId") Long documentId);

  /**
   * 按用户统计访问次数（M5-044）.
   *
   * @param documentId 文档ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 统计结果
   */
  @Select(
      "<script>"
          + "SELECT user_id, COUNT(*) as access_count "
          + "FROM doc_access_log WHERE 1=1 "
          + "<if test='documentId != null'> AND document_id = #{documentId} </if>"
          + "<if test='startTime != null'> AND created_at >= #{startTime} </if>"
          + "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>"
          + "GROUP BY user_id ORDER BY access_count DESC"
          + "</script>")
  List<Map<String, Object>> countByUser(
      @Param("documentId") Long documentId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);

  /**
   * 按文档统计访问次数（M5-044）.
   *
   * @param userId 用户ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 统计结果
   */
  @Select(
      "<script>"
          + "SELECT document_id, COUNT(*) as access_count "
          + "FROM doc_access_log WHERE 1=1 "
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='startTime != null'> AND created_at >= #{startTime} </if>"
          + "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>"
          + "GROUP BY document_id ORDER BY access_count DESC"
          + "</script>")
  List<Map<String, Object>> countByDocument(
      @Param("userId") Long userId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);

  /**
   * 按操作类型统计（M5-044）.
   *
   * @param documentId 文档ID
   * @param userId 用户ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 统计结果
   */
  @Select(
      "<script>"
          + "SELECT action_type, COUNT(*) as access_count "
          + "FROM doc_access_log WHERE 1=1 "
          + "<if test='documentId != null'> AND document_id = #{documentId} </if>"
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='startTime != null'> AND created_at >= #{startTime} </if>"
          + "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>"
          + "GROUP BY action_type ORDER BY access_count DESC"
          + "</script>")
  List<Map<String, Object>> countByActionType(
      @Param("documentId") Long documentId,
      @Param("userId") Long userId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);

  /**
   * 按时间统计访问趋势（M5-044）.
   *
   * @param documentId 文档ID
   * @param userId 用户ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 统计结果
   */
  @Select(
      "<script>"
          + "SELECT DATE(created_at) as access_date, COUNT(*) as access_count "
          + "FROM doc_access_log WHERE 1=1 "
          + "<if test='documentId != null'> AND document_id = #{documentId} </if>"
          + "<if test='userId != null'> AND user_id = #{userId} </if>"
          + "<if test='startTime != null'> AND created_at >= #{startTime} </if>"
          + "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>"
          + "GROUP BY DATE(created_at) ORDER BY access_date DESC"
          + "</script>")
  List<Map<String, Object>> countByDate(
      @Param("documentId") Long documentId,
      @Param("userId") Long userId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);

  /**
   * 查询审计报告数据（M5-045）.
   *
   * @param documentId 文档ID
   * @param userId 用户ID
   * @param actionType 操作类型
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return 审计报告数据
   */
  @Select(
      "<script>"
          + "SELECT dal.*, d.title as document_title, d.doc_no as document_no, "
          + "u.real_name as user_name, u.username "
          + "FROM doc_access_log dal "
          + "LEFT JOIN doc_document d ON dal.document_id = d.id "
          + "LEFT JOIN sys_user u ON dal.user_id = u.id "
          + "WHERE 1=1 "
          + "<if test='documentId != null'> AND dal.document_id = #{documentId} </if>"
          + "<if test='userId != null'> AND dal.user_id = #{userId} </if>"
          + "<if test='actionType != null'> AND dal.action_type = #{actionType} </if>"
          + "<if test='startTime != null'> AND dal.created_at >= #{startTime} </if>"
          + "<if test='endTime != null'> AND dal.created_at &lt;= #{endTime} </if>"
          + "ORDER BY dal.created_at DESC"
          + "</script>")
  List<Map<String, Object>> queryAuditReport(
      @Param("documentId") Long documentId,
      @Param("userId") Long userId,
      @Param("actionType") String actionType,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime);
}
