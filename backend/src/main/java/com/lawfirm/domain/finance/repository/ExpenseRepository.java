package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Expense;
import com.lawfirm.infrastructure.persistence.mapper.ExpenseMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 费用报销 Repository */
@Repository
public class ExpenseRepository extends AbstractRepository<ExpenseMapper, Expense> {

  /**
   * 根据报销单号查询
   *
   * @param expenseNo 报销单号
   * @return 费用报销记录，如果不存在则返回空
   */
  public Optional<Expense> findByExpenseNo(final String expenseNo) {
    Expense expense = baseMapper.selectByExpenseNo(expenseNo);
    return Optional.ofNullable(expense);
  }

  /**
   * 检查报销单号是否存在
   *
   * @param expenseNo 报销单号
   * @return 如果存在返回true，否则返回false
   */
  public boolean existsByExpenseNo(final String expenseNo) {
    return findByExpenseNo(expenseNo).isPresent();
  }

  /**
   * 根据申请人ID查询费用报销列表
   *
   * @param applicantId 申请人ID
   * @return 费用报销列表
   */
  public List<Expense> findByApplicantId(final Long applicantId) {
    return baseMapper.selectList(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Expense>()
            .eq(Expense::getApplicantId, applicantId)
            .eq(Expense::getDeleted, false)
            .orderByDesc(Expense::getCreatedAt));
  }
}
