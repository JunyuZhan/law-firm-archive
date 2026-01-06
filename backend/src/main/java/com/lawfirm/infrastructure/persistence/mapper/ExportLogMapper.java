package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.ExportLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 导出日志 Mapper
 * 
 * Requirements: 6.7
 */
@Mapper
public interface ExportLogMapper extends BaseMapper<ExportLog> {

    /**
     * 查询用户的导出日志
     */
    @Select("SELECT * FROM sys_export_log WHERE exported_by = #{userId} ORDER BY exported_at DESC LIMIT #{limit}")
    List<ExportLog> selectByExportedBy(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定类型的导出日志
     */
    @Select("SELECT * FROM sys_export_log WHERE export_type = #{exportType} ORDER BY exported_at DESC LIMIT #{limit}")
    List<ExportLog> selectByExportType(@Param("exportType") String exportType, @Param("limit") int limit);
}
