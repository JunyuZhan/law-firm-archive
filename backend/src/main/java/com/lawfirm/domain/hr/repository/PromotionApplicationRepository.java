package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PromotionApplication;
import com.lawfirm.infrastructure.persistence.mapper.PromotionApplicationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 晋升申请 Repository
 */
@Repository
public class PromotionApplicationRepository extends AbstractRepository<PromotionApplicationMapper, PromotionApplication> {

    /**
     * 根据申请编号查询
     */
    public Optional<PromotionApplication> findByApplicationNo(String applicationNo) {
        return Optional.ofNullable(baseMapper.selectByApplicationNo(applicationNo));
    }

    /**
     * 统计待审批数量
     */
    public int countPending() {
        return baseMapper.countPending();
    }
}
