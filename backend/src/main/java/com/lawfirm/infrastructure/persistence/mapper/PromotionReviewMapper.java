package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PromotionReview;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 晋升评审记录 Mapper */
@Mapper
public interface PromotionReviewMapper extends BaseMapper<PromotionReview> {

  /**
   * 查询申请的所有评审记录.
   *
   * @param applicationId 申请ID
   * @return 评审记录列表
   */
  @Select(
      "SELECT * FROM hr_promotion_review WHERE application_id = #{applicationId} ORDER BY created_at ASC")
  List<PromotionReview> selectByApplicationId(@Param("applicationId") Long applicationId);

  /**
   * 查询评审人是否已评审.
   *
   * @param applicationId 申请ID
   * @param reviewerId 评审人ID
   * @return 评审数量
   */
  @Select(
      "SELECT COUNT(*) FROM hr_promotion_review "
          + "WHERE application_id = #{applicationId} AND reviewer_id = #{reviewerId}")
  int countByApplicationAndReviewer(
      @Param("applicationId") Long applicationId, @Param("reviewerId") Long reviewerId);

  /**
   * 统计评审通过数量.
   *
   * @param applicationId 申请ID
   * @return 通过数量
   */
  @Select(
      "SELECT COUNT(*) FROM hr_promotion_review WHERE application_id = #{applicationId} AND review_opinion = 'APPROVE'")
  int countApproved(@Param("applicationId") Long applicationId);

  /**
   * 统计评审拒绝数量.
   *
   * @param applicationId 申请ID
   * @return 拒绝数量
   */
  @Select(
      "SELECT COUNT(*) FROM hr_promotion_review WHERE application_id = #{applicationId} AND review_opinion = 'REJECT'")
  int countRejected(@Param("applicationId") Long applicationId);
}
