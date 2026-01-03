package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import org.apache.ibatis.type.Alias;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 合同仓储
 */
@Alias("FinanceContractRepository")
@Repository("financeContractRepository")
public class ContractRepository extends AbstractRepository<FinanceContractMapper, Contract> {

    /**
     * 根据合同编号查询
     */
    public Optional<Contract> findByContractNo(String contractNo) {
        return Optional.ofNullable(baseMapper.selectByContractNo(contractNo));
    }

    /**
     * 检查合同编号是否存在
     */
    public boolean existsByContractNo(String contractNo) {
        return findByContractNo(contractNo).isPresent();
    }

    /**
     * 根据案件ID查询合同
     */
    public Optional<Contract> findByMatterId(Long matterId) {
        return Optional.ofNullable(baseMapper.selectByMatterId(matterId));
    }
}
