package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.CommissionDetail;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提成分配明细 Mapper */
@Mapper
public interface CommissionDetailMapper extends BaseMapper<CommissionDetail> {

  /**
   * 根据提成ID查询明细.
   *
   * @param commissionId 提成ID
   * @return 提成分配明细列表
   */
  @Select(
      "SELECT * FROM finance_commission_detail WHERE commission_id = #{commissionId} AND deleted = false")
  List<CommissionDetail> selectByCommissionId(@Param("commissionId") Long commissionId);

  /**
   * 根据用户ID查询明细.
   *
   * @param userId 用户ID
   * @return 提成分配明细列表
   */
  @Select(
      "SELECT * FROM finance_commission_detail WHERE user_id = #{userId} AND deleted = false ORDER BY created_at DESC")
  List<CommissionDetail> selectByUserId(@Param("userId") Long userId);
}
