package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.DataHandoverDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 数据交接明细Mapper
 */
@Mapper
public interface DataHandoverDetailMapper extends BaseMapper<DataHandoverDetail> {

    /**
     * 根据交接单ID查询明细
     */
    @Select("SELECT * FROM sys_data_handover_detail WHERE handover_id = #{handoverId} AND deleted = false ORDER BY id")
    List<DataHandoverDetail> selectByHandoverId(@Param("handoverId") Long handoverId);

    /**
     * 根据交接单ID和数据类型查询明细
     */
    @Select("SELECT * FROM sys_data_handover_detail WHERE handover_id = #{handoverId} AND data_type = #{dataType} AND deleted = false ORDER BY id")
    List<DataHandoverDetail> selectByHandoverIdAndType(@Param("handoverId") Long handoverId,
                                                        @Param("dataType") String dataType);

    /**
     * 根据交接单ID统计明细数量
     */
    @Select("SELECT COUNT(*) FROM sys_data_handover_detail WHERE handover_id = #{handoverId} AND deleted = false")
    int countByHandoverId(@Param("handoverId") Long handoverId);

    /**
     * 根据交接单ID和状态统计
     */
    @Select("SELECT COUNT(*) FROM sys_data_handover_detail WHERE handover_id = #{handoverId} AND status = #{status} AND deleted = false")
    int countByHandoverIdAndStatus(@Param("handoverId") Long handoverId, @Param("status") String status);

    /**
     * 批量更新状态
     */
    @Update("UPDATE sys_data_handover_detail SET status = #{status}, executed_at = NOW() WHERE handover_id = #{handoverId} AND deleted = false")
    int updateStatusByHandoverId(@Param("handoverId") Long handoverId, @Param("status") String status);
}

