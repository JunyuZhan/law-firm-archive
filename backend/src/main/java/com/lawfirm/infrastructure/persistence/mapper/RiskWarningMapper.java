package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.knowledge.entity.RiskWarning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 风险预警Mapper（M10-033）
 */
@Mapper
public interface RiskWarningMapper extends BaseMapper<RiskWarning> {

    /**
     * 根据预警编号查询
     */
    @Select("SELECT * FROM risk_warning WHERE warning_no = #{warningNo} AND deleted = false")
    RiskWarning selectByWarningNo(@Param("warningNo") String warningNo);

    /**
     * 查询项目的所有预警
     */
    @Select("SELECT * FROM risk_warning WHERE matter_id = #{matterId} AND deleted = false ORDER BY risk_level DESC, created_at DESC")
    List<RiskWarning> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 查询活跃的预警
     */
    @Select("SELECT * FROM risk_warning WHERE status = 'ACTIVE' AND deleted = false ORDER BY risk_level DESC, created_at DESC")
    List<RiskWarning> selectActiveWarnings();

    /**
     * 查询高风险预警
     */
    @Select("SELECT * FROM risk_warning WHERE risk_level = 'HIGH' AND status = 'ACTIVE' AND deleted = false ORDER BY created_at DESC")
    List<RiskWarning> selectHighRiskWarnings();
}

