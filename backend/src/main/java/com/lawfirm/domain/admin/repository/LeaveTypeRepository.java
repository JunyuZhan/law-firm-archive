package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.LeaveType;
import com.lawfirm.infrastructure.persistence.mapper.LeaveTypeMapper;
import org.springframework.stereotype.Repository;

/** 请假类型仓储 */
@Repository
public class LeaveTypeRepository extends AbstractRepository<LeaveTypeMapper, LeaveType> {}
