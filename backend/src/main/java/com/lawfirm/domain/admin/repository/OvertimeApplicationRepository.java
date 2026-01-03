package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.OvertimeApplication;
import com.lawfirm.infrastructure.persistence.mapper.OvertimeApplicationMapper;
import org.springframework.stereotype.Repository;

/**
 * 加班申请仓储（M8-004）
 */
@Repository
public class OvertimeApplicationRepository extends AbstractRepository<OvertimeApplicationMapper, OvertimeApplication> {
}

