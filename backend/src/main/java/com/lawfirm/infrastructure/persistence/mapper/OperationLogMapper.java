package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

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
}
