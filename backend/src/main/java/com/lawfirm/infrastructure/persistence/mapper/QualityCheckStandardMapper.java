package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.QualityCheckStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 质量检查标准Mapper（M10-030）
 */
@Mapper
public interface QualityCheckStandardMapper extends BaseMapper<QualityCheckStandard> {

    /**
     * 查询启用的检查标准
     */
    @Select("SELECT * FROM quality_check_standard WHERE enabled = true AND deleted = false ORDER BY sort_order, id")
    List<QualityCheckStandard> selectEnabledStandards();

    /**
     * 按分类查询检查标准
     */
    @Select("SELECT * FROM quality_check_standard WHERE category = #{category} AND enabled = true AND deleted = false ORDER BY sort_order, id")
    List<QualityCheckStandard> selectByCategory(@Param("category") String category);
}

