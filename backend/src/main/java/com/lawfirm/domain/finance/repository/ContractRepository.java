package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import org.apache.ibatis.type.Alias;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    /**
     * 统计指定日期创建的合同数量
     */
    public long countByCreatedDate(LocalDate date) {
        return baseMapper.countByCreatedDate(date);
    }

    /**
     * 统计指定年份创建的合同数量
     */
    public long countByCreatedYear(int year) {
        return baseMapper.countByCreatedYear(year);
    }

    /**
     * 根据ID查询合同并加行锁（用于并发控制）
     * 使用 SELECT ... FOR UPDATE 实现悲观锁
     */
    public Contract selectByIdForUpdate(Long id) {
        return baseMapper.selectByIdForUpdate(id);
    }

    /**
     * 查询即将到期的合同（问题255：合同到期提醒功能）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 在指定日期范围内到期的有效合同列表
     */
    public List<Contract> findExpiringContracts(LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectExpiringContracts(startDate, endDate);
    }

    /**
     * 查询已过期的合同
     * @param date 当前日期
     * @return 已过期但状态仍为ACTIVE的合同列表
     */
    public List<Contract> findExpiredContracts(LocalDate date) {
        return baseMapper.selectExpiredContracts(date);
    }

    /**
     * 统计使用指定模板的合同数量
     * ✅ 修复问题585/586: 支持检查模板使用情况
     */
    public long countByTemplateId(Long templateId) {
        return baseMapper.countByTemplateId(templateId);
    }
}
