package com.lawfirm.domain.ai.repository;

import com.lawfirm.domain.ai.entity.AiMonthlyBill;

import java.util.List;

/**
 * AI月度账单仓储接口
 */
public interface AiMonthlyBillRepository {

    /**
     * 保存账单
     */
    void save(AiMonthlyBill bill);

    /**
     * 更新账单
     */
    void update(AiMonthlyBill bill);

    /**
     * 根据ID查询
     */
    AiMonthlyBill findById(Long id);

    /**
     * 查询用户指定月份的账单
     */
    AiMonthlyBill findByUserAndMonth(Long userId, int year, int month);

    /**
     * 查询用户所有账单
     */
    List<AiMonthlyBill> findByUserId(Long userId);

    /**
     * 查询指定月份所有账单
     */
    List<AiMonthlyBill> findByMonth(int year, int month);

    /**
     * 查询指定月份待扣减的账单
     */
    List<AiMonthlyBill> findPendingByMonth(int year, int month);

    /**
     * 查询指定状态的账单
     */
    List<AiMonthlyBill> findByStatus(String status);

    /**
     * 批量更新扣减状态
     */
    void batchUpdateStatus(List<Long> ids, String status, Long operatorId, String remark);

    /**
     * 检查账单是否存在
     */
    boolean existsByUserAndMonth(Long userId, int year, int month);

    /**
     * 删除账单（逻辑删除）
     */
    void deleteById(Long id);

    /**
     * 查询指定月份所有账单（别名）
     */
    default List<AiMonthlyBill> findByPeriod(Integer year, Integer month) {
        return findByMonth(year, month);
    }

    /**
     * 查询用户指定月份的账单（别名）
     */
    default AiMonthlyBill findByUserAndPeriod(Long userId, Integer year, Integer month) {
        return findByUserAndMonth(userId, year, month);
    }

    /**
     * 根据ID查询，不存在则抛异常
     */
    default AiMonthlyBill getByIdOrThrow(Long id, String message) {
        AiMonthlyBill bill = findById(id);
        if (bill == null) {
            throw new com.lawfirm.common.exception.BusinessException(message);
        }
        return bill;
    }

    /**
     * 更新账单（别名）
     */
    default void updateById(AiMonthlyBill bill) {
        update(bill);
    }
}
