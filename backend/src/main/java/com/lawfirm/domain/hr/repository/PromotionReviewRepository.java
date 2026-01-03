package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.hr.entity.PromotionReview;
import com.lawfirm.infrastructure.persistence.mapper.PromotionReviewMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 晋升评审记录 Repository
 */
@Repository
public class PromotionReviewRepository extends ServiceImpl<PromotionReviewMapper, PromotionReview> {

    /**
     * 查询申请的所有评审记录
     */
    public List<PromotionReview> findByApplicationId(Long applicationId) {
        return baseMapper.selectByApplicationId(applicationId);
    }

    /**
     * 检查评审人是否已评审
     */
    public boolean hasReviewed(Long applicationId, Long reviewerId) {
        return baseMapper.countByApplicationAndReviewer(applicationId, reviewerId) > 0;
    }

    /**
     * 统计评审通过数量
     */
    public int countApproved(Long applicationId) {
        return baseMapper.countApproved(applicationId);
    }

    /**
     * 统计评审拒绝数量
     */
    public int countRejected(Long applicationId) {
        return baseMapper.countRejected(applicationId);
    }
}
