package com.lawfirm.domain.document.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.document.entity.SealApplication;
import com.lawfirm.infrastructure.persistence.mapper.SealApplicationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用印申请仓储
 */
@Repository
public class SealApplicationRepository extends AbstractRepository<SealApplicationMapper, SealApplication> {

    /**
     * 查询待审批的申请
     */
    public List<SealApplication> findPendingApplications() {
        return baseMapper.selectPendingApplications();
    }

    /**
     * 统计印章使用次数
     */
    public int countUsageBySealId(Long sealId) {
        return baseMapper.countUsageBySeaId(sealId);
    }

    /**
     * 统计印章待处理的申请数量
     */
    public int countPendingBySealId(Long sealId) {
        return baseMapper.countPendingBySealId(sealId);
    }
}
