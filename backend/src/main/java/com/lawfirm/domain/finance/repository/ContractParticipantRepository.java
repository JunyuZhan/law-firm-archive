package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.infrastructure.persistence.mapper.ContractParticipantMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 合同参与人仓储 */
@Repository
public class ContractParticipantRepository
    extends AbstractRepository<ContractParticipantMapper, ContractParticipant> {

  /**
   * 查询合同的所有参与人
   *
   * @param contractId 合同ID
   * @return 参与人列表
   */
  public List<ContractParticipant> findByContractId(final Long contractId) {
    return baseMapper.selectByContractId(contractId);
  }

  /**
   * 查询合同的承办律师
   *
   * @param contractId 合同ID
   * @return 承办律师
   */
  public ContractParticipant findLeadByContractId(final Long contractId) {
    return baseMapper.selectLeadByContractId(contractId);
  }

  /**
   * 统计合同参与人提成比例总和
   *
   * @param contractId 合同ID
   * @return 提成比例总和
   */
  public BigDecimal sumCommissionRateByContractId(final Long contractId) {
    return baseMapper.sumCommissionRateByContractId(contractId);
  }

  /**
   * 检查用户是否已是合同参与人
   *
   * @param contractId 合同ID
   * @param userId 用户ID
   * @return 是否存在
   */
  public boolean existsByContractIdAndUserId(final Long contractId, final Long userId) {
    return baseMapper.countByContractIdAndUserId(contractId, userId) > 0;
  }

  /**
   * 删除合同的所有参与人
   *
   * @param contractId 合同ID
   */
  public void deleteByContractId(final Long contractId) {
    baseMapper.deleteByContractId(contractId);
  }

  /**
   * 根据合同ID和用户ID删除参与人
   *
   * @param contractId 合同ID
   * @param userId 用户ID
   */
  public void deleteByContractIdAndUserId(final Long contractId, final Long userId) {
    baseMapper.deleteByContractIdAndUserId(contractId, userId);
  }

  /**
   * 根据用户ID和角色查询合同ID列表
   *
   * <p>Requirements: 5.4 - 按承办律师筛选
   *
   * @param userId 用户ID
   * @param role 角色
   * @return 合同ID列表
   */
  public List<Long> findContractIdsByUserIdAndRole(final Long userId, final String role) {
    return baseMapper.selectContractIdsByUserIdAndRole(userId, role);
  }

  /**
   * 根据用户ID查询所有参与的合同ID列表
   *
   * <p>Requirements: 8.1 - 律师只能访问自己参与的合同
   *
   * @param userId 用户ID
   * @return 合同ID列表
   */
  public List<Long> findContractIdsByUserId(final Long userId) {
    return baseMapper.selectContractIdsByUserId(userId);
  }

  /**
   * 根据用户ID查询所有参与记录
   *
   * @param userId 用户ID
   * @return 参与记录列表
   */
  public List<ContractParticipant> findByUserId(final Long userId) {
    return baseMapper.selectByUserId(userId);
  }

  /**
   * 批量查询合同参与人（避免N+1查询）
   *
   * @param contractIds 合同ID列表
   * @return 参与人列表
   */
  public List<ContractParticipant> findByContractIds(final List<Long> contractIds) {
    if (contractIds == null || contractIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    return lambdaQuery().in(ContractParticipant::getContractId, contractIds).list();
  }
}
