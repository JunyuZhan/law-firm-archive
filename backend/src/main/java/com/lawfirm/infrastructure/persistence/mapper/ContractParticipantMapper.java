package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 合同参与人 Mapper */
@Mapper
public interface ContractParticipantMapper extends BaseMapper<ContractParticipant> {

  /**
   * 查询合同的所有参与人.
   *
   * @param contractId 合同ID
   * @return 参与人列表
   */
  @Select(
      "SELECT * FROM contract_participant WHERE contract_id = #{contractId} "
          + "AND deleted = false ORDER BY role, created_at")
  List<ContractParticipant> selectByContractId(@Param("contractId") Long contractId);

  /**
   * 查询合同的承办律师.
   *
   * @param contractId 合同ID
   * @return 承办律师信息
   */
  @Select(
      "SELECT * FROM contract_participant WHERE contract_id = #{contractId} "
          + "AND role = 'LEAD' AND deleted = false LIMIT 1")
  ContractParticipant selectLeadByContractId(@Param("contractId") Long contractId);

  /**
   * 统计合同参与人提成比例总和.
   *
   * @param contractId 合同ID
   * @return 提成比例总和
   */
  @Select(
      "SELECT COALESCE(SUM(commission_rate), 0) FROM contract_participant "
          + "WHERE contract_id = #{contractId} AND deleted = false")
  BigDecimal sumCommissionRateByContractId(@Param("contractId") Long contractId);

  /**
   * 检查用户是否已是合同参与人.
   *
   * @param contractId 合同ID
   * @param userId 用户ID
   * @return 记录数
   */
  @Select(
      "SELECT COUNT(*) FROM contract_participant WHERE contract_id = #{contractId} "
          + "AND user_id = #{userId} AND deleted = false")
  int countByContractIdAndUserId(
      @Param("contractId") Long contractId, @Param("userId") Long userId);

  /**
   * 软删除合同的所有参与人.
   *
   * @param contractId 合同ID
   */
  @Update("UPDATE contract_participant SET deleted = true WHERE contract_id = #{contractId}")
  void deleteByContractId(@Param("contractId") Long contractId);

  /**
   * 根据合同ID和用户ID软删除参与人.
   *
   * @param contractId 合同ID
   * @param userId 用户ID
   */
  @Update(
      "UPDATE contract_participant SET deleted = true WHERE contract_id = #{contractId} AND user_id = #{userId}")
  void deleteByContractIdAndUserId(
      @Param("contractId") Long contractId, @Param("userId") Long userId);

  /**
   * 根据用户ID和角色查询合同ID列表 用于行政模块按承办律师筛选合同.
   *
   * @param userId 用户ID
   * @param role 角色
   * @return 合同ID列表
   */
  @Select(
      "SELECT contract_id FROM contract_participant WHERE user_id = #{userId} AND role = #{role} AND deleted = false")
  List<Long> selectContractIdsByUserIdAndRole(
      @Param("userId") Long userId, @Param("role") String role);

  /**
   * 根据用户ID查询所有参与的合同ID列表 用于数据权限控制.
   *
   * @param userId 用户ID
   * @return 合同ID列表
   */
  @Select(
      "SELECT DISTINCT contract_id FROM contract_participant WHERE user_id = #{userId} AND deleted = false")
  List<Long> selectContractIdsByUserId(@Param("userId") Long userId);

  /**
   * 根据用户ID查询所有参与记录.
   *
   * @param userId 用户ID
   * @return 参与记录列表
   */
  @Select(
      "SELECT * FROM contract_participant WHERE user_id = #{userId} AND deleted = false ORDER BY created_at DESC")
  List<ContractParticipant> selectByUserId(@Param("userId") Long userId);
}
