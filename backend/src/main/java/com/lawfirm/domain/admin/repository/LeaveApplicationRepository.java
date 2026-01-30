package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.LeaveApplication;
import com.lawfirm.infrastructure.persistence.mapper.LeaveApplicationMapper;
import org.springframework.stereotype.Repository;

/** 请假申请仓储 */
@Repository
public class LeaveApplicationRepository
    extends AbstractRepository<LeaveApplicationMapper, LeaveApplication> {}
