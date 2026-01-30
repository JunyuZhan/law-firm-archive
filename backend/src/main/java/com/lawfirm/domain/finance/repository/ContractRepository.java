package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.type.Alias;
import org.springframework.stereotype.Repository;

/** 合同仓储 */
@Alias("FinanceContractRepository")
@Repository("financeContractRepository")
public class ContractRepository extends AbstractRepository<FinanceContractMapper, Contract> {

  /**
   * 根据合同编号查询
   *
   * @param contractNo 合同编号
   * @return 合同Optional
   */
  public Optional<Contract> findByContractNo(final String contractNo) {
    return Optional.ofNullable(baseMapper.selectByContractNo(contractNo));
  }

  /**
   * 检查合同编号是否存在
   *
   * @param contractNo 合同编号
   * @return 是否存在
   */
  public boolean existsByContractNo(final String contractNo) {
    return findByContractNo(contractNo).isPresent();
  }

  /**
   * 根据案件ID查询合同
   *
   * @param matterId 案件ID
   * @return 合同Optional
   */
  public Optional<Contract> findByMatterId(final Long matterId) {
    return Optional.ofNullable(baseMapper.selectByMatterId(matterId));
  }

  /**
   * 统计指定日期创建的合同数量
   *
   * @param date 日期
   * @return 合同数量
   */
  public long countByCreatedDate(final LocalDate date) {
    return baseMapper.countByCreatedDate(date);
  }

  /**
   * 统计指定年份创建的合同数量
   *
   * @param year 年份
   * @return 合同数量
   */
  public long countByCreatedYear(final int year) {
    return baseMapper.countByCreatedYear(year);
  }

  /**
   * 统计指定日期和合同类型创建的合同数量（用于独立编号） 每种合同类型（模板类型）独立计数
   *
   * @param date 日期
   * @param contractType 合同类型
   * @return 合同数量
   */
  public long countByCreatedDateAndContractType(final LocalDate date, final String contractType) {
    return baseMapper.countByCreatedDateAndContractType(date, contractType);
  }

  /**
   * 统计指定年份和合同类型创建的合同数量（用于独立编号） 每种合同类型（模板类型）独立计数
   *
   * @param year 年份
   * @param contractType 合同类型
   * @return 合同数量
   */
  public long countByCreatedYearAndContractType(final int year, final String contractType) {
    return baseMapper.countByCreatedYearAndContractType(year, contractType);
  }

  /**
   * 根据ID查询合同并加行锁（用于并发控制） 使用 SELECT ... FOR UPDATE 实现悲观锁
   *
   * @param id 合同ID
   * @return 合同
   */
  public Contract selectByIdForUpdate(final Long id) {
    return baseMapper.selectByIdForUpdate(id);
  }

  /**
   * 查询即将到期的合同（问题255：合同到期提醒功能）
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 在指定日期范围内到期的有效合同列表
   */
  public List<Contract> findExpiringContracts(final LocalDate startDate, final LocalDate endDate) {
    return baseMapper.selectExpiringContracts(startDate, endDate);
  }

  /**
   * 查询已过期的合同
   *
   * @param date 当前日期
   * @return 已过期但状态仍为ACTIVE的合同列表
   */
  public List<Contract> findExpiredContracts(final LocalDate date) {
    return baseMapper.selectExpiredContracts(date);
  }

  /**
   * 统计使用指定模板的合同数量 ✅ 修复问题585/586: 支持检查模板使用情况
   *
   * @param templateId 模板ID
   * @return 合同数量
   */
  public long countByTemplateId(final Long templateId) {
    return baseMapper.countByTemplateId(templateId);
  }
}
