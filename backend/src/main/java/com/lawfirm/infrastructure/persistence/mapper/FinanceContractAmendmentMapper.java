package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 财务合同变更记录Mapper */
@Mapper
public interface FinanceContractAmendmentMapper extends BaseMapper<FinanceContractAmendment> {

  /**
   * 根据合同ID查询变更记录.
   *
   * @param contractId 合同ID
   * @return 变更记录列表
   */
  @Select(
      "SELECT * FROM finance_contract_amendment WHERE contract_id = #{contractId} "
          + "AND deleted = false ORDER BY created_at DESC")
  List<FinanceContractAmendment> selectByContractId(@Param("contractId") Long contractId);

  /**
   * 查询待处理的变更记录。
   *
   * @return 待处理的变更记录列表
   */
  @Select(
      "SELECT * FROM finance_contract_amendment WHERE status = 'PENDING' AND deleted = false ORDER BY created_at ASC")
  List<FinanceContractAmendment> selectPendingAmendments();

  /**
   * 统计合同的待处理变更数量.
   *
   * @param contractId 合同ID
   * @return 待处理变更数量
   */
  @Select(
      "SELECT COUNT(*) FROM finance_contract_amendment "
          + "WHERE contract_id = #{contractId} AND status = 'PENDING' "
          + "AND deleted = false")
  int countPendingByContractId(@Param("contractId") Long contractId);
}
