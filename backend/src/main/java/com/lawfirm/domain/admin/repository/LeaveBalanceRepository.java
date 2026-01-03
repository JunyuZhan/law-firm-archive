package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.LeaveBalance;
import com.lawfirm.infrastructure.persistence.mapper.LeaveBalanceMapper;
import org.springframework.stereotype.Repository;

/**
 * 假期余额仓储
 */
@Repository
public class LeaveBalanceRepository extends AbstractRepository<LeaveBalanceMapper, LeaveBalance> {
}
