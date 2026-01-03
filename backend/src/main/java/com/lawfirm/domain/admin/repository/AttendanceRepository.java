package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.Attendance;
import com.lawfirm.infrastructure.persistence.mapper.AttendanceMapper;
import org.springframework.stereotype.Repository;

/**
 * 考勤记录仓储
 */
@Repository
public class AttendanceRepository extends AbstractRepository<AttendanceMapper, Attendance> {
}
