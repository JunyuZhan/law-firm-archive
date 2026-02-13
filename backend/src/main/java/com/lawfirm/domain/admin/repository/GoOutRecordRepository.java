package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.GoOutRecord;
import com.lawfirm.infrastructure.persistence.mapper.GoOutRecordMapper;
import org.springframework.stereotype.Repository;

/** 外出登记仓储（M8-005） */
@Repository
public class GoOutRecordRepository extends AbstractRepository<GoOutRecordMapper, GoOutRecord> {}
