package com.lawfirm.domain.admin.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.admin.entity.MeetingRecord;
import com.lawfirm.infrastructure.persistence.mapper.MeetingRecordMapper;
import org.springframework.stereotype.Repository;

/** 会议记录仓储（M8-023） */
@Repository
public class MeetingRecordRepository
    extends AbstractRepository<MeetingRecordMapper, MeetingRecord> {}
