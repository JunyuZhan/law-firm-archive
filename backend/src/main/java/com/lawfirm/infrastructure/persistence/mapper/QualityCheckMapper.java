package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.QualityCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 质量检查Mapper（M10-031）
 */
@Mapper
public interface QualityCheckMapper extends BaseMapper<QualityCheck> {

    /**
     * 根据检查编号查询
     */
    @Select("SELECT * FROM quality_check WHERE check_no = #{checkNo} AND deleted = false")
    QualityCheck selectByCheckNo(@Param("checkNo") String checkNo);

    /**
     * 查询项目的所有检查
     */
    @Select("SELECT * FROM quality_check WHERE matter_id = #{matterId} AND deleted = false ORDER BY check_date DESC")
    List<QualityCheck> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询进行中的检查
     */
    @Select("SELECT * FROM quality_check WHERE status = 'IN_PROGRESS' AND deleted = false ORDER BY check_date DESC")
    List<QualityCheck> selectInProgress();
}

