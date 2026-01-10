package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 分页查询操作日志
     */
    @Select("<script>" +
            "SELECT * FROM sys_operation_log WHERE deleted = false " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='module != null'> AND module = #{module} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    IPage<OperationLog> selectLogPage(Page<OperationLog> page,
                                       @Param("userId") Long userId,
                                       @Param("module") String module,
                                       @Param("status") String status,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定日期之前的日志（软删除）
     */
    @org.apache.ibatis.annotations.Update("UPDATE sys_operation_log SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE created_at < #{beforeDate} AND deleted = false")
    int deleteLogsBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 统计指定日期之前的日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE created_at < #{beforeDate} AND deleted = false")
    long countLogsBeforeDate(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 查询所有符合条件的日志（不分页，用于导出）
     */
    @Select("<script>" +
            "SELECT * FROM sys_operation_log WHERE deleted = false " +
            "<if test='userId != null'> AND user_id = #{userId} </if>" +
            "<if test='module != null'> AND module = #{module} </if>" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<OperationLog> selectAllLogs(@Param("userId") Long userId,
                                     @Param("module") String module,
                                     @Param("status") String status,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_operation_log WHERE deleted = false " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "</script>")
    long countLogs(@Param("startTime") LocalDateTime startTime,
                   @Param("endTime") LocalDateTime endTime);

    /**
     * 按状态统计日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_operation_log WHERE deleted = false AND status = #{status} " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "</script>")
    long countLogsByStatus(@Param("status") String status,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按模块统计
     */
    @Select("<script>" +
            "SELECT module, COUNT(*) as count FROM sys_operation_log WHERE deleted = false " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "GROUP BY module ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> countByModule(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 按用户统计（Top 10）
     */
    @Select("<script>" +
            "SELECT user_name, COUNT(*) as count FROM sys_operation_log WHERE deleted = false " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "GROUP BY user_name ORDER BY count DESC LIMIT 10" +
            "</script>")
    List<Map<String, Object>> countByUser(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 平均执行时间
     */
    @Select("<script>" +
            "SELECT AVG(execution_time) FROM sys_operation_log WHERE deleted = false AND execution_time IS NOT NULL " +
            "<if test='startTime != null'> AND created_at &gt;= #{startTime} </if>" +
            "<if test='endTime != null'> AND created_at &lt;= #{endTime} </if>" +
            "</script>")
    Long avgExecutionTime(@Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);
}
