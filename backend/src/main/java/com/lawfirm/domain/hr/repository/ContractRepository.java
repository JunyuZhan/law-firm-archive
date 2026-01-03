package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import org.apache.ibatis.type.Alias;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 劳动合同 Repository
 */
@Alias("HrContractRepository")
@Repository("hrContractRepository")
public class ContractRepository extends AbstractRepository<ContractMapper, Contract> {

    /**
     * 根据合同编号查询
     */
    public Optional<Contract> findByContractNo(String contractNo) {
        Contract contract = lambdaQuery()
                .eq(Contract::getContractNo, contractNo)
                .one();
        return Optional.ofNullable(contract);
    }

    /**
     * 根据员工ID查询生效中的合同
     */
    public Optional<Contract> findActiveContractByEmployeeId(Long employeeId) {
        Contract contract = baseMapper.selectActiveContractByEmployeeId(employeeId);
        return Optional.ofNullable(contract);
    }

    /**
     * 根据员工ID查询所有合同
     */
    public List<Contract> findByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }
}
