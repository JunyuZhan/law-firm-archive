package com.lawfirm.domain.finance.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.finance.entity.Expense;
import com.lawfirm.infrastructure.persistence.mapper.ExpenseMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 费用报销 Repository
 */
@Repository
public class ExpenseRepository extends AbstractRepository<ExpenseMapper, Expense> {

    /**
     * 根据报销单号查询
     */
    public Optional<Expense> findByExpenseNo(String expenseNo) {
        Expense expense = baseMapper.selectByExpenseNo(expenseNo);
        return Optional.ofNullable(expense);
    }

    /**
     * 检查报销单号是否存在
     */
    public boolean existsByExpenseNo(String expenseNo) {
        return findByExpenseNo(expenseNo).isPresent();
    }
}

