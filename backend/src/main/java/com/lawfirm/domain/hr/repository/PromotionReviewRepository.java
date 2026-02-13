package com.lawfirm.domain.hr.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.hr.entity.PromotionReview;
import com.lawfirm.infrastructure.persistence.mapper.PromotionReviewMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 晋升评审记录 Repository. */
@Repository
public class PromotionReviewRepository extends ServiceImpl<PromotionReviewMapper, PromotionReview> {

  /**
   * 查询申请的所有评审记录.
   *
   * @param applicationId 申请ID
   * @return 评审记录列表
   */
  public List<PromotionReview> findByApplicationId(final Long applicationId) {
    return baseMapper.selectByApplicationId(applicationId);
  }

  /**
   * 检查评审人是否已评审.
   *
   * @param applicationId 申请ID
   * @param reviewerId 评审人ID
   * @return 是否已评审
   */
  public boolean hasReviewed(final Long applicationId, final Long reviewerId) {
    return baseMapper.countByApplicationAndReviewer(applicationId, reviewerId) > 0;
  }

  /**
   * 统计评审通过数量.
   *
   * @param applicationId 申请ID
   * @return 通过数量
   */
  public int countApproved(final Long applicationId) {
    return baseMapper.countApproved(applicationId);
  }

  /**
   * 统计评审拒绝数量.
   *
   * @param applicationId 申请ID
   * @return 拒绝数量
   */
  public int countRejected(final Long applicationId) {
    return baseMapper.countRejected(applicationId);
  }
}
