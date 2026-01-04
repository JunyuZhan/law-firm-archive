package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.infrastructure.persistence.mapper.ContractParticipantMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同参与人仓储
 */
@Repository
public class ContractParticipantRepository extends AbstractRepository<ContractParticipantMapper, ContractParticipant> {

    /**
     * 查询合同的所有参与人
     */
    public List<ContractParticipant> findByContractId(Long contractId) {
        return baseMapper.selectByContractId(contractId);
    }

    /**
     * 查询合同的承办律师
     */
    public ContractParticipant findLeadByContractId(Long contractId) {
        return baseMapper.selectLeadByContractId(contractId);
    }

    /**
     * 统计合同参与人提成比例总和
     */
    public BigDecimal sumCommissionRateByContractId(Long contractId) {
        return baseMapper.sumCommissionRateByContractId(contractId);
    }

    /**
     * 检查用户是否已是合同参与人
     */
    public boolean existsByContractIdAndUserId(Long contractId, Long userId) {
        return baseMapper.countByContractIdAndUserId(contractId, userId) > 0;
    }

    /**
     * 删除合同的所有参与人
     */
    public void deleteByContractId(Long contractId) {
        baseMapper.deleteByContractId(contractId);
    }

    /**
     * 根据合同ID和用户ID删除参与人
     */
    public void deleteByContractIdAndUserId(Long contractId, Long userId) {
        baseMapper.deleteByContractIdAndUserId(contractId, userId);
    }
}
