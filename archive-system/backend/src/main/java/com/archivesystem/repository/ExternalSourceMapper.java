package com.archivesystem.repository;

import com.archivesystem.entity.ExternalSource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 外部系统来源Mapper接口.
 */
@Mapper
public interface ExternalSourceMapper extends BaseMapper<ExternalSource> {

    /**
     * 根据来源编码查询.
     */
    @Select("SELECT * FROM arc_external_source WHERE source_code = #{sourceCode} AND deleted = false")
    ExternalSource selectBySourceCode(@Param("sourceCode") String sourceCode);
}
