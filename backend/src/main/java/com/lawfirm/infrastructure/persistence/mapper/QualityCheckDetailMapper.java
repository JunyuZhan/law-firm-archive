package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.QualityCheckDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 质量检查明细Mapper（M10-031）
 */
@Mapper
public interface QualityCheckDetailMapper extends BaseMapper<QualityCheckDetail> {

    /**
     * 查询检查的所有明细
     */
    @Select("SELECT * FROM quality_check_detail WHERE check_id = #{checkId} ORDER BY id")
    List<QualityCheckDetail> selectByCheckId(@Param("checkId") Long checkId);
}

