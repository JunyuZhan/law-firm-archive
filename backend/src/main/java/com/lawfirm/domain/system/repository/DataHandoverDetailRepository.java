package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.DataHandoverDetail;
import com.lawfirm.infrastructure.persistence.mapper.DataHandoverDetailMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据交接明细仓储
 */
@Repository
public class DataHandoverDetailRepository extends ServiceImpl<DataHandoverDetailMapper, DataHandoverDetail> {

    /**
     * 根据交接单ID查询明细
     */
    public List<DataHandoverDetail> findByHandoverId(Long handoverId) {
        return baseMapper.selectByHandoverId(handoverId);
    }

    /**
     * 根据交接单ID和数据类型查询明细
     */
    public List<DataHandoverDetail> findByHandoverIdAndType(Long handoverId, String dataType) {
        return baseMapper.selectByHandoverIdAndType(handoverId, dataType);
    }

    /**
     * 统计交接单明细数量
     */
    public int countByHandoverId(Long handoverId) {
        return baseMapper.countByHandoverId(handoverId);
    }

    /**
     * 统计交接单指定状态的明细数量
     */
    public int countByHandoverIdAndStatus(Long handoverId, String status) {
        return baseMapper.countByHandoverIdAndStatus(handoverId, status);
    }

    /**
     * 批量更新状态
     */
    public int updateStatusByHandoverId(Long handoverId, String status) {
        return baseMapper.updateStatusByHandoverId(handoverId, status);
    }
}

